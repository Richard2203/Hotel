package org.lopez.controller;

import org.lopez.entity.Rol;
import org.lopez.entity.Usuario;
import org.lopez.repository.RolRepository;
import org.lopez.repository.UsuarioRepository;
import org.lopez.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro e inicio de sesión con JWT.")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Data
    static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    static class RegisterRequest {
        @NotBlank
        private String nombre;
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 6)
        private String password;
        // "ADMIN" o "USUARIO"; por defecto USUARIO
        private String rol = "USUARIO";
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Devuelve un token JWT válido por 24 horas.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String token = jwtUtil.generateToken(userDetails);
            Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElseThrow();

            Map<String, Object> resp = new HashMap<>();
            resp.put("token", token);
            resp.put("type", "Bearer");
            resp.put("email", usuario.getEmail());
            resp.put("nombre", usuario.getNombre());
            resp.put("roles", usuario.getRoles().stream().map(Rol::getNombre).toList());
            return ResponseEntity.ok(resp);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciales inválidas"));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea un usuario. Por defecto con rol USUARIO.")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El correo ya está registrado"));
        }

        String rolNombre = (request.getRol() == null || request.getRol().isBlank())
                ? "USUARIO" : request.getRol().toUpperCase();

        Optional<Rol> rolOpt = rolRepository.findByNombre(rolNombre);
        if (rolOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El rol especificado no existe: " + rolNombre));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        usuario.setRoles(Set.of(rolOpt.get()));

        usuarioRepository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("mensaje", "Usuario registrado exitosamente"));
    }
}
