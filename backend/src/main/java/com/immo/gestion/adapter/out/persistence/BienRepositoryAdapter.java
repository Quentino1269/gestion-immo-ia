package com.immo.gestion.adapter.out.persistence;

import com.immo.gestion.domain.model.Bien;
import com.immo.gestion.domain.port.out.BienRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class BienRepositoryAdapter implements BienRepository {

    private final BienJpaRepository jpaRepository;

    public BienRepositoryAdapter(BienJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Bien sauvegarder(Bien bien) {
        BienEntity saved = jpaRepository.save(toEntity(bien));
        return toDomain(saved);
    }

    @Override
    public Optional<Bien> rechercherParId(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Bien> rechercherParReference(String reference) {
        return jpaRepository.findByReference(reference).map(this::toDomain);
    }

    private BienEntity toEntity(Bien b) {
        return new BienEntity(b.id(), b.reference(), b.adresse(), b.type(), b.surface(), b.prixEnCentimes());
    }

    private Bien toDomain(BienEntity e) {
        return new Bien(e.getId(), e.getReference(), e.getAdresse(), e.getType(), e.getSurface(), e.getPrixEnCentimes());
    }
}
