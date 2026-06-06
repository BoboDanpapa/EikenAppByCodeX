import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";

const pwaDir = path.resolve(process.cwd(), process.argv[2] || "pwa");
const html = fs.readFileSync(path.join(pwaDir, "index.html"), "utf8");

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function extractInlineScripts(source) {
  return [...source.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g)].map(match => match[1]);
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

function wait(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

const scriptSource = extractInlineScripts(html).join("\n");

class FakeRecognition {
  static instances = [];

  constructor() {
    FakeRecognition.instances.push(this);
    this.lang = "";
    this.interimResults = false;
    this.maxAlternatives = 0;
    this.continuous = true;
    this.stopped = false;
  }

  start() {
    this.onstart?.();
  }

  stop() {
    if (this.stopped) return;
    this.stopped = true;
    this.onend?.();
  }

  emitResult(transcript, isFinal, confidence = 0.9) {
    const result = [{ transcript, confidence }];
    result.isFinal = isFinal;
    this.onresult?.({ resultIndex: 0, results: [result] });
  }

  emitError(error) {
    this.onerror?.({ error });
    this.onend?.();
  }
}

function createSandbox() {
  const status = { innerText: "" };
  const answeredQuestions = [];
  FakeRecognition.instances = [];

  const sandbox = {
    window: {
      SpeechRecognition: FakeRecognition,
      webkitSpeechRecognition: FakeRecognition,
      setTimeout,
      clearTimeout
    },
    teacherShouldKeepListening: true,
    teacherRecognition: null,
    teacherSpeaking: false,
    teacherAnswerInFlight: false,
    teacherRecognitionHeardText: "",
    teacherRecognitionHeardConfidence: 0,
    teacherRecognitionLanguageIndex: 1,
    teacherListening: true,
    teacherTurns: 0,
    teacherPendingStudentQuestion: null,
    teacherAnswerSeenForPendingQuestion: false,
    teacherRecognitionRestartId: null,
    TEACHER_RECOGNITION_LANGUAGES: ["ja-JP", "en-US"],
    TEACHER_LOW_CONFIDENCE_THRESHOLD: 0.45,
    TEACHER_RECOGNITION_FINISH_DELAY_MS: 5,
    TEACHER_RECOGNITION_INTERIM_SUBMIT_MS: 5,
    TEACHER_RECOGNITION_MAX_LISTEN_MS: 25,
    TEACHER_MAX_QUESTIONS: 5,
    document: {
      getElementById(id) {
        if (id === "teacher-language-select") return { value: "en-US" };
        if (id === "teacher-status") return status;
        return null;
      }
    },
    renderTeacherDemo() {},
    stopTeacherTimer() {},
    answerBrowserTeacherQuestion(question) {
      answeredQuestions.push(question);
      sandbox.teacherAnswerInFlight = true;
    }
  };

  sandbox.window.window = sandbox.window;
  vm.createContext(sandbox);
  for (const fn of [
    "getSpeechRecognitionConstructor",
    "getTeacherRecognitionLanguage",
    "getTeacherRecognitionLanguageLabel",
    "shouldRetryTeacherRecognition",
    "startTeacherRecognitionCycle"
  ]) {
    vm.runInContext(extractFunction(scriptSource, fn), sandbox);
  }
  return { sandbox, status, answeredQuestions };
}

{
  const { sandbox, answeredQuestions } = createSandbox();
  sandbox.startTeacherRecognitionCycle(sandbox.document.getElementById("teacher-status"));
  const recognition = FakeRecognition.instances[0];
  assert(recognition.continuous === false, "Recognition should not run in continuous mode");
  recognition.emitResult("give me another example", true, 0.9);
  await wait(20);
  assert(answeredQuestions[0] === "give me another example", "Final speech result was not submitted");
  assert(sandbox.teacherShouldKeepListening === false, "Final speech path should stop listening");
}

{
  const { sandbox, answeredQuestions } = createSandbox();
  sandbox.startTeacherRecognitionCycle(sandbox.document.getElementById("teacher-status"));
  FakeRecognition.instances[0].emitResult("what does this word mean", false, 0);
  await wait(25);
  assert(answeredQuestions[0] === "what does this word mean", "Stable interim speech was not submitted");
  assert(sandbox.teacherShouldKeepListening === false, "Interim speech path should stop listening");
}

{
  const { sandbox, status, answeredQuestions } = createSandbox();
  sandbox.startTeacherRecognitionCycle(sandbox.document.getElementById("teacher-status"));
  FakeRecognition.instances[0].stop();
  await wait(5);
  assert(answeredQuestions.length === 0, "Empty recognition should not submit a question");
  assert(sandbox.teacherListening === false, "Empty recognition should release listening state");
  assert(status.innerText.includes("もう一度マイク"), "Empty recognition should tell the user to retry");
}

{
  const { sandbox, status, answeredQuestions } = createSandbox();
  sandbox.startTeacherRecognitionCycle(sandbox.document.getElementById("teacher-status"));
  FakeRecognition.instances[0].emitError("no-speech");
  await wait(5);
  assert(answeredQuestions.length === 0, "Recognition errors should not submit a question");
  assert(sandbox.teacherListening === false, "Recognition errors should release listening state");
  assert(status.innerText.includes("もう一度マイク"), "Recognition errors should tell the user to retry");
}

console.log(`PWA teacher microphone tests passed at ${pwaDir}`);
