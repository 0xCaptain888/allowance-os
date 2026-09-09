import { existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { homedir } from 'node:os';
import { join } from 'node:path';

const adbPath = process.env.ANDROID_HOME
  ? join(process.env.ANDROID_HOME, 'platform-tools', 'adb')
  : join(homedir(), 'Library', 'Android', 'sdk', 'platform-tools', 'adb');

const checks = [
  ['typescript sources', existsSync('src/engine.ts')],
  ['native program source', existsSync('program/src/lib.rs')],
  ['browser judge demo', existsSync('site/index.html')],
  ['CI workflow', existsSync('.github/workflows/ci.yml')],
  ['Pages workflow', existsSync('.github/workflows/pages.yml')],
  ['Android MWA client', existsSync('mobile/android/app/src/main/java/com/captain/allowanceos/AllowanceViewModel.kt')],
  ['Android Gradle wrapper', existsSync('mobile/android/gradlew')],
  ['Solana program lockfile', existsSync('program/Cargo.lock')],
  ['Device capability boundary', existsSync('mobile/README.md')],
  ['Solana CLI', Boolean(command('solana', ['--version']))],
  ['cargo-build-sbf', Boolean(command('cargo-build-sbf', ['--version']))],
  ['ADB', existsSync(adbPath) && Boolean(command(adbPath, ['version']))],
  ['Git remote configured', Boolean(command('git', ['remote', 'get-url', 'origin']))],
];

for (const [name, ok] of checks) console.log(`${ok ? 'READY' : 'BLOCKED'}\t${name}`);

function command(program, args) {
  try { execFileSync(program, args, { stdio: 'ignore' }); return true; } catch { return false; }
}
