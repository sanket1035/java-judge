package io.github.sanket1035.javajudge.model;

import java.util.List;

/** The result of running the program on one test case. */
public record CaseResult(
        int caseNumber,
        Verdict verdict,
        String input,
        String expected,
        String actual,
        String error,
        long timeMs,
        OutputDiff diff,
        List<Hint> hints) {

    public CaseResult(int caseNumber, Verdict verdict, String input, String expected,
                      String actual, String error, long timeMs) {
        this(caseNumber, verdict, input, expected, actual, error, timeMs, null, List.of());
    }

    public CaseResult withCoaching(OutputDiff diff, List<Hint> hints) {
        return new CaseResult(caseNumber, verdict, input, expected, actual, error, timeMs, diff, hints);
    }
}
