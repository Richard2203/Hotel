package org.lopez.controller;

import org.lopez.entity.Habitacion;
import org.lopez.entity.Reservacion;
import org.lopez.entity.Usuario;
import org.lopez.repository.HabitacionRepository;
import org.lopez.repository.ReservacionRepository;
import org.lopez.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/reservaciones")
@RequiredArgsConstructor
@Tag(name = "Reservaciones", description = "Registro, consulta, edición y cancelación de reservaciones.")
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

    // ── LISTAR TODAS (ADMIN) ───────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todas las reservaciones (ADMIN)")
    public ResponseEntity<List<Reservacion>> listarTodas(
            @RequestParam(required = false) Reservacion.EstadoReservacion estado) {
        List<Reservacion> lista = estado != null
                ? reservacionRepository.findByEstado(estado)
                : reservacionRepository.findAll();
        return ResponseEntity.ok(lista);
    }

    // ── MIS RESERVACIONES ──────────────────────────────────────
    @GetMapping("/mis-reservaciones")
    @Operation(summary = "Ver mis reservaciones")
    public ResponseEntity<List<Reservacion>> misReservaciones(
            @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(reservacionRepository.findByUsuarioId(usuario.getId()));
    }

    // ── OBTENER POR ID ─────────────────────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Obtener reservación por ID")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        Optional<Reservacion> found = reservacionRepository.findById(id);
        if (found.isPresent()) {
            return ResponseEntity.ok(found.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Reservación no encontrada"));
    }

    // ── REGISTRAR RESERVACIÓN ──────────────────────────────────
    @PostMapping
    @Operation(summary = "Registrar reservación")
    public ResponseEntity<?> crear(@Valid @RequestBody ReservacionRequest request,
                                   @AuthenticationPrincipal UserDetails userDetails) {

        if (!request.getFechaSalida().isAfter(request.getFechaEntrada())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "La fecha de salida debe ser posterior a la de entrada"));
        }

        Optional<Habitacion> habOpt = habitacionRepository.findById(request.getHabitacionId());
        if (habOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Habitación no encontrada"));
        }
        Habitacion habitacion = habOpt.get();

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
        r.setEstado(Reservacion.EstadoReservacion.PENDIENTE);

        habitacion.setEstado(Habitacion.EstadoHabitacion.OCUPADA);
        habitacionRepository.save(habitacion);

        return ResponseEntity.status(HttpStatus.CREATED).body(reservacionRepository.save(r));
    }

    // ── EDITAR RESERVACIÓN ─────────────────────────────────────
    @PutMapping("/{id}")
    @Operation(summary = "Editar reservación (fechas y observaciones)")
    public ResponseEntity<?> editar(@PathVariable Long id,
                                    @Valid @RequestBody ReservacionRequest request,
                                    @AuthenticationPrincipal UserDetails userDetails) {

        Optional<Reservacion> found = reservacionRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Reservación no encontrada"));
        }
        Reservacion r = found.get();

        // El usuario solo puede editar sus propias reservaciones (a menos que sea admin)
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        boolean esAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!esAdmin && !r.getUsuario().getId().equals(usuario.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No puedes editar reservaciones de otros usuarios"));
        }

        if (!request.getFechaSalida().isAfter(request.getFechaEntrada())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "La fecha de salida debe ser posterior a la de entrada"));
        }

        r.setFechaEntrada(request.getFechaEntrada());
        r.setFechaSalida(request.getFechaSalida());
        r.setObservaciones(request.getObservaciones());

        long noches = ChronoUnit.DAYS.between(request.getFechaEntrada(), request.getFechaSalida());
        r.setTotal(r.getHabitacion().getPrecioPorNoche().multiply(BigDecimal.valueOf(noches)));

        return ResponseEntity.ok(reservacionRepository.save(r));
    }

    // ── CAMBIAR ESTADO (ADMIN) ─────────────────────────────────
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de reservación (ADMIN)")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @RequestParam Reservacion.EstadoReservacion estado) {
        Optional<Reservacion> found = reservacionRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Reservación no encontrada"));
        }
        Reservacion r = found.get();
        r.setEstado(estado);
        if (estado == Reservacion.EstadoReservacion.CANCELADA
                || estado == Reservacion.EstadoReservacion.COMPLETADA) {
            r.getHabitacion().setEstado(Habitacion.EstadoHabitacion.DISPONIBLE);
            habitacionRepository.save(r.getHabitacion());
        }
        return ResponseEntity.ok(reservacionRepository.save(r));
    }

    // ── CANCELAR RESERVACIÓN ───────────────────────────────────
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar reservación")
    public ResponseEntity<?> cancelar(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        Optional<Reservacion> found = reservacionRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Reservación no encontrada"));
        }
        Reservacion r = found.get();

        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        boolean esAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!esAdmin && !r.getUsuario().getId().equals(usuario.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No puedes cancelar reservaciones de otros usuarios"));
        }

        r.setEstado(Reservacion.EstadoReservacion.CANCELADA);
        r.getHabitacion().setEstado(Habitacion.EstadoHabitacion.DISPONIBLE);
        habitacionRepository.save(r.getHabitacion());
        reservacionRepository.save(r);

        return ResponseEntity.ok(Map.of("mensaje", "Reservación cancelada exitosamente"));
    }
}
