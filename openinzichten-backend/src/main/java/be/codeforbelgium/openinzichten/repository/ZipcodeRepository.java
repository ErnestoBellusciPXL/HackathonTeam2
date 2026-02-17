package be.codeforbelgium.openinzichten.repository;

import be.codeforbelgium.openinzichten.domain.Zipcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZipcodeRepository extends JpaRepository<Zipcode, Long> {
    List<Zipcode> findAllByCode(String code);
}
