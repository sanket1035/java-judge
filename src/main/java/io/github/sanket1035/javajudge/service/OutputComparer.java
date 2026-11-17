package io.github.sanket1035.javajudge.service;

/**
 * Decides if the program's output matches the expected output.
 * Ignores trailing spaces on each line, trailing blank lines and Windows line endings,
 * the same way most online judges do.
 */
public final class OutputComparer {

    private OutputComparer() { }

    public static boolean matches(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }

    public static String normalize(String text) {
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.stripTrailing()).append('\n');
        }
        return sb.toString().strip();
    }
}
