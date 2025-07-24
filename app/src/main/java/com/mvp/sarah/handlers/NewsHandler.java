package com.mvp.sarah.handlers;

import android.content.Context;
import android.os.AsyncTask;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class NewsHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "news",
            "live news",
            "show me the news",
            "what's the news",
            "read the news"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("news") || lower.contains("headlines");
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Fetching the latest news headlines...");
        new FetchNewsTask(context).execute();
    }

    private static class FetchNewsTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        FetchNewsTask(Context context) { this.context = context; }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // You can replace this with your own API key and endpoint
                String urlStr = "https://newsdata.io/api/1/news?apikey=pub_34508e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7&country=us&language=en&category=top";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                JSONObject json = new JSONObject(response.toString());
                JSONArray articles = json.optJSONArray("results");
                if (articles == null || articles.length() == 0) return null;
                StringBuilder headlines = new StringBuilder();
                for (int i = 0; i < Math.min(5, articles.length()); i++) {
                    JSONObject article = articles.getJSONObject(i);
                    String title = article.optString("title");
                    if (title != null && !title.isEmpty()) {
                        headlines.append(i + 1).append(": ").append(title).append(". ");
                    }
                }
                return headlines.toString().trim();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.isEmpty()) {
                FeedbackProvider.speakAndToast(context, "Here are the top news headlines: " + result);
            } else {
                FeedbackProvider.speakAndToast(context, "Sorry, I couldn't fetch the news right now.");
            }
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 