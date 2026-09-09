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
  expiresAt: string;
  allowedProgram: string;
  appReleaseHash: string;
  revoked?: boolean;
};

export type ChargeRequest = {
  allowanceId: string;
  merchant: string;
  token: AllowancePolicy['token'];
  amount: number;
  program: string;
  requestedAt: string;
  evidenceHash: string;
};

export type CheckResult = {
  state: AllowanceState;
  reasons: string[];
  checks: Record<string, boolean>;
  nextSpentInPeriod: number;
};

export type Receipt = {
  receiptVersion: '1';
  allowanceId: string;
  state: AllowanceState;
  merchant: string;
  token: AllowancePolicy['token'];
  amount: number;
  policyHash: string;
  evidenceHash: string;
  txHash?: string;
  reasons: string[];
  createdAt: string;
};
