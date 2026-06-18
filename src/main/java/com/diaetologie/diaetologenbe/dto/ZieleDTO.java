package com.diaetologie.diaetologenbe.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ZieleDTO {
    private UUID fallId;
    private String langfristigesZiel;
    private String interventionsziel;
    private BigDecimal zielGewicht;
    private BigDecimal zielFettmasse;
    private BigDecimal zielKalorien;
    private BigDecimal zielFettaufnahme;
    private LocalDate zielDatum;
    private String status;

    public ZieleDTO() {}

    public UUID getFallId() { return fallId; }
    public void setFallId(UUID v) { this.fallId = v; }
    public String getLangfristigesZiel() { return langfristigesZiel; }
    public void setLangfristigesZiel(String v) { this.langfristigesZiel = v; }
    public String getInterventionsziel() { return interventionsziel; }
    public void setInterventionsziel(String v) { this.interventionsziel = v; }
    public BigDecimal getZielGewicht() { return zielGewicht; }
    public void setZielGewicht(BigDecimal v) { this.zielGewicht = v; }
    public BigDecimal getZielFettmasse() { return zielFettmasse; }
    public void setZielFettmasse(BigDecimal v) { this.zielFettmasse = v; }
    public BigDecimal getZielKalorien() { return zielKalorien; }
    public void setZielKalorien(BigDecimal v) { this.zielKalorien = v; }
    public BigDecimal getZielFettaufnahme() { return zielFettaufnahme; }
    public void setZielFettaufnahme(BigDecimal v) { this.zielFettaufnahme = v; }
    public LocalDate getZielDatum() { return zielDatum; }
    public void setZielDatum(LocalDate v) { this.zielDatum = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}