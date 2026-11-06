package com.UVGgo.backend.piloto;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PilotoRepository extends JpaRepository<Piloto,UUID> {
    List<Piloto> findByActivoTrueOrderByNombre();
}
