package com.hocc.fun.store;

import static com.hocc.fun.store.VersionCheckWorker.KEY_REPO_NAME;
import static com.hocc.fun.store.VersionCheckWorker.KEY_REPO_URL;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import org.xmlpull.v1.XmlPullParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public HashMap<String, String[]> Apps = new HashMap<>();
    public HashMap<String, ArrayList> RepoApps = new HashMap<>();
    List<AppItem> Applist = new ArrayList<>();
    RecyclerView recyclerView;
    ImageView reload;
    ImageView repo;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 101;

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

        checkAndRequestNotificationPermission();
        scheduleDailyVersionCheck("HOCC", "https://raw.githubusercontent.com/HOCC2011/HOCC-Store-Repo/main");
        scheduleDailyVersionCheck("Test", "https://raw.githubusercontent.com/HOCC2011/Store-Test-Repo/main");

        repo = findViewById(R.id.repo);
        repo.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, RepoEdit.class);
            startActivity(intent);
        });

        reload = findViewById(R.id.reload);
        reload.setOnClickListener(view -> {
            Toast.makeText(this, "Loading data...", Toast.LENGTH_LONG).show();
            Applist.clear();
            LoadRepo("HOCC", "https://raw.githubusercontent.com/HOCC2011/HOCC-Store-Repo/main");
            LoadRepo("Test", "https://raw.githubusercontent.com/HOCC2011/Store-Test-Repo/main");
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Applist.clear();
        LoadRepo("HOCC", "https://raw.githubusercontent.com/HOCC2011/HOCC-Store-Repo/main");
        LoadRepo("Test", "https://raw.githubusercontent.com/HOCC2011/Store-Test-Repo/main");
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
            String Developer = Apps.get(AppName)[3];
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
                String IconUrl = RepoUrl + "/" + AppName + "/" + Version + ".png";
                String AppUrl = RepoUrl + "/" + AppName + "/" + Version + ".apk";
                Applist.add(new AppItem(IconUrl, AppName, Version, Developer + " (" + RepoName + ")", ButtonText, AppUrl));
            }
        }
        AppListAdapter adapter = new AppListAdapter(Applist);
        recyclerView.setAdapter(adapter);
    }

    public void LoadRepo(String RepoName, String RepoUrl) {
        DownloadXml(RepoName, RepoUrl + "/index.xml", getApplicationContext(), new DownloadListener() {
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
                    String Developer = "";

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
                        } else if (tagName.equals("Developer")) {
                            Developer = ReadTagContent(parser, "Developer");
                        } else {
                            Skip(parser);
                        }
                    }
                    Apps.put(AppName, new String[]{PackageName, Version, SystemRequirment, FileName, Developer});
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

    private void scheduleDailyVersionCheck(String repoName, String repoUrl) {

        // 1. Build the Input Data
        Data inputData = new Data.Builder()
                .putString(KEY_REPO_NAME, repoName)
                .putString(KEY_REPO_URL, repoUrl + "/index.xml")
                .build();

        // 2. Calculate the time until the next 2:00 AM
        Calendar now = Calendar.getInstance();
        Calendar twoAm = Calendar.getInstance();

        // Set the target time to 2:00:00 AM
        twoAm.set(Calendar.HOUR_OF_DAY, 2);
        twoAm.set(Calendar.MINUTE, 0);
        twoAm.set(Calendar.SECOND, 0);

        // If the current time is past 2 AM, schedule it for 2 AM the next day
        if (now.after(twoAm)) {
            twoAm.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Calculate the difference (initial delay)
        long initialDelay = twoAm.getTimeInMillis() - now.getTimeInMillis();
        long initialDelayMinutes = TimeUnit.MILLISECONDS.toMinutes(initialDelay);

        Log.d("DailyVersionCheck", "Daily check scheduled. First run in: " + initialDelayMinutes + " minutes.");

        // 3. Create the Periodic Work Request
        // WorkManager requires a minimum repeat interval of 15 minutes, we use 24 hours.
        PeriodicWorkRequest versionCheckRequest =
                new PeriodicWorkRequest.Builder(VersionCheckWorker.class,
                        24, TimeUnit.HOURS)
                        .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                        .setInputData(inputData) // Pass the name/URL to the worker
                        .addTag("DailyVersionCheck")
                        // Optional: require network connection for the download to succeed
                        // .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build();

        // 4. Enqueue the work
        // KEEP: ensures that if the app is killed and restarted, the single daily task is maintained.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyVersionCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                versionCheckRequest);
    }

    public void checkAndRequestNotificationPermission() {
        // 1. Check if the device is running Android 13 (TIRAMISU) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            // 2. Check if the POST_NOTIFICATIONS permission has been granted
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {

                // Permission is already granted. Proceed with sending notifications.
                // Example: sendNotification();

            } else {

                // Permission is NOT granted. Request it from the user.
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS
                );
            }

        }
    }
}