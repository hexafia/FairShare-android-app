# 📋 FairShare Project
> [ Main README ](./README.md) &nbsp; | &nbsp; [ **Recent Changes** ] &nbsp; | &nbsp; [ [Walkthrough](./walkthrough_fairshare_first_design.md) ] &nbsp; | &nbsp; [ [Implementation Plan](./implementation_plan_fairshare.md) ]  &nbsp; | &nbsp; [ [Development Guide](./Repo_Cloning.md) ]
---

# Changes Log — April 14, 2026

## Overview
Implemented **OCR Receipt Scanning** for quick expense creation, advanced **Group Expense Split Computations**, and a full **Settle Up** flow with real-time UI updates and Firestore tracking. Included fixes for group synchronization and dynamic UI logic.

---

## 1. 🔍 OCR Receipt Scanning
- **Technology**: Integrated **Google ML Kit Text Recognition** for high-accuracy, on-device OCR.
- **High-Resolution Capture**: Implemented a `FileProvider` architecture. Instead of processing low-res thumbnails, the app captures and processes full-resolution images to ensure the ML Kit can read receipt characters clearly.
- **Intelligent Extraction**: Uses regex-based logic to extract the largest floating-point number from the receipt (typically the "Total Amount") and auto-populates the "Amount" field in expense dialogs.

## 2. ➕ Advanced Group Expense Logic
- **Split Computations**: Fixed a bug where expense breakdowns weren't being recorded.
- **Equal Split**: Automatically calculates per-person shares based on selected participants.
- **Unequal Split**: Added support for percentage-based splits. Includes a validation mechanism to ensure percentages add up to exactly 100%.
- **Authoritative Sync**: Fixed an issue where "Who Paid?" and "Who Participated?" lists only showed the creator. The UI now observes the live group membership stream, ensuring all joiners appear correctly in real-time.

## 3. 💸 "Settle Up" Tab & Flow
- **Debt Tracking**: Added a dedicated sub-tab to visualize who owes whom and for which expense.
- **Mark as Settled**: Users can now click a "Settle" button to mark a specific portion of an expense as paid.
- **Firestore Integration**: Implemented a `settledStatus` map in the `GroupExpense` model to track settlement status for each participant independently.
- **Visual UI Feedback**: Settled items are dynamically dimmed and display a "✓ Settled" badge.

## 4. 🛠️ Technical Improvements
- **Repository Pattern**: Centralized Firestore logic in `GroupRepository.java`.
- **UI Performance**: Refactored `GroupLobbyActivity.java` to use LiveData for real-time updates of balances and member counts.
- **Permissions**: Configured Android `CAMERA` permissions and `FileProvider` XML paths for secure image handling.

---

# Changes Log — April 05, 2026

## Overview
Successfully migrated the entire frontend UI design from the standalone preview project (`FairShare-frontend-preview`) into the main FairShare application. This includes a complete visual overhaul with new fonts, colors, and layouts while preserving all existing Java logic.

---

## 1. UI Design Migration

### Global Styling & Resources
- **Fonts (`res/font/`)** — [NEW] Integrated **Montserrat** (Bold, Thin) and **Roboto** (Regular) as the primary typefaces.
- **Colors (`res/values/colors.xml`)** — [MODIFIED] Added the new design palette: `teal`, `light_teal`, `orange`, `light_orange`, `off_white`, `off_black`, `gray`.
- **Typography (`res/values/type.xml`)** — [NEW] Defined `TextAppearance.MyApp` standard styles for Headings and Body text.
- **Themes (`res/values/themes.xml`)** — [MODIFIED] Added `App.Custom.Indicator` for the bottom navigation and updated status bar styling.

---

## 2. Layout Overhaul (ID Preservation)

Updated all major layout files to match the new design while ensuring `android:id` attributes match existing `ViewBinding` and `findViewById` calls in the Java controller classes.

| Layout File | Design Source | Notes |
|-------------|---------------|-------|
| `activity_main.xml` | Custom | Restyled NavHost container with tinted Bottom Navigation. |
| `activity_welcome.xml` | `activity_main.xml` (Preview) | [NEW] Landing/Welcome page as the new launcher destination. |
| `activity_login.xml` | `layout_custom_auth.xml` | Ported teal-background login/signup screen. |
| `fragment_dashboard.xml` | `dashboard.xml` | Teal header with rounded content card; preserved all summary IDs. |
| `fragment_profile.xml` | `profile.xml` | Card-based profile layout with avatar support and stat grids. |
| `fragment_groups.xml` | `group_mode_finance_tracker.xml` | Modernized group list view. |
| `fragment_ledger.xml` | `personal_expenses.xml` | Redesigned personal ledger view. |
| `fragment_notifications.xml` | `notifications.xml` | Sleek notification list layout. |
| `activity_settings.xml` | `settings.xml` | Ported full settings page design. |

---

