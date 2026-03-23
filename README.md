
# 📱 FairShare Project
> [ **Main README** ] &nbsp; | &nbsp; [ [Walkthrough](./walkthrough_fairshare_first_design.md) ] &nbsp; | &nbsp; [ [Implementation Plan](./implementation_plan_fairshare.md) ]
---


## Project Overview
(Your existing README content goes here...)

# FairShare-android-app
repository for Fairshare android app

# GitHub and Android Studio Integration Guide: FairShare Project

This guide outlines how to connect a local Android project to an existing GitHub repository and manage feature branching to keep the `main` branch clean.

---

## 1. Initial Account Connection
Before linking a project, Android Studio must be authorized to access your GitHub account.
* **Navigate to Settings**: Go to `File` > `Settings` (Windows) or `Android Studio` > `Settings` (macOS).
* **GitHub Section**: Go to `Version Control` > `GitHub`.
* **Add Account**: Click the **+** icon and select **Log In via GitHub** to authorize in your browser.

## 2. Connecting a Local Project to a Remote Repo
If you have already created a repository on GitHub (e.g., `FairShare-android-app`), follow these steps:
* **Enable Git**: Go to `VCS` > `Enable Version Control Integration` and select **Git**.
* **Add Remote**: Go to `Git` > `Manage Remotes...`.
* **Link URL**: Click **+**, name it `origin`, and paste the repository URL: `https://github.com/hexafia/FairShare-android-app.git`.



### 3. Tracking and Pushing the Code
Now that the remote is linked, you need to tell Git which files to actually "track."

1. **Add Files to Git:** - In the **Project** window (left side), right-click the top-level **FairShare** folder.
   - Select **Git > Add**.
   - *Result:* The file names should change from **Red** to **Green**.

2. **Commit Your Changes:**
   - Go to **Git > Commit...** (or press `Ctrl + K`).
   - In the commit window, check the box for **Unversioned Files**.
   - **IMPORTANT:** Uncheck `local.properties`. This file contains your specific computer's path to the Android SDK and will cause errors if shared with the team.
   - Type a commit message: `Initial project setup and development guide`.
   - Click the arrow on the **Commit** button and select **Commit and Push**.

3. **Push to GitHub:**
   - A dialog will appear showing the commits to be pushed.
   - Ensure the destination branch is `main`.
   - Click **Push**.

*Once the blue progress bar at the bottom finishes, your files are live on GitHub and will appear as "normal" (white/black text) in Android Studio.*

## 4. Managing Branches (UI Correction)
> **Note**: In newer versions of Android Studio, the Branch Widget has moved from the **bottom right** of the status bar to the **top left** of the main toolbar.

### Creating a New Branch
To avoid committing directly to `main` (e.g., when testing OCR features):
1.  Click the **branch label** (likely says `master` or `main`) at the **top left**.
2.  **Important**: If `+ New Branch...` is grayed out, you must perform at least one local **Commit** first.
3.  Once enabled, select **+ New Branch...** and enter a descriptive name (e.g., `ocr-prototype`).

## 5. The Development Workflow
To keep the repository organized, follow this "Commit and Push" cycle:

* **Commit (Ctrl + K)**: Select your files, write a clear message, and click **Commit** (not Commit and Push yet).
* **Push (Ctrl + Shift + K)**: Open the Push dialog. Ensure the mapping shows your current branch pointing to the remote branch (e.g., `ocr-prototype -> origin/ocr-prototype`).
* **Verify**: Click **Push Anyway** to ignore minor code warnings and upload the code to GitHub.

---
*Created for the FairShare Android Project - 2026*
