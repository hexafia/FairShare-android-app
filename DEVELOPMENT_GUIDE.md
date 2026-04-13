# Development Guide & Project Roadmap

This document explains the current state of the **FairShare Expense Tracker** and provides a technical roadmap for future enhancements.

## Current Project Highlights

### 1. Robust Architecture
- **MVVM Pattern**: Uses `ViewModel` and `LiveData` for a clean separation of concerns. UI logic is in `MainActivity.java`, while data management is handled by `ExpenseViewModel.java` and `ExpenseRepository.java`.
- **ViewBinding**: Enabled for efficient and safe UI element access, replacing the older `findViewById`.

### 2. Offline-First Synchronization
- **Firestore Persistence**: Configured in `FairShareApp.java` to use a large local cache (`FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED`).
- **Synchronous-Feel Writes**: Transactions are committed to the local cache immediately, and Firestore's SDK handles background synchronization when the network is available.
- **Real-Time Updates**: Uses a snapshot listener in `ExpenseRepository` to automatically update the UI when data changes in the cloud.

### 3. Premium UI & UX
- **Material Design 3**: Modern, clean, and interactive. Featuring a dynamic "Balance Card" and category-coded transaction entries.
- **Interactive Dialog**: Input validation for title and amount ensures clean data entry.
- **Swipe Action Compatibility**: The current `ExpenseAdapter` uses `ListAdapter` with `DiffUtil`, which is primed for future features like swipe-to-edit or list reordering.

---

## Technical Setup: Enabling Cloud Sync

The project currently uses a **placeholder** `google-services.json`. To enable real-world syncing:
1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Register an Android app using the package name `com.example.fairshare`.
3. Download the `google-services.json` file and replace the one in `/app/google-services.json`.
4. Enable **Cloud Firestore** and set the security rules (start with "Test Mode" for initial development).

---

## Future Roadmap & Features

Building on this foundation, several high-value features can be added:

### 1. User Authentication
**Implementation**: Use **Firebase Authentication**.
- **Benefit**: Each user has their own private data storage.
- **Logic**: Update Firestore queries in `ExpenseRepository` to filter by a `userId` field.

### 2. Group Sharing (True "FairShare")
**Implementation**: Create a `groups` collection in Firestore.
- **Benefit**: Allow multiple users to see and add transactions to a shared budget (e.g., for roommates or partners).
- **Logic**: Transactions would point to a `groupId` instead of an individual `userId`.

### 3. Data Visualization
**Implementation**: Integrate the [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) library.
- **Benefit**: Visual breakdown (Pie Charts, Bar Charts) of expenses by category.
- **Logic**: Add a method in `ExpenseViewModel` that aggregates transaction data into category-wise sums.

### 4. Recurring Transactions
**Implementation**: Use `WorkManager` API.
- **Benefit**: Automatically add monthly rent or weekly subscription transactions.
- **Logic**: Schedule a worker to fire periodically and call `repository.addExpense()`.

### 5. Search & Advanced Filtering
**Implementation**: Add a `SearchView` to the header.
- **Benefit**: Quickly find past transactions by title, category, or date range.
- **Logic**: Use Firestore query indexing for efficient searching across thousands of entries.

### 6. Export to CSV/PDF
**Implementation**: Use the Android `File` API and a PDF generation library (like `itext7`).
- **Benefit**: Use for personal accounting or sharing financial statements.
