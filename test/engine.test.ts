import test from 'node:test';
import assert from 'node:assert/strict';
import { applyVerifiedCharge, evaluateCharge, revokeAllowance } from '../src/engine.js';
import { chargeAllowanceInstruction, policyHash } from '../src/protocol.js';
import { capabilitySummary, standardAndroidProfile } from '../src/device-profile.js';
import type { AllowancePolicy, ChargeRequest } from '../src/types.js';

const policy: AllowancePolicy = {
  allowanceId: 'test-1', subscriber: 'seeker:alice.skr', merchant: 'merchant:researchpulse', merchantName: 'ResearchPulse',
  token: 'USDC', perCharge: 2, period: 'weekly', periodCap: 8, spentInPeriod: 0,
  expiresAt: '2026-10-09T00:00:00.000Z', allowedProgram: 'program:allowance-os-devnet', appReleaseHash: 'release:test',
};
const request = (overrides: Partial<ChargeRequest> = {}): ChargeRequest => ({ allowanceId: policy.allowanceId, merchant: policy.merchant, token: 'USDC', amount: 2, program: policy.allowedProgram, requestedAt: '2026-09-09T00:00:00.000Z', evidenceHash: 'a'.repeat(64), ...overrides });

test('verified charge passes every policy check', () => assert.equal(evaluateCharge(policy, request(), new Date('2026-09-09T00:00:00.000Z')).state, 'VERIFIED'));
test('over-cap charge is blocked', () => assert.equal(evaluateCharge(policy, request({ amount: 10 }), new Date('2026-09-09T00:00:00.000Z')).state, 'BLOCKED'));
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
