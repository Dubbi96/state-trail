import * as tanstackQuery from '@tanstack/eslint-plugin-query';
import typescriptPlugin from '@typescript-eslint/eslint-plugin';
import tsParser from '@typescript-eslint/parser';
import prettierPlugin from 'eslint-plugin-prettier';
import reactHooks from 'eslint-plugin-react-hooks';
import unusedImports from 'eslint-plugin-unused-imports';
import globals from 'globals';

export default {
    files: ['**/*.{ts,tsx,js,jsx}'],
    ignores: ['dist', 'craco.config.js', '.eslintrc.json'],
    languageOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
        parser: tsParser,
        parserOptions: {
            ecmaFeatures: {
                jsx: true,
            },
        },
        globals: globals.browser,
    },
    plugins: {
        'react-hooks': reactHooks,
        '@typescript-eslint': typescriptPlugin,
        'prettier': prettierPlugin,
        'unused-imports': unusedImports,
        '@tanstack/query': tanstackQuery,
    },
    rules: {
        'react-hooks/rules-of-hooks': 'error',
        // React Refresh 권장 규칙

        // TypeScript ESLint 규칙
        '@typescript-eslint/no-unused-vars': [
            'warn',
            {
                vars: 'all',
                varsIgnorePattern: '^_',
                args: 'after-used',
                argsIgnorePattern: '^_',
            },
        ],

        // Prettier 규칙
        'prettier/prettier': [
            'error',
            {
                endOfLine: 'lf',
                singleQuote: true,
                semi: true,
            },
        ],

        // Unused imports 관련 규칙
        'unused-imports/no-unused-imports': 'error',
        'no-multiple-empty-lines': 'error',
    },
    settings: {
        react: {
            version: 'detect',
        },
    },
};
