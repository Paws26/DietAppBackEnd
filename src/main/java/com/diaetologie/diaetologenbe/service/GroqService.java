package com.diaetologie.diaetologenbe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class GroqService {

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:mixtral-8x7b-32768}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── Main assessment recommendation ──────────────────────────────────────
    public String ernaehrungsempfehlung(
            Double bmi, Double fettmasse, Double fettmasseIdealMax,
            Integer blutdruckSys, Integer blutdruckDia,
            Double energiebedarfKcal, Double kalorienaufnahme,
            Double fettbedarfG, Double fettaufnahme,
            String zuckergetraenke, String alkoholKonsum,
            String fertigprodukteKonsum, String suesswarenKonsum,
            Boolean kochbereitschaft,
            String motivationLevel,
            String problem, String ursache, String symptom,
            // Goal values
            String langfristigesZiel, String interventionsziel,
            Double zielGewicht, Double zielFettmasse,
            Double zielKalorien, Double zielFettaufnahme) {

        if (apiKey == null || apiKey.isBlank()) {
            return getMockAssessment();
        }

        String prompt = buildAssessmentPrompt(
                bmi, fettmasse, fettmasseIdealMax,
                blutdruckSys, blutdruckDia,
                energiebedarfKcal, kalorienaufnahme,
                fettbedarfG, fettaufnahme,
                zuckergetraenke, alkoholKonsum,
                fertigprodukteKonsum, suesswarenKonsum,
                kochbereitschaft, motivationLevel,
                problem, ursache, symptom,
                langfristigesZiel, interventionsziel,
                zielGewicht, zielFettmasse,
                zielKalorien, zielFettaufnahme);

        return callGroq(prompt);
    }

    // ── Monitoring adaptation recommendation ────────────────────────────────
    public String monitoringAdaption(
            // Goal
            Double zielGewicht, Double zielFettmasse,
            Double zielKalorien, Double zielFettaufnahme,
            String interventionsziel,
            // Start values
            Double startGewicht, Double startFettmasse,
            // Current (latest monitoring)
            Double aktuellesGewicht, Double aktuelleFettmasse,
            Double aktuelleKalorien, Double aktuelleFettaufnahme,
            // History
            int anzahlMessungen,
            String letzteZielerreichung,
            String foerderndefaktoren,
            String hemmendefaktoren) {

        if (apiKey == null || apiKey.isBlank()) {
            return getMockMonitoring();
        }

        double gewDelta = aktuellesGewicht != null && startGewicht != null
                ? aktuellesGewicht - startGewicht : 0;
        double gewZielDelta = aktuellesGewicht != null && zielGewicht != null
                ? aktuellesGewicht - zielGewicht : 0;

        String prompt = String.format("""
            Du bist ein erfahrener österreichischer Diaetologe. Erstelle eine Monitoring-Auswertung
            auf Deutsch, basierend auf den folgenden Verlaufsdaten.
            
            INTERVENTIONSZIEL: %s
            
            ZIELWERTE:
            - Zielgewicht: %.1f kg | Ziel-Fettmasse: %.1f kg
            - Ziel-Kalorienaufnahme: %.0f kcal/d | Ziel-Fettaufnahme: %.0f g/d
            
            VERLAUF (%d Messungen):
            - Startgewicht: %.1f kg | Start-Fettmasse: %.1f kg
            - Aktuelles Gewicht: %.1f kg (Δ seit Start: %+.1f kg)
            - Aktuelle Fettmasse: %.1f kg
            - Aktuelle Kalorienaufnahme: %.0f kcal/d
            - Aktuelle Fettaufnahme: %.0f g/d
            - Differenz zum Zielgewicht: %+.1f kg
            - Letzte Zielerreichung: %s
            - Fördernde Faktoren: %s
            - Hemmende Faktoren: %s
            
            Erstelle auf Deutsch eine strukturierte Monitoring-Auswertung:
            
            📊 INTERPRETATION
            (2-3 Sätze: Wie entwickelt sich der Patient? Ist er auf Kurs?)
            
            ✅ ZIELERREICHUNG
            (Kurze Bewertung: Ja / Teilweise / Nein — mit Begründung)
            
            🔧 ADAPTION DER MASSNAHMEN
            (Konkret: Soll der Plan angepasst werden? Was genau?)
            
            📋 WEITERES PROZEDERE
            (2-3 konkrete nächste Schritte für die nächsten 2 Wochen)
            """,
                safeStr(interventionsziel),
                safe(zielGewicht), safe(zielFettmasse),
                safe(zielKalorien), safe(zielFettaufnahme),
                anzahlMessungen,
                safe(startGewicht), safe(startFettmasse),
                safe(aktuellesGewicht), gewDelta,
                safe(aktuelleFettmasse),
                safe(aktuelleKalorien), safe(aktuelleFettaufnahme),
                gewZielDelta,
                safeStr(letzteZielerreichung),
                safeStr(foerderndefaktoren),
                safeStr(hemmendefaktoren)
        );

        return callGroq(prompt);
    }

    // ── Private: build the full assessment prompt ────────────────────────────
    private String buildAssessmentPrompt(
            Double bmi, Double fettmasse, Double fettmasseIdealMax,
            Integer blutdruckSys, Integer blutdruckDia,
            Double energiebedarfKcal, Double kalorienaufnahme,
            Double fettbedarfG, Double fettaufnahme,
            String zuckergetraenke, String alkohol,
            String fertig, String suess,
            Boolean kocht, String motivation,
            String problem, String ursache, String symptom,
            String langfristigesZiel, String interventionsziel,
            Double zielGewicht, Double zielFettmasse,
            Double zielKalorien, Double zielFettaufnahme) {

        double energieAbw = energiebedarfKcal != null && kalorienaufnahme != null && energiebedarfKcal > 0
                ? ((kalorienaufnahme - energiebedarfKcal) / energiebedarfKcal * 100) : 0;
        double fettAbw = fettbedarfG != null && fettaufnahme != null && fettbedarfG > 0
                ? ((fettaufnahme - fettbedarfG) / fettbedarfG * 100) : 0;

        return String.format("""
            Du bist ein erfahrener österreichischer Diaetologe und erstellst einen
            individuellen Ernährungstherapieplan — professionell, konkret und umsetzbar.
            Verwende das Format des österreichischen diaetologischen Prozesses.
            
            ═══ KLINISCHE DATEN ═══
            
            KÖRPER:
            - BMI: %.1f kg/m²
            - Fettmasse: %.1f kg (Idealbereich bis %.1f kg)
            - Blutdruck: %d/%d mmHg
            
            ENERGIE- UND NÄHRSTOFFBEDARF:
            - Bedarf: %.0f kcal/d | Aktuelle Aufnahme: %.0f kcal/d (%+.1f%%)
            - Fettbedarf: %.0f g/d | Aktuelle Fettaufnahme: %.0f g/d (%+.1f%%)
            
            ERNÄHRUNGSVERHALTEN:
            - Zuckergetränke/Energydrinks: %s
            - Alkohol: %s
            - Fertigprodukte/Convenience: %s
            - Süßwaren & Chips: %s
            - Kochbereitschaft: %s
            - Motivation: %s
            
            DIAETOLOGISCHE DIAGNOSE (PES):
            - Problem (P): %s
            - Ursache (E): %s
            - Symptome (S): %s
            
            THERAPIEZIELE:
            - Langfristiges Ziel: %s
            - Interventionsziel: %s
            - Zielgewicht: %.1f kg | Ziel-Fettmasse: %.1f kg
            - Ziel-Kalorienaufnahme: %.0f kcal/d | Ziel-Fettaufnahme: %.0f g/d
            
            ═══ ERSTELLE FOLGENDEN THERAPIEPLAN ═══
            
            📋 ANALYSE DER IST-SITUATION
            (3-4 Sätze: Was sind die Hauptprobleme? Worauf muss fokussiert werden?)
            
            🎯 3 SOFORTMASSNAHMEN
            (Die 3 wichtigsten Änderungen, die sofort umsetzbar sind)
            
            🍽️ TÄGLICHE ERNÄHRUNGSEMPFEHLUNG
            (Konkrete Lebensmittelauswahl: Was ja, was nein, Portionsgrößen)
            
            📅 WOCHENSPEISEPLAN-VORSCHLAG
            (Kurzer Überblick: Frühstück / Mittagessen / Abendessen / Jause)
            
            📊 ZIELE IN 5 WOCHEN
            (Realistische Zwischenziele für Gewicht, Fettmasse, Kalorienaufnahme)
            
            ⚠️ DIESE LEBENSMITTEL/GEWOHNHEITEN VERMEIDEN
            (Konkrete Liste mit Alternativen)
            
            💡 MOTIVATIONSTIPP
            (1 individueller Tipp basierend auf den Förderfaktoren des Patienten)
            """,
                safe(bmi), safe(fettmasse), safe(fettmasseIdealMax),
                safeInt(blutdruckSys), safeInt(blutdruckDia),
                safe(energiebedarfKcal), safe(kalorienaufnahme), energieAbw,
                safe(fettbedarfG), safe(fettaufnahme), fettAbw,
                safeStr(zuckergetraenke), safeStr(alkohol),
                safeStr(fertig), safeStr(suess),
                Boolean.TRUE.equals(kocht) ? "Ja" : "Nein",
                safeStr(motivation),
                safeStr(problem), safeStr(ursache), safeStr(symptom),
                safeStr(langfristigesZiel), safeStr(interventionsziel),
                safe(zielGewicht), safe(zielFettmasse),
                safe(zielKalorien), safe(zielFettaufnahme)
        );
    }

    // ── Private: call Groq API ───────────────────────────────────────────────
    private String callGroq(String userPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("temperature", 0.6);
            body.put("max_tokens", 2000);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", "Du bist ein erfahrener Diaetologe in Österreich. " +
                            "Antworte immer auf Deutsch. Sei konkret, klinisch präzise " +
                            "und patientenorientiert."
            ));
            messages.add(Map.of("role", "user", "content", userPrompt));
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions",
                    entity, Map.class);

            if (response.getBody() != null) {
                List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> msg =
                            (Map<String, Object>) choices.get(0).get("message");
                    return (String) msg.get("content");
                }
            }
            return getMockAssessment();
        } catch (Exception e) {
            System.err.println("Groq API error: " + e.getMessage());
            return "⚠️ Groq nicht erreichbar: " + e.getMessage();
        }
    }

    // ── Fallbacks ────────────────────────────────────────────────────────────
    private String getMockAssessment() {
        return """
                📋 ANALYSE DER IST-SITUATION
                [Mock] Groq API Key fehlt in application.properties.
                Bitte groq.api.key setzen um echte KI-Empfehlungen zu erhalten.
                
                🎯 3 SOFORTMASSNAHMEN
                1. Groq API Key in application.properties eintragen
                2. App neu starten
                3. Empfehlung neu generieren
                """;
    }

    private String getMockMonitoring() {
        return """
                📊 INTERPRETATION
                [Mock] Groq API Key fehlt. Bitte in application.properties setzen.
                
                ✅ ZIELERREICHUNG
                Nicht bewertbar ohne KI-Verbindung.
                
                🔧 ADAPTION
                Bitte Groq API Key konfigurieren.
                """;
    }

    private double safe(Double v) { return v != null ? v : 0.0; }
    private int safeInt(Integer v) { return v != null ? v : 0; }
    private String safeStr(String v) {
        return v != null && !v.isBlank() ? v : "nicht angegeben";
    }
}