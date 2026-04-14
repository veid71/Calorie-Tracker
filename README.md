# CalorieTracker

An Android nutrition and health tracking app with barcode scanning, Health Connect integration, community recipes, and a 1M+ product food database.

## Features

### Food Logging
- Search 113,886 USDA foods and 1,000,000+ Open Food Facts products
- Barcode scanning with offline cache
- Voice input for hands-free logging
- Edit and delete past entries
- Retroactive entry with date picker
- Favorite meals for quick re-logging

### Barcode Database
- 1M+ products sourced from Open Food Facts
- Pre-built database updated monthly via GitHub Actions
- Download as a single fast file from Settings → Regional
- Report missing barcodes directly to Open Food Facts

### Health & Fitness
- Android Health Connect integration (steps, workouts, weight)
- Smart calorie goal adjustment based on workout calories
- Bluetooth scale support (Renpho)
- Water intake tracking

### Nutrition Dashboard
- Daily macro breakdown (protein, carbs, fat, fiber, sugar, sodium)
- Weekly and monthly analytics with charts
- Goal tracking with visual progress bars
- Streak tracking for daily logging consistency

### Recipes & Meal Planning
- Create and save custom recipes
- Import recipes from URLs
- Weekly meal planner with shopping list generation
- Community recipe sharing with ratings and reviews

### Home Screen Widget
- Calories consumed vs. goal at a glance
- Updates every 30 minutes and on food log

### Notifications
- Smart meal reminders at 8am, 12pm, and 6pm
- Weekly summary every Sunday evening
- Streak protection alerts

## Database Updates

The food database is rebuilt monthly from the Open Food Facts export and published as a GitHub Release. The app checks for updates automatically.

To manually update: **Settings → Regional → Download Food Database → Check for Update**

## Tech Stack

- Kotlin, Android SDK 26+
- Room (SQLite), WorkManager, Health Connect
- Material Design 3
- Retrofit, Open Food Facts API, USDA SR Legacy

## Building

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot"
./gradlew assembleDebug
```

Requires Android Studio or JDK 21+. Minimum SDK: API 26 (Android 8.0).

## License

MIT
