import {
  Connection,
  Keypair,
  PublicKey,
  SystemProgram,
  Transaction,
  TransactionInstruction,
} from "@solana/web3.js";
import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { ProxyAgent, setGlobalDispatcher } from "undici";

const proxyUrl = process.env.HTTPS_PROXY ?? process.env.HTTP_PROXY;
if (proxyUrl) {
  setGlobalDispatcher(new ProxyAgent(proxyUrl));
}

const PROGRAM_ID = new PublicKey(
  process.env.ALLOWANCE_PROGRAM_ID ?? "DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE",
);
const RPC_URL = process.env.SOLANA_RPC_URL ?? "https://api.devnet.solana.com";
const KEYPAIR_PATH = process.env.SOLANA_KEYPAIR;
const STATE_SIZE = 259;
const VERIFIED_AMOUNT = 1_000_000n;
const BLOCKED_AMOUNT = 3_000_000n;
const PER_CHARGE = 2_000_000n;
const PERIOD_CAP = 8_000_000n;
const MERCHANT = new PublicKey("D3XJqkeFiPNtuwKkyeJfVG1Gjvi88AV6fiNs29ukjKm6");
const TOKEN_PROGRAM_ID = new PublicKey("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA");

type StateSnapshot = {
  version: number;
  authority: string;
  merchant: string;
  tokenMint: string;
  allowedProgram: string;
  perCharge: string;
  periodCap: string;
  spentInPeriod: string;
  expiresAt: string;
  revoked: boolean;
  frozen: boolean;
  policyHash: string;
  requiredEvidenceHash: string;
  lastEvidenceHash: string;
};

function hash(value: string): Buffer {
  return createHash("sha256").update(value).digest();
}

function u64(value: bigint): Buffer {
  const buffer = Buffer.alloc(8);
  buffer.writeBigUInt64LE(value);
  return buffer;
}

function i64(value: bigint): Buffer {
  const buffer = Buffer.alloc(8);
  buffer.writeBigInt64LE(value);
  return buffer;
}

function createInstructionData(
  expiresAt: bigint,
  policyHash: Buffer,
  evidenceHash: Buffer,
  tokenMint: PublicKey,
): Buffer {
  return Buffer.concat([
    Buffer.from([0]),
    MERCHANT.toBuffer(),
    tokenMint.toBuffer(),
    PROGRAM_ID.toBuffer(),
    u64(PER_CHARGE),
    u64(PERIOD_CAP),
    i64(expiresAt),
    policyHash,
    evidenceHash,
  ]);
}

function chargeInstructionData(amount: bigint, evidenceHash: Buffer): Buffer {
  return Buffer.concat([Buffer.from([1]), u64(amount), evidenceHash]);
}

function revokeInstructionData(): Buffer {
  return Buffer.from([2]);
}

function programInstruction(authority: PublicKey, allowance: PublicKey, data: Buffer): TransactionInstruction {
  return new TransactionInstruction({
    programId: PROGRAM_ID,
    keys: [
      { pubkey: authority, isSigner: true, isWritable: false },
      { pubkey: allowance, isSigner: false, isWritable: true },
    ],
    data,
  });
}

function chargeProgramInstruction(
  authority: PublicKey,
  allowance: PublicKey,
  sourceToken: PublicKey,
  merchantToken: PublicKey,
  data: Buffer,
): TransactionInstruction {
  return new TransactionInstruction({
    programId: PROGRAM_ID,
    keys: [
      { pubkey: authority, isSigner: true, isWritable: false },
      { pubkey: allowance, isSigner: false, isWritable: true },
      { pubkey: sourceToken, isSigner: false, isWritable: true },
      { pubkey: merchantToken, isSigner: false, isWritable: true },
      { pubkey: TOKEN_PROGRAM_ID, isSigner: false, isWritable: false },
    ],
    data,
  });
}

function explorer(signature: string): string {
  return `https://explorer.solana.com/tx/${signature}?cluster=devnet`;
}

