package org.lopez.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entidad Rol almacenada en base de datos.
 * Permite gestionar los roles de forma dinámica (ADMIN, USUARIO).
 */
@Data
@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    public Rol() {}

    public Rol(String nombre) {
        this.nombre = nombre;
    }
}
