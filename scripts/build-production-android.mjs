import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const required = [
  'ALLOWANCE_OS_STORE_FILE',
  'ALLOWANCE_OS_STORE_PASSWORD',
  'ALLOWANCE_OS_KEY_ALIAS',
  'ALLOWANCE_OS_KEY_PASSWORD',
];
const missing = required.filter((name) => !process.env[name]);
if (missing.length > 0) {
  console.error(`Production signing is not configured. Missing: ${missing.join(', ')}`);
  console.error('Nothing was built. Never put these values in the repository or command history.');
  process.exit(2);
}

const androidRoot = resolve('mobile/android');
execFileSync('./gradlew', ['clean', 'testDebugUnitTest', 'assembleRelease', '--no-daemon'], {
  cwd: androidRoot,
  stdio: 'inherit',
  env: process.env,
});

const apk = resolve(androidRoot, 'app/build/outputs/apk/release/allowance-os-0.11.0-release.apk');
if (!existsSync(apk)) throw new Error(`Expected release APK not found: ${apk}`);
const digest = createHash('sha256').update(readFileSync(apk)).digest('hex');
const checksum = resolve(androidRoot, 'app/build/outputs/apk/release/SHA256SUMS.txt');
writeFileSync(checksum, `${digest}  allowance-os-0.11.0-release.apk\n`, { mode: 0o600 });
console.log(JSON.stringify({ apk, sha256: digest, checksum }, null, 2));
