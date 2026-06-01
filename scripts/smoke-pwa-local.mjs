import { spawn } from "node:child_process";
import fs from "node:fs";
import path from "node:path";

const pwaDir = path.resolve(process.cwd(), process.argv[2] || "pwa");
const port = Number(process.env.PWA_SMOKE_PORT || 18766);
const baseUrl = `http://127.0.0.1:${port}`;

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

async function waitForServer() {
  const deadline = Date.now() + 10000;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(`${baseUrl}/index.html?smoke=${Date.now()}`, { cache: "no-store" });
      if (response.ok) return;
    } catch {
      // Retry until the local static server is ready.
    }
    await new Promise(resolve => setTimeout(resolve, 200));
  }
  throw new Error("Timed out waiting for local PWA server");
}

const server = spawn("python3", ["-m", "http.server", String(port), "--bind", "127.0.0.1"], {
  cwd: pwaDir,
  stdio: "ignore"
});

try {
  await waitForServer();
  const [html, serviceWorker, backendConfig] = await Promise.all([
    fetch(`${baseUrl}/index.html?smoke=${Date.now()}`, { cache: "no-store" }).then(response => response.text()),
    fetch(`${baseUrl}/service-worker.js?smoke=${Date.now()}`, { cache: "no-store" }).then(response => response.text()),
    fetch(`${baseUrl}/backend-config.js?smoke=${Date.now()}`, { cache: "no-store" }).then(response => response.text())
  ]);
  const version = html.match(/<div class="version-badge">(v[\d.]+)<\/div>/)?.[1];
  assert(version, "Smoke test could not find PWA version badge");
  assert(serviceWorker.includes(`eiken-magicwords-pwa-${version}`), "Smoke test found a version/cache mismatch");
  assert(html.includes("teacher-language-select"), "Smoke test could not find teacher language selector");
  assert(html.includes("英検準2級"), "Smoke test could not find course selector content");
  assert(backendConfig.includes("EIKEN_GEMINI_BACKEND_URL"), "Smoke test could not find backend config");
  console.log(`PWA local smoke passed for ${version} at ${baseUrl}`);
} finally {
  server.kill();
}
