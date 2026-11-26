package edu.university.equipmentlending.loan;

import edu.university.equipmentlending.loan.dto.Loan;
import edu.university.equipmentlending.loan.dto.ReturnWrite;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanReturnController {
    private final LoanReturnService service;

    public LoanReturnController(LoanReturnService service) {
        this.service = service;
    }

    @PostMapping("/{id}/return")
    public Loan recordReturn(@PathVariable("id") @Positive long id,
            @Valid @RequestBody ReturnWrite request) {
        return service.recordReturn(id, request);
    }
}
