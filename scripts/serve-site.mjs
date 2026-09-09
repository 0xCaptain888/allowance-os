import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const root = resolve(new URL('../site/', import.meta.url).pathname);
const port = Number(process.env.PORT || 4178);
const server = createServer(async (_req, res) => {
  try {
    const body = await readFile(resolve(root, 'index.html'));
    res.writeHead(200, { 'content-type': 'text/html; charset=utf-8', 'cache-control': 'no-store' });
    res.end(body);
  } catch {
    res.writeHead(404);
    res.end('Not found');
  }
});
server.listen(port, '127.0.0.1', () => console.log(`Allowance OS demo: http://127.0.0.1:${port}`));
