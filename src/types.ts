export type AllowanceState = 'VERIFIED' | 'BLOCKED' | 'FROZEN' | 'REVOKED';

/** Base-10 integer string in the token mint's smallest unit. */
export type RawAmount = string;

export type AllowancePolicy = {
  allowanceId: string;
  subscriber: string;
  merchant: string;
  merchantName: string;
  token: 'USDC' | 'SKR';
  tokenMint: string;
  tokenDecimals: number;
  perChargeRaw: RawAmount;
  period: 'daily' | 'weekly' | 'monthly';
  periodCapRaw: RawAmount;
  spentInPeriodRaw: RawAmount;
  periodStartedAt?: string;
  expiresAt: string;
  allowedProgram: string;
  appReleaseHash: string;
  revoked?: boolean;
};

export type ChargeRequest = {
  requestId: string;
  nonce: number;
  allowanceId: string;
  merchant: string;
  token: AllowancePolicy['token'];
  tokenMint: string;
  amountRaw: RawAmount;
  program: string;
  requestedAt: string;
  expiresAt: string;
  evidenceHash: string;
  evidenceUri: string;
  evidenceType: 'content-delivery' | 'agent-run' | 'signal' | 'order' | 'usage-batch';
};

export type EvaluationContext = {
  usedNonces?: ReadonlySet<number>;
  usedEvidenceHashes?: ReadonlySet<string>;
};

export type CheckResult = {
  state: AllowanceState;
  reasons: string[];
  checks: Record<string, boolean>;
  nextSpentInPeriodRaw: RawAmount;
};

export type Receipt = {
  receiptVersion: '3';
  requestId: string;
  nonce: number;
  allowanceId: string;
  state: AllowanceState;
  merchant: string;
  token: AllowancePolicy['token'];
  tokenMint: string;
  tokenDecimals: number;
  amountRaw: RawAmount;
  policyHash: string;
  evidenceHash: string;
  evidenceUri: string;
  evidenceType: ChargeRequest['evidenceType'];
  txHash?: string;
  reasons: string[];
  checks: Record<string, boolean>;
  idempotentReplay?: boolean;
  createdAt: string;
};

export type WebhookEnvelope = {
  eventId: string;
  eventType: 'allowance.charge.completed';
  createdAt: string;
  receipt: Receipt;
};
