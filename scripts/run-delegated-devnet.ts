import {
  Connection,
  Keypair,
  PublicKey,
  SystemProgram,
  Transaction,
  type Signer,
} from '@solana/web3.js';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { ProxyAgent, setGlobalDispatcher } from 'undici';
import {
  chargeDelegatedInstruction,
  createDelegatedInstruction,
  DELEGATED_STATE_SIZE,
  delegatedAuthority,
  EVIDENCE_RECORD_SIZE,
  evidenceRecordAddress,
} from '../src/delegated-protocol.js';

const proxyUrl = process.env.HTTPS_PROXY ?? process.env.HTTP_PROXY;
if (proxyUrl) setGlobalDispatcher(new ProxyAgent(proxyUrl));

const rpcUrl = process.env.SOLANA_RPC_URL ?? 'https://api.devnet.solana.com';
const programIdValue = required('DELEGATED_PROGRAM_ID');
const authorityPath = required('SOLANA_KEYPAIR');
const executorPath = required('EXECUTOR_KEYPAIR');
const verifierPath = required('VERIFIER_KEYPAIR');
const tokenMint = new PublicKey(required('TOKEN_MINT'));
const sourceToken = new PublicKey(required('SOURCE_TOKEN_ACCOUNT'));
const merchant = new PublicKey(required('MERCHANT'));
const merchantToken = new PublicKey(required('MERCHANT_TOKEN_ACCOUNT'));
const programId = new PublicKey(programIdValue);
const perCharge = positiveBigInt('PER_CHARGE_RAW', 2_000_000n);
const periodCap = positiveBigInt('PERIOD_CAP_RAW', 8_000_000n);
const lifetimeCap = positiveBigInt('LIFETIME_CAP_RAW', 24_000_000n);
const chargeAmount = positiveBigInt('CHARGE_AMOUNT_RAW', 1_000_000n);
const periodSeconds = positiveBigInt('PERIOD_SECONDS', 604_800n);
const existingAllowanceValue = process.env.DELEGATED_ALLOWANCE?.trim();
const existingCreateSignature = process.env.CREATE_DELEGATED_SIGNATURE?.trim();

type DelegatedSnapshot = {
  version: number;
  authority: string;
  merchant: string;
  executor: string;
  verifier: string;
  tokenMint: string;
  sourceToken: string;
  perCharge: string;
  periodCap: string;
  lifetimeCap: string;
  spentInPeriod: string;
  spentLifetime: string;
  periodStartedAt: string;
  periodSeconds: string;
  expiresAt: string;
  nextNonce: string;
  paused: boolean;
  revoked: boolean;
  frozen: boolean;
  policyHash: string;
  lastEvidenceHash: string;
};

type EvidenceRecordSnapshot = {
  version: number;
  allowance: string;
  evidenceHash: string;
  kind: number;
  nonce: string;
  createdAt: string;
};

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`${name} is required`);
  return value;
}

function positiveBigInt(name: string, fallback: bigint): bigint {
  const value = process.env[name] ? BigInt(process.env[name]!) : fallback;
  if (value <= 0n) throw new Error(`${name} must be positive`);
  return value;
}

async function loadKeypair(path: string): Promise<Keypair> {
  const bytes = JSON.parse(await readFile(path, 'utf8')) as number[];
  return Keypair.fromSecretKey(Uint8Array.from(bytes));
}

function sha256(value: string): Buffer {
  return createHash('sha256').update(value).digest();
}

function explorer(signature: string): string {
  return `https://explorer.solana.com/tx/${signature}?cluster=devnet`;
}

async function send(
  connection: Connection,
  transaction: Transaction,
  feePayer: Keypair,
  signers: Signer[],
): Promise<string> {
  const latest = await connection.getLatestBlockhash('confirmed');
  transaction.feePayer = feePayer.publicKey;
  transaction.recentBlockhash = latest.blockhash;
  transaction.sign(feePayer, ...signers.filter(signer => !signer.publicKey.equals(feePayer.publicKey)));
  const signature = await connection.sendRawTransaction(transaction.serialize(), {
    skipPreflight: false,
    maxRetries: 5,
  });
  await confirmByHttpPolling(connection, signature, latest.lastValidBlockHeight);
  return signature;
}

