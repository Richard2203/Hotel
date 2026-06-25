package org.lopez.controller;

import org.lopez.entity.Habitacion;
import org.lopez.entity.Reservacion;
import org.lopez.entity.Usuario;
import org.lopez.repository.HabitacionRepository;
import org.lopez.repository.ReservacionRepository;
import org.lopez.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservaciones")
@RequiredArgsConstructor
@Tag(name = "Reservaciones", description = "Gestión de reservaciones de habitaciones: creación, consulta y cambio de estado.")
@SecurityRequirement(name = "bearerAuth")
public class ReservacionController {

    private final ReservacionRepository reservacionRepository;
    private final HabitacionRepository habitacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Data
    static class ReservacionRequest {
        @NotNull
        private Long habitacionId;
        @NotNull @Future
        private LocalDate fechaEntrada;
        @NotNull @Future
        private LocalDate fechaSalida;
        private String observaciones;
    }

    // ── GET /api/reservaciones ─────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todas las reservaciones",
               description = "Retorna todas las reservaciones del sistema. **Requiere rol ADMIN.**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de reservaciones"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<List<Reservacion>> listarTodas(
            @Parameter(description = "Filtrar por estado", example = "CONFIRMADA")
            @RequestParam(required = false) Reservacion.EstadoReservacion estado) {

        List<Reservacion> lista = estado != null
                ? reservacionRepository.findByEstado(estado)
                : reservacionRepository.findAll();
        return ResponseEntity.ok(lista);
    }

    // ── GET /api/reservaciones/mis-reservaciones ───────────────

    @GetMapping("/mis-reservaciones")
    @Operation(summary = "Ver mis reservaciones",
               description = "Retorna las reservaciones del usuario autenticado.")
    @ApiResponse(responseCode = "200", description = "Lista de reservaciones del usuario")
    public ResponseEntity<List<Reservacion>> misReservaciones(
            @AuthenticationPrincipal UserDetails userDetails) {

        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(reservacionRepository.findByUsuarioId(usuario.getId()));
    }

    // ── GET /api/reservaciones/{id} ────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Obtener reservación por ID",
               description = "Retorna los detalles de una reservación específica.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reservación encontrada"),
        @ApiResponse(responseCode = "404", description = "Reservación no encontrada",
            content = @Content(examples = @ExampleObject(value = """
                {"error": "Reservación no encontrada"}""")))
    })
    public ResponseEntity<?> obtenerPorId(
            @Parameter(description = "ID de la reservación", example = "1")
            @PathVariable Long id) {

        return reservacionRepository.findById(id)
                .map(r -> (ResponseEntity<?>) ResponseEntity.ok(r))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Reservación no encontrada")));
    }

    // ── POST /api/reservaciones ────────────────────────────────

    @PostMapping
    @Operation(summary = "Crear reservación",
               description = "Crea una nueva reservación para el usuario autenticado. El total se calcula automáticamente según los días y el precio de la habitación.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reservación creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Fechas inválidas o habitación no disponible",
            content = @Content(examples = @ExampleObject(value = """
                {"error": "La habitación no está disponible"}""")))
    })
    public ResponseEntity<?> crear(
            @Valid @RequestBody ReservacionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!request.getFechaSalida().isAfter(request.getFechaEntrada())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "La fecha de salida debe ser posterior a la de entrada"));
        }

        Habitacion habitacion = habitacionRepository.findById(request.getHabitacionId())
                .orElse(null);
        if (habitacion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Habitación no encontrada"));
        }
        if (habitacion.getEstado() != Habitacion.EstadoHabitacion.DISPONIBLE) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "La habitación no está disponible"));
        }

        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        long noches = ChronoUnit.DAYS.between(request.getFechaEntrada(), request.getFechaSalida());
        BigDecimal total = habitacion.getPrecioPorNoche().multiply(BigDecimal.valueOf(noches));

        Reservacion r = new Reservacion();
        r.setHabitacion(habitacion);
        r.setUsuario(usuario);
        r.setFechaEntrada(request.getFechaEntrada());
        r.setFechaSalida(request.getFechaSalida());
        r.setTotal(total);
        r.setObservaciones(request.getObservaciones());

        // Marcar habitación como ocupada
        habitacion.setEstado(Habitacion.EstadoHabitacion.OCUPADA);
        habitacionRepository.save(habitacion);

        return ResponseEntity.status(HttpStatus.CREATED).body(reservacionRepository.save(r));
    }

    // ── PATCH /api/reservaciones/{id}/estado ──────────────────

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de reservación",
               description = "Cambia el estado de una reservación. Si se cancela, la habitación vuelve a DISPONIBLE. **Requiere rol ADMIN.**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado"),
        @ApiResponse(responseCode = "404", description = "Reservación no encontrada")
    })
    public ResponseEntity<?> cambiarEstado(
            @Parameter(description = "ID de la reservación", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevo estado", example = "CONFIRMADA")
            @RequestParam Reservacion.EstadoReservacion estado) {

        return reservacionRepository.findById(id).map(r -> {
            r.setEstado(estado);
            if (estado == Reservacion.EstadoReservacion.CANCELADA
                    || estado == Reservacion.EstadoReservacion.COMPLETADA) {
                r.getHabitacion().setEstado(Habitacion.EstadoHabitacion.DISPONIBLE);
                habitacionRepository.save(r.getHabitacion());
            }
            return (ResponseEntity<?>) ResponseEntity.ok(reservacionRepository.save(r));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Reservación no encontrada")));
    }
}
