package com.hocc.fun.store;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class VersionCheckWorker extends Worker {
    private static final String CHANNEL_ID = "version_channel";
    public static final String KEY_REPO_NAME = "RepoName";
    public static final String KEY_REPO_URL = "RepoUrl";
    private final String repoName;
    private final String repoUrl;
    private final Context context;
    private final PackageManager pm;

    public VersionCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.pm = context.getPackageManager();
        Data inputData = workerParams.getInputData();
        this.repoName = inputData.getString(KEY_REPO_NAME);
        this.repoUrl = inputData.getString(KEY_REPO_URL);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (repoName == null || repoUrl == null) {
            Log.e("DailyVersionCheck", "RepoName or RepoUrl is null. Aborting check.");
            return Result.failure();
        }

        boolean downloadSuccess = downloadXmlBlocking(repoName, repoUrl, context);

        if (!downloadSuccess) {
            Log.e("DailyVersionCheck", "Failed to download latest XML data. Aborting check.");
            return Result.retry();
        }

        List<String> foundUpdates = checkLocalVersions();

        if (!foundUpdates.isEmpty()) {
            pushNotification("Update(s) Available", "New version avaliable for app(s) from repository " + repoName + ".");
        } else {
            Log.d("DailyVersionCheck", "No updates found for " + repoName + ".");
        }

        return Result.success();
    }

    private List<String> checkLocalVersions() {
        List<String> updates = new ArrayList<>();
        File file = new File(context.getFilesDir(), repoName + ".xml");

        if (!file.exists()) {
            Log.e("DailyVersionCheck", "XML File not found: " + file.getAbsolutePath() + ". Cannot perform check.");
            return updates;
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);
            parser.nextTag(); // <List>

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) continue;

                if (parser.getName().equals("App")) {
                    String appName = null;
                    String packageName = null;
                    String remoteVersion = null;
                    String systemRequirement = null;

                    // Parse App tags
                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) continue;

                        String tagName = parser.getName();
                        if (tagName.equals("Name")) appName = ReadTagContent(parser, "Name");
                        else if (tagName.equals("PackageName")) packageName = ReadTagContent(parser, "PackageName");
                        else if (tagName.equals("Version")) remoteVersion = ReadTagContent(parser, "Version");
                        else if (tagName.equals("SysVersion")) systemRequirement = ReadTagContent(parser, "SysVersion");
                        else Skip(parser);
                    }

                    // Check if update is needed
                    if (appName != null && packageName != null && remoteVersion != null && systemRequirement != null) {
                        if (isUpdateNeeded(packageName.replaceAll("\\s+", ""), remoteVersion.replaceAll("\\s+", ""), systemRequirement)) {
                            updates.add(appName);
                        }
                    }
                } else {
                    Skip(parser);
                }
            }
        } catch (Exception e) {
            Log.e("DailyVersionCheck", "Error during XML parsing or version check.", e);
        }
        return updates;
    }

    private boolean isUpdateNeeded(String packageName, String remoteVersion, String systemRequirement) {
        try {
            // 1. Check System Requirement
            if (Build.VERSION.SDK_INT <= Integer.valueOf(systemRequirement)) {
                return false; // Skip if system requirement is not met
            }

            // 2. Check Installed Status
            android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);

            // 3. Compare Versions
            if (!packageInfo.versionName.replaceAll("\\s+", "").equals(remoteVersion)) {
                return true; // Installed, but a new version is available (Update)
            }
        } catch (PackageManager.NameNotFoundException e) {
            // App is not installed, but meets system requirements (Download)
            return true;
        } catch (Exception e) {
            Log.e("DailyVersionCheck", "Error during version comparison.", e);
        }
        return false; // App is installed and up-to-date, or error occurred
    }

    private String ReadTagContent(XmlPullParser parser, String tagName) throws Exception {
        parser.require(XmlPullParser.START_TAG, null, tagName);
        String text = "";
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
        }
        while (parser.getEventType() != XmlPullParser.END_TAG) {
            parser.next();
        }
        parser.require(XmlPullParser.END_TAG, null, tagName);
        return text;
    }
    private void Skip(XmlPullParser parser) throws Exception {
        if (parser.getEventType() != XmlPullParser.START_TAG) throw new IllegalStateException();
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

    // --- Notification Logic ---
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private void pushNotification(String title, String message) {
        // ... (Implement your createNotificationChannel and notification code here)
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1002, builder.build());
    }

    private void createNotificationChannel() {
        // Check if the device is running Android Oreo or later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Define the channel's name and description (user-facing)
            CharSequence name = "App Update Notifications";
            String description = "New version avaliable for some app(s).";

            // Define the importance level (HIGH means sound and head-up notification)
            int importance = NotificationManager.IMPORTANCE_HIGH;

            // Create the channel object
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Get the system's NotificationManager
            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);

            // Create the channel in the system
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    private boolean downloadXmlBlocking(String repoName, String repoUrl, Context context) {
        boolean success = false;
        HttpsURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        final String WORK_TAG = "DailyVersionCheck"; // Use the local constant

        try {
            // 1. Setup Connection
            URL url = new URL(repoUrl);
            connection = (HttpsURLConnection) url.openConnection();
            connection.connect();

            // Check if the connection was successful
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                Log.e(WORK_TAG, "Server returned non-OK HTTP code: " + connection.getResponseCode());
                return false;
            }

            // 2. Get Input Stream and Setup Output File
            inputStream = connection.getInputStream();
            String fileName = repoName + ".xml";
            File file = new File(context.getFilesDir(), fileName);
            outputStream = new FileOutputStream(file);

            // 3. Write data to file
            byte[] buffer = new byte[1024];
            int bufferLength;

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bufferLength);
            }

            // Flush and confirm success
            outputStream.flush();
            Log.d(WORK_TAG, "XML file saved successfully to: " + file.getAbsolutePath());
            success = true;

        } catch (IOException e) {
            // Handles network errors, URL formatting errors, and file I/O errors
            Log.e(WORK_TAG, "Error downloading or saving XML file.", e);
            success = false;
        } finally {
            // 4. Clean up resources
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.e(WORK_TAG, "Error closing output stream.", e);
            }
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                Log.e(WORK_TAG, "Error closing input stream.", e);
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return success;
    }
}