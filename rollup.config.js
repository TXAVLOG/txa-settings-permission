import resolve from '@rollup/plugin-node-resolve';

export default [
  // UMD build cho browser (unpkg)
  {
    input: 'dist/esm/index.js',
    output: {
      file: 'dist/plugin.js',
      format: 'iife',
      name: 'txaSettingsPermission',
      globals: {
        '@capacitor/core': 'capacitorExports',
      },
      sourcemap: true,
      inlineDynamicImports: true,
    },
    external: ['@capacitor/core'],
    plugins: [resolve()],
  },
  // CJS build cho Node/bundler
  {
    input: 'dist/esm/index.js',
    output: {
      file: 'dist/plugin.cjs.js',
      format: 'cjs',
      sourcemap: true,
      inlineDynamicImports: true,
    },
    external: ['@capacitor/core'],
    plugins: [resolve()],
  },
];
