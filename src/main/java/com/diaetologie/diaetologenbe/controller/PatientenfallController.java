package com.diaetologie.diaetologenbe.controller;

import com.diaetologie.diaetologenbe.dto.NeuerFallRequest;
import com.diaetologie.diaetologenbe.dto.PatientenfallDTO;
import com.diaetologie.diaetologenbe.service.PatientenfallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/faelle")
public class PatientenfallController {

    private final PatientenfallService service;

    public PatientenfallController(PatientenfallService service) {
        this.service = service;
    }

    @GetMapping
    public List<PatientenfallDTO> alle() {
        return service.alleAlsDto();
    }

    @PostMapping
    public PatientenfallDTO neu(@RequestBody NeuerFallRequest req) {
        return service.neuerFall(req);
    }

    @GetMapping("/{id}")
    public PatientenfallDTO perFallId(@PathVariable UUID id) {
        return service.perIdAlsDto(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> loeschen(@PathVariable UUID id) {
        service.loeschen(id);
        return ResponseEntity.ok().build();
    }
}