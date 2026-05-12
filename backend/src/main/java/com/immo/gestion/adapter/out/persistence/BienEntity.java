package com.immo.gestion.adapter.out.persistence;

import com.immo.gestion.domain.model.TypeBien;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "bien")
public class BienEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String adresse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeBien type;

    @Column(nullable = false)
    private double surface;

    @Column(name = "prix_centimes", nullable = false)
    private long prixEnCentimes;

    protected BienEntity() {}

    public BienEntity(UUID id, String reference, String adresse, TypeBien type, double surface, long prixEnCentimes) {
        this.id = id;
        this.reference = reference;
        this.adresse = adresse;
        this.type = type;
        this.surface = surface;
        this.prixEnCentimes = prixEnCentimes;
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getAdresse() { return adresse; }
    public TypeBien getType() { return type; }
    public double getSurface() { return surface; }
    public long getPrixEnCentimes() { return prixEnCentimes; }
}
