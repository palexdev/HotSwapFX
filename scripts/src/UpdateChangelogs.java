package src;//================================================================================
// CONVENTIONS
//================================================================================
// Commit messages: <:gitmoji:> (<modules>) <message>
// ReleaseOld commits: :bookmark: <module>@<version>,...

//================================================================================
// FLOW
//================================================================================
// - parseArgs()
// - updateProjectChangelog()
//     - GitHeadHashCommand
//     - compare with lastHash from file
//     - GitCommitsBetweenCommand (module = "project")
//     - ProjectChangelogWriter.write()
// - updateModulesChangelog()
//     - GitReleaseCommitsCommand
//     - ModuleRelease.parseAll()
//     - for each module:
//         - compare hashes
//         - findPrevRelease()
//         - GitCommitsBetweenCommand
//         - ModuleChangelogWriter.write()
// - commit() (only if --commit arg is present)

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import src.commons.Command;
import src.commons.Logger.LogLevel;
import src.git.*;
import src.git.GitMojis.Category;

import static src.commons.Logger.LOGGER;
import static src.commons.Utils.findPrevGitRelease;
import static src.commons.Utils.instantToDate;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/// usr/bin/env jbang "$0" "$@" ; exit $?
// JAVA 25+
// SOURCES commons/*.java
// SOURCES git/*.java

//@formatter:off
public class UpdateChangelogs {
    static boolean COMMIT = false;

    static final String COMMIT_USER = "palexdev";
    static final String COMMIT_MAIL = "alessandro.parisi406@gmail.com";
    static final String COMMIT_AUTHOR = "github-actions[bot] <github-actions[bot]@users.noreply.github.com>";

    static final Path CHANGELOGS_PATH = Paths.get("changelogs");

    static void main(String[] args){
        parseArgs(args);
        updateProjectChangelogs();
        updateModulesChangelog();
        if (COMMIT) commit();
    }

