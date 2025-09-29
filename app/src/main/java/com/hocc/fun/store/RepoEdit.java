package com.hocc.fun.store;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RepoEdit extends AppCompatActivity {

    List<RepoItem> RepoList = new ArrayList<>();
    RecyclerView recyclerView;
    ImageView Add;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_repo_edit);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Add = findViewById(R.id.add);
        Add.setOnClickListener(view -> {
            AddRepoDialog(view);
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        RepoList.clear();
        RepoList.add(new RepoItem("https://raw.githubusercontent.com/HOCC2011/HOCC-Store-Repo/main/icon.png", "HOCC", "https://raw.githubusercontent.com/HOCC2011/HOCC-Store-Repo"));
        RepoList.add(new RepoItem("https://raw.githubusercontent.com/HOCC2011/Store-Test-Repo/main/icon.png", "Test", "https://raw.githubusercontent.com/HOCC2011/Store-Test-Repo"));
        RepoListAdapter adapter = new RepoListAdapter(RepoList);
        recyclerView.setAdapter(adapter);
    }

    public void AddRepo(String RepoURL) {

    }

    public void AddRepoDialog(View view){
        final AlertDialog.Builder builder = new AlertDialog.Builder(RepoEdit.this,R.style.CustomAlertDialog);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.add_repo_dia, viewGroup, false);
        TextView add = dialogView.findViewById(R.id.add);
        TextView cancel = dialogView.findViewById(R.id.cancel);
        EditText URL = dialogView.findViewById(R.id.url);
        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        add.setOnClickListener(v -> {
            AddRepo(URL.getText().toString());
            alertDialog.dismiss();
        });
        cancel.setOnClickListener(v -> {alertDialog.dismiss();});
        alertDialog.show();
    }
}