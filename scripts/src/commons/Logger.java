package src.commons;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import static src.commons.Utils.currentTime;

//@formatter:off
public class Logger {
    public static final Logger LOGGER = new Logger(LogLevel.INFO);

    private LogLevel threshold;
    public Logger(LogLevel threshold) {this.threshold = threshold;}
    public void atLevel(LogLevel threshold) {this.threshold = threshold;}

    public void trace(String message) {LogLevel.TRACE.print(message, threshold);}
    public void debug(String message) {LogLevel.DEBUG.print(message, threshold);}
    public void info(String message)  {LogLevel.INFO.print(message, threshold);}
    public void warn(String message)  {LogLevel.WARN.print(message, threshold);}
    public void error(String message) {LogLevel.ERROR.print(message, threshold);}
    public void error(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw, true));
        error(sw.toString());
    }

    public enum LogLevel {
        TRACE, DEBUG, INFO,
        WARN(System.err),
        ERROR(System.err),
        DISABLED {
            @Override
            public boolean isEnabled(LogLevel threshold) {return false;}
        };

        final PrintStream stream;
        LogLevel() {this.stream = System.out;}
        LogLevel(PrintStream stream) {this.stream = stream;}

        void print(String message, LogLevel threshold) {
            if (isEnabled(threshold))
                stream.printf("%s - [%s]: %s%n", currentTime(), name(), message);
        }

        boolean isEnabled(LogLevel threshold) {
            return this.ordinal() >= threshold.ordinal();
        }
    }
}