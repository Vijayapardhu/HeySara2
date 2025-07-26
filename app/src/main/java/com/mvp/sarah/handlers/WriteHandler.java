package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WriteHandler implements CommandHandler, CommandRegistry.SuggestionProvider {

    private static final String TAG = "WriteHandler";
    // OpenRouter API configuration
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        return lowerCmd.startsWith("write ") ||
               lowerCmd.startsWith("compose ") ||
               lowerCmd.startsWith("draft ") ||
               lowerCmd.contains("write code") ||
               lowerCmd.contains("write email") ||
               lowerCmd.contains("write message") ||
               lowerCmd.contains("write text") ||
               lowerCmd.contains("write letter") ||
               lowerCmd.contains("write response") ||
               lowerCmd.contains("write reply");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        
        // Extract the type of content to write
        String contentType = extractContentType(lowerCmd);
        String prompt = generatePrompt(contentType, command);
        
        FeedbackProvider.speakAndToast(context, "Writing " + contentType + "...");
        
        // Get API key from Firebase and then call OpenRouter API
        getApiKeyFromFirebase(prompt, context, contentType);
    }

    private String extractContentType(String command) {
        if (command.contains("code")) {
            return "code";
        } else if (command.contains("email")) {
            return "email";
        } else if (command.contains("message") || command.contains("text")) {
            return "message";
        } else if (command.contains("letter")) {
            return "letter";
        } else if (command.contains("response") || command.contains("reply")) {
            return "response";
        } else if (command.startsWith("write ")) {
            // Extract what comes after "write "
            String afterWrite = command.substring(6).trim();
            if (!afterWrite.isEmpty()) {
                return afterWrite;
            }
        } else if (command.startsWith("compose ")) {
            return command.substring(8).trim();
        } else if (command.startsWith("draft ")) {
            return command.substring(6).trim();
        }
        return "unknown";
    }

    private String generatePrompt(String contentType, String originalCommand) {
        switch (contentType) {
            case "code":
                return "Write a simple code example. Keep it concise and practical. " +
                       "If no specific programming language is mentioned, use Python. " +
                       "Include brief comments explaining what the code does.";
            
            case "email":
                return "Write a professional email. Keep it polite, clear, and concise. " +
                       "Include a subject line, greeting, main content, and closing. " +
                       "Make it appropriate for a business context.";
            
            case "message":
                return "Write a friendly message. Keep it casual and conversational. " +
                       "Make it suitable for texting or instant messaging.";
            
            case "letter":
                return "Write a formal letter. Include proper formatting with greeting, " +
                       "body paragraphs, and closing. Keep it professional and well-structured.";
            
            case "response":
            case "reply":
                return "Write a thoughtful response. Be polite and address the main points. " +
                       "Keep it clear and concise.";
            
            default:
                // For specific requests, use the original command
                if (!contentType.equals("unknown")) {
                    return "Write " + contentType + ". Keep it clear, concise, and helpful.";
                } else {
                    return "Write helpful text content based on this request: " + originalCommand;
                }
        }
    }

    private void getApiKeyFromFirebase(String prompt, Context context, String contentType) {
        Log.d(TAG, "Attempting to retrieve API key from Firebase...");
        DocumentReference docRef = db.collection("config").document("openrouter");
        Log.d(TAG, "Firebase document path: config/openrouter");
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                Log.d(TAG, "Firebase task successful. Document exists: " + document.exists());
                if (document.exists()) {
                    Log.d(TAG, "Document data: " + document.getData());
                    String apiKey = document.getString("api_key");
                    Log.d(TAG, "Raw API key from Firebase: " + (apiKey != null ? "found" : "null"));
                    if (apiKey != null && !apiKey.isEmpty()) {
                        apiKey = apiKey.trim(); // Clean the key
                        Log.d(TAG, "Retrieved OpenRouter API key from Firebase (length: " + apiKey.length() + ")");
                        // Log first and last few characters for debugging (but not the full key)
                        if (apiKey.length() > 10) {
                            Log.d(TAG, "API key format: " + apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4));
                        }
                        Log.d(TAG, "API key starts with 'sk-or-v1-': " + apiKey.startsWith("sk-or-v1-"));
                        callOpenRouterApi(prompt, context, contentType, apiKey);
                    } else {
                        Log.e(TAG, "OpenRouter API key is null or empty in Firebase");
                        FeedbackProvider.speakAndToast(context, "Sorry, the API key is not configured.");
                    }
                } else {
                    Log.e(TAG, "OpenRouter config document does not exist in Firebase");
                    FeedbackProvider.speakAndToast(context, "Sorry, the service is not configured.");
                }
            } else {
                Log.e(TAG, "Error getting OpenRouter config from Firebase", task.getException());
                FeedbackProvider.speakAndToast(context, "Sorry, there was an error accessing the configuration.");
            }
        });
    }

    private void callOpenRouterApi(String prompt, Context context, String contentType, String apiKey) {
        Log.d(TAG, "callOpenRouterApi called with API key length: " + (apiKey != null ? apiKey.length() : "null"));
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Log.e(TAG, "API key is null or empty in callOpenRouterApi");
            FeedbackProvider.speakAndToast(context, "Sorry, the API key is not available.");
            return;
        }
        
        apiKey = apiKey.trim(); // Ensure no whitespace
        Log.d(TAG, "Using API key for request (length: " + apiKey.length() + ")");
        
        OkHttpClient client = new OkHttpClient();
        
        JSONObject body = new JSONObject();
        try {
            body.put("model", "openai/gpt-3.5-turbo");
            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.put(userMsg);
            body.put("messages", messages);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error", e);
            FeedbackProvider.speakAndToast(context, "Sorry, there was an error preparing the request.");
            return;
        }

        String authHeader = "Bearer " + apiKey;
        Log.d(TAG, "Authorization header: Bearer " + apiKey.substring(0, Math.min(12, apiKey.length())) + "...");
        
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://github.com/your-app")
                .addHeader("X-Title", "Sarah Assistant")
                .build();
        
        Log.d(TAG, "Making OpenRouter API request to: " + API_URL);
        Log.d(TAG, "Request headers: " + request.headers().toString());
        Log.d(TAG, "Request body: " + body.toString());

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            // Fallback logic if AI is too slow
            FeedbackProvider.speakAndToast(context, "Sorry, the content generation timed out.");
        };
        handler.postDelayed(timeoutRunnable, 15000); // 15 seconds for content generation

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "OpenRouter API call failed", e);
                handler.removeCallbacks(timeoutRunnable);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    FeedbackProvider.speakAndToast(context, "Sorry, there was an error generating the content.");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handler.removeCallbacks(timeoutRunnable);
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "OpenRouter API response: " + responseBody);
                    String generatedText = parseOpenRouterResponse(responseBody);
                    if (generatedText != null && !generatedText.isEmpty()) {
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(() -> {
                            // Type the generated text into the current text field
                            Intent typeIntent = new Intent(TypeTextHandler.ACTION_TYPE_TEXT);
                            typeIntent.putExtra(TypeTextHandler.EXTRA_TEXT, generatedText);
                            context.sendBroadcast(typeIntent);
                            
                            // Provide feedback
                            String feedback = contentType.equals("unknown") ? 
                                "Text generated and typed" : 
                                contentType + " generated and typed";
                            FeedbackProvider.speakAndToast(context, feedback);
                        });
                    } else {
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(() -> {
                            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't generate the content.");
                        });
                    }
                } else {
                    Log.e(TAG, "OpenRouter API error: " + response.code());
                    // Log response body for debugging 401 errors
                    try {
                        String errorBody = response.body().string();
                        Log.e(TAG, "Error response body: " + errorBody);
                    } catch (IOException e) {
                        Log.e(TAG, "Could not read error response body", e);
                    }
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> {
                        String errorMessage = response.code() == 401 ? 
                            "Sorry, authentication failed. Please check the API key configuration." :
                            "Sorry, there was an error with the content generation service.";
                        FeedbackProvider.speakAndToast(context, errorMessage);
                    });
                }
            }
        });
    }

    private String parseOpenRouterResponse(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                return message.getString("content").trim();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing OpenRouter response", e);
        }
        return null;
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "write code",
            "write email",
            "write message",
            "write letter",
            "write response",
            "compose email",
            "draft message",
            "write a function",
            "write a hello world program",
            "write a professional email"
        );
    }
}