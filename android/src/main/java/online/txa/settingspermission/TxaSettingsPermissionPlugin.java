package online.txa.settingspermission;

/**
 * TXA Settings Permission Plugin
 * Author: TXA (txa@nrotxa.online)
 * GitHub: https://github.com/TXAVLOG/txa-settings-permission
 *
 * Hỗ trợ mở tất cả các màn hình Settings cấp quyền trên Android,
 * tự động tương thích theo Android version.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "TxaSettingsPermission")
public class TxaSettingsPermissionPlugin extends Plugin {

    private static final String TAG = "TxaSettingsPermission";

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: build log object đẹp trả về JS
    // ─────────────────────────────────────────────────────────────────────────

    private JSObject buildLog(String level, String message, String action, boolean success) {
        JSObject log = new JSObject();
        log.put("level", level);
        log.put("message", message);
        log.put("action", action);
        log.put("success", success);
        log.put("androidVersion", Build.VERSION.SDK_INT);
        log.put("androidRelease", Build.VERSION.RELEASE);
        log.put("timestamp", System.currentTimeMillis());
        return log;
    }

    private void logInfo(String msg) {
        Log.i(TAG, "✅ " + msg);
    }

    private void logWarn(String msg) {
        Log.w(TAG, "⚠️ " + msg);
    }

    private void logError(String msg) {
        Log.e(TAG, "❌ " + msg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getDeviceInfo: thông tin thiết bị + Android version
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void getDeviceInfo(PluginCall call) {
        JSObject result = new JSObject();
        result.put("androidSdk", Build.VERSION.SDK_INT);
        result.put("androidRelease", Build.VERSION.RELEASE);
        result.put("manufacturer", Build.MANUFACTURER);
        result.put("model", Build.MODEL);
        result.put("brand", Build.BRAND);
        result.put("device", Build.DEVICE);
        result.put("packageName", getContext().getPackageName());

        // Feature flags theo version
        result.put("supportsManageAllFiles", Build.VERSION.SDK_INT >= Build.VERSION_CODES.R);
        result.put("supportsNotificationPolicy", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
        result.put("supportsOverlay", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
        result.put("supportsBatteryOptimization", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
        result.put("supportsMediaPermissions", Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU);
        result.put("supportsInstallUnknownApps", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        result.put("supportsUsageStats", Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1);
        result.put("supportsAccessibility", true);
        result.put("supportsDeviceAdmin", true);

        logInfo("getDeviceInfo → SDK " + Build.VERSION.SDK_INT + " (" + Build.MANUFACTURER + " " + Build.MODEL + ")");
        call.resolve(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openSettings: mở màn hình Settings tổng quát của app
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openSettings(PluginCall call) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);

            logInfo("openSettings → Opened app settings");
            JSObject result = new JSObject();
            result.put("opened", true);
            result.put("screen", "APP_SETTINGS");
            result.put("log", buildLog("info", "Đã mở màn hình cài đặt ứng dụng", "APP_SETTINGS", true));
            call.resolve(result);
        } catch (Exception e) {
            logError("openSettings → " + e.getMessage());
            call.reject("TXA_ERR_OPEN_SETTINGS: Không thể mở Settings → " + e.getMessage(), "OPEN_SETTINGS_FAILED", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openManageAllFiles: MANAGE_EXTERNAL_STORAGE (Android 11+)
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openManageAllFiles(PluginCall call) {
        JSObject result = new JSObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                logInfo("openManageAllFiles → Already granted");
                result.put("granted", true);
                result.put("opened", false);
                result.put("log", buildLog("info", "Đã có quyền MANAGE_EXTERNAL_STORAGE", "MANAGE_ALL_FILES", true));
                call.resolve(result);
                return;
            }
            try {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getContext().getPackageName())
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                logInfo("openManageAllFiles → Opened per-app screen");
                result.put("granted", false);
                result.put("opened", true);
                result.put("screen", "MANAGE_APP_ALL_FILES_ACCESS");
                result.put("log", buildLog("info", "Đã mở màn hình 'Truy cập tất cả các file'", "MANAGE_APP_ALL_FILES", true));
                call.resolve(result);
            } catch (Exception e) {
                logWarn("openManageAllFiles → Per-app failed, trying global → " + e.getMessage());
                try {
                    Intent fallback = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getActivity().startActivity(fallback);
                    result.put("granted", false);
                    result.put("opened", true);
                    result.put("screen", "MANAGE_ALL_FILES_ACCESS_PERMISSION");
                    result.put("log", buildLog("warn", "Fallback: Đã mở màn hình All Files Access chung", "MANAGE_ALL_FILES_FALLBACK", true));
                    call.resolve(result);
                } catch (Exception e2) {
                    logError("openManageAllFiles → Both failed: " + e2.getMessage());
                    call.reject("TXA_ERR_MANAGE_ALL_FILES: Không thể mở màn hình quản lý file → " + e2.getMessage(), "MANAGE_ALL_FILES_FAILED", e2);
                }
            }
        } else {
            logInfo("openManageAllFiles → Android < 11, permission not needed");
            result.put("granted", true);
            result.put("opened", false);
            result.put("log", buildLog("info", "Android " + Build.VERSION.RELEASE + " < 11, không cần quyền MANAGE_EXTERNAL_STORAGE", "MANAGE_ALL_FILES_NOT_NEEDED", true));
            call.resolve(result);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // checkManageAllFiles
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void checkManageAllFiles(PluginCall call) {
        try {
            JSObject result = new JSObject();
            boolean granted = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                granted = Environment.isExternalStorageManager();
            }

            result.put("granted", granted);
            result.put("androidSdk", Build.VERSION.SDK_INT);
            result.put("log", buildLog(granted ? "info" : "warn",
                granted ? "Đã có quyền MANAGE_EXTERNAL_STORAGE" : "Chưa có quyền MANAGE_EXTERNAL_STORAGE",
                "CHECK_MANAGE_ALL_FILES", granted));
            logInfo("checkManageAllFiles → " + granted);
            call.resolve(result);
        } catch (Throwable e) {
            logError("checkManageAllFiles crash: " + e.getMessage());
            call.reject("TXA_ERR: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openOverlaySettings: Hiển thị trên ứng dụng khác (Android 6+)
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openOverlaySettings(PluginCall call) {
        JSObject result = new JSObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(getContext())) {
                logInfo("openOverlaySettings → Already granted");
                result.put("granted", true);
                result.put("opened", false);
                result.put("log", buildLog("info", "Đã có quyền hiển thị trên ứng dụng khác", "OVERLAY", true));
                call.resolve(result);
                return;
            }
            try {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getContext().getPackageName())
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                logInfo("openOverlaySettings → Opened");
                result.put("granted", false);
                result.put("opened", true);
                result.put("screen", "MANAGE_OVERLAY_PERMISSION");
                result.put("log", buildLog("info", "Đã mở màn hình 'Hiển thị trên ứng dụng khác'", "OVERLAY", true));
                call.resolve(result);
            } catch (Exception e) {
                logError("openOverlaySettings → " + e.getMessage());
                call.reject("TXA_ERR_OVERLAY: Không thể mở màn hình Overlay → " + e.getMessage(), "OVERLAY_FAILED", e);
            }
        } else {
            logInfo("openOverlaySettings → Android < 6, not needed");
            result.put("granted", true);
            result.put("opened", false);
            result.put("log", buildLog("info", "Android < 6, không cần quyền Overlay", "OVERLAY_NOT_NEEDED", true));
            call.resolve(result);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // checkOverlay
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void checkOverlay(PluginCall call) {
        boolean granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
            || Settings.canDrawOverlays(getContext());
        JSObject result = new JSObject();
        result.put("granted", granted);
        result.put("log", buildLog(granted ? "info" : "warn",
            granted ? "Đã có quyền Overlay" : "Chưa có quyền Overlay",
            "CHECK_OVERLAY", granted));
        call.resolve(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openInstallUnknownApps: Cài từ nguồn không rõ (Android 8+)
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openInstallUnknownApps(PluginCall call) {
        JSObject result = new JSObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (getContext().getPackageManager().canRequestPackageInstalls()) {
                logInfo("openInstallUnknownApps → Already granted");
                result.put("granted", true);
                result.put("opened", false);
                result.put("log", buildLog("info", "Đã có quyền cài ứng dụng từ nguồn không rõ", "INSTALL_UNKNOWN_APPS", true));
                call.resolve(result);
                return;
            }
            try {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getContext().getPackageName())
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                logInfo("openInstallUnknownApps → Opened");
                result.put("granted", false);
                result.put("opened", true);
                result.put("screen", "MANAGE_UNKNOWN_APP_SOURCES");
                result.put("log", buildLog("info", "Đã mở màn hình 'Cài ứng dụng không rõ nguồn'", "INSTALL_UNKNOWN_APPS", true));
                call.resolve(result);
            } catch (Exception e) {
                logError("openInstallUnknownApps → " + e.getMessage());
                call.reject("TXA_ERR_INSTALL_UNKNOWN: Không thể mở màn hình → " + e.getMessage(), "INSTALL_UNKNOWN_FAILED", e);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android 5-7: chỉ mở settings chung
            try {
                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                logWarn("openInstallUnknownApps → Android < 8, opened Security Settings");
                result.put("granted", false);
                result.put("opened", true);
                result.put("screen", "SECURITY_SETTINGS");
                result.put("log", buildLog("warn", "Android < 8: Mở Security Settings để bật 'Nguồn không xác định'", "INSTALL_UNKNOWN_LEGACY", true));
                call.resolve(result);
            } catch (Exception e) {
                call.reject("TXA_ERR_SECURITY_SETTINGS: " + e.getMessage(), "SECURITY_SETTINGS_FAILED", e);
            }
        } else {
            result.put("granted", true);
            result.put("log", buildLog("info", "Android quá cũ, mặc định cho phép", "INSTALL_UNKNOWN_OLD", true));
            call.resolve(result);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // checkInstallUnknownApps
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void checkInstallUnknownApps(PluginCall call) {
        try {
            boolean granted = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                granted = getContext().getPackageManager().canRequestPackageInstalls();
            }
            JSObject result = new JSObject();
            result.put("granted", granted);
            result.put("log", buildLog(granted ? "info" : "warn",
                granted ? "Đã có quyền cài APK ngoài" : "Chưa có quyền cài APK ngoài",
                "CHECK_INSTALL_UNKNOWN", granted));
            call.resolve(result);
        } catch (Throwable e) {
            logError("checkInstallUnknownApps crash: " + e.getMessage());
            call.reject("TXA_ERR: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openBatteryOptimization: Bỏ qua tối ưu hóa pin (Android 6+)
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openBatteryOptimization(PluginCall call) {
        JSObject result = new JSObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getContext().getPackageName())) {
                logInfo("openBatteryOptimization → Already ignoring");
                result.put("granted", true);
                result.put("opened", false);
                result.put("log", buildLog("info", "App đã được bỏ qua tối ưu hóa pin", "BATTERY_OPTIMIZATION", true));
                call.resolve(result);
                return;
            }
            try {
                Intent intent = new Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getContext().getPackageName())
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                logInfo("openBatteryOptimization → Opened");
                result.put("granted", false);
                result.put("opened", true);
                result.put("screen", "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
                result.put("log", buildLog("info", "Đã mở màn hình bỏ qua tối ưu hóa pin", "BATTERY_OPTIMIZATION", true));
                call.resolve(result);
            } catch (Exception e) {
                logWarn("openBatteryOptimization → Direct failed, opening battery settings: " + e.getMessage());
                try {
                    Intent fallback = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getActivity().startActivity(fallback);
                    result.put("opened", true);
                    result.put("screen", "BATTERY_SAVER_SETTINGS");
                    result.put("log", buildLog("warn", "Fallback: Đã mở Battery Saver Settings", "BATTERY_OPTIMIZATION_FALLBACK", true));
                    call.resolve(result);
                } catch (Exception e2) {
                    call.reject("TXA_ERR_BATTERY: Không thể mở màn hình pin → " + e2.getMessage(), "BATTERY_FAILED", e2);
                }
            }
        } else {
            logInfo("openBatteryOptimization → Android < 6, not needed");
            result.put("granted", true);
            result.put("log", buildLog("info", "Android < 6, không cần xử lý Battery Optimization", "BATTERY_NOT_NEEDED", true));
            call.resolve(result);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // checkBatteryOptimization
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void checkBatteryOptimization(PluginCall call) {
        try {
            boolean ignoring = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
                ignoring = pm != null && pm.isIgnoringBatteryOptimizations(getContext().getPackageName());
            }
            JSObject result = new JSObject();
            result.put("ignoring", ignoring);
            result.put("granted", ignoring);
            result.put("log", buildLog(ignoring ? "info" : "warn",
                ignoring ? "App đang được bỏ qua tối ưu hóa pin" : "App vẫn bị tối ưu hóa pin",
                "CHECK_BATTERY", ignoring));
            call.resolve(result);
        } catch (Throwable e) {
            logError("checkBatteryOptimization crash: " + e.getMessage());
            call.reject("TXA_ERR: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openNotificationSettings: Cài đặt thông báo
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openNotificationSettings(PluginCall call) {
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", getContext().getPackageName());
                intent.putExtra("app_uid", getContext().getApplicationInfo().uid);
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            logInfo("openNotificationSettings → Opened (SDK " + Build.VERSION.SDK_INT + ")");
            JSObject result = new JSObject();
            result.put("opened", true);
            result.put("screen", "APP_NOTIFICATION_SETTINGS");
            result.put("log", buildLog("info", "Đã mở màn hình cài đặt thông báo", "NOTIFICATION_SETTINGS", true));
            call.resolve(result);
        } catch (Exception e) {
            logError("openNotificationSettings → " + e.getMessage());
            call.reject("TXA_ERR_NOTIFICATION: Không thể mở cài đặt thông báo → " + e.getMessage(), "NOTIFICATION_FAILED", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openNotificationPolicyAccess: Chế độ không làm phiền (Android 6+)
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openNotificationPolicyAccess(PluginCall call) {
        JSObject result = new JSObject();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                logInfo("openNotificationPolicyAccess → Opened");
                result.put("opened", true);
                result.put("screen", "NOTIFICATION_POLICY_ACCESS_SETTINGS");
                result.put("log", buildLog("info", "Đã mở màn hình quyền truy cập chính sách thông báo (DND)", "NOTIFICATION_POLICY", true));
                call.resolve(result);
            } catch (Exception e) {
                logError("openNotificationPolicyAccess → " + e.getMessage());
                call.reject("TXA_ERR_NOTIFICATION_POLICY: " + e.getMessage(), "NOTIFICATION_POLICY_FAILED", e);
            }
        } else {
            result.put("granted", true);
            result.put("log", buildLog("info", "Android < 6, không cần quyền Notification Policy", "NOTIFICATION_POLICY_NOT_NEEDED", true));
            call.resolve(result);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openAccessibilitySettings: Quyền trợ năng
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openAccessibilitySettings(PluginCall call) {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            logInfo("openAccessibilitySettings → Opened");
            JSObject result = new JSObject();
            result.put("opened", true);
            result.put("screen", "ACCESSIBILITY_SETTINGS");
            result.put("log", buildLog("info", "Đã mở màn hình Trợ năng (Accessibility)", "ACCESSIBILITY", true));
            call.resolve(result);
        } catch (Exception e) {
            logError("openAccessibilitySettings → " + e.getMessage());
            call.reject("TXA_ERR_ACCESSIBILITY: " + e.getMessage(), "ACCESSIBILITY_FAILED", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openUsageStatsSettings: Quyền sử dụng dữ liệu (Android 5.1+)
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openUsageStatsSettings(PluginCall call) {
        JSObject result = new JSObject();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
                logInfo("openUsageStatsSettings → Opened");
                result.put("opened", true);
                result.put("screen", "USAGE_ACCESS_SETTINGS");
                result.put("log", buildLog("info", "Đã mở màn hình quyền truy cập dữ liệu sử dụng", "USAGE_STATS", true));
                call.resolve(result);
            } catch (Exception e) {
                logError("openUsageStatsSettings → " + e.getMessage());
                call.reject("TXA_ERR_USAGE_STATS: " + e.getMessage(), "USAGE_STATS_FAILED", e);
            }
        } else {
            result.put("granted", true);
            result.put("log", buildLog("info", "Android < 5.1, không cần quyền Usage Stats", "USAGE_STATS_NOT_NEEDED", true));
            call.resolve(result);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openDeviceAdminSettings: Quản trị thiết bị
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openDeviceAdminSettings(PluginCall call) {
        try {
            Intent intent = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            logInfo("openDeviceAdminSettings → Opened");
            JSObject result = new JSObject();
            result.put("opened", true);
            result.put("screen", "DEVICE_INFO_SETTINGS");
            result.put("log", buildLog("info", "Đã mở màn hình thông tin thiết bị", "DEVICE_ADMIN", true));
            call.resolve(result);
        } catch (Exception e) {
            logError("openDeviceAdminSettings → " + e.getMessage());
            call.reject("TXA_ERR_DEVICE_ADMIN: " + e.getMessage(), "DEVICE_ADMIN_FAILED", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // openWifiSettings / openLocationSettings / openBluetoothSettings
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void openWifiSettings(PluginCall call) {
        openSimpleSettings(call, Settings.ACTION_WIFI_SETTINGS, "WIFI_SETTINGS", "Đã mở cài đặt WiFi");
    }

    @PluginMethod
    public void openLocationSettings(PluginCall call) {
        openSimpleSettings(call, Settings.ACTION_LOCATION_SOURCE_SETTINGS, "LOCATION_SETTINGS", "Đã mở cài đặt Vị trí");
    }

    @PluginMethod
    public void openBluetoothSettings(PluginCall call) {
        openSimpleSettings(call, Settings.ACTION_BLUETOOTH_SETTINGS, "BLUETOOTH_SETTINGS", "Đã mở cài đặt Bluetooth");
    }

    @PluginMethod
    public void openNfcSettings(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            openSimpleSettings(call, Settings.ACTION_NFC_SETTINGS, "NFC_SETTINGS", "Đã mở cài đặt NFC");
        } else {
            openSimpleSettings(call, Settings.ACTION_WIRELESS_SETTINGS, "WIRELESS_SETTINGS", "Đã mở cài đặt Wireless (NFC fallback)");
        }
    }

    @PluginMethod
    public void openDateTimeSettings(PluginCall call) {
        openSimpleSettings(call, Settings.ACTION_DATE_SETTINGS, "DATE_SETTINGS", "Đã mở cài đặt Ngày giờ");
    }

    @PluginMethod
    public void openLanguageSettings(PluginCall call) {
        openSimpleSettings(call, Settings.ACTION_LOCALE_SETTINGS, "LOCALE_SETTINGS", "Đã mở cài đặt Ngôn ngữ");
    }

    @PluginMethod
    public void openStorageSettings(PluginCall call) {
        openSimpleSettings(call, Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "STORAGE_SETTINGS", "Đã mở cài đặt Bộ nhớ");
    }

    @PluginMethod
    public void openDisplaySettings(PluginCall call) {
        openSimpleSettings(call, Settings.ACTION_DISPLAY_SETTINGS, "DISPLAY_SETTINGS", "Đã mở cài đặt Màn hình");
    }

    @PluginMethod
    public void openSoundSettings(PluginCall call) {
        openSimpleSettings(call, Settings.ACTION_SOUND_SETTINGS, "SOUND_SETTINGS", "Đã mở cài đặt Âm thanh");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // checkRuntimePermission: kiểm tra bất kỳ runtime permission nào
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void checkRuntimePermission(PluginCall call) {
        try {
            String permission = call.getString("permission");
            if (permission == null || permission.isEmpty()) {
                call.reject("TXA_ERR_PARAM: Thiếu tham số 'permission'", "MISSING_PARAM");
                return;
            }
            boolean granted = ContextCompat.checkSelfPermission(getContext(), permission)
                == PackageManager.PERMISSION_GRANTED;
            JSObject result = new JSObject();
            result.put("permission", permission);
            result.put("granted", granted);
            result.put("log", buildLog(granted ? "info" : "warn",
                granted ? "Đã có quyền: " + permission : "Chưa có quyền: " + permission,
                "CHECK_RUNTIME_PERMISSION", granted));
            call.resolve(result);
        } catch (Throwable e) {
            logError("checkRuntimePermission crash: " + e.getMessage());
            call.reject("TXA_ERR: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // checkMultiplePermissions: check nhiều quyền 1 lúc
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void checkMultiplePermissions(PluginCall call) {
        JSArray permsArray = call.getArray("permissions");
        if (permsArray == null) {
            call.reject("TXA_ERR_PARAM: Thiếu tham số 'permissions' (array)", "MISSING_PARAM");
            return;
        }

        JSObject result = new JSObject();
        JSObject permResults = new JSObject();
        boolean allGranted = true;
        List<String> missing = new ArrayList<>();

        try {
            JSONArray arr = new JSONArray(permsArray.toList());
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    String perm = arr.getString(i);
                    boolean granted = ContextCompat.checkSelfPermission(getContext(), perm)
                        == PackageManager.PERMISSION_GRANTED;
                    permResults.put(perm, granted);
                    if (!granted) {
                        allGranted = false;
                        missing.add(perm);
                    }
                }
            }
        } catch (JSONException e) {
            call.reject("TXA_ERR_PARSE: Lỗi parse permissions array → " + e.getMessage(), "PARSE_ERROR", e);
            return;
        }

        result.put("results", permResults);
        result.put("allGranted", allGranted);
        try {
            result.put("missing", new JSArray(missing));
        } catch (Exception ignore) {}
        result.put("log", buildLog(allGranted ? "info" : "warn",
            allGranted ? "Tất cả quyền đã được cấp" : "Thiếu " + missing.size() + " quyền: " + missing,
            "CHECK_MULTIPLE_PERMISSIONS", allGranted));
        call.resolve(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // checkAllRequired: kiểm tra toàn bộ các permission cần thiết của app
    // ─────────────────────────────────────────────────────────────────────────

    @PluginMethod
    public void checkAllRequired(PluginCall call) {
        JSObject result = new JSObject();
        JSObject checks = new JSObject();

        // MANAGE_EXTERNAL_STORAGE
        boolean manageFiles = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
            || Environment.isExternalStorageManager();
        checks.put("manageAllFiles", manageFiles);

        // Overlay
        boolean overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
            || Settings.canDrawOverlays(getContext());
        checks.put("overlay", overlay);

        // Install unknown apps
        boolean installUnknown = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
            || getContext().getPackageManager().canRequestPackageInstalls();
        checks.put("installUnknownApps", installUnknown);

        // Battery optimization
        boolean batteryIgnoring = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            batteryIgnoring = pm != null && pm.isIgnoringBatteryOptimizations(getContext().getPackageName());
        }
        checks.put("batteryOptimization", batteryIgnoring);

        // Runtime permissions thường gặp
        checks.put("camera", ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
        checks.put("microphone", ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
        checks.put("location", ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);

        // Media permissions (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checks.put("readMediaImages", ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED);
            checks.put("readMediaVideo", ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED);
            checks.put("readMediaAudio", ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED);
            checks.put("postNotifications", ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        } else {
            checks.put("readExternalStorage", ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            checks.put("writeExternalStorage", ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        }

        result.put("checks", checks);
        result.put("androidSdk", Build.VERSION.SDK_INT);
        result.put("androidRelease", Build.VERSION.RELEASE);
        logInfo("checkAllRequired → SDK " + Build.VERSION.SDK_INT + " | checks done");
        call.resolve(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    // FILE SYSTEM METHODS (Plugin Style)
    // ─────────────────────────────────────────────────────────────────────────

    private File getFileObject(String path) {
        if (path.startsWith("/")) {
            return new File(path);
        }
        return new File(getContext().getExternalFilesDir(null), path);
    }

    @PluginMethod
    public void writeFile(PluginCall call) {
        String path = call.getString("path");
        String data = call.getString("data");
        String encoding = call.getString("encoding", "utf8");
        boolean recursive = call.getBoolean("recursive", false);

        if (path == null || data == null) {
            call.reject("Path and data are required");
            return;
        }

        try {
            File file = getFileObject(path);
            if (recursive && file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                if ("base64".equalsIgnoreCase(encoding)) {
                    byte[] decodedBytes = Base64.decode(data, Base64.DEFAULT);
                    fos.write(decodedBytes);
                } else {
                    fos.write(data.getBytes(StandardCharsets.UTF_8));
                }
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to write file: " + e.getMessage());
        }
    }

    @PluginMethod
    public void appendFile(PluginCall call) {
        String path = call.getString("path");
        String data = call.getString("data");
        String encoding = call.getString("encoding", "utf8");

        if (path == null || data == null) {
            call.reject("Path and data are required");
            return;
        }

        try {
            File file = getFileObject(path);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(file, true)) {
                if ("base64".equalsIgnoreCase(encoding)) {
                    byte[] decodedBytes = Base64.decode(data, Base64.DEFAULT);
                    fos.write(decodedBytes);
                } else {
                    fos.write(data.getBytes(StandardCharsets.UTF_8));
                }
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to append file: " + e.getMessage());
        }
    }

    @PluginMethod
    public void readFile(PluginCall call) {
        String path = call.getString("path");
        if (path == null) {
            call.reject("Path is required");
            return;
        }

        try {
            File file = getFileObject(path);
            if (!file.exists()) {
                call.reject("File does not exist");
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            String content = new String(data, StandardCharsets.UTF_8);
            JSObject result = new JSObject();
            result.put("data", content);
            call.resolve(result);
        } catch (Exception e) {
            call.reject("Failed to read file: " + e.getMessage());
        }
    }

    @PluginMethod
    public void deleteFile(PluginCall call) {
        String path = call.getString("path");
        if (path == null) {
            call.reject("Path is required");
            return;
        }

        try {
            File file = getFileObject(path);
            if (file.exists() && file.delete()) {
                call.resolve();
            } else {
                call.reject("File could not be deleted or does not exist");
            }
        } catch (Exception e) {
            call.reject("Failed to delete file: " + e.getMessage());
        }
    }

    @PluginMethod
    public void mkdir(PluginCall call) {
        String path = call.getString("path");
        boolean recursive = call.getBoolean("recursive", false);
        if (path == null) {
            call.reject("Path is required");
            return;
        }

        File dir = getFileObject(path);
        boolean success = recursive ? dir.mkdirs() : dir.mkdir();
        if (success || dir.exists()) {
            call.resolve();
        } else {
            call.reject("Could not create directory");
        }
    }

    @PluginMethod
    public void getUri(PluginCall call) {
        String path = call.getString("path");
        if (path == null) {
            call.reject("Path is required");
            return;
        }

        File file = getFileObject(path);
        JSObject result = new JSObject();
        result.put("uri", Uri.fromFile(file).toString());
        call.resolve(result);
    }

    private void openSimpleSettings(PluginCall call, String action, String screenName, String logMsg) {
        try {
            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            logInfo(screenName + " → Opened");
            JSObject result = new JSObject();
            result.put("opened", true);
            result.put("screen", screenName);
            result.put("log", buildLog("info", logMsg, screenName, true));
            call.resolve(result);
        } catch (Exception e) {
            logError(screenName + " → " + e.getMessage());
            call.reject("TXA_ERR_" + screenName + ": Không thể mở → " + e.getMessage(), screenName + "_FAILED", e);
        }
    }
}
