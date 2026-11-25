
// Load generator for SiMu: closed-loop HTTP load over the API gateway.
// Usage:
//   node scripts/load/simu-load.js prepare 50        # create users, store tokens.json
//   node scripts/load/simu-load.js run 32 45 reads   # 32 workers, 45s, mixed reads
//   node scripts/load/simu-load.js run 8 30 signin   # sign-in throughput (bcrypt bound)
const http = require('http');
const fs = require('fs');
const { URL } = require('url');

const agent = new http.Agent({ keepAlive: true, maxSockets: 512 });

function call(method, url, opts = {}) {
  return new Promise((resolve) => {
    const u = new URL(url);
    const data = opts.body ? JSON.stringify(opts.body) : null;
    const headers = {};
    if (opts.body) { headers['Content-Type'] = 'application/json'; headers['Content-Length'] = Buffer.byteLength(data); }
    if (opts.token) headers['Authorization'] = 'Bearer ' + opts.token;
    const started = process.hrtime.bigint();
    const req = http.request({ hostname: u.hostname, port: u.port, path: u.pathname + u.search, method, headers, agent }, (res) => {
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => {
        const ms = Number(process.hrtime.bigint() - started) / 1e6;
        resolve({ status: res.statusCode, ms, text: Buffer.concat(chunks).toString() });
      });
    });
    req.on('error', (e) => resolve({ status: -1, ms: 0, text: '', error: e.code || e.message }));
    req.setTimeout(30000, () => req.destroy(new Error('timeout')));
    if (data) req.write(data);
    req.end();
  });
}

function pct(sorted, p) {
  if (!sorted.length) return 0;
  return sorted[Math.min(sorted.length - 1, Math.floor((p / 100) * sorted.length))];
}

async function prepare(base, n) {
  const tokens = [];
  const users = [];
  const stamp = Date.now();
  for (let i = 0; i < n; i++) {
    const username = 'l' + stamp.toString(36) + i;
    const res = await call('POST', base + '/api/v1/auth/signup', { body: { username, email: username + '@example.com', password: 'Password123' } });
    if (res.status === 200 || res.status === 201) {
      try { tokens.push(JSON.parse(res.text).token); users.push(username); } catch (e) {}
    } else {
      console.log('signup failed', res.status, res.text.slice(0, 120));
    }
  }
  return { tokens, users };
}

async function run(base, tokens, users, concurrency, seconds, mode) {
  const deadline = Date.now() + seconds * 1000;
  const lat = [];
  const errors = {};
  let count = 0;
  let bytes = 0;

  async function worker(id) {
    let i = id;
    while (Date.now() < deadline) {
      const token = tokens[(i++) % tokens.length];
      let res;
      if (mode === 'signin') {
        res = await call('POST', base + '/api/v1/auth/signin', { body: { username: users[(i++) % users.length], password: 'Password123' } });
      } else {
        const r = Math.random();
        if (r < 0.55) res = await call('GET', base + '/api/v1/music', { token });
        else if (r < 0.85) res = await call('GET', base + '/api/v1/me/playlists', { token });
        else res = await call('GET', base + '/api/v1/users', { token });
      }
      count++;
      lat.push(res.ms);
      bytes += (res.text || '').length;
      if (res.status !== 200 && res.status !== 201) {
        const k = res.status + (res.error ? ':' + res.error : '');
        errors[k] = (errors[k] || 0) + 1;
      }
    }
  }

  const started = Date.now();
  await Promise.all(Array.from({ length: concurrency }, (_, i) => worker(i)));
  const elapsed = (Date.now() - started) / 1000;
  lat.sort((a, b) => a - b);
  const avg = lat.reduce((s, x) => s + x, 0) / (lat.length || 1);
  return {
    mode, concurrency, seconds: Math.round(elapsed),
    requests: count,
    rps: +(count / elapsed).toFixed(1),
    avg_ms: +avg.toFixed(1),
    p50: +pct(lat, 50).toFixed(1),
    p95: +pct(lat, 95).toFixed(1),
    p99: +pct(lat, 99).toFixed(1),
    max_ms: +pct(lat, 100).toFixed(1),
    errors,
    avg_response_kb: +(bytes / count / 1024).toFixed(1),
  };
}

(async () => {
  const [cmd, ...rest] = process.argv.slice(2);
  const base = 'http://localhost:8081';
  if (cmd === 'prepare') {
    const n = Number(rest[0] || 50);
    const data = await prepare(base, n);
    fs.writeFileSync(__dirname + '/tokens.json', JSON.stringify(data));
    console.log(JSON.stringify({ prepared: data.tokens.length }));
  } else if (cmd === 'run') {
    const [c, s, mode] = [Number(rest[0] || 32), Number(rest[1] || 45), rest[2] || 'reads'];
    const data = JSON.parse(fs.readFileSync(__dirname + '/tokens.json', 'utf8'));
    const out = await run(base, data.tokens, data.users, c, s, mode);
    console.log(JSON.stringify(out));
  }
})();