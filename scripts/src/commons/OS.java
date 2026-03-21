package src.commons;

public enum OS {
    Windows, Linux, MacOS, Other;

    public static OS os() {
        String name = System.getProperty("os.name");
        return switch (name) {
            case String s when s.toLowerCase().contains("win") -> OS.Windows;
            case String s when s.contains("nix") || s.contains("nux") -> OS.Linux;
            case String s when s.toLowerCase().contains("mac") -> OS.MacOS;
            default -> OS.Other;
        };
    }
}
