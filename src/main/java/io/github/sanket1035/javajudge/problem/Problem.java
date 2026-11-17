package io.github.sanket1035.javajudge.problem;

import io.github.sanket1035.javajudge.model.TestCase;

import java.util.List;

public record Problem(
        String id,
        String title,
        String difficulty,
        String statement,
        String starterCode,
        List<TestCase> testCases) {
}
