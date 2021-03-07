package com.imagepicker.spinnycamera;

public interface SpinnyCameraPhoto {

    void savePhoto(String uri);

    void deletePhoto();

    String getPhotoUri();

    String getPhotoLabel();
}
