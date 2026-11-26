package edu.university.equipmentlending.loan;

import edu.university.equipmentlending.loan.dto.Loan;
import edu.university.equipmentlending.shared.error.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanQueryController {
    private final LoanQueryService service;

    public LoanQueryController(LoanQueryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Loan> list(
            @RequestParam(name = "studentId", required = false) String studentId,
            @RequestParam(name = "equipmentId", required = false) String equipmentId) {
        return service.list(parsePositiveId(studentId), parsePositiveId(equipmentId));
    }

    @GetMapping("/overdue")
    public List<Loan> overdue() {
        return service.overdue();
    }

    private static Long parsePositiveId(String value) {
        if (value == null) return null;
        if (!value.matches("[1-9][0-9]*")) throw invalidRequest();
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw invalidRequest();
        }
    }

    private static ApiException invalidRequest() {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request");
    }
}