## 3. Navigation & Flow Updates

- **`WelcomeActivity.java`** — [NEW] Initial entry point for logged-out users to choose Login or Register.
- **`SplashActivity.java`** — [MODIFIED] Updated navigation logic to point to `WelcomeActivity` instead of `LoginActivity`.
- **Drawables (`res/drawable/`)** — [NEW/MODIFIED] Migrated 10+ new vector assets (ic_back, ic_logout, bg_dashboard_content, etc.).
- **Menus (`res/menu/bottom_nav_menu.xml`)** — [MODIFIED] Updated icons to the new sleek design while keeping existing `R.id` values.

---

## 4. RecyclerView Item Layouts

- **`item_expense.xml`** — [MODIFIED] Modern card design with dynamic category color indicator.
- **`item_group.xml`** — [MODIFIED] Rounded group card with member count and share code display.
- **`item_notification.xml`** — [MODIFIED] Profile-focused notification item design.

---

## Build Status
✅ **BUILD SUCCESSFUL** (`assembleDebug`)
- All XML resources resolve correctly.
- Layout IDs verified against Java source code bindings.

---

# Changes Log — April 02, 2026

## Overview
Implemented the **Shared Expense Lobby** for real-time group finance tracking, added an **Animated Splash Screen**, and integrated **Firebase Realtime Database (RTDB)** for low-latency collaboration.

---

## 1. Feature: Shared Expense Lobby (Real-Time)

### Real-Time Synchronization (RTDB)
- **`ui/groups/GroupLobbyActivity.java`** — [NEW] Core activity for managing group expenses and viewing debt settlements.
- **`GroupRepository.java`** — [NEW] Handles RTDB operations for groups and group expenses.
- **Node Structure**:
    - `/groups/{groupId}`: Stores metadata (name, shareCode, members).
    - `/group_expenses/{groupId}`: Stores chronological list of expenses.

### Settlement Logic (Debt Simplification)
- **`DebtSimplifier.java`** — [NEW] Implements the **Minimum Debt Path** algorithm.
- **Mechanism**:
    1. **Net Balance**: Calculates `Paid - Owed` for every member across all group expenses.
    2. **Matching**: Greedily matches members with negative balances (debtors) to those with positive balances (creditors).
    3. **Optimization**: Minimizes the number of actual transactions needed to settle the group ledger.

---

## 2. Feature: Animated Splash Screen

- **`ui/splash/SplashActivity.java`** — [NEW] Initial entry point with a fade-in animation and automated transition to Login/Main.
- **`res/layout/activity_splash.xml`** — [NEW] Layout with linear gradient background.
- **`AndroidManifest.xml`** — [MODIFIED] Updated `SplashActivity` as the new `LAUNCHER` activity.

---

## 3. Firebase Infrastructure & Security

- **`UserRepository.java`** — [NEW] Firestore-backed repository for user profile management.
- **`FairShareApp.java`** — [MODIFIED] Global Firebase initialization and state management.
- **`AndroidManifest.xml`** — [MODIFIED] Added `INTERNET` and `ACCESS_NETWORK_STATE` permissions.

---

## 4. UI Components & New Layouts

| Component | Files | Description |
|-----------|-------|-------------|
| Adapters | `ui/groups/GroupAdapter.java`, `ui/groups/GroupExpenseAdapter.java`, `ui/groups/DebtAdapter.java` | Recycler view logic for groups, individual expenses, and debt settlements. |
| Dialogs | `dialog_add_group_expense.xml`, `dialog_create_group.xml`, `dialog_join_group.xml` | Functional popups for group interactions. |
| Models | `Group.java`, `GroupExpense.java`, `UserProfile.java` | Refactored models for standard serialization. |

---

## Build Status
✅ **BUILD SUCCESSFUL**
- All Firebase nodes (Firestore/RTDB) verified and operational.
- Real-time updates tested for group expense additions.

---

# Changes Log — March 30, 2026

## Overview
Refactored the FairShare app from a single-screen expense tracker into a multi-page application with Jetpack Navigation, a Login flow, and a fully designed Profile page with a Settings activity.

---

## 1. Architecture: Multi-Page Navigation

### New Dependencies
- Added `androidx.navigation:navigation-fragment:2.7.5`
- Added `androidx.navigation:navigation-ui:2.7.5`
- Updated in both `gradle/libs.versions.toml` and `app/build.gradle.kts`

### Navigation Setup
- **`res/navigation/nav_graph.xml`** — [NEW] Jetpack Navigation graph with 5 destinations (Ledger as start)
- **`res/menu/bottom_nav_menu.xml`** — [NEW] Bottom navigation with tabs: Personal, Group, Home, Notifs, Profile
- **`activity_main.xml`** — [MODIFIED] Replaced old single-page layout with `FragmentContainerView` + `BottomNavigationView`
- **`MainActivity.java`** — [MODIFIED] Refactored to be a navigation host only (NavController + BottomNav wiring)