async function confirmByHttpPolling(
  connection: Connection,
  signature: string,
  lastValidBlockHeight: number,
): Promise<void> {
  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    const statuses = await connection.getSignatureStatuses([signature], { searchTransactionHistory: true });
    const status = statuses.value[0];
    if (status?.err) throw new Error(`Transaction ${signature} failed: ${JSON.stringify(status.err)}`);
    if (status && (status.confirmationStatus === 'confirmed' || status.confirmationStatus === 'finalized')) return;
    const blockHeight = await connection.getBlockHeight('confirmed');
    if (blockHeight > lastValidBlockHeight) {
      throw new Error(`Transaction ${signature} was not found before blockhash expiry`);
    }
    await new Promise(resolve => setTimeout(resolve, 1_000));
  }
  throw new Error(`Timed out while polling transaction ${signature}`);
}

function parseState(data: Buffer): DelegatedSnapshot {
  if (data.length < DELEGATED_STATE_SIZE) throw new Error('Delegated state account is too small');
  let offset = 0;
  const byte = () => data.readUInt8(offset++);
  const pubkey = () => {
    const value = new PublicKey(data.subarray(offset, offset + 32)).toBase58();
    offset += 32;
    return value;
  };
  const u64 = () => {
    const value = data.readBigUInt64LE(offset).toString();
    offset += 8;
    return value;
  };
  const i64 = () => {
    const value = data.readBigInt64LE(offset).toString();
    offset += 8;
    return value;
  };
  const digest = () => {
    const value = data.subarray(offset, offset + 32).toString('hex');
    offset += 32;
    return value;
  };
  return {
    version: byte(),
    authority: pubkey(),
    merchant: pubkey(),
    executor: pubkey(),
    verifier: pubkey(),
    tokenMint: pubkey(),
    sourceToken: pubkey(),
    perCharge: u64(),
    periodCap: u64(),
    lifetimeCap: u64(),
    spentInPeriod: u64(),
    spentLifetime: u64(),
    periodStartedAt: i64(),
    periodSeconds: i64(),
    expiresAt: i64(),
    nextNonce: u64(),
    paused: byte() === 1,
    revoked: byte() === 1,
    frozen: byte() === 1,
    policyHash: digest(),
    lastEvidenceHash: digest(),
  };
}

function parseEvidenceRecord(data: Buffer): EvidenceRecordSnapshot {
  if (data.length < EVIDENCE_RECORD_SIZE) throw new Error('Evidence record account is too small');
  let offset = 0;
  const version = data.readUInt8(offset++);
  const allowance = new PublicKey(data.subarray(offset, offset + 32)).toBase58();
  offset += 32;
  const evidenceHash = data.subarray(offset, offset + 32).toString('hex');
  offset += 32;
  const kind = data.readUInt8(offset++);
  const nonce = data.readBigUInt64LE(offset).toString();
  offset += 8;
  const createdAt = data.readBigInt64LE(offset).toString();
  return { version, allowance, evidenceHash, kind, nonce, createdAt };
}

