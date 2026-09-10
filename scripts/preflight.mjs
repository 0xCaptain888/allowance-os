import { existsSync } from 'node:fs';
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
  ['Merchant SDK runtime', existsSync('src/sdk.ts') && existsSync('src/runtime.ts')],
  ['AlphaBrief reference integration', existsSync('examples/alphabrief/integration.ts')],
  ['Security and privacy disclosures', existsSync('docs/security.md') && existsSync('docs/privacy-policy.md')],
  ['dApp Store submission pack', existsSync('dapp-store/listing.json') && existsSync('docs/dapp-store-submission.md')],
  ['Product maturity audit', existsSync('docs/product-maturity-audit.md')],
  ['Delegated Settlement v2 specification', existsSync('docs/delegated-settlement-v2.md')],
  ['Delegated v2 TypeScript builders', existsSync('src/delegated-protocol.ts')],
  ['Delegated v2 Devnet runner', existsSync('scripts/run-delegated-devnet.ts')],
  ['Delegated v2 source-build evidence', existsSync('evidence/delegated-v2-source-build.json')],
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
