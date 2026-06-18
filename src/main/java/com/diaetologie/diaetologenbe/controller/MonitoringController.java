package com.diaetologie.diaetologenbe.controller;

import com.diaetologie.diaetologenbe.entity.Monitoring;
import com.diaetologie.diaetologenbe.entity.Patientenfall;
import com.diaetologie.diaetologenbe.entity.Ziele;
import com.diaetologie.diaetologenbe.respository.MonitoringRepository;
import com.diaetologie.diaetologenbe.respository.PatientenfallRepository;
import com.diaetologie.diaetologenbe.respository.ZieleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final MonitoringRepository repo;
    private final PatientenfallRepository fallRepo;
    private final ZieleRepository zieleRepo;

    public MonitoringController(MonitoringRepository repo,
                                PatientenfallRepository fallRepo,
                                ZieleRepository zieleRepo) {
        this.repo = repo;
        this.fallRepo = fallRepo;
        this.zieleRepo = zieleRepo;
    }

    // GET all monitoring entries for a fall, newest first
    @GetMapping("/fall/{fallId}")
    public ResponseEntity<?> perFall(@PathVariable UUID fallId) {
        List<Monitoring> list = repo.findByFallIdOrderByDatumDesc(fallId);
        return ResponseEntity.ok(list);
    }

    // POST new monitoring entry
    @PostMapping
    public ResponseEntity<?> speichern(@RequestBody Map<String, Object> body) {
        try {
            String fallIdStr = (String) body.get("fallId");
            UUID fallId = UUID.fromString(fallIdStr);
            Patientenfall fall = fallRepo.findById(fallId).orElseThrow();

            Monitoring m = new Monitoring();
            m.setFall(fall);
            m.setDatum(body.get("datum") != null
                    ? LocalDate.parse((String) body.get("datum"))
                    : LocalDate.now());
            m.setGewicht(toBD(body.get("gewicht")));
            m.setFettmasse(toBD(body.get("fettmasse")));
            m.setKalorienaufnahme(toBD(body.get("kalorienaufnahme")));
            m.setFettaufnahme(toBD(body.get("fettaufnahme")));
            m.setZielerreichung((String) body.get("zielerreichung"));
            m.setFoerderndefaktoren((String) body.get("foerderndefaktoren"));
            m.setHemmendefaktoren((String) body.get("hemmendefaktoren"));
            m.setNotizen((String) body.get("notizen"));

            Monitoring saved = repo.save(m);

            // Build enriched response with delta to goal
            return ResponseEntity.ok(buildEnrichedResponse(saved, fallId));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("fehler", e.getMessage()));
        }
    }

    // GET enriched summary: latest values + delta to goal
    @GetMapping("/fall/{fallId}/zusammenfassung")
    public ResponseEntity<?> zusammenfassung(@PathVariable UUID fallId) {
        List<Monitoring> list = repo.findByFallIdOrderByDatumDesc(fallId);
        List<Ziele> ziele = zieleRepo.findByFallId(fallId);
        Ziele ziel = ziele.isEmpty() ? null : ziele.get(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("anzahlMessungen", list.size());

        if (!list.isEmpty()) {
            Monitoring latest = list.get(0);
            Monitoring first = list.get(list.size() - 1);

            result.put("letzteMessung", latest.getDatum());
            result.put("aktuellesGewicht", latest.getGewicht());
            result.put("aktuelleFettmasse", latest.getFettmasse());
            result.put("startGewicht", first.getGewicht());

            // Weight change from start
            if (latest.getGewicht() != null && first.getGewicht() != null) {
                double delta = latest.getGewicht().doubleValue()
                        - first.getGewicht().doubleValue();
                result.put("gewichtsDelta", Math.round(delta * 10.0) / 10.0);
            }

            // Delta to goal
            if (ziel != null) {
                result.put("zielGewicht", ziel.getZielGewicht());
                result.put("zielFettmasse", ziel.getZielFettmasse());
                result.put("zielDatum", ziel.getZielDatum());
                result.put("langfristigesZiel", ziel.getLangfristigesZiel());
                result.put("interventionsziel", ziel.getInterventionsziel());

                if (ziel.getZielGewicht() != null && latest.getGewicht() != null) {
                    double deltaZiel = latest.getGewicht().doubleValue()
                            - ziel.getZielGewicht().doubleValue();
                    result.put("deltaZuZielGewicht",
                            Math.round(deltaZiel * 10.0) / 10.0);

                    // Percent progress toward goal
                    if (first.getGewicht() != null) {
                        double totalNeeded = first.getGewicht().doubleValue()
                                - ziel.getZielGewicht().doubleValue();
                        double achieved = first.getGewicht().doubleValue()
                                - latest.getGewicht().doubleValue();
                        if (totalNeeded != 0) {
                            double pct = (achieved / totalNeeded) * 100;
                            result.put("zielErreichungProzent",
                                    Math.round(pct * 10.0) / 10.0);
                        }
                    }
                }

                if (ziel.getZielFettmasse() != null && latest.getFettmasse() != null) {
                    double deltaFett = latest.getFettmasse().doubleValue()
                            - ziel.getZielFettmasse().doubleValue();
                    result.put("deltaZuZielFettmasse",
                            Math.round(deltaFett * 10.0) / 10.0);
                }
            }

            // Compute next monitoring date (14 days from last)
            LocalDate naechstes = latest.getDatum().plusDays(14);
            result.put("naechstesDatum", naechstes);
            result.put("faellig", !naechstes.isAfter(LocalDate.now()));
        }

        return ResponseEntity.ok(result);
    }

    // Helper: build response enriched with goal deltas
    private Map<String, Object> buildEnrichedResponse(Monitoring m, UUID fallId) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", m.getId());
        r.put("datum", m.getDatum());
        r.put("gewicht", m.getGewicht());
        r.put("fettmasse", m.getFettmasse());
        r.put("kalorienaufnahme", m.getKalorienaufnahme());
        r.put("fettaufnahme", m.getFettaufnahme());
        r.put("zielerreichung", m.getZielerreichung());
        r.put("foerderndefaktoren", m.getFoerderndefaktoren());
        r.put("hemmendefaktoren", m.getHemmendefaktoren());
        r.put("notizen", m.getNotizen());

        // Add goal context
        List<Ziele> ziele = zieleRepo.findByFallId(fallId);
        if (!ziele.isEmpty()) {
            Ziele z = ziele.get(0);
            if (z.getZielGewicht() != null && m.getGewicht() != null) {
                r.put("deltaZuZielGewicht",
                        Math.round((m.getGewicht().doubleValue()
                                - z.getZielGewicht().doubleValue()) * 10.0) / 10.0);
            }
        }
        return r;
    }

    private BigDecimal toBD(Object v) {
        if (v == null) return null;
        return new BigDecimal(v.toString());
    }
}