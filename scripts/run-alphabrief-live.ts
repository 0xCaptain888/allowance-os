import {
  Connection,
  Keypair,
  PublicKey,
  SystemProgram,
  Transaction,
  type Signer,
} from '@solana/web3.js';
import { readFile } from 'node:fs/promises';
import { ProxyAgent, setGlobalDispatcher } from 'undici';
import {
  alphaBriefVerificationPolicy,
  verifyAlphaBriefDelivery,
  type AlphaBriefDelivery,
} from '../src/alphabrief.js';
import { stableHash } from '../src/hash.js';
import {
  chargeDelegatedInstruction,
  createDelegatedInstruction,
  DELEGATED_STATE_SIZE,
  delegatedAuthority,
  evidenceRecordAddress,
  freezeDelegatedInstruction,
} from '../src/delegated-protocol.js';

const proxyUrl = process.env.HTTPS_PROXY ?? process.env.HTTP_PROXY;
if (proxyUrl) setGlobalDispatcher(new ProxyAgent(proxyUrl));

const rpcUrl = process.env.SOLANA_RPC_URL ?? 'https://api.devnet.solana.com';
const programId = new PublicKey(required('DELEGATED_PROGRAM_ID'));
const tokenMint = new PublicKey(required('TOKEN_MINT'));
const sourceToken = new PublicKey(required('SOURCE_TOKEN_ACCOUNT'));
const merchant = new PublicKey(required('MERCHANT'));
const merchantToken = new PublicKey(required('MERCHANT_TOKEN_ACCOUNT'));
const reportPath = process.env.ALPHABRIEF_REPORT_PATH
  ?? 'examples/alphabrief/deliveries/solana-mobile-commerce-risk-2026-09-10.md';
const amountRaw = 2_000_000n;

type DelegatedState = {
  version: number;
  authority: string;
  merchant: string;
  executor: string;
  verifier: string;
  tokenMint: string;
  sourceToken: string;
  spentInPeriod: string;
  spentLifetime: string;
  nextNonce: string;
  paused: boolean;
  revoked: boolean;
  frozen: boolean;
  policyHash: string;
  lastEvidenceHash: string;
};

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`${name} is required`);
  return value;
}

async function loadKeypair(name: string): Promise<Keypair> {
  const bytes = JSON.parse(await readFile(required(name), 'utf8')) as number[];
  return Keypair.fromSecretKey(Uint8Array.from(bytes));
}

function explorer(signature: string): string {
  return `https://explorer.solana.com/tx/${signature}?cluster=devnet`;
}

async function confirmByHttp(
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
  feePayer: Keypair,
  signers: Signer[] = [],
): Promise<string> {
  const latest = await connection.getLatestBlockhash('confirmed');
  transaction.feePayer = feePayer.publicKey;
  transaction.recentBlockhash = latest.blockhash;
  transaction.sign(feePayer, ...signers.filter(signer => !signer.publicKey.equals(feePayer.publicKey)));
  const signature = await connection.sendRawTransaction(transaction.serialize(), {
    skipPreflight: false,
    maxRetries: 5,
  });
  await confirmByHttp(connection, signature, latest.lastValidBlockHeight);
  return signature;
}

function parseState(data: Buffer): DelegatedState {
  if (data.length < DELEGATED_STATE_SIZE) throw new Error('Delegated allowance state is too short');
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
  const version = byte();
  const authority = pubkey();
  const merchantValue = pubkey();
  const executor = pubkey();
  const verifier = pubkey();
  const mint = pubkey();
  const source = pubkey();
  u64(); // per charge
  u64(); // period cap
  u64(); // lifetime cap
  const spentInPeriod = u64();
  const spentLifetime = u64();
  i64(); // period start
  i64(); // period seconds
  i64(); // expiry
  const nextNonce = u64();
  const paused = byte() === 1;
  const revoked = byte() === 1;
  const frozen = byte() === 1;
  const policyHash = digest();
  const lastEvidenceHash = digest();
  return {
    version,
    authority,
    merchant: merchantValue,
    executor,
    verifier,
    tokenMint: mint,
    sourceToken: source,
    spentInPeriod,
    spentLifetime,
    nextNonce,
    paused,
    revoked,
    frozen,
    policyHash,
    lastEvidenceHash,
  };
}

async function rawTokenBalance(connection: Connection, account: PublicKey): Promise<bigint> {
  return BigInt((await connection.getTokenAccountBalance(account, 'confirmed')).value.amount);
}

