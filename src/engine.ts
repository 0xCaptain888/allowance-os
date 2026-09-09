import { stableHash } from './hash.js';
import type { AllowancePolicy, ChargeRequest, CheckResult, Receipt } from './types.js';

export function evaluateCharge(policy: AllowancePolicy, request: ChargeRequest, now = new Date()): CheckResult {
  if (policy.revoked) {
    return {
      state: 'REVOKED',
      reasons: ['allowance_revoked'],
      checks: { allowanceActive: false },
      nextSpentInPeriod: policy.spentInPeriod,
    };
  }
  const checks = {
    allowanceMatches: policy.allowanceId === request.allowanceId,
    allowanceActive: policy.expiresAt > now.toISOString(),
    merchantMatches: policy.merchant === request.merchant,
    tokenMatches: policy.token === request.token,
    perChargeWithinPolicy: request.amount <= policy.perCharge,
    periodCapWithinPolicy: policy.spentInPeriod + request.amount <= policy.periodCap,
    programAllowed: policy.allowedProgram === request.program,
    evidenceBound: request.evidenceHash.length === 64,
  };
  const reasons: string[] = [];
  if (!checks.allowanceMatches) reasons.push('allowance_id_mismatch');
  if (!checks.allowanceActive) reasons.push('allowance_expired');
  if (!checks.merchantMatches) reasons.push('merchant_identity_mismatch');
  if (!checks.tokenMatches) reasons.push('token_mismatch');
  if (!checks.perChargeWithinPolicy) reasons.push('per_charge_limit_exceeded');
  if (!checks.periodCapWithinPolicy) reasons.push('period_cap_exceeded');
  if (!checks.programAllowed) reasons.push('program_not_allowed');
  if (!checks.evidenceBound) reasons.push('evidence_not_bound');

  const passed = Object.values(checks).every(Boolean);
  const state = passed ? 'VERIFIED' : checks.merchantMatches && checks.tokenMatches && checks.allowanceMatches ? 'BLOCKED' : 'FROZEN';
  return {
    state,
    reasons,
    checks,
    nextSpentInPeriod: policy.spentInPeriod + request.amount,
  };
}

export function applyVerifiedCharge(policy: AllowancePolicy, request: ChargeRequest, now = new Date()): AllowancePolicy {
  const result = evaluateCharge(policy, request, now);
  if (result.state !== 'VERIFIED') return policy;
  return { ...policy, spentInPeriod: result.nextSpentInPeriod };
}

export function issueReceipt(policy: AllowancePolicy, request: ChargeRequest, result: CheckResult, now = new Date()): Receipt {
  return {
    receiptVersion: '1',
    allowanceId: policy.allowanceId,
    state: result.state,
    merchant: request.merchant,
    token: request.token,
    amount: request.amount,
    policyHash: stableHash(policy),
    evidenceHash: request.evidenceHash,
    txHash: result.state === 'VERIFIED' ? `simulated:${stableHash({ policy, request }).slice(0, 48)}` : undefined,
    reasons: result.reasons,
    createdAt: now.toISOString(),
  };
}

export function revokeAllowance(policy: AllowancePolicy): AllowancePolicy {
  return { ...policy, expiresAt: new Date(0).toISOString(), revoked: true };
}
