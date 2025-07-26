package com.mvp.sarah;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;

public class ManageCommandsActivity extends AppCompatActivity {
    private static final String PREFS = "SaraSettingsPrefs";
    private static final String KEY_DISABLED_COMMANDS = "disabled_commands";

    private EditText searchCommandsEditText;
    private RecyclerView commandsRecyclerView;
    private CommandCategoryAdapter commandAdapter;
    private Map<String, List<String>> categorizedCommands = new TreeMap<>();
    private Set<String> disabledCommands = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_commands);

        searchCommandsEditText = findViewById(R.id.search_commands);
        commandsRecyclerView = findViewById(R.id.commands_recycler_view);
        commandsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        disabledCommands = prefs.getStringSet(KEY_DISABLED_COMMANDS, new HashSet<>());

        // Gather and categorize commands
        List<CommandHandler> handlers = CommandRegistry.getAllHandlers();
        categorizedCommands.clear();
        Map<String, Set<String>> commandToCategories = new HashMap<>();
        for (CommandHandler handler : handlers) {
            String category = handler.getClass().getSimpleName().replace("Handler", "");
            if (handler instanceof CommandRegistry.SuggestionProvider) {
                List<String> suggestions = ((CommandRegistry.SuggestionProvider) handler).getSuggestions();
                Collections.sort(suggestions, String.CASE_INSENSITIVE_ORDER);
                if (!categorizedCommands.containsKey(category)) {
                    categorizedCommands.put(category, new ArrayList<>());
                }
                for (String command : suggestions) {
                    categorizedCommands.get(category).add(command);
                    if (!commandToCategories.containsKey(command)) {
                        commandToCategories.put(command, new HashSet<>());
                    }
                    commandToCategories.get(command).add(category);
                }
            }
        }
        commandAdapter = new CommandCategoryAdapter(categorizedCommands, commandToCategories);
        commandsRecyclerView.setAdapter(commandAdapter);

        searchCommandsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCommands(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterCommands(String query) {
        Map<String, List<String>> filtered = new TreeMap<>();
        for (String category : categorizedCommands.keySet()) {
            List<String> filteredList = new ArrayList<>();
            for (String command : categorizedCommands.get(category)) {
                if (command.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(command);
                }
            }
            if (!filteredList.isEmpty()) {
                filtered.put(category, filteredList);
            }
        }
        commandAdapter.setData(filtered);
    }
} 