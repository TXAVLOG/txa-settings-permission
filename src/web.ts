/**
 * TXA Settings Permission Plugin - Web Fallback
 * Web không có Android Settings, tất cả mock trả về granted/opened=true
 */

import { WebPlugin } from '@capacitor/core';
import type {
  TxaSettingsPermissionPlugin,
  DeviceInfoResult,
  OpenSettingsResult,
  PermissionResult,
  BatteryOptimizationResult,
  CheckAllRequiredResult,
  CheckRuntimePermissionOptions,
  CheckRuntimePermissionResult,
  CheckMultiplePermissionsOptions,
  CheckMultiplePermissionsResult,
  TxaPermissionLog,
  WriteFileOptions,
  ReadFileOptions,
  ReadFileResult,
  DeleteFileOptions,
  GetUriOptions,
  GetUriResult
} from './definitions';

function webLog(action: string): TxaPermissionLog {
  return {
    level: 'info',
    message: `[Web] ${action}: không áp dụng trên web, trả về mock`,
    action,
    success: true,
    androidVersion: 0,
    androidRelease: 'web',
    timestamp: Date.now(),
  };
}

function mockOpen(screen: string): OpenSettingsResult {
  console.warn(`[TxaSettingsPermission] Web: ${screen} không áp dụng trên web`);
  return { opened: true, granted: true, screen, log: webLog(screen) };
}

function mockGranted(action: string): PermissionResult {
  return { granted: true, log: webLog(action) };
}

export class TxaSettingsPermissionWeb
  extends WebPlugin
  implements TxaSettingsPermissionPlugin {
  async getDeviceInfo(): Promise<DeviceInfoResult> {
    return {
      androidSdk: 0, androidRelease: 'web', manufacturer: 'web',
      model: 'browser', brand: 'web', device: 'web', packageName: 'web',
      supportsManageAllFiles: false, supportsNotificationPolicy: false,
      supportsOverlay: false, supportsBatteryOptimization: false,
      supportsMediaPermissions: false, supportsInstallUnknownApps: false,
      supportsUsageStats: false, supportsAccessibility: false, supportsDeviceAdmin: false,
    };
  }

  async openSettings(): Promise<OpenSettingsResult> { return mockOpen('APP_SETTINGS'); }
  async openManageAllFiles(): Promise<OpenSettingsResult> { return mockOpen('MANAGE_ALL_FILES'); }
  async checkManageAllFiles(): Promise<PermissionResult> { return mockGranted('CHECK_MANAGE_ALL_FILES'); }
  async openOverlaySettings(): Promise<OpenSettingsResult> { return mockOpen('OVERLAY_SETTINGS'); }
  async checkOverlay(): Promise<PermissionResult> { return mockGranted('CHECK_OVERLAY'); }
  async openInstallUnknownApps(): Promise<OpenSettingsResult> { return mockOpen('INSTALL_UNKNOWN_APPS'); }
  async checkInstallUnknownApps(): Promise<PermissionResult> { return mockGranted('CHECK_INSTALL_UNKNOWN'); }

  async setBrightness(options: { value: number }): Promise<void> { console.log('Web mock setBrightness:', options.value); }
  async getBrightness(): Promise<{ value: number }> { return { value: 1 }; }
  async setVolume(options: { value: number }): Promise<void> { console.log('Web mock setVolume:', options.value); }
  async getVolume(): Promise<{ value: number }> { return { value: 1 }; }

  async openBatteryOptimization(): Promise<OpenSettingsResult> { return mockOpen('BATTERY_OPTIMIZATION'); }
  async checkBatteryOptimization(): Promise<BatteryOptimizationResult> {
    return { ignoring: true, granted: true, log: webLog('CHECK_BATTERY') };
  }
  async openNotificationSettings(): Promise<OpenSettingsResult> { return mockOpen('NOTIFICATION_SETTINGS'); }
  async openNotificationPolicyAccess(): Promise<OpenSettingsResult> { return mockOpen('NOTIFICATION_POLICY'); }
  async openAccessibilitySettings(): Promise<OpenSettingsResult> { return mockOpen('ACCESSIBILITY_SETTINGS'); }
  async openUsageStatsSettings(): Promise<OpenSettingsResult> { return mockOpen('USAGE_STATS_SETTINGS'); }
  async openDeviceAdminSettings(): Promise<OpenSettingsResult> { return mockOpen('DEVICE_ADMIN_SETTINGS'); }
  async openWifiSettings(): Promise<OpenSettingsResult> { return mockOpen('WIFI_SETTINGS'); }
  async openLocationSettings(): Promise<OpenSettingsResult> { return mockOpen('LOCATION_SETTINGS'); }
  async openBluetoothSettings(): Promise<OpenSettingsResult> { return mockOpen('BLUETOOTH_SETTINGS'); }
  async openNfcSettings(): Promise<OpenSettingsResult> { return mockOpen('NFC_SETTINGS'); }
  async openDateTimeSettings(): Promise<OpenSettingsResult> { return mockOpen('DATE_SETTINGS'); }
  async openLanguageSettings(): Promise<OpenSettingsResult> { return mockOpen('LOCALE_SETTINGS'); }
  async openStorageSettings(): Promise<OpenSettingsResult> { return mockOpen('STORAGE_SETTINGS'); }
  async openDisplaySettings(): Promise<OpenSettingsResult> { return mockOpen('DISPLAY_SETTINGS'); }
  async openSoundSettings(): Promise<OpenSettingsResult> { return mockOpen('SOUND_SETTINGS'); }

  async checkRuntimePermission(options: CheckRuntimePermissionOptions): Promise<CheckRuntimePermissionResult> {
    return { permission: options.permission, granted: true, log: webLog('CHECK_RUNTIME_PERMISSION') };
  }

  async checkMultiplePermissions(options: CheckMultiplePermissionsOptions): Promise<CheckMultiplePermissionsResult> {
    const results: Record<string, boolean> = {};
    options.permissions.forEach(p => { results[p] = true; });
    return { results, allGranted: true, missing: [], log: webLog('CHECK_MULTIPLE_PERMISSIONS') };
  }

  async checkAllRequired(): Promise<CheckAllRequiredResult> {
    return {
      checks: {
        manageAllFiles: true, overlay: true, installUnknownApps: true,
        batteryOptimization: true, camera: true, microphone: true, location: true,
      },
      androidSdk: 0,
      androidRelease: 'web',
    };
  }

  async writeFile(_options: WriteFileOptions): Promise<void> { console.warn('writeFile is not implemented on web'); }
  async appendFile(_options: WriteFileOptions): Promise<void> { console.warn('appendFile is not implemented on web'); }
  async readFile(_options: ReadFileOptions): Promise<ReadFileResult> {
    console.warn('readFile is not implemented on web');
    return { data: '' };
  }
  async deleteFile(_options: DeleteFileOptions): Promise<void> { console.warn('deleteFile is not implemented on web'); }
  async mkdir(_options: { path: string, recursive?: boolean }): Promise<void> { console.warn('mkdir is not implemented on web'); }
  async getUri(options: GetUriOptions): Promise<GetUriResult> {
    return { uri: options.path };
  }
}
