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

export type SolanaInstructionExecutor = {
  send(instruction: unknown): Promise<{ signature: string }>;
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

  constructor(private readonly executor?: SolanaInstructionExecutor) {}

  private configuredExecutor(): SolanaInstructionExecutor {
    if (!this.executor) {
      throw new Error('LIVE_ADAPTER_NOT_CONFIGURED: inject an MWA or server signer executor; credentials are never stored by the SDK');
    }
    return this.executor;
  }

  async authorizeAllowance(policy: AllowancePolicy): Promise<{ authorizationId: string; instruction: unknown }> {
    const instruction = createAllowanceInstruction(policy);
    const { signature } = await this.configuredExecutor().send(instruction);
    return { authorizationId: signature, instruction };
  }

  async executeCharge(policy: AllowancePolicy, request: ChargeRequest): Promise<Receipt> {
    const result = evaluateCharge(policy, request);
    if (result.state !== 'VERIFIED') return issueReceipt(policy, request, result);
    const { signature } = await this.configuredExecutor().send(chargeAllowanceInstruction(policy, request));
    return { ...issueReceipt(policy, request, result), txHash: signature };
  }

  async revokeAllowance(policy: AllowancePolicy): Promise<{ state: 'REVOKED'; instruction: unknown }> {
    const instruction = revokeAllowanceInstruction(revokeAllowance(policy));
    await this.configuredExecutor().send(instruction);
    return { state: 'REVOKED', instruction };
  }
}
