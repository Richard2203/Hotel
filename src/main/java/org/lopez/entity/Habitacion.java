package org.lopez.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "habitaciones")
@Schema(description = "Entidad que representa una habitación del hotel")
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador único de la habitación", example = "1")
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    @Schema(description = "Número de habitación", example = "101")
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Tipo de habitación", example = "DOBLE")
    private TipoHabitacion tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    @Schema(description = "Precio por noche en MXN", example = "1500.00")
    private BigDecimal precioPorNoche;

    @Column(length = 500)
    @Schema(description = "Descripción de la habitación", example = "Habitación con vista al mar, cama king size")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Estado actual de la habitación", example = "DISPONIBLE")
    private EstadoHabitacion estado = EstadoHabitacion.DISPONIBLE;

    @Column(nullable = false)
    @Schema(description = "Capacidad máxima de personas", example = "2")
    private Integer capacidad;

    public enum TipoHabitacion {
        SENCILLA, DOBLE, SUITE, PRESIDENCIAL
    }

    public enum EstadoHabitacion {
        DISPONIBLE, OCUPADA, MANTENIMIENTO
    }
}
