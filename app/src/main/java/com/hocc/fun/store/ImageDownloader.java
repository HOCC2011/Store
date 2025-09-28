package com.hocc.fun.store;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageDownloader {
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void downloadAndSetImage(String imageUrl, ImageView imageView) {
        // Run network operation on a background thread
        executorService.execute(() -> {
            Bitmap bitmap = null;
            try {
                // 1. Download the image data from the URL
                InputStream inputStream = new URL(imageUrl).openStream();

                // 2. Decode the stream into a Bitmap object
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
                // Handle the error (e.g., set an error placeholder)
            }

            final Bitmap finalBitmap = bitmap;

            // Update the UI on the main thread
            mainHandler.post(() -> {
                if (finalBitmap != null) {
                    imageView.setImageBitmap(finalBitmap);
                } else {
                    imageView.setImageResource(R.drawable.error_icon);
                }
            });
        });
    }
}
