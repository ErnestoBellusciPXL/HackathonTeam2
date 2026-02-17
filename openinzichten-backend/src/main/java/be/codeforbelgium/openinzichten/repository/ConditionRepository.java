package be.codeforbelgium.openinzichten.repository;

import be.codeforbelgium.openinzichten.domain.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConditionRepository extends JpaRepository<Condition, String> {
    Optional<Condition> findByName(String name);

    List<Condition> findByNameContainingIgnoreCase(String namePart);
}
