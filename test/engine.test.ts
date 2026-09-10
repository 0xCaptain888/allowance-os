import test from 'node:test';
import assert from 'node:assert/strict';
import { applyVerifiedCharge, evaluateCharge, revokeAllowance } from '../src/engine.js';
import { chargeAllowanceInstruction, policyHash } from '../src/protocol.js';
import { capabilitySummary, standardAndroidProfile } from '../src/device-profile.js';
import { AllowanceOS } from '../src/sdk.js';
import { verifyWebhookSignature } from '../src/webhook.js';
import { SolanaDevnetAdapter } from '../src/adapter.js';
import {
  chargeDelegatedInstruction,
  createDelegatedInstruction,
  delegatedAuthority,
  evidenceRecordAddress,
  freezeDelegatedInstruction,
  pauseDelegatedInstruction,
  revokeDelegatedInstruction,
  rotateExecutorInstruction,
  rotateVerifierInstruction,
  unfreezeDelegatedInstruction,
  unpauseDelegatedInstruction,
} from '../src/delegated-protocol.js';
import { PublicKey, SystemProgram } from '@solana/web3.js';
import { canonicalJson, stableHash } from '../src/hash.js';
import type { AllowancePolicy, ChargeRequest } from '../src/types.js';

const policy: AllowancePolicy = {
  allowanceId: 'test-1', subscriber: 'seeker:alice.skr', merchant: 'merchant:researchpulse', merchantName: 'ResearchPulse',
  token: 'USDC', tokenMint: 'mint:usdc-test', tokenDecimals: 6,
  perChargeRaw: '2000000', period: 'weekly', periodCapRaw: '8000000', spentInPeriodRaw: '0',
  expiresAt: '2026-10-09T00:00:00.000Z', allowedProgram: 'program:allowance-os-devnet', appReleaseHash: 'release:test',
};
const request = (overrides: Partial<ChargeRequest> = {}): ChargeRequest => ({
  requestId: 'req_alphabrief_0001', nonce: 1, allowanceId: policy.allowanceId, merchant: policy.merchant,
  token: 'USDC', tokenMint: policy.tokenMint, amountRaw: '2000000', program: policy.allowedProgram, requestedAt: '2026-09-09T00:00:00.000Z',
  expiresAt: '2026-09-09T00:05:00.000Z', evidenceHash: 'a'.repeat(64),
  evidenceUri: 'ipfs://alphabrief/report-001', evidenceType: 'content-delivery', ...overrides,
});

