package io.github.sanket1035.javajudge.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** JSON body for POST /api/run. */
public record RunRequest(
        @NotBlank @Size(max = 65_536) String code,
        @NotEmpty List<@Valid TestCase> testCases) {
}
