package com.diaetologie.diaetologenbe.respository;

import com.diaetologie.diaetologenbe.entity.KiEntscheidung;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface KiEntscheidungRepository extends JpaRepository<KiEntscheidung, UUID> {
    List<KiEntscheidung> findByFallIdOrderByErstelltAmDesc(UUID fallId);

    // Filtered by type — keeps assessment and monitoring recommendations separate
    List<KiEntscheidung> findByFallIdAndTypOrderByErstelltAmDesc(UUID fallId, String typ);
}