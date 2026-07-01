package com.immo.gestion.bien.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BienJpaRepository extends JpaRepository<BienEntity, UUID> {

    List<BienEntity> findByProprietaireInitialId(UUID proprietaireInitialId);

    List<BienEntity> findByBienParentId(UUID bienParentId);
}
