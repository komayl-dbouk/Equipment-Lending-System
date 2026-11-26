package edu.university.equipmentlending.loan;

import edu.university.equipmentlending.loan.dto.Loan;
import edu.university.equipmentlending.loan.dto.LoanWrite;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Loan> borrow(@Valid @RequestBody LoanWrite request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.borrow(request));
    }
}
