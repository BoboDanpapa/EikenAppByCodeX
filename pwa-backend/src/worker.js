const DEFAULT_MODEL = "gemini-2.5-flash";

function getGeminiEndpoint(model) {
  return `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;
}

function getAllowedOrigins(env) {
  return String(env.ALLOWED_ORIGINS || "")
    .split(",")
    .map(origin => origin.trim())
    .filter(Boolean);
}

function getCorsOrigin(request, env) {
  const origin = request.headers.get("Origin") || "";
  const allowedOrigins = getAllowedOrigins(env);
  if (!origin) return "*";
  if (allowedOrigins.length === 0) return origin;
  return allowedOrigins.includes(origin) ? origin : "";
}

function jsonResponse(request, env, status, body) {
  const corsOrigin = getCorsOrigin(request, env);
  const headers = {
    "Content-Type": "application/json; charset=utf-8",
    "Vary": "Origin"
  };
  if (corsOrigin) headers["Access-Control-Allow-Origin"] = corsOrigin;
  return new Response(JSON.stringify(body), { status, headers });
}

function optionsResponse(request, env) {
  const corsOrigin = getCorsOrigin(request, env);
  const headers = {
    "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
    "Access-Control-Max-Age": "86400",
    "Vary": "Origin"
  };
  if (corsOrigin) headers["Access-Control-Allow-Origin"] = corsOrigin;
  return new Response(null, { status: corsOrigin ? 204 : 403, headers });
}

function cleanText(value, maxLength = 1200) {
  return String(value || "").replace(/\s+/g, " ").trim().slice(0, maxLength);
}

function buildPrompt(question, context) {
  const levelLabel = cleanText(context.levelLabel || "EIKEN", 80);
  const word = cleanText(context.word, 120);
  const jp = cleanText(context.jp, 160);
  const enSent = cleanText(context.enSent, 240);
  const jpSent = cleanText(context.jpSent, 240);
  const inputLanguage = cleanText(context.inputLanguage || "Japanese or English", 80);
  const turns = Number.isFinite(Number(context.turns)) ? Number(context.turns) : 0;
  const maxTurns = Number.isFinite(Number(context.maxTurns)) ? Number(context.maxTurns) : 5;

  return [
    "You are an English learning teacher for a child using a vocabulary card app.",
    "The student may ask in Japanese or English, but you must answer in English only.",
    "Only discuss English learning for the current vocabulary card: meaning, usage, pronunciation, example sentences, similar words, exam understanding, and simple practice.",
    "Do not chat casually. Do not answer unrelated questions. If the student goes off topic, say exactly: I am your English teacher. Let's talk about this word.",
    "Answer the student's exact question first. Do not merely repeat the card definition unless the student asked for the meaning.",
    "If the student's question is unclear because of speech transcription, briefly say what you understood and ask them to repeat it.",
    "Keep the answer brief, warm, and useful for a child. Use 1 to 3 short sentences.",
    `Current course: ${levelLabel}`,
    `Current word or phrase: ${word}`,
    `Japanese meaning for context only: ${jp}`,
    `Example sentence: ${enSent}`,
    `Japanese example meaning for context only: ${jpSent}`,
    `Student selected input language: ${inputLanguage}`,
    `Current question count: ${turns} / ${maxTurns}`,
    `Student question: ${cleanText(question, 600)}`
  ].join("\n");
}

function extractGeminiText(data) {
  const parts = data?.candidates?.[0]?.content?.parts || [];
  return parts.map(part => part.text || "").join("").trim();
}

async function askGemini(question, context, env) {
  if (!env.GEMINI_API_KEY) {
    return { error: "GEMINI_API_KEY is not configured", status: 500 };
  }
  const model = env.GEMINI_MODEL || DEFAULT_MODEL;
  const response = await fetch(getGeminiEndpoint(model), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": env.GEMINI_API_KEY
    },
    body: JSON.stringify({
      contents: [
        {
          role: "user",
          parts: [{ text: buildPrompt(question, context) }]
        }
      ],
      generationConfig: {
        temperature: 0.35,
        maxOutputTokens: 160
      }
    })
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    return {
      error: data?.error?.message || `Gemini request failed with ${response.status}`,
      status: response.status
    };
  }
  const answer = extractGeminiText(data);
  if (!answer) return { error: "Gemini returned an empty answer", status: 502 };
  return { answer };
}

async function handleTeacherAsk(request, env) {
  const body = await request.json().catch(() => ({}));
  const question = cleanText(body.question, 600);
  const context = body.context && typeof body.context === "object" ? body.context : {};
  if (!question) return { error: "question is required", status: 400 };
  if (!cleanText(context.word, 120)) return { error: "context.word is required", status: 400 };
  return askGemini(question, context, env);
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return optionsResponse(request, env);
    const corsOrigin = getCorsOrigin(request, env);
    if (request.headers.get("Origin") && !corsOrigin) {
      return jsonResponse(request, env, 403, { error: "origin is not allowed" });
    }

    const url = new URL(request.url);
    try {
      if (request.method === "GET" && url.pathname === "/health") {
        return jsonResponse(request, env, 200, {
          ok: true,
          model: env.GEMINI_MODEL || DEFAULT_MODEL,
          configured: Boolean(env.GEMINI_API_KEY)
        });
      }
      if (request.method === "POST" && url.pathname === "/api/teacher/ask") {
        const result = await handleTeacherAsk(request, env);
        if (result.error) {
          return jsonResponse(request, env, result.status || 500, { error: result.error });
        }
        return jsonResponse(request, env, 200, { answer: result.answer });
      }
      return jsonResponse(request, env, 404, { error: "not found" });
    } catch (error) {
      return jsonResponse(request, env, 500, { error: error.message || "server error" });
    }
  }
};