async function main(): Promise<void> {
  const [authority, executor, verifier, report] = await Promise.all([
    loadKeypair('SOLANA_KEYPAIR'),
    loadKeypair('EXECUTOR_KEYPAIR'),
    loadKeypair('VERIFIER_KEYPAIR'),
    readFile(reportPath, 'utf8'),
  ]);
  if (new Set([
    authority.publicKey.toBase58(), executor.publicKey.toBase58(),
    verifier.publicKey.toBase58(), merchant.toBase58(),
  ]).size !== 4) throw new Error('Commercial flow requires four distinct actors');

  const now = new Date();
  const sourceDefinitions = [
    ['Solana Mobile Wallet Adapter', 'https://docs.solanamobile.com/developers/mobile-wallet-adapter'],
    ['Solana Program Derived Addresses', 'https://solana.com/docs/core/pda'],
    ['SPL Token', 'https://spl.solana.com/token'],
  ] as const;
  const delivery: AlphaBriefDelivery = {
    schemaVersion: '1',
    serviceId: 'alphabrief',
    deliveryId: 'ab_solana_mobile_risk_20260910',
    subscriber: authority.publicKey.toBase58(),
    merchant: 'merchant:alphabrief',
    title: 'Solana Mobile Recurring Commerce Risk Brief',
    generatedAt: now.toISOString(),
    content: report,
    sources: sourceDefinitions.map(([label, uri]) => ({ label, uri, capturedAt: now.toISOString() })),
  };
  const verification = verifyAlphaBriefDelivery(delivery, now);
  if (!verification.passed) throw new Error(`AlphaBrief delivery failed verification: ${verification.reasons.join(', ')}`);

  const badDelivery: AlphaBriefDelivery = {
    ...delivery,
    deliveryId: 'ab_bad_output_20260910',
    title: 'Incomplete AlphaBrief output',
    content: 'The tool call returned successfully, but the report body and sources were missing.',
    sources: [],
  };
  const badVerification = verifyAlphaBriefDelivery(badDelivery, now);
  if (badVerification.passed) throw new Error('The intentionally bad output unexpectedly passed verification');

  const connection = new Connection(rpcUrl, 'confirmed');
  const [sourceInfo, merchantInfo] = await Promise.all([
    connection.getParsedAccountInfo(sourceToken, 'confirmed'),
    connection.getParsedAccountInfo(merchantToken, 'confirmed'),
  ]);
  const sourceData = sourceInfo.value?.data;
  const merchantData = merchantInfo.value?.data;
  if (!sourceData || Buffer.isBuffer(sourceData) || !merchantData || Buffer.isBuffer(merchantData)) {
    throw new Error('Expected parsed SPL token accounts');
  }
  const sourceParsed = sourceData.parsed.info as { mint: string; owner: string };
  const merchantParsed = merchantData.parsed.info as { mint: string; owner: string };
  if (sourceParsed.owner !== authority.publicKey.toBase58() || sourceParsed.mint !== tokenMint.toBase58()) {
    throw new Error('Source token account does not match the authority and mint');
  }
  if (merchantParsed.owner !== merchant.toBase58() || merchantParsed.mint !== tokenMint.toBase58()) {
    throw new Error('Merchant token account does not match the merchant and mint');
  }

  const allowance = Keypair.generate();
  const [delegate] = delegatedAuthority(programId, allowance.publicKey);
  const expiresAt = BigInt(Math.floor(now.getTime() / 1_000)) + 30n * 86_400n;
  const policyHashHex = stableHash({
    schemaVersion: '1',
    serviceId: 'alphabrief',
    authority: authority.publicKey.toBase58(),
    merchant: merchant.toBase58(),
    executor: executor.publicKey.toBase58(),
    verifier: verifier.publicKey.toBase58(),
    tokenMint: tokenMint.toBase58(),
    sourceToken: sourceToken.toBase58(),
    perChargeRaw: amountRaw,
    periodCapRaw: 8_000_000n,
    lifetimeCapRaw: 8_000_000n,
    verificationPolicy: alphaBriefVerificationPolicy,
  });
  const rent = await connection.getMinimumBalanceForRentExemption(DELEGATED_STATE_SIZE, 'confirmed');
  const createSignature = await send(
    connection,
    new Transaction().add(
      SystemProgram.createAccount({
        fromPubkey: authority.publicKey,
        newAccountPubkey: allowance.publicKey,
        lamports: rent,
        space: DELEGATED_STATE_SIZE,
        programId,
      }),
      createDelegatedInstruction({
        programId,
        allowance: allowance.publicKey,
        authority: authority.publicKey,
        merchant,
        executor: executor.publicKey,
        verifier: verifier.publicKey,
        tokenMint,
        sourceToken,
        perCharge: amountRaw,
        periodCap: 8_000_000n,
        lifetimeCap: 8_000_000n,
        periodSeconds: 604_800n,
        expiresAt,
        policyHash: Buffer.from(policyHashHex, 'hex'),
      }),
    ),
    authority,
    [allowance],
  );

  const [acceptedRecord] = evidenceRecordAddress(
    programId,
    allowance.publicKey,
    Buffer.from(verification.evidenceHash, 'hex'),
  );
  const sourceBefore = await rawTokenBalance(connection, sourceToken);
  const merchantBefore = await rawTokenBalance(connection, merchantToken);
  const chargeSignature = await send(
    connection,
    new Transaction().add(chargeDelegatedInstruction({
      programId,
      allowance: allowance.publicKey,
      executor: executor.publicKey,
      verifier: verifier.publicKey,
      sourceToken,
      merchantToken,
      amount: amountRaw,
      nonce: 0n,
      evidenceHash: Buffer.from(verification.evidenceHash, 'hex'),
    })),
    executor,
    [verifier],
  );
  const sourceAfterCharge = await rawTokenBalance(connection, sourceToken);
  const merchantAfterCharge = await rawTokenBalance(connection, merchantToken);
  if (sourceBefore - sourceAfterCharge !== amountRaw || merchantAfterCharge - merchantBefore !== amountRaw) {
    throw new Error('AlphaBrief settlement token deltas did not match the service price');
  }
  const acceptedRecordAccount = await connection.getAccountInfo(acceptedRecord, 'confirmed');
  if (!acceptedRecordAccount?.owner.equals(programId)) throw new Error('Accepted delivery evidence record is missing');

  const [frozenRecord] = evidenceRecordAddress(
    programId,
    allowance.publicKey,
    Buffer.from(badVerification.evidenceHash, 'hex'),
  );
  const freezeSourceBefore = await rawTokenBalance(connection, sourceToken);
  const freezeMerchantBefore = await rawTokenBalance(connection, merchantToken);
  const freezeSignature = await send(
    connection,
    new Transaction().add(freezeDelegatedInstruction(
      programId,
      verifier.publicKey,
      allowance.publicKey,
      Buffer.from(badVerification.evidenceHash, 'hex'),
    )),
    verifier,
  );
  const [stateAccount, frozenRecordAccount, freezeSourceAfter, freezeMerchantAfter] = await Promise.all([
    connection.getAccountInfo(allowance.publicKey, 'confirmed'),
    connection.getAccountInfo(frozenRecord, 'confirmed'),
    rawTokenBalance(connection, sourceToken),
    rawTokenBalance(connection, merchantToken),
  ]);
  if (!stateAccount?.owner.equals(programId)) throw new Error('AlphaBrief allowance state is missing');
  if (!frozenRecordAccount?.owner.equals(programId)) throw new Error('Bad-output evidence record is missing');
  const state = parseState(stateAccount.data);
  if (!state.frozen || state.nextNonce !== '1' || state.spentLifetime !== amountRaw.toString()) {
    throw new Error(`Unexpected final AlphaBrief allowance state: ${JSON.stringify(state)}`);
  }
  if (freezeSourceBefore !== freezeSourceAfter || freezeMerchantBefore !== freezeMerchantAfter) {
    throw new Error('FROZEN bad-output path moved tokens');
  }

  console.log(JSON.stringify({
    status: 'ALPHABRIEF_LIVE_COMMERCIAL_FLOW_VERIFIED',
    truthBoundary: 'Real Solana Devnet settlement using a project-created test mint, not canonical USDC or Mainnet.',
    service: {
      id: 'alphabrief',
      priceRaw: amountRaw.toString(),
      reportPath,
      deliveryId: delivery.deliveryId,
      title: delivery.title,
    },
    actors: {
      subscriber: authority.publicKey.toBase58(),
      merchant: merchant.toBase58(),
      executor: executor.publicKey.toBase58(),
      verifier: verifier.publicKey.toBase58(),
    },
    programId: programId.toBase58(),
    allowance: allowance.publicKey.toBase58(),
    delegate: delegate.toBase58(),
    policyHash: policyHashHex,
    acceptedDelivery: {
      verification,
      evidenceRecord: acceptedRecord.toBase58(),
      authoritySignedCharge: false,
      sourceTokenDeltaRaw: `-${amountRaw}`,
      merchantTokenDeltaRaw: `+${amountRaw}`,
    },
    badOutput: {
      verification: badVerification,
      evidenceRecord: frozenRecord.toBase58(),
      tokenMovementRaw: '0',
      finalState: { frozen: state.frozen, nextNonce: state.nextNonce, spentLifetime: state.spentLifetime },
    },
    notifications: [
      { type: 'SERVICE_DELIVERED', title: 'AlphaBrief delivered', body: 'Independent verification passed and settlement completed.' },
      { type: 'ALLOWANCE_FROZEN', title: 'AlphaBrief allowance frozen', body: 'A later bad output failed verification; no payment moved.' },
    ],
    transactions: {
      purchase: { signature: createSignature, explorer: explorer(createSignature) },
      settlement: { signature: chargeSignature, explorer: explorer(chargeSignature) },
      badOutputFreeze: { signature: freezeSignature, explorer: explorer(freezeSignature) },
    },
  }, null, 2));
}

await main();
