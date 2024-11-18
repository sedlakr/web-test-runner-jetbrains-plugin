# web-test-runner-jetbrains-plugin


<!-- Plugin description -->
PEON  IntelliJ Platform Plugin for running tests via WTR ([web-test-runner](https://modern-web.dev/docs/test-runner/overview/)).

<!-- Plugin description end -->

## Installation

- Manually:

  Download the [latest release](https://github.com/sedlakr/web-test-runner-jetbrains-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Development

### New version release

 - change `pluginVersion` in `gradle.properties`
 - update `CHANGELOG.md`
 - run gradle task `build.assemble`

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
