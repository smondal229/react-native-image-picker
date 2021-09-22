package com.imagepicker;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.imagepicker.spinnycamera.BaseSpinnyCameraModuleActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;


public class CameraActivity extends BaseSpinnyCameraModuleActivity {
    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final String LOG_TAG = CameraActivity.class.getSimpleName();
    private String photoName;
    private String photoPath;
    private int current_orientation = 0;
    private OrientationEventListener orientationEventListener;
    private Bitmap capturedData = null;
    private Dialog dialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getBundleData();
        Log.d("onCreate", "Suvodip");
        super.onCreate(savedInstanceState);

        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                current_orientation = orientation;
                if(capturedData != null && dialog != null) {
                    dialog.dismiss();
                    showImagePreviewDialog(null);
                }
            }
        };

        orientationEventListener.enable();
    }

    @Override
    protected void onPhotoTaken(byte[] data) {
        showImagePreviewDialog(data);
    }

    // Show photo preview
    private void showImagePreviewDialog(@Nullable final byte[] bitmapData) {
        if (capturedData == null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
            Matrix mat = new Matrix();
            mat.postRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
            capturedData = bitmap;
        }

        // Create bitmap from data
        dialog = new Dialog(this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        // Set dialog view
        dialog.setContentView(R.layout.view_photo_preview);

        ImageView imageView = (ImageView) dialog.findViewById(R.id.imv_photo_preview);
        imageView.setImageBitmap(capturedData);

        LinearLayout buttonContainer = dialog.findViewById(R.id.button_container);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) buttonContainer.getLayoutParams();
        int parent_pos = 0;

        if (current_orientation == 0) {
            parent_pos = RelativeLayout.ALIGN_BOTTOM;
        } else if (current_orientation == 90) {
            parent_pos = RelativeLayout.ALIGN_RIGHT;
        } else if (current_orientation == 180) {
            parent_pos = RelativeLayout.ALIGN_TOP;
        } else if (current_orientation == 270) {
            parent_pos = RelativeLayout.ALIGN_LEFT;
        }

        layoutParams.addRule(parent_pos, R.id.imv_photo_preview);

        if (current_orientation % 180 != 0) {
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            buttonContainer.setRotation(current_orientation + 180);
        } else {
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            buttonContainer.setRotation(current_orientation);
        }

        buttonContainer.setLayoutParams(layoutParams);

        // Ok button listener
        Button btn_ok = (Button) dialog.findViewById(R.id.btn_ok_photo_dialog);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.putExtra("file_name",photoName);
                intent.putExtra("path",photoPath);
                try {
                    saveBitmap(bitmapData);
                    setResult(RESULT_OK,intent);
                } catch (IOException e) {
                    e.printStackTrace();
                    setResult(RESULT_CANCELED,intent);
                }
                dismissDialog();
                finish();
            }
        });
        // Discard button listener
        Button btn_discard = (Button) dialog.findViewById(R.id.btn_discard_photo_dialog);
        btn_discard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissDialog();
            }
        });

        // Show dialog
        dialog.show();
    }

    /**
     * Method to save bitmap data to file.
     *
     * @param data byte array to be saved.
     */
    private void saveBitmap(byte[] data) throws IOException{
        //File f = generatePhotoFile();
            FileOutputStream outputStream = new FileOutputStream(photoPath);
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
    }

    private void dismissDialog() {
        dialog.dismiss();
        dialog = null;
        capturedData = null;
    }
    /**
     * Method to generate file name.
     *
     * @return file name.
     */
    private File generatePhotoFile() {
        File outputDir = getPhotoDirectory();
        File photoFile = null;
        if (outputDir != null) {
            String photoFileName = (photoName==null||photoName.isEmpty())?"IMG" + "_" + System.currentTimeMillis() + ".jpg":photoName;
            photoFile = new File(outputDir, photoFileName);
            photoName=photoFileName;
        }
        return photoFile;
    }

    private File getPhotoDirectory() {
        File outputDir = null;
        if (photoPath!=null&&!photoPath.isEmpty()){
            outputDir=new File(photoPath);
            return outputDir;
        }
        String externalStorageState = Environment.getExternalStorageState();
        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            outputDir = new File(pictureDir, String.format("Spinny/Refurb"));
            if (!outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    Toast.makeText(this, "Failed to create directory :" + outputDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                }
            }
        }
        photoPath=outputDir.getAbsolutePath();
        return outputDir;
    }

    // get data from Activity Intent
    private void getBundleData() {
        Bundle bundle = getIntent().getExtras();
        photoPath=getIntent().getStringExtra("path");
        photoName=getIntent().getStringExtra("file_name");
    }

    @Override
    protected void onPause() {
        orientationEventListener.disable();
        super.onPause();
    }

    @Override
    protected void onResume() {
        orientationEventListener.enable();
        super.onResume();
    }
}

