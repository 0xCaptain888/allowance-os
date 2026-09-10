# Build and verification

## TypeScript

```bash
npm install
npm run typecheck
npm test
npm run demo
```

## Rust program

```bash
cd program
cargo fmt -- --check
cargo test
cargo install cargo-build-sbf --version 4.3.0 --locked
cargo-build-sbf --manifest-path Cargo.toml
```

The current macOS SBF artifact is `130,280` bytes with SHA-256 `e0eb4726bdfda25fa2a377f347b9590087072e4ad1d9e455598760f6b865e7d6`. Ubuntu CI run `34449449052` independently produced the same length with SHA-256 `45a3996ee011587e394951ee344742290c0aa7d0d132d972cb0168360df191dc`. Host hashes are recorded separately because the project does not claim cross-platform byte-for-byte reproducibility. If a machine has a broken global Cargo mirror override, use a task-scoped Cargo config or restore crates.io rather than changing dependency versions.

## Live evidence rule

The repository does not mark a charge live unless a confirmed Solana signature, block slot, and explorer URL are present. Local `simulated:` receipts are for judge replay only.
