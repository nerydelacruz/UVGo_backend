package com.UVGgo.backend.listas;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ListaMaterialesRepository extends JpaRepository<ListaMateriales, UUID> {
    Page<ListaMateriales> findByUsuarioIdOrderByCreadaDesc(UUID usuarioId, Pageable page);
    Optional<ListaMateriales> findByIdAndUsuarioId(UUID id, UUID usuarioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ListaMateriales l where l.id = :id and l.usuarioId = :usuarioId")
    Optional<ListaMateriales> bloquearPorPropietario(UUID id, UUID usuarioId);
}
