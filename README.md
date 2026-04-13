# 📱 Calorie Tracker - Complete Health & Fitness Edition

A comprehensive Android calorie and nutrition tracking app with OnePlus Watch 3 integration, smart weight goal calculations, offline caching, and advanced analytics features.

## 🌟 Key Features

### Core Functionality
- **📝 Smart Manual Entry**: Food logging with intelligent search suggestions and auto-fill from online databases
- **📷 Barcode Scanning**: Camera-based barcode scanning with Open Food Facts integration
- **🌍 Smart Food Search**: Real-time search with dropdown suggestions from multiple food databases
- **📊 Nutrition Dashboard**: Track 7 nutrients with daily recommendations and progress visualization
- **⚙️ Settings & Goals**: Customizable daily goals and regional preferences
- **📅 History Tracking**: View past entries organized by date

### 🆕 Smart Features (NEW!)
- **🔍 Intelligent Food Search**: Type food names to get instant suggestions with auto-complete
- **🥄 Serving Size Calculator**: Enter portions like "2 cups" or "half serving" for automatic nutrition recalculation
- **💾 Offline Food Storage**: Save foods locally with "(saved)" indicator for internet-free access
- **🏪 Barcode Cache System**: Previously scanned barcodes stored offline for instant re-access
- **📈 Enhanced Analytics**: Interactive charts with calorie trends and macro breakdowns
- **🎨 Modern UI Design**: Material Design 3 with beautiful colors and smooth interactions

### 🏃 Fitness Integration (LATEST!)
- **⌚ OnePlus Watch 3 Integration**: Seamless sync with your OnePlus Watch 3 via Health Connect
- **🔥 Workout Calorie Tracking**: Automatically reads calories burned from your fitness activities
- **📊 Adjusted Calorie Goals**: Smart goal adjustment based on workout intensity (70% of calories burned)
- **🎯 Bonus Calories Display**: Shows extra calories you can eat after workouts
- **💪 Exercise Type Recognition**: Tracks different workout types and durations
- **🔄 Real-Time Sync**: Automatic fitness data sync when app opens

### ⚖️ Advanced Health Management (NEWEST!)
- **📊 Daily Weight Tracking**: Log weight daily with progress visualization over time
- **🎯 Smart Weight Goals**: Set current weight, target weight, and timeline for personalized plans
- **🧮 BMR/TDEE Calculations**: Uses Mifflin-St Jeor equation for accurate metabolic rate estimation
- **📊 Personalized Calorie Recommendations**: Automatic daily calorie goals based on your weight goals
- **👤 Demographics Support**: Optional age, height, gender, and activity level for precise calculations
- **⚠️ Health Safety Checks**: Warns against unhealthy weight loss/gain rates (max 2 lbs/week)
- **🔄 Auto-Goal Updates**: Weight goals automatically update your daily calorie targets
- **🔧 Automatic Calorie Adjustment**: AI-powered calorie adjustments based on weight trends and progress
- **💧 Water Intake Tracking**: Daily water logging with interactive charts and progress visualization
- **🔔 Smart Notifications**: Customizable reminders for weight logging, meals, and water intake
- **📱 Renpho Scale Integration**: Research-backed Health Connect integration for automatic weight sync
- **🔍 Health Connect Debug Tools**: Comprehensive sync verification and troubleshooting tools

## 🏗️ Technical Details

### Architecture
- **Offline-First**: Works completely offline with local SQLite database storage
- **Smart Cache System**: Intelligent caching of barcode scans and food searches
- **Room Database**: Modern Android database with KSP compilation and migration support (v6)
- **Repository Pattern**: Clean architecture with LiveData observations and network management
- **Material Design 3**: Modern UI with custom color schemes and typography
- **Health Connect Integration**: Android's unified health platform for fitness data synchronization

