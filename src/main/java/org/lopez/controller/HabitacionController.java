package org.lopez.controller;

import org.lopez.entity.Habitacion;
import org.lopez.repository.HabitacionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Habitaciones", description = "Gestión de habitaciones del hotel: consulta, creación, actualización y eliminación.")
@SecurityRequirement(name = "bearerAuth")
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

    // ── GET /api/habitaciones ──────────────────────────────────

    @GetMapping
    @Operation(summary = "Listar todas las habitaciones",
            description = "Retorna la lista completa de habitaciones del hotel. Opcionalmente se puede filtrar por estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de habitaciones obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT no proporcionado o inválido",
                    content = @Content(examples = @ExampleObject(value = """
                {"error": "Unauthorized"}""")))
    })
    public ResponseEntity<List<Habitacion>> listarTodas(
            @Parameter(description = "Filtrar por estado", example = "DISPONIBLE")
            @RequestParam(required = false) Habitacion.EstadoHabitacion estado) {

        List<Habitacion> habitaciones = estado != null
                ? habitacionRepository.findByEstado(estado)
                : habitacionRepository.findAll();

        return ResponseEntity.ok(habitaciones);
    }

    // ── GET /api/habitaciones/{id} ─────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Obtener habitación por ID",
            description = "Retorna los detalles de una habitación específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habitación encontrada"),
            @ApiResponse(responseCode = "404", description = "Habitación no encontrada",
                    content = @Content(examples = @ExampleObject(value = """
                {"error": "Habitación no encontrada"}""")))
    })
    public ResponseEntity<?> obtenerPorId(
            @Parameter(description = "ID de la habitación", example = "1")
            @PathVariable Long id) {

        Optional<Habitacion> found = habitacionRepository.findById(id);
        if (found.isPresent()) {
            return ResponseEntity.ok(found.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Habitación no encontrada"));
    }

    // ── POST /api/habitaciones ─────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nueva habitación",
            description = "Crea una nueva habitación. **Requiere rol ADMIN.**")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Habitación creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "El número de habitación ya existe",
                    content = @Content(examples = @ExampleObject(value = """
                {"error": "El número de habitación ya existe"}"""))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado — se requiere rol ADMIN")
    })
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

    // ── PUT /api/habitaciones/{id} ─────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar habitación",
            description = "Actualiza los datos de una habitación existente. **Requiere rol ADMIN.**")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habitación actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Habitación no encontrada")
    })
    public ResponseEntity<?> actualizar(
            @Parameter(description = "ID de la habitación", example = "1")
            @PathVariable Long id,
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

    // ── PATCH /api/habitaciones/{id}/estado ───────────────────

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de habitación",
            description = "Cambia el estado de una habitación (DISPONIBLE, OCUPADA, MANTENIMIENTO). **Requiere rol ADMIN.**")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "Habitación no encontrada")
    })
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @Parameter(description = "Nuevo estado", example = "MANTENIMIENTO")
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

    // ── DELETE /api/habitaciones/{id} ─────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar habitación",
            description = "Elimina una habitación del sistema. **Requiere rol ADMIN.**")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Habitación eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Habitación no encontrada")
    })
    public ResponseEntity<?> eliminar(
            @Parameter(description = "ID de la habitación", example = "1")
            @PathVariable Long id) {

        if (!habitacionRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Habitación no encontrada"));
        }
        habitacionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}