function parseState(data: Buffer): StateSnapshot {
  let offset = 0;
  const byte = () => data.readUInt8(offset++);
  const pubkey = () => {
    const value = new PublicKey(data.subarray(offset, offset + 32)).toBase58();
    offset += 32;
    return value;
  };
  const readU64 = () => {
    const value = data.readBigUInt64LE(offset);
    offset += 8;
    return value.toString();
  };
  const readI64 = () => {
    const value = data.readBigInt64LE(offset);
    offset += 8;
    return value.toString();
  };
  const digest = () => {
    const value = data.subarray(offset, offset + 32).toString("hex");
    offset += 32;
    return value;
  };

  return {
    version: byte(),
    authority: pubkey(),
    merchant: pubkey(),
    tokenMint: pubkey(),
    allowedProgram: pubkey(),
    perCharge: readU64(),
    periodCap: readU64(),
    spentInPeriod: readU64(),
    expiresAt: readI64(),
    revoked: byte() === 1,
    frozen: byte() === 1,
    policyHash: digest(),
    requiredEvidenceHash: digest(),
    lastEvidenceHash: digest(),
  };
}

async function fetchState(connection: Connection, allowance: PublicKey): Promise<StateSnapshot> {
  const account = await connection.getAccountInfo(allowance, "confirmed");
  if (!account) throw new Error(`Allowance account ${allowance.toBase58()} was not found`);
  if (!account.owner.equals(PROGRAM_ID)) throw new Error("Allowance account has the wrong owner");
  return parseState(account.data);
}

async function tokenBalances(connection: Connection, source: PublicKey, merchant: PublicKey) {
  const [sourceBalance, merchantBalance] = await Promise.all([
    connection.getTokenAccountBalance(source, "confirmed"),
    connection.getTokenAccountBalance(merchant, "confirmed"),
  ]);
  return {
    sourceRaw: sourceBalance.value.amount,
    merchantRaw: merchantBalance.value.amount,
    decimals: sourceBalance.value.decimals,
  };
}

function assertTokenBalances(
  label: string,
  actual: Awaited<ReturnType<typeof tokenBalances>>,
  expectedSource: bigint,
  expectedMerchant: bigint,
): void {
  if (BigInt(actual.sourceRaw) !== expectedSource || BigInt(actual.merchantRaw) !== expectedMerchant) {
    throw new Error(
      `${label} token balances did not match: source=${actual.sourceRaw}, merchant=${actual.merchantRaw}`,
    );
  }
}

async function sendRejectedTransaction(
  connection: Connection,
  payer: Keypair,
  instruction: TransactionInstruction,
): Promise<{ signature: string; error: unknown }> {
  const latest = await connection.getLatestBlockhash("confirmed");
  const transaction = new Transaction({
    feePayer: payer.publicKey,
    blockhash: latest.blockhash,
    lastValidBlockHeight: latest.lastValidBlockHeight,
  }).add(instruction);
  transaction.sign(payer);
  const signature = await connection.sendRawTransaction(transaction.serialize(), {
    skipPreflight: true,
    maxRetries: 5,
  });
  const status = await waitForSignature(connection, signature);
  if (!status.err) throw new Error("Expected the over-budget transaction to be rejected onchain");
  return { signature, error: status.err };
}

async function waitForSignature(connection: Connection, signature: string) {
  const deadline = Date.now() + 90_000;
  while (Date.now() < deadline) {
    const response = await connection.getSignatureStatuses([signature], {
      searchTransactionHistory: true,
    });
    const status = response.value[0];
    if (status && (status.confirmationStatus === "confirmed" || status.confirmationStatus === "finalized")) {
      return status;
    }
    await new Promise((resolve) => setTimeout(resolve, 2_000));
  }
  throw new Error(`Timed out waiting for signature ${signature}`);
}

