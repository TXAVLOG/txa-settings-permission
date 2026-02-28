#!/usr/bin/env node

/**
 * Script chạy sau khi npm install
 * Kiểm tra @capacitor/core đã được cài chưa
 * Nếu chưa → warning rõ ràng
 */

const fs = require('fs');
const path = require('path');

const REQUIRED_PEER = '@capacitor/core';
const MIN_VERSION = 4;

function findPackageJson(startDir) {
  let current = startDir;
  // Đi lên tối đa 5 cấp để tìm package.json của project
  for (let i = 0; i < 5; i++) {
    const candidate = path.join(current, 'package.json');
    if (fs.existsSync(candidate)) {
      // Bỏ qua nếu là chính plugin này
      try {
        const pkg = JSON.parse(fs.readFileSync(candidate, 'utf8'));
        if (pkg.name !== 'txa-settings-permission') {
          return candidate;
        }
      } catch (e) {
        // ignore
      }
    }
    const parent = path.dirname(current);
    if (parent === current) break;
    current = parent;
  }
  return null;
}

function getMajorVersion(versionString) {
  if (!versionString) return null;
  const cleaned = versionString.replace(/[\^~>=<]/g, '').trim();
  const match = cleaned.match(/^(\d+)/);
  return match ? parseInt(match[1], 10) : null;
}

function checkCapacitorInstalled() {
  // Tìm node_modules của project
  const scriptDir = path.dirname(__dirname); // /node_modules/txa-settings-permission
  const nodeModulesDir = path.dirname(scriptDir);        // /node_modules
  const projectDir = path.dirname(nodeModulesDir);       // project root

  const capacitorPath = path.join(nodeModulesDir, REQUIRED_PEER, 'package.json');
  const projectPkgPath = findPackageJson(projectDir);

  let projectPkg = null;
  if (projectPkgPath) {
    try {
      projectPkg = JSON.parse(fs.readFileSync(projectPkgPath, 'utf8'));
    } catch (e) {
      // ignore
    }
  }

  // ✅ Case 1: @capacitor/core có trong node_modules
  if (fs.existsSync(capacitorPath)) {
    try {
      const capPkg = JSON.parse(fs.readFileSync(capacitorPath, 'utf8'));
      const major = getMajorVersion(capPkg.version);

      if (major !== null && major < MIN_VERSION) {
        printWarning(`
⚠️  Phiên bản @capacitor/core quá cũ!
   Đang dùng: v${capPkg.version}
   Yêu cầu  : >= v${MIN_VERSION}.0.0

   Chạy lệnh sau để update:
   npm install @capacitor/core@latest
        `);
      } else {
        printSuccess(`✅ @capacitor/core v${capPkg.version} - OK!`);
      }
      return;
    } catch (e) {
      // ignore, fall through
    }
  }

  // ❌ Case 2: Không tìm thấy → check package.json xem có khai báo không
  let hasDeclared = false;
  if (projectPkg) {
    const allDeps = {
      ...(projectPkg.dependencies || {}),
      ...(projectPkg.devDependencies || {}),
      ...(projectPkg.peerDependencies || {}),
    };
    if (allDeps[REQUIRED_PEER]) {
      hasDeclared = true;
    }
  }

  if (hasDeclared) {
    printWarning(`
⚠️  txa-settings-permission đã được cài nhưng @capacitor/core chưa được install đầy đủ.

   Chạy lại:
   npm install

   Hoặc cài trực tiếp:
   npm install @capacitor/core@latest
    `);
  } else {
    printError(`
❌ THIẾU DEPENDENCY BẮT BUỘC: @capacitor/core

   Plugin "txa-settings-permission" YÊU CẦU @capacitor/core >= v${MIN_VERSION}.0.0

   Cài đặt ngay:
   npm install @capacitor/core @capacitor/android @capacitor/cli

   Sau đó khởi tạo Capacitor (nếu chưa):
   npx cap init

   Sync với Android:
   npx cap sync android

   Tài liệu: https://capacitorjs.com/docs/getting-started
    `);
  }
}

function printSuccess(msg) {
  console.log('\x1b[32m%s\x1b[0m', msg);
}

function printWarning(msg) {
  console.warn('\x1b[33m%s\x1b[0m', msg);
}

function printError(msg) {
  console.error('\x1b[31m%s\x1b[0m', msg);
}

// Tự động phát hiện nếu đang chạy trong node_modules của một project khác
const pluginRoot = path.dirname(__dirname);
const isCI = process.env.CI === 'true';

// Nếu plugin nằm trong node_modules → Chắc chắn là đang được cài vào project khác
const isDependency = pluginRoot.includes('node_modules');

// Hoặc nếu INIT_CWD (thư mục chạy npm install) khác với thư mục của plugin
const initCwd = process.env.INIT_CWD;
const isBeingInstalled = initCwd && initCwd !== pluginRoot;

if (!isCI && (isDependency || isBeingInstalled)) {
  checkCapacitorInstalled();
}
