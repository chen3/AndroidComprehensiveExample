/*
 * 参考 https://github.com/googlesamples/android-Camera2Basic
 *      http://www.jianshu.com/p/7f766eb2f4e7
 */
package cn.qiditu.guet.android.project8.camera;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import cn.qiditu.guet.android.project8.Application;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2 extends AbstractCamera {

    private TextureView view;

    public Camera2(TextureView view) {
        super();
        this.view = view;
    }

    //后摄像头ID
    private String cameraID = String.valueOf(CameraCharacteristics.LENS_FACING_FRONT);
    private CameraManager cameraManager = (CameraManager) Application
                                            .getGlobalApplicationContext()
                                            .getSystemService(Context.CAMERA_SERVICE);

    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private ImageReader imageReader;

    @RequiresPermission(Manifest.permission.CAMERA)
    @Override
    public void openCamera(int rotation) {
        if(view.isAvailable()) {
            openCamera2();
        }
        else {
            view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                                      int width, int height) {
                    openCamera2();
                }
                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                        int width, int height) {
                }
                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }
                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                }
            });
        }
    }

    private void openCamera2() {
        startBackgroundThreadHandle();
        try {
            Size largest = getCameraSize();
            imageReader = ImageReader.newInstance(largest.getWidth(),
                                                largest.getHeight(),
                                                ImageFormat.JPEG,
                                                /*maxImages*/2);
            imageReader.setOnImageAvailableListener(imageAvailable, handler);
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            cameraManager.openCamera(cameraID, cameraDeviceStateCallback, handler);
        } catch (CameraAccessException |
                InterruptedException |
                SecurityException e) {
            throw new RuntimeException("Camera Open Error", e);
        }
    }

    private ImageReader.OnImageAvailableListener imageAvailable
                            = new ImageReader.OnImageAvailableListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            Bitmap bitmap = convertToBitmap(image);
            image.close();

            if(bitmap.getHeight() < bitmap.getWidth()) {
                //旋转90度
                Matrix matrix = new Matrix();
                matrix.setRotate(90, (float)bitmap.getWidth() / 2,
                                     (float)bitmap.getHeight() / 2);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(),
                        matrix, true);
            }

            takePictureFinished.emit(bitmap);
        }
    };

    @Override
    public void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != cameraCaptureSession) {
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
            stopBackgroundThreadHandle();
        }
    }

    @Nullable
    @SuppressWarnings("WeakerAccess")
    public static Bitmap convertToBitmap(@NonNull Image image) {
        final Image.Plane[] planes = image.getPlanes();
        if(planes.length == 0) {
            return null;
        }
        final ByteBuffer buffer = planes[0].getBuffer();

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    ///为了使照片竖直显示
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private enum State {
        PREVIEW, WAITING_CAPTURE
    }
    private State state = State.PREVIEW;

    /**
     *
     * @param rotation getWindowManager().getDefaultDisplay().getRotation();    手机方向
     */
    @Override
    public void takePicture(int rotation) {
        if(cameraDevice == null) {
            return;
        }
        Log.i("Rotation", String.valueOf(rotation));
        try {
            final CaptureRequest.Builder captureRequestBuilder
                        = cameraDevice.createCaptureRequest(
                                        CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(imageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                                    ORIENTATIONS.get(rotation));
            //拍照
            state = State.WAITING_CAPTURE;
            cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback captureCallback =
                    new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            if (state != State.WAITING_CAPTURE) {
                return;
            }
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
            if(afState == null) {
                return;
            }

            if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                    || CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState
                    || CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED == afState) {
                state = State.PREVIEW;
            }
        }
        private MediaPlayer shutter;
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                    @NonNull CaptureRequest request,
                                    long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            try {
                if (shutter == null) {
                    Context context = Application.getGlobalApplicationContext();
                    Uri uri = Uri.parse("file:///system/media/audio/ui/camera_click.ogg");
                    shutter = MediaPlayer.create(context, uri);
                }
                if (shutter != null) {
                    shutter.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private HandlerThread handlerThread;
    private Handler handler;
    private void startBackgroundThreadHandle() {
        handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }
    private void stopBackgroundThreadHandle() {
        if(handler != null) {
            handler = null;
        }
        if(handlerThread != null) {
            handlerThread.quit();
            handlerThread = null;
        }
    }

    private Size getCameraSize() throws CameraAccessException {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraID);
        StreamConfigurationMap map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        assert map != null;
        return Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                sizeComparator);
    }

    private final static Comparator<Size> sizeComparator = new Comparator<Size>() {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    };

    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {            //打开摄像头
            cameraOpenCloseLock.release();
            cameraDevice = camera;
            startPreview();         //开启预览
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {      //关闭摄像头
            cameraOpenCloseLock.release();
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {  //发生错误
            cameraOpenCloseLock.release();
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            Camera2.this.error.emit("Camera StateCallback Error, Error Code:" + error);
            Log.e("Camera StateCallback", "Error Code:" + error);
        }
    };

    private CaptureRequest.Builder previewRequestBuilder;
    private void startPreview() {
        try {
            SurfaceTexture texture = view.getSurfaceTexture();
            Surface surface = new Surface(texture);
            // 创建预览需要的CaptureRequest.Builder
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    cameraCaptureSessionStateCallback,
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession cameraCaptureSession;
    private CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback =
            new CameraCaptureSession.StateCallback() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if (null == cameraDevice) {
                Log.e("onConfigured", "Not CameraDevice");
            }
            // 当摄像头已经准备好时，开始显示预览
            cameraCaptureSession = session;
            try {
                // 自动对焦
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 打开闪光灯
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                // 显示预览
                session.setRepeatingRequest(previewRequestBuilder.build(), null, handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e("onConfigureFailed", "unKnow");
        }
    };

}