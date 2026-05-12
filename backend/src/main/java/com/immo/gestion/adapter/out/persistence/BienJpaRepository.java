package com.immo.gestion.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BienJpaRepository extends JpaRepository<BienEntity, UUID> {

    Optional<BienEntity> findByReference(String reference);
}
