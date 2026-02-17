package be.codeforbelgium.openinzichten.repository;

import be.codeforbelgium.openinzichten.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    boolean existsByReporterIdAndStoryId(UUID reporterId, UUID storyId);

    List<Ticket> findAllByStoryId(UUID storyId);

    List<Ticket> findAllByReporterId(UUID reporterId);

    List<Ticket> findAllByReporteeId(UUID reporteeId);

    @Query("select t from Ticket t join t.story s where s.owner.id = :ownerId")
    List<Ticket> findAllByStoryOwnerId(@Param("ownerId") UUID ownerId);
}
