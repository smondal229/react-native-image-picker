package com.imagepicker.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import com.imagepicker.R;

public class CameraActivity extends AppCompatActivity {

    CameraView cameraView;

    ProgressDialog progressDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        progressDialog=new ProgressDialog(CameraActivity.this);

        cameraView=findViewById(R.id.camera);

        cameraView.setLifecycleOwner(this);



        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                super.onPictureTaken(result);

                final String imageSavePath=getIntent().getStringExtra("path");

                Log.d("DATA", "onPictureTaken here: "+ (result.getData().length));

                progressDialog.dismiss();
                // the File to save , append increasing numeric counter to prevent files from getting overwritten.
                Intent intent=new Intent();
                intent.putExtra("path",imageSavePath);
                try {
                    FileOutputStream outputStream = new FileOutputStream(imageSavePath);
                    outputStream.write(result.getData());
                    outputStream.flush();
                    outputStream.close();
                    setResult(RESULT_OK,intent);
                } catch (IOException e) {
                    e.printStackTrace();
                    setResult(RESULT_CANCELED,intent);
                }
                finish();
                return;
            }

            @Override
            public void onCameraError(@NonNull CameraException exception) {
                super.onCameraError(exception);
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        findViewById(R.id.takePicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.setMessage("Processing");
                progressDialog.show();
                cameraView.takePictureSnapshot();
            }
        });
    }
}
