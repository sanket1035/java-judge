package io.github.sanket1035.javajudge.service;

import io.github.sanket1035.javajudge.coach.CompileErrorExplainer;
import io.github.sanket1035.javajudge.coach.DebugCoach;
import io.github.sanket1035.javajudge.model.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Compiles one Java source file and runs it against each test case.
 *
 * Flow: save code in a fresh temp folder -> javac -> for each test: java with a time
 * and memory limit, input from a file, output to a file -> compare -> delete the folder.
 *
 * In LOCAL mode the code runs directly on this machine: development only.
 * In DOCKER mode every step runs in a throwaway container (see CommandBuilder).
 */
@Service
public class CodeRunner {

    private static final Pattern PUBLIC_CLASS = Pattern.compile("public\\s+(?:final\\s+)?class\\s+(\\w+)");

    /** Extra time on top of the limit before we stop waiting for Docker itself. */
    private static final long DOCKER_GRACE_MS = 10_000;

    private final RunnerProperties limits;
    private final CommandBuilder commands;
    /** Caps parallel runs so a burst of submissions can't use up the server's CPU and memory. */
    private final Semaphore slots;

    public CodeRunner(RunnerProperties limits) {
        this.limits = limits;
        this.commands = new CommandBuilder(limits);
        this.slots = new Semaphore(Math.max(1, limits.maxConcurrentRuns()));
    }

    public RunReport run(String code, List<TestCase> testCases) {
        return run(code, testCases, 5_000);
    }

