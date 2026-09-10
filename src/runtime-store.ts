import { mkdirSync, readFileSync, renameSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import type { AllowancePolicy, Receipt } from './types.js';

export type StoredRequestRecord = {
  fingerprint: string;
  receipt: Receipt;
};

export type RuntimeSnapshot = {
  schemaVersion: 1;
  policies: Record<string, AllowancePolicy>;
  requests: Record<string, StoredRequestRecord>;
  nonces: Record<string, number[]>;
  evidenceHashes: Record<string, string[]>;
};

export interface RuntimeStateStore {
  load(): RuntimeSnapshot | undefined;
  save(snapshot: RuntimeSnapshot): void;
}

export class MemoryRuntimeStateStore implements RuntimeStateStore {
  private snapshot?: RuntimeSnapshot;

  load(): RuntimeSnapshot | undefined {
    return this.snapshot ? structuredClone(this.snapshot) : undefined;
  }

  save(snapshot: RuntimeSnapshot): void {
    this.snapshot = structuredClone(snapshot);
  }
}

/**
 * Atomic local reference store for a single merchant process.
 * Production clusters should implement RuntimeStateStore with a transactional DB.
 */
export class JsonFileRuntimeStateStore implements RuntimeStateStore {
  constructor(private readonly path: string) {}

  load(): RuntimeSnapshot | undefined {
    try {
      const parsed = JSON.parse(readFileSync(this.path, 'utf8')) as RuntimeSnapshot;
      if (parsed.schemaVersion !== 1) throw new Error('UNSUPPORTED_RUNTIME_SNAPSHOT');
      return parsed;
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') return undefined;
      throw error;
    }
  }

  save(snapshot: RuntimeSnapshot): void {
    mkdirSync(dirname(this.path), { recursive: true });
    const temporary = `${this.path}.${process.pid}.tmp`;
    writeFileSync(temporary, `${JSON.stringify(snapshot, null, 2)}\n`, { mode: 0o600 });
    renameSync(temporary, this.path);
  }
}
