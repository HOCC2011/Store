package com.hocc.fun.store;

// AppListAdapter.java
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    private final List<AppItem> appList;

    public AppListAdapter(List<AppItem> appList) {
        this.appList = appList;
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        public ImageView appIcon;
        public TextView appName;
        public TextView version;
        public TextView provider;
        public TextView button;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.AppIcon);
            appName = itemView.findViewById(R.id.AppName);
            version = itemView.findViewById(R.id.Version);
            provider = itemView.findViewById(R.id.Provider);
            button = itemView.findViewById(R.id.Button);
        }
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_view, parent, false); // Use your actual XML file name
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppItem currentItem = appList.get(position);

        String imageUrl = String.valueOf(currentItem.getIconUrl());
        ImageDownloader.downloadAndSetImage(imageUrl, holder.appIcon);
        holder.appName.setText(currentItem.getAppName());
        holder.version.setText(currentItem.getVersion());
        holder.provider.setText(currentItem.getProvider());
        holder.button.setText(currentItem.getButtonText());

        holder.button.setOnClickListener(v -> {
            if (holder.button.getText().toString().equals("Download") || holder.button.getText().toString().equals("Update")) {
                String url = currentItem.getDownloadUrl();

                // 1. Create an Intent to view the URI
                Intent intent = new Intent(Intent.ACTION_VIEW);

                // 2. Parse the URL string into a Uri object
                intent.setData(Uri.parse(url));

                try {
                    // 3. Start the Intent, which opens the default handler (Browser, etc.)
                    holder.itemView.getContext().startActivity(intent);
                } catch (Exception e) {
                    // Handle case where no app can handle the intent (highly unlikely for a web URL)
                    e.printStackTrace();
                    // Optionally show a Toast message here
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }
}