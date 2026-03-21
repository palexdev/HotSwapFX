package src.git;

import java.util.ArrayList;
import java.util.List;

import static src.commons.Logger.LOGGER;

public record ModuleRelease(String module, String version, Commit commit) {

    /// Parses a list of `ModuleReleases` from a release commit.
    ///
    /// The release commit header is expected to follow the convention: `:bookmark: module@version, module2@version2, ...`
    ///
    /// The `:bookmark:` gitmoji is stripped first, then the remaining string is split by `,` and
    /// each token is split by `@` to extract the module name and version.
    ///
    /// Malformed tokens (i.e. not containing exactly one `@`) are skipped and logged as errors.
    ///
    /// @param releaseCommit the release commit to parse, identified by the `:bookmark:` gitmoji
    public static List<ModuleRelease> parseAll(Commit releaseCommit) {
        List<ModuleRelease> releases = new ArrayList<>();
        String header = releaseCommit.header().replace(":bookmark:", "").trim();
        String[] sReleases = header.split(",");
        for (String release : sReleases) {
            String[] moduleAndVer = release.trim().split("@");
            if (moduleAndVer.length == 2) {
                String module = moduleAndVer[0];
                String version = moduleAndVer[1];
                releases.add(new ModuleRelease(module, version, releaseCommit));
                continue;
            }
            LOGGER.error("Invalid release commit: " + release);
        }
        return releases;
    }
}
