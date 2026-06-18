package com.diaetologie.diaetologenbe.service;

import com.diaetologie.diaetologenbe.dto.NeuerFallRequest;
import com.diaetologie.diaetologenbe.dto.PatientenfallDTO;
import com.diaetologie.diaetologenbe.entity.PatientStammdaten;
import com.diaetologie.diaetologenbe.entity.Patientenfall;
import com.diaetologie.diaetologenbe.respository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatientenfallService {

    private final PatientenfallRepository        fallRepo;
    private final PatientStammdatenRepository    patientRepo;
    private final AssessmentAllgemeinRepository  allgemeinRepo;
    private final AssessmentKoerperRepository    koerperRepo;
    private final AssessmentErnaehrungRepository ernaehrungRepo;
    private final DiagnoseRepository             diagnoseRepo;
    private final ZieleRepository                zieleRepo;
    private final MonitoringRepository           monitoringRepo;
    private final KiEntscheidungRepository       kiRepo;

    public PatientenfallService(
            PatientenfallRepository        fallRepo,
            PatientStammdatenRepository    patientRepo,
            AssessmentAllgemeinRepository  allgemeinRepo,
            AssessmentKoerperRepository    koerperRepo,
            AssessmentErnaehrungRepository ernaehrungRepo,
            DiagnoseRepository             diagnoseRepo,
            ZieleRepository                zieleRepo,
            MonitoringRepository           monitoringRepo,
            KiEntscheidungRepository       kiRepo) {
        this.fallRepo       = fallRepo;
        this.patientRepo    = patientRepo;
        this.allgemeinRepo  = allgemeinRepo;
        this.koerperRepo    = koerperRepo;
        this.ernaehrungRepo = ernaehrungRepo;
        this.diagnoseRepo   = diagnoseRepo;
        this.zieleRepo      = zieleRepo;
        this.monitoringRepo = monitoringRepo;
        this.kiRepo         = kiRepo;
    }

    public List<PatientenfallDTO> alleAlsDto() {
        return fallRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public PatientenfallDTO neuerFall(NeuerFallRequest req) {
        PatientStammdaten p = new PatientStammdaten();
        p.setAnonymerCode(generiereCode());
        p.setVorname(req.getVorname());
        p.setNachname(req.getNachname());
        p.setAlterJahre(req.getAlterJahre());
        p.setGeschlecht(req.getGeschlecht());
        patientRepo.save(p);

        Patientenfall fall = new Patientenfall();
        fall.setPatient(p);
        fallRepo.save(fall);
        return toDto(fall);
    }

    public PatientenfallDTO perIdAlsDto(UUID id) {
        return fallRepo.findById(id).map(this::toDto).orElseThrow();
    }

    // Delete fall + all related data + patient stammdaten
    @Transactional
    public void loeschen(UUID fallId) {
        Patientenfall fall = fallRepo.findById(fallId)
                .orElseThrow(() -> new RuntimeException("Fall nicht gefunden: " + fallId));

        // Delete all child records first (FK constraints)
        kiRepo.deleteAll(kiRepo.findByFallIdOrderByErstelltAmDesc(fallId));
        monitoringRepo.deleteAll(monitoringRepo.findByFallIdOrderByDatumDesc(fallId));
        zieleRepo.deleteAll(zieleRepo.findByFallId(fallId));
        diagnoseRepo.deleteAll(diagnoseRepo.findByFallId(fallId));
        ernaehrungRepo.deleteAll(ernaehrungRepo.findByFallId(fallId));
        koerperRepo.deleteAll(koerperRepo.findByFallId(fallId));
        allgemeinRepo.deleteAll(allgemeinRepo.findByFallId(fallId));

        // Delete the fall itself
        UUID patientId = fall.getPatient() != null ? fall.getPatient().getId() : null;
        fallRepo.delete(fall);

        // Delete patient stammdaten if no other falls reference it
        if (patientId != null) {
            boolean nochFaelle = fallRepo.findAll().stream()
                    .anyMatch(f -> f.getPatient() != null
                            && f.getPatient().getId().equals(patientId));
            if (!nochFaelle) {
                patientRepo.deleteById(patientId);
            }
        }
    }

    private PatientenfallDTO toDto(Patientenfall fall) {
        PatientenfallDTO dto = new PatientenfallDTO();
        dto.setId(fall.getId());
        dto.setErstelltAm(fall.getErstelltAm() != null
                ? fall.getErstelltAm().toString() : null);
        if (fall.getPatient() != null) {
            dto.setPatientId(fall.getPatient().getId());
            dto.setAlterJahre(fall.getPatient().getAlterJahre());
            dto.setGeschlecht(fall.getPatient().getGeschlecht());
            dto.setAnonymerCode(fall.getPatient().getAnonymerCode());
            dto.setVorname(fall.getPatient().getVorname());
            dto.setNachname(fall.getPatient().getNachname());
        }
        return dto;
    }

    private String generiereCode() {
        String z = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random r = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder("ADI-")
                    .append(java.time.Year.now().getValue()).append("-");
            for (int i = 0; i < 4; i++) sb.append(z.charAt(r.nextInt(z.length())));
            code = sb.toString();
        } while (patientRepo.existsByAnonymerCode(code));
        return code;
    }
}