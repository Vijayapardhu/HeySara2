package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
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
    private static final String API_KEY = "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";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

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
        
        // Generate content using OpenRouter API
        callOpenRouterApi(prompt, context, contentType);
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

    private void callOpenRouterApi(String prompt, Context context, String contentType) {
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

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

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
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> {
                        FeedbackProvider.speakAndToast(context, "Sorry, there was an error with the content generation service.");
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