# 🏠 Home

**Welcome to the Sara Wiki!**

Sara is an intelligent voice assistant for Android, designed to help you manage your device, tasks, and communication—all hands-free. This wiki provides comprehensive documentation for users, contributors, and developers.

**Quick Links:**
- [Getting Started](#getting-started)
- [Features](#features)
- [Mobile Setup Guide](#mobile-setup-guide)
- [FAQ](#faq)
- [Contributing](#contributing)
- [Troubleshooting](#troubleshooting)

---

# 🚀 Getting Started

## Prerequisites

- Android device (API 26+)
- [Sara APK](https://github.com/your-repo/releases) or build from source
- [Picovoice Access Key](https://console.picovoice.ai/)

## Installation Steps

1. [Disable Play Protect](#1-disable-play-protect-before-installing-the-apk)
2. [Sideload and Install the APK](#2-how-to-sideload-and-install-the-apk)
3. [Get a Picovoice Access Key](#3-how-to-get-a-picovoice-access-key)
4. [Grant All Permissions](#4-how-to-grant-all-restricted-permissions)
5. Complete onboarding in the app

---

# ✨ Features

- **Voice-Activated Commands:** Wake Sara and control your device with your voice.
- **Communication:** Make calls, send SMS, and WhatsApp messages.
- **Productivity:** Set alarms, reminders, and notes.
- **Device Control:** Adjust brightness, volume, and more.
- **Accessibility:** Designed for all users, including those with disabilities.
- **Offline Support:** Many commands work without internet.
- **Privacy:** No ads, no tracking, your data stays with you.

---

# 📱 Mobile Setup Guide

## 1. Disable Play Protect (Before Installing the APK)

> **Warning:** Disabling Play Protect reduces your device’s security. Only do this if you trust the APK source.

1. Open the **Google Play Store** app.
2. Tap your **profile icon** in the top right corner.
3. Tap **Play Protect**.
4. Tap the **Settings** icon (top right).
5. Turn off **Scan apps with Play Protect** and confirm.

---

## 2. How to Sideload and Install the APK

1. Transfer the APK to your device (USB, email, or cloud).
2. Go to **Settings > Apps & notifications > Special app access > Install unknown apps**.
3. Select your file manager or browser and enable **Allow from this source**.
4. Open your file manager, tap the APK, and follow the prompts to install.

---

## 3. How to Get a Picovoice Access Key

1. Go to [https://console.picovoice.ai/](https://console.picovoice.ai/)
2. Sign up or log in.
3. Click **Access Keys** in the sidebar.
4. Click **Create Access Key**, name it, and click **Create**.
5. Copy the key and paste it into the HeySara app’s settings.

---

## 4. How to Grant All Restricted Permissions

- During onboarding, tap **Allow** for all permission dialogs.
- If you missed any:
  - Go to **Settings > Apps > HeySara > Permissions** and enable all permissions.
  - Enable Device Admin: **Settings > Security > Device admin apps > HeySara**.
  - Enable Accessibility: **Settings > Accessibility > Installed services > HeySara**.
  - Enable DND: **Settings > Special app access > Do Not Disturb access > HeySara**.
  - Enable Overlay: **Settings > Special app access > Display over other apps > HeySara**.
  - Enable Notification Access: **Settings > Special app access > Notification access > HeySara**.
  - Enable Background Location: **Settings > Apps > HeySara > Permissions > Location > Allow all the time**.

---

## 5. First Launch & Onboarding

- Open **HeySara**.
- Grant all permissions when prompted.
- Complete onboarding (Device Admin, Accessibility, DND, Overlay, Notification access, Picovoice key).

---

# ❓ FAQ

**Q: Why do I need to disable Play Protect?**  
A: Play Protect may block unsigned or custom APKs. Disabling it allows you to install Sara safely.

**Q: What is a Picovoice Access Key?**  
A: It’s a free key from [Picovoice Console](https://console.picovoice.ai/) that enables voice wakeword detection.

**Q: The app isn’t recognizing my voice. What should I do?**  
A: Ensure microphone permission is granted and your Picovoice key is correct.

**Q: How do I update Sara?**  
A: Download the latest APK from [Releases](https://github.com/your-repo/releases) and install it over your current version.

---

# 🛠️ Contributing

We welcome contributions!

- See [CONTRIBUTING.md](https://github.com/your-repo/CONTRIBUTING.md) for guidelines.
- Open issues for bugs or feature requests.
- Submit pull requests for improvements.

---

# 🐞 Troubleshooting

- **App not installed:** Uninstall any previous version first.
- **Permission denied:** Go to Settings > Apps > HeySara > Permissions and enable manually.
- **Picovoice not working:** Double-check your access key and internet connection.
- **Login/registration issues:** Ensure you have a stable internet connection and all permissions are granted.

---

# 📄 License

Sara is released under the [MIT License](https://github.com/your-repo/LICENSE).

---

**Tip:**  
You can create a new Wiki page for each section above, or use this as a single page. Add screenshots or GIFs for visual guidance as needed!

If you want more technical/developer documentation, just ask! 