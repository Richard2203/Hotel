package org.lopez.config;

import org.lopez.entity.Rol;
import org.lopez.entity.Usuario;
import org.lopez.repository.RolRepository;
import org.lopez.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Inicializa los roles (ADMIN, USUARIO) y un usuario administrador
 * por defecto la primera vez que arranca la aplicación.
 *
 * Credenciales admin por defecto:
 *   email: admin@hotel.com
 *   password: admin123
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Crear roles si no existen
        Rol rolAdmin = rolRepository.findByNombre("ADMIN")
                .orElseGet(() -> rolRepository.save(new Rol("ADMIN")));
        Rol rolUsuario = rolRepository.findByNombre("USUARIO")
                .orElseGet(() -> rolRepository.save(new Rol("USUARIO")));

        // Crear usuario admin por defecto
        if (!usuarioRepository.existsByEmail("admin@hotel.com")) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setEmail("admin@hotel.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setActivo(true);
            admin.setRoles(Set.of(rolAdmin));
            usuarioRepository.save(admin);
            System.out.println(">>> Usuario admin creado: admin@hotel.com / admin123");
        }
    }
}
