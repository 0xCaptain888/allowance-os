import { stableHash } from './hash.js';

export type AlphaBriefSource = {
  label: string;
  uri: string;
  capturedAt: string;
};

export type AlphaBriefDelivery = {
  schemaVersion: '1';
  serviceId: 'alphabrief';
  deliveryId: string;
  subscriber: string;
  merchant: string;
  title: string;
  generatedAt: string;
  content: string;
  sources: AlphaBriefSource[];
};

export type AlphaBriefVerificationPolicy = {
  expectedMerchant: string;
  minimumWords: number;
  minimumSources: number;
  requiredSections: string[];
  maximumAgeMs: number;
};

export type AlphaBriefVerification = {
  schemaVersion: '1';
  verifierType: 'INDEPENDENT_DELIVERY_VERIFIER';
  passed: boolean;
  reasons: string[];
  checks: {
    merchantBound: boolean;
    deliveryIdValid: boolean;
    generatedAtValid: boolean;
    deliveryFresh: boolean;
    contentSubstantial: boolean;
    requiredSectionsPresent: boolean;
    sourcesSufficient: boolean;
    sourceUrisValid: boolean;
  };
  wordCount: number;
  contentHash: string;
  evidenceHash: string;
  verifiedAt: string;
};

export const alphaBriefVerificationPolicy: AlphaBriefVerificationPolicy = {
  expectedMerchant: 'merchant:alphabrief',
  minimumWords: 180,
  minimumSources: 2,
  requiredSections: ['Executive Summary', 'Risk Findings', 'Recommendations', 'Sources'],
  maximumAgeMs: 7 * 24 * 60 * 60 * 1_000,
};

export function verifyAlphaBriefDelivery(
  delivery: AlphaBriefDelivery,
  verifiedAt: Date,
  policy: AlphaBriefVerificationPolicy = alphaBriefVerificationPolicy,
): AlphaBriefVerification {
  const generatedAtMs = Date.parse(delivery.generatedAt);
  const wordCount = delivery.content.trim().split(/\s+/u).filter(Boolean).length;
  const checks = {
    merchantBound: delivery.merchant === policy.expectedMerchant,
    deliveryIdValid: /^ab_[a-z0-9_]{8,80}$/u.test(delivery.deliveryId),
    generatedAtValid: Number.isFinite(generatedAtMs),
    deliveryFresh: Number.isFinite(generatedAtMs)
      && generatedAtMs <= verifiedAt.getTime()
      && verifiedAt.getTime() - generatedAtMs <= policy.maximumAgeMs,
    contentSubstantial: wordCount >= policy.minimumWords,
    requiredSectionsPresent: policy.requiredSections.every(section => (
      delivery.content.includes(`# ${section}`) || delivery.content.includes(`## ${section}`)
    )),
    sourcesSufficient: delivery.sources.length >= policy.minimumSources,
    sourceUrisValid: delivery.sources.every(source => {
      const capturedAtMs = Date.parse(source.capturedAt);
      return /^https:\/\//u.test(source.uri) && Number.isFinite(capturedAtMs) && capturedAtMs <= verifiedAt.getTime();
    }),
  };
  const reasons = Object.entries(checks).filter(([, passed]) => !passed).map(([name]) => name);
  const contentHash = stableHash({ content: delivery.content });
  const evidenceHash = stableHash({
    schemaVersion: '1',
    serviceId: delivery.serviceId,
    deliveryId: delivery.deliveryId,
    subscriber: delivery.subscriber,
    merchant: delivery.merchant,
    title: delivery.title,
    generatedAt: delivery.generatedAt,
    contentHash,
    sources: delivery.sources,
    verifier: {
      type: 'INDEPENDENT_DELIVERY_VERIFIER',
      passed: reasons.length === 0,
      checks,
      policy,
    },
  });
  return {
    schemaVersion: '1',
    verifierType: 'INDEPENDENT_DELIVERY_VERIFIER',
    passed: reasons.length === 0,
    reasons,
    checks,
    wordCount,
    contentHash,
    evidenceHash,
    verifiedAt: verifiedAt.toISOString(),
  };
}
