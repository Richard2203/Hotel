package org.lopez.controller;

import org.lopez.entity.Usuario;
import org.lopez.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema. **Acceso exclusivo para ADMIN.**")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping
    @Operation(summary = "Listar todos los usuarios",
               description = "Retorna la lista completa de usuarios registrados en el sistema.")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente")
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<?> obtener(
            @Parameter(description = "ID del usuario", example = "1")
            @PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(u -> (ResponseEntity<?>) ResponseEntity.ok(u))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado")));
    }

    @PatchMapping("/{id}/toggle-activo")
    @Operation(summary = "Activar/desactivar usuario",
               description = "Alterna el estado activo/inactivo de un usuario.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado del usuario actualizado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        return usuarioRepository.findById(id).map(u -> {
            u.setActivo(!u.isActivo());
            return (ResponseEntity<?>) ResponseEntity.ok(usuarioRepository.save(u));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Usuario no encontrado")));
    }

    @PatchMapping("/{id}/rol")
    @Operation(summary = "Cambiar rol de usuario",
               description = "Cambia el rol de un usuario entre USER y ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol actualizado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<?> cambiarRol(
            @PathVariable Long id,
            @Parameter(description = "Nuevo rol", example = "ADMIN")
            @RequestParam Usuario.Rol rol) {
        return usuarioRepository.findById(id).map(u -> {
            u.setRol(rol);
            return (ResponseEntity<?>) ResponseEntity.ok(usuarioRepository.save(u));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Usuario no encontrado")));
    }
}
