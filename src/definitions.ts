/**
 * TXA Settings Permission Plugin
 * Author: TXA (txa@nrotxa.online)
 * GitHub: https://github.com/TXAVLOG/txa-settings-permission
 */

export interface TxaPermissionLog {
  level: 'info' | 'warn' | 'error';
  message: string;
  action: string;
  success: boolean;
  androidVersion: number;
  androidRelease: string;
  timestamp: number;
}

export interface PermissionResult {
  granted: boolean;
  log: TxaPermissionLog;
}

export interface OpenSettingsResult {
  opened: boolean;
  granted?: boolean;
  screen?: string;
  log: TxaPermissionLog;
}

export interface DeviceInfoResult {
  androidSdk: number;
  androidRelease: string;
  manufacturer: string;
  model: string;
  brand: string;
  device: string;
  packageName: string;
  supportsManageAllFiles: boolean;
  supportsNotificationPolicy: boolean;
  supportsOverlay: boolean;
  supportsBatteryOptimization: boolean;
  supportsMediaPermissions: boolean;
  supportsInstallUnknownApps: boolean;
  supportsUsageStats: boolean;
  supportsAccessibility: boolean;
  supportsDeviceAdmin: boolean;
}

export interface AllRequiredChecks {
  manageAllFiles: boolean;
  overlay: boolean;
  installUnknownApps: boolean;
  batteryOptimization: boolean;
  camera: boolean;
  microphone: boolean;
  location: boolean;
  readMediaImages?: boolean;
  readMediaVideo?: boolean;
  readMediaAudio?: boolean;
  postNotifications?: boolean;
  readExternalStorage?: boolean;
  writeExternalStorage?: boolean;
}

export interface CheckAllRequiredResult {
  checks: AllRequiredChecks;
  androidSdk: number;
  androidRelease: string;
}

export interface CheckRuntimePermissionOptions {
  /** Tên đầy đủ của permission, vd: "android.permission.CAMERA" */
  permission: string;
}

export interface CheckRuntimePermissionResult {
  permission: string;
  granted: boolean;
  log: TxaPermissionLog;
}

export interface CheckMultiplePermissionsOptions {
  permissions: string[];
}

export interface CheckMultiplePermissionsResult {
  results: Record<string, boolean>;
  allGranted: boolean;
  missing: string[];
  log: TxaPermissionLog;
}

export interface BatteryOptimizationResult {
  ignoring: boolean;
  granted: boolean;
  log: TxaPermissionLog;
}

export interface TxaSettingsPermissionPlugin {
  // Device Info
  getDeviceInfo(): Promise<DeviceInfoResult>;

  // App Settings
  openSettings(): Promise<OpenSettingsResult>;

  // Manage All Files (Android 11+)
  openManageAllFiles(): Promise<OpenSettingsResult>;
  checkManageAllFiles(): Promise<PermissionResult>;

  // Overlay (Android 6+)
  openOverlaySettings(): Promise<OpenSettingsResult>;
  checkOverlay(): Promise<PermissionResult>;

  // Install Unknown Apps (Android 8+)
  openInstallUnknownApps(): Promise<OpenSettingsResult>;
  checkInstallUnknownApps(): Promise<PermissionResult>;

  // Battery Optimization (Android 6+)
  openBatteryOptimization(): Promise<OpenSettingsResult>;
  checkBatteryOptimization(): Promise<BatteryOptimizationResult>;

  // Notifications
  openNotificationSettings(): Promise<OpenSettingsResult>;
  openNotificationPolicyAccess(): Promise<OpenSettingsResult>;

  // Accessibility
  openAccessibilitySettings(): Promise<OpenSettingsResult>;

  // Usage Stats (Android 5.1+)
  openUsageStatsSettings(): Promise<OpenSettingsResult>;

  // Device Admin
  openDeviceAdminSettings(): Promise<OpenSettingsResult>;

  // System Settings
  openWifiSettings(): Promise<OpenSettingsResult>;
  openLocationSettings(): Promise<OpenSettingsResult>;
  openBluetoothSettings(): Promise<OpenSettingsResult>;
  openNfcSettings(): Promise<OpenSettingsResult>;
  openDateTimeSettings(): Promise<OpenSettingsResult>;
  openLanguageSettings(): Promise<OpenSettingsResult>;
  openStorageSettings(): Promise<OpenSettingsResult>;
  openDisplaySettings(): Promise<OpenSettingsResult>;
  openSoundSettings(): Promise<OpenSettingsResult>;

  // Runtime Permission checks
  checkRuntimePermission(options: CheckRuntimePermissionOptions): Promise<CheckRuntimePermissionResult>;
  checkMultiplePermissions(options: CheckMultiplePermissionsOptions): Promise<CheckMultiplePermissionsResult>;

  // All-in-one check
  checkAllRequired(): Promise<CheckAllRequiredResult>;
}
