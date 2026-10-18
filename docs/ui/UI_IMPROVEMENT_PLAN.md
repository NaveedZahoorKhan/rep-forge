# Rep Forge UI improvement plan

Date: 18 October 2026 · Implementation baseline v1

## Goal and evidence

Make planning, starting and logging a workout easy to understand and operate with one hand. Keep the existing Kotlin/Jetpack Compose/Material 3 stack and teal identity. Preserve stored data and functional fixes.

This is a source-based screen audit and design specification, not a completed device usability study. The reviewed app has five primary destinations, five workout tabs, five progress tabs and four profile tabs. Theme.kt inherits ambient typography; CommonComponents.kt contains clickable tags with empty actions. Runtime screenshots and usability validation remain required: qa_api33 and qa_api24 failed to start.

## Reference research and limits

- [Strong](https://www.strong.app/): official page and public workout screenshot informed the compact set table, subdued previous-performance column and clear completion action. Adapt the interaction hierarchy, not its branding.
- [Hevy routine library guide](https://help.hevyapp.com/hc/en-us/articles/36011518408983-How-to-Access-and-Use-Hevy-s-Routine-and-Program-Library): documented explore → filter → save → use flow informs discovery. Only expose filters backed by Rep Forge data.
- [Android Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): informs native semantics and minimum touch areas.
- [Mobbin](https://mobbin.com/discover/apps/ios): requested searches for fitness home, active workout and progress screens were blocked by the connector’s paid-plan requirement. No Mobbin screens were inspected. The baseline below is an original proposal informed by accessible references; Mobbin research is an explicit outstanding checkpoint, not a claimed source. With access, compare onboarding, workout logging and progress flows and document adopted/rejected patterns before final visual sign-off.

## Visual direction: calm training notebook

Use clear left-aligned headings, strong readable numbers, quiet neutral surfaces and teal actions. Retain primary #0F766E on light and #2DD4BF on dark, with existing theme neutrals as the starting palette; verify all actual foreground/background combinations. Reserve red/amber/green for meaningful states. Avoid decorative gradients, arbitrary accent colors, card-within-card layouts and imagery that displaces logging controls.

Use system-native type with an explicit scale: 28/34 title, 20/26 section, 16/24 body, 14/20 label, 32/38 key metric (sp); tabular numerals for changing values. Use 4/8/12/16/24/32dp spacing, 16dp phone gutters, 12dp controls and 16dp cards. Put related settings in divided rows; cards should group a coherent task. Use one consistent native icon family with text on ambiguous actions. Motion should explain state change, remain brief and respect system settings.

## Information architecture

| Destination | Main content | Secondary destinations |
|---|---|---|
| Today | Resume/start workout, weekly activity, daily shortcuts | Nutrition, body log, history, contextual Coach |
| Train | Routines, Exercises, Schedule | Routine detail/editor, Coach, active workout, warm-up |
| Progress | Overview and selected trend | Strength, Body, Photos, History/session detail |
| You | Account and grouped settings | Preferences, units/reminders, equipment, connections, data/privacy, Nutrition |

Coach remains a labeled action on Train and contextual entry on Today. Nutrition has one implementation reachable from Today and You. Health connection settings move to You with a contextual link from Progress. Active sessions have a persistent Resume bar above bottom navigation. Root state persists when switching tabs; drill-ins return to their origin. Route migration must preserve existing links and arguments.

## Success criteria

- Start a scheduled workout or resume from Today in one tap; open Nutrition in one tap; reach Coach and History within two taps.
- Primary workout action is visible without scrolling on a 360dp default-text display; at large text all actions remain reachable.
- Complete a set without opening a modal; maintain input focus and visual context after logging.
- Every screen has useful empty/error feedback and a clear next action; no dead-looking controls, fake data or ambiguous saved states.
- Pass the shared accessibility and device acceptance criteria below. These are acceptance targets, not measured results.

## Delivery sequence

1. **Foundation:** tokens/component gallery and route migration. Produce annotated Today, active workout and Progress layouts in both themes before broad migration.
2. **Core journey:** onboarding, Today, discovery, routine editor, schedule, active logging and warm-up. Keep behavior fixes from existing issues as prerequisites.
3. **Supporting journeys:** history, progress/body/photos, nutrition, Coach and You/settings.
4. **Release validation:** reference decision log, complete screen-state matrix, ARTEMIS behavior exploration and device accessibility checks.

Each screen ticket depends on foundation; navigation changes land before relocating destinations. Screen work can proceed independently after those contracts stabilize. Functional issues #3–17 remain the authority for behavior fixes; closed status does not replace regression verification. No schema rewrite, new social features, subscription system, replacement backend or new exercise-media catalog is part of this UI scope.

## Screen specifications and tickets

### foundation: Define the Compose visual system and reusable UI states

**Priority:** P1 · **Delivery phase:** 1

**Source areas:** `theme/Theme.kt; components/CommonComponents.kt; components/Charts.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Theme inherits the ambient typography; common metrics, tags and empty states lack a single interaction and hierarchy contract. TagRow uses clickable AssistChip with an empty action.

**Proposed layout:** Keep the teal identity and native Material 3. Define explicit typography, spacing, surfaces, shapes, icons and component variants. Build a preview gallery containing buttons, fields, metric rows, badges, cards, dialogs, sheets and feedback states.

**Acceptance criteria:**
- [ ] Provide named light/dark tokens: teal primary, neutral surfaces, semantic success/warning/error; remove decorative secondary colors from migrated components.
- [ ] Implement type roles 28/34 screen title, 20/26 section, 16/24 body, 14/20 label and 32/38 metric (sp); use tabular numerals for changing metrics and timers. Scale with system font size.
- [ ] Use spacing 4/8/12/16/24/32dp, 16dp phone gutters, 12dp controls and 16dp cards; document exceptions for dense workout rows.
- [ ] Provide primary/secondary/destructive/loading/disabled button variants and field helper/error states. Information tags have no click semantics; filter chips expose selection.
- [ ] Empty states include a contextual next action; loading/error/offline states preserve content and offer retry where relevant. No fake populated data.
- [ ] Preview gallery includes every component in both themes, large text, long labels and disabled/error states; publish measured contrast and touch-target checks.

**Dependencies:** None. **Related functional work:** Cross-cutting.

### navigation: Replace nested tab overload with Today, Train, Progress and You

**Priority:** P1 · **Delivery phase:** 1

**Source areas:** `navigation/AppRoutes.kt; root navigation host; WorkoutsScreen.kt; ProgressScreen.kt; ProfileScreen.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Five top-level destinations plus five workout tabs, five progress tabs and four profile tabs distribute related tasks across competing navigation levels.

**Proposed layout:** Use four labeled bottom destinations. Today owns daily entry points; Train contains Routines, Exercises and Schedule; Progress opens an overview with Strength, Body and Photos drill-ins; You groups account, preferences, equipment, connections and data. Coach is a labeled Train action plus contextual Today entry; Nutrition is a Today shortcut and You entry.

**Acceptance criteria:**
- [ ] Create an old-to-new route map covering every existing destination, dialog entry and navigation argument; no feature becomes unreachable.
- [ ] Existing saved routes and deep links resolve to the corresponding new destination; Android Back returns to the originating screen without loops.
- [ ] From Today, starting the scheduled workout or resuming an active session requires one tap; Nutrition is one tap; Coach and History are reachable within two taps.
- [ ] An active-session bar appears above bottom navigation only while a session exists; it shows workout name and Resume, without obscuring content or the rest timer.
- [ ] Each root preserves scroll and selection on tab changes; drill-ins show a back action; selected bottom item has text and accessibility selected state.
- [ ] Empty, signed-out and active-session route maps are verified; remove duplicate embedded Nutrition implementations in favor of one route.

**Dependencies:** foundation. **Related functional work:** #6, #7, #11, #12, #13.

### onboarding: Make onboarding a short, readable step-by-step flow

**Priority:** P1 · **Delivery phase:** 2

**Source areas:** `onboarding/OnboardingScreen.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Onboarding combines identity, goals, measurements and generated-plan review; it needs a deliberate visual hierarchy and visible progress.

**Proposed layout:** Use Welcome → Goals & experience → Schedule & equipment → Optional measurements → Plan review. Ask for account/permissions at their relevant benefit, with truthful skip behavior where supported. Pin Continue above the keyboard.

**Acceptance criteria:**
- [ ] Each step has a descriptive title, step indicator and one primary action; Back preserves answers and scrolls to the active step.
- [ ] Optional measurements are explicitly marked optional with units beside fields; invalid input shows inline guidance and is never replaced with a fabricated value.
- [ ] Choice groups show selected state using text/icon and shape in addition to color; long labels wrap.
- [ ] Review displays goal, days, equipment and plan summary with Edit links returning to the correct step.
- [ ] Generation shows progress text, retry on failure and a usable manual setup path; changing inputs visibly invalidates the old preview.
- [ ] Keyboard, 200% text, denied permission and returning-user flows are documented with screenshots.

**Dependencies:** foundation, navigation. **Related functional work:** #3, #5.

### today: Give Today one dominant next action and compact daily summaries

**Priority:** P1 · **Delivery phase:** 2

**Source areas:** `dashboard/DashboardScreen.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Daily actions compete with metrics; the screen needs a clear order for new, returning and mid-workout users.

**Proposed layout:** Order: date/header → active or planned workout hero → compact weekly consistency row → Nutrition and body-log shortcuts → optional Coach suggestion. Show at most three supporting metric summaries above secondary detail.

**Acceptance criteria:**
- [ ] Hero priority is Resume active workout, then Start scheduled workout, then Choose workout; exactly one primary workout action appears.
- [ ] Hero communicates routine name, expected duration when known, and exercise count without inventing estimates.
- [ ] New-user state offers Choose workout and explains the next step; rest-day state offers Browse routines without implying a missed workout.
- [ ] Weekly consistency distinguishes completed, scheduled and today using labels/semantics as well as color.
- [ ] Nutrition and weight shortcuts open the correct date/context; summaries display units and distinguish missing entries from zero.
- [ ] At 360dp/default text the primary workout action appears without scrolling; at larger text all content remains reachable with no overlap.

**Dependencies:** foundation, navigation. **Related functional work:** #6.

### discovery: Unify routine browsing and exercise discovery

**Priority:** P1 · **Delivery phase:** 2

**Source areas:** `workouts/WorkoutsScreen.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Workouts, Library and Create are peers despite representing content and actions; filter and tag presentation makes browsing harder to scan.

**Proposed layout:** Train opens Routines with a prominent Create routine action, search, saved routines and an Explore entry. Exercises is a separate searchable catalog. Use consistent list rows with title, useful metadata and explicit action.

**Acceptance criteria:**
- [ ] Routine rows show title, exercise count and relevant equipment; row opens details, Start is a separately labeled action.
- [ ] Search persists across detail/back navigation; filters expose selected values, result count and Clear all.
- [ ] Exercise filters use available muscle/equipment data; unsupported filter dimensions are omitted rather than fabricated.
- [ ] No-results state displays the query and Reset filters action; no-routines state offers Create routine and Explore.
- [ ] Routine detail shows ordered exercises, set targets and rest metadata before Start; save/add operations provide feedback.
- [ ] Explore patterns adapt Hevy’s documented search/filter/save flow to existing data; no imitation branding or new program catalog dependency.

**Dependencies:** foundation, navigation. **Related functional work:** #7.

### builder: Design a focused routine editor and exercise picker

**Priority:** P1 · **Delivery phase:** 2

**Source areas:** `workouts/WorkoutsScreen.kt routine creation/editing` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Routine composition needs a clear distinction between editable exercise targets, ordering and the save action.

**Proposed layout:** Use a dedicated editor with routine name, ordered exercise rows, Add exercise and persistent Save. Exercise selection uses search and a selected-count footer. Advanced set options expand on demand.

**Acceptance criteria:**
- [ ] Create and Edit use the same layout and populate existing values; save is disabled with a visible reason when required data is missing.
- [ ] Each exercise exposes sets/reps/rest with units; validation appears beside the relevant input.
- [ ] Exercise reorder supports accessible Move up/down actions in addition to any drag gesture.
- [ ] Remove exercise offers undo; Back with unsaved changes offers Keep editing or Discard; save failure preserves the draft.
- [ ] Picker retains selected exercises while searching/filtering, prevents accidental duplicate adds and confirms selection count.
- [ ] Keyboard never covers the focused field or Save; successful save returns to the routine detail once without duplicates.

**Dependencies:** foundation, navigation, discovery. **Related functional work:** #7.

### schedule: Make the workout planner readable as a weekly agenda

**Priority:** P2 · **Delivery phase:** 2

**Source areas:** `workouts/WorkoutsScreen.kt planner` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Planning needs explicit dates, empty-day affordances and a useful small-screen alternative to dense calendar controls.

**Proposed layout:** Use a week strip and vertical agenda for the selected day. Tap an empty day to assign a routine; tap an entry for start/reschedule/remove. Keep rest days visually quiet.

**Acceptance criteria:**
- [ ] Week strip labels weekday and date, announces selected date, and supports previous/next week and Today.
- [ ] Scheduled entries show routine name and status; empty days offer Schedule workout without fake events.
- [ ] Routine selection confirms target date before saving; changing weeks does not change the pending assignment silently.
- [ ] Reschedule and remove provide clear feedback and recovery; completed workouts are not edited as planned items.
- [ ] Agenda works at 320dp and large text without requiring a seven-column detail grid.
- [ ] Start scheduled workout opens the same session flow as Today and does not create duplicate sessions.

**Dependencies:** foundation, navigation, discovery. **Related functional work:** #7.

### logging: Optimize active workout rows, rest timer and finish review

**Priority:** P1 · **Delivery phase:** 2

**Source areas:** `active/ActiveWorkoutScreen.kt; active/PersistentRestTimerComponent.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Logging is the highest-frequency task and needs compact but usable input, clear completion and safe finish hierarchy.

**Proposed layout:** Use exercise headers followed by Set / Previous / Weight / Reps / Done rows. Keep previous performance secondary. Put rest timer in a stable lower region; Finish opens a summary before committing.

**Acceptance criteria:**
- [ ] Current set is visually distinct; completed sets include a check and accessible completed label; previous values are explicitly historical.
- [ ] Weight/reps inputs have numeric keyboards, unit labels and Next focus behavior; invalid values remain editable with inline guidance.
- [ ] Logging a set does not collapse the exercise or jump away from the next set; editing a completed set remains possible.
- [ ] Rest timer exposes remaining time, pause/resume, skip and adjustment with at least 48dp targets; it never covers keyboard input or finish controls.
- [ ] Finish review shows completed sets, incomplete work and duration; cancel returns without data loss; repeated Finish cannot create duplicates.
- [ ] Explore and capture resume after backgrounding/rotation, empty exercise, long exercise names and timer running while navigating.

**Dependencies:** foundation, navigation. **Related functional work:** #8, #10.

### warmup: Create a clear warm-up player with visible progress

**Priority:** P2 · **Delivery phase:** 2

**Source areas:** `warmup/DynamicWarmUpPlayer.kt; components/DynamicWarmUpCard.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Warm-up controls need to separate current movement, elapsed progress and completion without relying only on a timer.

**Proposed layout:** Use current movement title/instruction, large timer or repetition target, step count, next movement preview and a bottom control row.

**Acceptance criteria:**
- [ ] Current movement, step X of Y and next movement are visible; skipped and completed steps are distinguishable.
- [ ] Pause/resume, previous, next and exit have labeled accessible controls; exit makes remaining progress clear.
- [ ] Pause freezes functional progress; background/foreground shows the actual timer state without replaying completed steps.
- [ ] Completion offers Start workout or Return to workout using the originating context.
- [ ] Missing illustration uses useful text instructions; no decorative image is required to understand the movement.
- [ ] Verify long instructions, no warm-up data, large text and interrupted playback.

**Dependencies:** foundation, logging. **Related functional work:** #9.

### history: Make history and session detail easy to scan and edit

**Priority:** P1 · **Delivery phase:** 3

**Source areas:** `history/WorkoutHistoryScreen.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Historical sessions and manually logged workouts need a consistent date-first layout and clear edit affordances.

**Proposed layout:** Place History in Progress with a Today shortcut. Group sessions by date; detail shows summary then exercise/set results. Manual entry uses a dedicated form.

**Acceptance criteria:**
- [ ] History rows show date, workout title, duration when recorded and completed-set count; absent values display an appropriate placeholder, never invented totals.
- [ ] Filters/date range retain state when returning from details; empty states distinguish no history from no filter matches.
- [ ] Detail separates recorded results from template targets and exposes Edit in a predictable app-bar action.
- [ ] Manual entry labels date, duration and units; editing opens the original values and provides explicit Save/Cancel.
- [ ] Delete requires confirmation naming the session and offers recovery when storage supports it; failure leaves the record visible.
- [ ] Long sessions scroll smoothly and have readable exercise headings and set columns at narrow widths.

**Dependencies:** foundation, navigation, logging. **Related functional work:** #10.

### progress: Redesign progress charts, body logs and photo comparison

**Priority:** P1 · **Delivery phase:** 3

**Source areas:** `progress/ProgressScreen.kt; components/Charts.kt; components/WeightLogChartCard.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Five progress tabs mix insights, input and integrations; dense charts and ambiguous empty states reduce readability.

**Proposed layout:** Overview presents one selected trend, period selector and summary, followed by Strength, Body, Photos and History rows. Each drill-in owns its filters and input actions. Move Health connection settings to You.

**Acceptance criteria:**
- [ ] Charts show metric, unit, date range and selected-value details; changing period updates title, data and summary together.
- [ ] No data, one point and unavailable data have different states; charts do not imply trends from one point.
- [ ] Every chart has a textual summary and accessible data list; color is supplemented with labels/line style.
- [ ] Strength explains estimated 1RM and its source; Body shows dated measurements and a clear Add measurement action.
- [ ] Photos use dated thumbnails, explicit Add photo, permission-denied recovery and comparison with two clearly labeled dates; no automatic sharing.
- [ ] Photo deletion and measurement editing show confirmation/feedback as appropriate; missing photos render a recoverable placeholder.
- [ ] Range controls, legends and selected-value overlays remain readable in both themes and at 200% font scale.

**Dependencies:** foundation, navigation. **Related functional work:** #4, #11.

### nutrition: Create a simple daily nutrition and water log

**Priority:** P1 · **Delivery phase:** 3

**Source areas:** `nutrition/NutritionScreen.kt; profile nutrition entry` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Nutrition needs a single visual destination with clear day context, editable entries and honest goals.

**Proposed layout:** Use date selector, compact calories/macros summary, meal sections, water row and Add food action. Keep targets secondary and identify unavailable targets.

**Acceptance criteria:**
- [ ] Selected date appears in the header and all add/edit sheets; previous/next day and Today preserve the correct log context.
- [ ] Summary labels consumed and target separately; missing targets say Not set; any ring has a matching numeric explanation.
- [ ] Food rows show name, serving and calories with edit access; fields expose units and validation.
- [ ] Water controls provide increment feedback and an editable total; rapid taps cannot produce misleading displayed totals.
- [ ] Empty meals offer Add food; failed saves preserve inputs; deleting an entry provides recovery feedback.
- [ ] Today and You open the same screen and data source; back returns to the entry point.

**Dependencies:** foundation, navigation. **Related functional work:** #12.

### coach: Present Coach inputs, generated plans and reports clearly

**Priority:** P2 · **Delivery phase:** 3

**Source areas:** `gemini/GeminiCoachScreen.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Coach combines forms and long generated output; users need to see what is editable, generated, saved or outdated.

**Proposed layout:** Use a compact goal/input summary with expandable advanced choices, Generate plan action, structured result sections and a separate Reports history. Use teal identity without a special AI gradient.

**Acceptance criteria:**
- [ ] Visible input summary includes the settings actually used; advanced choices remain discoverable and labeled.
- [ ] Generation shows meaningful progress state, cancellation where supported, and retry without losing inputs; failures never look successful.
- [ ] Results use headings, exercise rows and collapsed explanation sections; Save plan and Edit inputs are visually distinct.
- [ ] Changing relevant inputs marks results outdated and prevents silently saving them as current.
- [ ] Reports show date, covered period and real source label; generated text is identified as AI output and is not presented as medical advice.
- [ ] Health background and personal notes use labeled optional sections with clear disclosure of what is sent; machine-photo upload shows selected preview, replace/remove, permission/error feedback and identified-machine review before applying results.
- [ ] Long outputs, empty history, provider unavailable and saved-result feedback are designed and captured.

**Dependencies:** foundation, navigation, builder. **Related functional work:** #16, #17.

### you: Organize account, settings, equipment, connections and data

**Priority:** P1 · **Delivery phase:** 3

**Source areas:** `profile/ProfileScreen.kt; components/GoogleHealthLinkComponents.kt` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Profile uses broad tabs including equipment, nutrition and data; preferences and destructive actions need clearer grouping and state presentation.

**Proposed layout:** You opens a grouped settings list: Account; Training preferences; Units & reminders; Equipment; Connections; Data & privacy; Nutrition shortcut. Each complex section is a drill-in with an explicit save model.

**Acceptance criteria:**
- [ ] Account row displays verified identity or Sign in, with sign-in/loading/error/signed-out states; no inferred connected status.
- [ ] Preferences group related fields and show saved values; units appear beside measurements and changes provide accurate feedback.
- [ ] Equipment list includes search/filter when useful, descriptive rows and separate Add/Edit actions; delete uses clear confirmation/undo behavior.
- [ ] Connections distinguish unavailable, permission needed, connected and syncing; show last successful sync only from actual data and provide settings/retry.
- [ ] Data screen separates export/import/restore from a labeled danger section; preview describes scope and destructive confirmation names the affected data.
- [ ] Reminder controls explain schedule and permission status; blocked notifications have an actionable settings link.
- [ ] Nutrition is a shortcut to the canonical route; every drill-in has predictable Back and preserves unsaved changes appropriately.

**Dependencies:** foundation, navigation. **Related functional work:** #3, #4, #13, #14, #15.

### validation: Validate the complete UI system and close the reference review

**Priority:** P1 · **Delivery phase:** 4

**Source areas:** `All presentation screens and dialogs` (under `app/src/main/java/com/gymtracker/app/presentation/` unless noted).

**Audit / design need:** Current audit is source-based; emulators failed to start, and Mobbin access is blocked. Layout and interaction acceptance require real device evidence.

**Proposed layout:** Run a complete journey and visual consistency review after screen work lands. Maintain a screenshot matrix and a reference decision log. Verify behavior before writing executable mobile tests.

**Acceptance criteria:**
- [ ] Obtain user-authorized Mobbin access or supplied references; review onboarding, workout logging and progress flows, recording exact reference links and adopted/rejected patterns. If access remains blocked, record explicit deferral rather than claiming Mobbin review complete.
- [ ] Confirm an available user-selected device using ARTEMIS diagnosis; explore each target flow using ARTEMIS before authoring runnable UI tests, following AGENTS.md.
- [ ] Capture every screen and major modal in light/dark, empty/populated/error states; include 320dp/360dp layouts and 100%/200% text coverage with failures tracked.
- [ ] Run TalkBack journey for onboarding → start → log → finish → history; verify labels, order, selected states, editable values, timer announcements and non-color status cues.
- [ ] Complete Today → scheduled workout → set log → finish → history plus create → schedule → start, nutrition edit, measurement/photo, Coach failure and account/data journeys.
- [ ] Verify system bars, keyboard insets, rotation, background/resume, offline/error recovery and Android Back; attach screenshots and exact device/build details.
- [ ] Compare task counts to the navigation targets, record issues and block rollout on data loss, inaccessible primary actions, unreadable layouts or false success states; publish the final coverage report.

**Dependencies:** onboarding, today, discovery, builder, schedule, logging, warmup, history, progress, nutrition, coach, you. **Related functional work:** Cross-cutting.

### Shared acceptance criteria
- [ ] Use the shared Compose tokens/components; include light and dark themes with consistent terminology and one primary action per task region.
- [ ] Interactive targets are at least 48dp and do not overlap; controls expose meaningful TalkBack labels, role, state and logical focus order.
- [ ] At 320dp and 360dp width, and 100%/200% text, primary actions and content remain reachable without clipped text or overlapping controls. Dense rows may reflow.
- [ ] Text contrast meets the project target of 4.5:1 for body text and 3:1 for large text; state is never conveyed only by color.
- [ ] Loading, empty, error, disabled and populated states are implemented where applicable; retry preserves user input; no fabricated metrics or false success.
- [ ] Keyboard/system-bar insets, Back, rotation and background/resume preserve appropriate state. Motion respects system animation settings.
- [ ] Attach before/after screenshots and an acceptance checklist with device/build details. Follow AGENTS.md: explore with ARTEMIS before authoring executable mobile tests.

## Review and release controls

Keep UI items in Todo until work begins. Each PR links its ticket and screenshots, documents intentionally unsupported states, and confirms that existing records and routes survive migration. Review visual tokens first, representative core screens second, then complete screen coverage. Preserve the current functional UI until its replacement meets the same behavior requirements. Do not close the reference review as complete while Mobbin access is unresolved; an explicit approved deferral may be recorded separately.

No UI implementation or new test code is included in this planning change. This plan creates detailed work ready for implementation, with runtime verification and Mobbin research limitations visible.


## GitHub delivery checklist

- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/31 — [UI] Define the Compose visual system and reusable UI states
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/34 — [UI] Replace nested tab overload with Today, Train, Progress and You
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/35 — [UI] Make onboarding a short, readable step-by-step flow
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/36 — [UI] Give Today one dominant next action and compact daily summaries
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/37 — [UI] Unify routine browsing and exercise discovery
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/38 — [UI] Design a focused routine editor and exercise picker
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/39 — [UI] Make the workout planner readable as a weekly agenda
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/40 — [UI] Optimize active workout rows, rest timer and finish review
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/41 — [UI] Create a clear warm-up player with visible progress
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/42 — [UI] Make history and session detail easy to scan and edit
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/43 — [UI] Redesign progress charts, body logs and photo comparison
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/44 — [UI] Create a simple daily nutrition and water log
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/45 — [UI] Present Coach inputs, generated plans and reports clearly
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/46 — [UI] Organize account, settings, equipment, connections and data
- [ ] https://github.com/NaveedZahoorKhan/rep-forge/issues/47 — [UI] Validate the complete UI system and close the reference review
