import { evaluateCharge, issueReceipt, revokeAllowance } from './engine.js';
import type { AllowancePolicy, ChargeRequest } from './types.js';

const policy: AllowancePolicy = {
  allowanceId: 'allowance-researchpulse-001',
  subscriber: 'seeker:alice.skr',
  merchant: 'merchant:researchpulse',
  merchantName: 'ResearchPulse',
  token: 'USDC',
  perCharge: 2,
  period: 'weekly',
  periodCap: 8,
  spentInPeriod: 0,
  expiresAt: '2026-10-09T00:00:00.000Z',
  allowedProgram: 'program:allowance-os-devnet',
  appReleaseHash: 'release:allowance-os-0.11.0',
};

const scenarios: Array<[string, ChargeRequest]> = [
  ['VERIFIED', { requestId: 'req_demo_verified', nonce: 1, allowanceId: policy.allowanceId, merchant: policy.merchant, token: 'USDC', amount: 2, program: policy.allowedProgram, requestedAt: '2026-09-09T00:00:00.000Z', expiresAt: '2026-09-09T00:05:00.000Z', evidenceHash: 'a'.repeat(64), evidenceUri: 'ipfs://demo/verified', evidenceType: 'content-delivery' }],
  ['BLOCKED', { requestId: 'req_demo_blocked', nonce: 2, allowanceId: policy.allowanceId, merchant: policy.merchant, token: 'USDC', amount: 10, program: policy.allowedProgram, requestedAt: '2026-09-09T00:01:00.000Z', expiresAt: '2026-09-09T00:05:00.000Z', evidenceHash: 'b'.repeat(64), evidenceUri: 'ipfs://demo/blocked', evidenceType: 'content-delivery' }],
  ['FROZEN', { requestId: 'req_demo_frozen', nonce: 3, allowanceId: policy.allowanceId, merchant: 'merchant:lookalike', token: 'USDC', amount: 2, program: policy.allowedProgram, requestedAt: '2026-09-09T00:02:00.000Z', expiresAt: '2026-09-09T00:05:00.000Z', evidenceHash: 'c'.repeat(64), evidenceUri: 'ipfs://demo/frozen', evidenceType: 'content-delivery' }],
];

for (const [expected, request] of scenarios) {
  const result = evaluateCharge(policy, request, new Date('2026-09-09T00:00:00.000Z'));
  console.log(JSON.stringify({ expected, state: result.state, receipt: issueReceipt(policy, request, result) }, null, 2));
}

console.log(JSON.stringify({ action: 'revoke', result: revokeAllowance(policy) }, null, 2));
