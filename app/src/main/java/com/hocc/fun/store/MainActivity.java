package com.hocc.fun.store;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public HashMap<String, String[]> Apps = new HashMap<>();
    public HashMap<String, ArrayList> RepoApps = new HashMap<>();
    List<AppItem> Applist = new ArrayList<>();
    RecyclerView recyclerView;
    ImageView reload;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        reload = findViewById(R.id.reload);
        reload.setOnClickListener(view -> {
            Toast.makeText(this, "Loading data...", Toast.LENGTH_LONG).show();
            Applist.clear();
            LoadRepo("HOCC", "https://raw.githubusercontent.com/HOCC2011/HOCC-Store-Repo/main/index.xml");
            LoadRepo("Test", "https://raw.githubusercontent.com/HOCC2011/Store-Test-Repo/main/index.xml");
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Applist.clear();
        LoadRepo("HOCC", "https://raw.githubusercontent.com/HOCC2011/HOCC-Store-Repo/main/index.xml");
        LoadRepo("Test", "https://raw.githubusercontent.com/HOCC2011/Store-Test-Repo/main/index.xml");
        Log.d("Android Version", String.valueOf(Build.VERSION.SDK_INT));
    }

    public void EditList(String RepoName, String RepoUrl) {
        int AppNumber = RepoApps.get(RepoName).size();
        Log.d("App Number", String.valueOf(AppNumber));
        for (int i = 1; i <= AppNumber; i++) {
            String AppName = (String) RepoApps.get(RepoName).get(i - 1);
            String PackageName = Apps.get(AppName)[0];
            Log.d("PackageName", PackageName);
            String Version = Apps.get(AppName)[1].replaceAll("\\s+", "");
            String SystemRequirment = Apps.get(AppName)[2];
            if (Build.VERSION.SDK_INT > Integer.valueOf(SystemRequirment)){
                String ButtonText = "null";
                PackageManager pm = getPackageManager();
                try {
                    pm.getPackageInfo(PackageName.replaceAll("\\s+", ""), PackageManager.GET_ACTIVITIES);
                    android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(PackageName, PackageManager.GET_ACTIVITIES);
                    if (packageInfo.versionName.replaceAll("\\s+", "").equals(Version)) {
                        ButtonText = "Installed";
                    } else {
                        ButtonText = "Update";
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    ButtonText = "Download";
                }
                String IconUrl = RepoUrl.replace("index.xml",  AppName + "/" + Version + ".png");
                String AppUrl = RepoUrl.replace("index.xml",  AppName + "/" + Version + ".apk");
                Applist.add(new AppItem(IconUrl, AppName, Version, RepoName, ButtonText, AppUrl));
            }
        }
        AppListAdapter adapter = new AppListAdapter(Applist);
        recyclerView.setAdapter(adapter);
    }

    public void LoadRepo(String RepoName, String RepoUrl) {
        DownloadXml(RepoName, RepoUrl, getApplicationContext(), new DownloadListener() {
            @Override
            public void onDownloadComplete(boolean success) {
                if (success) {
                    Log.i("App", "Download complete. Starting to parse XML.");
                    LoadAppsData(RepoName, RepoUrl, getApplicationContext());
                } else {
                    // Handle download failure
                    Log.e("App", "Download failed.");
                }
            }
        });
    }

    public void LoadAppsData(String FileName, String RepoUrl, Context context) {
        File file = new File(context.getFilesDir(), FileName + ".xml");
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
            parser.require(XmlPullParser.START_TAG, null, "List");

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                if (parser.getName().equals("App")) {
                    parser.require(XmlPullParser.START_TAG, null, "App");
                    String AppName = "";
                    String PackageName = "";
                    String Version = "";
                    String SystemRequirment = "";

                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }


                        String tagName = parser.getName();
                        if (tagName.equals("Name")) {
                            AppName = ReadTagContent(parser, "Name");
                        } else if (tagName.equals("PackageName")) {
                            PackageName = ReadTagContent(parser, "PackageName");
                        } else if (tagName.equals("Version")) {
                            Version = ReadTagContent(parser, "Version");
                        } else if (tagName.equals("SysVersion")) {
                            SystemRequirment = ReadTagContent(parser, "SysVersion");
                        } else {
                            Skip(parser);
                        }
                    }
                    Apps.put(AppName, new String[]{PackageName, Version, SystemRequirment, FileName});
                    RepoAppList.add(AppName);
                    RepoApps.put(FileName, RepoAppList);
                } else {
                    Skip(parser);
                }
            }
            EditList(FileName, RepoUrl);
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

    public void DownloadXml(String RepoName, String RepoURL, Context context, DownloadListener listener) {
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
                String FileName = RepoName + ".xml";
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

}