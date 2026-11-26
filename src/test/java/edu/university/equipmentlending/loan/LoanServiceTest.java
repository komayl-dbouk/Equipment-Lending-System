package edu.university.equipmentlending.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.university.equipmentlending.equipment.persistence.EquipmentEntity;
import edu.university.equipmentlending.equipment.persistence.EquipmentRepository;
import edu.university.equipmentlending.loan.dto.Loan;
import edu.university.equipmentlending.loan.dto.LoanWrite;
import edu.university.equipmentlending.loan.persistence.LoanEntity;
import edu.university.equipmentlending.loan.persistence.LoanRepository;
import edu.university.equipmentlending.shared.error.ApiException;
import edu.university.equipmentlending.shared.error.BusinessConflictException;
import edu.university.equipmentlending.student.persistence.StudentEntity;
import edu.university.equipmentlending.student.persistence.StudentRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockMakers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class LoanServiceTest {
    private StudentRepository students;
    private EquipmentRepository equipment;
    private LoanRepository loans;
    private PlatformTransactionManager transactions;
    private LoanService service;

    @BeforeEach
    void setUp() {
        students = mock(StudentRepository.class, org.mockito.Mockito.withSettings()
                .mockMaker(MockMakers.PROXY));
        equipment = mock(EquipmentRepository.class, org.mockito.Mockito.withSettings()
                .mockMaker(MockMakers.PROXY));
        loans = mock(LoanRepository.class, org.mockito.Mockito.withSettings()
                .mockMaker(MockMakers.PROXY));
        transactions = mock(PlatformTransactionManager.class, org.mockito.Mockito.withSettings()
                .mockMaker(MockMakers.PROXY));
        when(transactions.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        service = new LoanService(students, equipment, loans, transactions);
    }

    @Test
    void successfulBorrowLocksBeforeCheckingStateAndCommitsBeforeReturningDto() {
        StudentEntity student = student();
        EquipmentEntity item = item();
        when(students.findById(10L)).thenReturn(Optional.of(student));
        when(equipment.findByIdForUpdate(20L)).thenReturn(Optional.of(item));
        when(loans.saveAndFlush(any())).thenAnswer(call -> {
            LoanEntity loan = call.getArgument(0);
            ReflectionTestUtils.setField(loan, "id", 30L);
            return loan;
        });

        Loan result = service.borrow(request());

        assertThat(result).isEqualTo(new Loan(30L, 10L, 20L,
                LocalDate.of(2030, 1, 10), LocalDate.of(2030, 1, 13), null));
        var order = inOrder(transactions, students, equipment, loans);
        order.verify(transactions).getTransaction(org.mockito.ArgumentMatchers.argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        order.verify(students).findById(10L);
        order.verify(equipment).findByIdForUpdate(20L);
        order.verify(loans).existsByEquipmentIdAndReturnDateIsNull(20L);
        order.verify(loans).saveAndFlush(any());
        order.verify(transactions).commit(any());
    }

    @Test
    void aFailedCommitDoesNotReturnThePreparedDto() {
        readyToInsert();
        when(loans.saveAndFlush(any())).thenAnswer(call -> {
            LoanEntity loan = call.getArgument(0);
            ReflectionTestUtils.setField(loan, "id", 30L);
            return loan;
        });
        IllegalStateException failure = new IllegalStateException("commit failed");
        doThrow(failure).when(transactions).commit(any());

        assertThatThrownBy(() -> service.borrow(request())).isSameAs(failure);
        verify(loans).saveAndFlush(any());
    }

    @Test
    void confirmedActiveIndexViolationRollsBackBeforeConflictClassification() {
        readyToInsert();
        when(loans.saveAndFlush(any())).thenThrow(failure("23505", "ux_loan_active_equipment"));

        assertThatThrownBy(() -> service.borrow(request()))
                .isInstanceOfSatisfying(BusinessConflictException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("EQUIPMENT_UNAVAILABLE"));
        var order = inOrder(loans, transactions);
        order.verify(loans).saveAndFlush(any());
        order.verify(transactions).rollback(any());
        org.mockito.Mockito.verify(transactions, org.mockito.Mockito.never()).commit(any());
    }

    @ParameterizedTest
    @MethodSource("unrelatedFailures")
    void unrelatedDatabaseFailuresStayTechnical(DataIntegrityViolationException failure) {
        readyToInsert();
        when(loans.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.borrow(request())).isSameAs(failure);
        verify(transactions).rollback(any());
    }

    static Stream<Arguments> unrelatedFailures() {
        return Stream.of(
                Arguments.of(failure("23505", "loan_pkey")),
                Arguments.of(failure("23514", "ux_loan_active_equipment")),
                Arguments.of(failure("23505", null)),
                Arguments.of(new DataIntegrityViolationException(
                        "ux_loan_active_equipment 23505 in text is not proof")));
    }

    @Test
    void invalidDateOrderIsRejectedBeforeStartingATransaction() {
        LoanWrite invalid = new LoanWrite(10L, 20L,
                LocalDate.of(2030, 1, 10), LocalDate.of(2030, 1, 10));

        assertThatThrownBy(() -> service.borrow(invalid))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_LOAN_DATES"));
        org.mockito.Mockito.verifyNoInteractions(students, equipment, loans, transactions);
    }

    private void readyToInsert() {
        when(students.findById(10L)).thenReturn(Optional.of(student()));
        when(equipment.findByIdForUpdate(20L)).thenReturn(Optional.of(item()));
    }

    private static LoanWrite request() {
        return new LoanWrite(10L, 20L,
                LocalDate.of(2030, 1, 10), LocalDate.of(2030, 1, 13));
    }

    private static StudentEntity student() {
        StudentEntity student = new StudentEntity("Synthetic Borrower", "borrower@example.test");
        ReflectionTestUtils.setField(student, "id", 10L);
        return student;
    }

    private static EquipmentEntity item() {
        EquipmentEntity item = new EquipmentEntity("CAM-001", "Camera", "Camera", null);
        ReflectionTestUtils.setField(item, "id", 20L);
        return item;
    }

    private static DataIntegrityViolationException failure(String state, String constraint) {
        var cause = new org.hibernate.exception.ConstraintViolationException(
                "private detail", new SQLException("private detail", state), constraint);
        return new DataIntegrityViolationException("private detail", cause);
    }
}
