package com.example.edgedetection;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
//import java.util.Base64;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
//    private static final int IMAGE_PICK_REQUEST_CODE = 1234;
//    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 4321;
    private static String string = "";
    ImageView forUploadedImage;
    ImageView forResultantImage;
    Button send,upload, save;
    Bitmap mOriginalBitmap, mProcessedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        forUploadedImage = findViewById(R.id.uploadedImage);
        forResultantImage = findViewById(R.id.resultantImage);
        send = findViewById(R.id.send);
        upload = findViewById(R.id.upload);
        save = findViewById(R.id.save);

//        mOriginalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sonia);

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(intent, 100);
            }
        });

//        forUploadedImage.setImageBitmap(mOriginalBitmap);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendImageTask().execute(mOriginalBitmap);
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageToGallery(mProcessedBitmap);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                forUploadedImage.setImageBitmap(bitmap);
                mOriginalBitmap = bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class SendImageTask extends AsyncTask<Bitmap, Void, Bitmap> {

        private static final String URL = "http://10.3.5.96:8080/"; // Replace with your Flask API URL
        private OkHttpClient mHttpClient;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            send.setEnabled(false);
        }

        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            // Encode the image to Base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmaps[0].compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);

            // Create the JSON request body
//            String requestBodyJson = "{\"image\": \"" + encodedImage + "\"}";

            JSONObject requestBodyJson = new JSONObject();
            try {
                requestBodyJson.put("image", encodedImage);
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            String requestBodyJson = "{\"image\": \"" + mOriginalBitmap + "\"}";
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), requestBodyJson.toString());

//            RequestBody requestBody = new MultipartBody.Builder()
//                    .setType(MultipartBody.FORM)
//                    .addFormDataPart("image", "sonia.jpeg",
//                            RequestBody.create(MediaType.parse("image/jpeg"), requestBodyJson))
//                    .build();

//            MediaType mediaType = MediaType.parse("image/jpeg");
//            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
//                    .addFormDataPart("image", "sonia.jpeg",
//                            RequestBody.create(MediaType.parse("application/octet-stream"),
//                                    new File("/root/AndroidStudioProjects/EdgeDetection/app/src/main/res/drawable/sonia.jpeg")))
//                    .build();

            // Create the HTTP client and request

            mHttpClient = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(URL)
                    .post(requestBody)
                    .build();

            // Send the HTTP request and receive the response
            try {

                Response response = mHttpClient.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBodyJson = response.body().string();
                    // Extract the processed image from the response JSON
                    String processedImageString = responseBodyJson.split("\"")[3];
                    // Decode the processed image from Base64
                    byte[] processedImageBytes = Base64.decode(processedImageString, Base64.DEFAULT);

                    mProcessedBitmap = BitmapFactory.decodeByteArray(processedImageBytes, 0, processedImageBytes.length);
                }
            } catch (IOException e) {
                string = e.toString();
                e.printStackTrace();
            }catch (Exception e){
                string = e.toString();
                e.printStackTrace();
            }
            return mProcessedBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            send.setEnabled(true);
            if (bitmap != null) {
                forResultantImage.setImageBitmap(bitmap);
            } else {
                Toast.makeText(MainActivity.this, "Error processing image" + string, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageToGallery(Bitmap bitmap) {
        // Get the directory for storing the image
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!directory.exists()) {
            directory.mkdir();
        }

        // Generate a unique file name for the image
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        // Save the image to the file
        File file = new File(directory, fileName);


        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);


        try{
            OutputStream outputStream = getContentResolver().openOutputStream(imageUri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            // Add the image to the user's gallery
//            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//            mediaScanIntent.setData(Uri.fromFile(file));
//            sendBroadcast(mediaScanIntent);

            // Show a message to the user
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.d("tag1", e.toString());
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}