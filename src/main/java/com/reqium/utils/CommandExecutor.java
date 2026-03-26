package com.reqium.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

/**
 * Utility for executing system shell commands from within the Minecraft client runtime.
 * Commands are always run asynchronously so they never block the game loop.
 *
 * Example – run a Windows CMD command:
 * <pre>
 *   CommandExecutor.run("cmd /c start notepad").thenAccept(out -> {});
 * </pre>
 */
public final class CommandExecutor {

    private CommandExecutor() {}

    /**
     * Executes an arbitrary shell command asynchronously.
     *
     * @param command  The command string (split by spaces, or use the array overload).
     * @return A {@link CompletableFuture} that completes with the combined stdout+stderr output.
     */
    public static CompletableFuture<String> run(String command) {
        // On Windows split via cmd /c so the full command string is passed correctly.
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String[] args = isWindows
                ? new String[]{"cmd.exe", "/c", command}
                : new String[]{"/bin/sh",  "-c", command};
        return run(args);
    }

    /**
     * Executes a command using an explicit argument array (avoids shell-expansion issues).
     *
     * @param args  Command + arguments, e.g. {@code new String[]{"cmd.exe", "/c", "dir"}}
     * @return A {@link CompletableFuture} that completes with the combined stdout+stderr output.
     */
    public static CompletableFuture<String> run(String... args) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                ProcessBuilder pb = new ProcessBuilder(args);
                pb.redirectErrorStream(true); // merge stderr into stdout
                Process process = pb.start();

                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                sb.append("[CommandExecutor error] ").append(e.getMessage());
            }
            return sb.toString().trim();
        });
    }
}
