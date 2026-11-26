package edu.university.equipmentlending.loan.persistence;

import edu.university.equipmentlending.equipment.persistence.EquipmentEntity;
import edu.university.equipmentlending.student.persistence.StudentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "loan")
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private EquipmentEntity equipment;

    @Column(name = "borrowing_date", nullable = false)
    private LocalDate borrowingDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    protected LoanEntity() {
    }

    public LoanEntity(StudentEntity student, EquipmentEntity equipment,
            LocalDate borrowingDate, LocalDate dueDate) {
        this.student = student;
        this.equipment = equipment;
        this.borrowingDate = borrowingDate;
        this.dueDate = dueDate;
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return student.getId();
    }

    public Long getEquipmentId() {
        return equipment.getId();
    }

    public LocalDate getBorrowingDate() {
        return borrowingDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }
}
