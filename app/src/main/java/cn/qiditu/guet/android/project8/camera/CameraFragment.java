package cn.qiditu.guet.android.project8.camera;

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import cn.qiditu.guet.android.project8.PermissionHelper;
import cn.qiditu.guet.android.project8.R;

@Deprecated
public class CameraFragment extends Fragment {

    private AbstractCamera camera;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view = inflater.inflate(R.layout.fragment_camera2, container, false);
            TextureView textureView =
                    (TextureView) view.findViewById(R.id.camera_preview);
            camera = new Camera2(textureView);
        } else {
            throw new RuntimeException("Not Support");
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        camera.closeCamera();
        super.onDestroyView();
    }

    public void startPreview() {
        if (PermissionHelper.checkAndRequestPermission(this,
                Manifest.permission.CAMERA,
                R.string.use_camera_need_permission,
                requestCodeUseCamera)) {
            camera.openCamera(this.getActivity().getWindowManager()
                                                .getDefaultDisplay().getRotation());
        }
    }

    public void stopPerview() {
        camera.closeCamera();
    }

    public void tackPicture() {     //TODO
        int rotation = this.getActivity().getWindowManager()
                            .getDefaultDisplay().getRotation();
        camera.takePicture(rotation);
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
