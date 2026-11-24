package edu.university.equipmentlending.student;

import edu.university.equipmentlending.student.dto.Student;
import edu.university.equipmentlending.student.dto.StudentWrite;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService service;

    public StudentController(StudentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Student> register(@Valid @RequestBody StudentWrite input) {
        Student student = service.register(input);
        return ResponseEntity.created(URI.create("/api/students/" + student.id())).body(student);
    }

    @GetMapping("/{id}")
    public Student get(@PathVariable("id") @Positive long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public Student update(@PathVariable("id") @Positive long id,
            @Valid @RequestBody StudentWrite input) {
        return service.update(id, input);
    }
}
