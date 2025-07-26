# OpenRouter API Troubleshooting Guide

## 401 Authentication Error

If you're getting a 401 error, it means there's an authentication problem. Here's how to fix it:

### 1. Check Your Firebase Configuration

Make sure your Firebase Firestore has the correct structure:

```
Collection: config
Document: openrouter
Fields:
  - api_key: "sk-or-v1-your-actual-key-here"
```

### 2. Verify Your OpenRouter API Key

1. **Get Your API Key**:
   - Go to [OpenRouter.ai](https://openrouter.ai)
   - Sign up or log in to your account
   - Go to Keys section
   - Create a new API key or copy existing one

2. **API Key Format**:
   - OpenRouter keys should start with `sk-or-v1-`
   - Example: `sk-or-v1-abc123def456...`
   - Make sure there are no extra spaces or characters

### 3. Common Issues and Solutions

**Issue**: API key has extra spaces
**Solution**: The code now trims the API key automatically, but make sure your Firebase field doesn't have leading/trailing spaces.

**Issue**: Wrong API key format
**Solution**: OpenRouter keys must start with `sk-or-v1-`. If you have a different format, get a new key from OpenRouter.

**Issue**: API key is expired or invalid
**Solution**: Generate a new API key from your OpenRouter dashboard.

**Issue**: Insufficient credits
**Solution**: Check your OpenRouter account balance and add credits if needed.

### 4. Firebase Setup Steps

1. **Open Firebase Console**:
   - Go to [console.firebase.google.com](https://console.firebase.google.com)
   - Select your project

2. **Navigate to Firestore**:
   - Click on "Firestore Database" in the left sidebar
   - If not set up, create a database in test mode

3. **Create Configuration**:
   - Click "Start collection"
   - Collection ID: `config`
   - Document ID: `openrouter`
   - Field name: `api_key`
   - Field type: string
   - Field value: Your OpenRouter API key (starting with `sk-or-v1-`)

### 5. Testing Your Configuration

After setting up Firebase, try the write command:

1. Open any app with a text field
2. Focus on the text field
3. Say "write code"
4. Check the logs for debug information

### 6. Debug Information

The updated code now logs:
- API key length (for verification)
- API key format (first 8 and last 4 characters)
- Request details
- Full error response body

Look for these logs in Android Studio or using `adb logcat` to diagnose issues.

### 7. Alternative Models

If you continue having issues, you can try different models in the OpenRouter API:

Current model: `openai/gpt-3.5-turbo`
Alternatives:
- `anthropic/claude-3-haiku`
- `meta-llama/llama-3.1-8b-instruct:free`
- `microsoft/wizardlm-2-8x22b`

### 8. Contact Support

If issues persist:
1. Check OpenRouter status page
2. Verify your account is in good standing
3. Contact OpenRouter support with your API key prefix (first 8 characters only)

## Example Log Output (Success)

```
WriteHandler: Retrieved OpenRouter API key from Firebase (length: 64)
WriteHandler: API key format: sk-or-v1...e2b4
WriteHandler: Making OpenRouter API request to: https://openrouter.ai/api/v1/chat/completions
WriteHandler: OpenRouter API response: {"choices":[...]}
```

## Example Log Output (401 Error)

```
WriteHandler: Retrieved OpenRouter API key from Firebase (length: 32)
WriteHandler: API key format: invalid-...key
WriteHandler: OpenRouter API error: 401
WriteHandler: Error response body: {"error": {"message": "Invalid API key"}}
```