    static void updateProjectChangelogs() {
        LOGGER.info("Updating project changelog...");
        try {
            String headHash = new GitHeadHashCommand().exec();
            var writer = new ProjectChangelogWriter(CHANGELOGS_PATH.resolve("project.md"), headHash);
            String lastHash = writer.readLastHash();

            if (headHash.equals(lastHash)) {
                LOGGER.info("Project changelog is up to date.");
                return;
            }

            List<Commit> commits = new GitCommitsBetweenCommand(lastHash, headHash, "project").exec();
            commits = commits.stream()
                .filter(c -> !c.header().contains("Updated changelogs"))
                .toList();
            if (commits.isEmpty()) {
                LOGGER.info("No changes found for project, skipping...");
                return;
            }

            LOGGER.info("Found %d commits since last update, updating...".formatted(commits.size()));
            writer.write(commits);
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

    static void updateModulesChangelog() {
        LOGGER.info("Updating modules changelogs...");
        try {
            List<Commit> releaseCommits = new GitReleaseCommitsCommand().exec();
            if (releaseCommits.isEmpty()) {
                LOGGER.warn("No release commits found.");
                return;
            }

            List<ModuleRelease> releases = ModuleRelease.parseAll(releaseCommits.getFirst());
            LOGGER.info("Found releases: " + String.join(",", releases.stream().map(ModuleRelease::module).toList()));
            for (ModuleRelease release : releases) {
                String module = release.module();
                Path path = CHANGELOGS_PATH.resolve(module + ".md");
                var writer = new ModuleChangelogWriter(path, release);
                String lastHash = writer.readLastHash();
                if(release.commit().hash().equals(lastHash)) {
                    LOGGER.info("Module changelog for %s is up to date, skipping...".formatted(module));
                    continue;
                }

                String prevReleaseHash = findPrevGitRelease(module, releaseCommits, releaseCommits.getFirst());
                List<Commit> commits = new GitCommitsBetweenCommand(prevReleaseHash, release.commit().hash(), module).exec();
                if (commits.isEmpty()) {
                    LOGGER.info("No changes found for module %s, skipping...".formatted(module));
                    continue;
                }

                LOGGER.info("Found %d commits for module %s since last update, updating...".formatted(commits.size(), module));
                writer.write(commits);
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

    static void commit() {
        try {
            LOGGER.info("Committing and pushing changes...");
            Command.command("git", "config", "user.name", COMMIT_USER).exec();
            Command.command("git", "config", "user.email", COMMIT_MAIL).exec();
            LOGGER.debug("Configured git...");
            Command.command("git", "add", ".").exec();
            LOGGER.debug("Added all files...");
            Command.command(
                "git", "commit", "--all",
                "-m", ":memo: (project) Updated changelogs!",
                "--author=%s".formatted(COMMIT_AUTHOR)
            ).exec();
            LOGGER.debug("Committed changes...");
            Command.command("git", "push").exec();
            LOGGER.debug("Pushed changes...");
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

    static void parseArgs(String[] args) {
        NavigableSet<String> toArgsSet = new TreeSet<>(Arrays.asList(args));
        if (toArgsSet.contains("--help") || toArgsSet.contains("-h")) {
            printHelp();
            System.exit(0);
        }
        if (toArgsSet.contains("--commit")) {
            COMMIT = true;
        }

        String logArg = toArgsSet.ceiling("--log-level=");
        if (logArg != null && logArg.startsWith("--log-level=")) {
            String level = logArg.split("=", 2)[1];
            LOGGER.atLevel(LogLevel.valueOf(level.toUpperCase()));
        }
    }

    static void printHelp() {
        String message = """
            This script is responsible for updating the changelogs of the project and its modules.
            It fetches all the commits between the current release and the previous release. Then, for each module in
            the current release, takes the related commits, categorizes them and appends their header to the changelog.
            
            ** Conventions & Standards**
            - Release commits are identified by the :bookmark: gitmoji
            - To indicate to which module the commit belongs to, it should be indicated in the header as (<module1>, <module2>...)
            - The gitmoji in the commit tells to which category the changes belong to. If the gitmoji is not present
              or not recognized, it falls back to the Misc category.
            - Modules changelogs have this format:
              ## <release version> - <release date> - <release commit hash>
              - <commit hash> <header message>
            - Project changelog has this format:
              ## Project - <release commit hash>
              - <commit date> <commit hash> <header message>
            - Dates are in the format of my country, sorry americans: dd-MM-yyyy
            
            ** Arguments **
              • --help, -h          : Prints this help message.
              • --commit            : Updates the changelogs, makes a commit and pushes to remote
              • --log-level=<level> : Enables logging message. Available levels are TRACE, DEBUG, INFO, WARN, ERROR (by default, WARN)
            """;
        System.out.println(message);
    }

    //================================================================================
    // WRITERS
    //================================================================================

    static abstract class ChangelogWriter {
        final Path path;
        ChangelogWriter(Path path) {this.path = path;}

        abstract String buildHeader();
        abstract String formatCommit(Commit commit);
        abstract void write(List<Commit> commits) throws IOException;

        /// Reads the last `##` header line from the changelog file, hash is the last segment
        String readLastHash() throws IOException {
            if (!Files.exists(path)) return null;
            List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return allLines.stream()
                .filter(l -> l.startsWith("## "))
                .findFirst()
                .map(l -> l.split(" - "))
                .map(parts -> parts[parts.length - 1].trim())
                .orElse(null);
        }

        void ensureFileExists() throws IOException {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }
        }
    }

    static class ProjectChangelogWriter extends ChangelogWriter {
        final String hash;
        ProjectChangelogWriter(Path path, String hash) {super(path); this.hash = hash;}

        @Override
        String buildHeader() {return "## %s - %s%n".formatted("Project", hash);}

        @Override
        String formatCommit(Commit commit) {
            String message = commit.header()
                .replaceFirst("^:\\w+:\\s*", "") // strips gitmoji
                .replaceFirst("\\(project\\)", "") // strips project tag
                .trim();
            return "- %s <%s> %s".formatted(commit.date(), commit.hash(), message);
        }

        @Override
        void write(List<Commit> commits) throws IOException {
            ensureFileExists();
            List<String> content = new LinkedList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
            List<String> toPrepend = new ArrayList<>();
            toPrepend.add(buildHeader());
            commits.stream().map(this::formatCommit).forEach(toPrepend::add);
            toPrepend.add("\n\n");
            content.addAll(0, toPrepend);
            Files.writeString(path, String.join("\n", content), StandardCharsets.UTF_8, TRUNCATE_EXISTING);
        }
    }

    static class ModuleChangelogWriter extends ChangelogWriter {
        final ModuleRelease release;
        ModuleChangelogWriter(Path path, ModuleRelease release) {super(path); this.release = release;}

        @Override
        String buildHeader() {
            return "## %s - %s - %s".formatted(release.version(), instantToDate(Instant.now()), release.commit().hash());
        }

        @Override
        String formatCommit(Commit commit) {
            String message = commit.header()
                .replaceFirst("^:\\w+:\\s*", "") // strips gitmoji
                .replaceFirst("\\([^)]+\\)\\s*", "") // strips all modules tags
                .trim();
            return "- <%s> %s".formatted(commit.hash(), message);
        }

        @Override
        void write(List<Commit> commits)throws IOException {
            ensureFileExists();
            List<String> content = new LinkedList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
            List<String> toPrepend = new ArrayList<>();
            toPrepend.add(buildHeader());
            Map<Category,List<Commit>> categorized = GitMojis.categorize(commits);
            for (Category category : GitMojis.CATEGORIES) {
                List<Commit> byCat = categorized.get(category);
                if (byCat == null || byCat.isEmpty()) continue;
                toPrepend.add("\n### %s%n".formatted(category.name()));
                byCat.stream().map(this::formatCommit).forEach(toPrepend::add);
            }
            toPrepend.add("\n\n");
            content.addAll(0, toPrepend);
            Files.writeString(path, String.join("\n", content), StandardCharsets.UTF_8, TRUNCATE_EXISTING);
        }
    }
}
