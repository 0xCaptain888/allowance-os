import {
  Connection,
  Keypair,
  PublicKey,
  SystemProgram,
  Transaction,
  TransactionInstruction,
  type Signer,
} from '@solana/web3.js';
import { readFile } from 'node:fs/promises';
import { ProxyAgent, setGlobalDispatcher } from 'undici';

const proxyUrl = process.env.HTTPS_PROXY ?? process.env.HTTP_PROXY;
if (proxyUrl) setGlobalDispatcher(new ProxyAgent(proxyUrl));

const rpcUrl = process.env.SOLANA_RPC_URL ?? 'https://api.devnet.solana.com';
const merchant = new PublicKey(required('MERCHANT'));
const TOKEN_PROGRAM_ID = new PublicKey('TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA');
const ASSOCIATED_TOKEN_PROGRAM_ID = new PublicKey('ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL');
const MINT_SIZE = 82;

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`${name} is required`);
  return value;
}

async function loadKeypair(name: string): Promise<Keypair> {
  const bytes = JSON.parse(await readFile(required(name), 'utf8')) as number[];
  return Keypair.fromSecretKey(Uint8Array.from(bytes));
}

function associatedTokenAddress(mint: PublicKey, owner: PublicKey): PublicKey {
  return PublicKey.findProgramAddressSync(
    [owner.toBuffer(), TOKEN_PROGRAM_ID.toBuffer(), mint.toBuffer()],
    ASSOCIATED_TOKEN_PROGRAM_ID,
  )[0];
}

function initializeMint2Instruction(mint: PublicKey, authority: PublicKey): TransactionInstruction {
  const data = Buffer.alloc(1 + 1 + 32 + 4);
  data.writeUInt8(20, 0); // InitializeMint2
  data.writeUInt8(6, 1);
  authority.toBuffer().copy(data, 2);
  data.writeUInt32LE(0, 34); // no freeze authority
  return new TransactionInstruction({
    programId: TOKEN_PROGRAM_ID,
    keys: [{ pubkey: mint, isSigner: false, isWritable: true }],
    data,
  });
}

function createAssociatedTokenInstruction(
  payer: PublicKey,
  account: PublicKey,
  owner: PublicKey,
  mint: PublicKey,
): TransactionInstruction {
  return new TransactionInstruction({
    programId: ASSOCIATED_TOKEN_PROGRAM_ID,
    keys: [
      { pubkey: payer, isSigner: true, isWritable: true },
      { pubkey: account, isSigner: false, isWritable: true },
      { pubkey: owner, isSigner: false, isWritable: false },
      { pubkey: mint, isSigner: false, isWritable: false },
      { pubkey: SystemProgram.programId, isSigner: false, isWritable: false },
      { pubkey: TOKEN_PROGRAM_ID, isSigner: false, isWritable: false },
    ],
    data: Buffer.alloc(0),
  });
}

function mintToInstruction(
  mint: PublicKey,
  destination: PublicKey,
  authority: PublicKey,
  amount: bigint,
): TransactionInstruction {
  const data = Buffer.alloc(9);
  data.writeUInt8(7, 0); // MintTo
  data.writeBigUInt64LE(amount, 1);
  return new TransactionInstruction({
    programId: TOKEN_PROGRAM_ID,
    keys: [
      { pubkey: mint, isSigner: false, isWritable: true },
      { pubkey: destination, isSigner: false, isWritable: true },
      { pubkey: authority, isSigner: true, isWritable: false },
    ],
    data,
  });
}

async function confirmByHttp(
  connection: Connection,
  signature: string,
  lastValidBlockHeight: number,
): Promise<void> {
  const deadline = Date.now() + 150_000;
  while (Date.now() < deadline) {
    const statuses = await connection.getSignatureStatuses([signature], { searchTransactionHistory: true });
    const status = statuses.value[0];
    if (status?.err) throw new Error(`Transaction ${signature} failed: ${JSON.stringify(status.err)}`);
    if (status && (status.confirmationStatus === 'confirmed' || status.confirmationStatus === 'finalized')) return;
    if (await connection.getBlockHeight('confirmed') > lastValidBlockHeight) {
      throw new Error(`Transaction ${signature} was not found before blockhash expiry`);
    }
    await new Promise(resolve => setTimeout(resolve, 1_000));
  }
  throw new Error(`Timed out while polling transaction ${signature}`);
}

