import { stableHash } from './hash.js';
import type { AllowancePolicy, ChargeRequest, CheckResult, EvaluationContext, Receipt } from './types.js';

const HASH_PATTERN = /^[a-f0-9]{64}$/i;
const REQUEST_ID_PATTERN = /^[a-zA-Z0-9][a-zA-Z0-9:_-]{7,127}$/;
const MAX_CLOCK_SKEW_MS = 5 * 60 * 1000;

export function evaluateCharge(
  policy: AllowancePolicy,
  request: ChargeRequest,
  now = new Date(),
  context: EvaluationContext = {},
): CheckResult {
  if (policy.revoked) {
    return {
      state: 'REVOKED',
      reasons: ['allowance_revoked'],
      checks: { allowanceActive: false },
      nextSpentInPeriod: policy.spentInPeriod,
    };
  }
  const requestedAt = Date.parse(request.requestedAt);
  const requestExpiresAt = Date.parse(request.expiresAt);
  const nowMs = now.getTime();
  const checks = {
    requestIdBound: REQUEST_ID_PATTERN.test(request.requestId),
    nonceValid: Number.isSafeInteger(request.nonce) && request.nonce >= 0,
    nonceUnused: !context.usedNonces?.has(request.nonce),
    requestTimeValid: Number.isFinite(requestedAt) && requestedAt <= nowMs + MAX_CLOCK_SKEW_MS,
    requestFresh: Number.isFinite(requestExpiresAt) && requestExpiresAt > nowMs && requestExpiresAt > requestedAt,
    allowanceMatches: policy.allowanceId === request.allowanceId,
    allowanceActive: policy.expiresAt > now.toISOString(),
    merchantMatches: policy.merchant === request.merchant,
    tokenMatches: policy.token === request.token,
    perChargeWithinPolicy: request.amount <= policy.perCharge,
    periodCapWithinPolicy: policy.spentInPeriod + request.amount <= policy.periodCap,
    programAllowed: policy.allowedProgram === request.program,
    evidenceBound: HASH_PATTERN.test(request.evidenceHash) && request.evidenceUri.length > 0,
    evidenceUnused: !context.usedEvidenceHashes?.has(request.evidenceHash.toLowerCase()),
  };
  const reasons: string[] = [];
  if (!checks.requestIdBound) reasons.push('request_id_invalid');
  if (!checks.nonceValid) reasons.push('nonce_invalid');
  if (!checks.nonceUnused) reasons.push('nonce_replayed');
  if (!checks.requestTimeValid) reasons.push('request_time_invalid');
  if (!checks.requestFresh) reasons.push('request_expired');
  if (!checks.allowanceMatches) reasons.push('allowance_id_mismatch');
  if (!checks.allowanceActive) reasons.push('allowance_expired');
  if (!checks.merchantMatches) reasons.push('merchant_identity_mismatch');
  if (!checks.tokenMatches) reasons.push('token_mismatch');
  if (!checks.perChargeWithinPolicy) reasons.push('per_charge_limit_exceeded');
  if (!checks.periodCapWithinPolicy) reasons.push('period_cap_exceeded');
  if (!checks.programAllowed) reasons.push('program_not_allowed');
  if (!checks.evidenceBound) reasons.push('evidence_not_bound');
  if (!checks.evidenceUnused) reasons.push('evidence_replayed');

  const passed = Object.values(checks).every(Boolean);
  const identityAndEvidenceSafe = checks.allowanceMatches
    && checks.merchantMatches
    && checks.tokenMatches
    && checks.programAllowed
    && checks.evidenceBound;
  const state = passed ? 'VERIFIED' : identityAndEvidenceSafe ? 'BLOCKED' : 'FROZEN';
  return {
    state,
    reasons,
    checks,
    nextSpentInPeriod: passed ? policy.spentInPeriod + request.amount : policy.spentInPeriod,
  };
}

export function applyVerifiedCharge(policy: AllowancePolicy, request: ChargeRequest, now = new Date()): AllowancePolicy {
  const result = evaluateCharge(policy, request, now);
  if (result.state !== 'VERIFIED') return policy;
  return { ...policy, spentInPeriod: result.nextSpentInPeriod };
}

export function issueReceipt(policy: AllowancePolicy, request: ChargeRequest, result: CheckResult, now = new Date()): Receipt {
  return {
    receiptVersion: '2',
    requestId: request.requestId,
    nonce: request.nonce,
    allowanceId: policy.allowanceId,
    state: result.state,
    merchant: request.merchant,
    token: request.token,
    amount: request.amount,
    policyHash: stableHash(policy),
    evidenceHash: request.evidenceHash,
    evidenceUri: request.evidenceUri,
    evidenceType: request.evidenceType,
    txHash: result.state === 'VERIFIED' ? `simulated:${stableHash({ policy, request }).slice(0, 48)}` : undefined,
    reasons: result.reasons,
    checks: result.checks,
    createdAt: now.toISOString(),
  };
}

export function revokeAllowance(policy: AllowancePolicy): AllowancePolicy {
  return { ...policy, expiresAt: new Date(0).toISOString(), revoked: true };
}
