package io.github.sanket1035.javajudge.model;

import java.util.List;

/**
 * Everything the user sees after a submission.
 *
 * @param compileHints friendly explanations of javac errors (empty when it compiled)
 * @param summaryHints hints that look at all test cases together, for example "only the edge case fails"
 */
public record RunReport(
        boolean compiled,
        String compileError,
        List<Hint> compileHints,
        List<CaseResult> results,
        List<Hint> summaryHints,
        int passed,
        int total) {

    public static RunReport compileFailed(String error, List<Hint> hints, int total) {
        return new RunReport(false, error, hints, List.of(), List.of(), 0, total);
    }

    public static RunReport of(List<CaseResult> results, List<Hint> summaryHints) {
        int passed = (int) results.stream().filter(r -> r.verdict() == Verdict.PASSED).count();
        return new RunReport(true, null, List.of(), results, summaryHints, passed, results.size());
    }
}
