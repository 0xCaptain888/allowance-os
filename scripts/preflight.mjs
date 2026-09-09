import { existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';

const checks = [
  ['typescript sources', existsSync('src/engine.ts')],
  ['native program source', existsSync('program/src/lib.rs')],
  ['browser judge demo', existsSync('site/index.html')],
  ['CI workflow', existsSync('.github/workflows/ci.yml')],
  ['Seeker mobile boundary', existsSync('mobile/README.md')],
  ['Solana CLI', Boolean(command('solana', ['--version']))],
  ['cargo-build-sbf', Boolean(command('cargo-build-sbf', ['--version']))],
  ['ADB', Boolean(command('adb', ['version']))],
  ['GitHub auth', Boolean(command('gh', ['auth', 'status']))],
];

for (const [name, ok] of checks) console.log(`${ok ? 'READY' : 'BLOCKED'}\t${name}`);

function command(program, args) {
  try { execFileSync(program, args, { stdio: 'ignore' }); return true; } catch { return false; }
}
