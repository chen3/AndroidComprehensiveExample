package cn.qiditu.guet.android.project8;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.telephony.SmsMessage;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SmsReceiverFragment extends Fragment {

    public SmsReceiverFragment() {
        super();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            smsReceiverIntentFilter =
                    new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        }
        else {
            smsReceiverIntentFilter =
                    new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        }
    }

    @BindView(R.id.content)
    TextView content;
    Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sms_receiver, container, false);
        unbinder = ButterKnife.bind(this, view);

        if(PermissionHelper.checkAndRequestPermission(this,
                                                    Manifest.permission.RECEIVE_SMS,
                                                    R.string.receive_sms_need_permission,
                                                    requestCodeReceiverMessage)) {
            registerReceiver();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        try {
            this.getActivity().unregisterReceiver(smsReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        unbinder.unbind();
        super.onDestroyView();
    }

    private IntentFilter smsReceiverIntentFilter;
    private void registerReceiver() {
        this.getActivity().registerReceiver(smsReceiver, smsReceiverIntentFilter);
    }


    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Object[] smsMessages = (Object[])intent.getExtras().get("pdus");
            if(smsMessages == null) {
                return;
            }

            final String format = intent.getStringExtra("format");
            Map<String, String> map = new HashMap<>();
            for(Object msg : smsMessages) {
                SmsMessage smsMessage = getSmsMessage((byte[])msg, format);
                String phoneNumber = smsMessage.getOriginatingAddress();
                String message = smsMessage.getMessageBody();
                if(map.containsKey(phoneNumber)) {
                    message = map.get(phoneNumber) + message;
                    map.put(phoneNumber, map.get(phoneNumber) + message);
                }
                map.put(phoneNumber, message);
            }

            StringBuilder builder = new StringBuilder();
            for(String key : map.keySet()) {
                builder.append(key);
                builder.append(":\n");
                builder.append(map.get(key));
                builder.append("\n\n");
            }

            content.setText(builder);
        }
    };

    @SuppressWarnings("deprecation")
    private static SmsMessage getSmsMessage(@NonNull byte[] msg,
                                            @NonNull String format) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return SmsMessage.createFromPdu(msg, format);
        }
        else {
            return SmsMessage.createFromPdu(msg);
        }
    }

    private static final int requestCodeReceiverMessage = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case requestCodeReceiverMessage: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    registerReceiver();
                }
                else {
                    PermissionHelper.checkAndGoToSetting(this,
                       Manifest.permission.RECEIVE_SMS,
                       R.string.receiver_sms_permission_disable_manual_open_permissions);
                }
            } break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
