# My Journal — Android app (Phase 2: Organize)

A calm, Keep-style notes app: quick capture first, organize later.
Kotlin + Jetpack Compose + Room. Everything is stored on the phone and works offline.

## What the app does now

**Phase 1 — Foundation**
- **Quick thought**: the + button opens a writing sheet with the keyboard up; saved on the first keystroke.
- **Notes and checklists** that save as you type. Empty notes are removed when you go back.
- **Pin** and **delete with Undo**; Trash keeps notes 30 days.

**Phase 2 — Organize (this update)**
- **Bottom bar:** Notes · Projects · Tags · More.
- **Projects** (each with its own soft colour), **nested folders** and **unlimited tags** — all optional.
  Set them from chips at the bottom of any note, or for many notes at once.
- **Swipe** a note right to pin, left to archive. **Long-press** to select several, then
  Pin · Archive · Delete · ⋮ (project, folder, tags, select all).
- **Archive** and **Trash** screens (restore, delete forever, empty trash).
- **Search** across titles, text, checklists, tags, projects and folders, with filters you can combine:
  tags, project, folder, date (today … this month, or a custom range) and pinned. Offline, English and Bangla.
- **Timeline**: notes by the day you wrote them, with "jump to date".
- **Settings**: light / dark / system, default list or grid view, pin limit 5 or 10.
- Ten starter tags (Rumor, AI, Research, Market …) are added on first launch.

Updating from Phase 1 keeps every note: the database upgrade only adds new tables.

## Updating the app on GitHub (when you get a new version)

1. Unzip the new project zip.
2. In your repository: *Add file → Upload files*, drag in everything inside the `my-journal` folder,
   and commit. Files with the same name are replaced.
3. If `.github/workflows/build-apk.yml` changed, open it in the repository, click the pencil, paste the
   new contents, and commit.
4. The build starts by itself. When it finishes, install the new APK from *Releases* — it installs over
   the old version and keeps your notes.

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
