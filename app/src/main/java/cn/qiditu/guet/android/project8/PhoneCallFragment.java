package cn.qiditu.guet.android.project8;

import android.Manifest;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class PhoneCallFragment extends Fragment {

    @BindView(R.id.phone_number)
    TextInputEditText etPhoneNumber;
    @BindView(R.id.btn_open_call_phone_app)
    Button btnOpenCallPhoneApp;
    @BindView(R.id.btn_call_phone)
    Button btnCallPhone;
    @BindView(R.id.layout_root)
    ViewGroup rootLayout;
    Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_phone_call, container, false);
        unbinder = ButterKnife.bind(this, view);
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after) {
            }
            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                boolean isEmpty = etPhoneNumber.length() == 0;
                btnOpenCallPhoneApp.setEnabled(!isEmpty);
                btnCallPhone.setEnabled(!isEmpty);
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_open_call_phone_app)
    void openCallPhoneApp() {
        Uri uri = Uri.parse("tel:" + etPhoneNumber.getText().toString());
        Intent intent = new Intent(Intent.ACTION_DIAL, uri);
        try {
            this.getActivity().startActivity(intent);
        }
        catch (ActivityNotFoundException e) {
            Snackbar.make(rootLayout,
                        R.string.not_found_call_phone_app,
                        Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private static final int requestCodeCallPhone = 1;

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_call_phone)
    void callPhone() {
        if(PermissionHelper.checkAndRequestPermission(this,
                Manifest.permission.CALL_PHONE,
                R.string.call_phone_need_permission,
                requestCodeCallPhone)) {
            callPhone2();
        }
    }

    private void callPhone2() {
        Uri uri = Uri.parse("tel:" + etPhoneNumber.getText().toString());
        Intent intent = new Intent(Intent.ACTION_CALL, uri);
        try {
            this.getActivity().startActivity(intent);
        }
        catch (ActivityNotFoundException e) {
            Snackbar.make(rootLayout,
                    R.string.not_found_call_phone_app,
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case requestCodeCallPhone: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callPhone2();
                }
                else {
                    PermissionHelper.checkAndGoToSetting(this,
                            Manifest.permission.CALL_PHONE,
                            R.string.call_phone_permission_disable_manual_open_permissions);
                }
            } break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
