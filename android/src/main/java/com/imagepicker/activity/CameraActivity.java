package com.imagepicker.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        cameraView=findViewById(R.id.camera);

        cameraView.setLifecycleOwner(this);


        final String imageSavePath=getIntent().getStringExtra("path");

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                super.onPictureTaken(result);
                Log.d("DATA", "onPictureTaken: "+ (result.getData().length));

                // Assume block needs to be inside a Try/Catch block.
                String path = Environment.getExternalStorageDirectory().toString();
                Integer counter = 0;
                File file = new File(imageSavePath); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
                try {
                    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
                    outputStream.write(result.getData());
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent=new Intent();
                intent.putExtra("location",path+"Test"+counter+".jpg");
                setResult(RESULT_OK,intent);
                finish();
                return;
            }
        });

        findViewById(R.id.takePicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.takePicture();
            }
        });
    }
}