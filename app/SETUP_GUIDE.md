# 📱 Calorie Tracker App - Setup & Installation Guide

## 🎯 Quick Overview
This guide will help you build and install your Android calorie tracking app on your phone for testing. The app tracks calories and nutrition, works offline, and supports multiple regions (US, UK, Canada, Australia).

---

## 📋 Prerequisites

### Required Software
1. **Android Studio** (Latest version)
   - Download from: https://developer.android.com/studio
   - Make sure to install the Android SDK

2. **Java Development Kit (JDK)**
   - Android Studio usually includes this
   - If needed: JDK 8 or higher

### Your Phone Requirements
- **Android 8.0 (API level 26) or higher** (required for Health Connect)
- **At least 200MB free storage** (for food databases)
- **Camera** (for barcode scanning)
- **OnePlus Watch 3** (optional, for fitness integration)
- **Notification permissions** (for health reminders)

---

## 🔧 Step 1: Setup API Keys (Pre-configured)

The app now includes pre-configured API keys for USDA food database access. For enhanced online food lookups, you can optionally add Edamam API keys:

### Get Edamam API Keys (Optional)
1. Visit: https://developer.edamam.com/
2. Click **"Get Started"** → **"Sign Up"**
3. Create a free account
4. Go to **"Applications"** → **"Create a new application"**
5. Select **"Food Database API"**
6. Copy your **App ID** and **App Key**

### Add Keys to App
1. Open `NetworkManager.kt` in Android Studio
2. Find lines 23-24:
   ```kotlin
   private val edamamAppId = "YOUR_EDAMAM_APP_ID" 
   private val edamamAppKey = "YOUR_EDAMAM_APP_KEY"
   ```
3. Replace with your actual keys:
   ```kotlin
   private val edamamAppId = "abc123def456"  // Your actual App ID
   private val edamamAppKey = "xyz789uvw012" // Your actual App Key
   ```

> **Note**: The app works fine without API keys - you just won't get online food lookups

---

## 🏗️ Step 2: Build the App

### Open Project in Android Studio
1. Launch **Android Studio**
2. Click **"Open an existing project"**
3. Navigate to your `CalorieTracker` folder
4. Select the `app` folder and click **"OK"**
5. Wait for Android Studio to sync (may take 2-5 minutes first time)

