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
