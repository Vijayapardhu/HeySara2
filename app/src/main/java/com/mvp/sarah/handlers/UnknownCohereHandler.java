package com.mvp.sarah.handlers;

import android.content.Context;
import android.os.AsyncTask;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class UnknownCohereHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String COHERE_API_KEY = "OZTIkeqLrZtR1DCh4IUWIa5txiqZxPSzunhnWNLo";
    private static final String COHERE_API_URL = "https://api.cohere.ai/v1/generate";

    @Override
    public boolean canHandle(String command) {
        // This should be the fallback handler, so always return true
        return true;
    }

    @Override
    public void handle(Context context, String command) {
        new CohereTask(context, command).execute();
    }

    private static class CohereTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        private final String prompt;
        CohereTask(Context ctx, String prompt) { this.context = ctx; this.prompt = prompt; }
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(COHERE_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + COHERE_API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("model", "command");
                body.put("prompt", prompt);
                body.put("max_tokens", 60000);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.close();
                StringBuilder sb = new StringBuilder();
                int c;
                while ((c = conn.getInputStream().read()) != -1) sb.append((char) c);
                JSONObject response = new JSONObject(sb.toString());
                return response.getJSONArray("generations").getJSONObject(0).getString("text");
            } catch (Exception e) {
                return "Sorry, I couldn't get a smart reply right now.";
            }
        }
        @Override
        protected void onPostExecute(String reply) {
            FeedbackProvider.speakAndToast(context, reply);
        }
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("unknown [query]");
    }
} 