package io.github.sanket1035.javajudge.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits and sandbox settings, read from application.properties (keys start with "runner.").
 *
 * @param sandbox     LOCAL runs javac/java directly (fast, for development only).
 *                    DOCKER runs every compile and every test in a fresh locked-down container.
 * @param dockerImage image with a JDK, used in DOCKER mode
 * @param workRoot    folder for per-run temp folders; empty means the system temp folder.
 *                    In Docker deploys it must be the same path on the host and in the app container.
 * @param maxConcurrentRuns how many submissions may run at the same time; extra ones get "busy"
 */
@ConfigurationProperties(prefix = "runner")
public record RunnerProperties(
        long timeLimitMs,
        long compileTimeLimitMs,
        int memoryMb,
        int maxOutputBytes,
        int maxTestCases,
        Sandbox sandbox,
        String dockerImage,
        String workRoot,
        int maxConcurrentRuns) {

    public enum Sandbox { LOCAL, DOCKER }

    /** Shortcut for tests and local runs. */
    public static RunnerProperties local(long timeLimitMs) {
        return new RunnerProperties(timeLimitMs, 15_000, 128, 65_536, 20, Sandbox.LOCAL, null, null, 4);
    }
}
