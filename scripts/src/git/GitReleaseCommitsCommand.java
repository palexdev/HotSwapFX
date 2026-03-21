package src.git;

import java.util.ArrayList;
import java.util.List;

import src.commons.Command;

/// Retrieves all the releases commits (denoted by :bookmark:)
public class GitReleaseCommitsCommand extends Command<List<Commit>> {
    @Override
    protected String[] args() {return new String[]{"git", "log", "--grep=:bookmark:", "--pretty=format:%at%n%h%n%s"};}

    @Override
    protected List<Commit> parse(String output) {
        List<Commit> commits = new ArrayList<>();
        String[] lines = output.split("\n");
        for (int i = 0; i < lines.length - 2; i += 3) {
            long timestamp = Long.parseLong(lines[i].trim());
            String hash = lines[i + 1].trim();
            String header = lines[i + 2].trim();
            commits.add(new Commit(timestamp, hash, header));
        }
        return commits;
    }
}
