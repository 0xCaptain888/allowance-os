import { createHmac, timingSafeEqual } from 'node:crypto';
import { stableHash } from './hash.js';
import type { Receipt, WebhookEnvelope } from './types.js';

export function createWebhookEnvelope(receipt: Receipt, now = new Date()): WebhookEnvelope {
  return {
    eventId: `evt_${stableHash({ receipt, createdAt: now.toISOString() }).slice(0, 24)}`,
    eventType: 'allowance.charge.completed',
    createdAt: now.toISOString(),
    receipt,
  };
}

export function signWebhook(envelope: WebhookEnvelope, secret: string): string {
  if (secret.length < 16) throw new Error('WEBHOOK_SECRET_TOO_SHORT');
  return `sha256=${createHmac('sha256', secret).update(JSON.stringify(envelope)).digest('hex')}`;
}

export function verifyWebhookSignature(envelope: WebhookEnvelope, signature: string, secret: string): boolean {
  const expected = signWebhook(envelope, secret);
  const actualBuffer = Buffer.from(signature);
  const expectedBuffer = Buffer.from(expected);
  return actualBuffer.length === expectedBuffer.length && timingSafeEqual(actualBuffer, expectedBuffer);
}