### Key Technologies
- **Kotlin**: Primary development language with coroutines for async operations
- **Room + KSP**: Database ORM with Kotlin Symbol Processing and automatic migrations (v8)
- **CameraX + ML Kit**: Barcode scanning functionality with Google Vision API
- **MPAndroidChart**: Interactive charts and data visualization for analytics and water tracking
- **Retrofit**: API integration for Open Food Facts, Edamam, and USDA food databases
- **AutoCompleteTextView**: Smart search functionality with dropdown suggestions
- **LiveData**: Reactive UI updates with lifecycle-aware data observation
- **Health Connect**: Android's health platform for reading fitness data from smartwatches
- **Guava**: Support for ListenableFuture and concurrent operations
- **AlarmManager**: Precise notification scheduling for reminders
- **Linear Regression**: Advanced weight trend analysis for calorie adjustments
- **Material Design 3**: Tabbed settings interface with modern UI components

### Analytics Implementation
- **Line Charts**: Daily calorie trends with customizable time periods
- **Pie Charts**: Macro balance visualization with color coding
- **Streak Calculation**: Real-time goal achievement tracking
- **Data Aggregation**: Smart grouping by date with null-safe operations

## 📁 Project Structure

```
app/
├── src/main/java/com/calorietracker/
│   ├── MainActivity.kt                    # Main dashboard with modern UI
│   ├── AnalyticsActivity.kt              # 📈 Analytics charts & insights (crash-fixed)
│   ├── CalorieEntryActivity.kt           # 🆕 Smart food entry with search & serving size
│   ├── BarcodeScanActivity.kt            # Barcode scanning with caching
│   ├── NutritionDashboardActivity.kt     # Daily nutrition progress
│   ├── HistoryActivity.kt                # Historical entries
│   ├── SettingsActivity.kt               # App configuration
│   ├── database/                         # Room database entities + migrations (v8)
│   │   ├── BarcodeCache.kt              # 🆕 Offline barcode cache storage
│   │   ├── FoodItem.kt                  # 🆕 Local food storage
│   │   ├── WorkoutCalories.kt           # 🏃 Fitness data from Health Connect
│   │   ├── WeightEntry.kt               # ⚖️ Weight tracking entries
│   │   ├── WeightGoal.kt                # ⚖️ Weight goals and targets
│   │   ├── DailyWeightEntry.kt          # 📊 Daily weight tracking with progress
│   │   ├── WaterIntakeEntry.kt          # 💧 Water consumption tracking
│   │   ├── ReminderNotification.kt      # 🔔 Scheduled notification reminders
│   │   ├── USDAFoodItem.kt              # 🥗 USDA food database integration
│   │   └── OpenFoodFactsItem.kt         # 🌍 Open Food Facts database
│   ├── repository/                       # Data access layer with smart caching
│   ├── network/                          # API services (Open Food Facts + Edamam)
│   ├── fitness/                          # 🏃 Health Connect integration
│   │   └── HealthConnectManager.kt      # 🆕 OnePlus Watch 3 data reader
│   ├── weight/                           # ⚖️ Weight management system
│   │   └── CalorieAdjustmentCalculator.kt # 🔧 AI-powered calorie adjustments
│   ├── notifications/                    # 🔔 Notification system
│   │   ├── NotificationScheduler.kt     # ⏰ Smart reminder scheduling
│   │   └── ReminderReceiver.kt          # 📱 Notification broadcast receiver
│   ├── WaterTrackingActivity.kt         # 💧 Water intake logging interface
│   └── HealthConnectDebugActivity.kt    # 🔍 Health Connect troubleshooting tools
│   ├── utils/                           # Utility classes
│   │   └── CalorieCalculator.kt         # ⚖️ BMR/TDEE and weight goal calculations
│   └── nutrition/                        # Nutrition calculations
└── res/
    ├── layout/
    │   ├── activity_analytics.xml        # 🆕 Analytics screen layout
    │   └── ...
    └── values/
        ├── strings.xml
        ├── colors.xml
        └── themes.xml
```

## 🚀 Getting Started

1. **Build Requirements**:
   - Android Studio (latest version)
   - Android SDK API 26+ (Android 8.0+) *[Updated for Health Connect]*
   - JDK 8+ (included with Android Studio)

2. **Installation**:
   - Open project in Android Studio
   - Build → Build APK(s)
   - Install on device or emulator

3. **OnePlus Watch 3 Setup** (Optional):
   - Install OHealth app and pair your OnePlus Watch 3
   - Go to CalorieTracker Settings → Health Connect Setup
   - Follow the setup wizard to grant Health Connect permissions
   - **Note**: If CalorieTracker doesn't appear in Health Connect apps list, try force-stopping Health Connect and restarting it

