package com.immo.gestion.utilisateur.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UtilisateurJpaRepository extends JpaRepository<UtilisateurEntity, UUID> {

    boolean existsByEmail(String email);
}
