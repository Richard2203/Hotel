package org.lopez.controller;

import org.lopez.entity.Rol;
import org.lopez.entity.Usuario;
import org.lopez.repository.RolRepository;
import org.lopez.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Administración de usuarios y roles. Acceso exclusivo ADMIN.")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @GetMapping
    @Operation(summary = "Listar usuarios")
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        Optional<Usuario> found = usuarioRepository.findById(id);
        if (found.isPresent()) {
            return ResponseEntity.ok(found.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Usuario no encontrado"));
    }

    @PatchMapping("/{id}/toggle-activo")
    @Operation(summary = "Activar/desactivar usuario")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        Optional<Usuario> found = usuarioRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }
        Usuario u = found.get();
        u.setActivo(!u.isActivo());
        return ResponseEntity.ok(usuarioRepository.save(u));
    }

    @PatchMapping("/{id}/rol")
    @Operation(summary = "Asignar rol a usuario")
    public ResponseEntity<?> cambiarRol(@PathVariable Long id,
                                        @RequestParam String rol) {
        Optional<Usuario> userOpt = usuarioRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        }
        Optional<Rol> rolOpt = rolRepository.findByNombre(rol.toUpperCase());
        if (rolOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El rol no existe: " + rol));
        }
        Usuario u = userOpt.get();
        u.setRoles(Set.of(rolOpt.get()));
        return ResponseEntity.ok(usuarioRepository.save(u));
    }
}
