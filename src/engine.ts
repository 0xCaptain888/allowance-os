import { stableHash } from './hash.js';
import type { AllowancePolicy, ChargeRequest, CheckResult, EvaluationContext, Receipt } from './types.js';

const HASH_PATTERN = /^[a-f0-9]{64}$/i;
const REQUEST_ID_PATTERN = /^[a-zA-Z0-9][a-zA-Z0-9:_-]{7,127}$/;
const MAX_CLOCK_SKEW_MS = 5 * 60 * 1000;
const RAW_AMOUNT_PATTERN = /^(0|[1-9][0-9]*)$/;
const U64_MAX = (1n << 64n) - 1n;

function rawAmount(value: string): bigint | null {
  if (!RAW_AMOUNT_PATTERN.test(value)) return null;
  const parsed = BigInt(value);
  return parsed <= U64_MAX ? parsed : null;
}

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
      nextSpentInPeriodRaw: policy.spentInPeriodRaw,
    };
  }
  const perCharge = rawAmount(policy.perChargeRaw);
  const periodCap = rawAmount(policy.periodCapRaw);
  const spentInPeriod = rawAmount(policy.spentInPeriodRaw);
  const validPolicyBudget = perCharge !== null
    && perCharge > 0n
    && periodCap !== null
    && periodCap >= perCharge
    && spentInPeriod !== null
    && spentInPeriod <= periodCap;
  const validTokenMetadata = policy.tokenMint.trim().length > 0
    && Number.isInteger(policy.tokenDecimals)
    && policy.tokenDecimals >= 0
    && policy.tokenDecimals <= 18;
  if (!validPolicyBudget || !validTokenMetadata) {
    return {
      state: 'FROZEN',
      reasons: [!validPolicyBudget ? 'invalid_policy_budget' : 'invalid_token_metadata'],
      checks: { policyBudgetValid: validPolicyBudget, tokenMetadataValid: validTokenMetadata },
      nextSpentInPeriodRaw: policy.spentInPeriodRaw,
    };
  }
  const amount = rawAmount(request.amountRaw);
  if (amount === null || amount <= 0n) {
    return {
      state: 'BLOCKED',
      reasons: ['amount_invalid'],
      checks: { rawAmountValid: false },
      nextSpentInPeriodRaw: policy.spentInPeriodRaw,
    };
  }
  const requestedAt = Date.parse(request.requestedAt);
  const requestExpiresAt = Date.parse(request.expiresAt);
  const policyExpiresAt = Date.parse(policy.expiresAt);
  const nowMs = now.getTime();
  const checks = {
    requestIdBound: REQUEST_ID_PATTERN.test(request.requestId),
    nonceValid: Number.isSafeInteger(request.nonce) && request.nonce >= 0,
    nonceUnused: !context.usedNonces?.has(request.nonce),
    requestTimeValid: Number.isFinite(requestedAt) && requestedAt <= nowMs + MAX_CLOCK_SKEW_MS,
    requestFresh: Number.isFinite(requestExpiresAt) && requestExpiresAt > nowMs && requestExpiresAt > requestedAt,
    allowanceMatches: policy.allowanceId === request.allowanceId,
    allowanceActive: Number.isFinite(policyExpiresAt) && policyExpiresAt > nowMs,
    merchantMatches: policy.merchant === request.merchant,
    tokenMatches: policy.token === request.token,
    tokenMintMatches: policy.tokenMint === request.tokenMint,
    perChargeWithinPolicy: amount <= perCharge,
    periodCapWithinPolicy: spentInPeriod + amount <= periodCap,
    programAllowed: policy.allowedProgram === request.program,
    evidenceBound: HASH_PATTERN.test(request.evidenceHash) && request.evidenceUri.trim().length > 0,
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
  if (!checks.tokenMintMatches) reasons.push('token_mint_mismatch');
  if (!checks.perChargeWithinPolicy) reasons.push('per_charge_limit_exceeded');
  if (!checks.periodCapWithinPolicy) reasons.push('period_cap_exceeded');
  if (!checks.programAllowed) reasons.push('program_not_allowed');
  if (!checks.evidenceBound) reasons.push('evidence_not_bound');
  if (!checks.evidenceUnused) reasons.push('evidence_replayed');

  const passed = Object.values(checks).every(Boolean);
  const identityAndEvidenceSafe = checks.allowanceMatches
    && checks.merchantMatches
    && checks.tokenMatches
    && checks.tokenMintMatches
    && checks.programAllowed
    && checks.evidenceBound;
  const state = passed ? 'VERIFIED' : identityAndEvidenceSafe ? 'BLOCKED' : 'FROZEN';
  return {
    state,
    reasons,
    checks,
    nextSpentInPeriodRaw: passed ? (spentInPeriod + amount).toString(10) : policy.spentInPeriodRaw,
  };
}

export function applyVerifiedCharge(policy: AllowancePolicy, request: ChargeRequest, now = new Date()): AllowancePolicy {
  const result = evaluateCharge(policy, request, now);
  if (result.state !== 'VERIFIED') return policy;
  return { ...policy, spentInPeriodRaw: result.nextSpentInPeriodRaw };
}

export function issueReceipt(policy: AllowancePolicy, request: ChargeRequest, result: CheckResult, now = new Date()): Receipt {
  return {
    receiptVersion: '3',
    requestId: request.requestId,
    nonce: request.nonce,
    allowanceId: policy.allowanceId,
    state: result.state,
    merchant: request.merchant,
    token: request.token,
    tokenMint: request.tokenMint,
    tokenDecimals: policy.tokenDecimals,
    amountRaw: request.amountRaw,
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
