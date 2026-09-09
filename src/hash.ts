import { createHash } from 'node:crypto';

export function stableHash(value: unknown): string {
  const normalized = JSON.stringify(value, Object.keys((value ?? {}) as object).sort());
  return createHash('sha256').update(normalized).digest('hex');
}
