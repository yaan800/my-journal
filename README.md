# My Journal — Android app (Phase 1: Foundation)

A calm, Keep-style notes app: quick capture first, organize later.
Kotlin + Jetpack Compose + Room. Everything is stored on the phone and works offline.

## What Phase 1 does

- **Quick thought** — the + button (and the home button) opens a writing sheet with the keyboard up.
  The thought is saved on the first keystroke, with no title, folder or tag needed.
  "Open as full note" moves it into the full editor.
- **Notes and checklists** — a full editor that saves as you type (no Save button).
  Leaving a note empty removes it, like Keep. Convert text ⇄ checklist with one tap.
- **Home** — search bar, Quick thought and Checklist buttons, then Pinned and Recent.
  Switch between list and 2-column grid.
- **Pin** (up to 10) and **Delete** with Undo. Deleted notes stay in Trash for 30 days.
- **Search** — titles, text and checklist lines, English and Bangla, offline.
- **Colours** — calm blue-teal on cool paper; follows the phone's light or dark mode.

Done when: you capture and find notes all day with the phone in airplane mode.

## Get the APK without Android Studio (GitHub builds it for you)

You need a free GitHub account. Everything below works in a web browser; no installs.

1. **Create a private repository.** On github.com: *+ → New repository*. Name it `my-journal`,
   choose **Private**, tick *Add a README*, and create it.
2. **Upload the project.** Unzip `my-journal-phase1.zip` on your computer. In the repository:
   *Add file → Upload files*, then drag in everything inside the `my-journal` folder
   (the `app` and `gradle` folders and the files next to them). Commit.
   Then add the build recipe: *Add file → Create new file*, name it exactly
   `.github/workflows/build-apk.yml`, paste in the contents of that file from the zip, and commit.
   (Browsers often skip folders starting with a dot, which is why this one is added by hand.)
3. **Add the signing key as two secrets.** *Settings → Secrets and variables → Actions →
   New repository secret*:
   - `KEYSTORE_BASE64` — paste the whole contents of `myjournal-signing.base64.txt`
   - `KEYSTORE_PASSWORD` — the password from `SIGNING-KEY-README.txt`
4. **Build.** Open the *Actions* tab → *Build APK* → *Run workflow*. It takes about 5–10 minutes.
   Every later commit to `main` builds a new version automatically.
5. **Install.** On your phone, open the repository's *Releases* page (signed in to GitHub),
   tap the newest `MyJournal-build-N.apk`, then open the download. Allow "Install unknown apps"
   for your browser the first time. Later builds install over the old one and keep your notes.

If a build fails, open the failed run, copy the red error lines, and paste them to Claude.

## Run it on your phone with Android Studio (optional)

1. Install **Android Studio** (latest stable).
2. *File → Open* → choose this `my-journal` folder. Wait for "Gradle sync" to finish
   (first time downloads Gradle and libraries; needs internet once).
3. On the phone: *Settings → About phone →* tap *Build number* 7 times, then turn on
   *Developer options → USB debugging*. Connect by USB and allow the computer.
4. Pick your phone in the device list at the top and press ▶ **Run**.

If Android Studio offers to upgrade Gradle, AGP or library versions, it's safe to accept.

## Make an installable APK

1. *Build → Generate Signed App Bundle or APK → APK → Next*.
2. *Create new…* key store. **Back up the .jks file and its passwords in two places** —
   every future update must be signed with the same key, or Android won't install it over
   the old version (and you'd have to uninstall, losing notes).
3. Choose *release*, finish. The APK appears in `app/release/`.
4. Copy it to the phone and open it (allow "Install unknown apps" for your file manager once).

## Tests

- `app/src/test/…/TextFormatTest.kt` — card previews and date labels. Right-click → Run.
- `app/src/androidTest/…/NoteRepositoryTest.kt` — saving, original text kept, English/Bangla/checklist
  search, empty-note discard, pin limit, 30-day trash. Runs on the phone or emulator.

## Where data lives

One SQLite database (`my_journal.db`) in the app's private storage, managed by Room.
Schema version 1. Room writes a copy of each schema to `app/schemas/` — commit it; later phases
use it to write migrations so updates never lose notes.

## Project layout

```
app/src/main/java/com/yeasin/myjournal/
  MyJournalApp.kt        app start + AppContainer (simple dependency wiring)
  MainActivity.kt
  data/db/               Room: NoteEntity, ChecklistItemEntity, NoteDao, AppDatabase
  data/repo/             NoteRepository — the only way screens touch data
  ui/AppNav.kt           home ⇄ editor ⇄ search
  ui/home/ ui/editor/ ui/search/
  ui/components/         NoteCard, QuickCaptureSheet, EmptyState
  ui/theme/              colours (light + dark), typography
  util/TextFormat.kt     previews and "Today · 11:42 AM" labels
docs/prototype/          the canvas prototype — the spec for every later phase
```

## Not in Phase 1 (by design)

Projects, folders, tags, archive/trash screens, swipe and multi-select (Phase 2) ·
AI Inbox and all AI features (Phase 3) · widget, Quick Settings tile, share, voice (Phase 4) ·
backup, app lock, Continue thinking, Daily review (Phase 5).
The database already has the columns these need.
