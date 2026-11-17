package io.github.sanket1035.javajudge.model;

public enum Verdict {
    PASSED,
    WRONG_ANSWER,        // ran fine, printed the wrong thing
    RUNTIME_ERROR,       // threw an exception or exited with a non-zero code
    TIME_LIMIT_EXCEEDED, // still running when the timer ran out (often an infinite loop)
    OUTPUT_LIMIT_EXCEEDED
}