async function sendSuccessfulTransaction(
  connection: Connection,
  transaction: Transaction,
  signers: Keypair[],
): Promise<string> {
  const latest = await connection.getLatestBlockhash("confirmed");
  transaction.feePayer = signers[0].publicKey;
  transaction.recentBlockhash = latest.blockhash;
  transaction.sign(...signers);
  const signature = await connection.sendRawTransaction(transaction.serialize(), {
    skipPreflight: false,
    maxRetries: 5,
  });
  const status = await waitForSignature(connection, signature);
  if (status.err) throw new Error(`Transaction ${signature} failed: ${JSON.stringify(status.err)}`);
  return signature;
}

async function main(): Promise<void> {
  if (!KEYPAIR_PATH) {
    throw new Error("Set SOLANA_KEYPAIR to a funded Devnet payer keypair file");
  }
  if (!process.env.TOKEN_MINT || !process.env.SOURCE_TOKEN_ACCOUNT || !process.env.MERCHANT_TOKEN_ACCOUNT) {
    throw new Error("Set TOKEN_MINT, SOURCE_TOKEN_ACCOUNT, and MERCHANT_TOKEN_ACCOUNT for live SPL settlement");
  }
  const tokenMint = new PublicKey(process.env.TOKEN_MINT);
  const sourceToken = new PublicKey(process.env.SOURCE_TOKEN_ACCOUNT);
  const merchantToken = new PublicKey(process.env.MERCHANT_TOKEN_ACCOUNT);
  const secret = JSON.parse(await readFile(KEYPAIR_PATH, "utf8")) as number[];
  const payer = Keypair.fromSecretKey(Uint8Array.from(secret));
  const resumedAllowance = process.env.ALLOWANCE_ACCOUNT;
  const allowanceKeypair = resumedAllowance ? null : Keypair.generate();
  const allowance = resumedAllowance
    ? new PublicKey(resumedAllowance)
    : allowanceKeypair!.publicKey;
  const connection = new Connection(RPC_URL, "confirmed");
  const policyHash = hash(
    JSON.stringify({
      programId: PROGRAM_ID.toBase58(),
      authority: payer.publicKey.toBase58(),
      merchant: MERCHANT.toBase58(),
      tokenMint: tokenMint.toBase58(),
      perCharge: PER_CHARGE.toString(),
      periodCap: PERIOD_CAP.toString(),
    }),
  );
  const requiredEvidenceHash = hash("ALLOWANCE_OS|LIVE_DEVNET|VERIFIED|v0.7.0");
  const mismatchedEvidenceHash = hash("ALLOWANCE_OS|LIVE_DEVNET|MISMATCH|v0.7.0");
  const expiresAt = BigInt(Math.floor(Date.now() / 1000) + 30 * 24 * 60 * 60);
  const rent = await connection.getMinimumBalanceForRentExemption(STATE_SIZE, "confirmed");

  const createSignature = resumedAllowance
    ? process.env.ALLOWANCE_CREATE_SIGNATURE ?? "resumed-existing-account"
    : await sendSuccessfulTransaction(
        connection,
        new Transaction().add(
          SystemProgram.createAccount({
            fromPubkey: payer.publicKey,
            newAccountPubkey: allowance,
            lamports: rent,
            space: STATE_SIZE,
            programId: PROGRAM_ID,
          }),
          programInstruction(
            payer.publicKey,
            allowance,
            createInstructionData(expiresAt, policyHash, requiredEvidenceHash, tokenMint),
          ),
        ),
        [payer, allowanceKeypair!],
      );
  const created = await fetchState(connection, allowance);

  const balancesBefore = await tokenBalances(connection, sourceToken, merchantToken);
  const verifiedSignature = await sendSuccessfulTransaction(
    connection,
    new Transaction().add(
      chargeProgramInstruction(
        payer.publicKey,
        allowance,
        sourceToken,
        merchantToken,
        chargeInstructionData(VERIFIED_AMOUNT, requiredEvidenceHash),
      ),
    ),
    [payer],
  );
  const verified = await fetchState(connection, allowance);
  const balancesAfterVerified = await tokenBalances(connection, sourceToken, merchantToken);
  const sourceAfterVerified = BigInt(balancesBefore.sourceRaw) - VERIFIED_AMOUNT;
  const merchantAfterVerified = BigInt(balancesBefore.merchantRaw) + VERIFIED_AMOUNT;
  assertTokenBalances("VERIFIED", balancesAfterVerified, sourceAfterVerified, merchantAfterVerified);

  const blocked = await sendRejectedTransaction(
    connection,
    payer,
    chargeProgramInstruction(
      payer.publicKey,
      allowance,
      sourceToken,
      merchantToken,
      chargeInstructionData(BLOCKED_AMOUNT, requiredEvidenceHash),
    ),
  );
  const afterBlocked = await fetchState(connection, allowance);
  const balancesAfterBlocked = await tokenBalances(connection, sourceToken, merchantToken);
  assertTokenBalances("BLOCKED", balancesAfterBlocked, sourceAfterVerified, merchantAfterVerified);

  const frozenSignature = await sendSuccessfulTransaction(
    connection,
    new Transaction().add(
      chargeProgramInstruction(
        payer.publicKey,
        allowance,
        sourceToken,
        merchantToken,
        chargeInstructionData(VERIFIED_AMOUNT, mismatchedEvidenceHash),
      ),
    ),
    [payer],
  );
  const frozen = await fetchState(connection, allowance);
  const balancesAfterFrozen = await tokenBalances(connection, sourceToken, merchantToken);
  assertTokenBalances("FROZEN", balancesAfterFrozen, sourceAfterVerified, merchantAfterVerified);

  const revokeSignature = await sendSuccessfulTransaction(
    connection,
    new Transaction().add(
      programInstruction(payer.publicKey, allowance, revokeInstructionData()),
    ),
    [payer],
  );
  const revoked = await fetchState(connection, allowance);
  const balancesAfterRevoke = await tokenBalances(connection, sourceToken, merchantToken);
  assertTokenBalances("REVOKED", balancesAfterRevoke, sourceAfterVerified, merchantAfterVerified);

  console.log(JSON.stringify({
    status: "LIVE_DEVNET_TRI_STATE_COMPLETE",
    evidenceLevel: "PROGRAM_ENFORCED_SPL_TOKEN_SETTLEMENT",
    settlementDisclosure: "Only the VERIFIED transaction transfers SPL tokens; BLOCKED and FROZEN do not.",
    rpcUrl: RPC_URL,
    programId: PROGRAM_ID.toBase58(),
    allowanceAccount: allowance.toBase58(),
    payer: payer.publicKey.toBase58(),
    policyHash: policyHash.toString("hex"),
    requiredEvidenceHash: requiredEvidenceHash.toString("hex"),
    tokenSettlement: {
      mint: tokenMint.toBase58(),
      sourceTokenAccount: sourceToken.toBase58(),
      merchantTokenAccount: merchantToken.toBase58(),
      rawTransferAmount: VERIFIED_AMOUNT.toString(),
      balancesBefore,
      balancesAfterVerified,
      balancesAfterBlocked,
      balancesAfterFrozen,
      balancesAfterRevoke,
    },
    transactions: {
      create: { state: "CREATED", signature: createSignature, explorer: explorer(createSignature) },
      verified: { state: "VERIFIED", signature: verifiedSignature, explorer: explorer(verifiedSignature) },
      blocked: { state: "BLOCKED", signature: blocked.signature, error: blocked.error, explorer: explorer(blocked.signature) },
      frozen: { state: "FROZEN", signature: frozenSignature, explorer: explorer(frozenSignature) },
      revoke: { state: "REVOKED", signature: revokeSignature, explorer: explorer(revokeSignature) },
    },
    stateSnapshots: { created, verified, afterBlocked, frozen, revoked },
  }, null, 2));
}

await main();