test('verified charge passes every policy check', () => assert.equal(evaluateCharge(policy, request(), new Date('2026-09-09T00:00:00.000Z')).state, 'VERIFIED'));
test('over-cap charge is blocked', () => assert.equal(evaluateCharge(policy, request({ amountRaw: '10000000' }), new Date('2026-09-09T00:00:00.000Z')).state, 'BLOCKED'));
test('zero, negative, decimal, malformed, and u64-overflow charges are blocked', () => {
  for (const amountRaw of ['0', '-1', '1.5', '1e6', '', '18446744073709551616']) {
    assert.equal(evaluateCharge(policy, request({ amountRaw }), new Date('2026-09-09T00:00:00.000Z')).state, 'BLOCKED');
  }
});
test('invalid policy budget freezes instead of authorizing', () => {
  assert.equal(evaluateCharge({ ...policy, periodCapRaw: '1000000' }, request(), new Date('2026-09-09T00:00:00.000Z')).state, 'FROZEN');
});
test('invalid token metadata freezes the policy', () => {
  assert.equal(evaluateCharge({ ...policy, tokenDecimals: 1.5 }, request(), new Date('2026-09-09T00:00:00.000Z')).state, 'FROZEN');
  assert.equal(evaluateCharge({ ...policy, tokenMint: '' }, request(), new Date('2026-09-09T00:00:00.000Z')).state, 'FROZEN');
});
test('mint mismatch freezes even when the display symbol matches', () => {
  const result = evaluateCharge(policy, request({ tokenMint: 'mint:lookalike' }), new Date('2026-09-09T00:00:00.000Z'));
  assert.equal(result.state, 'FROZEN');
  assert.ok(result.reasons.includes('token_mint_mismatch'));
});
test('merchant mismatch freezes the allowance', () => assert.equal(evaluateCharge(policy, request({ merchant: 'merchant:lookalike' }), new Date('2026-09-09T00:00:00.000Z')).state, 'FROZEN'));
test('allowance ID mismatch freezes the request', () => assert.equal(evaluateCharge(policy, request({ allowanceId: 'other-allowance' }), new Date('2026-09-09T00:00:00.000Z')).state, 'FROZEN'));
test('verified charge advances raw period spend without floating point', () => assert.equal(applyVerifiedCharge(policy, request(), new Date('2026-09-09T00:00:00.000Z')).spentInPeriodRaw, '2000000'));
test('charge instruction carries the policy hash', () => {
  const instruction = chargeAllowanceInstruction(policy, request());
  assert.equal(instruction.kind, 'chargeAllowance');
  if (instruction.kind === 'chargeAllowance') assert.equal(instruction.policyHash, policyHash(policy));
});
test('canonical hashes include nested values and ignore object key order', () => {
  const left = { policy: { merchant: 'merchant:a', limits: { period: '8000000', perCharge: '2000000' } } };
  const reordered = { policy: { limits: { perCharge: '2000000', period: '8000000' }, merchant: 'merchant:a' } };
  const changed = { policy: { merchant: 'merchant:b', limits: { period: '8000000', perCharge: '2000000' } } };
  assert.equal(canonicalJson(left), canonicalJson(reordered));
  assert.equal(stableHash(left), stableHash(reordered));
  assert.notEqual(stableHash(left), stableHash(changed));
});
test('standard Android can use MWA without claiming Seed Vault', () => {
  assert.deepEqual(capabilitySummary(standardAndroidProfile), {
    walletAuthorization: true,
    seedVault: 'SEEKER_ONLY',
    seekerGenesis: 'NOT_AVAILABLE',
    liveAllowance: 'AVAILABLE',
  });
});
test('revoke makes an allowance immediately inactive', () => {
  const revoked = revokeAllowance(policy);
  assert.equal(revoked.expiresAt, '1970-01-01T00:00:00.000Z');
  assert.equal(revoked.revoked, true);
  assert.equal(evaluateCharge(revoked, request(), new Date('2026-09-09T00:00:00.000Z')).state, 'REVOKED');
});
test('expired requests are blocked and never advance period spend', () => {
  const result = evaluateCharge(policy, request({ expiresAt: '2026-09-08T23:59:59.000Z' }), new Date('2026-09-09T00:00:00.000Z'));
  assert.equal(result.state, 'BLOCKED');
  assert.equal(result.nextSpentInPeriodRaw, '0');
  assert.ok(result.reasons.includes('request_expired'));
});
test('replayed evidence is blocked', () => {
  const result = evaluateCharge(policy, request(), new Date('2026-09-09T00:00:00.000Z'), { usedEvidenceHashes: new Set(['a'.repeat(64)]) });
  assert.equal(result.state, 'BLOCKED');
  assert.ok(result.reasons.includes('evidence_replayed'));
});
test('SDK is idempotent for the same request and does not double-spend', () => {
  const sdk = new AllowanceOS('test-webhook-secret-32-characters');
  sdk.createAllowance(policy);
  const first = sdk.requestCharge(request(), new Date('2026-09-09T00:00:00.000Z'));
  const retry = sdk.requestCharge(request(), new Date('2026-09-09T00:00:01.000Z'));
  assert.equal(first.receipt.state, 'VERIFIED');
  assert.equal(retry.receipt.idempotentReplay, true);
  assert.equal(sdk.revokeAllowance(policy.allowanceId).spentInPeriodRaw, '2000000');
  assert.equal(verifyWebhookSignature(first.webhook, first.webhookSignature, 'test-webhook-secret-32-characters'), true);
});
test('a new request cannot reuse delivered evidence', () => {
  const sdk = new AllowanceOS('test-webhook-secret-32-characters');
  sdk.createAllowance(policy);
  sdk.requestCharge(request(), new Date('2026-09-09T00:00:00.000Z'));
  const replay = sdk.requestCharge(request({ requestId: 'req_alphabrief_0002', nonce: 2 }), new Date('2026-09-09T00:00:01.000Z'));
  assert.equal(replay.receipt.state, 'BLOCKED');
  assert.ok(replay.receipt.reasons.includes('evidence_replayed'));
});
test('signed webhooks fail closed after payload tampering', () => {
  const sdk = new AllowanceOS('test-webhook-secret-32-characters');
  sdk.createAllowance(policy);
  const completed = sdk.requestCharge(request(), new Date('2026-09-09T00:00:00.000Z'));
  const tampered = { ...completed.webhook, eventId: 'evt_tampered' };
  assert.equal(verifyWebhookSignature(tampered, completed.webhookSignature, 'test-webhook-secret-32-characters'), false);
});
test('live adapter delegates signing without accepting wallet secrets', async () => {
  const sent: unknown[] = [];
  const adapter = new SolanaDevnetAdapter({ send: async instruction => { sent.push(instruction); return { signature: `devnet:${sent.length}` }; } });
  const authorization = await adapter.authorizeAllowance(policy);
  const revoked = await adapter.revokeAllowance(policy);
  assert.equal(authorization.authorizationId, 'devnet:1');
  assert.equal(revoked.state, 'REVOKED');
  assert.equal(sent.length, 2);
});

