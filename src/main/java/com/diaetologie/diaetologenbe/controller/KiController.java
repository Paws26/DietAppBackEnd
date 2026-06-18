package com.diaetologie.diaetologenbe.controller;

import com.diaetologie.diaetologenbe.entity.*;
import com.diaetologie.diaetologenbe.respository.*;
import com.diaetologie.diaetologenbe.service.GroqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ki")
public class KiController {

    private final GroqService groqService;
    private final PatientenfallRepository fallRepo;
    private final AssessmentKoerperRepository koerperRepo;
    private final AssessmentErnaehrungRepository ernaehrungRepo;
    private final DiagnoseRepository diagnoseRepo;
    private final AssessmentAllgemeinRepository allgemeinRepo;
    private final KiEntscheidungRepository kiRepo;
    private final ZieleRepository zieleRepo;
    private final MonitoringRepository monitoringRepo;

    public KiController(GroqService groqService,
                        PatientenfallRepository fallRepo,
                        AssessmentKoerperRepository koerperRepo,
                        AssessmentErnaehrungRepository ernaehrungRepo,
                        DiagnoseRepository diagnoseRepo,
                        AssessmentAllgemeinRepository allgemeinRepo,
                        KiEntscheidungRepository kiRepo,
                        ZieleRepository zieleRepo,
                        MonitoringRepository monitoringRepo) {
        this.groqService = groqService;
        this.fallRepo = fallRepo;
        this.koerperRepo = koerperRepo;
        this.ernaehrungRepo = ernaehrungRepo;
        this.diagnoseRepo = diagnoseRepo;
        this.allgemeinRepo = allgemeinRepo;
        this.kiRepo = kiRepo;
        this.zieleRepo = zieleRepo;
        this.monitoringRepo = monitoringRepo;
    }

