package org.lopez.controller;

import org.lopez.entity.Habitacion;
import org.lopez.repository.HabitacionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/habitaciones")
@RequiredArgsConstructor
@Tag(name = "Habitaciones", description = "Gestión de habitaciones del hotel.")
public class HabitacionController {

    private final HabitacionRepository habitacionRepository;

    @Data
    static class HabitacionRequest {
        @NotBlank
        private String numero;
        @NotNull
        private Habitacion.TipoHabitacion tipo;
        @NotNull @DecimalMin("0.01")
        private BigDecimal precioPorNoche;
        private String descripcion;
        @NotNull @Min(1)
        private Integer capacidad;
    }

    @GetMapping
    @Operation(summary = "Listar habitaciones")
    public ResponseEntity<List<Habitacion>> listarTodas(
            @RequestParam(required = false) Habitacion.EstadoHabitacion estado) {
        List<Habitacion> habitaciones = estado != null
                ? habitacionRepository.findByEstado(estado)
                : habitacionRepository.findAll();
        return ResponseEntity.ok(habitaciones);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener habitación por ID")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        Optional<Habitacion> found = habitacionRepository.findById(id);
        if (found.isPresent()) {
            return ResponseEntity.ok(found.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Habitación no encontrada"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear habitación (ADMIN)")
    public ResponseEntity<?> crear(@Valid @RequestBody HabitacionRequest request) {
        if (habitacionRepository.existsByNumero(request.getNumero())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El número de habitación ya existe"));
        }
        Habitacion h = new Habitacion();
        h.setNumero(request.getNumero());
        h.setTipo(request.getTipo());
        h.setPrecioPorNoche(request.getPrecioPorNoche());
        h.setDescripcion(request.getDescripcion());
        h.setCapacidad(request.getCapacidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(habitacionRepository.save(h));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar habitación (ADMIN)")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @Valid @RequestBody HabitacionRequest request) {
        Optional<Habitacion> found = habitacionRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Habitación no encontrada"));
        }
        Habitacion h = found.get();
        h.setNumero(request.getNumero());
        h.setTipo(request.getTipo());
        h.setPrecioPorNoche(request.getPrecioPorNoche());
        h.setDescripcion(request.getDescripcion());
        h.setCapacidad(request.getCapacidad());
        return ResponseEntity.ok(habitacionRepository.save(h));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de habitación (ADMIN)")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @RequestParam Habitacion.EstadoHabitacion estado) {
        Optional<Habitacion> found = habitacionRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Habitación no encontrada"));
        }
        Habitacion h = found.get();
        h.setEstado(estado);
        return ResponseEntity.ok(habitacionRepository.save(h));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar habitación (ADMIN)")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!habitacionRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Habitación no encontrada"));
        }
        habitacionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
