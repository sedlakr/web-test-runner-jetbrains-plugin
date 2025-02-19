<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# web-test-runner-jetbrains-plugin Changelog

## [Unreleased]

### Added
- added support for running test in project where wtr.plugin.runner.js is present, plugin will call this file with given arguments to run tests in single file and by pattern like via peonRunner.js

## 2.3.0

### Fixed
- fixed resolving of working directory when package.json is in module folder

## 2.2.0

### Bumped
- compatibility with idea 2024.3

## 2.1.0

### Added
- possibility to run test or debug tests with opened browser, devtools and watch mode

## 2.0.0

### Upgraded
- gradle 8.6 -> 8.9

### Changed
- support latest IntelliJ version 2024.2.0.1, migrate from gradle plugin 1.x to 2.x
- filled changelog
