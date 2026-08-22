# GymTracker

GymTracker is a Kotlin + Jetpack Compose Android workout tracker built as a full greenfield app.

The project includes:

- Workout templates, custom workout building, exercise search/filtering, active workout logging, rest timers, PRs, plate and one-rep-max calculators.
- Room persistence for exercises, workouts, sessions, sets, body measurements, photos, nutrition, water, weight logs, schedules, reminders, and profile settings.
- Material 3 Compose UI with bottom navigation, onboarding, dynamic/dark theme support, Vico charts, haptics, sharing/export, and WorkManager reminders.
- Hilt DI, repository/use-case layering, Kotlin Coroutines/Flow, local JSON/CSV backup, optional Firebase cloud backup hooks, and Google sign-in entry points.

Open the project in Android Studio and run the `app` module. Firebase/Google sign-in requires the normal Google services project configuration before runtime cloud sync can be enabled.
# rep-forge
