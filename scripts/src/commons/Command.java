package src.commons;

import java.nio.charset.StandardCharsets;import java.util.Arrays;import java.util.concurrent.CompletableFuture;import java.util.concurrent.CompletionException;import static src.commons.Logger.LOGGER;//@formatter:off
public abstract class Command<T> {
    protected abstract String[] args();
    protected abstract T parse(String output);

    protected ProcessOutputPolicy outputPolicy() {return ProcessOutputPolicy.REDIRECT_ERR;}
    public Command<T> withOutputPolicy(ProcessOutputPolicy policy) {
        return new Command<>() {
            @Override protected String[] args() {return Command.this.args();}
            @Override protected T parse(String output) {return Command.this.parse(output);}
            @Override protected ProcessOutputPolicy outputPolicy() {return policy;}
        };
    }

    /// For fire and forget commands with no output/parsing
    public static Command<Void> command(String... args) {
        return new Command<>() {
            @Override protected String[] args() {return args;}
            @Override protected Void parse(String output) {return null;}
        };
    }
    
    public T exec() throws CommandExecutionException {
        try {
            LOGGER.trace("Executing command %s: %s".formatted(getClass().getSimpleName(), Arrays.toString(args())));
            ProcessBuilder pb = new ProcessBuilder(args());
            outputPolicy().apply(pb);
            Process proc = pb.start();

            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                } catch (Exception ex) {
                    throw new CompletionException(ex);
                }
            });

            int exitCode = proc.waitFor();
            String output;
            try {
                output = outputFuture.join();
            } catch (CompletionException ex) {
                throw new CommandExecutionException(ex.getCause());
            }

            if (exitCode != 0) {
                throw new CommandExecutionException(
                    new RuntimeException("Command failed (exit %d): %s".formatted(exitCode, output))
                );
            }
            return parse(output);
            } catch (CommandExecutionException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new CommandExecutionException(ex);
            }
        }

    public static class CommandExecutionException extends RuntimeException {
        CommandExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
