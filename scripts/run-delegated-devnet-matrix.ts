import { Connection, Keypair, PublicKey, Transaction, type Signer, type SignatureStatus } from '@solana/web3.js';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { ProxyAgent, setGlobalDispatcher } from 'undici';
import {
  chargeDelegatedInstruction,
  DELEGATED_STATE_SIZE,
  evidenceRecordAddress,
  freezeDelegatedInstruction,
  pauseDelegatedInstruction,
  revokeDelegatedInstruction,
  rotateExecutorInstruction,
  rotateVerifierInstruction,
  unfreezeDelegatedInstruction,
  unpauseDelegatedInstruction,
} from '../src/delegated-protocol.js';

const proxyUrl = process.env.HTTPS_PROXY ?? process.env.HTTP_PROXY;
if (proxyUrl) setGlobalDispatcher(new ProxyAgent(proxyUrl));

const rpcUrl = process.env.SOLANA_RPC_URL ?? 'https://api.devnet.solana.com';
const programId = new PublicKey(required('DELEGATED_PROGRAM_ID'));
const allowance = new PublicKey(required('DELEGATED_ALLOWANCE'));
const sourceToken = new PublicKey(required('SOURCE_TOKEN_ACCOUNT'));
const merchantToken = new PublicKey(required('MERCHANT_TOKEN_ACCOUNT'));

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

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`${name} is required`);
  return value;
}

async function loadKeypair(name: string): Promise<Keypair> {
  const bytes = JSON.parse(await readFile(required(name), 'utf8')) as number[];
  return Keypair.fromSecretKey(Uint8Array.from(bytes));
}

function digest(label: string): Buffer {
  return createHash('sha256').update(label).digest();
}

function explorer(signature: string): string {
  return `https://explorer.solana.com/tx/${signature}?cluster=devnet`;
}

async function pollStatus(
  connection: Connection,
  signature: string,
  lastValidBlockHeight: number,
): Promise<SignatureStatus> {
  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    const response = await connection.getSignatureStatuses([signature], { searchTransactionHistory: true });
    const status = response.value[0];
    if (status && (status.err || status.confirmationStatus === 'confirmed' || status.confirmationStatus === 'finalized')) {
      return status;
    }
    if (await connection.getBlockHeight('confirmed') > lastValidBlockHeight) {
      throw new Error(`Transaction ${signature} was not found before blockhash expiry`);
    }
    await new Promise(resolve => setTimeout(resolve, 1_000));
  }
  throw new Error(`Timed out while polling transaction ${signature}`);
}

async function broadcast(
  connection: Connection,
  transaction: Transaction,
  feePayer: Keypair,
  signers: Signer[],
  expectFailure = false,
): Promise<{ signature: string; error: unknown }> {
  const latest = await connection.getLatestBlockhash('confirmed');
  transaction.feePayer = feePayer.publicKey;
  transaction.recentBlockhash = latest.blockhash;
  transaction.sign(feePayer, ...signers.filter(signer => !signer.publicKey.equals(feePayer.publicKey)));
  const signature = await connection.sendRawTransaction(transaction.serialize(), {
    skipPreflight: expectFailure,
    maxRetries: 5,
  });
  const status = await pollStatus(connection, signature, latest.lastValidBlockHeight);
  if (expectFailure && !status.err) throw new Error(`Transaction ${signature} unexpectedly succeeded`);
  if (!expectFailure && status.err) throw new Error(`Transaction ${signature} failed: ${JSON.stringify(status.err)}`);
  return { signature, error: status.err };
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
  const hash = () => {
    const value = data.subarray(offset, offset + 32).toString('hex');
    offset += 32;
    return value;
  };
  return {
    version: byte(), authority: pubkey(), merchant: pubkey(), executor: pubkey(), verifier: pubkey(),
    tokenMint: pubkey(), sourceToken: pubkey(), perCharge: u64(), periodCap: u64(), lifetimeCap: u64(),
    spentInPeriod: u64(), spentLifetime: u64(), periodStartedAt: i64(), periodSeconds: i64(), expiresAt: i64(),
    nextNonce: u64(), paused: byte() === 1, revoked: byte() === 1, frozen: byte() === 1,
    policyHash: hash(), lastEvidenceHash: hash(),
  };
}

async function readState(connection: Connection): Promise<DelegatedSnapshot> {
  const account = await connection.getAccountInfo(allowance, 'confirmed');
  if (!account || !account.owner.equals(programId)) throw new Error('Delegated allowance account was not found');
  return parseState(account.data);
}

async function rawBalance(connection: Connection, account: PublicKey): Promise<bigint> {
  return BigInt((await connection.getTokenAccountBalance(account, 'confirmed')).value.amount);
}

