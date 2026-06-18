package com.diaetologie.diaetologenbe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ki_entscheidung")
public class KiEntscheidung {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fall_id")
    private Patientenfall fall;

    @Column(name = "typ")
    private String typ; // "assessment" oder "monitoring"

    @Column(name = "eingabe_json", columnDefinition = "TEXT")
    private String eingabeJson;

    @Column(name = "ausgabe_json", columnDefinition = "TEXT")
    private String ausgabeJson;

    @Column(name = "erstellt_am")
    private LocalDateTime erstelltAm;

    public KiEntscheidung() {}

    @PrePersist
    void setzeDatum() {
        this.erstelltAm = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public Patientenfall getFall() { return fall; }
    public void setFall(Patientenfall v) { this.fall = v; }
    public String getTyp() { return typ; }
    public void setTyp(String v) { this.typ = v; }
    public String getEingabeJson() { return eingabeJson; }
    public void setEingabeJson(String v) { this.eingabeJson = v; }
    public String getAusgabeJson() { return ausgabeJson; }
    public void setAusgabeJson(String v) { this.ausgabeJson = v; }
    public LocalDateTime getErstelltAm() { return erstelltAm; }
}