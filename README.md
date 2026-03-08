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

## 3. Managing Branches (UI Correction)
> **Note**: In newer versions of Android Studio, the Branch Widget has moved from the **bottom right** of the status bar to the **top left** of the main toolbar.

### Creating a New Branch
To avoid committing directly to `main` (e.g., when testing OCR features):
1.  Click the **branch label** (likely says `master` or `main`) at the **top left**.
2.  **Important**: If `+ New Branch...` is grayed out, you must perform at least one local **Commit** first.
3.  Once enabled, select **+ New Branch...** and enter a descriptive name (e.g., `ocr-prototype`).

## 4. The Development Workflow
To keep the repository organized, follow this "Commit and Push" cycle:

* **Commit (Ctrl + K)**: Select your files, write a clear message, and click **Commit** (not Commit and Push yet).
* **Push (Ctrl + Shift + K)**: Open the Push dialog. Ensure the mapping shows your current branch pointing to the remote branch (e.g., `ocr-prototype -> origin/ocr-prototype`).
* **Verify**: Click **Push Anyway** to ignore minor code warnings and upload the code to GitHub.

---
*Created for the FairShare Android Project - 2026*
