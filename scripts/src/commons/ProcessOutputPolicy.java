package src.commons;

public enum ProcessOutputPolicy {
    INHERIT {
        @Override
        public void apply(ProcessBuilder pb) {
            pb.inheritIO();
        }
    },
    REDIRECT_ERR {
        @Override
        public void apply(ProcessBuilder pb) {
            pb.redirectErrorStream(true);
        }
    };

    public abstract void apply(ProcessBuilder pb);
}