async function send(
  connection: Connection,
  transaction: Transaction,
  payer: Keypair,
  signers: Signer[] = [],
): Promise<string> {
  const latest = await connection.getLatestBlockhash('confirmed');
  transaction.feePayer = payer.publicKey;
  transaction.recentBlockhash = latest.blockhash;
  transaction.sign(payer, ...signers.filter(signer => !signer.publicKey.equals(payer.publicKey)));
  const signature = await connection.sendRawTransaction(transaction.serialize(), {
    skipPreflight: false,
    maxRetries: 10,
  });
  await confirmByHttp(connection, signature, latest.lastValidBlockHeight);
  return signature;
}

async function main(): Promise<void> {
  const [authority, mint] = await Promise.all([
    loadKeypair('SOLANA_KEYPAIR'),
    loadKeypair('MINT_KEYPAIR'),
  ]);
  const connection = new Connection(rpcUrl, 'confirmed');
  const signatures: Record<string, string> = {};

  if (!await connection.getAccountInfo(mint.publicKey, 'confirmed')) {
    const rent = await connection.getMinimumBalanceForRentExemption(MINT_SIZE, 'confirmed');
    signatures.createMint = await send(
      connection,
      new Transaction().add(
        SystemProgram.createAccount({
          fromPubkey: authority.publicKey,
          newAccountPubkey: mint.publicKey,
          lamports: rent,
          space: MINT_SIZE,
          programId: TOKEN_PROGRAM_ID,
        }),
        initializeMint2Instruction(mint.publicKey, authority.publicKey),
      ),
      authority,
      [mint],
    );
  }

  const sourceToken = associatedTokenAddress(mint.publicKey, authority.publicKey);
  if (!await connection.getAccountInfo(sourceToken, 'confirmed')) {
    signatures.createSourceToken = await send(
      connection,
      new Transaction().add(createAssociatedTokenInstruction(
        authority.publicKey,
        sourceToken,
        authority.publicKey,
        mint.publicKey,
      )),
      authority,
    );
  }

  const merchantToken = associatedTokenAddress(mint.publicKey, merchant);
  if (!await connection.getAccountInfo(merchantToken, 'confirmed')) {
    signatures.createMerchantToken = await send(
      connection,
      new Transaction().add(createAssociatedTokenInstruction(
        authority.publicKey,
        merchantToken,
        merchant,
        mint.publicKey,
      )),
      authority,
    );
  }

  const currentRaw = BigInt((await connection.getTokenAccountBalance(sourceToken, 'confirmed')).value.amount);
  if (currentRaw < 20_000_000n) {
    signatures.mintTestTokens = await send(
      connection,
      new Transaction().add(mintToInstruction(
        mint.publicKey,
        sourceToken,
        authority.publicKey,
        20_000_000n - currentRaw,
      )),
      authority,
    );
  }

  console.log(JSON.stringify({
    truthBoundary: 'Project-created 6-decimal Solana Devnet test mint; not canonical USDC.',
    mint: mint.publicKey.toBase58(),
    sourceTokenAccount: sourceToken.toBase58(),
    merchantTokenAccount: merchantToken.toBase58(),
    sourceBalanceRaw: (await connection.getTokenAccountBalance(sourceToken, 'confirmed')).value.amount,
    setupTransactions: Object.fromEntries(Object.entries(signatures).map(([name, signature]) => [name, {
      signature,
      explorer: `https://explorer.solana.com/tx/${signature}?cluster=devnet`,
    }])),
  }, null, 2));
}

await main();
