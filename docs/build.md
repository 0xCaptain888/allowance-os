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
```

The local macOS environment used during bootstrap has a global Cargo registry replacement that may prevent dependency resolution. CI uses the default crates.io registry and is the source of truth for the native program build until the local registry configuration is corrected.

## Live evidence rule

The repository does not mark a charge live unless a confirmed Solana signature, block slot, and explorer URL are present. Local `simulated:` receipts are for judge replay only.
