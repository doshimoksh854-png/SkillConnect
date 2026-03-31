# SkillConnect - Peer-to-Peer Skill Marketplace

**Bridging the gap between talent and demand**

A modern, production-ready Native Android application built with Material Design 3 (Material You) for connecting service providers with customers seeking skills.

## 🎨 Design Highlights

- **Material Design 3 (Material You)** - Modern, adaptive UI with custom purple-blue color scheme
- **Startup-grade UI/UX** - Clean, professional interface designed to impress
- **Dual Role System** - Seamless switching between Customer and Provider modes
- **Production-ready** - Follows Android best practices and clean architecture patterns

## 📱 Features

### For Customers
- Browse 6 service categories (Software Dev, Tech Support, Design, Education, Marketing, Business)
- Search and filter skills
- View detailed skill information with provider profiles
- Track booking status with visual progress indicators
- Switch to Provider mode anytime

### For Providers
- Dashboard with key statistics (Active Requests, Completed Jobs, Rating)
- Add and manage skills
- View booking requests
- Track earnings and performance
- Switch to Customer mode anytime

## 🏗️ Project Structure

```
skillconnect/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/skillconnect/
│   │       │   ├── MainActivity.java
│   │       │   ├── SplashActivity.java
│   │       │   ├── LoginActivity.java
│   │       │   ├── RegisterActivity.java
│   │       │   ├── SkillListActivity.java
│   │       │   ├── SkillDetailActivity.java
│   │       │   ├── BookingStatusActivity.java
│   │       │   ├── ProfileActivity.java
│   │       │   ├── fragments/
│   │       │   │   ├── HomeCustomerFragment.java
│   │       │   │   └── HomeProviderFragment.java
│   │       │   ├── adapters/
│   │       │   │   ├── CategoryAdapter.java
│   │       │   │   ├── FeaturedProviderAdapter.java
│   │       │   │   ├── SkillListAdapter.java
│   │       │   │   └── StatsAdapter.java
│   │       │   └── models/
│   │       │       ├── Category.java
│   │       │       ├── Provider.java
│   │       │       ├── Skill.java
│   │       │       └── Stat.java
│   │       ├── res/
│   │       │   ├── layout/          # XML layouts for all screens
│   │       │   ├── values/          # colors, themes, strings, dimens
│   │       │   ├── drawable/        # Icons and shapes
│   │       │   └── menu/            # Toolbar menus
│   │       └── AndroidManifest.xml
│   └── build.gradle
└── README.md
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Minimum SDK 24 (Android 7.0)
- Java 8 or higher

### Setup Instructions

1. **Clone or extract the project**
   ```bash
   cd c:/Users/GCETCP/skillconnect
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the skillconnect folder
   - Click OK

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle files
   - If not, click "Sync Project with Gradle Files" in the toolbar

4. **Run the Application**
   - Connect an Android device or start an emulator
   - Click the "Run" button (green play icon)
   - Select your device
   - The app will build and install

## 🎯 Screen Flow

```
Splash Screen
    ↓
Login Screen ←→ Register Screen
    ↓
Main Activity (with Fragments)
    ├── Customer Home
    │   ├── Categories Grid
    │   ├── Featured Providers
    │   └── Search → Skill List → Skill Detail → Book → Booking Status
    │
    └── Provider Home
        ├── Dashboard Stats
        ├── Add New Skill (FAB)
        └── My Skills
    
All screens → Profile (via toolbar)
```

## 🎨 Color Scheme

```xml
Primary: Deep Purple (#6750A4)
Secondary: Teal (#00897B)
Tertiary: Blue (#0061A4)
Background: Off-White (#FFFBFE)
Surface: White (#FFFBFE)
```

## 📐 Key Design Patterns

### Material Design 3 Components Used
- `MaterialCardView` - Elevated cards with rounded corners
- `MaterialButton` - Filled, outlined, and text variants
- `TextInputLayout` - Outlined text fields with validation
- `SwitchMaterial` - Role switcher
- `ExtendedFloatingActionButton` - Add skill button
- `CollapsingToolbarLayout` - Skill detail parallax effect
- `MaterialToolbar` - Top app bars

### Architecture Patterns
- **Fragment-based navigation** for home screens
- **ViewHolder pattern** for RecyclerView efficiency
- **Interface-based callbacks** for adapter click handling
- **Intent-based data passing** between activities
- **Model classes** for clean data structure

## 🔧 Customization Guide

### Changing Colors
Edit `res/values/colors.xml` to customize the color palette:
```xml
<color name="md_theme_light_primary">#YOUR_COLOR</color>
```

### Modifying Categories
Update `HomeCustomerFragment.java`:
```java
categories.add(new Category(7, "Your Category", "🎯"));
```

### Adding New Fields to Models
Extend model classes in `models/` package and update corresponding adapters.

### Changing App Name
Edit `res/values/strings.xml`:
```xml
<string name="app_name">YourAppName</string>
```

## 📝 Notes

### Frontend Only
This is a **UI/UX focused implementation**. Backend integration points are marked with `// TODO:` comments. To connect to a backend:

1. Replace sample data in fragments/activities with API calls
2. Implement authentication service in `LoginActivity` and `RegisterActivity`
3. Add networking library (Retrofit, Volley, etc.)
4. Create repository pattern for data management

### No Backend Logic
- User authentication is UI-validated only
- All data is sample/mock data
- Booking system is UI flow only
- No actual database or server communication

## 🎓 Learning Resources

- [Material Design 3](https://m3.material.io/)
- [Android Developers Guide](https://developer.android.com/)
- [RecyclerView Best Practices](https://developer.android.com/develop/ui/views/layout/recyclerview)
- [Fragment Navigation](https://developer.android.com/guide/fragments)

## 📄 License

This project is created for educational and demonstration purposes.

## 🤝 Contributing

This is a frontend showcase project. For production use:
- Add proper error handling
- Implement data persistence
- Add unit and UI tests
- Connect to backend services
- Implement proper security measures

---

**Built with ❤️ using Material Design 3 and Native Android**