    // ── Assessment recommendation ────────────────────────────────────────────
    @PostMapping("/empfehlung/{fallId}")
    public ResponseEntity<?> empfehlung(@PathVariable UUID fallId) {
        try {
            Patientenfall fall = fallRepo.findById(fallId).orElseThrow();

            AssessmentKoerper k = first(koerperRepo.findByFallId(fallId));
            AssessmentErnaehrung e = first(ernaehrungRepo.findByFallId(fallId));
            Diagnose d = first(diagnoseRepo.findByFallId(fallId));
            AssessmentAllgemein a = first(allgemeinRepo.findByFallId(fallId));
            Ziele z = first(zieleRepo.findByFallId(fallId));

            String antwort = groqService.ernaehrungsempfehlung(
                    bd(k, "bmi"), bd(k, "fettmasse"), bd(k, "fettmasseIdealMax"),
                    k != null ? k.getBlutdruckSystolisch() : null,
                    k != null ? k.getBlutdruckDiastolisch() : null,
                    bd(e, "energiebedarfKcal"), bd(e, "kalorienaufnahme"),
                    bd(e, "fettbedarfG"), bd(e, "fettaufnahme"),
                    e != null ? e.getZuckergetraenke() : null,
                    e != null ? e.getAlkoholKonsum() : null,
                    e != null ? e.getFertigprodukteKonsum() : null,
                    e != null ? e.getSuesswarenKonsum() : null,
                    e != null ? e.getKochbereitschaft() : null,
                    a != null ? a.getMotivationLevel() : null,
                    d != null ? d.getProblem() : null,
                    d != null ? d.getUrsache() : null,
                    d != null ? d.getSymptom() : null,
                    z != null ? z.getLangfristigesZiel() : null,
                    z != null ? z.getInterventionsziel() : null,
                    z != null && z.getZielGewicht() != null
                            ? z.getZielGewicht().doubleValue() : null,
                    z != null && z.getZielFettmasse() != null
                            ? z.getZielFettmasse().doubleValue() : null,
                    z != null && z.getZielKalorien() != null
                            ? z.getZielKalorien().doubleValue() : null,
                    z != null && z.getZielFettaufnahme() != null
                            ? z.getZielFettaufnahme().doubleValue() : null
            );

            KiEntscheidung ki = new KiEntscheidung();
            ki.setFall(fall);
            ki.setTyp("assessment");
            ki.setEingabeJson("Assessment-Empfehlung für Fall: " + fallId);
            ki.setAusgabeJson(antwort);
            kiRepo.save(ki);

            return ResponseEntity.ok(Map.of(
                    "empfehlung", antwort,
                    "gespeichert", true,
                    "typ", "assessment"
            ));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("fehler", ex.getMessage()));
        }
    }

    // ── Monitoring adaptation recommendation ────────────────────────────────
    @PostMapping("/monitoring/{fallId}")
    public ResponseEntity<?> monitoringAdaption(@PathVariable UUID fallId) {
        try {
            Patientenfall fall = fallRepo.findById(fallId).orElseThrow();

            Ziele z = first(zieleRepo.findByFallId(fallId));
            List<Monitoring> verlauf = monitoringRepo.findByFallIdOrderByDatumDesc(fallId);

            if (verlauf.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("fehler", "Keine Monitoring-Daten vorhanden"));
            }

            Monitoring latest = verlauf.get(0);
            Monitoring start = verlauf.get(verlauf.size() - 1);

            String antwort = groqService.monitoringAdaption(
                    z != null && z.getZielGewicht() != null
                            ? z.getZielGewicht().doubleValue() : null,
                    z != null && z.getZielFettmasse() != null
                            ? z.getZielFettmasse().doubleValue() : null,
                    z != null && z.getZielKalorien() != null
                            ? z.getZielKalorien().doubleValue() : null,
                    z != null && z.getZielFettaufnahme() != null
                            ? z.getZielFettaufnahme().doubleValue() : null,
                    z != null ? z.getInterventionsziel() : null,
                    start.getGewicht() != null ? start.getGewicht().doubleValue() : null,
                    start.getFettmasse() != null ? start.getFettmasse().doubleValue() : null,
                    latest.getGewicht() != null ? latest.getGewicht().doubleValue() : null,
                    latest.getFettmasse() != null ? latest.getFettmasse().doubleValue() : null,
                    latest.getKalorienaufnahme() != null
                            ? latest.getKalorienaufnahme().doubleValue() : null,
                    latest.getFettaufnahme() != null
                            ? latest.getFettaufnahme().doubleValue() : null,
                    verlauf.size(),
                    latest.getZielerreichung(),
                    latest.getFoerderndefaktoren(),
                    latest.getHemmendefaktoren()
            );

            KiEntscheidung ki = new KiEntscheidung();
            ki.setFall(fall);
            ki.setTyp("monitoring");
            ki.setEingabeJson("Monitoring-Adaption für Fall: " + fallId);
            ki.setAusgabeJson(antwort);
            kiRepo.save(ki);

            return ResponseEntity.ok(Map.of(
                    "empfehlung", antwort,
                    "gespeichert", true,
                    "typ", "monitoring"
            ));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("fehler", ex.getMessage()));
        }
    }

    // ── Get last ASSESSMENT recommendation only ──────────────────────────────
    @GetMapping("/fall/{fallId}")
    public ResponseEntity<?> letzteEmpfehlung(@PathVariable UUID fallId) {
        List<KiEntscheidung> list =
                kiRepo.findByFallIdAndTypOrderByErstelltAmDesc(fallId, "assessment");
        if (list.isEmpty()) {
            return ResponseEntity.ok(
                    Map.of("message", "Keine Empfehlungen vorhanden"));
        }
        KiEntscheidung latest = list.get(0);
        return ResponseEntity.ok(Map.of(
                "empfehlung", latest.getAusgabeJson(),
                "erstelltAm", latest.getErstelltAm().toString()
        ));
    }

    // ── Get last MONITORING recommendation only ──────────────────────────────
    @GetMapping("/monitoring/{fallId}/letzte")
    public ResponseEntity<?> letzteMonitoringAdaption(@PathVariable UUID fallId) {
        List<KiEntscheidung> list =
                kiRepo.findByFallIdAndTypOrderByErstelltAmDesc(fallId, "monitoring");
        if (list.isEmpty()) {
            return ResponseEntity.ok(
                    Map.of("message", "Keine Monitoring-Auswertung vorhanden"));
        }
        KiEntscheidung latest = list.get(0);
        return ResponseEntity.ok(Map.of(
                "empfehlung", latest.getAusgabeJson(),
                "erstelltAm", latest.getErstelltAm().toString()
        ));
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "Groq"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private <T> T first(List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private Double bd(Object entity, String field) {
        if (entity == null) return null;
        try {
            var method = entity.getClass()
                    .getMethod("get" + Character.toUpperCase(field.charAt(0))
                            + field.substring(1));
            Object val = method.invoke(entity);
            if (val == null) return null;
            return ((java.math.BigDecimal) val).doubleValue();
        } catch (Exception e) {
            return null;
        }
    }
}