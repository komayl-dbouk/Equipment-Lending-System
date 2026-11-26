package edu.university.equipmentlending.loan;

import edu.university.equipmentlending.loan.dto.Loan;
import edu.university.equipmentlending.loan.persistence.LoanQueryRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanQueryService {
    private final LoanQueryRepository repository;
    private final Clock clock;

    public LoanQueryService(LoanQueryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Loan> list(Long studentId, Long equipmentId) {
        return repository.findHistory(studentId, equipmentId);
    }

    @Transactional(readOnly = true)
    public List<Loan> overdue() {
        return repository.findOverdue(LocalDate.now(clock));
    }
}
