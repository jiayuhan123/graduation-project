import eslint from '@eslint/js'
import eslintConfigPrettier from 'eslint-config-prettier'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  {
    ignores: ['miniprogram_npm/**', 'miniprogram/miniprogram_npm/**', 'node_modules/**'],
  },
  {
    files: ['miniprogram/**/*.ts', 'typings/**/*.d.ts'],
    extends: [eslint.configs.recommended, ...tseslint.configs.recommendedTypeChecked],
    languageOptions: {
      globals: {
        App: 'readonly',
        Behavior: 'readonly',
        Component: 'readonly',
        Page: 'readonly',
        getApp: 'readonly',
        getCurrentPages: 'readonly',
        requirePlugin: 'readonly',
        wx: 'readonly',
      },
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
  },
  {
    files: ['*.mjs'],
    extends: [eslint.configs.recommended],
  },
  eslintConfigPrettier,
)
