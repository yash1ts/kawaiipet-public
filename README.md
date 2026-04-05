# KawaiiPet

AI Pet Overlay App for Android — a floating pet companion with LLM conversations, long-term memory, and offline voice.

## Stack

- **Kotlin + Jetpack Compose** — UI framework
- **Lottie** — Character animation (one JSON per expression)
- **Sherpa-ONNX** — Local/offline STT and TTS
- **Google Gemini API** — LLM conversations
- **Room** — Long-term fact memory
- **Hilt** — Dependency injection

## Setup

1. Open in Android Studio (Hedgehog or newer)
2. Sync Gradle
3. Add your Gemini API key in Settings
4. Lottie animation files are in `app/src/main/assets/lottie/`
5. Build and run on device (API 26+)

## Lottie Animations

The app loads Lottie JSON files from `app/src/main/assets/lottie/` — one per expression:

| File | Expression |
|---|---|
| `pet_idle.json` | Default relaxed animation |
| `pet_happy.json` | Smiling, bouncing |
| `pet_sad.json` | Droopy expression |
| `pet_angry.json` | Shaking, pulsing glow |
| `pet_thinking.json` | Loading/processing |
| `pet_talking.json` | Talking animation |
| `pet_sleeping.json` | Zzz animation |
| `pet_listening.json` | Attentive pose |

## Permissions

- **SYSTEM_ALERT_WINDOW** — Overlay on top of other apps
- **RECORD_AUDIO** — Microphone for STT
- **FOREGROUND_SERVICE** — Keep pet service alive
- **INTERNET** — Gemini API calls
- **POST_NOTIFICATIONS** — Service notification (Android 13+)

## Architecture

```
overlay/     → Floating window system (Service + ComposeView + LifecycleOwner bridge)
pet/         → Pet character logic (ViewModel coordinator, animation controller, expressions)
llm/         → LLM integration (Gemini, conversation orchestrator, fact extraction)
memory/      → Long-term memory (Room DB, keyword matching, short-term rolling buffer)
audio/       → Voice pipeline (Sherpa-ONNX STT/TTS, mic capture, audio playback)
ui/          → Main app screens (Home, Settings, Memory viewer, Model download)
```

## UX Flow

```
[Pet idle on screen]
  → Tap → Pet glows (LISTENING) → Speak → STT transcribes
  → Pet thinks (THINKING) → Gemini responds
  → Pet talks (TALKING, mouth moves) → TTS speaks
  → Pet shows emotion (HAPPY/SAD) for 3s → Back to IDLE

[Pet idle on screen]
  → Long press → Text box appears → Type → Submit
  → Same flow from THINKING onward

[Pet on screen]
  → Drag → Reposition anywhere
  → Double tap → Dismiss pet
```

## Marketing site (Vercel)

The landing page is a **Next.js** app in [`website/`](website/). On Vercel, open **Project → Settings → General → Root Directory**, set it to **`website`**, then redeploy. If Root Directory stays at the repository root, Vercel will not run `next build` for this app and you may see **404 NOT_FOUND**.

Add **`APP_LINK`** or **`APK_LINK`** (and any `NEXT_PUBLIC_*` vars you use) under **Settings → Environment Variables** for production.

## Voice Models

Download STT/TTS models from the Voice Models screen. Models are stored locally and run offline via Sherpa-ONNX.
