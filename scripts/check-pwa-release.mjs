import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";

const args = new Map();
for (let i = 2; i < process.argv.length; i += 1) {
  const arg = process.argv[i];
  if (!arg.startsWith("--")) continue;
  const key = arg.slice(2);
  const next = process.argv[i + 1];
  if (next && !next.startsWith("--")) {
    args.set(key, next);
    i += 1;
  } else {
    args.set(key, "true");
  }
}

const root = process.cwd();
const pwaDir = path.resolve(root, args.get("pwa-dir") || "pwa");
const backendWorkerPath = args.get("backend-worker")
  ? path.resolve(root, args.get("backend-worker"))
  : path.resolve(root, "pwa-backend/src/worker.js");
const checkBackend = args.get("pwa-only") !== "true" && fs.existsSync(backendWorkerPath);

function readText(filePath) {
  return fs.readFileSync(filePath, "utf8");
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function extractInlineScripts(html) {
  return [...html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g)].map(match => match[1]);
}

function extractFunction(source, name) {
  const start = source.indexOf(`function ${name}(`);
  assert(start >= 0, `Missing function ${name}`);
  let index = source.indexOf("{", start);
  let depth = 0;
  for (; index < source.length; index += 1) {
    if (source[index] === "{") depth += 1;
    if (source[index] === "}") {
      depth -= 1;
      if (depth === 0) return source.slice(start, index + 1);
    }
  }
  throw new Error(`Unclosed function ${name}`);
}

function getVersion(html) {
  const match = html.match(/<div class="version-badge">(v[\d.]+)<\/div>/);
  assert(match, "Missing version badge");
  return match[1];
}

function getCacheVersion(serviceWorker) {
  const match = serviceWorker.match(/const CACHE_VERSION = "eiken-magicwords-pwa-(v[\d.]+)"/);
  assert(match, "Missing service worker cache version");
  return match[1];
}

const html = readText(path.join(pwaDir, "index.html"));
const serviceWorker = readText(path.join(pwaDir, "service-worker.js"));
const backendConfig = readText(path.join(pwaDir, "backend-config.js"));
const inlineScripts = extractInlineScripts(html);

assert(inlineScripts.length > 0, "No inline scripts found in PWA HTML");
for (const script of inlineScripts) new Function(script);

const version = getVersion(html);
const cacheVersion = getCacheVersion(serviceWorker);
assert(version === cacheVersion, `Version mismatch: HTML ${version}, SW ${cacheVersion}`);
assert(backendConfig.includes("EIKEN_GEMINI_BACKEND_URL"), "Missing backend URL config symbol");
assert(/workers\.dev|localhost|127\.0\.0\.1|""/.test(backendConfig), "Backend config does not look intentional");

for (const required of [
  "teacher-language-select",
  "日本語",
  "English",
  "teacherConversationHistory",
  "history: teacherConversationHistory",
  "isCompleteTeacherAnswer",
  "shouldRejectTeacherAnswer",
  "buildNewExampleFallback"
]) {
  assert(html.includes(required), `Missing PWA teacher requirement: ${required}`);
}

for (const forbidden of ["文字で質問", "teacher-day-remaining", "<span>のこり</span>"]) {
  assert(!html.includes(forbidden), `Forbidden UI returned: ${forbidden}`);
}

for (const required of [
  "isNavigationRequest",
  "networkFirst",
  "staleWhileRevalidate",
  "cacheFirst"
]) {
  assert(serviceWorker.includes(required), `Missing service worker strategy helper: ${required}`);
}

const scriptSource = inlineScripts.join("\n");
const sandbox = {
  teacherConversationHistory: [],
  TEACHER_HISTORY_MAX_MESSAGES: 8,
  getTeacherContext: () => ({
    word: "a lot of",
    jp: "たくさんの",
    enSent: "I have a lot of books.",
    jpSent: "私はたくさんの本を持っています。"
  })
};
vm.createContext(sandbox);
for (const fn of [
  "isCompleteTeacherAnswer",
  "normalizeTeacherText",
  "isNewExampleRequest",
  "shouldRejectTeacherAnswer",
  "buildNewExampleFallback",
  "buildFallbackTeacherAnswer",
  "addTeacherConversationTurn"
]) {
  vm.runInContext(extractFunction(scriptSource, fn), sandbox);
}

assert(!sandbox.isCompleteTeacherAnswer('The opposite of "a lot of" is'), "Incomplete Gemini answer was accepted");
assert(sandbox.isCompleteTeacherAnswer("The opposite is a few books."), "Complete Gemini answer was rejected");
assert(sandbox.isNewExampleRequest("他の例文を作ってください"), "Japanese new-example request was not detected");
assert(sandbox.isNewExampleRequest("Give me another example sentence."), "English new-example request was not detected");
assert(
  sandbox.shouldRejectTeacherAnswer("他の例文をください", "Here is another example: I have a lot of books."),
  "Repeated original example was not rejected"
);
const fallback = sandbox.buildFallbackTeacherAnswer("他の例文をください");
assert(fallback.includes("a lot of stars"), `Unexpected new-example fallback: ${fallback}`);
assert(!fallback.includes("I have a lot of books"), "Fallback repeated the original example");
sandbox.teacherConversationHistory.push({ role: "teacher", text: fallback });
assert(
  sandbox.shouldRejectTeacherAnswer("もう一つ例文をください", fallback),
  "Repeated prior teacher example was not rejected"
);
const secondFallback = sandbox.buildFallbackTeacherAnswer("もう一つ例文をください");
assert(secondFallback.includes("stickers"), `Second fallback did not rotate examples: ${secondFallback}`);
assert(!secondFallback.includes("a lot of stars"), "Second fallback repeated the prior teacher example");

sandbox.teacherConversationHistory = [];
for (let i = 1; i <= 5; i += 1) sandbox.addTeacherConversationTurn(`q${i}`, `a${i}`);
assert(sandbox.teacherConversationHistory.length === 8, "Conversation history was not trimmed to 8 messages");
assert(sandbox.teacherConversationHistory[0].text === "q2", "Conversation history did not keep the most recent turns");
assert(sandbox.teacherConversationHistory[7].text === "a5", "Conversation history last answer is wrong");

if (checkBackend) {
  const backendSource = readText(backendWorkerPath);
  new Function(backendSource.replace(/export default\s*\{[\s\S]*$/, ""));
  for (const required of [
    "cleanHistory",
    "getQuestionIntent",
    "Detected student intent",
    "Never repeat an example sentence you already gave",
    "Recent conversation for this same word",
    "Use the recent conversation",
    "Do not reuse the provided example sentence",
    "Only discuss English learning for the current vocabulary card",
    "maxOutputTokens: 420"
  ]) {
    assert(backendSource.includes(required), `Missing backend teacher requirement: ${required}`);
  }
}

console.log(`PWA release checks passed for ${version} at ${pwaDir}`);
