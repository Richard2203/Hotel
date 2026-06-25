package org.lopez.repository;

import org.lopez.entity.Reservacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReservacionRepository extends JpaRepository<Reservacion, Long> {
    List<Reservacion> findByUsuarioId(Long usuarioId);
    List<Reservacion> findByHabitacionId(Long habitacionId);
    List<Reservacion> findByEstado(Reservacion.EstadoReservacion estado);
}
