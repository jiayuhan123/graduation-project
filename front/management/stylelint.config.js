export default {
  extends: ['stylelint-config-standard-scss'],
  ignoreFiles: ['dist/**/*', 'coverage/**/*', 'node_modules/**/*'],
  overrides: [
    {
      files: ['**/*.vue'],
      customSyntax: 'postcss-html',
    },
  ],
  rules: {
    'selector-class-pattern': null,
    'selector-pseudo-class-no-unknown': [true, { ignorePseudoClasses: ['deep'] }],
  },
}
