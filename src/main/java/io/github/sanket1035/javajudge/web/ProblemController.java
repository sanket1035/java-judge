package io.github.sanket1035.javajudge.web;

import io.github.sanket1035.javajudge.problem.Problem;
import io.github.sanket1035.javajudge.problem.ProblemCatalog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemCatalog catalog;

    public ProblemController(ProblemCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<Problem> list() {
        return catalog.all();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Problem> get(@PathVariable String id) {
        return ResponseEntity.of(catalog.find(id));
    }
}
