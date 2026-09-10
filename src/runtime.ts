import { applyVerifiedCharge, evaluateCharge, issueReceipt } from './engine.js';
import { stableHash } from './hash.js';
import { MemoryRuntimeStateStore, type RuntimeSnapshot, type RuntimeStateStore, type StoredRequestRecord } from './runtime-store.js';
import type { AllowancePolicy, ChargeRequest, Receipt } from './types.js';

export class AllowanceRuntime {
  private readonly policies = new Map<string, AllowancePolicy>();
  private readonly requests = new Map<string, StoredRequestRecord>();
  private readonly nonces = new Map<string, Set<number>>();
  private readonly evidenceHashes = new Map<string, Set<string>>();

  constructor(private readonly store: RuntimeStateStore = new MemoryRuntimeStateStore()) {
    const snapshot = store.load();
    if (!snapshot) return;
    Object.entries(snapshot.policies).forEach(([key, value]) => this.policies.set(key, value));
    Object.entries(snapshot.requests).forEach(([key, value]) => this.requests.set(key, value));
    Object.entries(snapshot.nonces).forEach(([key, value]) => this.nonces.set(key, new Set(value)));
    Object.entries(snapshot.evidenceHashes).forEach(([key, value]) => this.evidenceHashes.set(key, new Set(value)));
  }

  createAllowance(policy: AllowancePolicy): AllowancePolicy {
    if (this.policies.has(policy.allowanceId)) throw new Error('ALLOWANCE_ALREADY_EXISTS');
    const stored = { ...policy };
    this.policies.set(policy.allowanceId, stored);
    this.persist();
    return { ...stored };
  }

  getAllowance(allowanceId: string): AllowancePolicy | undefined {
    const policy = this.policies.get(allowanceId);
    return policy ? { ...policy } : undefined;
  }

  requestCharge(request: ChargeRequest, now = new Date()): Receipt {
    const policy = this.policies.get(request.allowanceId);
    if (!policy) throw new Error('ALLOWANCE_NOT_FOUND');

    const fingerprint = stableHash(request);
    const prior = this.requests.get(request.requestId);
    if (prior) {
      if (prior.fingerprint !== fingerprint) throw new Error('IDEMPOTENCY_KEY_CONFLICT');
      return { ...prior.receipt, idempotentReplay: true };
    }

    const usedNonces = this.nonces.get(request.allowanceId) ?? new Set<number>();
    const usedEvidenceHashes = this.evidenceHashes.get(request.allowanceId) ?? new Set<string>();
    const result = evaluateCharge(policy, request, now, { usedNonces, usedEvidenceHashes });
    const receipt = issueReceipt(policy, request, result, now);

    this.requests.set(request.requestId, { fingerprint, receipt });
    usedNonces.add(request.nonce);
    this.nonces.set(request.allowanceId, usedNonces);
    if (result.state === 'VERIFIED') {
      usedEvidenceHashes.add(request.evidenceHash.toLowerCase());
      this.evidenceHashes.set(request.allowanceId, usedEvidenceHashes);
      this.policies.set(request.allowanceId, applyVerifiedCharge(policy, request, now));
    }
    this.persist();
    return { ...receipt };
  }

  revokeAllowance(allowanceId: string): AllowancePolicy {
    const policy = this.policies.get(allowanceId);
    if (!policy) throw new Error('ALLOWANCE_NOT_FOUND');
    const revoked = { ...policy, revoked: true, expiresAt: new Date(0).toISOString() };
    this.policies.set(allowanceId, revoked);
    this.persist();
    return { ...revoked };
  }

  snapshot(): RuntimeSnapshot {
    return {
      schemaVersion: 1,
      policies: Object.fromEntries(this.policies),
      requests: Object.fromEntries(this.requests),
      nonces: Object.fromEntries([...this.nonces].map(([key, value]) => [key, [...value].sort((a, b) => a - b)])),
      evidenceHashes: Object.fromEntries([...this.evidenceHashes].map(([key, value]) => [key, [...value].sort()])),
    };
  }

  private persist(): void {
    this.store.save(this.snapshot());
  }
}