async function main(): Promise<void> {
  if (perCharge > periodCap || periodCap > lifetimeCap || chargeAmount > perCharge) {
    throw new Error('Require CHARGE_AMOUNT_RAW <= PER_CHARGE_RAW <= PERIOD_CAP_RAW <= LIFETIME_CAP_RAW');
  }
  const [authority, executor, verifier] = await Promise.all([
    loadKeypair(authorityPath),
    loadKeypair(executorPath),
    loadKeypair(verifierPath),
  ]);
  const identities = new Set([
    authority.publicKey.toBase58(),
    executor.publicKey.toBase58(),
    verifier.publicKey.toBase58(),
    merchant.toBase58(),
  ]);
  if (identities.size !== 4) throw new Error('Authority, executor, verifier, and merchant must be distinct');

  const connection = new Connection(rpcUrl, 'confirmed');
  const [sourceInfo, merchantInfo] = await Promise.all([
    connection.getParsedAccountInfo(sourceToken, 'confirmed'),
    connection.getParsedAccountInfo(merchantToken, 'confirmed'),
  ]);
  const sourceParsed = sourceInfo.value?.data;
  const merchantParsed = merchantInfo.value?.data;
  if (!sourceParsed || Buffer.isBuffer(sourceParsed) || !merchantParsed || Buffer.isBuffer(merchantParsed)) {
    throw new Error('Expected parsed SPL token accounts');
  }
  const sourceData = sourceParsed.parsed.info as { mint: string; owner: string };
  const merchantData = merchantParsed.parsed.info as { mint: string; owner: string };
  if (sourceData.owner !== authority.publicKey.toBase58() || sourceData.mint !== tokenMint.toBase58()) {
    throw new Error('SOURCE_TOKEN_ACCOUNT must be owned by SOLANA_KEYPAIR and use TOKEN_MINT');
  }
  if (merchantData.owner !== merchant.toBase58() || merchantData.mint !== tokenMint.toBase58()) {
    throw new Error('MERCHANT_TOKEN_ACCOUNT must be owned by MERCHANT and use TOKEN_MINT');
  }

  const allowanceKeypair = existingAllowanceValue ? undefined : Keypair.generate();
  const allowance = existingAllowanceValue
    ? new PublicKey(existingAllowanceValue)
    : allowanceKeypair!.publicKey;
  const [delegate] = delegatedAuthority(programId, allowance);
  let createSignature = existingCreateSignature;

  if (!existingAllowanceValue) {
    const now = BigInt(Math.floor(Date.now() / 1000));
    const expiresAt = now + 30n * 86_400n;
    const policyHash = sha256(JSON.stringify({
      version: 2,
      authority: authority.publicKey.toBase58(),
      merchant: merchant.toBase58(),
      executor: executor.publicKey.toBase58(),
      verifier: verifier.publicKey.toBase58(),
      tokenMint: tokenMint.toBase58(),
      sourceToken: sourceToken.toBase58(),
      perCharge: perCharge.toString(),
      periodCap: periodCap.toString(),
      lifetimeCap: lifetimeCap.toString(),
      periodSeconds: periodSeconds.toString(),
      expiresAt: expiresAt.toString(),
    }));
    const rent = await connection.getMinimumBalanceForRentExemption(DELEGATED_STATE_SIZE, 'confirmed');
    createSignature = await send(
      connection,
      new Transaction().add(
        SystemProgram.createAccount({
          fromPubkey: authority.publicKey,
          newAccountPubkey: allowance,
          lamports: rent,
          space: DELEGATED_STATE_SIZE,
          programId,
        }),
        createDelegatedInstruction({
          programId,
          allowance,
          authority: authority.publicKey,
          merchant,
          executor: executor.publicKey,
          verifier: verifier.publicKey,
          tokenMint,
          sourceToken,
          perCharge,
          periodCap,
          lifetimeCap,
          periodSeconds,
          expiresAt,
          policyHash,
        }),
      ),
      authority,
      [allowanceKeypair!],
    );
  }

  const stateBeforeAccount = await connection.getAccountInfo(allowance, 'confirmed');
  if (!stateBeforeAccount || !stateBeforeAccount.owner.equals(programId)) {
    throw new Error('Delegated state was not found before charge');
  }
  const stateBefore = parseState(stateBeforeAccount.data);
  const expectedActors = {
    authority: authority.publicKey.toBase58(),
    merchant: merchant.toBase58(),
    executor: executor.publicKey.toBase58(),
    verifier: verifier.publicKey.toBase58(),
    tokenMint: tokenMint.toBase58(),
    sourceToken: sourceToken.toBase58(),
  };
  for (const [field, expected] of Object.entries(expectedActors)) {
    if (stateBefore[field as keyof DelegatedSnapshot] !== expected) {
      throw new Error(`Existing allowance ${field} does not match the requested role or asset`);
    }
  }
  if (stateBefore.paused || stateBefore.revoked || stateBefore.frozen) {
    throw new Error('Existing allowance is not active');
  }
  const nonce = BigInt(stateBefore.nextNonce);
  const evidenceHash = sha256(`ALLOWANCE_OS|DELEGATED_V2|${allowance.toBase58()}|${nonce}`);
  const [evidenceRecordAddressValue] = evidenceRecordAddress(programId, allowance, evidenceHash);

  const [sourceBefore, merchantBefore] = await Promise.all([
    connection.getTokenAccountBalance(sourceToken, 'confirmed'),
    connection.getTokenAccountBalance(merchantToken, 'confirmed'),
  ]);
  const chargeSignature = await send(
    connection,
    new Transaction().add(chargeDelegatedInstruction({
      programId,
      allowance,
      executor: executor.publicKey,
      verifier: verifier.publicKey,
      sourceToken,
      merchantToken,
      amount: chargeAmount,
      nonce,
      evidenceHash,
    })),
    executor,
    [verifier],
  );
  const [sourceAfter, merchantAfter, stateAccount, evidenceRecordAccount] = await Promise.all([
    connection.getTokenAccountBalance(sourceToken, 'confirmed'),
    connection.getTokenAccountBalance(merchantToken, 'confirmed'),
    connection.getAccountInfo(allowance, 'confirmed'),
    connection.getAccountInfo(evidenceRecordAddressValue, 'confirmed'),
  ]);
  if (!stateAccount || !stateAccount.owner.equals(programId)) throw new Error('Delegated state was not found');
  if (!evidenceRecordAccount || !evidenceRecordAccount.owner.equals(programId)) {
    throw new Error('Historical evidence record was not found');
  }
  const state = parseState(stateAccount.data);
  const evidenceRecord = parseEvidenceRecord(evidenceRecordAccount.data);
  const expectedNextNonce = (nonce + 1n).toString();
  const expectedLifetimeSpend = (BigInt(stateBefore.spentLifetime) + chargeAmount).toString();
  if (state.version !== 2 || state.nextNonce !== expectedNextNonce || state.spentLifetime !== expectedLifetimeSpend) {
    throw new Error(`Unexpected delegated state after charge: ${JSON.stringify(state)}`);
  }
  if (
    evidenceRecord.version !== 1
    || evidenceRecord.allowance !== allowance.toBase58()
    || evidenceRecord.evidenceHash !== evidenceHash.toString('hex')
    || evidenceRecord.kind !== 1
    || evidenceRecord.nonce !== nonce.toString()
  ) {
    throw new Error(`Unexpected historical evidence record: ${JSON.stringify(evidenceRecord)}`);
  }
  const sourceDelta = BigInt(sourceBefore.value.amount) - BigInt(sourceAfter.value.amount);
  const merchantDelta = BigInt(merchantAfter.value.amount) - BigInt(merchantBefore.value.amount);
  if (sourceDelta !== chargeAmount || merchantDelta !== chargeAmount) {
    throw new Error(`Token deltas do not equal charge: source=${sourceDelta}, merchant=${merchantDelta}`);
  }

  console.log(JSON.stringify({
    status: 'DELEGATED_V2_VERIFIED',
    truthBoundary: 'This output is live only when produced against a deployed v2 Program.',
    programId: programId.toBase58(),
    allowance: allowance.toBase58(),
    delegate: delegate.toBase58(),
    authority: authority.publicKey.toBase58(),
    executor: executor.publicKey.toBase58(),
    verifier: verifier.publicKey.toBase58(),
    authoritySignedCharge: false,
    policyHash: state.policyHash,
    evidenceHash: evidenceHash.toString('hex'),
    evidenceRecordAddress: evidenceRecordAddressValue.toBase58(),
    transactions: {
      createDelegated: createSignature
        ? { signature: createSignature, explorer: explorer(createSignature) }
        : { signature: null, explorer: null },
      chargeDelegated: { signature: chargeSignature, explorer: explorer(chargeSignature) },
    },
    tokenDeltas: {
      sourceRaw: `-${sourceDelta}`,
      merchantRaw: `+${merchantDelta}`,
    },
    state,
    evidenceRecord,
  }, null, 2));
}

await main();
