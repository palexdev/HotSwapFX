## 25.2.1 - 17/04/2026 - 67e0876

### Misc

- <d1a11dc> Add logging for classes reloaded on matched resources patterns



## 25.2.0 - 12/04/2026 - c2d1f51

### Features

- <39875da> Replace HotSwapStrategy with two new annotations for better encapsulation (do not force to expose dev methods)
- <5b63891> Allow reloading of marked types when resources change
- <125123b> Backport hooking system from `legacy` branch

### Refactoring

- <bd4f0bd> Allow retrieving registered/tracked classes
- <3939166> Improve FX thread utilities
- <9941e85> SwapStrategy: improve swap in parent logic
- <3e9f458> Improve management of dependencies

### Documentation

- <48dcfb6> Clarify limitations of HotSwappable annotation system

### Misc

- <0b92528> Properly mark JavaFX dependencies as transitive and also project modules
- <3ead95e> SwapStrategy: null-check scene parameter
- <ad55d05> SwapStrategy: remove superfluous Platform.runLater call



## 25.1.0 - 21/03/2026 - 28a7962

### Refactoring

- <e714679> HotSwappable: make dependencies optional
- <77f7be0> HotSwapRegistry: use Class<?> instead of plain Strings
