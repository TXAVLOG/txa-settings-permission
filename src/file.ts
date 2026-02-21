import { TxaSettingsPermission } from './index';

/**
 * File Plugin Wrapper for TxaSettingsPermission
 */
export const TxaFile = {
    writeFile: TxaSettingsPermission.writeFile,
    appendFile: TxaSettingsPermission.appendFile,
    readFile: TxaSettingsPermission.readFile,
    deleteFile: TxaSettingsPermission.deleteFile,
    mkdir: TxaSettingsPermission.mkdir,
    getUri: TxaSettingsPermission.getUri,
};
