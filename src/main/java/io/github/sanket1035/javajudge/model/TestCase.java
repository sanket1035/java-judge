package io.github.sanket1035.javajudge.model;

import jakarta.validation.constraints.NotNull;

/** One test: what to type into the program, and what it should print. */
public record TestCase(@NotNull String input, @NotNull String expected) {
}
