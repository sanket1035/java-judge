# Explaining JavaJudge in an interview

## In 90 seconds

1. **Problem.** When a student's code fails a test on HackerRank, all they see is "Wrong answer". They don't learn why.
2. **What I built.** A Spring Boot service that compiles Java, runs it against test cases in a Docker sandbox, and a debugging coach that names the likely bug: int overflow, off-by-one, the Scanner nextLine trap, infinite loops, edge cases.
3. **Hardest part.** Running untrusted code safely: timeouts that really kill the program, no network, memory and process limits, no access to server secrets, and not deadlocking on large output.
4. **Result.** 59 automated tests, including sandbox tests that try a network call, a file write, a memory bomb and a thread bomb, and check that each one fails.
5. **Next step.** Run test cases in parallel, and rate-limit per user.

## Questions to expect

**Why not run the code directly on the server?**
It could delete files, read secrets, call other services or use all the CPU. Every run gets its own container with no network, read-only files, capped memory, CPU and processes, as a non-root user.

**How do you detect an infinite loop?**
A timeout. `timeout 2.0 java ...` inside the container stops it, and exit code 124 means time limit exceeded. A second timer outside removes the container if Docker itself hangs.

**Why write output to files instead of reading the stream?**
A pipe has a small buffer. If the program fills it and nobody reads it, the program blocks forever. With files there's no buffer to fill, and I read at most 64 KB.

**Compile-time vs runtime error?**
Compile-time: javac rejects the code, such as a missing `;` or unknown variable. Runtime: it compiles but fails while running, such as `ArrayIndexOutOfBoundsException` or a wrong answer.

**What happens if two users submit at the same time?**
Spring handles each request on its own thread. Each run has its own temp folder and container, so they can't see each other. A `Semaphore` allows at most 2 runs at once on the server; others wait 5 seconds, then get 503.

**What is `-Xmx`, and what happens when a program goes over it?**
The maximum heap. The JVM throws `OutOfMemoryError`, the program exits with a non-zero code, and the verdict is runtime error. The container's own memory cap is a second line of defence.

**How does the int overflow hint work?**
If the expected output has a number bigger than `Integer.MAX_VALUE`, or the expected value is positive but the program printed a negative one, and the code declares an `int`, it suggests `long`.

**How does the Scanner hint work?**
`nextInt()` reads the number but leaves the newline. The next `nextLine()` returns an empty string. If the code calls both and the test fails, the coach explains it and points at the `nextLine()` line.

**Why rules and not an AI model for hints?**
Rules are fast, free, predictable and testable. Each rule has a unit test with a real buggy program. An LLM could add explanations later, but it could also be confidently wrong.

**How would you scale it to 1,000 users?**
Put submissions on a queue, run a pool of worker machines that take jobs, keep warm containers instead of starting a new one each time, and rate-limit per user.

**What is a record in Java and why did you use them?**
An immutable data class with fields, constructor, `equals`, `hashCode` and `toString` generated. Requests and results are plain data, so records fit and keep the code short.

**What is `@RestControllerAdvice`?**
A class that handles exceptions for all controllers. Mine turns bad input into a 400 with `{"error": "..."}` and "busy" into a 503, instead of a stack trace.
