package com.custom.camlib.spinnycamera.CameraController;

public abstract class CameraControllerManager {
	public abstract int getNumberOfCameras();
	public abstract boolean isFrontFacing(int cameraId);
}
