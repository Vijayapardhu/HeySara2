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

public class DictionaryHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "define serendipity",
            "what is the meaning of ubiquitous"
    );

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("define ") || command.contains("meaning of ");
    }

    @Override
    public void handle(Context context, String command) {
        String word = extractWord(command);
        if (word == null || word.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please tell me which word to define.");
            return;
        }
        new FetchDefinitionTask(context, word).execute();
    }
    
    private String extractWord(String command) {
        if (command.startsWith("define ")) {
            return command.substring("define ".length()).trim();
        }
        if (command.contains("meaning of ")) {
            return command.substring(command.indexOf("meaning of ") + "meaning of ".length()).trim();
        }
        return null;
    }

    private static class FetchDefinitionTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        private final String word;
        FetchDefinitionTask(Context context, String word) {
            this.context = context;
            this.word = word;
        }
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String urlString = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word;
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JSONArray jsonArray = new JSONArray(response.toString());
                JSONObject firstResult = jsonArray.getJSONObject(0);
                JSONArray meanings = firstResult.getJSONArray("meanings");
                JSONObject firstMeaning = meanings.getJSONObject(0);
                JSONArray definitions = firstMeaning.getJSONArray("definitions");
                JSONObject firstDefinition = definitions.getJSONObject(0);
                String definition = firstDefinition.getString("definition");
                
                return word + ": " + definition;
            } catch (Exception e) {
                return null;
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                FeedbackProvider.speakAndToast(context, result);
            } else {
                FeedbackProvider.speakAndToast(context, "Sorry, I couldn't find a definition for " + word + ".");
            }
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 