package cn.qiditu.guet.android.project8.camera;

import android.Manifest;
import android.graphics.Bitmap;
import android.support.annotation.RequiresPermission;

import cn.qiditu.signalslot.signals.Signal1;

public abstract class AbstractCamera {

    @SuppressWarnings("WeakerAccess")
    protected AbstractCamera() {
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public abstract void openCamera(int rotation);

    public abstract void closeCamera();

    public abstract void takePicture(int rotation);

    public final Signal1<String> error = new Signal1<>(this);
    public final Signal1<Bitmap> takePictureFinished = new Signal1<>(this);

}