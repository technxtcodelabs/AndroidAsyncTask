package net.technxt.androidasynctask;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Objects;

import static java.lang.Double.SIZE;

public class MainActivity extends AppCompatActivity {

    ImageView img;
    Button startDownload, cancelDownload;
    ProgressBar progressBar;
    TextView progressPercent;
    //Create an Instance of AsyncTask Subclass
    MyDownLoadTask myDownLoadTask = null;
    String downloadUrl = "http://technxt.net/img/paris.jpg";
    URL url = null;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);
        progressPercent = findViewById(R.id.txt);
        startDownload = findViewById(R.id.download);
        cancelDownload = findViewById(R.id.cancel);
        myDownLoadTask = new MyDownLoadTask();
        try {
            url = new URL(downloadUrl);
        }catch (MalformedURLException ex){
            ex.printStackTrace();
        }

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            // do you work now
                            Log.d("Permission", "OK");
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permenantly, navigate user to app settings
                            // show alert dialog navigating to Settings
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();

        startDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myDownLoadTask != null) {
                    myDownLoadTask.execute(url);
                }
            }
        });

        cancelDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myDownLoadTask != null) {
                    myDownLoadTask.cancel(true);
                }
            }
        });

    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    public class MyDownLoadTask extends AsyncTask<URL, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Good for toggling visibility of a progress indicator
            progressBar.setVisibility(ProgressBar.VISIBLE);
            progressPercent.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(URL... urls) {
            // Start long-running task of downloading an image in background thread
            String download = downloadImageFromUrl(urls[0]);
            return download;
        }

        private String downloadImageFromUrl(URL url) {
            int lenghtOfFile = 0;
            try {
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                lenghtOfFile = conection.getContentLength();

                File folder = new File(Environment
                        .getExternalStorageDirectory().getAbsolutePath() + "/" +"MyImages");
                if (!folder.exists()){
                    folder.mkdir();
                }
                File imgFile = new File(folder,"myimage.jpg");
                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);
                byte data[] = new byte[1024];
                int total = 0;
                int count = 0;
                OutputStream outputStream = new FileOutputStream(imgFile);
                while ((count = input.read(data)) != -1 && !isCancelled()) {
                    total += count;
                    outputStream.write(data,0,count);
                    // publishing the progress....
                    int  progress = (int)(total*100)/lenghtOfFile;
                    publishProgress(progress);
                    Log.d("Progress:", String.valueOf(progress));
                }
                input.close();
                outputStream.close();
            }catch (IOException ex){
                ex.printStackTrace();
            }

            return "Download Completed !";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressPercent.setText("Download "+values[0]+"/"+progressBar.getMax());
            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String str) {
            super.onPostExecute(str);
            // Hide the progress bar
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            progressPercent.setVisibility(View.INVISIBLE);
            Toast.makeText(getApplicationContext(),str,Toast.LENGTH_LONG).show();
            // Set the result of the long running task to imageview
            String path = Environment
                    .getExternalStorageDirectory().getAbsolutePath() + "/"+"MyImages/myimage.jpg";
            img.setImageDrawable(Drawable.createFromPath(path));

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            progressPercent.setVisibility(View.INVISIBLE);
            Toast.makeText(MainActivity.this, "Download Task Cancelled !", Toast.LENGTH_LONG).show();
        }
    }

}
