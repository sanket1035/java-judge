package io.github.sanket1035.javajudge.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sanket1035.javajudge.model.RunReport;
import io.github.sanket1035.javajudge.service.CodeRunner;
import io.github.sanket1035.javajudge.service.RunnerProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Every bundled problem must have a reference solution that passes all its test cases. */
class ProblemCatalogTest {

    private static final ProblemCatalog CATALOG = load();
    private final CodeRunner runner = new CodeRunner(RunnerProperties.local(5000));

    private static ProblemCatalog load() {
        try {
            return new ProblemCatalog(new ObjectMapper());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static List<Problem> problems() {
        return CATALOG.all();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("problems")
    void referenceSolutionPasses(Problem problem) throws Exception {
        String code = new ClassPathResource("solutions/" + problem.id() + ".java")
                .getContentAsString(StandardCharsets.UTF_8);

        RunReport report = runner.run(code, problem.testCases());

        assertThat(report.compiled()).as(report.compileError()).isTrue();
        assertThat(report.passed()).as("%s: %s", problem.id(), report.results()).isEqualTo(report.total());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("problems")
    void starterCodeCompiles(Problem problem) {
        assertThat(runner.run(problem.starterCode(), problem.testCases()).compiled()).isTrue();
    }
}
