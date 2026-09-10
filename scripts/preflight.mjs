import { createHash } from 'node:crypto';
import { existsSync, readFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { homedir } from 'node:os';
import { join } from 'node:path';

const adbPath = process.env.ANDROID_HOME
  ? join(process.env.ANDROID_HOME, 'platform-tools', 'adb')
  : join(homedir(), 'Library', 'Android', 'sdk', 'platform-tools', 'adb');
const solanaPath = process.env.SOLANA_BIN
  ?? join(homedir(), '.local', 'share', 'solana', 'install', 'active_release', 'bin', 'solana');
const cargoBuildSbfPath = process.env.CARGO_BUILD_SBF_BIN
  ?? join(homedir(), '.local', 'share', 'solana', 'install', 'active_release', 'bin', 'cargo-build-sbf');
const sourceBuildEvidence = readJson('evidence/delegated-v2-source-build.json');
const sourceFileEntries = Object.entries(sourceBuildEvidence?.sourceFiles ?? {});
const sourceHashesMatch = sourceFileEntries.length > 0 && sourceFileEntries.every(([path, expected]) => (
  existsSync(path) && sha256(path) === expected
));
const sbfArtifactPath = sourceBuildEvidence?.builds?.localMacOs?.artifact;
const sbfArtifactMatches = !sbfArtifactPath || !existsSync(sbfArtifactPath) || (
  sha256(sbfArtifactPath) === sourceBuildEvidence?.builds?.localMacOs?.sha256
  && readFileSync(sbfArtifactPath).byteLength === sourceBuildEvidence?.builds?.localMacOs?.sizeBytes
);
const sbfBuilderVersion = commandOutput(cargoBuildSbfPath, ['--version']);

const checks = [
  ['typescript sources', existsSync('src/engine.ts')],
  ['native program source', existsSync('program/src/lib.rs')],
  ['browser judge demo', existsSync('site/index.html')],
  ['CI workflow', existsSync('.github/workflows/ci.yml')],
  ['Pages workflow', existsSync('.github/workflows/pages.yml')],
  ['Android MWA client', existsSync('mobile/android/app/src/main/java/com/captain/allowanceos/AllowanceViewModel.kt')],
  ['Android Gradle wrapper', existsSync('mobile/android/gradlew')],
  ['Solana program lockfile', existsSync('program/Cargo.lock')],
  ['Live Devnet Program evidence', existsSync('evidence/live-devnet-program.json')],
  ['Evidence truth index', existsSync('evidence/index.json')],
  ['Merchant SDK runtime', existsSync('src/sdk.ts') && existsSync('src/runtime.ts')],
  ['AlphaBrief reference integration', existsSync('examples/alphabrief/integration.ts')],
  ['Security and privacy disclosures', existsSync('docs/security.md') && existsSync('docs/privacy-policy.md')],
  ['Security reporting and operations runbook', existsSync('SECURITY.md') && existsSync('docs/operations.md')],
  ['dApp Store submission pack', existsSync('dapp-store/listing.json') && existsSync('docs/dapp-store-submission.md')],
  ['Product maturity audit', existsSync('docs/product-maturity-audit.md')],
  ['Delegated Settlement v2 specification', existsSync('docs/delegated-settlement-v2.md')],
  ['Delegated v2 TypeScript builders', existsSync('src/delegated-protocol.ts')],
  ['Delegated v2 Devnet runner', existsSync('scripts/run-delegated-devnet.ts')],
  ['Delegated v2 control-matrix runner', existsSync('scripts/run-delegated-devnet-matrix.ts')],
  ['Delegated v2 source-build evidence', existsSync('evidence/delegated-v2-source-build.json')],
  ['Delegated v2 live Devnet evidence', existsSync('evidence/live-devnet-v2.json')],
  ['Delegated v2 source hashes match evidence', sourceHashesMatch],
  ['Local SBF artifact matches evidence when present', sbfArtifactMatches],
  ['Pinned cargo-build-sbf 4.3.0', sbfBuilderVersion.includes('cargo-build-sbf 4.3.0')],
  ['Encrypted Android session store', existsSync('mobile/android/app/src/main/java/com/captain/allowanceos/SecureSessionStore.kt')],
  ['Production Android builder', existsSync('scripts/build-production-android.mjs')],
  ['Device capability boundary', existsSync('mobile/README.md')],
  ['Solana CLI', Boolean(command('solana', ['--version']) || command(solanaPath, ['--version']))],
  ['cargo-build-sbf', Boolean(command('cargo-build-sbf', ['--version']) || command(cargoBuildSbfPath, ['--version']))],
  ['ADB', existsSync(adbPath) && Boolean(command(adbPath, ['version']))],
  ['Git remote configured', Boolean(command('git', ['remote', 'get-url', 'origin']))],
];

for (const [name, ok] of checks) console.log(`${ok ? 'READY' : 'BLOCKED'}\t${name}`);

function command(program, args) {
  try { execFileSync(program, args, { stdio: 'ignore' }); return true; } catch { return false; }
}

function commandOutput(program, args) {
  try { return execFileSync(program, args, { encoding: 'utf8' }); } catch { return ''; }
}

function sha256(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

function readJson(path) {
  try { return JSON.parse(readFileSync(path, 'utf8')); } catch { return null; }
}
