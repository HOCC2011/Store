package com.hocc.fun.store;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    public HashMap<String, String[]> Apps = new HashMap<>();
    public HashMap<Double, String> AppIndex = new HashMap<>();
    TextView Text;
    StringBuilder Data = new StringBuilder("");

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
        Text = findViewById(R.id.Text);
        LoadRepo("HOCC", "https://raw.githubusercontent.com/HOCC2011/HOCC-Store-Repo/main/index.xml");
        LoadRepo("Test", "https://raw.githubusercontent.com/HOCC2011/Store-Test-Repo/main/index.xml");
        Log.d("Android Version", String.valueOf(Build.VERSION.SDK_INT));
    }

    public void EditList() {
        Data.setLength(0);
        int AppNumber = Apps.size();
        Log.d("App Number", String.valueOf(AppNumber));
        for (int i = 1; i <= AppNumber; i++) {
            String AppName = AppIndex.get(Double.valueOf(i));
            String PackageName = Apps.get(AppName)[0];
            String Version = Apps.get(AppName)[1];
            String SystemRequirment = Apps.get(AppName)[2];
            String RepoName = Apps.get(AppName)[3];
            if (Build.VERSION.SDK_INT > Integer.valueOf(SystemRequirment)){
                if (Data.length() > 0) {
                    Data.append("\n\n");
                }
                Data.append("Name: " + AppName + "\nPackage Name: " + PackageName + "\nVersion: " + Version + "\nSystem Requirment: " + SystemRequirment +"\nProvider: " + RepoName);
            }
        }
        Text.setText(Data);
    }

    public void LoadRepo(String RepoName, String RepoUrl) {
        DownloadXml(RepoName, RepoUrl, getApplicationContext(), new DownloadListener() {
            @Override
            public void onDownloadComplete(boolean success) {
                if (success) {
                    Log.i("App", "Download complete. Starting to parse XML.");
                    LoadAppsData(RepoName, getApplicationContext());
                } else {
                    // Handle download failure
                    Log.e("App", "Download failed.");
                }
            }
        });
    }

    public void LoadAppsData(String FileName, Context context) {
        File file = new File(context.getFilesDir(), FileName + ".xml");

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

                    Double IndexNumber = null;
                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }

                        if (Apps != null) {
                            IndexNumber = Double.valueOf(Apps.size() + 1);
                        } else {
                            IndexNumber = Double.valueOf(1);
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
                    AppIndex.put(IndexNumber, AppName);
                } else {
                    Skip(parser);
                }
            }
            EditList();
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