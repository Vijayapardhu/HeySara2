# 📱 HeySara: Complete Mobile Setup Guide

---

## 1. Disable Play Protect (Before Installing the APK)

> **Warning:** Disabling Play Protect reduces your device’s security. Only do this if you trust the APK source.

**Step 1:** Open the **Google Play Store** app.

**Step 2:** Tap your **profile icon** in the top right corner.

**Step 3:** Tap **Play Protect**.

**Step 4:** Tap the **Settings** icon (top right).

**Step 5:** Turn off **Scan apps with Play Protect**.
- You may see a warning. Confirm to proceed.

---

## 2. How to Sideload and Install the APK

**Step 1:** Transfer the APK to your device
- Use a USB cable, email, or cloud storage (Google Drive, Dropbox, etc.) to move the APK file to your phone.

**Step 2:** Enable “Install Unknown Apps”
- Go to **Settings > Apps & notifications > Special app access > Install unknown apps**.
- Select the app you’ll use to open the APK (e.g., your file manager or browser).
- Toggle **Allow from this source** ON.

**Step 3:** Install the APK
- Open your file manager and navigate to the APK file.
- Tap the APK file.
- If prompted, allow installation from unknown sources.
- Follow the prompts to complete installation.

---

## 3. How to Get a Picovoice Access Key

**Step 1:** Go to the [Picovoice Console](https://console.picovoice.ai/)
- Open your browser and visit: https://console.picovoice.ai/

**Step 2:** Sign Up or Log In
- If you don’t have an account, click **Sign Up** and fill in your details.
- If you already have an account, click **Log In** and enter your credentials.

**Step 3:** Create a New Access Key
- Once logged in, look for the **Access Keys** section in the left sidebar.
- Click on **Access Keys**.
- Click the **Create Access Key** button (usually at the top right).
- Enter a name for your key (e.g., “HeySaraApp”).
- Click **Create**.

**Step 4:** Copy the Access Key
- Your new key will appear in the list.
- Click the **copy** icon next to the key to copy it to your clipboard.

**Step 5:** Enter the Access Key in HeySara
- Open the HeySara app on your phone.
- Go to **Settings** (or follow the onboarding prompts).
- Find the field labeled **Picovoice Access Key**.
- Paste your copied key into this field and save.

---

## 4. How to Grant All Restricted Permissions

### A. During Onboarding
- When you first open HeySara, the app will request several permissions.
- Tap **Allow** or **Grant** for each permission dialog that appears.

### B. Manually Granting Permissions (if you missed any)

#### 1. App Permissions
- Open **Settings** on your Android device.
- Go to **Apps** or **Apps & notifications**.
- Find and tap on **HeySara**.
- Tap **Permissions**.
- Enable all permissions:
  - **Microphone**
  - **Camera**
  - **Location** (both coarse and fine)
  - **Phone**
  - **SMS**
  - **Contacts**
  - **Storage/Files and media**
  - **Others** as requested

#### 2. Device Admin Permission
- Go to **Settings > Security > Device admin apps** (or **Device administrators**).
- Find **HeySara** in the list.
- Toggle it ON and confirm.

#### 3. Accessibility Service
- Go to **Settings > Accessibility > Installed services** (or **Downloaded services**).
- Find **HeySara** (or the service name, e.g., “EnhancedAccessibilityService”).
- Tap it, then toggle the switch ON.
- Confirm the warning dialog.

#### 4. Do Not Disturb (DND) Access
- Go to **Settings > Apps & notifications > Special app access > Do Not Disturb access**.
- Find **HeySara** and toggle it ON.

#### 5. Display Over Other Apps (Overlay Permission)
- Go to **Settings > Apps & notifications > Special app access > Display over other apps**.
- Find **HeySara** and toggle it ON.

#### 6. Notification Access (if required)
- Go to **Settings > Apps & notifications > Special app access > Notification access**.
- Find **HeySara** and toggle it ON.

#### 7. Background Location (if required)
- Go to **Settings > Apps > HeySara > Permissions > Location**.
- Select **Allow all the time**.

---

## 5. First Launch & Onboarding
- Open **HeySara**.
- Grant all permissions when prompted.
- Complete onboarding:
  - Enable Device Admin, Accessibility, DND, Overlay, and Notification access as prompted.
  - Enter your Picovoice Access Key in settings or onboarding.

---

## 6. Troubleshooting
- **App not installed:** Uninstall any previous version first.
- **Permission denied:** Go to **Settings > Apps > HeySara > Permissions** and enable manually.
- **Picovoice not working:** Double-check your access key and internet connection.
- **Login/registration issues:** Ensure you have a stable internet connection and all permissions are granted.
- **Play Protect warning:** If you see a warning, confirm you want to install anyway.

---

## 7. Summary Table

| Step                | Where to Go                                      | What to Do                                 |
|---------------------|--------------------------------------------------|--------------------------------------------|
| Play Protect        | Play Store > Play Protect > Settings             | Turn off “Scan apps with Play Protect”     |
| Sideload APK        | File Manager                                     | Tap APK, allow install from unknown source |
| Picovoice Key       | https://console.picovoice.ai/                    | Create & copy access key                   |
| Permissions         | Settings > Apps > HeySara > Permissions          | Enable all permissions                     |
| Device Admin        | Settings > Security > Device admin apps          | Enable HeySara                             |
| Accessibility       | Settings > Accessibility > Installed services    | Enable HeySara                             |
| DND Access          | Settings > Special app access > DND access       | Enable HeySara                             |
| Overlay             | Settings > Special app access > Display over apps| Enable HeySara                             |
| Notification Access | Settings > Special app access > Notification access | Enable HeySara                          | 