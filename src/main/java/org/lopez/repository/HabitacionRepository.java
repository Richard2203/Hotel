package org.lopez.repository;

import org.lopez.entity.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {
    Optional<Habitacion> findByNumero(String numero);
    List<Habitacion> findByEstado(Habitacion.EstadoHabitacion estado);
    List<Habitacion> findByTipo(Habitacion.TipoHabitacion tipo);
    boolean existsByNumero(String numero);
}
