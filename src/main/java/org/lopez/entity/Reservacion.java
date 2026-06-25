package org.lopez.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reservaciones")
@Schema(description = "Entidad que representa una reservación de habitación")
public class Reservacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador único de la reservación", example = "1")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habitacion_id", nullable = false)
    @Schema(description = "Habitación reservada")
    private Habitacion habitacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @Schema(description = "Usuario que realiza la reservación")
    private Usuario usuario;

    @Column(nullable = false)
    @Schema(description = "Fecha de entrada", example = "2026-07-01")
    private LocalDate fechaEntrada;

    @Column(nullable = false)
    @Schema(description = "Fecha de salida", example = "2026-07-05")
    private LocalDate fechaSalida;

    @Column(nullable = false, precision = 10, scale = 2)
    @Schema(description = "Total de la reservación en MXN", example = "6000.00")
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Estado de la reservación", example = "CONFIRMADA")
    private EstadoReservacion estado = EstadoReservacion.PENDIENTE;

    @Column(nullable = false, updatable = false)
    @Schema(description = "Fecha y hora de creación de la reservación")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(length = 500)
    @Schema(description = "Observaciones adicionales", example = "Solicita cuna para bebé")
    private String observaciones;

    public enum EstadoReservacion {
        PENDIENTE, CONFIRMADA, CANCELADA, COMPLETADA
    }
}
