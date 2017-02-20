package cn.qiditu.guet.android.project8;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class CameraOpenFragment extends Fragment {

    private Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_open, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }

    private static final int requestCodeOpenCameraAppResult = 1;
    private static final int requestCodeOpenCameraApp = 2;

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_open_camera_app)
    void openCameraApp() {
        if(PermissionHelper.checkAndRequestPermission(this,
                Manifest.permission.CAMERA,
                R.string.open_camera_app_need_permission,
                requestCodeOpenCameraApp)) {
            openCameraApp2();
        }
    }

    private void openCameraApp2() {
        this.startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                requestCodeOpenCameraAppResult);
    }

    @BindView(R.id.layout_root)
    ViewGroup rootLayout;
    @BindString(R.string.result_error_with_code)
    String resultErrorWithCode;
    @BindView(R.id.imageView)
    ImageView imageView;
    @BindView(R.id.card_view)
    CardView cardView;

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private Bitmap bitmap;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case requestCodeOpenCameraAppResult: {
                if(resultCode == Activity.RESULT_OK) {
                    resultBitmap((Bitmap)data.getExtras().get("data"));
                }
                else if(resultCode != Activity.RESULT_CANCELED){
                    Snackbar.make(rootLayout,
                                String.format(resultErrorWithCode, resultCode),
                                Snackbar.LENGTH_LONG)
                            .show();
                }
            } break;
            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @BindString(R.string.open_camera_app_permission_disable_manual_open_permissions)
    String openSystemCameraPermissionDisable;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case requestCodeOpenCameraApp: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCameraApp2();
                }
                else {
                    PermissionHelper.checkAndGoToSetting(this,
                            Manifest.permission.CAMERA,
                            openSystemCameraPermissionDisable);
                }
            } break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void resultBitmap(final Bitmap bitmap) {
        //修正旋转90度问题
        Matrix matrix = new Matrix();
        matrix.setRotate(90, (float)bitmap.getWidth() / 2,
                (float)bitmap.getHeight() / 2);
        final Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
        this.bitmap = bitmap1;

        final Bitmap bitmap2 = scaleBitmap(bitmap1);
        final RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams)cardView.getLayoutParams();
        layoutParams.width = bitmap2.getWidth();
        layoutParams.height = bitmap2.getHeight();
        cardView.setLayoutParams(layoutParams);
        imageView.setImageBitmap(bitmap2);
    }

    private Bitmap scaleBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        float scale = Math.min(((float)cardView.getHeight()) / bitmap.getHeight(),
                               ((float)cardView.getWidth()) / bitmap.getWidth());
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                                    matrix, true);
    }

}
