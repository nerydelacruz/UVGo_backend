package com.UVGgo.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SesionRepository extends JpaRepository<Sesion, UUID> {}
