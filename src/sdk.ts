import { AllowanceRuntime } from './runtime.js';
import { createWebhookEnvelope, signWebhook } from './webhook.js';
import type { AllowancePolicy, ChargeRequest, Receipt, WebhookEnvelope } from './types.js';

export type ChargeResult = {
  receipt: Receipt;
  webhook: WebhookEnvelope;
  webhookSignature: string;
};

export class AllowanceOS {
  constructor(
    private readonly webhookSecret: string,
    private readonly runtime = new AllowanceRuntime(),
  ) {}

  createAllowance(policy: AllowancePolicy): AllowancePolicy {
    return this.runtime.createAllowance(policy);
  }

  requestCharge(request: ChargeRequest, now = new Date()): ChargeResult {
    const receipt = this.runtime.requestCharge(request, now);
    const webhook = createWebhookEnvelope(receipt, now);
    return { receipt, webhook, webhookSignature: signWebhook(webhook, this.webhookSecret) };
  }

  verifyEvidence(receipt: Receipt, evidenceHash: string): boolean {
    return receipt.state === 'VERIFIED' && receipt.evidenceHash.toLowerCase() === evidenceHash.toLowerCase();
  }

  revokeAllowance(allowanceId: string): AllowancePolicy {
    return this.runtime.revokeAllowance(allowanceId);
  }
}

export { JsonFileRuntimeStateStore, MemoryRuntimeStateStore } from './runtime-store.js';
export type { RuntimeSnapshot, RuntimeStateStore } from './runtime-store.js';
