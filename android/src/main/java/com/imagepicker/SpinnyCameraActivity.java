package com.imagepicker;

import static com.imagepicker.utils.MediaUtils.createNewFile;
import static com.imagepicker.utils.MediaUtils.fileScan;
import static com.imagepicker.utils.MediaUtils.getResizedImage;
import static com.imagepicker.utils.MediaUtils.readExifInterface;
import static com.imagepicker.utils.MediaUtils.removeUselessFiles;
import static com.imagepicker.utils.MediaUtils.rolloutPhotoFromCamera;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.imagepicker.media.ImageConfig;
import com.imagepicker.spinnycamera.BaseSpinnyCameraModuleActivity;
import com.imagepicker.utils.MediaUtils;
import com.imagepicker.utils.RealPathUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

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
    private ArrayList<ImageConfig> imageConfigList = new ArrayList<>();
    private Context moduleContext = null;
    private ReadableMap moduleOptions;
    private int totalPartsCaptured = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getBundleData();
        super.onCreate(savedInstanceState);
        carPartName = findViewById(R.id.txv_current_photo_label);

        if (carPartList!=null && carPartList.size() > 0) {
            carPartName.setText(new StringBuilder().append(carPartList.get(currentPartIndex).get("label")).toString());
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
                onOkPressed(bitmapData, false);
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
        Button btn_add_more = dialog.findViewById(R.id.add_more_photo);
        if (carPartList!=null && carPartList.size() > 0) {
            btn_add_more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onOkPressed(bitmapData, true);
                }
            });
        } else {
            btn_add_more.setVisibility(View.GONE);
        }
        // Show dialog
        dialog.show();
    }

    private void rotateButtons() {
        LinearLayout buttonContainer = dialog.findViewById(R.id.button_container);
        ImageView previewImg = dialog.findViewById(R.id.imv_photo_preview);
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

        int margin = getMeasureInDp(-25);
        int padding = getMeasureInDp(90);
        if (ui_rotation == 0) {
            previewImg.setPadding(0, 0, 0, padding);
            parent_pos = RelativeLayout.ALIGN_BOTTOM;
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 0, getMeasureInDp(-5));
        } else if (ui_rotation == 90) {
            previewImg.setPadding(padding, 0, 0, 0);
            parent_pos = RelativeLayout.ALIGN_LEFT;
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            layoutParams.setMargins(margin, 0, 0, 0);
        } else if (ui_rotation == 180) {
            previewImg.setPadding(0, padding, 0, 0);
            parent_pos = RelativeLayout.ALIGN_TOP;
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 0, getMeasureInDp(-5));
        } else if (ui_rotation == 270) {
            previewImg.setPadding(0, 0, padding, 0);
            parent_pos = RelativeLayout.ALIGN_RIGHT;
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, margin, 0);
        }

        buttonContainer.setRotation(ui_rotation);
        layoutParams.addRule(parent_pos, R.id.imv_photo_preview);
        buttonContainer.setLayoutParams(layoutParams);
        buttonContainer.bringToFront();
    }

    private int getMeasureInDp(int pixel) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                pixel,
                getResources().getDisplayMetrics()
        );
    }

    /**
     * Method to save bitmap data to file.
     *
     * @param data byte array to be saved.
     */
    private void saveBitmap(byte[] data) throws IOException{
        //File f = generatePhotoFile();
        final String currentPath = photoPath;
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

    private void onOkPressed(@Nullable final byte[] bitmapData, final boolean addMore) {
        Intent intent=new Intent();
        intent.putExtra("file_name",photoName);
        intent.putExtra("path",photoPath);

        try {
            saveBitmap(bitmapData);
            setResult(RESULT_OK,intent);
            count++;
            intent.putExtra("partIndex", currentPartIndex);
            intent.putExtra("addMore", addMore);
            postCaptureImage(ImagePickerModule.REQUEST_LAUNCH_IMAGE_CAPTURE, ImagePickerModule.cameraCaptureURI, currentPartIndex);
        } catch (IOException e) {
            e.printStackTrace();
            setResult(RESULT_CANCELED,intent);
        }
        dismissDialog();

        configureNextImage(addMore);
    }

    @Override
    protected void onPause() {
        orientationEventListener.disable();
        super.onPause();
    }

    private void configureNextImage(final boolean addMore) {
        if (carPartList == null || carPartList.size() == 0) {
            finish();
            return;
        }

        final File original = createNewFile(ImagePickerModule.reactContext, ImagePickerModule.options, false);
        ImagePickerModule.imageConfig = ImagePickerModule.imageConfig.withOriginalFile(original);

        if (ImagePickerModule.imageConfig.original != null) {
            ImagePickerModule.cameraCaptureURI = RealPathUtil.compatUriFromFile(ImagePickerModule.reactContext, ImagePickerModule.imageConfig.original);
        } else {
            return;
        }

        photoPath = original.getAbsolutePath();
        if (!addMore) {
            totalPartsCaptured++;
            currentPartIndex = (currentPartIndex+1) % carPartList.size();
            if (totalPartsCaptured == carPartList.size()) finish();
            carPartName.setText(new StringBuilder().append(carPartList.get(currentPartIndex).get("label")).toString());
        }
    }

    private void postCaptureImage(int requestCode, Uri uri, int partIndex) {
        ImagePickerModule.responseHelper.cleanResponse();
        final MediaUtils.ReadExifResult result = readExifInterface(ImagePickerModule.responseHelper, ImagePickerModule.imageConfig);

//        if (result.error != null)
//        {
//            removeUselessFiles(requestCode, imageConfig);
//            responseHelper.invokeError(this.callback, result.error.getMessage());
//            callback = null;
//            return;
//        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ImagePickerModule.imageConfig.original.getAbsolutePath(), options);
        int initialWidth = options.outWidth;
        int initialHeight = options.outHeight;
        updatedResultResponse(uri, ImagePickerModule.imageConfig.original.getAbsolutePath(), partIndex);

        // don't create a new file if contraint are respected
        if (ImagePickerModule.imageConfig.useOriginal(initialWidth, initialHeight, result.currentRotation))
        {
            ImagePickerModule.responseHelper.putInt("width", initialWidth);
            ImagePickerModule.responseHelper.putInt("height", initialHeight);
            fileScan(ImagePickerModule.reactContext, ImagePickerModule.imageConfig.original.getAbsolutePath());
        }
        else
        {
            ImagePickerModule.imageConfig = getResizedImage(ImagePickerModule.reactContext, ImagePickerModule.options, ImagePickerModule.imageConfig, initialWidth, initialHeight, requestCode);
            if (ImagePickerModule.imageConfig.resized == null)
            {
                removeUselessFiles(requestCode, ImagePickerModule.imageConfig);
                ImagePickerModule.responseHelper.putString("error", "Can't resize the image");
            }
            else
            {
                uri = Uri.fromFile(ImagePickerModule.imageConfig.resized);
                BitmapFactory.decodeFile(ImagePickerModule.imageConfig.resized.getAbsolutePath(), options);
                ImagePickerModule.responseHelper.putInt("width", options.outWidth);
                ImagePickerModule.responseHelper.putInt("height", options.outHeight);

                updatedResultResponse(uri, ImagePickerModule.imageConfig.resized.getAbsolutePath(), partIndex);
                fileScan(ImagePickerModule.reactContext, ImagePickerModule.imageConfig.resized.getAbsolutePath());
            }
        }

        if (ImagePickerModule.imageConfig.saveToCameraRoll && requestCode == ImagePickerModule.REQUEST_LAUNCH_IMAGE_CAPTURE)
        {
            final MediaUtils.RolloutPhotoResult rolloutResult = rolloutPhotoFromCamera(ImagePickerModule.imageConfig);

            if (rolloutResult.error == null)
            {
                ImagePickerModule.imageConfig = rolloutResult.imageConfig;
                uri = Uri.fromFile(ImagePickerModule.imageConfig.getActualFile());
                updatedResultResponse(uri, ImagePickerModule.imageConfig.getActualFile().getAbsolutePath(), partIndex);
            }
            else
            {
                removeUselessFiles(requestCode, ImagePickerModule.imageConfig);
                final String errorMessage = new StringBuilder("Error moving image to camera roll: ")
                        .append(rolloutResult.error.getMessage()).toString();
                ImagePickerModule.responseHelper.putString("error", errorMessage);
                return;
            }
        }

        ImagePickerModule.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(ImagePickerModule.EVENT, ImagePickerModule.responseHelper.getResponse());
    }



    private void putExtraFileInfo(@NonNull final String path,
                                  @NonNull final ResponseHelper responseHelper)
    {
        try {
            // size && filename
            File f = new File(path);
            responseHelper.putDouble("fileSize", f.length());
            responseHelper.putString("fileName", f.getName());
            // type
            String extension = MimeTypeMap.getFileExtensionFromUrl(path);
            String fileName = f.getName();
            if (extension != "") {
                responseHelper.putString("type", MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
            } else {
                int i = fileName.lastIndexOf('.');
                if (i > 0) {
                    extension = fileName.substring(i+1);
                    responseHelper.putString("type", MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatedResultResponse(@androidx.annotation.Nullable final Uri uri,
                                       @NonNull final String path,
                                       @androidx.annotation.Nullable final int partIndex)
    {
        ImagePickerModule.responseHelper.putString("uri", uri.toString());
        ImagePickerModule.responseHelper.putString("path", path);
        if (carPartList != null && carPartList.size() > 0) {
            ImagePickerModule.responseHelper.putString("label", (String) Objects.requireNonNull(carPartList.get(partIndex).get("label")));
            ImagePickerModule.responseHelper.putString("value", (String) Objects.requireNonNull(carPartList.get(partIndex).get("value")));
        }

        putExtraFileInfo(path, ImagePickerModule.responseHelper);
    }
}
