package io.github.sanket1035.javajudge.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sanket1035.javajudge.model.RunReport;
import io.github.sanket1035.javajudge.model.RunRequest;
import io.github.sanket1035.javajudge.model.TestCase;
import io.github.sanket1035.javajudge.service.CodeRunner;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/run")
public class RunController {

    private final CodeRunner runner;
    private final ObjectMapper json;

    public RunController(CodeRunner runner, ObjectMapper json) {
        this.runner = runner;
        this.json = json;
    }

    /** Code pasted as text. Body: {"code": "...", "testCases": [{"input": "2 3", "expected": "5"}]} */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public RunReport runJson(@Valid @RequestBody RunRequest request) {
        return runner.run(request.code(), request.testCases());
    }

    /** A .java file upload plus the test cases as a JSON string in the "tests" field. */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RunReport runUpload(@RequestParam("file") MultipartFile file,
                               @RequestParam("tests") String testsJson) throws IOException {
        if (file.isEmpty() || !String.valueOf(file.getOriginalFilename()).endsWith(".java")) {
            throw new IllegalArgumentException("Upload one non-empty .java file");
        }
        List<TestCase> tests = json.readValue(testsJson, new TypeReference<>() { });
        if (tests.isEmpty()) {
            throw new IllegalArgumentException("Add at least one test case");
        }
        String code = new String(file.getBytes(), StandardCharsets.UTF_8);
        return runner.run(code, tests);
    }
}
