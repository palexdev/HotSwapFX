package src.git;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import src.commons.Command;

import static src.commons.Logger.LOGGER;

/// Finds all the commits in a range filtered for a specific module (works for multi modules commits too)
public class GitCommitsBetweenCommand extends Command<List<Commit>> {
    private static final Pattern SCOPE_PATTERN = Pattern.compile("\\(([^)]+)\\)");
    private final String from;
    private final String to;
    private final String module;

    public GitCommitsBetweenCommand(String from, String to, String module) {
        this.from = from;
        this.to = to;
        this.module = module;
    }

    @Override
    protected String[] args() {
        String range = from != null ? from + ".." + to : to;
        return new String[]{"git", "log", range, "--pretty=format:%at%n%h%n%s"};
    }

    @Override
    protected List<Commit> parse(String output) {
        List<Commit> commits = new ArrayList<>();
        String[] lines = output.split("\n");
        for (int i = 0; i < lines.length - 2; i += 3) {
            LOGGER.trace("Parsing commit: " + lines[i]);
            long timestamp = Long.parseLong(lines[i].trim());
            String hash = lines[i + 1].trim();
            String header = lines[i + 2].trim();
            Matcher matcher = SCOPE_PATTERN.matcher(header);
            if (matcher.find() && Arrays.stream(matcher.group(1).split(","))
                .map(String::trim)
                .anyMatch(module::equals)
            ) {
                commits.add(new Commit(timestamp, hash, header));
            } else {
                LOGGER.trace("...was not a match");
            }
        }
        return commits;
    }
}
