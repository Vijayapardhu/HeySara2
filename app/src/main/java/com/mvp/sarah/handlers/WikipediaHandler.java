package com.mvp.sarah.handlers;

import android.content.Context;
import android.os.AsyncTask;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WikipediaHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        return command.toLowerCase(Locale.ROOT).startsWith("tell me about ");
    }

    @Override
    public void handle(Context context, String command) {
        String topic = command.substring("tell me about ".length()).trim();
        if (topic.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify a topic.");
            return;
        }
        new WikiTask(context, topic).execute();
    }

    private static class WikiTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        private final String topic;
        WikiTask(Context ctx, String topic) { this.context = ctx; this.topic = topic; }
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String urlStr = "https://en.wikipedia.org/api/rest_v1/page/summary/" + URLEncoder.encode(topic, "UTF-8");
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                JSONObject json = new JSONObject(sb.toString());
                if (json.has("extract")) {
                    return json.getString("extract");
                } else {
                    return "Sorry, I couldn't find information about " + topic + ".";
                }
            } catch (Exception e) {
                return "Sorry, I couldn't fetch information from Wikipedia.";
            }
        }
        @Override
        protected void onPostExecute(String summary) {
            FeedbackProvider.speakAndToast(context, summary);
        }
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("tell me about [topic]");
    }
} 