package io.github.sanket1035.javajudge.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandBuilderTest {

    private static RunnerProperties docker() {
        return new RunnerProperties(2000, 15000, 128, 65_536, 20,
                RunnerProperties.Sandbox.DOCKER, "eclipse-temurin:17-jdk-alpine", null, 2);
    }

    @Test
    void localModeRunsJavaDirectly() {
        List<String> cmd = new CommandBuilder(RunnerProperties.local(2000)).run(Path.of("/tmp/x"), "Main", "n");
        assertThat(cmd.get(0)).isEqualTo("java");
        assertThat(cmd).contains("-Xmx128m", "Main");
    }

    @Test
    void dockerRunIsLockedDown() {
        List<String> cmd = new CommandBuilder(docker()).run(Path.of("/tmp/x"), "Main", "judge-1");
        String joined = String.join(" ", cmd);

        assertThat(joined).startsWith("docker run --rm -i --name judge-1");
        assertThat(joined).contains("--network none", "--memory 256m", "--memory-swap 256m",
                "--cpus 1", "--pids-limit 64", "--read-only", "--cap-drop ALL",
                "--security-opt no-new-privileges", "--user 65534:65534",
                "-v /tmp/x:/code:ro", "timeout 2.0 java -Xmx128m");
    }

    @Test
    void dockerCompileCanWriteClassFiles() {
        List<String> cmd = new CommandBuilder(docker()).compile(Path.of("/tmp/x"), "Main", "judge-2");
        assertThat(cmd).contains("/tmp/x:/code").doesNotContain("/tmp/x:/code:ro");
        assertThat(cmd.subList(cmd.size() - 4, cmd.size())).containsExactly("javac", "-encoding", "UTF-8", "Main.java");
    }
}
