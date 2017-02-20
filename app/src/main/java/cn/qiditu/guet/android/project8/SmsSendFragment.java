package cn.qiditu.guet.android.project8;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class SmsSendFragment extends Fragment {

    @BindView(R.id.phone_number)
    EditText etPhoneNumber;
    @BindView(R.id.message)
    EditText etMessage;
    @BindView(R.id.btn_open_send_message_app)
    Button btnOpenSendMessageApp;
    @BindView(R.id.btn_send_message)
    Button btnSendMessage;
    @BindView(R.id.layout_root)
    ViewGroup rootLayout;
    Unbinder unbinder;

    private BroadcastReceiver sendReceiver;
    private BroadcastReceiver deliveredReceiver;

    private static final String sendAction = "SmsFragmentSend";
    private static final String deliveredAction = "SmsFragmentDelivered";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sms_send, container, false);
        unbinder = ButterKnife.bind(this, view);

        initPendingIntent();
        initReceiver();

        TextWatcher watcher =  new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                boolean isNotEmpty = etPhoneNumber.length() != 0 && etMessage.length() != 0;
                btnOpenSendMessageApp.setEnabled(isNotEmpty);
                btnSendMessage.setEnabled(isNotEmpty);
            }
        };
        etPhoneNumber.addTextChangedListener(watcher);
        etMessage.addTextChangedListener(watcher);

        //短信发送完成
        this.getActivity().registerReceiver(sendReceiver,
                                            new IntentFilter(sendAction));
        //对方接受完成
        this.getActivity().registerReceiver(deliveredReceiver,
                                            new IntentFilter(deliveredAction));
        return view;
    }

    @Override
    public void onDestroyView() {
        try {
            this.getActivity().unregisterReceiver(sendReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.getActivity().unregisterReceiver(deliveredReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        unbinder.unbind();
        super.onDestroyView();
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_open_send_message_app)
    void openSendMessageApp() {
        Uri uri = Uri.parse("sms:" + etPhoneNumber.getText().toString());
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.putExtra("sms_body", etMessage.getText().toString());
        try {
            this.getActivity().startActivity(intent);
        }
        catch (ActivityNotFoundException e) {
            Snackbar.make(rootLayout,
                        R.string.not_found_send_message_app,
                        Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private final static int requestCodeSendMessage = 1;

    @SuppressWarnings("unused")
    @OnClick(R.id.btn_send_message)
    void sendMessage() {
        if(PermissionHelper.checkAndRequestPermission(this,
                                                    Manifest.permission.SEND_SMS,
                                                    R.string.send_need_permission,
                                                    requestCodeSendMessage)) {
            sendMessage2();
        }
    }

    private PendingIntent sentPendingIntent;
    private PendingIntent deliveredPendingIntent;
    private void sendMessage2() {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(etPhoneNumber.getText().toString(),
                                null,
                                etMessage.getText().toString(),
                                sentPendingIntent,
                                deliveredPendingIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case requestCodeSendMessage: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendMessage2();
                }
                else {
                    PermissionHelper.checkAndGoToSetting(this,
                                            Manifest.permission.SEND_SMS,
                            R.string.send_permission_disable_manual_open_permissions);
                }
            } break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initPendingIntent() {
        sentPendingIntent = PendingIntent.getBroadcast(this.getActivity(),
                                                    0,
                                                    new Intent(sendAction),
                                                    PendingIntent.FLAG_CANCEL_CURRENT);
        deliveredPendingIntent = PendingIntent.getBroadcast(this.getActivity(),
                                                    0,
                                                    new Intent(deliveredAction),
                                                    PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void initReceiver() {
        final String sendSuccess = this.getString(R.string.send_success);
        final String sendGenericFailure = this.getString(R.string.send_generic_failure);
        final String sendServiceCurrentlyUnavailable =
                            this.getString(R.string.send_service_currently_unavailable);
        final String sendFailNoPduProvided =
                            this.getString(R.string.send_no_pdu_provided);
        final String sendFailRadioTurnOff =
                            this.getString(R.string.send_radio_turned_off);
        final String sendFail = this.getString(R.string.send_fail);
        sendReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message;
                switch (this.getResultCode()) {
                    case Activity.RESULT_OK: {
                        message = sendSuccess;
                    } break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE: {
                        message = sendGenericFailure;
                    } break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE: {
                        message = sendServiceCurrentlyUnavailable;
                    } break;
                    case SmsManager.RESULT_ERROR_NULL_PDU: {
                        message = sendFailNoPduProvided;
                    } break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF: {
                        message = sendFailRadioTurnOff;
                    } break;
                    default: {
                        message = sendFail;
                    } break;
                }
                Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG).show();
            }
        };
        final String deliveredSuccess = this.getString(R.string.delivered_success);
        final String deliveredFail = this.getString(R.string.delivered_fail);
        deliveredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message;
                switch (this.getResultCode()) {
                    case Activity.RESULT_OK: {
                        message = deliveredSuccess;
                    } break;
                    default: {
                        message = deliveredFail;
                    } break;
                }
                Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG).show();
            }
        };
    }
}
