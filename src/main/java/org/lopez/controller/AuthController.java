package org.lopez.controller;

import org.lopez.entity.Usuario;
import org.lopez.repository.UsuarioRepository;
import org.lopez.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para registro e inicio de sesión. Los tokens JWT obtenidos aquí deben usarse en el botón Authorize (🔒) para acceder a endpoints protegidos.")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // ── DTOs internos ──────────────────────────────────────────

    @Data
    static class LoginRequest {
        @Schema(description = "Correo electrónico", example = "admin@hotel.com")
        @NotBlank @Email
        private String email;

        @Schema(description = "Contraseña", example = "Admin123!")
        @NotBlank
        private String password;
    }

    @Data
    static class RegisterRequest {
        @Schema(description = "Nombre completo", example = "Ricardo López García")
        @NotBlank
        private String nombre;

        @Schema(description = "Correo electrónico", example = "usuario@gmail.com")
        @NotBlank @Email
        private String email;

        @Schema(description = "Contraseña mínimo 6 caracteres", example = "Pass123!")
        @NotBlank @Size(min = 6)
        private String password;

        @Schema(description = "Rol del usuario", example = "USER",
                allowableValues = {"USER", "ADMIN"})
        private Usuario.Rol rol = Usuario.Rol.USER;
    }

    // ── POST /api/auth/login ───────────────────────────────────

    @PostMapping("/login")
    @Operation(
        summary = "Iniciar sesión",
        description = "Autentica al usuario con email y contraseña. Devuelve un **JWT Bearer Token** válido por 24 horas que debe usarse en el header `Authorization: Bearer <token>` para acceder a los endpoints protegidos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso — token JWT generado",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "token": "eyJhbGciOiJIUzI1NiJ9...",
                      "type": "Bearer",
                      "email": "admin@hotel.com",
                      "rol": "ADMIN"
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"error": "Credenciales inválidas"}""")))
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String token = jwtUtil.generateToken(userDetails);
            Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElseThrow();

            return ResponseEntity.ok(Map.of(
                "token", token,
                "type", "Bearer",
                "email", request.getEmail(),
                "rol", usuario.getRol().name()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciales inválidas"));
        }
    }

    // ── POST /api/auth/register ────────────────────────────────

    @PostMapping("/register")
    @Operation(
        summary = "Registrar nuevo usuario",
        description = "Crea una nueva cuenta de usuario en el sistema. Por defecto se asigna el rol `USER`. Para crear un `ADMIN` se debe especificar explícitamente."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"mensaje": "Usuario registrado exitosamente"}"""))),
        @ApiResponse(responseCode = "400", description = "El correo ya está registrado",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {"error": "El correo ya está registrado"}""")))
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El correo ya está registrado"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(request.getRol() != null ? request.getRol() : Usuario.Rol.USER);

        usuarioRepository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("mensaje", "Usuario registrado exitosamente"));
    }
}
