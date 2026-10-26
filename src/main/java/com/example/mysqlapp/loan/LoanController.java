package com.example.mysqlapp.loan;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private static final int MAX_LIMIT = 500;

    private final LoanRepository repo;

    public LoanController(LoanRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Loan> list(@RequestParam(required = false) String orgId,
                           @RequestParam(required = false) String userId,
                           @RequestParam(required = false) String status,
                           @RequestParam(defaultValue = "50") int limit,
                           @RequestParam(defaultValue = "0") int offset) {
        return repo.findAll(orgId, userId, status, Math.max(1, Math.min(limit, MAX_LIMIT)), Math.max(0, offset));
    }

    @GetMapping("/{id}")
    public Loan get(@PathVariable String id) {
        return repo.findById(id).orElseThrow(() -> notFound(id));
    }

    @PostMapping
    public ResponseEntity<Loan> create(@Valid @RequestBody Loan loan) {
        String id = repo.insert(loan);
        return ResponseEntity.created(URI.create("/api/loans/" + id)).body(get(id));
    }

    @PutMapping("/{id}")
    public Loan update(@PathVariable String id, @Valid @RequestBody Loan loan) {
        if (repo.update(id, loan) == 0) {
            throw notFound(id);
        }
        return get(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (repo.delete(id) == 0) {
            throw notFound(id);
        }
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<String> duplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("A loan with this id already exists");
    }

    private static ResponseStatusException notFound(String id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan " + id + " not found");
    }
}