---

## 2. Login Flow (Facebook-style persistence)

- **`activity_login.xml`** — [NEW] Login UI with email/password fields, login/signup toggle
- **`ui/login/LoginActivity.java`** — [NEW] Mock login with SharedPreferences persistence; skips login on subsequent launches
- **`AndroidManifest.xml`** — [MODIFIED] `LoginActivity` set as launcher; `MainActivity` is no longer exported

---

## 3. Feature Fragments (UI Skeletons)

| Fragment | Java File | Layout File | Description |
|----------|-----------|-------------|-------------|
| Dashboard | `ui/dashboard/DashboardFragment.java` | `fragment_dashboard.xml` | Migrated expense logic from old `MainActivity` (balance card, income/expense summary, recent transactions, add dialog) |
| Ledger | `ui/ledger/LedgerFragment.java` | `fragment_ledger.xml` | Personal Finance Ledger placeholder |
| Groups | `ui/groups/GroupsFragment.java` | `fragment_groups.xml` | Groups list placeholder |
| Group Detail | `ui/groups/GroupDetailFragment.java` | `fragment_group_detail.xml` | Group detail view placeholder |
| Notifications | `ui/notifications/NotificationsFragment.java` | `fragment_notifications.xml` | Alerts/Nudges placeholder |
| Profile | `ui/profile/ProfileFragment.java` | `fragment_profile.xml` | Fully designed (see below) |

---

## 4. Profile Page (Fully Designed)

### Layout (`fragment_profile.xml`)
- Teal header with "Profile" title, "Manage personal details" subtitle
- Settings gear icon (top-right) → opens `SettingsActivity`
- Rounded profile card overlapping the header:
  - Avatar circle with camera badge
  - Name, Email (with icon), Phone (with icon), Location (with icon)
  - "Edit Profile" button (placeholder toast)
- 4 summary stat cards in 2×2 grid:
  - Total Income (₱65,000) | Total Expenses (₱16,450)
  - Active Groups (2) | Settled IOUs (12)

### Logic (`ProfileFragment.java`)
- Gear icon click → launches `SettingsActivity`
- Edit Profile button → shows "Coming soon!" toast

---

## 5. Settings Activity (Dark Mode Toggle)

- **`activity_settings.xml`** — [NEW] Teal toolbar, Dark Mode switch card, Logout placeholder card
- **`ui/settings/SettingsActivity.java`** — [NEW] Reads/writes `darkMode` preference via `SharedPreferences`, calls `AppCompatDelegate.setDefaultNightMode()` on toggle
- **`AndroidManifest.xml`** — [MODIFIED] Registered `SettingsActivity`

---

## 6. Theme System (Light + Dark Mode)

### Colors
- **`values/colors.xml`** — [MODIFIED] Light mode: teal primary (`#2CB4A5`), orange accent (`#FF9F1C`), white backgrounds
- **`values-night/colors.xml`** — [MODIFIED] Dark mode: same teal/orange palette, navy backgrounds (`#1A1A2E`, `#16213E`)

### Themes
- **`values/themes.xml`** — [MODIFIED] Updated with `colorSurface`, `colorOnSurface`, teal status bar
- **`values-night/themes.xml`** — [MODIFIED] Dark variant with deep navy backgrounds

### Boot Persistence
- **`FairShareApp.java`** — [MODIFIED] Reads saved `darkMode` preference on startup and applies `MODE_NIGHT_YES` or `MODE_NIGHT_NO` before any Activity is created

---

## 7. New Drawable Icons (11 Vector Assets)

| Icon | File |
|------|------|
| Settings gear | `ic_settings.xml` |
| Person | `ic_person.xml` |
| Email | `ic_email.xml` |
| Phone | `ic_phone.xml` |
| Location pin | `ic_location.xml` |
| Edit pencil | `ic_edit.xml` |
| Camera | `ic_camera.xml` |
| Group/people | `ic_group.xml` |
| Home | `ic_home.xml` |
| Notifications bell | `ic_notifications.xml` |
| Wallet | `ic_wallet.xml` |

---

## 8. String Resources
- **`values/strings.xml`** — [MODIFIED] Added labels for:
  - Profile page (title, subtitle, user fields, stat card labels)
  - Settings page (dark mode label/summary)
  - Bottom navigation tabs (Personal, Group, Home, Notifs, Profile)

---

## 9. Housekeeping
- Cleaned up temporary files: `read_pdf_tmp.py`, `pdf_output_tmp.txt`, `pdf_output_utf8.txt`, `build_log.txt`, `build_log_utf8.txt`
- `.gitignore` updated to exclude `context_docs/`

---

## Build Status
✅ **BUILD SUCCESSFUL** (`assembleDebug`)  
⚠️ Minor deprecation warning on `FirebaseFirestoreSettings.Builder()` — non-blocking
