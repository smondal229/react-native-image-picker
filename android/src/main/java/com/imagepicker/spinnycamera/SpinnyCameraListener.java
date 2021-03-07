package com.imagepicker.spinnycamera;

/**
 * Spinny camera listener.
 */
public interface SpinnyCameraListener {
    /**
     * Method to be called when photo needs to be saved.
     */
    void onPhotoConfirm();

    /**
     * Method to be called when photo needs to be discarded.
     */
    void onPhotoDiscard();

    /**
     * Method to be called when photo needs to be deleted.
     */
    void onPhotoDelete();
}
