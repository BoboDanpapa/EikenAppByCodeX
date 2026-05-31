# Eiken Magic Words PWA Gemini Backend

This Cloudflare Worker keeps the Gemini API key out of the static PWA and gives `英語の先生` a server-side Gemini endpoint.

## Setup

If you only have a Cloudflare account, start here. These commands create/deploy the Worker from this folder and set the Gemini key as a Cloudflare secret.

```sh
cd pwa-backend
npm install
npx wrangler login
npx wrangler secret put GEMINI_API_KEY
```

Paste the Gemini API key when Wrangler asks for it. Do not commit the key.

For local Worker testing, create `.dev.vars` from the example:

```sh
cp .dev.vars.example .dev.vars
# edit .dev.vars and set GEMINI_API_KEY
npm run dev
```

## Deploy

```sh
cd pwa-backend
npm run deploy
```

Wrangler prints a Worker URL similar to:

```text
https://eiken-magicwords-pwa-backend.<your-subdomain>.workers.dev
```

After deploying, edit `pwa/backend-config.js`:

```js
window.EIKEN_GEMINI_BACKEND_URL = "https://eiken-magicwords-pwa-backend.<your-subdomain>.workers.dev";
```

## Endpoints

- `GET /health`
- `POST /api/teacher/ask`

Request body:

```json
{
  "question": "What does this word mean?",
  "context": {
    "courseId": "level2",
    "levelLabel": "EIKEN 2",
    "word": "encourage",
    "jp": "励ます",
    "enSent": "My teacher encouraged me.",
    "jpSent": "先生が私を励ましてくれました。",
    "turns": 0,
    "maxTurns": 5
  }
}
```

Response body:

```json
{
  "answer": "Encourage means to give someone hope or confidence."
}
```

## Notes

- `GEMINI_API_KEY` is a Worker secret, not a `wrangler.toml` variable.
- `GEMINI_MODEL` and `ALLOWED_ORIGINS` are configured in `wrangler.toml`.
- Keep `ALLOWED_ORIGINS` limited to the GitHub Pages URL and local preview URLs.
- If text Q&A is not enough, continue with a separate Web Live audio backend plan later.
