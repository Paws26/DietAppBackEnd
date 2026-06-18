package com.diaetologie.diaetologenbe.controller;

import com.diaetologie.diaetologenbe.dto.ZieleDTO;
import com.diaetologie.diaetologenbe.entity.Patientenfall;
import com.diaetologie.diaetologenbe.entity.Ziele;
import com.diaetologie.diaetologenbe.respository.PatientenfallRepository;
import com.diaetologie.diaetologenbe.respository.ZieleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ziele")
public class ZieleController {

    private final ZieleRepository repo;
    private final PatientenfallRepository fallRepo;

    public ZieleController(ZieleRepository repo, PatientenfallRepository fallRepo) {
        this.repo = repo;
        this.fallRepo = fallRepo;
    }

    @GetMapping("/fall/{fallId}")
    public List<Ziele> perFall(@PathVariable UUID fallId) {
        return repo.findByFallId(fallId);
    }

    // Returns the most recent Ziel for a fall — used by monitoring + assessment
    @GetMapping("/fall/{fallId}/latest")
    public ResponseEntity<?> getLatest(@PathVariable UUID fallId) {
        List<Ziele> list = repo.findByFallId(fallId);
        if (list.isEmpty()) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(list.get(0));
    }

    @PostMapping
    public ResponseEntity<?> speichern(@RequestBody ZieleDTO dto) {
        try {
            Patientenfall fall = fallRepo.findById(dto.getFallId()).orElseThrow();

            // Update existing if present, otherwise create new
            List<Ziele> existing = repo.findByFallId(dto.getFallId());
            Ziele z = existing.isEmpty() ? new Ziele() : existing.get(0);

            z.setFall(fall);
            z.setLangfristigesZiel(dto.getLangfristigesZiel());
            z.setInterventionsziel(dto.getInterventionsziel());
            z.setZielGewicht(dto.getZielGewicht());
            z.setZielFettmasse(dto.getZielFettmasse());
            z.setZielKalorien(dto.getZielKalorien());
            z.setZielFettaufnahme(dto.getZielFettaufnahme());
            z.setZielDatum(dto.getZielDatum());
            z.setStatus(dto.getStatus() != null ? dto.getStatus() : "offen");

            return ResponseEntity.ok(repo.save(z));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Fehler: " + e.getMessage());
        }
    }
}