test('delegated v2 builders preserve signer separation and stable discriminants', () => {
  const programId = new PublicKey('DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE');
  const allowance = PublicKey.unique();
  const authority = PublicKey.unique();
  const merchant = PublicKey.unique();
  const executor = PublicKey.unique();
  const verifier = PublicKey.unique();
  const tokenMint = PublicKey.unique();
  const sourceToken = PublicKey.unique();
  const merchantToken = PublicKey.unique();
  const policyHash = new Uint8Array(32).fill(3);
  const evidenceHash = new Uint8Array(32).fill(7);
  const [delegate] = delegatedAuthority(programId, allowance);
  const [evidenceRecord] = evidenceRecordAddress(programId, allowance, evidenceHash);

  const create = createDelegatedInstruction({
    programId,
    allowance,
    authority,
    merchant,
    executor,
    verifier,
    tokenMint,
    sourceToken,
    perCharge: 2_000_000n,
    periodCap: 8_000_000n,
    lifetimeCap: 24_000_000n,
    periodSeconds: 604_800n,
    expiresAt: 2_000_000_000n,
    policyHash,
  });
  assert.equal(create.data[0], 3);
  assert.equal(create.data.length, 233);
  assert.equal(create.keys[0].pubkey.equals(authority), true);
  assert.equal(create.keys[0].isSigner, true);
  assert.equal(create.keys[3].pubkey.equals(delegate), true);

  const charge = chargeDelegatedInstruction({
    programId,
    allowance,
    executor,
    verifier,
    sourceToken,
    merchantToken,
    amount: 1_000_000n,
    nonce: 4n,
    evidenceHash,
  });
  assert.equal(charge.data[0], 4);
  assert.equal(charge.data.length, 49);
  assert.equal(charge.keys[0].pubkey.equals(executor), true);
  assert.equal(charge.keys[0].isSigner, true);
  assert.equal(charge.keys[0].isWritable, true);
  assert.equal(charge.keys[1].pubkey.equals(verifier), true);
  assert.equal(charge.keys[1].isSigner, true);
  assert.equal(charge.keys.some(key => key.pubkey.equals(authority)), false);
  assert.equal(charge.keys[7].pubkey.equals(evidenceRecord), true);
  assert.equal(charge.keys[7].isWritable, true);
  assert.equal(charge.keys[8].pubkey.equals(SystemProgram.programId), true);

  assert.equal(pauseDelegatedInstruction(programId, authority, allowance).data[0], 5);
  assert.equal(unpauseDelegatedInstruction(programId, authority, allowance).data[0], 6);
  const freeze = freezeDelegatedInstruction(programId, verifier, allowance, evidenceHash);
  assert.equal(freeze.data[0], 7);
  assert.equal(freeze.data.length, 33);
  assert.equal(unfreezeDelegatedInstruction({ programId, authority, verifier, allowance }).data[0], 8);
  assert.equal(revokeDelegatedInstruction({ programId, authority, allowance, sourceToken }).data[0], 9);
  const newExecutor = PublicKey.unique();
  const newVerifier = PublicKey.unique();
  const rotateExecutor = rotateExecutorInstruction({ programId, authority, allowance, newExecutor });
  const rotateVerifier = rotateVerifierInstruction({ programId, authority, currentVerifier: verifier, allowance, newVerifier });
  assert.equal(rotateExecutor.data[0], 10);
  assert.equal(new PublicKey(rotateExecutor.data.subarray(1)).equals(newExecutor), true);
  assert.equal(rotateVerifier.data[0], 11);
  assert.equal(rotateVerifier.keys[1].pubkey.equals(verifier), true);
  assert.equal(rotateVerifier.keys[1].isSigner, true);
  assert.equal(new PublicKey(rotateVerifier.data.subarray(1)).equals(newVerifier), true);
});

test('evidence record PDAs bind allowance and complete evidence hash', () => {
  const programId = PublicKey.unique();
  const allowance = PublicKey.unique();
  const firstHash = new Uint8Array(32).fill(1);
  const secondHash = new Uint8Array(32).fill(2);
  const [first] = evidenceRecordAddress(programId, allowance, firstHash);
  const [same] = evidenceRecordAddress(programId, allowance, firstHash);
  const [differentEvidence] = evidenceRecordAddress(programId, allowance, secondHash);
  const [differentAllowance] = evidenceRecordAddress(programId, PublicKey.unique(), firstHash);
  assert.equal(first.equals(same), true);
  assert.equal(first.equals(differentEvidence), false);
  assert.equal(first.equals(differentAllowance), false);
});

test('delegated v2 builders reject malformed hashes and integer ranges', () => {
  const programId = PublicKey.unique();
  const allowance = PublicKey.unique();
  assert.throws(() => chargeDelegatedInstruction({
    programId,
    allowance,
    executor: PublicKey.unique(),
    verifier: PublicKey.unique(),
    sourceToken: PublicKey.unique(),
    merchantToken: PublicKey.unique(),
    amount: -1n,
    nonce: 0n,
    evidenceHash: new Uint8Array(32),
  }), /u64 out of range/);
  assert.throws(() => chargeDelegatedInstruction({
    programId,
    allowance,
    executor: PublicKey.unique(),
    verifier: PublicKey.unique(),
    sourceToken: PublicKey.unique(),
    merchantToken: PublicKey.unique(),
    amount: 1n,
    nonce: 0n,
    evidenceHash: new Uint8Array(31),
  }), /exactly 32 bytes/);
});
