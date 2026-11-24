# NILORA LIVE — PHASE 01: Real-Time Voice Translation Engine

**Application:** NILORA (`ONE APP. ONE WORLD. NO LANGUAGE BARRIERS.`)  
**Module:** NILORA LIVE (`com.example.niloralive`)  
**Founder & Creator Attribution:** **Created & Founded by Mohamed Mostafa Taher**

---

## 1. Architecture & Swappable Engine Contracts
NILORA LIVE separates speech capture, translation, validation, and voice synthesis behind clean interfaces in `LiveEngineContracts.kt` so any stage can be swapped or upgraded without rewriting UI or room logic:

- `SpeechRecognitionEngine` (`AndroidSpeechRecognitionEngine.kt`): Wraps `android.speech.SpeechRecognizer` (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`), computes real `onRmsChanged` VAD energy (`rmsNormalized`), separates unstable partial previews from final results, measures capture latency, and maps recognizer error codes into actionable user messages.
- `TranslationEngine` (`EgyptianTranslationEngine.kt`): Uses provisioned Firebase AI Logic (`gemini-3.8-flash` via App Check, zero client API keys) configured with structured JSON output (`translatedText`, `detectedSourceDialect`, `confidence`, `ambiguityWarning`), prioritizing natural **Egyptian Colloquial Arabic (`ar-EG`)** by default with an explicit toggle for **Modern Standard Arabic (`ar-MSA`)**.
- `TranslationValidator` (`EgyptianTranslationEngine.kt`): Verifies non-blank output, catches un-translated echoed text across languages, and checks preservation of numeric values (supporting both ASCII digits and Arabic-Indic numerals `٠–٩`).
- `VoiceGenerationEngine` (`AndroidVoiceGenerationEngine.kt`): Wraps Android `TextToSpeech` + `UtteranceProgressListener` with `QUEUE_FLUSH` overlap prevention, exact inspection of installed `ar-EG` vs generic `ar` vs `en`/`ru`/`fr` voice packages, speed control (`0.6x–1.6x`), volume (`0%–100%`), replay, stop/pause, and mute.
- `AudioCaptureAndHardwareProfiler.kt`: Inspects real device capabilities (`RECORD_AUDIO` permission, `RecognitionService` packages, 16kHz PCM `AudioRecord`, hardware `NoiseSuppressor` / `AcousticEchoCanceler`, RAM/CPU/ABIs) and performs real 16kHz PCM ambient noise measurements in the Diagnostics Lab.
- `SpeechEvaluationHarness.kt`: Computes deterministic Levenshtein Word Error Rate (`computeWordErrorRate`) without ever fabricating scores when a human-verified reference transcript is absent, and exports Markdown evaluation reports.

---

## 2. Pipeline State Machine
Every voice or text translation turn follows explicit pipeline states (`PipelineStage`):
`IDLE` → `LISTENING` → `SPEECH_DETECTED` → `RECOGNIZING` → (`AWAITING_CORRECTION` when auto-translate is off) → `TRANSLATING` → `VALIDATING` → `GENERATING_VOICE` → `PLAYING` → `IDLE` (or `PAUSED` / `ERROR`).

---

## 3. Speech & Translation Model Evaluation Matrix (Free-Cost Initial Architecture)

| Component / Model | License | Approximate Size | Hardware Requirement | Runtime Status in NILORA | Key Trade-offs & Limitations |
|---|---|---|---|---|---|
| **Android `SpeechRecognizer`** | Android Platform API | 0 MB bundled | Android API 24+, Microphone | **Active Default STT** | Depends on installed vendor/Google `RecognitionService` packs; dialect accuracy varies in noisy environments. |
| **Firebase AI Logic (`gemini-3.8-flash`)** | Google Firebase Terms | 0 MB local weights (Cloud + App Check) | Active internet + App Check | **Active Default Translation** | Cloud-dependent; subject to project rate limits/quotas; excels at natural Egyptian Colloquial Arabic (`ar-EG`). |
| **`whisper.cpp` (GGUF)** | MIT | ~75 MB (`tiny` Q5) to ~466 MB (`small`) | `arm64-v8a` NDK JNI (`libwhisper.so`), >=2–4 GB RAM | **Evaluated / Not Bundled** | Base/tiny weights frequently normalize Egyptian dialect into MSA; requires NDK build + external model download. |
| **`faster-whisper` (CTranslate2)** | MIT | ~75 MB to ~1.6 GB (INT8) | Python + CTranslate2 server | **Server-Only Alternative** | Not deployable inside client-only Android APK; requires external hosting beyond free Spark tier. |
| **Meta `NLLB-200-distilled-600M`** | CC-BY-NC 4.0 | ~1.2 GB INT8 / ~2.4 GB FP16 | Server or high-RAM ONNX Runtime | **Evaluated / Not Bundled** | Non-commercial CC-BY-NC license; too heavy for lightweight mobile APK; weaker conversational slang vs instruction-tuned LLMs. |
| **Android `TextToSpeech`** | Android Platform API | 0 MB bundled | Installed system voice packs | **Active Default TTS** | Some devices provide generic `ar-*` voices rather than dedicated `ar-EG`; NILORA reports this honestly per device. |

---

## 4. Privacy, Consent & Two-Phone Room Transport Honesty
- **Explicit Consent Gate:** Before capturing speech or sending transcripts to Firebase AI Logic, users must explicitly grant consent via the NILORA LIVE consent card (`voiceTransmissionConsent` stored in DataStore and revocable at any time).
- **Audio Storage:** NILORA never uploads or stores raw PCM/microphone audio files in Cloud Firestore or Firebase Storage. Room text transcripts are temporary unless the host opts into `retainTranscript`.
- **Two-Phone Room Transport:** Synchronizes room membership, spoken languages, and translated messages in real time via **Cloud Firestore snapshot listeners** (`rooms/{roomCode}` and `messages`), with local Android TTS playback on each participant's device. Raw P2P WebRTC audio media streaming (which requires STUN/TURN infrastructure) is not bundled in this Spark-tier APK and is explicitly disclosed in the UI.

---

## 5. Real-Device Human Verification Checklist
Because the cloud build environment is headless and lacks physical microphones, speakers, or paired Android hardware, execute this checklist on two physical Android devices before production release:
1. Verify `RECORD_AUDIO` runtime permission prompt, allow/deny UX, and `AudioRecord` ambient noise floor check in **Diagnostics & Evaluation Lab**.
2. Verify `AndroidVoiceGenerationEngine` voice report on target hardware (check whether `ar-EG` or generic `ar` voice is installed).
3. Test **Egyptian Colloquial (`ar-EG`)** <-> **Russian (`ru`)**, **English (`en`)**, and **French (`fr`)** in quiet and noisy rooms; verify numbers/prices (`500`, `10:30`) survive translation via `TranslationValidator`.
4. Test **One-Phone Split Mode** (both 180° rotated and upright toggle), transcript editing (`autoTranslateAfterSpeech = false`), replay, stop, speed (`0.6x–1.6x`), and volume (`0%–100%`).
5. Test **Two-Phone Room Mode** across two real devices (`Create Room` -> share invite link `https://nilora.app/join/<code>` -> `Join Room` -> `Leave` vs `End Room` destruction).