async function main(): Promise<void> {
  const [authority, executor, verifier, newExecutor, newVerifier] = await Promise.all([
    loadKeypair('SOLANA_KEYPAIR'),
    loadKeypair('EXECUTOR_KEYPAIR'),
    loadKeypair('VERIFIER_KEYPAIR'),
    loadKeypair('NEW_EXECUTOR_KEYPAIR'),
    loadKeypair('NEW_VERIFIER_KEYPAIR'),
  ]);
  const actors = [authority, executor, verifier, newExecutor, newVerifier].map(actor => actor.publicKey.toBase58());
  if (new Set(actors).size !== actors.length) throw new Error('All signer keypairs must be distinct');

  const connection = new Connection(rpcUrl, 'confirmed');
  const initial = await readState(connection);
  if (initial.version !== 2 || initial.paused || initial.revoked || initial.frozen) {
    throw new Error('Expected an active version-2 allowance');
  }
  if (
    initial.authority !== authority.publicKey.toBase58()
    || initial.executor !== executor.publicKey.toBase58()
    || initial.verifier !== verifier.publicKey.toBase58()
    || initial.sourceToken !== sourceToken.toBase58()
  ) throw new Error('Current onchain actors or source token do not match the supplied keys');

  const nonce = BigInt(initial.nextNonce);
  const sourceBeforeBlocked = await rawBalance(connection, sourceToken);
  const merchantBeforeBlocked = await rawBalance(connection, merchantToken);
  const blockedEvidence = digest(`ALLOWANCE_OS|DELEGATED_V2|BLOCKED_OVER_CAP|${allowance.toBase58()}|${nonce}`);
  const [blockedRecord] = evidenceRecordAddress(programId, allowance, blockedEvidence);
  const blocked = await broadcast(
    connection,
    new Transaction().add(chargeDelegatedInstruction({
      programId, allowance, executor: executor.publicKey, verifier: verifier.publicKey,
      sourceToken, merchantToken, amount: BigInt(initial.perCharge) + 1n, nonce, evidenceHash: blockedEvidence,
    })),
    executor,
    [verifier],
    true,
  );
  const sourceAfterBlocked = await rawBalance(connection, sourceToken);
  const merchantAfterBlocked = await rawBalance(connection, merchantToken);
  if (sourceAfterBlocked !== sourceBeforeBlocked || merchantAfterBlocked !== merchantBeforeBlocked) {
    throw new Error('BLOCKED transaction changed token balances');
  }
  if (await connection.getAccountInfo(blockedRecord, 'confirmed')) {
    throw new Error('BLOCKED transaction unexpectedly created an evidence record');
  }

  const paused = await broadcast(
    connection,
    new Transaction().add(pauseDelegatedInstruction(programId, authority.publicKey, allowance)),
    authority,
    [],
  );
  if (!(await readState(connection)).paused) throw new Error('Pause did not persist');

  const unpaused = await broadcast(
    connection,
    new Transaction().add(unpauseDelegatedInstruction(programId, authority.publicKey, allowance)),
    authority,
    [],
  );
  if ((await readState(connection)).paused) throw new Error('Unpause did not persist');

  const freezeEvidence = digest(`ALLOWANCE_OS|DELEGATED_V2|FROZEN_BAD_OUTPUT|${allowance.toBase58()}|${nonce}`);
  const [freezeRecord] = evidenceRecordAddress(programId, allowance, freezeEvidence);
  const frozen = await broadcast(
    connection,
    new Transaction().add(freezeDelegatedInstruction(programId, verifier.publicKey, allowance, freezeEvidence)),
    verifier,
    [],
  );
  if (!(await readState(connection)).frozen) throw new Error('Freeze did not persist');
  const freezeRecordAccount = await connection.getAccountInfo(freezeRecord, 'confirmed');
  if (!freezeRecordAccount || !freezeRecordAccount.owner.equals(programId)) {
    throw new Error('Freeze evidence record was not created');
  }

  const unfrozen = await broadcast(
    connection,
    new Transaction().add(unfreezeDelegatedInstruction({
      programId, authority: authority.publicKey, verifier: verifier.publicKey, allowance,
    })),
    authority,
    [verifier],
  );
  if ((await readState(connection)).frozen) throw new Error('Unfreeze did not persist');

  const executorRotated = await broadcast(
    connection,
    new Transaction().add(rotateExecutorInstruction({
      programId, authority: authority.publicKey, allowance, newExecutor: newExecutor.publicKey,
    })),
    authority,
    [],
  );
  const verifierRotated = await broadcast(
    connection,
    new Transaction().add(rotateVerifierInstruction({
      programId, authority: authority.publicKey, currentVerifier: verifier.publicKey,
      allowance, newVerifier: newVerifier.publicKey,
    })),
    authority,
    [verifier],
  );
  const rotatedState = await readState(connection);
  if (
    rotatedState.executor !== newExecutor.publicKey.toBase58()
    || rotatedState.verifier !== newVerifier.publicKey.toBase58()
  ) throw new Error('Role rotation did not persist');

  const rotatedNonce = BigInt(rotatedState.nextNonce);
  const rotatedEvidence = digest(`ALLOWANCE_OS|DELEGATED_V2|ROTATED_VERIFIED|${allowance.toBase58()}|${rotatedNonce}`);
  const [rotatedRecord] = evidenceRecordAddress(programId, allowance, rotatedEvidence);
  const sourceBeforeRotated = await rawBalance(connection, sourceToken);
  const merchantBeforeRotated = await rawBalance(connection, merchantToken);
  const rotatedCharge = await broadcast(
    connection,
    new Transaction().add(chargeDelegatedInstruction({
      programId, allowance, executor: newExecutor.publicKey, verifier: newVerifier.publicKey,
      sourceToken, merchantToken, amount: 1_000_000n, nonce: rotatedNonce, evidenceHash: rotatedEvidence,
    })),
    newExecutor,
    [newVerifier],
  );
  const sourceAfterRotated = await rawBalance(connection, sourceToken);
  const merchantAfterRotated = await rawBalance(connection, merchantToken);
  if (sourceBeforeRotated - sourceAfterRotated !== 1_000_000n || merchantAfterRotated - merchantBeforeRotated !== 1_000_000n) {
    throw new Error('Rotated-role charge did not transfer exactly 1,000,000 raw units');
  }
  if (!(await connection.getAccountInfo(rotatedRecord, 'confirmed'))?.owner.equals(programId)) {
    throw new Error('Rotated-role charge evidence record was not created');
  }

  const revoked = await broadcast(
    connection,
    new Transaction().add(revokeDelegatedInstruction({
      programId, authority: authority.publicKey, allowance, sourceToken,
    })),
    authority,
    [],
  );
  const finalState = await readState(connection);
  if (!finalState.revoked || !finalState.paused) throw new Error('Revoke did not persist terminal state');
  const sourceAccount = await connection.getParsedAccountInfo(sourceToken, 'confirmed');
  const parsed = sourceAccount.value?.data;
  if (!parsed || Buffer.isBuffer(parsed)) throw new Error('Unable to parse source token account after revoke');
  const sourceInfo = parsed.parsed.info as { delegate?: string; delegatedAmount?: { amount: string } };
  if (sourceInfo.delegate || (sourceInfo.delegatedAmount && sourceInfo.delegatedAmount.amount !== '0')) {
    throw new Error('SPL delegate remains after revoke');
  }

  const transaction = (entry: { signature: string; error: unknown }) => ({
    signature: entry.signature,
    explorer: explorer(entry.signature),
    error: entry.error,
  });
  console.log(JSON.stringify({
    status: 'DELEGATED_V2_CONTROL_MATRIX_VERIFIED',
    programId: programId.toBase58(),
    allowance: allowance.toBase58(),
    roles: {
      authority: authority.publicKey.toBase58(),
      originalExecutor: executor.publicKey.toBase58(),
      originalVerifier: verifier.publicKey.toBase58(),
      rotatedExecutor: newExecutor.publicKey.toBase58(),
      rotatedVerifier: newVerifier.publicKey.toBase58(),
    },
    checks: {
      blockedMovedZeroTokens: true,
      blockedCreatedNoEvidenceRecord: true,
      pausePersisted: true,
      unpausePersisted: true,
      freezePersistedEvidenceRecord: true,
      dualSignatureUnfreeze: true,
      roleRotationPersisted: true,
      rotatedActorsSettledWithoutAuthority: true,
      rotatedChargeRaw: '1000000',
      revokeRemovedSplDelegate: true,
    },
    evidence: {
      blockedHash: blockedEvidence.toString('hex'),
      freezeHash: freezeEvidence.toString('hex'),
      freezeRecord: freezeRecord.toBase58(),
      rotatedChargeHash: rotatedEvidence.toString('hex'),
      rotatedChargeRecord: rotatedRecord.toBase58(),
    },
    transactions: {
      blocked: transaction(blocked),
      pause: transaction(paused),
      unpause: transaction(unpaused),
      freeze: transaction(frozen),
      unfreeze: transaction(unfrozen),
      rotateExecutor: transaction(executorRotated),
      rotateVerifier: transaction(verifierRotated),
      rotatedCharge: transaction(rotatedCharge),
      revoke: transaction(revoked),
    },
    finalState,
  }, null, 2));
}

await main();
