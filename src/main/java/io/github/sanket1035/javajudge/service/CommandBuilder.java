package io.github.sanket1035.javajudge.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the shell commands for compiling and running, in LOCAL or DOCKER mode.
 * Kept separate from CodeRunner so the exact sandbox flags can be unit tested.
 */
public class CommandBuilder {

    /** Exit code of the "timeout" tool when it had to stop the program. */
    public static final int TIMEOUT_EXIT_CODE = 124;

    private final RunnerProperties p;

    public CommandBuilder(RunnerProperties p) {
        this.p = p;
    }

    public boolean isDocker() {
        return p.sandbox() == RunnerProperties.Sandbox.DOCKER;
    }

    public List<String> compile(Path dir, String className, String containerName) {
        List<String> javac = List.of("javac", "-encoding", "UTF-8", className + ".java");
        if (!isDocker()) {
            return javac;
        }
        // javac needs to write .class files, so the folder is mounted read-write for this step only
        List<String> cmd = dockerBase(dir, containerName, false);
        cmd.add(p.dockerImage());
        cmd.addAll(javac);
        return cmd;
    }

    public List<String> run(Path dir, String className, String containerName) {
        List<String> java = List.of("java", "-Xmx" + p.memoryMb() + "m", "-XX:+UseSerialGC",
                "-Xss64m", "-cp", ".", className);
        if (!isDocker()) {
            return java;
        }
        List<String> cmd = dockerBase(dir, containerName, true);
        cmd.add(p.dockerImage());
        // "timeout" inside the container, so container start-up time does not count against the program
        cmd.add("timeout");
        cmd.add(seconds(p.timeLimitMs()));
        cmd.addAll(java);
        return cmd;
    }

    /** Force-removes a container that is still running after the outer safety timeout. */
    public List<String> kill(String containerName) {
        return List.of("docker", "rm", "-f", containerName);
    }

    private List<String> dockerBase(Path dir, String name, boolean readOnlyCode) {
        // Container gets the JVM limit plus room for JVM overhead (metaspace, threads, code cache)
        int containerMb = p.memoryMb() * 2;
        List<String> cmd = new ArrayList<>(List.of(
                "docker", "run", "--rm", "-i",
                "--name", name,
                "--network", "none",                         // no internet, no calls to other services
                "--memory", containerMb + "m",
                "--memory-swap", containerMb + "m",          // no swap, so the memory cap is real
                "--cpus", "1",
                "--pids-limit", "64",                        // stops fork bombs and thread bombs
                "--read-only",                               // container filesystem cannot be changed
                "--tmpfs", "/tmp:rw,size=16m",
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",
                "--user", "65534:65534",                     // "nobody", never root
                "-v", dir.toAbsolutePath() + ":/code" + (readOnlyCode ? ":ro" : ""),
                "-w", "/code"));
        return cmd;
    }

    private static String seconds(long ms) {
        return String.format(java.util.Locale.ROOT, "%.1f", ms / 1000.0);
    }
}
