package io.github.sanket1035.javajudge.service;

import io.github.sanket1035.javajudge.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs real programs inside the Docker sandbox and checks that the limits hold.
 * Skipped automatically when Docker is not running on this machine.
 */
class DockerSandboxTest {

    private static final String IMAGE = "eclipse-temurin:17-jdk-alpine";

    private final CodeRunner runner = new CodeRunner(new RunnerProperties(
            2000, 30000, 128, 65_536, 20, RunnerProperties.Sandbox.DOCKER, IMAGE, null, 2));

    @BeforeAll
    static void needsDocker() throws Exception {
        boolean ok;
        try {
            Process p = new ProcessBuilder("docker", "image", "inspect", IMAGE)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            ok = p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            ok = false;
        }
        assumeTrue(ok, "Docker or the " + IMAGE + " image is not available, skipping sandbox tests");
    }

    private CaseResult runOne(String body, String expected) {
        String code = "import java.io.*; import java.net.*; import java.util.*;\n"
                + "public class Main { public static void main(String[] a) throws Exception { " + body + " } }";
        RunReport r = runner.run(code, List.of(new TestCase("", expected)));
        assertThat(r.compiled()).as("compile error: %s", r.compileError()).isTrue();
        return r.results().get(0);
    }

    @Test
    void normalProgramPasses() {
        assertThat(runOne("System.out.println(6 * 7);", "42").verdict()).isEqualTo(Verdict.PASSED);
    }

    @Test
    void infiniteLoopIsStopped() {
        assertThat(runOne("while (true) { }", "").verdict()).isEqualTo(Verdict.TIME_LIMIT_EXCEEDED);
    }

    @Test
    void networkIsBlocked() {
        CaseResult c = runOne("new Socket(\"1.1.1.1\", 80); System.out.println(\"connected\");", "connected");
        assertThat(c.verdict()).isEqualTo(Verdict.RUNTIME_ERROR);
    }

    @Test
    void cannotWriteFiles() {
        CaseResult c = runOne("new FileWriter(\"hack.txt\").close(); System.out.println(\"wrote\");", "wrote");
        assertThat(c.verdict()).isEqualTo(Verdict.RUNTIME_ERROR);
        assertThat(c.error()).contains("hack.txt");
    }

    @Test
    void memoryBombFails() {
        CaseResult c = runOne("List<long[]> l = new ArrayList<>(); while (true) l.add(new long[1_000_000]);", "");
        assertThat(c.verdict()).isEqualTo(Verdict.RUNTIME_ERROR);
        assertThat(c.error()).contains("OutOfMemoryError");
    }

    @Test
    void threadBombIsCapped() {
        CaseResult c = runOne("while (true) new Thread(() -> { try { Thread.sleep(60000); } catch (Exception e) { } }).start();", "");
        assertThat(c.verdict()).isIn(Verdict.RUNTIME_ERROR, Verdict.TIME_LIMIT_EXCEEDED);
    }
}
