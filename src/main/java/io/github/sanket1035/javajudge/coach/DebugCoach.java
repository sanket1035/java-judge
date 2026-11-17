package io.github.sanket1035.javajudge.coach;

import io.github.sanket1035.javajudge.model.*;
import io.github.sanket1035.javajudge.service.OutputComparer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks at a failed test (verdict, output, error and the source code) and suggests the
 * likely bug, the way a senior would when you show them a red test.
 *
 * Every rule is a simple, explainable check. It can be wrong, so the UI calls them "hints".
 */
public final class DebugCoach {

    private static final Pattern INT_DECL = Pattern.compile("\\bint\\b(?!\\s*\\[)\\s+\\w+");
    private static final Pattern SCANNER_TOKEN_READ = Pattern.compile("\\.\\s*(nextInt|nextLong|nextDouble|next)\\s*\\(");
    private static final Pattern SCANNER_LINE_READ = Pattern.compile("\\.\\s*nextLine\\s*\\(");
    private static final Pattern LOOP_LE_LENGTH = Pattern.compile("<=\\s*\\w+\\s*\\.\\s*(length|size)\\b");
    private static final Pattern WHILE_LOOP = Pattern.compile("\\bwhile\\s*\\(");
    private static final Pattern NESTED_FOR = Pattern.compile("\\bfor\\s*\\(");
    private static final Pattern PROMPT_PRINT = Pattern.compile("System\\.out\\.print(ln)?\\s*\\(\\s*\"[^\"]*(enter|input|number|value)[^\"]*\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern STACK_LINE = Pattern.compile("at \\w+\\.[\\w$<>]+\\(\\w+\\.java:(\\d+)\\)");
    private static final Pattern EXCEPTION_NAME = Pattern.compile("Exception in thread \"main\" ([\\w.$]+)(?::\\s*(.*))?");
    private static final Pattern INDEX_MSG = Pattern.compile("Index (-?\\d+) out of bounds for length (\\d+)");
    private static final Pattern NUMBER = Pattern.compile("-?\\d+");
    private static final long INT_MAX = Integer.MAX_VALUE;

    private DebugCoach() { }

    // ---------- per test case ----------

    public static CaseResult coach(CaseResult r, String code) {
        if (r.verdict() == Verdict.PASSED) {
            return r;
        }
        SourceInfo src = new SourceInfo(code);
        OutputDiff diff = r.verdict() == Verdict.WRONG_ANSWER ? firstDiff(r.actual(), r.expected()) : null;

        List<Hint> hints = switch (r.verdict()) {
            case WRONG_ANSWER -> wrongAnswer(r, src);
            case RUNTIME_ERROR -> runtimeError(r, src);
            case TIME_LIMIT_EXCEEDED -> timeLimit(r, src);
            case OUTPUT_LIMIT_EXCEEDED -> List.of(Hint.of("OUTPUT_FLOOD", "Your program printed too much",
                    "Usually a print inside a loop that never ends, or printing inside the inner loop "
                            + "instead of after it.").atLine(src.firstLine(WHILE_LOOP)));
            default -> List.of();
        };
        return r.withCoaching(diff, hints.stream().limit(3).toList());
    }

    static OutputDiff firstDiff(String actual, String expected) {
        String[] a = OutputComparer.normalize(actual == null ? "" : actual).split("\n", -1);
        String[] e = OutputComparer.normalize(expected).split("\n", -1);
        if (a.length == 1 && a[0].isEmpty()) a = new String[0];
        if (e.length == 1 && e[0].isEmpty()) e = new String[0];
        int n = Math.max(a.length, e.length);
        for (int i = 0; i < n; i++) {
            String ai = i < a.length ? a[i] : null;
            String ei = i < e.length ? e[i] : null;
            if (ai == null || !ai.equals(ei)) {
                return new OutputDiff(i + 1, ei, ai);
            }
        }
        return null;
    }

    private static List<Hint> wrongAnswer(CaseResult r, SourceInfo src) {
        List<Hint> hints = new ArrayList<>();
        String actual = OutputComparer.normalize(r.actual());
        String expected = OutputComparer.normalize(r.expected());

        if (actual.isEmpty()) {
            hints.add(Hint.of("NO_OUTPUT", "Your program printed nothing",
                    "Check that the print runs on this input: it may be inside an if that is false, "
                            + "or after a return. Also check you used System.out, not System.err."));
            return hints;
        }
        if (actual.equalsIgnoreCase(expected)) {
            hints.add(Hint.of("CASE", "Right answer, wrong capital letters",
                    "Judges compare exactly. Print \"" + firstLineOf(expected) + "\", not \"" + firstLineOf(actual) + "\"."));
            return hints;
        }
        if (tokens(actual).equals(tokens(expected))) {
            hints.add(Hint.of("FORMAT", "Right values, wrong format",
                    "The numbers or words are correct but spaces, commas or line breaks differ. "
                            + "Check println vs print and the separator you add between values."));
            return hints;
        }
        if (actual.contains(expected) || src.contains(PROMPT_PRINT)) {
            Integer line = src.firstLine(PROMPT_PRINT);
            if (line != null || actual.length() > expected.length()) {
                hints.add(Hint.of("EXTRA_OUTPUT", "Extra text in your output",
                        "Remove prompts like \"Enter a number:\" and debug prints. The judge reads "
                                + "everything you print.").atLine(line));
            }
        }
        if (looksLikeOverflow(actual, expected) && src.contains(INT_DECL)) {
            hints.add(Hint.of("INT_OVERFLOW", "Number too big for int",
                    "int stops at 2,147,483,647 and then wraps to a negative number. Use long "
                            + "(and sc.nextLong()), or BigInteger for factorials and huge values.")
                    .atLine(src.firstLine(INT_DECL)));
        }
        if (offByOneNumbers(actual, expected)) {
            hints.add(Hint.of("OFF_BY_ONE", "Answer is off by one",
                    "Check loop bounds (< vs <=), whether you start at 0 or 1, and whether the "
                            + "first or last element is skipped.").atLine(src.firstLine(LOOP_LE_LENGTH)));
        }
        if (src.contains(SCANNER_TOKEN_READ) && src.contains(SCANNER_LINE_READ)) {
            hints.add(scannerTrap(src));
        }
        if (hints.isEmpty()) {
            hints.add(Hint.of("TRACE", "Dry-run this input by hand",
                    "Write the variables in a table and step through the code with input \""
                            + firstLineOf(r.input()) + "\". The first line where your value differs from "
                            + "what it should be is the bug."));
        }
        return hints;
    }

    private static List<Hint> runtimeError(CaseResult r, SourceInfo src) {
        String err = r.error() == null ? "" : r.error();
        Integer line = userLine(err);
        Matcher ex = EXCEPTION_NAME.matcher(err);
        String name = ex.find() ? ex.group(1) : "";
        String message = ex.groupCount() >= 2 && ex.group(2) != null ? ex.group(2) : "";
        String simple = name.substring(name.lastIndexOf('.') + 1);

        Hint h = switch (simple) {
            case "ArrayIndexOutOfBoundsException", "StringIndexOutOfBoundsException", "IndexOutOfBoundsException" -> {
                Matcher m = INDEX_MSG.matcher(message);
                if (m.find() && m.group(1).equals(m.group(2))) {
                    yield Hint.of("OFF_BY_ONE", "Index " + m.group(1) + " on something of length " + m.group(2),
                            "Valid indexes are 0 to " + (Integer.parseInt(m.group(2)) - 1)
                                    + ". Look for i <= length in a loop (should be <), or length() - i where "
                                    + "you meant length() - 1 - i.");
                }
                if (m.find(0) && m.group(1).startsWith("-")) {
                    yield Hint.of("NEGATIVE_INDEX", "Negative index " + m.group(1),
                            "Something like i - 1 when i is 0. Start the loop at 1 or check i > 0 first.");
                }
                yield Hint.of("INDEX", "Index outside the array or string",
                        "Check the size you created and every [i], [i + 1], charAt(i). "
                                + "Edge cases like n = 1 or an empty string often hit this.");
            }
            case "NullPointerException" -> Hint.of("NULL", "Used something that is null",
                    message.isBlank() ? "An object was never created (missing new) before you used it."
                            : "Java says: " + message + ". Create the object with new before using it.");
            case "ArithmeticException" -> Hint.of("DIVIDE_BY_ZERO", "Division by zero",
                    "Check the divisor before dividing, especially for inputs like 0 or n = 1.");
            case "NumberFormatException" -> Hint.of("PARSE", "Tried to turn text into a number and failed",
                    message.contains("\"\"") ? scannerTrap(src).detail()
                            : "Java says: " + message + ". Trim the text, and split the line before parsing.");
            case "NoSuchElementException" -> Hint.of("READ_PAST_INPUT", "Read more input than was given",
                    "Your code calls next/nextInt/nextLine more times than there are values. "
                            + "Check the loop count and the input format.");
            case "InputMismatchException" -> Hint.of("WRONG_READ_TYPE", "Read the wrong type",
                    "For example nextInt() on a word, or on a number too big for int (use nextLong()).");
            case "StackOverflowError" -> Hint.of("RECURSION", "Recursion never stops",
                    "Check the base case is reached for every input, including 0 and 1.");
            case "OutOfMemoryError" -> Hint.of("MEMORY", "Used more than the memory limit",
                    "A list that keeps growing in a loop, or an array sized by a huge number.");
            case "ConcurrentModificationException" -> Hint.of("MODIFY_WHILE_LOOP", "Changed a list while looping over it",
                    "Use an Iterator and iterator.remove(), or collect changes and apply them after the loop.");
            case "ClassCastException" -> Hint.of("CAST", "Wrong cast", "Java says: " + message);
            default -> err.contains("Killed") ? Hint.of("MEMORY", "Process was killed",
                    "Most likely it went over the memory limit.")
                    : Hint.of("RUNTIME", "The program crashed" + (simple.isEmpty() ? "" : " with " + simple),
                    "Read the first \"at Main...\" line in the error: that is where it happened.");
        };
        List<Hint> hints = new ArrayList<>();
        hints.add(line != null ? h.atLine(line) : h);
        if (line != null && !src.line(line).isEmpty()) {
            hints.add(Hint.of("CRASH_LINE", "Crashed on line " + line, src.line(line)).atLine(line));
        }
        return hints;
    }

    private static List<Hint> timeLimit(CaseResult r, SourceInfo src) {
        List<Hint> hints = new ArrayList<>();
        Integer whileLine = src.firstLine(WHILE_LOOP);
        if (whileLine != null) {
            hints.add(Hint.of("INFINITE_LOOP", "A loop probably never ends",
                    "Check the while condition can become false and the loop variable changes "
                            + "every time round (i++, n /= 10, left++...).").atLine(whileLine));
        }
        if (r.input() != null && r.input().length() > 2000) {
            hints.add(Hint.of("TOO_SLOW", "Correct idea, too slow for big input",
                    "A loop inside a loop is O(n^2). Try sorting, a HashMap or a running sum to make it O(n log n) or O(n)."));
        } else if (whileLine == null && src.contains(NESTED_FOR)) {
            hints.add(Hint.of("LOOP_BOUNDS", "Check the for loop counters",
                    "A for loop that changes the wrong variable (i++ in a j loop) never finishes."));
        }
        if (hints.isEmpty()) {
            hints.add(Hint.of("WAITING_FOR_INPUT", "Program waited too long",
                    "If you read input in a loop, make sure you stop after n values, not while(true)."));
        }
        return hints;
    }

    // ---------- whole report ----------

    /** Hints that need all the results, like "everything passes except the smallest input". */
    public static List<Hint> summarize(List<CaseResult> results) {
        List<CaseResult> failed = results.stream().filter(r -> r.verdict() != Verdict.PASSED).toList();
        if (failed.isEmpty() || failed.size() == results.size()) {
            return List.of();
        }
        boolean allEdge = failed.stream().allMatch(r -> isEdgeInput(r.input()));
        if (allEdge) {
            return List.of(Hint.of("EDGE_CASE", "Only the edge cases fail",
                    "Normal inputs pass, but tests like a single element, zero, negative numbers or "
                            + "all-equal values fail. Interviewers love asking about these: handle them first."));
        }
        return List.of();
    }

    // ---------- helpers ----------

    private static Hint scannerTrap(SourceInfo src) {
        return Hint.of("SCANNER_NEXTLINE", "Scanner nextLine() trap",
                "After nextInt() the rest of the line (just the Enter key) is still waiting, so the next "
                        + "nextLine() returns \"\". Call sc.nextLine() once to skip it, or read every line with "
                        + "nextLine() and parse with Integer.parseInt().")
                .atLine(src.firstLine(SCANNER_LINE_READ));
    }

    private static Integer userLine(String err) {
        Matcher m = STACK_LINE.matcher(err);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static boolean looksLikeOverflow(String actual, String expected) {
        List<Long> e = numbers(expected);
        List<Long> a = numbers(actual);
        boolean expectedBig = e.stream().anyMatch(x -> Math.abs(x) > INT_MAX);
        boolean signFlip = false;
        for (int i = 0; i < Math.min(a.size(), e.size()); i++) {
            if (e.get(i) > 0 && a.get(i) < 0) signFlip = true;
        }
        return expectedBig || signFlip;
    }

    private static boolean offByOneNumbers(String actual, String expected) {
        List<Long> a = numbers(actual);
        List<Long> e = numbers(expected);
        if (a.size() != 1 || e.size() != 1) return false;
        return Math.abs(a.get(0) - e.get(0)) == 1;
    }

    private static List<Long> numbers(String s) {
        List<Long> out = new ArrayList<>();
        Matcher m = NUMBER.matcher(s);
        while (m.find()) {
            String t = m.group();
            if (t.length() > 18) {
                out.add(Long.MAX_VALUE); // bigger than long: certainly bigger than int
                continue;
            }
            out.add(Long.parseLong(t));
        }
        return out;
    }

    private static List<String> tokens(String s) {
        return Arrays.stream(s.split("[\\s,;]+")).filter(t -> !t.isEmpty()).toList();
    }

    private static boolean isEdgeInput(String input) {
        if (input == null || input.isBlank()) return true;
        List<String> t = tokens(input);
        if (t.size() == 1) return true;
        // looks like "n\nvalues" with n <= 1
        if (t.get(0).matches("[01]")) return true;
        return t.stream().anyMatch(x -> x.equals("0") || x.startsWith("-"));
    }

    private static String firstLineOf(String s) {
        if (s == null) return "";
        String first = s.strip().lines().findFirst().orElse("");
        return first.length() > 60 ? first.substring(0, 60) + "..." : first;
    }
}