4. **Weight Goal Setup** (Optional):
   - Go to CalorieTracker Settings → Weight Goals section
   - Enter current weight, target weight, and timeline
   - Add optional demographics (age, height, gender, activity level) for more accurate recommendations
   - Tap "Calculate Recommendation" to see your personalized daily calorie goal
   - Tap "Save Weight Goal" to apply the recommendation to your daily targets

5. **Optional Setup**:
   - Get free Edamam API keys for enhanced online food lookups
   - Add keys to `NetworkManager.kt`

📖 **Detailed setup instructions**: See [SETUP_GUIDE.md](app/SETUP_GUIDE.md)

## 🔧 Health Connect Troubleshooting

### CalorieTracker Not Appearing in Health Connect Apps List

This is a known Android Health Connect bug affecting apps installed before Health Connect. Here are the solutions:

#### **Method 1: Force Restart Health Connect (Recommended)**
1. Go to **Settings → Apps → Health Connect**
2. Tap **"Force Stop"**
3. Open Health Connect again
4. CalorieTracker should now appear in the Apps list!

#### **Method 2: Clear Health Connect Cache**
1. Settings → Apps → Health Connect → Storage → **Clear Cache**
2. Restart Health Connect
3. Check Apps list again

#### **Method 3: OnePlus Users**
1. Check **OHealth → Personal Center → Data Sharing → Health Connect**
2. Ensure OHealth has Health Connect permissions
3. Then try Method 1 above

### Permission Setup Process
1. Once CalorieTracker appears in Health Connect:
   - Grant permissions for **Active Calories**, **Total Calories**, and **Exercise Sessions**
   - Return to CalorieTracker Settings
   - Tap **"Sync Workout Data Now"**
2. You should see workout calories appear on your main dashboard!

### Debug Information
- Long-press the "Setup OnePlus Watch Integration" button in CalorieTracker Settings for debug info
- This shows which Health Connect packages are installed on your device

## 📊 Analytics Features in Detail

### Calorie Trends Chart
- **Interactive line chart** showing daily calorie intake
- **Time period selection**: 7-day or 30-day views
- **Visual styling**: Green gradient with data points
- **Date labels**: Month/day format for easy reading

### Macro Balance Chart
- **Pie chart visualization** of macronutrient ratios
- **Color-coded segments**: Protein (green), Carbs (teal), Fat (orange)
- **Gram measurements**: Displays actual gram amounts
- **Empty state handling**: Shows message when no macro data available

### Goal Streak Tracking
- **Current streak**: Consecutive days meeting calorie goals
- **Best streak**: Highest achievement in selected period
- **Smart tolerance**: 80-120% of daily target counts as success
- **Real-time updates**: Recalculates with new food entries

## 🎯 Usage

1. **Daily Tracking**: Use Manual Entry or Barcode Scan to log foods
2. **Monitor Progress**: Check Nutrition dashboard for daily goals
3. **Analyze Trends**: Use new Analytics screen for insights
4. **Adjust Goals**: Modify targets in Settings based on analytics

## 🆕 What's New in This Version

### 🔧 Settings & Permissions Management (v6.0)
- **📋 Complete Settings Overhaul**: Fixed all dropdown population issues across Regional, Goals, and Preferences tabs
- **🔵 Smart Bluetooth Permissions**: Interactive permission dialogs with step-by-step instructions for scale integration
- **⚙️ Regional Settings**: Fully functional region selection (US, UK, Canada, Australia) with persistent preferences
- **🎯 Nutrition Goals Management**: Complete database integration for loading/saving daily nutrition targets
- **🔄 Real-time Weight Calculations**: Persistent calorie recommendation display in Weight tab with interactive metrics
- **📱 App Settings Deep Links**: Direct navigation to Android settings for permission management
- **💾 Enhanced Data Persistence**: Comprehensive SharedPreferences integration for all user settings
- **🔍 Permission State Detection**: Intelligent detection of temporary vs permanently denied permissions

