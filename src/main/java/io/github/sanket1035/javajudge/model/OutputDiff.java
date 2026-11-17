package io.github.sanket1035.javajudge.model;

/**
 * The first output line that differs from the expected output (1-based).
 * A null side means that output ended before this line.
 */
public record OutputDiff(int line, String expected, String actual) {
}