    /** @param waitMs how long to wait for a free slot before answering "busy" */
    RunReport run(String code, List<TestCase> testCases, long waitMs) {
        if (testCases.size() > limits.maxTestCases()) {
            throw new IllegalArgumentException("At most " + limits.maxTestCases() + " test cases allowed");
        }
        String className = findClassName(code);

        boolean acquired;
        try {
            acquired = slots.tryAcquire(waitMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Run was interrupted", e);
        }
        if (!acquired) {
            throw new ServerBusyException();
        }
        try {
            return runInWorkDir(code, className, testCases);
        } finally {
            slots.release();
        }
    }

    private RunReport runInWorkDir(String code, String className, List<TestCase> testCases) {

        Path workDir = null;
        try {
            workDir = createWorkDir();
            Files.writeString(workDir.resolve(className + ".java"), code);

            String compileError = compile(workDir, className);
            if (compileError != null) {
                return RunReport.compileFailed(compileError, CompileErrorExplainer.explain(compileError),
                        testCases.size());
            }

            List<CaseResult> results = new ArrayList<>();
            for (int i = 0; i < testCases.size(); i++) {
                CaseResult raw = runOne(workDir, className, i + 1, testCases.get(i));
                results.add(DebugCoach.coach(raw, code));
            }
            return RunReport.of(results, DebugCoach.summarize(results));
        } catch (IOException e) {
            throw new IllegalStateException("Could not prepare the run folder", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Run was interrupted", e);
        } finally {
            deleteQuietly(workDir);
        }
    }

    /** Java needs the file name to match the public class, so read it from the code. */
    static String findClassName(String code) {
        Matcher m = PUBLIC_CLASS.matcher(code);
        if (m.find()) {
            return m.group(1);
        }
        throw new IllegalArgumentException("Code must contain a public class, for example: public class Main");
    }

    private Path createWorkDir() throws IOException {
        Path dir;
        if (limits.workRoot() == null || limits.workRoot().isBlank()) {
            dir = Files.createTempDirectory("run-");
        } else {
            Path root = Files.createDirectories(Path.of(limits.workRoot()));
            dir = Files.createTempDirectory(root, "run-");
        }
        if (commands.isDocker()) {
            // the container runs as user "nobody", which must be able to write .class files here
            Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxrwxrwx"));
        }
        return dir;
    }

    /** Returns null when compilation works, otherwise the javac error text. */
    private String compile(Path dir, String className) throws IOException, InterruptedException {
        Path log = dir.resolve("compile.log");
        String name = containerName();
        Process p = cleanEnv(new ProcessBuilder(commands.compile(dir, className, name)))
                .directory(dir.toFile())
                .redirectInput(ProcessBuilder.Redirect.from(emptyFile(dir)))
                .redirectErrorStream(true)          // javac writes errors to stderr; merge into one log
                .redirectOutput(log.toFile())
                .start();

        if (!p.waitFor(limits.compileTimeLimitMs(), TimeUnit.MILLISECONDS)) {
            stop(p, name);
            return "Compilation took too long";
        }
        return p.exitValue() == 0 ? null : readLimited(log);
    }

    private CaseResult runOne(Path dir, String className, int number, TestCase test)
            throws IOException, InterruptedException {
        Path in = dir.resolve("in-" + number + ".txt");
        Path out = dir.resolve("out-" + number + ".txt");
        Path err = dir.resolve("err-" + number + ".txt");
        Files.writeString(in, test.input());
        String name = containerName();

        // Files instead of pipes: a program that prints a lot cannot block on a full pipe.
        long start = System.nanoTime();
        Process p = cleanEnv(new ProcessBuilder(commands.run(dir, className, name)))
                .directory(dir.toFile())
                .redirectInput(in.toFile())
                .redirectOutput(out.toFile())
                .redirectError(err.toFile())
                .start();

        long wait = limits.timeLimitMs() + (commands.isDocker() ? DOCKER_GRACE_MS : 0);
        boolean finished = p.waitFor(wait, TimeUnit.MILLISECONDS);
        long timeMs = (System.nanoTime() - start) / 1_000_000;

        boolean timedOut = !finished
                || (commands.isDocker() && p.exitValue() == CommandBuilder.TIMEOUT_EXIT_CODE);
        if (!finished) {
            stop(p, name);
        }
        if (timedOut) {
            return result(number, Verdict.TIME_LIMIT_EXCEEDED, test, readLimited(out),
                    "Stopped after " + limits.timeLimitMs() + " ms", timeMs);
        }

        if (Files.size(out) > limits.maxOutputBytes()) {
            return result(number, Verdict.OUTPUT_LIMIT_EXCEEDED, test, readLimited(out),
                    "Printed more than " + limits.maxOutputBytes() + " bytes", timeMs);
        }

        String actual = readLimited(out);
        String error = readLimited(err);

        if (p.exitValue() != 0) {
            if (error.isBlank() && p.exitValue() == 137) {
                error = "Killed: probably went over the memory limit";
            }
            return result(number, Verdict.RUNTIME_ERROR, test, actual, error, timeMs);
        }
        Verdict verdict = OutputComparer.matches(actual, test.expected())
                ? Verdict.PASSED : Verdict.WRONG_ANSWER;
        return result(number, verdict, test, actual, error.isEmpty() ? null : error, timeMs);
    }

    /** Kills the process, and in Docker mode also the container (killing the client alone leaves it running). */
    private void stop(Process p, String containerName) throws InterruptedException, IOException {
        p.destroyForcibly().waitFor();
        if (commands.isDocker()) {
            new ProcessBuilder(commands.kill(containerName))
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor(10, TimeUnit.SECONDS);
        }
    }

    private static String containerName() {
        return "judge-" + UUID.randomUUID();
    }

    private static java.io.File emptyFile(Path dir) throws IOException {
        Path empty = dir.resolve("empty.txt");
        if (!Files.exists(empty)) {
            Files.createFile(empty);
        }
        return empty.toFile();
    }

    /**
     * A child process inherits the server's environment variables by default. Keep only PATH
     * (and DOCKER_HOST if set), so submitted code can't read secrets from System.getenv().
     */
    private static ProcessBuilder cleanEnv(ProcessBuilder pb) {
        String path = System.getenv("PATH");
        String dockerHost = System.getenv("DOCKER_HOST");
        pb.environment().clear();
        if (path != null) {
            pb.environment().put("PATH", path);
        }
        if (dockerHost != null) {
            pb.environment().put("DOCKER_HOST", dockerHost); // read by the docker client only, never passed into the container
        }
        return pb;
    }

    private static CaseResult result(int n, Verdict v, TestCase t, String actual, String error, long ms) {
        return new CaseResult(n, v, t.input(), t.expected(), actual, error, ms);
    }

    /** Reads at most maxOutputBytes, so a huge output cannot fill the server's memory. */
    private String readLimited(Path file) throws IOException {
        if (!Files.exists(file)) {
            return "";
        }
        try (InputStream s = Files.newInputStream(file)) {
            return new String(s.readNBytes(limits.maxOutputBytes()), StandardCharsets.UTF_8);
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // temp folder cleanup is best effort
        }
    }
}
