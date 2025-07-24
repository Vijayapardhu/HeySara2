package com.mvp.sarah;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppLockAdapter extends RecyclerView.Adapter<AppLockAdapter.ViewHolder> {
    public interface OnToggleLockListener {
        void onToggle(ManageAppLockActivity.AppLockItem item, boolean locked);
    }
    private final List<ManageAppLockActivity.AppLockItem> appList;
    private final OnToggleLockListener toggleListener;
    private final Context context;

    public AppLockAdapter(List<ManageAppLockActivity.AppLockItem> appList, OnToggleLockListener listener) {
        this.appList = appList;
        this.toggleListener = listener;
        this.context = null;
    }
    public AppLockAdapter(List<ManageAppLockActivity.AppLockItem> appList, OnToggleLockListener listener, Context ctx) {
        this.appList = appList;
        this.toggleListener = listener;
        this.context = ctx;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_lock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ManageAppLockActivity.AppLockItem item = appList.get(position);
        holder.appName.setText(item.appName);
        holder.btnLockToggle.setText(item.locked ? "Unlock" : "Lock");
        holder.btnLockToggle.setIconResource(item.locked ? R.drawable.ic_lock : R.drawable.ic_unlock);
        holder.btnLockToggle.setOnClickListener(v -> {
            boolean newLocked = !item.locked;
            toggleListener.onToggle(item, newLocked);
            item.locked = newLocked;
            notifyItemChanged(position);
        });
        // Visual cues
        if (item.locked) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0")); // light orange
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#F5F5F5")); // light gray
        }
        // Load app icon if possible
        Drawable icon = null;
        try {
            Context ctx = context != null ? context : holder.itemView.getContext();
            PackageManager pm = ctx.getPackageManager();
            icon = pm.getApplicationIcon(item.packageName);
        } catch (Exception e) {
            icon = ctxOr(holder).getDrawable(R.drawable.ic_launcher_foreground);
        }
        holder.appIcon.setImageDrawable(icon);
    }

    public void updateList(List<ManageAppLockActivity.AppLockItem> newList) {
        appList.clear();
        appList.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        MaterialButton btnLockToggle;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            btnLockToggle = itemView.findViewById(R.id.btn_lock_toggle);
        }
    }

    private Context ctxOr(ViewHolder holder) {
        return context != null ? context : holder.itemView.getContext();
    }
} 