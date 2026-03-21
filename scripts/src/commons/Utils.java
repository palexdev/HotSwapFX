package src.commons;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import src.git.Commit;

public class Utils {

    private Utils() {}

    public static String instantToDate(Instant instant) {
        Date date = Date.from(instant);
        return String.format("%1$td/%1$tm/%1$tY", date);
    }

    public static String currentTime() {
        return String.format("%tT", System.currentTimeMillis());
    }

    /// Walks the given release commit history backwards to find the previous release that included the given module
    public static String findPrevGitRelease(String module, List<Commit> releaseCommits, Commit currCommit) {
        boolean found = false;
        for (Commit rc : releaseCommits) {
            if (rc.hash().equals(currCommit.hash())) {
                found = true;
                continue;
            }
            if (!found) continue;

            if (rc.header().contains(module))
                return rc.hash();
        }
        return null;
    }
}
