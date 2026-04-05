KawaiiPet — AI Pet Overlay App for Android
Context
There is no Android app that combines a floating overlay pet + LLM conversations + long-term memory + offline voice (STT/TTS). Desktop has Open-LLM-VTuber, but mobile has a gap. This app fills it.
Stack: Kotlin + Jetpack Compose + Lottie + Sherpa-ONNX + Google Gemini API

Project Structure
kawaii-pet/
├── app/src/main/
│   ├── assets/
│   │   └── lottie/                        # Lottie animation JSON files per expression
│   ├── java/com/kawaiipet/app/
│   │   ├── KawaiiPetApplication.kt        # @HiltAndroidApp entry
│   │   ├── di/
│   │   │   ├── AppModule.kt               # DB, LLM, prefs singletons
│   │   │   └── AudioModule.kt             # Sherpa STT/TTS providers
│   │   ├── ui/
│   │   │   ├── MainActivity.kt            # Permission flow + nav host
│   │   │   ├── theme/                     # Material3 theme
│   │   │   ├── screens/
│   │   │   │   ├── HomeScreen.kt          # "Start Pet" button
│   │   │   │   ├── SettingsScreen.kt      # API key, pet name, model select
│   │   │   │   ├── MemoryScreen.kt        # View/delete stored facts
│   │   │   │   └── ModelDownloadScreen.kt # Download STT/TTS models
│   │   │   └── navigation/
│   │   │       └── AppNavigation.kt
│   │   ├── overlay/                       # ★ Floating overlay system
│   │   │   ├── OverlayService.kt          # Foreground service + WindowManager
│   │   │   ├── OverlayLifecycleOwner.kt   # Lifecycle bridge for Compose in Service
│   │   │   ├── OverlayContent.kt          # Root overlay composable
│   │   │   ├── PetOverlay.kt              # Lottie character + touch handling
│   │   │   ├── ChatBubble.kt              # Speech bubble for responses
│   │   │   ├── TextInputOverlay.kt        # Text box on long press
│   │   │   └── OverlayState.kt            # Sealed class: Idle/Listening/Processing/Speaking/TextInput
│   │   ├── pet/                           # Pet character logic
│   │   │   ├── PetViewModel.kt            # ★ Central coordinator
│   │   │   ├── PetExpression.kt           # Enum: IDLE, HAPPY, SAD, ANGRY, THINKING, TALKING, SLEEPING, LISTENING
│   │   │   ├── PetAnimationController.kt  # Animation state holder
│   │   │   └── PetPersonality.kt          # Personality config
│   │   ├── llm/                           # LLM integration
│   │   │   ├── LlmService.kt             # Interface (swappable providers)
│   │   │   ├── GeminiLlmService.kt        # Google Gemini implementation
│   │   │   ├── ConversationManager.kt     # ★ Orchestrates memory + LLM
│   │   │   ├── FactExtractor.kt           # Extracts facts via secondary LLM call
│   │   │   └── PromptBuilder.kt           # Builds system prompt with personality + facts
│   │   ├── memory/                        # Memory system
│   │   │   ├── db/
│   │   │   │   ├── AppDatabase.kt         # Room database
│   │   │   │   ├── FactEntity.kt          # facts table: text, keywords, category, importance
│   │   │   │   └── FactDao.kt             # Queries by keyword, recent, all
│   │   │   ├── MemoryRepository.kt        # Repository pattern
│   │   │   ├── ShortTermMemory.kt         # Rolling 30-message in-memory list
│   │   │   └── FactMatcher.kt             # Keyword extraction + matching
│   │   ├── audio/                         # Voice pipeline
│   │   │   ├── AudioPipeline.kt           # ★ State machine: IDLE→LISTENING→PROCESSING→SPEAKING
│   │   │   ├── SherpaSTT.kt              # Sherpa-ONNX streaming STT wrapper
│   │   │   ├── SherpaTTS.kt              # Sherpa-ONNX TTS wrapper
│   │   │   ├── AudioRecordManager.kt      # Mic capture at 16kHz
│   │   │   ├── AudioTrackManager.kt       # Playback + amplitude callback for mouth anim
│   │   │   └── ModelManager.kt            # Model download, storage, validation
│   │   └── util/
│   │       ├── PermissionHelper.kt
│   │       └── PreferenceManager.kt       # DataStore for settings
│   └── res/
├── build.gradle.kts
├── gradle/libs.versions.toml              # Version catalog
├── LICENSE
└── README.md

Key Dependencies
| Library | Purpose |
|---|---|
| Jetpack Compose + Material3 | UI framework |
| Hilt | Dependency injection |
| Room | Local database for facts |
| DataStore | Settings persistence |
| Lottie Compose | Character animation |
| Sherpa-ONNX | Local STT + TTS |
| google-genai SDK | Gemini API calls |