### Build the APK
1. In Android Studio menu: **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait for build to complete (you'll see "BUILD SUCCESSFUL" at bottom)
3. Click **"locate"** in the notification popup, or find APK at:
   ```
   CalorieTracker/app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📱 Step 3: Install on Your Phone

### Method 1: Direct Install (Easiest)
1. Connect your phone to computer with USB cable
2. In Android Studio: **Run** → **Run 'app'**
3. Select your phone from the device list
4. App will install and launch automatically

### Method 2: APK Install
1. Copy `app-debug.apk` to your phone (via USB, email, cloud storage)
2. On your phone, go to **Settings** → **Security** → **Install unknown apps**
3. Enable installation for your file manager
4. Open the APK file on your phone
5. Tap **"Install"**

---

## ✅ Step 4: First Run Setup

### Initial Launch
1. Open the **Calorie Tracker** app
2. The app will automatically:
   - Create the database with health tracking tables
   - Set default nutrition goals (US region)
   - Prepare for comprehensive food database downloads

### Choose Your Region (Optional)
1. Tap **"Nutrition"** button on main screen
2. Tap **"Settings"** in top right
3. Select your region from dropdown (US, UK, Canada, Australia)
4. Tap **"Update Food Database for Selected Region"**
5. Tap **"Update"** to download region-specific foods
6. Tap **"Save Settings"**

---

## 🧪 Step 5: Test the App

### Test Manual Entry
1. On main screen, tap **"Manual Entry"**
2. Enter: Food name: "Apple", Calories: "95"
3. Tap **"Save"**
4. Verify it appears on main screen

### Test Barcode Scanning
1. Tap **"Scan Barcode"**
2. Grant camera permission when prompted
3. Point camera at any food barcode
4. App will either:
   - Find food in offline database → Auto-fill entry form
   - Not find food → Ask you to add manually

### Test Nutrition Dashboard
1. Tap **"Nutrition"** on main screen
2. View your nutrition breakdown with progress bars
3. Colors show your progress:
   - **Gray**: Just getting started
   - **Orange**: Getting close to goal
   - **Green**: Goal reached (good!)
   - **Red**: Over limit (for sugar/sodium)

### Test Analytics (NEW!)
1. Tap **"Analytics"** on main screen
2. View comprehensive charts and insights:
   - **Calorie Trends**: Line chart showing daily intake patterns
   - **Macro Balance**: Pie chart of protein/carbs/fat ratios
   - **Goal Streaks**: Track consecutive days meeting your goals
3. Toggle between **"Weekly"** and **"Monthly"** views
4. Charts update automatically with your food entries

### Test Settings
1. From main screen, tap **"Settings"**
2. Try changing your daily calorie goal
3. Try selecting a different region
4. Tap **"Save Settings"** and check Nutrition screen updates

---

## 🚨 Troubleshooting

### App Won't Build
- **Error**: "SDK not found"
  - **Fix**: In Android Studio: Tools → SDK Manager → Install latest Android SDK

- **Error**: "Gradle sync failed"
  - **Fix**: File → Invalidate Caches and Restart

### App Won't Install
- **Error**: "App not installed"
  - **Fix**: Enable "Install unknown apps" in phone settings
  - **Alternative**: Use direct install method instead of APK

### Camera Not Working
- **Issue**: Barcode scanner shows black screen
  - **Fix**: Go to phone Settings → Apps → Calorie Tracker → Permissions → Enable Camera

### App Crashes When Opening Nutrition Tab
- **Issue**: App force closes when tapping "Nutrition" button
  - **Fix**: Settings → Apps → Calorie Tracker → Storage → Clear Data
  - **Restart app** to recreate database with proper defaults

### App Crashes When Opening View History
- **Issue**: App force closes when tapping "View History" button
  - **Root cause**: Code bug (missing button reference)
  - **Status**: 🔧 **FIXED** - Rebuild app to get the fix
  - **Solution**: Build new APK in Android Studio and reinstall

### All Barcodes Show "Not Found"
- **Issue**: Every scanned barcode returns "Food not found"
  - **Possible causes**: No internet, API rate limits, unsupported barcode format
  - **Fix**: Check internet connection, try common product barcodes (Coca-Cola, etc.)
  - **Note**: Free API tier has 100 requests/day limit

### Barcode Dialog Flashes Repeatedly
- **Issue**: "Food not found" dialog appears and disappears rapidly multiple times
  - **Root cause**: Camera detects same barcode repeatedly without throttling
  - **Status**: 🔧 **FIXED** - Rebuild app to get the fix
  - **Solution**: Build new APK and reinstall

### Analytics Charts Are Empty
- **Issue**: Analytics screen shows blank charts
  - **Expected behavior**: Charts need multiple food entries across different days
  - **Fix**: Add several manual entries over 2-3 days, then check analytics

### No Internet Features
- **Issue**: "Food not found" even with internet
  - **Fix**: API keys are already configured, check internet connection
  - **Alternative**: Use offline database (300+ foods included)

---

## 📊 Using the App

### Quick Daily Workflow
1. **Add breakfast**: Scan barcode or manual entry
2. **Check progress**: Tap "Nutrition" to see daily progress
3. **Add lunch/dinner**: Repeat step 1
4. **Review day**: Main screen shows calorie progress
5. **Analyze trends**: Tap "Analytics" to see weekly/monthly patterns

### Key Features
- **Offline mode**: Works without internet (300+ foods stored locally)
- **Regional foods**: Different foods for US/UK/Canada/Australia
- **Nutrition tracking**: 7 nutrients tracked with daily recommendations
- **Smart barcode**: Queues unknown barcodes for later online lookup
- **Quick entry**: Buttons for 100, 200, 300, 500 calories
- **📊 Analytics Dashboard**: Interactive charts showing:
  - **Calorie trends** over 7-day or 30-day periods
  - **Macro balance** with protein/carbs/fat ratios
  - **Goal streaks** tracking consecutive successful days
- **📈 Visual Progress**: Color-coded charts with trend analysis
- **🎯 Smart Goal Tracking**: Considers 80-120% of target as "meeting goal"

---

## 🆘 Need Help?

### Check These First
1. **Main screen shows calories correctly?** ✅ Basic functionality working
2. **Barcode scanner opens camera?** ✅ Permissions set correctly  
3. **Nutrition screen shows progress bars?** ✅ Database working properly
4. **Settings save changes?** ✅ Full functionality confirmed

### Common Issues
- **Slow first launch**: Normal - app is creating database and downloading foods
- **"Food not found" messages**: Normal without API keys - add manually or get free API keys
- **Progress bars all gray**: Normal when starting - add some food entries to see progress

---

## 🎉 You're All Set!

Your enhanced calorie tracking app is now ready to use! It will:
- ✅ Track calories and 7 key nutrients
- ✅ Work offline with 300+ pre-loaded foods
- ✅ Show daily progress with color-coded feedback
- ✅ Sync unknown barcodes when internet returns
- ✅ Provide region-appropriate nutrition recommendations
- 🆕 **Display interactive analytics charts** with calorie trends
- 🆕 **Show macro balance** with protein/carbs/fat ratios
- 🆕 **Track goal streaks** for motivation and consistency
- 🆕 **Toggle between weekly/monthly** views for deeper insights

**Happy tracking and analyzing!** 🥗📱📊