package io.github.sanket1035.javajudge.service;

import io.github.sanket1035.javajudge.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/** Runs real javac and java, so each verdict is checked end to end. */
class CodeRunnerTest {

    private final CodeRunner runner = new CodeRunner(RunnerProperties.local(2000));

    private static final String ADD = """
            import java.util.Scanner;
            public class Main {
                public static void main(String[] args) {
                    Scanner sc = new Scanner(System.in);
                    System.out.println(sc.nextInt() + sc.nextInt());
                }
            }
            """;

    @Test
    void passesAndFailsPerTestCase() {
        RunReport r = runner.run(ADD, List.of(
                new TestCase("2 3", "5"),
                new TestCase("10 20", "31")));

        assertThat(r.compiled()).isTrue();
        assertThat(r.passed()).isEqualTo(1);
        assertThat(r.results().get(0).verdict()).isEqualTo(Verdict.PASSED);
        assertThat(r.results().get(1).verdict()).isEqualTo(Verdict.WRONG_ANSWER);
        assertThat(r.results().get(1).actual().trim()).isEqualTo("30");
    }

    @Test
    void reportsCompileError() {
        RunReport r = runner.run("public class Main { void f() { int x = } }",
                List.of(new TestCase("", "")));

        assertThat(r.compiled()).isFalse();
        assertThat(r.compileError()).contains("error");
    }

    @Test
    void reportsRuntimeError() {
        String code = """
                public class Main {
                    public static void main(String[] a) { int[] x = new int[2]; System.out.println(x[5]); }
                }
                """;
        CaseResult c = runner.run(code, List.of(new TestCase("", "0"))).results().get(0);

        assertThat(c.verdict()).isEqualTo(Verdict.RUNTIME_ERROR);
        assertThat(c.error()).contains("ArrayIndexOutOfBoundsException");
    }

    @Test
    void stopsInfiniteLoop() {
        String code = "public class Main { public static void main(String[] a) { while (true) { } } }";
        CaseResult c = runner.run(code, List.of(new TestCase("", ""))).results().get(0);

        assertThat(c.verdict()).isEqualTo(Verdict.TIME_LIMIT_EXCEEDED);
    }

    @Test
    void usesTheClassNameFromTheCode() {
        String code = "public class Solution { public static void main(String[] a) { System.out.println(\"hi\"); } }";
        RunReport r = runner.run(code, List.of(new TestCase("", "hi")));

        assertThat(r.passed()).isEqualTo(1);
    }

    @Test
    void rejectsCodeWithoutPublicClass() {
        assertThatThrownBy(() -> runner.run("class Main {}", List.of(new TestCase("", ""))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hidesServerEnvironmentVariables() {
        String code = """
                public class Main {
                    public static void main(String[] a) { System.out.println(System.getenv().keySet()); }
                }
                """;
        CaseResult c = runner.run(code, List.of(new TestCase("", "[PATH]"))).results().get(0);

        assertThat(c.verdict()).isEqualTo(Verdict.PASSED);
        assertThat(c.error()).isNull();
    }

    @Test
    void comparerIgnoresTrailingSpacesAndLineEndings() {
        assertThat(OutputComparer.matches("1 2 \r\n3\n\n", "1 2\n3")).isTrue();
        assertThat(OutputComparer.matches("1 2 3", "1 2\n3")).isFalse();
    }

    @Test
    void refusesWhenAllSlotsAreBusy() throws Exception {
        CodeRunner oneSlot = new CodeRunner(new RunnerProperties(3000, 15000, 128, 65_536, 20,
                RunnerProperties.Sandbox.LOCAL, null, null, 1));
        String slow = "public class Main { public static void main(String[] a) throws Exception { Thread.sleep(2500); } }";
        Thread busy = new Thread(() -> oneSlot.run(slow, List.of(new TestCase("", "")), 0));
        busy.start();
        Thread.sleep(300);
        assertThatThrownBy(() -> oneSlot.run(slow, List.of(new TestCase("", "")), 0))
                .isInstanceOf(ServerBusyException.class);
        busy.join();
    }
}
