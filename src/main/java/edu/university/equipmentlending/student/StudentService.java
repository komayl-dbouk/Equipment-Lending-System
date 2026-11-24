package edu.university.equipmentlending.student;

import edu.university.equipmentlending.shared.error.BusinessConflictException;
import edu.university.equipmentlending.shared.error.ResourceNotFoundException;
import edu.university.equipmentlending.student.dto.Student;
import edu.university.equipmentlending.student.dto.StudentWrite;
import edu.university.equipmentlending.student.persistence.StudentEntity;
import edu.university.equipmentlending.student.persistence.StudentRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.util.Locale;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StudentService {
    private final StudentRepository repository;
    private final Validator validator;
    private final TransactionTemplate writes;
    private final TransactionTemplate reads;

    public StudentService(StudentRepository repository, Validator validator,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.validator = validator;
        writes = new TransactionTemplate(transactionManager);
        // Complete rollback before classifying a conflict, even if a caller has a transaction.
        writes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        reads = new TransactionTemplate(transactionManager);
        reads.setReadOnly(true);
    }

    public Student register(StudentWrite input) {
        StudentWrite normalized = normalize(input);
        return write(() -> toDto(repository.saveAndFlush(
                new StudentEntity(normalized.name(), normalized.email()))));
    }

    public Student get(long id) {
        return reads.execute(status -> toDto(find(id)));
    }

    public Student update(long id, StudentWrite input) {
        StudentWrite normalized = normalize(input);
        return write(() -> {
            StudentEntity student = find(id);
            student.replaceDetails(normalized.name(), normalized.email());
            return toDto(repository.saveAndFlush(student));
        });
    }

    private StudentWrite normalize(StudentWrite input) {
        validate(input, Default.class);
        StudentWrite normalized = new StudentWrite(input.name().trim(),
                input.email().trim().toLowerCase(Locale.ROOT));
        validate(normalized, Default.class, StudentWrite.Normalized.class);
        return normalized;
    }

    private void validate(StudentWrite input, Class<?>... groups) {
        var violations = validator.validate(input, groups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private Student write(Supplier<Student> action) {
        try {
            return writes.execute(status -> action.get());
        } catch (DataIntegrityViolationException exception) {
            // The transaction has ended. Never query an aborted transaction or parse SQL messages.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && "23505".equals(violation.getSQLState())
                        && "student_email_key".equals(violation.getConstraintName())) {
                    throw new BusinessConflictException("DUPLICATE_EMAIL", "Email already registered");
                }
            }
            throw exception;
        }
    }

    private StudentEntity find(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("STUDENT_NOT_FOUND", "Student not found"));
    }

    private Student toDto(StudentEntity entity) {
        return new Student(entity.getId(), entity.getName(), entity.getEmail());
    }
}
