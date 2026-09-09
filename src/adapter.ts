import { evaluateCharge, issueReceipt, revokeAllowance } from './engine.js';
import { chargeAllowanceInstruction, createAllowanceInstruction, revokeAllowanceInstruction } from './protocol.js';
import type { AllowancePolicy, ChargeRequest, Receipt } from './types.js';

export type AdapterMode = 'simulated' | 'solana-devnet';

export type AllowanceAdapter = {
  readonly mode: AdapterMode;
  authorizeAllowance(policy: AllowancePolicy): Promise<{ authorizationId: string; instruction: unknown }>;
  executeCharge(policy: AllowancePolicy, request: ChargeRequest): Promise<Receipt>;
  revokeAllowance(policy: AllowancePolicy): Promise<{ state: 'REVOKED'; instruction: unknown }>;
};

export class SimulatedAllowanceAdapter implements AllowanceAdapter {
  readonly mode = 'simulated' as const;

  async authorizeAllowance(policy: AllowancePolicy) {
    return { authorizationId: `simulated-auth:${policy.allowanceId}`, instruction: createAllowanceInstruction(policy) };
  }

  async executeCharge(policy: AllowancePolicy, request: ChargeRequest) {
    const result = evaluateCharge(policy, request);
    return issueReceipt(policy, request, result);
  }

  async revokeAllowance(policy: AllowancePolicy) {
    return { state: 'REVOKED' as const, instruction: revokeAllowanceInstruction(revokeAllowance(policy)) };
  }
}

export class SolanaDevnetAdapter implements AllowanceAdapter {
  readonly mode = 'solana-devnet' as const;

  async authorizeAllowance(_policy: AllowancePolicy): Promise<{ authorizationId: string; instruction: unknown }> {
    throw new Error('LIVE_ADAPTER_NOT_CONFIGURED: MWA and Seed Vault authorization are required before broadcasting');
  }

  async executeCharge(_policy: AllowancePolicy, _request: ChargeRequest): Promise<Receipt> {
    throw new Error('LIVE_ADAPTER_NOT_CONFIGURED: no live Solana transaction has been broadcast');
  }

  async revokeAllowance(_policy: AllowancePolicy): Promise<{ state: 'REVOKED'; instruction: unknown }> {
    throw new Error('LIVE_ADAPTER_NOT_CONFIGURED: revoke requires a signed Solana instruction');
  }
}
