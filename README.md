<div align="left" style="position: relative;">
<img src="assets/logo.svg" align="right" width="30%" style="margin: -20px 0 0 20px; background: white; border-radius: 12px;">
<h1>HotSwapFX</h1>
<p align="left">
	<em><i>Faster JavaFX development with HotSwap</i></em>
</p>
<p align="left">
	<img src="https://img.shields.io/github/license/palexdev/HotSwapFX?style=default&logo=opensourceinitiative&logoColor=white&color=0080ff" alt="license">
	<img src="https://img.shields.io/github/last-commit/palexdev/HotSwapFX?style=default&logo=git&logoColor=white&color=0080ff" alt="last-commit">
	<img src="https://img.shields.io/github/languages/top/palexdev/HotSwapFX?style=default&color=0080ff" alt="repo-top-language">
    <img src="https://img.shields.io/maven-central/v/io.github.palexdev/hotswapfx" alt="maven-central-version">
</p>
<p align="left"><!-- default option, no dependency badges. -->
</p>
<p align="left">
	<!-- default option, no dependency badges. -->
</p>
</div>

<br clear="right">

## 🔗 Table of Contents

- [🙌 Showcase](#-showcase)
- [📍 Overview, Features & Limitations](#-overview-features--limitations)
- [🚀 Getting Started](#-getting-started)
    - [⚙️ Dependencies](#-dependencies)
    - [🔨 Building & Running](#-building--running)
    - [⭐ Usage](#-usage)
- [📌 Project Roadmap](#-project-roadmap)
- [🔰 Contributing](#-contributing)
- 📢 [Acknowledgments](#-acknowledgments)

---

## 🙌 Showcase

<video loop src="assets/showcase.webm"></video>

## 📍 Overview, Features & Limitations

`HotSwapFX` is a small Java library aimed at assisting you during the development of your JavaFX apps. It allows to modify your views and reload them without the need to recompile and restart the whole app. It's intended to be used in a development environment, not in production, as the at its core it just watches the classpath and listens for events on the files on it, triggering the reload under certain conditions.

The service is designed to be as flexible as possible. Views are registered on the service by a unique id. When a class file changes, all the views of that class are reloaded. So, by default, the service works only on `.class` files. However, there's a pretty cool hook mechanism that allows you to either add:

- `Early hooks`: these are notified as soon as something changes on the classpath, it could be anything. This is very useful if, for example, you want to reload a view when an asset or a stylesheet changes. It's a bit like [CSSFX](https://github.com/McFoggy/cssfx) but on steroids (although a little slower and more tedious as you can see from the showcase video).
- `Late hooks`: these are notifies only when `.class` files change, and only if the class is a JavaFX Node. This can be useful if you want to reload a view when its children change. This example is also already implemented by the library, see [ChildrenTracker](https://github.com/palexdev/HotSwapFX/blob/main/src/main/java/io/github/palexdev/hotswapfx/ChildrenTracker.java).

About the components, there's plenty of config options for them too:

- You can specify how to instantiate them (by default uses reflection and the no-args constructor)
- You can specify the logic to copy the state from the old Node to the new one
- You can specify how to replace the old view in the scenegraph (there's a default here too, check [HotSwappable](https://github.com/palexdev/HotSwapFX/blob/main/src/main/java/io/github/palexdev/hotswapfx/HotSwappable.java))

<br >

Unfortunately, it's not all sunshine and roses, there are some annoying limitations and quirks:

1) I noticed that for some reason, sometimes the view would be desynchronized with the class changes. For example, if you changed a label's color to `red`, you would not see the change on reload. Then if you changed it to `blue`, you would see it `red`. I could not figure out the actual culprit of this, but I found out that delaying the reload by a slow amount of time (200ms by default, can be configured) made the mechanism much more stable and reliable.
2) This is more an implementation detail than anything else, but still a bit annoying. Java ClassLoaders cache the classes they load. This is no good for this project, because when a `.class` file changes, and we reload it to instantiate the new component, it would still load the old class. The solution is as easy as using a new ClassLoader every time a class has to be loaded, reading the bytes from the file and redefining the class. The quirk here, is that comparison via `=` or `.equals()` between classes does not work anymore. Even if the classes have the same fully qualified name, they are still different to Java. So, to solve this, the library wraps the classes in a record which overrides the comparison to check the fully qualified name.
3) In general limitations due to the JVM design, see [HotSwap](https://www.jetbrains.com/help/fleet/hotswap-java.html). The article mentions the [DCEVM](https://github.com/dcevm/dcevm) (you can find newer versions [here](https://github.com/JetBrains/JetBrainsRuntime)) project which should expand the Java's HotSwap capabilities but I did not test it (also because this projects requires Java 24 and there's no such release yet, probably once Java 25 is out).

---

## 🚀 Getting Started

### ⚙️ Dependencies

`HotSwapFX` is a small library, but still uses some external libraries, mostly yo make the implementation easier:

1) [Directory Watcher](https://github.com/palexdev/directory-watcher): used to watch file changes on the class path. It's a nice wrapper for the unconvenient Java `WatchService` API. A personal fork of the amazing work done by [(Greg Methvin)](https://github.com/gmethvin).
2) [Joor](https://github.com/jOOQ/jOOR): a nice fluent API for Java reflection. `HotSwapFX` by default uses reflection to instantiate the new views.
3) [TinyLog](https://tinylog.org/): I love it, it's the ultimate logging library for me. Easy to use and configure, how any printing library should be.

### 🔨 Building & Running

**Using `gradle`** &nbsp; [<img align="center" src="https://img.shields.io/badge/Gradle-02303A.svg?style={badge_style}&logo=gradle&logoColor=white" />](https://gradle.org/)
To build the project:

```sh
❯ gradle build
```

To run the showcase app (remember to run in debug mode):

```
❯ gradle showcase
```

### ⭐ Usage

**Using `gradle`** &nbsp; [<img align="center" src="https://img.shields.io/badge/Gradle-02303A.svg?style={badge_style}&logo=gradle&logoColor=white" />](https://gradle.org/)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.palexdev:hotswapfx:<version>'
}
```

**Using `maven`** &nbsp; [<img align="center" src="https://img.shields.io/badge/Maven-C71A36.svg?style={badge_style}&logo=apachemaven&logoColor=white" />](https://maven.apache.org/)

```xml
<dependency>
    <groupId>io.github.palexdev</groupId>
    <artifactId>hotswapfx</artifactId>
    <version>xx.x.x</version>
</dependency>
```




---
## 📌 Project Roadmap

For now, I don't have many ideas. I expect some bug fixes or minor refactors/convenience additions. I'm open to suggestions! 🙌

---

## 🔰 Contributing

- **💬 [Join the Discussions](https://github.com/palexdev/HotSwapFX/discussions)**: Share your insights, provide feedback, or ask questions.
- **🐛 [Report Issues](https://github.com/palexdev/HotSwapFX/issues)**: Submit bugs found or log feature requests for the `HotSwapFX` project.
- **💡 [Submit Pull Requests](https://github.com/palexdev/HotSwapFX/blob/main/CONTRIBUTING.md)**: Review open PRs, and submit your own PRs.

<details closed>
<summary>Contributing Guidelines</summary>

1. **Fork the Repository**: Start by forking the project repository to your github account.
2. **Clone Locally**: Clone the forked repository to your local machine using a git client.
   
   ```sh
   git clone https://github.com/palexdev/HotSwapFX
   ```
3. **Create a New Branch**: Always work on a new branch, giving it a descriptive name.
   ```sh
   git checkout -b new-feature-x
   ```
4. **Make Your Changes**: Develop and test your changes locally.
5. **Commit Your Changes**: Commit with a clear message describing your updates.
   ```sh
   git commit -m 'Implemented new feature x.'
   ```
6. **Push to github**: Push the changes to your forked repository.
   ```sh
   git push origin new-feature-x
   ```
7. **Submit a Pull Request**: Create a PR against the original project repository. Clearly describe the changes and their motivations.
8. **Review**: Once your PR is reviewed and approved, it will be merged into the main branch. Congratulations on your contribution!
</details>

<details closed>
<summary>Contributor Graph</summary>
<br>
<p align="left">
   <a href="https://github.com{/palexdev/HotSwapFX/}graphs/contributors">
      <img src="https://contrib.rocks/image?repo=palexdev/HotSwapFX">
   </a>
</p>
</details>
---

## **📢 Acknowledgments**

Shoutout to [Mauro de Wit](https://github.com/mfdewit) for showcasing this [hot reload](https://github.com/mfdewit/javafx-hot-reload) concept for JavaFX. It got me really curious about it and wanted to explore the idea further.

---
