# Debug Firebase Connection Issue

## Current Problem
Getting 401 "No auth credentials found" but no Firebase logs are showing. This suggests the Firebase retrieval is failing silently.

## Step-by-Step Debugging

### Step 1: Test Firebase Connection
Say: **"test firebase connection"**

This will trigger a dedicated Firebase test and show detailed logs. Look for:

```
WriteHandler: === FIREBASE CONNECTION TEST ===
WriteHandler: FirebaseFirestore instance: initialized
WriteHandler: === FIREBASE TEST RESULT ===
WriteHandler: Task successful: true/false
WriteHandler: Document exists: true/false
```

### Step 2: Check Expected Log Sequence

When you say "write code", you should see this log sequence:

```
WriteHandler: Starting Firebase API key retrieval process...
WriteHandler: Attempting to retrieve API key from Firebase...
WriteHandler: FirebaseFirestore instance: initialized
WriteHandler: Firebase document path: config/openrouter
WriteHandler: DocumentReference created: success
WriteHandler: Firebase get() callback triggered
WriteHandler: Task successful: true
WriteHandler: Firebase task successful. Document exists: true
WriteHandler: Document data: {api_key=sk-or-v1-...}
WriteHandler: Raw API key from Firebase: found
```

### Step 3: Identify the Issue

**If you don't see ANY WriteHandler logs:**
- The WriteHandler is not being called
- Check if it's registered in CommandRegistry
- Make sure you're using supported commands

**If you see "Starting Firebase..." but nothing after:**
- Firebase is hanging or timing out
- You should see a timeout message after 10 seconds
- Check internet connection
- Check Firebase project configuration

**If you see "Task successful: false":**
- Firebase operation failed
- Check the exception in logs
- Verify Firebase project is set up correctly

**If you see "Document exists: false":**
- The Firebase document doesn't exist
- Go to Firebase Console → Firestore → Create config/openrouter document

**If you see "Raw API key from Firebase: null":**
- Document exists but field is wrong
- Check field name is exactly `api_key`
- Check field has a value

### Step 4: Firebase Console Verification

1. **Go to Firebase Console**: https://console.firebase.google.com
2. **Select your project**
3. **Click "Firestore Database"**
4. **Verify structure**:
   ```
   📁 config (collection)
     📄 openrouter (document)
       🔑 api_key: "sk-or-v1-your-key-here"
   ```

### Step 5: Common Issues

**Firebase Rules**
Make sure your Firestore rules allow reading:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

**Firebase Project Configuration**
- Make sure the app is connected to the correct Firebase project
- Check `google-services.json` is in the right place
- Verify Firebase SDK is initialized in the app

**Network Issues**
- Check internet connection
- Check if Firebase endpoints are blocked
- Try on different network

### Step 6: Manual Test Commands

Try these commands and note which logs appear:

1. **"test firebase connection"** - Tests Firebase only
2. **"write code"** - Full write flow
3. **"write test"** - Simple write command

### Step 7: Log Filtering

Make sure you're seeing all logs. In Android Studio:
- Filter by "WriteHandler" 
- Set log level to "Debug" or "Verbose"
- Clear logs before testing

### Expected Troubleshooting Results

**Scenario A: Firebase Working**
```
WriteHandler: === FIREBASE CONNECTION TEST ===
WriteHandler: Task successful: true
WriteHandler: Document exists: true
WriteHandler: API key present: true
WriteHandler: API key starts with sk-or-v1: true
```
→ **Issue is in API request, not Firebase**

**Scenario B: Document Missing**
```
WriteHandler: === FIREBASE CONNECTION TEST ===
WriteHandler: Task successful: true
WriteHandler: Document exists: false
```
→ **Create the Firebase document**

**Scenario C: Firebase Failing**
```
WriteHandler: === FIREBASE CONNECTION TEST ===
WriteHandler: Task successful: false
WriteHandler: Firebase task failed: [exception details]
```
→ **Fix Firebase configuration**

**Scenario D: No Logs at All**
→ **WriteHandler not being called - check command registration**

Run the test and let me know what logs you see!