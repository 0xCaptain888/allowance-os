import { createHmac, timingSafeEqual } from 'node:crypto';
import { canonicalJson, stableHash } from './hash.js';
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
  return `sha256=${createHmac('sha256', secret).update(canonicalJson(envelope)).digest('hex')}`;
}

export function verifyWebhookSignature(envelope: WebhookEnvelope, signature: string, secret: string): boolean {
  const expected = signWebhook(envelope, secret);
  const actualBuffer = Buffer.from(signature);
  const expectedBuffer = Buffer.from(expected);
  return actualBuffer.length === expectedBuffer.length && timingSafeEqual(actualBuffer, expectedBuffer);
}

export type WebhookVerificationResult = {
  valid: boolean;
  reason: 'verified' | 'invalid_signature' | 'invalid_timestamp' | 'stale_event' | 'replayed_event';
};

/** Stateful merchant-side verifier with timestamp tolerance, replay defense and secret rotation. */
export class WebhookVerifier {
  private readonly seenEvents = new Map<string, number>();

  constructor(
    private secrets: string[],
    private readonly toleranceMs = 5 * 60_000,
  ) {
    this.setSecrets(secrets);
    if (!Number.isSafeInteger(toleranceMs) || toleranceMs <= 0) throw new Error('INVALID_WEBHOOK_TOLERANCE');
  }

  setSecrets(secrets: string[]): void {
    if (secrets.length === 0 || secrets.some(secret => secret.length < 16)) {
      throw new Error('INVALID_WEBHOOK_SECRETS');
    }
    this.secrets = [...new Set(secrets)];
  }

  verify(envelope: WebhookEnvelope, signature: string, now = new Date()): WebhookVerificationResult {
    const createdAt = Date.parse(envelope.createdAt);
    if (!Number.isFinite(createdAt)) return { valid: false, reason: 'invalid_timestamp' };
    this.prune(now.getTime());
    if (Math.abs(now.getTime() - createdAt) > this.toleranceMs) return { valid: false, reason: 'stale_event' };
    if (this.seenEvents.has(envelope.eventId)) return { valid: false, reason: 'replayed_event' };
    if (!this.secrets.some(secret => verifyWebhookSignature(envelope, signature, secret))) {
      return { valid: false, reason: 'invalid_signature' };
    }
    this.seenEvents.set(envelope.eventId, createdAt);
    return { valid: true, reason: 'verified' };
  }

  private prune(now: number): void {
    for (const [eventId, createdAt] of this.seenEvents) {
      if (now - createdAt > this.toleranceMs) this.seenEvents.delete(eventId);
    }
  }
}
