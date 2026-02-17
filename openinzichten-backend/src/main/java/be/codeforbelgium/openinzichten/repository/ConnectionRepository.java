package be.codeforbelgium.openinzichten.repository;

import be.codeforbelgium.openinzichten.domain.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<Connection, UUID> {

    @Query("select c from Connection c where (c.accountA.id = :a and c.accountB.id = :b) or (c.accountA.id = :b and c.accountB.id = :a)")
    Optional<Connection> findBetween(@Param("a") UUID a, @Param("b") UUID b);

    List<Connection> findByAccountA_IdOrAccountB_Id(UUID accountAId, UUID accountBId);

    Optional<Connection> findByChatId(UUID chatId);
}
