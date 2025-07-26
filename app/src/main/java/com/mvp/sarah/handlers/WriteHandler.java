package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WriteHandler implements CommandHandler, CommandRegistry.SuggestionProvider {

    private static final String TAG = "WriteHandler";
    // Using the same API key as GeminiHandler
    private static final String API_KEY = "AIzaSyAH4ljHr5OTLWBDIOSPW1xzNzpxNN3L7kA";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + API_KEY;

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
        
        // Generate content using Gemini API in a background thread
        new Thread(() -> {
            try {
                String generatedText = generateContent(prompt);
                
                if (generatedText != null && !generatedText.isEmpty()) {
                    // Type the generated text into the current text field
                    Intent typeIntent = new Intent(TypeTextHandler.ACTION_TYPE_TEXT);
                    typeIntent.putExtra(TypeTextHandler.EXTRA_TEXT, generatedText);
                    context.sendBroadcast(typeIntent);
                    
                    // Provide feedback
                    String feedback = contentType.equals("unknown") ? 
                        "Text generated and typed" : 
                        contentType + " generated and typed";
                    FeedbackProvider.speakAndToast(context, feedback);
                } else {
                    FeedbackProvider.speakAndToast(context, "Sorry, I couldn't generate the content.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error generating content", e);
                FeedbackProvider.speakAndToast(context, "Sorry, there was an error generating the content.");
            }
        }).start();
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

    private String generateContent(String prompt) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        // Create the JSON request body
        String jsonInputString = "{\"contents\":[{\"parts\":[{\"text\":\"" + JSONObject.quote(prompt) + "\"}]}]}";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                
                // Parse the JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                String text = jsonResponse.getJSONArray("candidates")
                                          .getJSONObject(0)
                                          .getJSONObject("content")
                                          .getJSONArray("parts")
                                          .getJSONObject(0)
                                          .getString("text");

                return text;
            }
        } else {
            String errorMessage = "Error from API: " + responseCode + " " + conn.getResponseMessage();
            Log.e(TAG, errorMessage);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    errorResponse.append(responseLine.trim());
                }
                Log.e(TAG, "Error details: " + errorResponse.toString());
            }
            throw new Exception("API call failed: " + errorMessage);
        }
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