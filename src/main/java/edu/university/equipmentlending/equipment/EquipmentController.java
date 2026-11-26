package edu.university.equipmentlending.equipment;

import edu.university.equipmentlending.equipment.dto.EquipmentResponse;
import edu.university.equipmentlending.equipment.dto.EquipmentWrite;
import edu.university.equipmentlending.shared.error.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {
    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    public ResponseEntity<EquipmentResponse> create(
            @Valid @RequestBody EquipmentWrite request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.create(request));
    }

    @GetMapping
    public List<EquipmentResponse> list(
            @RequestParam(name = "available", required = false) String available) {
        return equipmentService.list(parseAvailable(available));
    }

    @PutMapping("/{id}")
    public EquipmentResponse update(
            @PathVariable("id") @Positive long id,
            @Valid @RequestBody EquipmentWrite request) {
        return equipmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static Boolean parseAvailable(String available) {
        if (available == null) return null;
        return switch (available) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request");
        };
    }
}
