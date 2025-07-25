package com.mvp.sarah;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import android.widget.ImageView;
import java.util.Set;
import android.widget.Switch;
import androidx.cardview.widget.CardView;
import java.util.HashSet;
import java.util.HashMap;

public class CommandCategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_COMMAND = 1;

    public static class CommandItem {
        public String category;
        public String command;
        public CommandItem(String category, String command) {
            this.category = category;
            this.command = command;
        }
    }

    private List<Object> items = new ArrayList<>();
    private Map<String, Set<String>> commandToCategories;
    private Map<String, Integer> categoryIconMap;
    private Set<String> disabledCommands;

    public CommandCategoryAdapter(Map<String, List<String>> categorizedCommands, Map<String, Set<String>> commandToCategories) {
        this(categorizedCommands, commandToCategories, new HashSet<>());
    }
    public CommandCategoryAdapter(Map<String, List<String>> categorizedCommands, Map<String, Set<String>> commandToCategories, Set<String> disabledCommands) {
        this.commandToCategories = commandToCategories;
        this.disabledCommands = disabledCommands;
        this.categoryIconMap = new HashMap<>();
        // Example mapping, update as needed
        categoryIconMap.put("Alarm", R.drawable.ic_snooze);
        categoryIconMap.put("Device", R.drawable.ic_lock);
        categoryIconMap.put("Communication", R.drawable.ic_mic);
        categoryIconMap.put("Utility", R.drawable.ic_assistant);
        categoryIconMap.put("App", R.drawable.ic_launcher_foreground);
        setData(categorizedCommands);
    }

    public void setData(Map<String, List<String>> categorizedCommands) {
        items.clear();
        for (String category : categorizedCommands.keySet()) {
            items.add(category); // header
            for (String command : categorizedCommands.get(category)) {
                items.add(new CommandItem(category, command));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof String) ? TYPE_HEADER : TYPE_COMMAND;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_command_card, parent, false);
            return new CommandViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            ((HeaderViewHolder) holder).header.setText((String) items.get(position));
        } else {
            CommandItem item = (CommandItem) items.get(position);
            CommandViewHolder vh = (CommandViewHolder) holder;
            vh.command.setText(item.command);
            Set<String> categories = commandToCategories.get(item.command);
            if (categories != null) {
                vh.categories.setText(android.text.TextUtils.join(", ", categories));
                // Use the first category for the icon
                String firstCat = categories.iterator().next();
                Integer iconRes = categoryIconMap.get(firstCat);
                if (iconRes != null) {
                    vh.icon.setImageResource(iconRes);
                } else {
                    vh.icon.setImageResource(R.drawable.ic_assistant);
                }
            } else {
                vh.categories.setText(item.category);
                vh.icon.setImageResource(R.drawable.ic_assistant);
            }
            vh.toggle.setChecked(!disabledCommands.contains(item.command));
            vh.toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    disabledCommands.remove(item.command);
                } else {
                    disabledCommands.add(item.command);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView header;
        HeaderViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(android.R.id.text1);
        }
    }

    static class CommandViewHolder extends RecyclerView.ViewHolder {
        TextView command, categories;
        ImageView icon;
        Switch toggle;
        CommandViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.command_icon);
            command = itemView.findViewById(R.id.command_name);
            categories = itemView.findViewById(R.id.command_categories);
            toggle = itemView.findViewById(R.id.command_toggle);
        }
    }
} 