Critical Architecture Decisions
Lottie over Rive/Live2D — Lottie is free and battle-tested on Android. One JSON per expression, no state machine complexity. Compose integration via lottie-compose.
Custom OverlayLifecycleOwner — Compose needs a LifecycleOwner but Services don't provide one. A ~40-line bridge class implements LifecycleOwner + SavedStateRegistryOwner + ViewModelStoreOwner. This is the single hardest piece.
Emotion tags in LLM response — Pet appends [happy], [sad] etc. to responses. We parse + strip them to drive Lottie expression changes. Simpler than running a separate sentiment model.
Keyword matching for fact retrieval (MVP) — Facts stored with comma-separated keywords in Room. Query by keyword overlap. Upgrade to embeddings later.
google-genai SDK (not Firebase) — Direct API key auth, no Firebase project setup needed. Swappable via LlmService interface.
Sherpa-ONNX for both STT and TTS — Single library, local/offline, user can choose models. Models downloaded on-demand (40-75MB each).

Key Class Designs
OverlayState (sealed class)
sealed class OverlayState {
    object Idle : OverlayState()
    object Listening : OverlayState()
    data class Processing(val userText: String) : OverlayState()
    data class Speaking(val responseText: String) : OverlayState()
    object TextInput : OverlayState()
    object Minimized : OverlayState()
}
PetExpression (enum)
enum class PetExpression(val lottieAsset: String) {
    IDLE("lottie/pet_idle.json"),
    HAPPY("lottie/pet_happy.json"),
    SAD("lottie/pet_sad.json"),
    ANGRY("lottie/pet_angry.json"),
    THINKING("lottie/pet_thinking.json"),
    TALKING("lottie/pet_talking.json"),
    SLEEPING("lottie/pet_sleeping.json"),
    LISTENING("lottie/pet_listening.json")
}
LlmService (interface — swappable providers)
interface LlmService {
    suspend fun chat(messages: List<ChatMessage>, systemPrompt: String): String
    suspend fun extractFacts(conversationSnippet: String): List<String>
}
ConversationManager (orchestrator)
// Flow:
// 1. Add user message to short-term memory
// 2. Query long-term memory for relevant facts by keyword
// 3. Build system prompt with personality + facts
// 4. Call LLM with short-term history + system prompt
// 5. Add response to short-term memory
// 6. Extract new facts from conversation (fire-and-forget)
// 7. Return response text + emotion tag
FactEntity (Room)
@Entity(tableName = "facts")
data class FactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val factText: String,
    val category: String = "general",
    val keywords: String,           // comma-separated for search
    val createdAt: Long,
    val lastAccessed: Long,
    val importanceScore: Float = 1.0f
)
AudioPipeline (state machine)
// States: IDLE → LISTENING → PROCESSING → SPEAKING → IDLE
// Tap pet → startListening() → mic capture → Sherpa STT → final text
// → processWithLLM() → response text
// → speak() → Sherpa TTS → AudioTrack playback → amplitude → mouth anim
PetViewModel (central coordinator)
// onPetTapped():
//   1. Set LISTENING state + expression
//   2. AudioPipeline.startListening() → user text
//   3. Set THINKING expression
//   4. ConversationManager.processUserInput(text) → response
//   5. Parse emotion tag from response
//   6. Set TALKING expression + start TTS
//   7. AudioPipeline.speak(response)
//   8. Show emotion for 3s → back to IDLE
//
// onPetLongPressed():
//   Show TextInputOverlay → onTextSubmitted() → same flow minus STT

Overlay Service — Key Details
WindowManager LayoutParams
type = TYPE_APPLICATION_OVERLAY
flags = FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_NO_LIMITS (switch to focusable for text input)
format = PixelFormat.TRANSLUCENT
width = WRAP_CONTENT, height = WRAP_CONTENT
Touch Handling
Tap → start listening (STT)
Long press → show text input box
Drag → reposition pet (update WindowManager layout params)
Permissions Required
SYSTEM_ALERT_WINDOW — overlay on top of other apps
RECORD_AUDIO — microphone for STT
FOREGROUND_SERVICE — keep service alive
INTERNET — Gemini API calls
POST_NOTIFICATIONS — foreground service notification (Android 13+)

Lottie Animation Assets
| File | Expression | Description |
|---|---|---|
| pet_idle.json | IDLE | Default relaxed animation |
| pet_happy.json | HAPPY | Smiling, bouncing |
| pet_sad.json | SAD | Droopy expression |
| pet_angry.json | ANGRY | Shaking, pulsing glow |
| pet_thinking.json | THINKING | Loading animation |
| pet_talking.json | TALKING | Talking animation |
| pet_sleeping.json | SLEEPING | Zzz animation |
| pet_listening.json | LISTENING | Attentive pose |

