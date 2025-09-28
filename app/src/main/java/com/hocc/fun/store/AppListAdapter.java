package com.hocc.fun.store;

// AppListAdapter.java
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    private final List<AppItem> appList;

    public AppListAdapter(List<AppItem> appList) {
        this.appList = appList;
    }

    // --- 1. ViewHolder Class ---
    // Holds references to the views in your list item layout
    public static class AppViewHolder extends RecyclerView.ViewHolder {
        public ImageView appIcon;
        public TextView appName;
        public TextView version;
        public TextView provider;
        public TextView button;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link the Java variables to the IDs from your XML layout
            appIcon = itemView.findViewById(R.id.AppIcon);
            appName = itemView.findViewById(R.id.AppName);
            version = itemView.findViewById(R.id.Version);
            provider = itemView.findViewById(R.id.Provider);
            button = itemView.findViewById(R.id.Button);
        }
    }

    // --- 2. onCreateViewHolder ---
    // Inflates the layout (your XML) and creates the ViewHolder
    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_view, parent, false); // Use your actual XML file name
        return new AppViewHolder(view);
    }

    // --- 3. onBindViewHolder ---
    // Binds the data from the AppList to the views in the ViewHolder
    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppItem currentItem = appList.get(position);

        String imageUrl = String.valueOf(currentItem.getIconUrl());
        ImageDownloader.downloadAndSetImage(imageUrl, holder.appIcon);
        holder.appName.setText(currentItem.getAppName());
        holder.version.setText(currentItem.getVersion());
        holder.provider.setText(currentItem.getProvider());
        holder.button.setText(currentItem.getButtonText());

        // Example: Add a click listener to the button
        holder.button.setOnClickListener(v -> {
            // Handle the button click for this specific item
            System.out.println("Clicked button for " + currentItem.getAppName());
        });
    }

    // --- 4. getItemCount ---
    // Returns the total number of items
    @Override
    public int getItemCount() {
        return appList.size();
    }
}