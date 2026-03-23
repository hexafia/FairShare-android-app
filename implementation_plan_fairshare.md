
# 📝 Implementation Plan
> [ [Main README](./README.md) ] &nbsp; | &nbsp; [ [Walkthrough](./walkthrough_fairshare_first_design.md) ] &nbsp; | &nbsp; [ **Implementation Plan** ]  &nbsp; | &nbsp; [ [Development Guide](./Repo_Cloning.md) ]
---

# Expense Tracker Implementation Plan

## Goal Description
Create a full Expense Tracker Android App with offline-first Firestore syncing. We will construct a Java-based Android app with a clean architecture (Repository + ViewModel), and we will initialize Firestore using dummy credentials with persistent offline caching enabled so you don't need a real backend just yet!

## Proposed Changes

### Configuration
#### [MODIFY] app/build.gradle.kts
- Add Firebase Firestore, AndroidX Lifecycle (ViewModel/LiveData) dependencies.
- Enable ViewBinding.
#### [MODIFY] app/src/main/AndroidManifest.xml
- Add `android:name=".FairShareApp"` to the `<application>` tag.

### Data Layer
#### [NEW] app/src/main/java/com/example/fairshare/Transaction.java
- Standard POJO representing an expense (id, title, amount, date, category).
#### [NEW] app/src/main/java/com/example/fairshare/ExpenseRepository.java
- Abstracted layer to handle inserting/fetching data from the `expenses` Firestore collection.

### Business Logic
#### [NEW] app/src/main/java/com/example/fairshare/ExpenseViewModel.java
- State mapping ViewModel that retrieves LiveData from the Repository.
#### [NEW] app/src/main/java/com/example/fairshare/FairShareApp.java
- Custom application class. In `onCreate`, we will manually initialize `FirebaseApp` using dummy settings (API key = dummy, URL = dummy) and configure `FirebaseFirestoreSettings` to enable persistent offline caching.

### UI Layer
#### [MODIFY] app/src/main/res/layout/activity_main.xml
- A Material Design layout featuring a `RecyclerView` and a `FloatingActionButton`.
#### [NEW] app/src/main/res/layout/item_expense.xml
- Clean card layout for individual list items showing title, amount, and date.
#### [NEW] app/src/main/res/layout/dialog_add_expense.xml
- A bottom sheet or dialog layout with inputs for Title, Amount, and Category.
#### [NEW] app/src/main/java/com/example/fairshare/ExpenseAdapter.java
- `RecyclerView.Adapter` to bind expenses to the UI.
#### [MODIFY] app/src/main/java/com/example/fairshare/MainActivity.java
- Wires the UI, handles user input from the dialog, and observes `ExpenseViewModel`.

## Verification Plan

### Automated Tests
1. `./gradlew assembleDebug` to verify compilation and dependency downloads succeed.

### Manual Verification
1. Open the project in Android Studio and run it on an Emulator or Device.
2. Tap the '+' FloatingActionButton and add an expense.
3. Validate it shows up in the RecyclerView immediately.
4. Kill the app and restart it to ensure the dummy Firestore's local persistence cache successfully reloads the transaction.
