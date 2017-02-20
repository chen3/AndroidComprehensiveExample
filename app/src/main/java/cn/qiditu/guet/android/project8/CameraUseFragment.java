package cn.qiditu.guet.android.project8;

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import cn.qiditu.guet.android.project8.camera.AbstractCamera;
import cn.qiditu.guet.android.project8.camera.Camera1;
import cn.qiditu.guet.android.project8.camera.Camera2;
import cn.qiditu.signalslot.slots.Slot1;

public class CameraUseFragment extends Fragment {

    @BindView(R.id.layout_root)
    ViewGroup rootLayout;
    @BindView(R.id.image_view)
    ImageView imageView;
    @BindView(R.id.card_view)
    CardView cardView;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private View previewView;
    private Unbinder unbinder;
    private AbstractCamera camera;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_use, container, false);
        unbinder = ButterKnife.bind(this, view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            previewView = view.findViewById(R.id.camera_preview);
            camera = new Camera2((TextureView)previewView);
        } else {
              previewView = view.findViewById(R.id.camera_preview);
              camera = new Camera1((SurfaceView)previewView);
        }

        camera.error.connect(cameraError);
        camera.takePictureFinished.connect(takePictureFinished);
        if (PermissionHelper.checkAndRequestPermission(this,
                Manifest.permission.CAMERA,
                R.string.use_camera_need_permission,
                requestCodeUseCamera)) {
            camera.openCamera(this.getActivity().getWindowManager()
                                        .getDefaultDisplay().getRotation());
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        camera.closeCamera();
        camera.error.disconnect(cameraError);
        camera.takePictureFinished.disconnect(takePictureFinished);
        unbinder.unbind();
        super.onDestroyView();
    }

    private Slot1<String> cameraError = new Slot1<String>() {
        @Override
        public void accept(@Nullable String s) {
            if(s != null) {
                Snackbar.make(rootLayout, s, Snackbar.LENGTH_LONG).show();
            }
        }
    };

    @SuppressWarnings("unused")
    @OnClick(R.id.camera_preview)
    void takePicture() {
        int rotation = this.getActivity().getWindowManager()
                            .getDefaultDisplay().getRotation();
        camera.takePicture(rotation);
    }

    @SuppressWarnings("UnusedDeclaration")
    private Bitmap bitmap;

    public boolean handleBackPressed() {
        if(cardView.getVisibility() != View.INVISIBLE) {
            cardView.setVisibility(View.INVISIBLE);
            previewView.setVisibility(View.VISIBLE);
            return true;
        }
        else {
            return false;
        }
    }

    private Slot1<Bitmap> takePictureFinished = new Slot1<Bitmap>() {
        @Override
        public void accept(@Nullable final Bitmap bitmap) {
            CameraUseFragment.this.bitmap = bitmap;
            if(bitmap != null) {
                final Bitmap bitmap1 = scaleBitmap(bitmap);
                final RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams)cardView.getLayoutParams();
                layoutParams.width = bitmap1.getWidth();
                layoutParams.height = bitmap1.getHeight();

                CameraUseFragment.this.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cardView.setLayoutParams(layoutParams);
                        imageView.setImageBitmap(bitmap1);
                        cardView.setVisibility(View.VISIBLE);
                        previewView.setVisibility(View.GONE);
                    }
                });
            }
        }
    };

    private Bitmap scaleBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        float scale = Math.min(((float)cardView.getHeight()) / bitmap.getHeight(),
                               ((float)cardView.getWidth()) / bitmap.getWidth());
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                                matrix, true);
    }

    private static final int requestCodeUseCamera = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case requestCodeUseCamera: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        camera.openCamera(this.getActivity().getWindowManager()
                                                .getDefaultDisplay().getRotation());
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                } else {
                    PermissionHelper.checkAndGoToSetting(this,
                            Manifest.permission.CAMERA,
                            R.string.use_camera_permission_disable_manual_open_permissions);
                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            break;
        }
    }

}
