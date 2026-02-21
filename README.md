# txa-settings-permission

<div align="center">

![npm](https://img.shields.io/npm/v/txa-settings-permission?color=6366f1&style=flat-square)
![license](https://img.shields.io/npm/l/txa-settings-permission?color=22c55e&style=flat-square)
![platform](https://img.shields.io/badge/platform-Android-3ddc84?style=flat-square)
![capacitor](https://img.shields.io/badge/Capacitor-v4%20%7C%20v5%20%7C%20v6-119EFF?style=flat-square)

**Capacitor plugin by TXA** — Mở tất cả các màn hình cài đặt cấp quyền Android,
tự động tương thích theo Android version, log đẹp khi lỗi.

[📦 NPM](https://www.npmjs.com/package/txa-settings-permission) · [🐛 Issues](https://github.com/TXAVLOG/txa-settings-permission/issues) · [👤 TXA](mailto:txa@nrotxa.online)

</div>

---

## ✨ Tính năng

| Tính năng | Mô tả |
|-----------|-------|
| 📂 **All Files Access** | `MANAGE_EXTERNAL_STORAGE` (Android 11+) |
| 🪟 **Overlay** | Hiển thị trên ứng dụng khác (Android 6+) |
| 📦 **Install Unknown Apps** | Cài từ nguồn không rõ (Android 8+) |
| 🔋 **Battery Optimization** | Bỏ qua tối ưu hóa pin (Android 6+) |
| 🔔 **Notification Settings** | Cài đặt thông báo & DND |
| ♿ **Accessibility** | Quyền trợ năng |
| 📊 **Usage Stats** | Quyền dữ liệu sử dụng (Android 5.1+) |
| 📡 **System Settings** | WiFi, Location, Bluetooth, NFC, Sound, Display... |
| 🔍 **Runtime Check** | Check bất kỳ permission nào theo tên |
| 📋 **Bulk Check** | Check nhiều permission cùng lúc |
| 🔄 **Auto-compat** | Tự fallback theo Android version |
| 📝 **Đẹp Log** | Mọi kết quả đều kèm log object chi tiết |

---

## 📋 Yêu cầu

- **Capacitor** `>= 4.0.0`
- **Android SDK** `>= 22` (Android 5.0+)
- **Node.js** `>= 16`

> ⚠️ Plugin này **chỉ hoạt động trên Android**. Trên Web/iOS trả về mock (`granted: true`).

---

## 🚀 Cài đặt

```bash
npm install txa-settings-permission
npx cap sync
```

Sau khi cài, script `postinstall` tự kiểm tra `@capacitor/core`. Nếu thiếu sẽ báo:

```
❌ THIẾU DEPENDENCY BẮT BUỘC: @capacitor/core
   npm install @capacitor/core @capacitor/android @capacitor/cli
```

---

## ⚙️ Cấu hình Android

### Thêm permissions vào `AndroidManifest.xml`

Chỉ thêm permission app bạn **thực sự cần**:

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<manifest ...>

    <!-- All Files Access (Android 11+) -->
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />

    <!-- Overlay -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <!-- Bỏ qua tối ưu hóa pin -->
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <!-- Storage (Android < 13) -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="29" />

    <!-- Media (Android 13+) -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <!-- Notifications (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

</manifest>
```

### Register plugin thủ công (nếu cần)

Capacitor 6+ tự auto-discovery. Nếu cần register thủ công trong `MainActivity.java`:

```java
import online.txa.settingspermission.TxaSettingsPermissionPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(TxaSettingsPermissionPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
```

---

## 📖 API Reference

### `getDeviceInfo()`

Lấy thông tin thiết bị và feature flags theo Android version.

```typescript
const info = await TxaSettingsPermission.getDeviceInfo();
// {
//   androidSdk: 34, androidRelease: "14",
//   manufacturer: "samsung", model: "SM-S918B",
//   supportsManageAllFiles: true,     // Android 11+
//   supportsOverlay: true,            // Android 6+
//   supportsInstallUnknownApps: true, // Android 8+
//   supportsBatteryOptimization: true,
//   supportsMediaPermissions: true,   // Android 13+
//   ...
// }
```

---

### `checkAllRequired()`

Check toàn bộ permission phổ biến cùng lúc. **Nên gọi khi app khởi động.**

```typescript
const { checks, androidSdk } = await TxaSettingsPermission.checkAllRequired();

// checks.manageAllFiles   → MANAGE_EXTERNAL_STORAGE
// checks.overlay          → Draw Overlay
// checks.installUnknownApps
// checks.batteryOptimization
// checks.camera, microphone, location
// checks.readMediaVideo (Android 13+)
// checks.postNotifications (Android 13+)
// checks.readExternalStorage (Android < 13)
```

---

### All Files Access (Android 11+)

```typescript
// Check
const { granted, log } = await TxaSettingsPermission.checkManageAllFiles();
console.log(log.message); // "Đã có quyền MANAGE_EXTERNAL_STORAGE"

// Mở Settings
const result = await TxaSettingsPermission.openManageAllFiles();
// result.opened = true  → Settings đã mở
// result.granted = true → Đã có quyền từ trước
// result.log.level = 'warn' nếu fallback sang màn hình chung
```

> Android < 11: Tự động `{ granted: true }`, không mở gì.

---

### Overlay (Android 6+)

```typescript
const { granted } = await TxaSettingsPermission.checkOverlay();
if (!granted) await TxaSettingsPermission.openOverlaySettings();
```

---

### Install Unknown Apps (Android 8+)

```typescript
const { granted } = await TxaSettingsPermission.checkInstallUnknownApps();
if (!granted) {
    await TxaSettingsPermission.openInstallUnknownApps();
    // Android 8+: màn hình riêng cho app
    // Android 5-7: mở Security Settings (log.level = 'warn')
}
```

---

### Battery Optimization (Android 6+)

```typescript
const { ignoring } = await TxaSettingsPermission.checkBatteryOptimization();
if (!ignoring) {
    const result = await TxaSettingsPermission.openBatteryOptimization();
    // Tự fallback sang Battery Saver Settings nếu device không hỗ trợ
    console.log(result.log.level, result.log.message);
}
```

---

### Notifications

```typescript
await TxaSettingsPermission.openNotificationSettings();     // Cài đặt thông báo
await TxaSettingsPermission.openNotificationPolicyAccess(); // DND / Không làm phiền
```

---

### System Settings

```typescript
await TxaSettingsPermission.openWifiSettings();
await TxaSettingsPermission.openLocationSettings();
await TxaSettingsPermission.openBluetoothSettings();
await TxaSettingsPermission.openNfcSettings();
await TxaSettingsPermission.openDateTimeSettings();
await TxaSettingsPermission.openLanguageSettings();
await TxaSettingsPermission.openStorageSettings();
await TxaSettingsPermission.openDisplaySettings();
await TxaSettingsPermission.openSoundSettings();
await TxaSettingsPermission.openAccessibilitySettings();
await TxaSettingsPermission.openUsageStatsSettings();
await TxaSettingsPermission.openDeviceAdminSettings();
await TxaSettingsPermission.openSettings(); // App Settings tổng quát
```

---

### Runtime Permission Check

```typescript
// Check 1 permission
const { granted, log } = await TxaSettingsPermission.checkRuntimePermission({
    permission: 'android.permission.CAMERA',
});

// Check nhiều permission cùng lúc
const result = await TxaSettingsPermission.checkMultiplePermissions({
    permissions: [
        'android.permission.CAMERA',
        'android.permission.RECORD_AUDIO',
        'android.permission.READ_MEDIA_VIDEO',
    ],
});
console.log('Tất cả đã cấp:', result.allGranted);
console.log('Thiếu:', result.missing);
```

---

## 📝 Log Object

Mọi method đều trả về `log` kèm theo:

```typescript
{
    level: 'info',              // 'info' | 'warn' | 'error'
    message: 'Đã mở...',       // Mô tả bằng tiếng Việt
    action: 'MANAGE_ALL_FILES', // Tên action
    success: true,
    androidVersion: 34,         // Android SDK
    androidRelease: '14',       // Version string
    timestamp: 1700000000000    // Unix ms
}
```

| Level | Ý nghĩa |
|-------|---------|
| `info` | Thành công hoặc không cần thiết (Android version cũ) |
| `warn` | Fallback sang phương án dự phòng |
| `error` | Không thể thực hiện |

---

## 🔄 Ví dụ đầy đủ — Setup Permission khi khởi động

```typescript
import { TxaSettingsPermission } from 'txa-settings-permission';
import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';

export async function setupPermissions() {
    if (!Capacitor.isNativePlatform()) return;

    const info = await TxaSettingsPermission.getDeviceInfo();
    console.log(`Android ${info.androidRelease} (SDK ${info.androidSdk})`);

    const { checks } = await TxaSettingsPermission.checkAllRequired();

    if (!checks.manageAllFiles && info.supportsManageAllFiles) {
        await TxaSettingsPermission.openManageAllFiles();
        await waitForResume();
    }

    if (!checks.overlay && info.supportsOverlay) {
        await TxaSettingsPermission.openOverlaySettings();
        await waitForResume();
    }

    if (!checks.batteryOptimization && info.supportsBatteryOptimization) {
        await TxaSettingsPermission.openBatteryOptimization();
        await waitForResume();
    }
}

function waitForResume(): Promise<void> {
    return new Promise(resolve => {
        const h = App.addListener('appStateChange', async ({ isActive }) => {
            if (isActive) { (await h).remove(); resolve(); }
        });
    });
}
```

---

## 🗺️ Android Version Compatibility

| Method | API 22-22 (5.1) | API 23 (6) | API 26 (8) | API 30 (11) | API 33 (13) |
|--------|:-:|:-:|:-:|:-:|:-:|
| `openManageAllFiles` | ✅ auto | ✅ auto | ✅ auto | ✅ Settings | ✅ Settings |
| `openOverlaySettings` | ✅ auto | ✅ Settings | ✅ Settings | ✅ Settings | ✅ Settings |
| `openInstallUnknownApps` | ✅ auto | ⚠️ Security | ⚠️ Security | ✅ Per-app | ✅ Per-app |
| `openBatteryOptimization` | ✅ auto | ✅ Settings | ✅ Settings | ✅ Settings | ✅ Settings |
| `openNotificationSettings` | ⚠️ App Details | ⚠️ App Details | ✅ Settings | ✅ Settings | ✅ Settings |
| `openUsageStatsSettings` | ✅ Settings | ✅ Settings | ✅ Settings | ✅ Settings | ✅ Settings |

> ✅ auto = không cần, tự trả `granted: true` · ⚠️ = fallback/log warn

---

## 🏗️ Build & Publish

```bash
# Cài dev dependencies
npm install

# Build
npm run build

# Publish lên NPM
npm login
npm publish --access public

# Update version
npm version patch && npm publish --access public
```

---

## 📂 Cấu trúc

```
txa-settings-permission/
├── android/
│   ├── build.gradle
│   └── src/main/java/online/txa/settingspermission/
│       └── TxaSettingsPermissionPlugin.java
├── src/
│   ├── index.ts          ← Entry point
│   ├── definitions.ts    ← TypeScript types
│   └── web.ts            ← Web mock
├── scripts/
│   └── check-capacitor.js
├── package.json
├── tsconfig.json
└── rollup.config.js
```

---

## 📄 License

MIT © [TXA](mailto:txa@nrotxa.online)

---

<div align="center">
Made with ❤️ by <strong>TXA</strong> · <a href="https://github.com/TXAVLOG">github.com/TXAVLOG</a>
</div>
