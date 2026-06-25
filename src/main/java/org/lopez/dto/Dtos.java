package org.lopez.dto;

import org.lopez.entity.Habitacion;
import org.lopez.entity.Reservacion;
import org.lopez.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

// ── AUTH ─────────────────────────────────────────────────────

@Data
class LoginRequest {
    @Schema(description = "Correo electrónico del usuario", example = "admin@hotel.com")
    @NotBlank @Email
    public String email;

    @Schema(description = "Contraseña del usuario", example = "Admin123!")
    @NotBlank
    public String password;
}

@Data
class RegisterRequest {
    @Schema(description = "Nombre completo", example = "Ricardo López García")
    @NotBlank
    public String nombre;

    @Schema(description = "Correo electrónico", example = "usuario@gmail.com")
    @NotBlank @Email
    public String email;

    @Schema(description = "Contraseña (mínimo 6 caracteres)", example = "Pass123!")
    @NotBlank @Size(min = 6)
    public String password;

    @Schema(description = "Rol del usuario", example = "USER")
    public Usuario.Rol rol = Usuario.Rol.USER;
}

@Data
class AuthResponse {
    @Schema(description = "Token JWT para autenticación")
    public String token;

    @Schema(description = "Tipo de token", example = "Bearer")
    public String type = "Bearer";

    @Schema(description = "Email del usuario autenticado")
    public String email;

    @Schema(description = "Rol del usuario")
    public String rol;
}

// ── HABITACION ───────────────────────────────────────────────

@Data
class HabitacionRequest {
    @Schema(description = "Número de habitación", example = "101")
    @NotBlank
    public String numero;

    @Schema(description = "Tipo de habitación", example = "DOBLE")
    @NotNull
    public Habitacion.TipoHabitacion tipo;

    @Schema(description = "Precio por noche en MXN", example = "1500.00")
    @NotNull @DecimalMin("0.01")
    public BigDecimal precioPorNoche;

    @Schema(description = "Descripción", example = "Vista al mar, cama king size")
    public String descripcion;

    @Schema(description = "Capacidad máxima", example = "2")
    @NotNull @Min(1)
    public Integer capacidad;
}

// ── RESERVACION ──────────────────────────────────────────────

@Data
class ReservacionRequest {
    @Schema(description = "ID de la habitación", example = "1")
    @NotNull
    public Long habitacionId;

    @Schema(description = "Fecha de entrada", example = "2026-07-01")
    @NotNull @Future
    public LocalDate fechaEntrada;

    @Schema(description = "Fecha de salida", example = "2026-07-05")
    @NotNull @Future
    public LocalDate fechaSalida;

    @Schema(description = "Observaciones adicionales", example = "Solicita cuna para bebé")
    public String observaciones;
}

@Data
class ReservacionStatusRequest {
    @Schema(description = "Nuevo estado de la reservación", example = "CONFIRMADA")
    @NotNull
    public Reservacion.EstadoReservacion estado;
}
