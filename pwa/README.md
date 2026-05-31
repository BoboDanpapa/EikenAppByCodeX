# Eiken Magic Words PWA

英検4級・3級・準2級・2級・準1級・1級の単語と熟語を楽しく覚える子ども向けPWAです。

## Local Preview

```sh
python3 -m http.server 8766
```

Then open:

```text
http://localhost:8766/index.html
```

## GitHub Pages

Deploy this repository from `main` branch and `/ (root)`.

## Gemini Teacher Backend

The PWA can call the Cloudflare Worker backend in `../pwa-backend/` for the `英語の先生` feature. Keep the Gemini API key in a Worker secret only; do not put it in this static PWA repository.

After deploying the backend, set the endpoint in `backend-config.js`:

```js
window.EIKEN_GEMINI_BACKEND_URL = "https://your-backend.example.com";
```

If this value is empty, the PWA uses the offline fallback teacher.
