package org.lopez.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "habitaciones")
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoHabitacion tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioPorNoche;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoHabitacion estado = EstadoHabitacion.DISPONIBLE;

    @Column(nullable = false)
    private Integer capacidad;

    public enum TipoHabitacion {
        SENCILLA, DOBLE, SUITE, PRESIDENCIAL
    }

    public enum EstadoHabitacion {
        DISPONIBLE, OCUPADA, MANTENIMIENTO
    }
}
