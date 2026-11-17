package io.github.sanket1035.javajudge.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RunControllerTest {

    @Autowired MockMvc mvc;

    private static final String HELLO =
            "public class Main { public static void main(String[] a) { System.out.println(\\\"hi\\\"); } }";

    @Test
    void runsJsonSubmission() throws Exception {
        String body = "{\"code\": \"" + HELLO + "\", \"testCases\": [{\"input\": \"\", \"expected\": \"hi\"}]}";

        mvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(1))
                .andExpect(jsonPath("$.results[0].verdict").value("PASSED"));
    }

    @Test
    void rejectsEmptyTestList() throws Exception {
        String body = "{\"code\": \"public class Main {}\", \"testCases\": []}";

        mvc.perform(post("/api/run").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void runsUploadedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "Main.java", "text/plain",
                "public class Main { public static void main(String[] a) { System.out.println(7); } }".getBytes());

        mvc.perform(multipart("/api/run/upload").file(file)
                        .param("tests", "[{\"input\":\"\",\"expected\":\"7\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(1));
    }

    @Test
    void rejectsNonJavaUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "x".getBytes());

        mvc.perform(multipart("/api/run/upload").file(file).param("tests", "[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsProblems() throws Exception {
        mvc.perform(get("/api/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].id").value("sum-of-two"));
    }

    @Test
    void unknownProblemIs404() throws Exception {
        mvc.perform(get("/api/problems/nope")).andExpect(status().isNotFound());
    }

    @Test
    void servesTheWebPage() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }
}
