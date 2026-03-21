package src.git;

import src.commons.Command;

public class GitHeadCommitCommand extends Command<Commit> {
    @Override
    protected String[] args() {
        return new String[]{"git", "log", "-1", "--pretty=format:%at%n%h%n%s"};
    }

    @Override
    protected Commit parse(String output) {
        String[] lines = output.trim().split("\n");
        long timestamp = Long.parseLong(lines[0].trim());
        String hash = lines[1].trim();
        String header = lines[2].trim();
        return new Commit(timestamp, hash, header);
    }
}
