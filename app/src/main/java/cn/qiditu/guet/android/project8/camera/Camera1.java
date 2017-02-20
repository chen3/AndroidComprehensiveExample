/*
 * 参考   http://wiki.jikexueyuan.com/project/android-actual-combat-skills/photos-and-videos-with-camera.html
 */
package cn.qiditu.guet.android.project8.camera;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import cn.qiditu.guet.android.project8.Application;
import cn.qiditu.guet.android.project8.R;

@SuppressWarnings("deprecation")
public class Camera1 extends AbstractCamera {

    private SurfaceView surfaceView;
    private boolean isSurfaceCreated = false;
    private int rotation = 0;

    private enum State {
        PREVIEW, CAPTURE, WAIT_SURFACE_CREATED, NONE
    }
    private State state = State.NONE;

    public Camera1(@NonNull final SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceHolderCallback);

        PackageManager packageManager = Application.getGlobalApplicationContext()
                                                    .getPackageManager();
        hasAutoFocusFeature = packageManager.hasSystemFeature(
                                            PackageManager.FEATURE_CAMERA_AUTOFOCUS);
    }

    private Camera camera;
    private static final int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;   //后置摄像头

    @Override
    public void openCamera(int rotation) {
        if(!isSurfaceCreated) {
            state = State.WAIT_SURFACE_CREATED;
            this.rotation = rotation;
            return;
        }
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        try {
            camera.setPreviewDisplay(surfaceView.getHolder());
            camera.setDisplayOrientation(getDisplayOrientation(rotation));
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeCamera() {
        if(camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private boolean hasAutoFocusFeature = false;

    @Override
    public void takePicture(int rotation) {
        if(hasAutoFocusFeature) {
            handler.postDelayed(autoFocusTimeOutRunnable, autoFocusTimeOut);
            camera.autoFocus(cameraAutoFocusCallback);
        }
        else {
            takePacture();
        }
    }

    private Handler handler = new Handler();
    @SuppressWarnings("FieldCanBeLocal")
    private int autoFocusTimeOut = 3 * 1000;    //3s
    private Runnable autoFocusTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            takePacture();
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            isSurfaceCreated = true;
            switch (state) {
                case WAIT_SURFACE_CREATED: {
                    state = State.PREVIEW;
                    try {
                        openCamera(rotation);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                } break;
                case CAPTURE: {
                    try {
                        camera.setPreviewDisplay(surfaceView.getHolder());
                        camera.startPreview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } break;
            }
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width, int height) {
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    private Camera.AutoFocusCallback cameraAutoFocusCallback =
            new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            handler.removeCallbacks(autoFocusTimeOutRunnable);
            if(success) {
                takePacture();
            }
            else {
                error.emit(Application.getGlobalApplicationContext()
                                        .getString(R.string.auto_focus_fail));
            }
        }
    };

    private void takePacture() {
        state = State.CAPTURE;
        camera.takePicture(shutterCallback, null, cameraPictureCallback);
    }

    private Camera.PictureCallback cameraPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            //修正旋转90度问题
            Matrix matrix = new Matrix();
            matrix.setRotate(90, (float)bitmap.getWidth() / 2,
                                (float)bitmap.getHeight() / 2);
            final Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0,
                                                bitmap.getWidth(), bitmap.getHeight(),
                                                matrix, true);
//            try {
//                camera.startPreview();
//                camera.reconnect();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            takePictureFinished.emit(bitmap1);
        }
    };

    private int getDisplayOrientation(int rotation) {
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: {
                degrees = 0;
            } break;
            case Surface.ROTATION_90: {
                degrees = 90;
            } break;
            case Surface.ROTATION_180: {
                degrees = 180;
            } break;
            case Surface.ROTATION_270: {
                degrees = 270;
            }  break;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  //  compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onShutter() {
        }
    };
}
