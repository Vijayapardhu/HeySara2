package com.mvp.sarah;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SpeakerAdapter extends RecyclerView.Adapter<SpeakerAdapter.ViewHolder> {
    public interface OnSpeakerActionListener {
        void onDelete(int pos);
        void onRename(int pos);
    }

    private final List<String> speakerNames;
    private final OnSpeakerActionListener actionListener;

    public SpeakerAdapter(List<String> speakerNames, OnSpeakerActionListener actionListener) {
        this.speakerNames = speakerNames;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.speaker_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.speakerName.setText(speakerNames.get(position));
        holder.deleteButton.setOnClickListener(v -> actionListener.onDelete(holder.getAdapterPosition()));
        holder.renameButton.setOnClickListener(v -> actionListener.onRename(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return speakerNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView speakerName;
        public Button deleteButton;
        public Button renameButton;
        public ViewHolder(View v) {
            super(v);
            speakerName = v.findViewById(R.id.speakerName);
            deleteButton = v.findViewById(R.id.deleteButton);
            renameButton = v.findViewById(R.id.renameButton);
        }
    }
} 