package com.immo.gestion.adapter.in.web;

import com.immo.gestion.domain.model.Bien;
import com.immo.gestion.domain.model.TypeBien;
import com.immo.gestion.domain.port.in.CreerBienUseCase;
import com.immo.gestion.domain.port.in.CreerBienUseCase.CreerBienCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/biens")
public class BienController {

    private final CreerBienUseCase creerBienUseCase;

    public BienController(CreerBienUseCase creerBienUseCase) {
        this.creerBienUseCase = creerBienUseCase;
    }

    @PostMapping
    public ResponseEntity<BienResponse> creer(@RequestBody CreerBienRequest request) {
        Bien bien = creerBienUseCase.creer(new CreerBienCommand(
                request.reference(),
                request.adresse(),
                request.type(),
                request.surface(),
                request.prixEnCentimes()
        ));
        return ResponseEntity.created(URI.create("/api/biens/" + bien.id()))
                .body(BienResponse.from(bien));
    }

    public record CreerBienRequest(
            @NotBlank String reference,
            @NotBlank String adresse,
            @NotNull TypeBien type,
            @Positive double surface,
            @PositiveOrZero long prixEnCentimes
    ) {}

    public record BienResponse(
            String id,
            String reference,
            String adresse,
            TypeBien type,
            double surface,
            long prixEnCentimes
    ) {
        static BienResponse from(Bien b) {
            return new BienResponse(
                    b.id().toString(),
                    b.reference(),
                    b.adresse(),
                    b.type(),
                    b.surface(),
                    b.prixEnCentimes()
            );
        }
    }
}
