# Working on My Journal (for Claude Code)

## What this is
A personal Android journal/notes app. Google Keep simplicity + projects, tags, folders, search,
and AI that suggests but never forces. Owner builds it phase by phase with Claude Code.

## The spec
`docs/prototype/prototype-v0.4.dc.html` is the source of truth for screens, wording, flows and
colours. Match it. When the prototype and this file disagree, ask.

## Stack
Kotlin, Jetpack Compose (Material 3), Room (KSP), Navigation Compose, coroutines/Flow.
minSdk 29, targetSdk 35. Dependency wiring is the hand-written `AppContainer` (Hilt may come in Phase 3).

## Rules that never bend
1. Never lose a note. No destructive migrations. Every schema change = version bump + Migration + test.
2. `originalContent` is the raw thought. Nothing — especially AI — writes to it after capture.
3. AI output is stored separately, labelled with its provider, and changes a note only after the user accepts. Every AI change is revertible.
4. Note creation, editing and search must work with no internet.
5. Quick capture saves on the first keystroke and asks for nothing.
6. English and Bangla must work everywhere; add a Bangla case to any search or text test.
7. Colours come from `ui/theme`. Keep text contrast at WCAG AA. No gradients, glows or emoji in the UI.

## Phases (see the build plan doc)
1. Foundation — DONE in this commit.
2. Organize — projects, folders (nested), tags, archive, trash screen, swipe right=pin / left=archive,
   long-press multi-select, FTS5 search + filters (tag, project, folder, date, pinned), timeline, bottom nav
   Notes · Projects · Tags · More. Settings: theme, default view, pin limit 5/10 (DataStore).
3. AI — Cloudflare Worker holding the Claude key; AiService with ClaudeProvider + DemoProvider; AI Inbox;
   organize → review → "Save as"; Ask, Challenge, Clean up, Summarize…; organized brainstorm; consent dialog;
   history + revert. Prompts and JSON shapes: copy from the prototype's `TASKS()`.
4. Capture anywhere — Glance widget, Quick Settings tile, share target, voice (SpeechRecognizer).
5. Trust and polish — Drive backup, export/import, biometric lock, Continue thinking, Daily review, links.

## Working style
- One phase per branch. Write tests with each feature. Run on the owner's phone before merging.
- Keep files small and named like the folders above.
