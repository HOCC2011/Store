package com.hocc.fun.store;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RepoListAdapter extends RecyclerView.Adapter<RepoListAdapter.RepoViewHolder> {

    private final List<RepoItem> repoList;

    public RepoListAdapter(List<RepoItem> repoList) {
        this.repoList = repoList;
    }

    public static class RepoViewHolder extends RecyclerView.ViewHolder {
        public ImageView repoIcon;
        public TextView repoName;
        public TextView repoUrl;
        public ConstraintLayout main;

        public RepoViewHolder(@NonNull View itemView) {
            super(itemView);
            repoIcon = itemView.findViewById(R.id.RepoIcon);
            repoName = itemView.findViewById(R.id.RepoName);
            repoUrl = itemView.findViewById(R.id.RepoUrl);
            main = itemView.findViewById(R.id.main);
        }
    }

    @NonNull
    @Override
    public RepoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.repo_view, parent, false); // Use your actual XML file name
        return new RepoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RepoViewHolder holder, int position) {
        RepoItem currentItem = repoList.get(position);

        String imageUrl = String.valueOf(currentItem.getIconUrl());
        ImageDownloader.downloadAndSetImage(imageUrl, holder.repoIcon);
        holder.repoName.setText(currentItem.getRepoName());
        holder.repoUrl.setText(currentItem.getRepoUrl());

        holder.main.setOnLongClickListener(view -> {
            RemoveRepoDialog(view.getContext(), view, holder.repoName.getText().toString());
            return true;
        });
    }

    @SuppressLint("MissingInflatedId")
    public void RemoveRepoDialog(Context context, View view, String RepoName){
        final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext(), R.style.CustomAlertDialog);
        ViewGroup viewGroup = view.findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.remove_repo_dia, viewGroup, false);
        TextView remove = dialogView.findViewById(R.id.remove);
        TextView cancel = dialogView.findViewById(R.id.cancel);
        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();
        remove.setOnClickListener(v -> {
            Intent intent = new Intent("com.hocc.fun.store.removerepo").setPackage(context.getPackageName());
            intent.putExtra("RepoName", RepoName);
            context.sendBroadcast(intent);
            alertDialog.dismiss();
        });
        cancel.setOnClickListener(v -> {alertDialog.dismiss();});
        alertDialog.show();
    }

    @Override
    public int getItemCount() {
        return repoList.size();
    }
}