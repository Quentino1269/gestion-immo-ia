package com.immo.gestion.application.service;

import com.immo.gestion.domain.model.Bien;
import com.immo.gestion.domain.port.in.CreerBienUseCase;
import com.immo.gestion.domain.port.out.BienRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BienService implements CreerBienUseCase {

    private final BienRepository bienRepository;

    public BienService(BienRepository bienRepository) {
        this.bienRepository = bienRepository;
    }

    @Override
    public Bien creer(CreerBienCommand command) {
        bienRepository.rechercherParReference(command.reference()).ifPresent(b -> {
            throw new IllegalStateException("Un bien avec cette référence existe déjà : " + command.reference());
        });

        Bien bien = new Bien(
                UUID.randomUUID(),
                command.reference(),
                command.adresse(),
                command.type(),
                command.surface(),
                command.prixEnCentimes()
        );

        return bienRepository.sauvegarder(bien);
    }
}
