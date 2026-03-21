package src.git;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GitMojis {
    public static final List<Category> CATEGORIES = List.of(
        new Category("Removed", ":fire:", ":wastebasket:"),
        new Category("Features", ":sparkles:", ":boom:"),
        new Category("Bug Fixes", ":bug:"),
        new Category("Refactoring", ":recycle:"),
        new Category("Documentation", ":memo:", ":books:"),
        new Category("Performance", ":zap:"),
        new Category("Style", ":art:", ":lipstick:"),
        new Category("Tests", ":white_check_mark:", ":rotating_light:"),
        new Category("Misc")
    );
    // for fast (O(1)) lookup of emoji -> category
    public static final Map<String, Category> EMOJI_CATEGORY_MAP = CATEGORIES.stream()
        .flatMap(cat -> cat.emojis().stream().map(emoji -> Map.entry(emoji, cat)))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    /// Categorizes the given commits in a map depending on the gitmoji found in the header.
    ///
    /// Defaults to `Misc` category.
    public static Map<Category, List<Commit>> categorize(List<Commit> commits) {
        return commits.stream().collect(Collectors.groupingBy(commit ->
            EMOJI_CATEGORY_MAP.entrySet().stream()
                .filter(e -> commit.header().contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(GitMojis::misc)
        ));
    }

    public static Category misc() {
        return CATEGORIES.getLast();
    }

    public record Category(String name, Set<String> emojis) {
        public Category(String name, String... emojis) {
            this(name, Set.of(emojis));
        }
    }
}
