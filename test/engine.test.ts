import test from 'node:test';
import assert from 'node:assert/strict';
import { applyVerifiedCharge, evaluateCharge, revokeAllowance } from '../src/engine.js';
import { chargeAllowanceInstruction, policyHash } from '../src/protocol.js';
import { capabilitySummary, standardAndroidProfile } from '../src/device-profile.js';
import { AllowanceOS } from '../src/sdk.js';
import { verifyWebhookSignature } from '../src/webhook.js';
import { SolanaDevnetAdapter } from '../src/adapter.js';
import type { AllowancePolicy, ChargeRequest } from '../src/types.js';

const policy: AllowancePolicy = {
  allowanceId: 'test-1', subscriber: 'seeker:alice.skr', merchant: 'merchant:researchpulse', merchantName: 'ResearchPulse',
  token: 'USDC', perCharge: 2, period: 'weekly', periodCap: 8, spentInPeriod: 0,
  expiresAt: '2026-10-09T00:00:00.000Z', allowedProgram: 'program:allowance-os-devnet', appReleaseHash: 'release:test',
};
const request = (overrides: Partial<ChargeRequest> = {}): ChargeRequest => ({
  requestId: 'req_alphabrief_0001', nonce: 1, allowanceId: policy.allowanceId, merchant: policy.merchant,
  token: 'USDC', amount: 2, program: policy.allowedProgram, requestedAt: '2026-09-09T00:00:00.000Z',
  expiresAt: '2026-09-09T00:05:00.000Z', evidenceHash: 'a'.repeat(64),
  evidenceUri: 'ipfs://alphabrief/report-001', evidenceType: 'content-delivery', ...overrides,
});

test('verified charge passes every policy check', () => assert.equal(evaluateCharge(policy, request(), new Date('2026-09-09T00:00:00.000Z')).state, 'VERIFIED'));
test('over-cap charge is blocked', () => assert.equal(evaluateCharge(policy, request({ amount: 10 }), new Date('2026-09-09T00:00:00.000Z')).state, 'BLOCKED'));
test('zero, negative, and non-finite charges are blocked', () => {
  for (const amount of [0, -1, Number.NaN, Number.POSITIVE_INFINITY]) {
    assert.equal(evaluateCharge(policy, request({ amount }), new Date('2026-09-09T00:00:00.000Z')).state, 'BLOCKED');
  }
});
test('invalid policy budget freezes instead of authorizing', () => {
  assert.equal(evaluateCharge({ ...policy, periodCap: 1 }, request(), new Date('2026-09-09T00:00:00.000Z')).state, 'FROZEN');
});
test('merchant mismatch freezes the allowance', () => assert.equal(evaluateCharge(policy, request({ merchant: 'merchant:lookalike' }), new Date('2026-09-09T00:00:00.000Z')).state, 'FROZEN'));
test('allowance ID mismatch freezes the request', () => assert.equal(evaluateCharge(policy, request({ allowanceId: 'other-allowance' }), new Date('2026-09-09T00:00:00.000Z')).state, 'FROZEN'));
test('verified charge advances the period spend', () => assert.equal(applyVerifiedCharge(policy, request(), new Date('2026-09-09T00:00:00.000Z')).spentInPeriod, 2));
test('charge instruction carries the policy hash', () => {
  const instruction = chargeAllowanceInstruction(policy, request());
  assert.equal(instruction.kind, 'chargeAllowance');
  if (instruction.kind === 'chargeAllowance') assert.equal(instruction.policyHash, policyHash(policy));
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
  assert.equal(result.nextSpentInPeriod, 0);
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
  assert.equal(sdk.revokeAllowance(policy.allowanceId).spentInPeriod, 2);
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
