package cn.qiditu.guet.android.project8;

import android.Manifest;
import android.app.Fragment;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class PhoneStateListenerFragment extends Fragment {

    @BindView(R.id.content)
    TextView content;
    Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_phone_state_listener,
                                    container,
                                    false);
        unbinder = ButterKnife.bind(this, view);
        initReceiver();
        if(PermissionHelper.checkAndRequestPermission(this,
                Manifest.permission.READ_PHONE_STATE,
                R.string.receive_phone_state_changed_need_permission,
                requestCodeReceiverPhoneStateChanged)) {
            registerReceiver();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        try {
            this.getActivity().unregisterReceiver(phoneStateChangedReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        unbinder.unbind();
        super.onDestroyView();
    }

    private BroadcastReceiver phoneStateChangedReceiver;
    private void initReceiver() {
        final String prefixTooltipCallNumber =
                        this.getString(R.string.prefix_tooltip_call_number);
        final String prefixTooltipIncomingRinging =
                        this.getString(R.string.prefix_tooltip_incoming_ringing);
        final String phoneIncomingAccept =
                        this.getString(R.string.phone_incoming_accept);
        final String phoneIdle = this.getString(R.string.phone_idle);
        final String missCallStr = this.getString(R.string.miss_call);
        final String callOverNumber = this.getString(R.string.call_over_number_is);
        final TelephonyManager telephonyManager = (TelephonyManager)this.getActivity()
                                            .getSystemService(Service.TELEPHONY_SERVICE);
        final SimpleDateFormat dateFormat =
                            new SimpleDateFormat("MM-dd hh:mm:ss", Locale.getDefault());
        phoneStateChangedReceiver = new BroadcastReceiver() {
            private boolean isIncoming = false;
            private String phoneNumber;
            private boolean missCall = false;
            private boolean isCall = false;
            @Override
            public void onReceive(Context context, Intent intent) {
                content.append(dateFormat.format(new Date()));
                content.append(":");
                String number = intent.getStringExtra("incoming_number");
                if(number != null) {
                    phoneNumber = number;
                }
                switch (telephonyManager.getCallState()) {
                    case TelephonyManager.CALL_STATE_RINGING: {
                        isIncoming = true;
                        missCall = true;
                        content.append(prefixTooltipIncomingRinging);
                        content.append(phoneNumber);
                    } break;
                    case TelephonyManager.CALL_STATE_OFFHOOK: {
                        missCall = false;
                        isCall = true;
                        content.append(isIncoming ? phoneIncomingAccept
                                                : prefixTooltipCallNumber);
                        content.append(phoneNumber);
                    } break;
                    case TelephonyManager.CALL_STATE_IDLE: {
                        if(isIncoming && missCall) {
                            content.append(missCallStr);
                            content.append(phoneNumber);
                        }
                        else if(isCall) {
                            content.append(callOverNumber);
                            content.append(phoneNumber);
                            isCall = false;
                        }
                        else {
                            content.append(phoneIdle);
                        }
                        isIncoming = false;
                        missCall = false;
                        phoneNumber = null;
                    } break;
                }
                content.append("\n");
            }
        };
    }

    private void registerReceiver() {
        this.getActivity().registerReceiver(phoneStateChangedReceiver,
                    new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
    }

    private static final int requestCodeReceiverPhoneStateChanged = 1;
    private static final @StringRes int alias1 =
        R.string.receiver_phone_state_changed_permission_disable_manual_open_permissions;
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case requestCodeReceiverPhoneStateChanged: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    registerReceiver();
                }
                else {
                    PermissionHelper.checkAndGoToSetting(this,
                            Manifest.permission.READ_PHONE_STATE,
                            alias1);
                }
            } break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