### 💧 Comprehensive Health Management (v5.0)
- **📊 Daily Weight Tracking**: Complete weight monitoring system with progress visualization
- **🔧 Automatic Calorie Adjustment**: AI-powered calorie recommendations based on weight trends using linear regression
- **💧 Water Intake Tracking**: Daily water logging with interactive charts and hydration goals
- **🔔 Smart Notification System**: Customizable reminders for weight, meals (breakfast/lunch/dinner), and water intake
- **🔍 Health Connect Debug Tools**: Comprehensive sync verification to troubleshoot watch connectivity issues
- **📱 Improved Database Downloads**: Fixed USDA food database with proper API key integration
- **🎨 Tabbed Settings Interface**: Modern Material Design 3 settings with organized categories

### ⚖️ Weight Goal & Calorie Recommendations (v4.0)
- **🎯 Smart Weight Goals**: Complete weight management system with personalized calorie recommendations
- **🧮 BMR/TDEE Calculator**: Scientific calculations using Mifflin-St Jeor equation for accuracy
- **👤 Demographics Support**: Age, height, gender, and activity level for precise recommendations
- **⚠️ Health Safety Checks**: Built-in warnings for unhealthy weight loss/gain rates
- **🔄 Auto-Goal Updates**: Weight goals automatically adjust your daily calorie targets
- **🔧 Health Connect Troubleshooting**: Comprehensive solutions for app discovery and permission issues

### 🏃 Fitness Integration (v3.0)
- **⌚ OnePlus Watch 3 Support**: Full integration with your smartwatch via Android Health Connect
- **🔥 Workout Calorie Tracking**: Automatically sync active calories burned from workouts
- **📊 Smart Goal Adjustment**: Daily calorie goals adjust based on workout intensity (70% bonus)
- **💪 Exercise Recognition**: Track workout types and duration from your watch
- **🎯 Bonus Calorie Display**: See exactly how many extra calories you earned from exercise

### 🔍 Smart Search Features (v2.0)
- **Intelligent Food Search**: Dropdown search with auto-complete from multiple food databases
- **Serving Size Calculator**: Enter portions like "2 cups", "half", "double" for automatic nutrition recalculation
- **Offline Food Storage**: SQLite-based local food storage with "(saved)" indicators
- **Barcode Cache System**: Offline storage of scanned barcodes for internet-free re-scanning
- **Material Design 3 UI**: Complete visual overhaul with modern colors, gradients, and typography

### 🛠️ Technical Improvements
- **Database Migration**: v4 → v9 with comprehensive health tracking tables
- **USDA Dataset Integration**: Successfully parsing 113,886 food items from 2025 USDA dataset
- **CSV Parser Enhancements**: Robust handling of ZIP file streams and modern dataset structures
- **Health Connect Integration**: Android's unified health platform for smartwatch data
- **Analytics Crash Fixes**: Resolved memory leaks and lifecycle issues
- **Multi-API Integration**: USDA, Open Food Facts, and Edamam APIs with proper error handling
- **Minimum SDK Update**: Android 8.0+ (API 26) for Health Connect compatibility
- **BMR/TDEE Calculator**: Scientifically-accurate calorie recommendations using Mifflin-St Jeor equation
- **Health Connect Debug Tools**: Built-in diagnostic tools for sync verification and troubleshooting
- **USDA API Integration**: Fixed food database downloads with proper API key management
- **Notification System**: AlarmManager-based precise scheduling for health reminders
- **Weight Trend Analysis**: Linear regression algorithms for intelligent calorie adjustments
- **Material Design 3**: Comprehensive UI overhaul with tabbed interface and modern components
- **Settings Architecture**: Complete tabbed Settings interface with fragment-based organization
- **Dropdown Components**: Consistent ArrayAdapter implementation across all spinner components
- **Permission Management**: Advanced permission flow handling with educational dialogs
- **Data Validation**: Type-safe database operations with proper error handling

## 📱 App Structure

**Main Screen** → **Analytics** → Charts & Insights
             → **Manual Entry** → Food logging
             → **Scan Barcode** → Camera scanning
             → **Nutrition** → Daily progress
             → **Settings** → Configuration

---

**Built with ❤️ and comprehensive analytics for better nutrition tracking!** 🥗📊