package be.codeforbelgium.openinzichten.repository;

import be.codeforbelgium.openinzichten.domain.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityRepository extends JpaRepository<Community, String> {
    Optional<Community> findByName(String name);
}
