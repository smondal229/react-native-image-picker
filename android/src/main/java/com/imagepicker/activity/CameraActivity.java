package com.imagepicker.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;


import com.imagepicker.R;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;



public class CameraActivity extends AppCompatActivity {

    CameraView cameraView;

    ProgressDialog progressDialog;

    String imageSavePath



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        progressDialog=new ProgressDialog(CameraActivity.this);

        cameraView=findViewById(R.id.camera);

        cameraView.setLifecycleOwner(this);

        imageSavePath=getIntent().getStringExtra("path");

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                super.onPictureTaken(result);


                progressDialog.dismiss();
                if (result.getData().length>0){
                    //image captured
                    showPreview(result.getData());
                }else{
                    //0 byte received
                    setResult(RESULT_CANCELED);
                    finish();
                }
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


    private void showPreview(byte[] data){

        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

        final Dialog dialog=new Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.camera_image_preview_layout);

        AppCompatImageView imgOk=dialog.findViewById(R.id.img_ok);
        AppCompatImageView imgCancel=dialog.findViewById(R.id.img_cancel);
        AppCompatImageView imgPreview=dialog.findViewById(R.id.img_preview);

        imgPreview.setImageBitmap(bmp);

        imgCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        imgOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //save the image and set result
                // the File to save , append increasing numeric counter to prevent files from getting overwritten.
                Intent intent=new Intent();
                intent.putExtra("path",imageSavePath);
                try {
                    FileOutputStream outputStream = new FileOutputStream(imageSavePath);
                    outputStream.write(data);
                    outputStream.flush();
                    outputStream.close();
                    setResult(RESULT_OK,intent);
                } catch (IOException e) {
                    e.printStackTrace();
                    setResult(RESULT_CANCELED,intent);
                }
                finish();
            }
        });

        dialog.show();
    }
}
