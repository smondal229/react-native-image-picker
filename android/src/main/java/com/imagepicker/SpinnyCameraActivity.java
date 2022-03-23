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
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.imagepicker.spinnycamera.BaseSpinnyCameraModuleActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Nullable;


public class SpinnyCameraActivity extends BaseSpinnyCameraModuleActivity {
    private static final String TAG = SpinnyCameraActivity.class.getSimpleName();
    private static final String LOG_TAG = SpinnyCameraActivity.class.getSimpleName();
    private String photoName;
    private String photoPath;
    private int current_orientation = 0;
    private int count = 0;
    private OrientationEventListener orientationEventListener;
    private Bitmap capturedData = null;
    private Dialog dialog = null;
    private ArrayList<HashMap<String, String>> carPartList = new ArrayList<>();
    private TextView carPartName;
    private int currentPartIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getBundleData();
        super.onCreate(savedInstanceState);
        carPartName = findViewById(R.id.txv_current_photo_label);

        if (carPartList!=null && carPartList.size() > 0) {
            carPartName.setText(new StringBuilder().append(carPartList.get(currentPartIndex).get("label"))
                    .append(" (captured: ").append(count).append("/").append(carPartList.size()).append(")").toString());
        } else {
            carPartName.setVisibility(View.GONE);
        }
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN)
                    return;
                int diff = Math.abs(orientation - current_orientation);
                if (diff > 180)
                    diff = 360 - diff;
                // only change orientation when sufficiently changed
                if (diff > 60) {
                    orientation = (orientation + 45) / 90 * 90;
                    orientation = orientation % 360;
                    if (orientation != current_orientation) {
                        current_orientation = orientation;
                        if (dialog != null) {
//                            rotateButtons();
                        }
                    }
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
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
        Matrix mat = new Matrix();
        mat.postRotate(90);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);

        // Create bitmap from data
        dialog = new Dialog(this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        // Set dialog view
        dialog.setContentView(R.layout.display_preview);

        ImageView imageView = (ImageView) dialog.findViewById(R.id.imv_photo_preview);
        imageView.setImageBitmap(bitmap);
        rotateButtons();

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
                    count++;
                } catch (IOException e) {
                    e.printStackTrace();
                    setResult(RESULT_CANCELED,intent);
                }
                intent.putExtra("partsCaptured", count);
                dismissDialog();
                if (carPartList!=null && carPartList.size() > count) {
                    currentPartIndex = (currentPartIndex + 1) % carPartList.size();
                    carPartName.setText(new StringBuilder()
                            .append(carPartList.get(currentPartIndex).get("label"))
                            .append(" (captured: ").append(count).append("/")
                            .append(carPartList.size())
                            .append(")")
                            .toString());
                } else {
                    finish();
                }
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

    private void rotateButtons() {
        LinearLayout buttonContainer = dialog.findViewById(R.id.button_container);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) buttonContainer.getLayoutParams();
        int parent_pos = 0;
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        // getRotation is anti-clockwise, but current_orientation is clockwise, so we
        // add rather than subtract
        // relative_orientation is clockwise from landscape-left
        // int relative_orientation = (current_orientation + 360 - degrees) % 360;
        int relative_orientation = (current_orientation + degrees) % 360;
        int ui_rotation = (360 - relative_orientation) % 360;

        if (ui_rotation == 0) {
            parent_pos = RelativeLayout.ALIGN_BOTTOM;
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        } else if (ui_rotation == 90) {
            parent_pos = RelativeLayout.ALIGN_LEFT;
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        } else if (ui_rotation == 180) {
            parent_pos = RelativeLayout.ALIGN_TOP;
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        } else if (ui_rotation == 270) {
            parent_pos = RelativeLayout.ALIGN_RIGHT;
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        }

        buttonContainer.setRotation(ui_rotation);
        layoutParams.addRule(parent_pos, R.id.imv_photo_preview);
        buttonContainer.setLayoutParams(layoutParams);
        buttonContainer.bringToFront();
    }

    /**
     * Method to save bitmap data to file.
     *
     * @param data byte array to be saved.
     */
    private void saveBitmap(byte[] data) throws IOException{
        //File f = generatePhotoFile();
            String currentPath;
            if (carPartList!=null && carPartList.size() > currentPartIndex && carPartList.get(currentPartIndex).containsKey("path")) {
                currentPath = carPartList.get(currentPartIndex).get("path");
            } else {
                currentPath = photoPath;
            }
            FileOutputStream outputStream = new FileOutputStream(currentPath);
            outputStream.write(data);
            System.gc();
            outputStream.flush();
            outputStream.close();
    }

    private void dismissDialog() {
        dialog.dismiss();
        dialog = null;
//        capturedData = null;
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
        currentPartIndex=getIntent().getIntExtra("currentPartIndex", 0);
        carPartList= (ArrayList<HashMap<String, String>>) getIntent().getSerializableExtra("partsList");
    }

    @Override
    protected void onPause() {
        orientationEventListener.disable();
        super.onPause();
    }
}
