package io.github.sanket1035.javajudge.problem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/** Practice problems, loaded once from src/main/resources/problems.json. */
@Component
public class ProblemCatalog {

    private final List<Problem> problems;

    public ProblemCatalog(ObjectMapper json) throws IOException {
        try (InputStream in = new ClassPathResource("problems.json").getInputStream()) {
            this.problems = List.copyOf(json.readValue(in, new TypeReference<List<Problem>>() { }));
        }
    }

    public List<Problem> all() {
        return problems;
    }

    public Optional<Problem> find(String id) {
        return problems.stream().filter(p -> p.id().equals(id)).findFirst();
    }
}
