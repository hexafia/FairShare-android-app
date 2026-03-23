
> [ [Main README](./README.md) ] &nbsp; | &nbsp; [ [Walkthrough](./walkthrough_fairshare_first_design.md) ] &nbsp; | &nbsp; [ [Implementation Plan](./implementation_plan_fairshare.md) ] &nbsp; | &nbsp; [ **Development Guide** ]
---

# 🛠 How to Clone and Build on the FairShare App

This guide covers how to download the FairShare Android project to your local machine, open it in Android Studio, and create a branch to start building new features.

## 1. Clone the Repository
Instead of downloading a ZIP file, you need to "clone" the repository so Git tracks your changes.

1. Open **Android Studio**.
2. On the Welcome screen, click **Get from VCS** (Version Control System). 
   * *(If you already have a project open, go to `File > New > Project from Version Control...`)*
3. In the **URL** field, paste the repository link:
   `https://github.com/hexafia/FairShare-android-app.git`
4. Choose a local directory on your computer to save the project.
5. Click **Clone**.

## 2. Syncing and Building
Once Android Studio opens the project, it needs to download the necessary Android dependencies.

1. Android Studio will automatically start a **Gradle Sync**. Look at the bottom right corner for a loading bar. 
2. Wait for the sync to finish completely. 
3. If you see a prompt asking to "Trust Project", click **Trust Project**.
4. To verify everything is working, click the **Green Play Button** (▶️) at the top to run the app on your emulator or connected device.

## 3. Creating Your Feature Branch
**Never build directly on the `main` branch.** Always create a separate branch for the specific feature you are working on to keep the main codebase stable.

1. Look at the top left of the Android Studio toolbar (or bottom right in older versions) and click on **`main`**.
2. Select **+ New Branch**.
3. Name your branch based on what you are building (e.g., `feature/login-screen` or `bugfix/expense-crash`).
4. Ensure **"Checkout branch"** is ticked, then click **Create**.
5. You are now safely on your own branch and can start coding!

## 4. Keeping Your Code Updated
As other teammates finish their features, the `main` branch on GitHub will be updated. You need to pull those updates into your local computer regularly.

1. Switch back to the `main` branch by clicking your current branch name and selecting **`main` > Checkout**.
2. Go to **Git > Pull...** in the top menu (or click the blue down-arrow icon `⬇️` in the toolbar).
3. Ensure it is pulling from `origin/main` and click **Pull**.
4. Switch back to your feature branch (`Your Branch Name` > **Checkout**).
5. If you need those new updates inside your feature branch, go to **Git > Merge...**, select `main`, and click **Merge**.
