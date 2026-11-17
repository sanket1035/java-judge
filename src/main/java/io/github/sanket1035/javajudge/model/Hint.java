package io.github.sanket1035.javajudge.model;

/**
 * A plain-English guess at why a test failed.
 *
 * @param code   stable id, for example INT_OVERFLOW (useful for counting common mistakes)
 * @param title  one short line
 * @param detail what to check and how to fix it
 * @param line   line in the user's code this points to, or null
 */
public record Hint(String code, String title, String detail, Integer line) {

    public static Hint of(String code, String title, String detail) {
        return new Hint(code, title, detail, null);
    }

    public Hint atLine(Integer line) {
        return new Hint(code, title, detail, line);
    }
}
