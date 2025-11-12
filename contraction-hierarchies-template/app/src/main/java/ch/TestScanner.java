package ch;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility to count how many lines in a graph file have a final integer value different from -1.
 * Usage: {@code java ch.TestScanner <path-to-graph-file>}
 */
public final class TestScanner {

    private TestScanner() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java ch.TestScanner <graph-file>");
            System.exit(1);
        }
        int count = countLinesWithNonNegativeVia(Path.of(args[0]));
        System.out.printf("Lines with last integer != -1: %d%n", count);
    }

    /**
     * Reads the file line-by-line and counts entries whose final integer is not -1.
     */
    static int countLinesWithNonNegativeVia(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line = reader.readLine();
            if (line == null) {
                return 0;
            }
            String[] header = line.trim().split("\\s+");
            int vertexCount = 0;
            if (header.length >= 1) {
                try {
                    vertexCount = Integer.parseInt(header[0]);
                } catch (NumberFormatException ignored) {
                    vertexCount = 0;
                }
            }
            for (int i = 0; i < vertexCount; i++) {
                if (reader.readLine() == null) {
                    return 0;
                }
            }
            int total = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length == 0) {
                    continue;
                }
                try {
                    long last = Long.parseLong(parts[parts.length - 1]);
                    if (last != -1L) {
                        total++;
                    }
                } catch (NumberFormatException ignored) {
                    // Skip malformed lines silently.
                }
            }
            return total;
        }
    }
}
