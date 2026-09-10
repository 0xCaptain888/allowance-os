import { PublicKey, TransactionInstruction } from '@solana/web3.js';

export const DELEGATED_STATE_SIZE = 332;
export const DELEGATE_SEED = 'allowance-delegate';
export const SPL_TOKEN_PROGRAM_ID = new PublicKey('TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA');

export type DelegatedAllowanceConfig = {
  programId: PublicKey;
  allowance: PublicKey;
  authority: PublicKey;
  merchant: PublicKey;
  executor: PublicKey;
  verifier: PublicKey;
  tokenMint: PublicKey;
  sourceToken: PublicKey;
  perCharge: bigint;
  periodCap: bigint;
  lifetimeCap: bigint;
  periodSeconds: bigint;
  expiresAt: bigint;
  policyHash: Uint8Array;
};

function u64(value: bigint): Buffer {
  if (value < 0n || value > 0xffff_ffff_ffff_ffffn) throw new Error('u64 out of range');
  const output = Buffer.alloc(8);
  output.writeBigUInt64LE(value);
  return output;
}

function i64(value: bigint): Buffer {
  if (value < -0x8000_0000_0000_0000n || value > 0x7fff_ffff_ffff_ffffn) {
    throw new Error('i64 out of range');
  }
  const output = Buffer.alloc(8);
  output.writeBigInt64LE(value);
  return output;
}

function digest(value: Uint8Array, label: string): Buffer {
  if (value.length !== 32) throw new Error(`${label} must contain exactly 32 bytes`);
  return Buffer.from(value);
}

export function delegatedAuthority(programId: PublicKey, allowance: PublicKey): [PublicKey, number] {
  return PublicKey.findProgramAddressSync(
    [Buffer.from(DELEGATE_SEED, 'utf8'), allowance.toBuffer()],
    programId,
  );
}

export function createDelegatedInstruction(config: DelegatedAllowanceConfig): TransactionInstruction {
  const [delegate] = delegatedAuthority(config.programId, config.allowance);
  const data = Buffer.concat([
    Buffer.from([3]),
    config.merchant.toBuffer(),
    config.executor.toBuffer(),
    config.verifier.toBuffer(),
    config.tokenMint.toBuffer(),
    config.sourceToken.toBuffer(),
    u64(config.perCharge),
    u64(config.periodCap),
    u64(config.lifetimeCap),
    i64(config.periodSeconds),
    i64(config.expiresAt),
    digest(config.policyHash, 'policyHash'),
  ]);
  return new TransactionInstruction({
    programId: config.programId,
    keys: [
      { pubkey: config.authority, isSigner: true, isWritable: false },
      { pubkey: config.allowance, isSigner: false, isWritable: true },
      { pubkey: config.sourceToken, isSigner: false, isWritable: true },
      { pubkey: delegate, isSigner: false, isWritable: false },
      { pubkey: SPL_TOKEN_PROGRAM_ID, isSigner: false, isWritable: false },
    ],
    data,
  });
}

export function chargeDelegatedInstruction(input: {
  programId: PublicKey;
  allowance: PublicKey;
  executor: PublicKey;
  verifier: PublicKey;
  sourceToken: PublicKey;
  merchantToken: PublicKey;
  amount: bigint;
  nonce: bigint;
  evidenceHash: Uint8Array;
}): TransactionInstruction {
  const [delegate] = delegatedAuthority(input.programId, input.allowance);
  return new TransactionInstruction({
    programId: input.programId,
    keys: [
      { pubkey: input.executor, isSigner: true, isWritable: false },
      { pubkey: input.verifier, isSigner: true, isWritable: false },
      { pubkey: input.allowance, isSigner: false, isWritable: true },
      { pubkey: input.sourceToken, isSigner: false, isWritable: true },
      { pubkey: input.merchantToken, isSigner: false, isWritable: true },
      { pubkey: delegate, isSigner: false, isWritable: false },
      { pubkey: SPL_TOKEN_PROGRAM_ID, isSigner: false, isWritable: false },
    ],
    data: Buffer.concat([
      Buffer.from([4]),
      u64(input.amount),
      u64(input.nonce),
      digest(input.evidenceHash, 'evidenceHash'),
    ]),
  });
}

function authorityControlInstruction(
  discriminant: 5 | 6,
  programId: PublicKey,
  authority: PublicKey,
  allowance: PublicKey,
): TransactionInstruction {
  return new TransactionInstruction({
    programId,
    keys: [
      { pubkey: authority, isSigner: true, isWritable: false },
      { pubkey: allowance, isSigner: false, isWritable: true },
    ],
    data: Buffer.from([discriminant]),
  });
}

export function pauseDelegatedInstruction(
  programId: PublicKey,
  authority: PublicKey,
  allowance: PublicKey,
): TransactionInstruction {
  return authorityControlInstruction(5, programId, authority, allowance);
}

export function unpauseDelegatedInstruction(
  programId: PublicKey,
  authority: PublicKey,
  allowance: PublicKey,
): TransactionInstruction {
  return authorityControlInstruction(6, programId, authority, allowance);
}

export function freezeDelegatedInstruction(
  programId: PublicKey,
  verifier: PublicKey,
  allowance: PublicKey,
  evidenceHash: Uint8Array,
): TransactionInstruction {
  return new TransactionInstruction({
    programId,
    keys: [
      { pubkey: verifier, isSigner: true, isWritable: false },
      { pubkey: allowance, isSigner: false, isWritable: true },
    ],
    data: Buffer.concat([Buffer.from([7]), digest(evidenceHash, 'evidenceHash')]),
  });
}

export function unfreezeDelegatedInstruction(input: {
  programId: PublicKey;
  authority: PublicKey;
  verifier: PublicKey;
  allowance: PublicKey;
}): TransactionInstruction {
  return new TransactionInstruction({
    programId: input.programId,
    keys: [
      { pubkey: input.authority, isSigner: true, isWritable: false },
      { pubkey: input.verifier, isSigner: true, isWritable: false },
      { pubkey: input.allowance, isSigner: false, isWritable: true },
    ],
    data: Buffer.from([8]),
  });
}

export function revokeDelegatedInstruction(input: {
  programId: PublicKey;
  authority: PublicKey;
  allowance: PublicKey;
  sourceToken: PublicKey;
}): TransactionInstruction {
  return new TransactionInstruction({
    programId: input.programId,
    keys: [
      { pubkey: input.authority, isSigner: true, isWritable: false },
      { pubkey: input.allowance, isSigner: false, isWritable: true },
      { pubkey: input.sourceToken, isSigner: false, isWritable: true },
      { pubkey: SPL_TOKEN_PROGRAM_ID, isSigner: false, isWritable: false },
    ],
    data: Buffer.from([9]),
  });
}
