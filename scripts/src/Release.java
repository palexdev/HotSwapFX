package src;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import src.commons.Command;
import src.commons.Logger.LogLevel;
import src.commons.OS;
import src.commons.ProcessOutputPolicy;
import src.git.Commit;
import src.git.GitHeadCommitCommand;
import src.git.ModuleRelease;

import static src.commons.Logger.LOGGER;
import static src.commons.OS.os;

/// usr/bin/env jbang "$0" "$@" ; exit $?
// JAVA 25+
// SOURCES commons/*.java
// SOURCES git/*.java

// @formatter:off
public class Release {
    static boolean DRY_RUN = false;

    static final Path GRADLEW = Paths.get(os() == OS.Windows ? "gradlew.bat" : "gradlew");


    static void main(String[] args) {
        parseArgs(args);
        run();
    }

    static void run() {
        try {
            Commit head = new GitHeadCommitCommand().exec();
            LOGGER.debug("Head commit: [%s] %s".formatted(head.hash(), head.header()));

            List<ModuleRelease> releases = ModuleRelease.parseAll(head);
            if (releases.isEmpty()) {
                LOGGER.info("✅ No modules to release.");
                return;
            }

            if (DRY_RUN) {
                StringBuilder sb = new StringBuilder("Modules to release:");
                releases.forEach(r -> sb.append("  - %s@%s%n".formatted(r.module(), r.version())));
                LOGGER.info(sb.toString());
                return;
            }

            releases.forEach(Release::release);
            LOGGER.info("🎉 Release process completed.");
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

    static void release(ModuleRelease release) {
        try {
            LOGGER.info("🚀 Releasing: %s@%s%n".formatted(release.module(), release.version()));
            Command.command(GRADLEW.toAbsolutePath().toString(), ":%s:publishAndReleaseToMavenCentral".formatted(release.module()))
                .withOutputPolicy(ProcessOutputPolicy.INHERIT)
                .exec();
        } catch (Exception ex) {
            LOGGER.error("❌ Failed to release module: " + release.module());
            LOGGER.error(ex);
        }
    }

    static void parseArgs(String[] args) {
        NavigableSet<String> argSet = new TreeSet<>(Arrays.asList(args));
        if (argSet.contains("--help") || argSet.contains("-h")) {
            printHelp();
            System.exit(0);
        }
        if (argSet.contains("--dry-run")) DRY_RUN = true;

        String logArg = argSet.ceiling("--log-level=");
        if (logArg != null && logArg.startsWith("--log-level=")) {
            String level = logArg.split("=", 2)[1];
            LOGGER.atLevel(LogLevel.valueOf(level.toUpperCase()));
        }
    }

    static void printHelp() {
        String message = """
            This script publishes modules to Maven Central.
            Reads the latest commit (expected to be a release commit marked by :bookmark:),
            parses the released modules and versions from the header, and runs the Gradle
            publish task for each.

            ** Arguments **
              • --help, -h          : Prints this help message.
              • --dry-run           : Prints the modules that would be released without publishing.
              • --log-level=<level> : Sets log verbosity. Levels: TRACE, DEBUG, INFO, WARN, ERROR (default: WARN)
            """;
        System.out.println(message);
    }
}
