package io.github.sanket1035.javajudge.coach;

import io.github.sanket1035.javajudge.model.*;
import io.github.sanket1035.javajudge.service.CodeRunner;
import io.github.sanket1035.javajudge.service.RunnerProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Each test runs a real buggy program and checks the coach names the bug. */
class DebugCoachTest {

    private final CodeRunner runner = new CodeRunner(RunnerProperties.local(2000));

    private static String main(String body) {
        return "import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n"
                + "        Scanner sc = new Scanner(System.in);\n" + body + "\n    }\n}\n";
    }

    private CaseResult one(String code, String input, String expected) {
        RunReport r = runner.run(code, List.of(new TestCase(input, expected)));
        assertThat(r.compiled()).as(r.compileError()).isTrue();
        return r.results().get(0);
    }

    private static List<String> codes(CaseResult r) {
        return r.hints().stream().map(Hint::code).toList();
    }

    @Test
    void intOverflow() {
        String code = main("        int a = sc.nextInt(), b = sc.nextInt();\n        System.out.println(a * b);");
        CaseResult r = one(code, "100000 100000", "10000000000");
        assertThat(codes(r)).contains("INT_OVERFLOW");
    }

    @Test
    void scannerNextLineTrap() {
        String code = main("        int n = sc.nextInt();\n        String name = sc.nextLine();\n"
                + "        System.out.println(name + \" \" + n);");
        CaseResult r = one(code, "5\nSanket", "Sanket 5");
        assertThat(codes(r)).contains("SCANNER_NEXTLINE");
        assertThat(r.hints().stream().filter(h -> h.code().equals("SCANNER_NEXTLINE")).findFirst().get().line())
                .isEqualTo(6);
    }

    @Test
    void offByOneIndexPointsAtCrashLine() {
        String code = main("        int[] a = {1, 2, 3};\n        int s = 0;\n"
                + "        for (int i = 0; i <= a.length; i++) s += a[i];\n        System.out.println(s);");
        CaseResult r = one(code, "", "6");
        assertThat(r.verdict()).isEqualTo(Verdict.RUNTIME_ERROR);
        assertThat(r.hints().get(0).code()).isEqualTo("OFF_BY_ONE");
        assertThat(r.hints().get(0).line()).isEqualTo(7);
    }

    @Test
    void wrongCase() {
        CaseResult r = one(main("        System.out.println(\"Yes\");"), "", "YES");
        assertThat(codes(r)).containsExactly("CASE");
    }

    @Test
    void wrongSeparator() {
        CaseResult r = one(main("        System.out.println(\"1,2,3\");"), "", "1 2 3");
        assertThat(codes(r)).containsExactly("FORMAT");
    }

    @Test
    void inputPrompt() {
        String code = main("        System.out.print(\"Enter a number: \");\n        System.out.println(sc.nextInt() * 2);");
        CaseResult r = one(code, "4", "8");
        assertThat(codes(r)).contains("EXTRA_OUTPUT");
    }

    @Test
    void noOutput() {
        CaseResult r = one(main("        int n = sc.nextInt();\n        if (n > 100) System.out.println(n);"), "5", "5");
        assertThat(codes(r)).containsExactly("NO_OUTPUT");
    }

    @Test
    void infiniteWhileLoop() {
        String code = main("        int n = sc.nextInt();\n        int i = 0;\n"
                + "        while (i < n) { System.out.print(\"\"); }\n        System.out.println(n);");
        CaseResult r = one(code, "3", "3");
        assertThat(r.verdict()).isEqualTo(Verdict.TIME_LIMIT_EXCEEDED);
        assertThat(r.hints().get(0).code()).isEqualTo("INFINITE_LOOP");
        assertThat(r.hints().get(0).line()).isEqualTo(7);
    }

    @Test
    void showsFirstWrongLine() {
        CaseResult r = one(main("        System.out.println(\"a\\nb\\nX\\nd\");"), "", "a\nb\nc\nd");
        assertThat(r.diff()).isEqualTo(new OutputDiff(3, "c", "X"));
    }

    @Test
    void missingLastLine() {
        assertThat(DebugCoach.firstDiff("1\n2", "1\n2\n3")).isEqualTo(new OutputDiff(3, "3", null));
    }

    @Test
    void edgeCaseSummary() {
        String code = main("        int n = sc.nextInt();\n        int max = 0;\n"
                + "        for (int i = 0; i < n; i++) max = Math.max(max, sc.nextInt());\n        System.out.println(max);");
        RunReport r = runner.run(code, List.of(
                new TestCase("3\n4 9 2", "9"),
                new TestCase("4\n1 5 3 2", "5"),
                new TestCase("2\n-3 -7", "-3")));
        assertThat(r.passed()).isEqualTo(2);
        assertThat(r.summaryHints()).extracting(Hint::code).containsExactly("EDGE_CASE");
    }

    @Test
    void passedCaseHasNoHints() {
        CaseResult r = one(main("        System.out.println(1);"), "", "1");
        assertThat(r.hints()).isEmpty();
        assertThat(r.diff()).isNull();
    }

    @Test
    void explainsCompileErrors() {
        RunReport r = runner.run("public class Main {\n  public static void main(String[] a) {\n"
                + "    int x = 5\n    System.out.println(y);\n  }\n}\n", List.of(new TestCase("", "")));
        assertThat(r.compiled()).isFalse();
        assertThat(r.compileHints()).isNotEmpty();
        assertThat(r.compileHints().get(0).code()).isEqualTo("SYNTAX");
        assertThat(r.compileHints().get(0).line()).isEqualTo(3);
    }

    @Test
    void explainsUnknownSymbol() {
        List<Hint> hints = CompileErrorExplainer.explain(
                "Main.java:4: error: cannot find symbol\n    System.out.println(y);\n"
                        + "                       ^\n  symbol:   variable y\n  location: class Main\n1 error\n");
        assertThat(hints).hasSize(1);
        assertThat(hints.get(0).title()).contains("\"y\"");
        assertThat(hints.get(0).line()).isEqualTo(4);
    }
}