Implementation Phases
Phase 1: Skeleton App (Days 1-2)
Create project with Compose template
Set up libs.versions.toml, Hilt, Navigation
Build HomeScreen, SettingsScreen, PreferenceManager
Milestone: App compiles, stores API key + pet name
Phase 2: Overlay Service (Days 3-5)
Build OverlayLifecycleOwner (lifecycle bridge)
Build OverlayService (foreground service + WindowManager + ComposeView)
Build OverlayContent with a simple draggable circle
Add SYSTEM_ALERT_WINDOW permission flow
Add tap + long press + drag touch handling
Milestone: Draggable circle overlay on top of other apps ★
Phase 3: Lottie Character (Days 6-7)
Create Lottie JSON files per expression (slime character, 8 expression states)
Build PetAnimationController, PetExpression enum
Replace circle with animated Lottie character in overlay
Milestone: Animated pet on screen, expressions switchable
Phase 4: LLM + Text Chat (Days 8-9)
Build LlmService interface + GeminiLlmService
Build ShortTermMemory, PromptBuilder, ConversationManager
Build TextInputOverlay (long press → text box) + ChatBubble
Build PetViewModel text flow: input → LLM → response → expression change
Milestone: Long-press pet → type → get response in bubble → pet emotes
Phase 5: Long-Term Memory (Days 10-11)
Build Room database (FactEntity, FactDao, AppDatabase)
Build FactExtractor (secondary LLM call to pull facts)
Build FactMatcher + MemoryRepository
Update ConversationManager to inject/extract facts
Build MemoryScreen (view + delete facts)
Milestone: Pet remembers your name, preferences across restarts
Phase 6: Voice — Sherpa-ONNX (Days 12-15)
Build ModelManager + ModelDownloadScreen
Build SherpaSTT (streaming recognition) + AudioRecordManager
Build SherpaTTS + AudioTrackManager (with amplitude callback)
Build AudioPipeline state machine
Wire mouth animation to TTS amplitude, glow to listening state
Update PetViewModel: tap → listen → STT → LLM → TTS → speak
Milestone: Full voice loop working. MVP complete ★
Phase 7: Polish (Days 16-18)
Onboarding flow (permissions, API key, model download)
Minimize/dismiss controls
Error handling (no internet, no model, empty API key)
Multi-device testing (API 26+)
GitHub repo setup

Overlay UX Flow
[Pet idle on screen]
    → Tap → Pet glows (LISTENING) → User speaks → STT transcribes
    → Pet thinks (THINKING expression) → Gemini responds
    → Pet talks (TALKING expression, mouth moves) → TTS speaks
    → Pet shows emotion (HAPPY/SAD) for 3s → Back to IDLE
[Pet idle on screen]
    → Long press → Text box appears → User types → Submit
    → Same flow from THINKING onward
[Pet on screen]
    → Drag → Reposition anywhere on screen

Risks & Mitigations
| Risk | Mitigation |
|---|---|
| Compose in Service crashes | OverlayLifecycleOwner handles lifecycle. Test in isolation first. |
| Lottie JSON quality | Each expression is a separate file, easy to replace/update |
| Sherpa-ONNX models are large | Download on demand, not bundled. Progress UI. |
| Service killed by OS | START_STICKY + persistent notification |
| Overlay blocks underlying app touches | FLAG_NOT_FOCUSABLE + small 120dp pet size |
| Battery drain | No proactive behavior. Audio only captures on tap. Service is lightweight when idle. |

Source & Licensing Strategy
Closed source for now. Can open source later — can never un-open-source.
Private GitHub repo
No public code until MVP is validated and monetization is clear
If open sourced later: use AGPL (not MIT) to prevent copying without contributing back
Future Open Core Model (if/when open sourcing)
Core open source: overlay, basic chat, basic memory
Premium closed: advanced characters, cloud memory sync, proactive behavior, custom voices
Memory/personality cloud service as paid tier

Verification / Testing
Overlay: Start service → pet appears on top of other apps → drag works → tap/long-press detected
Lottie: Each expression triggers correct animation → Lottie loops during active state
LLM: Text input → response in bubble → emotion tag parsed → correct expression shown
Memory: Chat about your name → restart app → ask "what's my name?" → pet knows
Voice: Tap → speak "hello" → STT shows text → LLM responds → TTS speaks → mouth animates
Models: Download screen shows models → download completes → STT/TTS work with downloaded model
