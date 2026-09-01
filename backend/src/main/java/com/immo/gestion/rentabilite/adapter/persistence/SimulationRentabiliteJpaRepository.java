package com.immo.gestion.rentabilite.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationRentabiliteJpaRepository extends JpaRepository<SimulationRentabiliteEntity, UUID> {

    List<SimulationRentabiliteEntity> findByBienId(UUID bienId);
}
