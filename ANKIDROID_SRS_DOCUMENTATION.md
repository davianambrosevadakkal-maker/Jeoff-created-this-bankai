# AnkiDroid BANKAI — Advanced Spaced Repetition System (SRS) Documentation

AnkiDroid BANKAI is a comprehensive spaced repetition flashcard learning system for Android built with Jetpack Compose, Kotlin Coroutines & Flow, and Room Database (SQLite).

---

## 1. Core SRS Scheduling Engines

### SuperMemo 2 (SM-2)
- **Classic interval calculation**: Multiplies current interval by an Ease Factor (EF) starting at 2.50.
- **Dynamic Ease Factor adjustments**:
  - `Again` (Rating 1): Ease decreases by 0.20; interval drops to learning step.
  - `Hard` (Rating 2): Ease decreases by 0.15; interval advances by $1.2\times$.
  - `Good` (Rating 3): Ease remains stable; interval multiplies by current EF.
  - `Easy` (Rating 4): Ease increases by 0.15; interval receives an extra easy bonus multiplier ($1.3\times$).
- **Fuzz Factor**: Adds a slight pseudo-random variance ($\pm 5\%$) to scheduled intervals to prevent card clustering on future days.
- **Configurable Learning & Relearning Steps**: Custom intra-day steps (e.g. 1m, 10m, 1d) before card graduation.

### Free Spaced Repetition Scheduler (FSRS)
- **DSR Mathematical Model**: Based on Difficulty ($D$), Stability ($S$), and Retrievability ($R$).
- **Desired Retention Rate**: User sets exact retention probability targets (e.g. 85%, 90%, 95%). The engine calculates future intervals to mathematically hit that exact forgetting curve.
- **Dynamic Weight Optimization**: Personalizes scheduling parameters dynamically based on individual review history.

---

## 2. Study Analytics & Review Heatmap Dashboard

Accessed via the **Analytics (BarChart)** icon in the top app bar:
- **60-Day Review Heatmap**: Visual contribution grid with 5 color intensity tiers tracking daily study consistency and streaks.
- **Card Maturity Distribution**: Categorizes the entire collection:
  - **Mature**: Cards with interval $\ge 21$ days (Emerald Green).
  - **Young**: Cards with interval $< 21$ days (Sky Blue).
  - **Learning**: Cards in intra-day step progression (Amber).
  - **New**: Unseen cards (Purple).
  - **Suspended**: Frozen cards (Slate Gray).
- **Scheduling Forecast Graph**: Visual load projection for Today, Tomorrow, 2–3 Days, 4–7 Days, and 8–30 Days.
- **Retention Rate KPI**: Real-time pass/fail accuracy percentages calculated across historical logs.
- **Active Study Time Tracker**: Total study minutes logged across review sessions.
- **Leech Card Inspector**: Auto-identifies persistent failure cards ($\ge 8$ lapses) for targeted intervention.

---

## 3. Note & Card Formatting Features

- **Cloze Deletion Parsing**:
  - Recognizes standard Anki cloze syntax: `{{c1::answer}}` and `{{c1::answer::hint}}`.
  - Question display shows `[...]` or `[hint]`.
  - Revealed card shows highlighted bracketed target `【answer】`.
  - Typing verification compares input directly against the cloze deletion target.
- **Type-in-the-Answer Mode**: Interactive spelling verification highlighting German umlauts (`ä`, `ö`, `ü`, `ß`).
- **Dual-Direction & Alternating Study**: Flip between German $\rightarrow$ English, English $\rightarrow$ German, or mixed alternating sets.
- **Pronunciation Microphone Tester**: Built-in audio recorder (`MediaRecorder` AAC/MPEG4) allowing users to record their spoken attempt and play it back alongside native German TTS.
- **Gemini AI Vocabulary Service**: Dynamic synonym fetching and contextual example sentence generation.

---

## 4. Navigation, Gestures & Haptic Controls

- **Swipe Gestures**:
  - Swipe Right: Grade card as **Good** (or reveal answer).
  - Swipe Left: Grade card as **Again** (or reveal answer).
- **Haptic Vibration Feedback**: Confirmatory tactile pulse on grading button taps (`LocalHapticFeedback`).
- **Built-in Whiteboard**: Full-screen finger/stylus canvas overlay for practicing written German words and umlauts.
- **Instant Undo**: One-tap rollback restoring the card's exact previous interval and ease factor.
- **SRS Card Diagnostics**: Deep inspection sheet displaying exact interval, ease factor, lapses, due date, and history log.
- **Four-Color Flagging**: Flag cards during study with **Red**, **Orange**, **Green**, or **Blue** flags.
- **Timeboxing Alarm**: Periodic notification prompting rest breaks after configured study durations.

---

## 5. Card Browser & Anki Search Syntax

- **Anki Query Syntax**:
  - `is:leech` — Cards with $\ge 8$ lapses.
  - `is:flagged`, `flag:red`, `flag:orange`, `flag:green`, `flag:blue` — Filter by color flags.
  - `is:new`, `is:learning`, `is:review`, `is:suspended` — Filter by card status.
  - `tag:<name>` — Filter cards with specific tags.
- **Multi-Select Batch Actions**:
  - Batch Flag (Red, Orange, Green, Blue, Clear).
  - Batch Delete.
  - Batch Suspend / Unsuspend.
  - Batch Reset Progress (returns cards to New).
- **Sorting Options**:
  - Order index (asc/desc), German A–Z / Z–A, English A–Z / Z–A, Due Date, Difficulty (lowest ease), Lapses (highest), Flags First, Leeches First.
- **Micro-Set Range Filter**: Study in 25-card micro-batches for focused sessions.

---

## 6. Database Maintenance & Local Snapshots

- **Integrity Check**: Runs `PRAGMA integrity_check` on the SQLite database to verify B-tree pages and indices.
- **Vacuum & Optimize**: Compacts disk storage and analyzes query plans via `VACUUM` and `ANALYZE`.
- **Automated JSON Snapshots**: One-tap snapshot generation storing all decks, flashcards, flags, intervals, and logs in local app storage.
- **Room Migration (v1 $\rightarrow$ v2)**: Clean database migration pipeline adding flag support without resetting existing user data.
- **Anki .apkg & CSV Importer**: Load standard Anki decks and customized CSV files.
