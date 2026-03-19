package io.github.palexdev.hotswapfx.orchestration.message;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/// A message that can be sent to the server to signal which files changed, so that the service can reload and or swap
/// registered classes.
public record ReloadRequest(Changes changes) implements Message {

    //================================================================================
    // Inner Classes
    //================================================================================

    public enum ChangeType {
        ADD,
        REMOVE,
        UPDATE
    }

    public static class Changes extends HashMap<Path, ChangeType> {
        public Changes() {}
        public Changes(Map<? extends Path, ? extends ChangeType> m) {super(m);}
    }
}
