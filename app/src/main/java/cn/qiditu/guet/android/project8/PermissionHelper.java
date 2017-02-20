package cn.qiditu.guet.android.project8;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

public class PermissionHelper {

    @SuppressWarnings("unused")
    public static boolean checkAndRequestPermission(@NonNull final Fragment fragment,
                                             @NonNull final String permission) {
        return checkAndRequestPermission(fragment.getActivity(), permission);
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean checkAndRequestPermission(@NonNull final Activity activity,
                                             @NonNull final String permission) {
        return ContextCompat.checkSelfPermission(activity, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(23)
    public static boolean checkAndRequestPermission(@NonNull final Fragment fragment,
                                         @NonNull final String permission,
                                         @StringRes final int showRequestPermissionText,
                                         @IntRange(from = 0) final int requestCode) {
        return checkAndRequestPermission(fragment,
                                        permission,
                                        Application.getGlobalApplicationContext()
                                                   .getString(showRequestPermissionText),
                                        requestCode);
    }

    @TargetApi(23)
    public static boolean checkAndRequestPermission(@NonNull final Fragment fragment,
                                             @NonNull final String permission,
                                             @NonNull final String showRequestPermissionText,
                                             @IntRange(from = 0) final int requestCode) {
        boolean hasPermission = checkAndRequestPermission(fragment, permission);
        if (hasPermission) {
            return true;
        }

        Activity activity = fragment.getActivity();
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            new AlertDialog.Builder(activity)
                    .setMessage(showRequestPermissionText)
                    .setPositiveButton(activity.getString(R.string.try_permission),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    fragment.requestPermissions(new String[]{permission},
                                                                requestCode);
                                }
                            })
                    .setNegativeButton(activity.getString(R.string.cancel), null)
                    .show();
        } else {
            fragment.requestPermissions(new String[]{permission}, requestCode);
        }
        return false;
    }

    @TargetApi(23)
    @SuppressWarnings("unused")
    public static boolean checkAndRequestPermission(@NonNull final Activity activity,
                                         @NonNull final String permission,
                                         @StringRes final int showRequestPermissionText,
                                         @IntRange(from = 0) final int requestCode) {
        return checkAndRequestPermission(activity,
                                        permission,
                                        Application.getGlobalApplicationContext()
                                                   .getString(showRequestPermissionText),
                                        requestCode);
    }

    @TargetApi(23)
    @SuppressWarnings("WeakerAccess")
    public static boolean checkAndRequestPermission(@NonNull final Activity activity,
                                         @NonNull final String permission,
                                         @NonNull final String showRequestPermissionText,
                                         @IntRange(from = 0) final int requestCode) {
        boolean hasPermission = checkAndRequestPermission(activity, permission);
        if (hasPermission) {
            return true;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            new AlertDialog.Builder(activity)
                    .setMessage(showRequestPermissionText)
                    .setPositiveButton(activity.getString(R.string.try_permission),
                                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{permission},
                                    requestCode);
                        }
                    })
                    .setNegativeButton(activity.getString(R.string.cancel), null)
                    .show();
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{permission},
                    requestCode);
        }
        return false;
    }

    @SuppressWarnings("unused")
    public static void checkAndGoToSetting(@NonNull final Fragment fragment,
                                    @NonNull final String permission,
                                    @NonNull final String showRequestPermissionText) {
        checkAndGoToSetting(fragment.getActivity(), permission, showRequestPermissionText);
    }

    public static void checkAndGoToSetting(@NonNull final Fragment fragment,
                                    @NonNull final String permission,
                                    @StringRes final int showRequestPermissionText) {
        checkAndGoToSetting(fragment.getActivity(),
                            permission,
                            Application.getGlobalApplicationContext()
                                        .getString(showRequestPermissionText));
    }

    @SuppressWarnings("unused")
    public static void checkAndGoToSetting(@NonNull final Activity activity,
                                    @NonNull final String permission,
                                    @StringRes final int showRequestPermissionText) {
        checkAndGoToSetting(activity,
                            permission,
                            Application.getGlobalApplicationContext()
                                        .getString(showRequestPermissionText));
    }

    private static final String settingAction =
                                    "android.settings.APPLICATION_DETAILS_SETTINGS";
    private static final String settingPackage = "com.android.settings";
    private static final String settingClass =
                                    "com.android.settings.InstalledAppDetails";
    private static final String extraName = "com.android.settings.ApplicationPkgName";

    @SuppressWarnings("WeakerAccess")
    public static void checkAndGoToSetting(@NonNull final Activity activity,
                                    @NonNull final String permission,
                                    @NonNull final String showRequestPermissionText) {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            new AlertDialog.Builder(activity)
                    .setMessage(showRequestPermissionText)
                    .setPositiveButton("前往设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent localIntent = new Intent();
                            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (Build.VERSION.SDK_INT >= 9) {
                                localIntent.setAction(settingAction);
                                Uri uri = Uri.fromParts("package",
                                                    activity.getPackageName(), null);
                                localIntent.setData(uri);
                            } else if (Build.VERSION.SDK_INT <= 8) {
                                localIntent.setAction(Intent.ACTION_VIEW);
                                localIntent.setClassName(settingPackage, settingClass);
                                localIntent.putExtra(extraName,
                                                    activity.getPackageName());
                            }
                            activity.startActivity(localIntent);
                        }
                    })
                    .setNegativeButton(activity.getString(R.string.cancel), null)
                    .show();
        }
    }

}
