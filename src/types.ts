export type AllowanceState = 'VERIFIED' | 'BLOCKED' | 'FROZEN' | 'REVOKED';

export type AllowancePolicy = {
  allowanceId: string;
  subscriber: string;
  merchant: string;
  merchantName: string;
  token: 'USDC' | 'SKR';
  perCharge: number;
  period: 'daily' | 'weekly' | 'monthly';
  periodCap: number;
  spentInPeriod: number;
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
  amount: number;
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
  nextSpentInPeriod: number;
};

export type Receipt = {
  receiptVersion: '2';
  requestId: string;
  nonce: number;
  allowanceId: string;
  state: AllowanceState;
  merchant: string;
  token: AllowancePolicy['token'];
  amount: number;
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
