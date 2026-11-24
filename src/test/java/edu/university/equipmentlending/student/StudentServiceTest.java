package edu.university.equipmentlending.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.university.equipmentlending.shared.error.BusinessConflictException;
import edu.university.equipmentlending.shared.error.ResourceNotFoundException;
import edu.university.equipmentlending.student.dto.Student;
import edu.university.equipmentlending.student.dto.StudentWrite;
import edu.university.equipmentlending.student.persistence.StudentEntity;
import edu.university.equipmentlending.student.persistence.StudentRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockMakers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class StudentServiceTest {
    private StudentRepository repository;
    private PlatformTransactionManager transactions;
    private ValidatorFactory validation;
    private StudentService service;

    @BeforeEach
    void setUp() {
        repository = mock(StudentRepository.class, withSettings().mockMaker(MockMakers.PROXY));
        transactions = mock(PlatformTransactionManager.class, withSettings().mockMaker(MockMakers.PROXY));
        when(transactions.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        validation = Validation.buildDefaultValidatorFactory();
        service = new StudentService(repository, validation.getValidator(), transactions);
    }

    @AfterEach
    void closeValidation() { if (validation != null) validation.close(); }

    @Test
    void registrationTrimsNameNormalizesEmailAndCommitsBeforeReturning() {
        when(repository.saveAndFlush(any())).thenAnswer(call -> {
            StudentEntity student = call.getArgument(0);
            assertThat(student.getId()).isNull();
            assertThat(student.getName()).isEqualTo("Test Student One");
            assertThat(student.getEmail()).isEqualTo("borrower.one@example.test");
            ReflectionTestUtils.setField(student, "id", 41L);
            return student;
        });

        Student result = service.register(new StudentWrite("\u2003  Test Student One \t\u2003",
                "\u2003 Borrower.One@Example.test \u2003"));

        assertThat(result).isEqualTo(new Student(41, "Test Student One", "borrower.one@example.test"));
        var order = inOrder(repository, transactions);
        order.verify(transactions).getTransaction(argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        order.verify(repository).saveAndFlush(any());
        order.verify(transactions).commit(any());
        verifyNoMoreInteractions(repository, transactions);
    }

    @Test
    void emailNormalizationUsesRootLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            when(repository.saveAndFlush(any())).thenAnswer(call -> {
                StudentEntity student = call.getArgument(0);
                ReflectionTestUtils.setField(student, "id", 41L);
                return student;
            });
            assertThat(service.register(new StudentWrite("Test Student", " I@EXAMPLE.TEST ")).email())
                    .isEqualTo("i@example.test");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void retrievalReturnsOnlyTheStudentDtoInAReadOnlyTransaction() {
        when(repository.findById(41L)).thenReturn(Optional.of(student(41, "Stored Student", "stored@example.test")));

        assertThat(service.get(41)).isEqualTo(new Student(41, "Stored Student", "stored@example.test"));
        verify(transactions).getTransaction(argThat(TransactionDefinition::isReadOnly));
        verify(repository, never()).saveAndFlush(any());
        verify(transactions).commit(any());
    }

    @Test
    void updateReplacesBothEditableFieldsAndPreservesGeneratedId() {
        StudentEntity stored = student(41, "Original Name", "original@example.test");
        when(repository.findById(41L)).thenReturn(Optional.of(stored));
        when(repository.saveAndFlush(stored)).thenReturn(stored);

        assertThat(service.update(41, new StudentWrite(" Updated Name ", " NEW@Example.test ")))
                .isEqualTo(new Student(41, "Updated Name", "new@example.test"));
        assertThat(stored.getId()).isEqualTo(41L);
        verify(transactions).commit(any());
    }

    @Test
    void sameStudentCanKeepItsOwnNormalizedEmail() {
        StudentEntity stored = student(41, "Original Name", "borrower.one@example.test");
        when(repository.findById(41L)).thenReturn(Optional.of(stored));
        when(repository.saveAndFlush(stored)).thenReturn(stored);

        assertThat(service.update(41, new StudentWrite("Updated Name", " BORROWER.ONE@EXAMPLE.TEST ")))
                .isEqualTo(new Student(41, "Updated Name", "borrower.one@example.test"));
        verify(transactions).commit(any());
    }

    @ParameterizedTest
    @CsvSource({"get", "update"})
    void missingStudentReturnsTheDocumentedFailureWithoutWriting(String operation) {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            if (operation.equals("get")) service.get(99);
            else service.update(99, new StudentWrite("Updated Name", "new@example.test"));
        }).isInstanceOfSatisfying(ResourceNotFoundException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo("STUDENT_NOT_FOUND");
            assertThat(exception.getMessage()).isEqualTo("Student not found");
        });
        verify(repository, never()).saveAndFlush(any());
        verify(transactions).rollback(any());
        verify(transactions, never()).commit(any());
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void rejectedInputDoesNotStartATransactionOrTouchPersistence(StudentWrite input) {
        assertThatThrownBy(() -> service.register(input)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.update(41, input)).isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(repository, transactions);
    }

    static Stream<StudentWrite> invalidInputs() {
        return Stream.of(
                new StudentWrite(null, "one@example.test"),
                new StudentWrite("", "one@example.test"),
                new StudentWrite(" \t\r\n", "one@example.test"),
                new StudentWrite("\u2003", "one@example.test"),
                new StudentWrite(" \t\u2003\r\n", "one@example.test"),
                new StudentWrite("\u0000", "one@example.test"),
                new StudentWrite(" ".repeat(2) + "n".repeat(119), "one@example.test"),
                new StudentWrite("n".repeat(121), "one@example.test"),
                new StudentWrite("Test Student", null),
                new StudentWrite("Test Student", " \t "),
                new StudentWrite("Test Student", "not-an-email"),
                new StudentWrite("Test Student", "a b@example.test"),
                new StudentWrite("Test Student", "one@@example.test"),
                new StudentWrite("Test Student", " ".repeat(235) + "one@example.test" + "    "));
    }

    @ParameterizedTest
    @CsvSource({"register", "update"})
    void confirmedEmailUniquenessFailureRollsBackBeforeBecomingAConflict(String operation) {
        StudentEntity stored = student(41, "Original Name", "original@example.test");
        if (operation.equals("update")) when(repository.findById(41L)).thenReturn(Optional.of(stored));
        when(repository.saveAndFlush(any())).thenThrow(constraintFailure("23505", "student_email_key"));

        assertThatThrownBy(() -> {
            if (operation.equals("register")) service.register(new StudentWrite("New Name", "used@example.test"));
            else service.update(41, new StudentWrite("New Name", "used@example.test"));
        }).isInstanceOfSatisfying(BusinessConflictException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo("DUPLICATE_EMAIL");
            assertThat(exception.getMessage()).isEqualTo("Email already registered");
        });
        var order = inOrder(repository, transactions);
        order.verify(transactions).getTransaction(any());
        if (operation.equals("update")) order.verify(repository).findById(41L);
        order.verify(repository).saveAndFlush(any());
        order.verify(transactions).rollback(any());
        order.verifyNoMoreInteractions();
        verify(transactions, never()).commit(any());
    }

    @ParameterizedTest
    @MethodSource("unrelatedFailures")
    void unrelatedPersistenceFailuresKeepTheirClassification(DataIntegrityViolationException failure) {
        when(repository.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.register(new StudentWrite("Test Student", "one@example.test")))
                .isSameAs(failure);
        verify(transactions).rollback(any());
        verify(transactions, never()).commit(any());
        verify(repository).saveAndFlush(any());
        verifyNoMoreInteractions(repository);
    }

    static Stream<Arguments> unrelatedFailures() {
        return Stream.of(
                Arguments.of(constraintFailure("23505", "student_pkey")),
                Arguments.of(constraintFailure("23514", "student_email_key")),
                Arguments.of(constraintFailure("23503", "loan_student_id_fkey")),
                Arguments.of(constraintFailure("23505", null)),
                Arguments.of(new DataIntegrityViolationException("student_email_key 23505 in a message is not proof")));
    }

    @Test
    void commitFailureCannotReturnASuccessfulStudent() {
        when(repository.saveAndFlush(any())).thenReturn(student(41, "Test Student", "one@example.test"));
        IllegalStateException failure = new IllegalStateException("commit unavailable");
        doThrow(failure).when(transactions).commit(any());

        assertThatThrownBy(() -> service.register(new StudentWrite("Test Student", "one@example.test")))
                .isSameAs(failure);
    }

    static StudentEntity student(long id, String name, String email) {
        StudentEntity student = new StudentEntity(name, email);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    static DataIntegrityViolationException constraintFailure(String state, String constraint) {
        var cause = new org.hibernate.exception.ConstraintViolationException(
                "private database detail", new SQLException("private database detail", state), constraint);
        return new DataIntegrityViolationException("private persistence detail", cause);
    }
}
