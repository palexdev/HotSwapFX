## 25.2.1 - 17/04/2026 - 67e0876

### Refactoring

- <849b032> Make hotReload task depend on build rather than compileJava and processResources (also rename config for consistency)



## 25.2.0 - 12/04/2026 - c2d1f51

### Features

- <f20c293> Implemented DevTools
- <649131c> Implemented a legacy file watch service for those not using the Gradle plugin

### Refactoring

- <3f01f4e> Watch for resources too and notify any change
- <71a8177> Disable caching for HotRunTask

