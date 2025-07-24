# HeySara Android App

## Setup Process

### 1. Prerequisites
- **Android Studio** (latest stable version recommended)
- **Java JDK 11+**
- **Android SDK** (API 26+)
- **Firebase Project** (for Auth and Firestore)

### 2. Clone the Repository
```sh
git clone <your-repo-url>
cd HeySara
```

### 3. Firebase Setup
1. Go to [Firebase Console](https://console.firebase.google.com/) and create a new project (or use an existing one).
2. Add an Android app to your Firebase project:
   - Use your app's package name (e.g., `com.mvp.sarah`).
   - Download the `google-services.json` file and place it in the `app/` directory.
3. In the Firebase Console, enable **Authentication** (Email/Password) and **Cloud Firestore**.
4. Set Firestore security rules (for development):
   ```
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```
   > **Note:** Use stricter rules for production.

### 4. Build the App
- Open the project in Android Studio.
- Let Gradle sync and download dependencies.
- Build the project using **Build > Make Project** or by running:
  ```sh
  ./gradlew assembleDebug
  ```

### 5. Run the App
- Connect your Android device or start an emulator.
- Click **Run** in Android Studio, or use:
  ```sh
  ./gradlew installDebug
  ```

### 6. Permissions & Onboarding
- On first launch, the app will request all necessary permissions.
- Complete the onboarding process, including:
  - Granting permissions
  - Enabling Device Admin, Accessibility, and DND access
  - Entering your Picovoice Access Key in settings (if required)

### 7. Additional Notes
- To generate a signed APK, use **Build > Generate Signed Bundle/APK** in Android Studio.
- For production, update the Firestore rules and app version as needed.

---

For any issues, please check Logcat for errors or open an issue in the repository. 
