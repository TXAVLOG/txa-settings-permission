/**
 * TXA Settings Permission Plugin
 * Author: TXA (txa@nrotxa.online)
 * GitHub: https://github.com/TXAVLOG/txa-settings-permission
 */

import { registerPlugin } from '@capacitor/core';
import type { TxaSettingsPermissionPlugin } from './definitions';

const TxaSettingsPermission = registerPlugin<TxaSettingsPermissionPlugin>(
  'TxaSettingsPermission',
  {
    web: () => import('./web').then(m => new m.TxaSettingsPermissionWeb()),
  }
);

export * from './definitions';
export { TxaSettingsPermission };
