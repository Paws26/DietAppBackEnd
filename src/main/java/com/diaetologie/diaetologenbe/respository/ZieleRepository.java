package com.diaetologie.diaetologenbe.respository;

import com.diaetologie.diaetologenbe.entity.Ziele;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ZieleRepository extends JpaRepository<Ziele, UUID> {
    List<Ziele> findByFallId(UUID fallId);
}