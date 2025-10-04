package com.hocc.fun.store;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

        LoadAndSetList();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(
                RemoveReceiver,
                new IntentFilter("com.hocc.fun.store.removerepo"),
                Context.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(RemoveReceiver);
    }
    private final BroadcastReceiver RemoveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("RemoveReceiver", "Received Broadcast!");
            RemoveRepo(intent.getStringExtra("RepoName"));
        }
    };
    public void LoadAndSetList() {
        RepoList.clear();
        int RepoCount = getSharedPreferences("Repositories", MODE_PRIVATE).getInt("RepoCount", 0);
        for (int i = 1; i <= RepoCount; i++) {
            String RepoName = getSharedPreferences("Repositories", MODE_PRIVATE).getString(String.valueOf(i), null);
            if (RepoName != null) {
                String RepoUrl = getSharedPreferences("Repositories", MODE_PRIVATE).getString(RepoName, null);
                if (RepoUrl != null) {
                    RepoList.add(new RepoItem( RepoUrl + "/icon.png", RepoName, RepoUrl));
                } else {
                    Log.e("Error", "Repo url is null");
                }
            } else {
                Log.e("Error", "Repo name is null");
            }
        }
        RepoListAdapter adapter = new RepoListAdapter(RepoList);
        recyclerView.setAdapter(adapter);
    }
    public void RemoveRepo(String RepoName) {
        int CurrentRepoCount = getSharedPreferences("Repositories", MODE_PRIVATE).getInt("RepoCount", 0);
        getSharedPreferences("Repositories", MODE_PRIVATE).edit() // All in string
                .putInt("RepoCount", CurrentRepoCount - 1)
                .remove(String.valueOf(CurrentRepoCount + 1))
                .remove(RepoName)
                .apply();
        LoadAndSetList();
    }
    public void WriteRepo(String RepoURL, String RepoName) {
        int CurrentRepoCount = getSharedPreferences("Repositories", MODE_PRIVATE).getInt("RepoCount", 0);
        if (getSharedPreferences("Repositories", MODE_PRIVATE).getString(RepoName, null) == null) {
            this.getSharedPreferences("Repositories", MODE_PRIVATE).edit() // All in string
                    .putInt("RepoCount", CurrentRepoCount + 1)
                    .putString(String.valueOf(CurrentRepoCount + 1), RepoName)
                    .putString(RepoName, RepoURL)
                    .apply();
            RepoList.add(new RepoItem( RepoURL + "/icon.png", RepoName, RepoURL));
            RepoListAdapter adapter = new RepoListAdapter(RepoList);
            recyclerView.setAdapter(adapter);
        } else {
            Toast.makeText(this,  "Repository already exists.", Toast.LENGTH_LONG).show();
        }
    }
    public void AddRepo(String RepoURL) {
        DownloadXml(RepoURL + "/index.xml", getApplicationContext(), new DownloadListener() {
            @Override
            public void onDownloadComplete(boolean success) {
                if (success) {
                    Log.i("App", "Download complete. Starting to parse XML.");
                    LoadRepoName(RepoURL, getApplicationContext());
                } else {
                    // Handle download failure
                    Log.e("App", "Download failed.");
                }
            }
        });
    }
    public void DownloadXml(String RepoURL, Context context, DownloadListener listener) {
        // Use a new Thread for the network operation
        new Thread(() -> {
            boolean success = false;
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;

            try {
                URL url = new URL(RepoURL);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                inputStream = connection.getInputStream();
                String FileName = "index.xml";
                File file = new File(context.getFilesDir(), FileName);
                outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int bufferLength = 0;

                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, bufferLength);
                }

                Log.d("DownloadXml", "XML file saved successfully to: " + file.getAbsolutePath());
                success = true;

            } catch (Exception e) {
                Log.e("DownloadXml", "Error downloading or saving XML file.", e);
            } finally {
                try {
                    if (outputStream != null) outputStream.close();
                    if (inputStream != null) inputStream.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception e) {
                    Log.e("DownloadXml", "Error closing streams.", e);
                }
            }

            // Report the result back to the main thread
            boolean finalSuccess = success;
            new Handler(Looper.getMainLooper()).post(() -> {
                listener.onDownloadComplete(finalSuccess);
            });
        }).start();
    }
    public void LoadRepoName(String RepoURL, Context context) {
        File file = new File(context.getFilesDir(), "index.xml");
        ArrayList<String> RepoAppList = new ArrayList<>();

        if (!file.exists()) {
            Log.e("XmlParser", "File not found: " + file.getAbsolutePath());
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            // --- Logic from readFeed integrated here ---
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);
            parser.nextTag(); // Advance to the root tag <List>

            // --- Logic from readList integrated here ---
            parser.require(XmlPullParser.START_TAG, null, "Repository");
            String RepoName = "null";

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                if (parser.getName().equals("RepoName")) {
                    RepoName = ReadTagContent(parser, "RepoName");
                } else {
                    Skip(parser);
                }
            }
            Log.d("Repo Name", RepoName);

            WriteRepo(RepoURL, RepoName);
        } catch (Exception e) {
            Log.e("XmlParser", "Error during XML parsing.", e);
        }
    }
    private String ReadTagContent(XmlPullParser parser, String tagName) throws Exception {
        parser.require(XmlPullParser.START_TAG, null, tagName);
        String text = "";

        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
        }

        while (parser.getEventType() != XmlPullParser.END_TAG) {
            parser.next(); // Skip to the end tag
        }

        parser.require(XmlPullParser.END_TAG, null, tagName);
        return text;
    }
    private void Skip(XmlPullParser parser) throws Exception {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
    public interface DownloadListener {
        void onDownloadComplete(boolean success);
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