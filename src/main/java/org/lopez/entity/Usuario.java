package org.lopez.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "usuarios")
@Schema(description = "Entidad que representa un usuario del sistema")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador único del usuario", example = "1")
    private Long id;

    @Column(nullable = false, length = 100)
    @Schema(description = "Nombre completo del usuario", example = "Ricardo López García")
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    @Schema(description = "Correo electrónico (usado como username)", example = "usuario@gmail.com")
    private String email;

    @Column(nullable = false)
    @Schema(description = "Contraseña encriptada con BCrypt")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Rol del usuario en el sistema", example = "USER")
    private Rol rol = Rol.USER;

    @Column(nullable = false)
    @Schema(description = "Indica si la cuenta está activa", example = "true")
    private boolean activo = true;

    public enum Rol {
        USER, ADMIN
    }
}
