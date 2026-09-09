import { stableHash } from './hash.js';
import type { AllowancePolicy, ChargeRequest } from './types.js';

export type AllowanceInstruction =
  | { kind: 'createAllowance'; policy: AllowancePolicy }
  | { kind: 'chargeAllowance'; request: ChargeRequest; policyHash: string }
  | { kind: 'revokeAllowance'; allowanceId: string; policyHash: string };

export function policyHash(policy: AllowancePolicy): string {
  return stableHash(policy);
}

export function createAllowanceInstruction(policy: AllowancePolicy): AllowanceInstruction {
  return { kind: 'createAllowance', policy };
}

export function chargeAllowanceInstruction(policy: AllowancePolicy, request: ChargeRequest): AllowanceInstruction {
  return { kind: 'chargeAllowance', request, policyHash: policyHash(policy) };
}

export function revokeAllowanceInstruction(policy: AllowancePolicy): AllowanceInstruction {
  return { kind: 'revokeAllowance', allowanceId: policy.allowanceId, policyHash: policyHash(policy) };
}
