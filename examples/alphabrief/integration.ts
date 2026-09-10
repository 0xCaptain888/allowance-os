import { readFile } from 'node:fs/promises';
import { stableHash } from '../../src/hash.js';
import { AllowanceOS } from '../../src/sdk.js';
import { verifyWebhookSignature } from '../../src/webhook.js';
import type { AllowancePolicy, ChargeRequest } from '../../src/types.js';

const now = new Date('2026-09-10T02:00:00.000Z');
const report = await readFile(new URL('./report.md', import.meta.url), 'utf8');
const evidenceHash = stableHash({ content: report });
const webhookSecret = 'alphabrief-local-demo-secret';
const sdk = new AllowanceOS(webhookSecret);

const policy: AllowancePolicy = {
  allowanceId: 'allowance-alphabrief-reference', subscriber: 'wallet:demo-subscriber',
  merchant: 'merchant:alphabrief', merchantName: 'AlphaBrief', token: 'USDC', perCharge: 2,
  period: 'weekly', periodCap: 8, spentInPeriod: 0, periodStartedAt: now.toISOString(),
  expiresAt: '2026-10-10T00:00:00.000Z', allowedProgram: 'program:allowance-os-devnet', appReleaseHash: 'android-v0.11.0',
};

const request: ChargeRequest = {
  requestId: 'req_alphabrief_reference_001', nonce: 1, allowanceId: policy.allowanceId,
  merchant: policy.merchant, token: policy.token, amount: 2, program: policy.allowedProgram,
  requestedAt: now.toISOString(), expiresAt: new Date(now.getTime() + 5 * 60_000).toISOString(),
  evidenceHash, evidenceUri: 'repo://examples/alphabrief/report.md', evidenceType: 'content-delivery',
};

sdk.createAllowance(policy);
const completed = sdk.requestCharge(request, now);
const webhookAuthentic = verifyWebhookSignature(completed.webhook, completed.webhookSignature, webhookSecret);
const unlocked = webhookAuthentic && sdk.verifyEvidence(completed.receipt, evidenceHash);
const idempotentRetry = sdk.requestCharge(request, new Date(now.getTime() + 1_000));
const replayAttempt = sdk.requestCharge({ ...request, requestId: 'req_alphabrief_reference_002', nonce: 2 }, new Date(now.getTime() + 2_000));

console.log(JSON.stringify({
  flow: 'AlphaBrief report -> content hash -> policy -> receipt -> signed webhook -> unlock',
  report: { evidenceHash, unlocked, preview: unlocked ? report.split('\n').slice(0, 3).join('\n') : 'LOCKED' },
  completed,
  retry: { state: idempotentRetry.receipt.state, idempotentReplay: idempotentRetry.receipt.idempotentReplay },
  replayAttempt: { state: replayAttempt.receipt.state, reasons: replayAttempt.receipt.reasons },
}, null, 2));
