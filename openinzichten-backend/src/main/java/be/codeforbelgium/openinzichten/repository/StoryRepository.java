package be.codeforbelgium.openinzichten.repository;

import be.codeforbelgium.openinzichten.domain.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface StoryRepository extends JpaRepository<Story, UUID> {
    List<Story> findByOwnerId(UUID ownerId);

    Page<Story> getStoriesByTitleOrContentContaining(String title, String content, Pageable pageable);

    @Query("select s from Story s left join s.likes l group by s.id order by count(l) desc")
    Page<Story> findTopStories(Pageable pageable);

    @Query("""
            select s from Story s
            join s.conditions c
            left join s.likes l
            where c.name in :conditionNames
            group by s.id
            order by count(l) desc
            """)
    Page<Story> findTopByConditionNames(@Param("conditionNames") List<String> conditionNames, Pageable pageable);

    @Query("""
            select s from Story s
            join s.conditions c
            left join s.likes l
            where c.name = :conditionName
            group by s.id
            order by count(l) desc
            """)
    Page<Story> findTopByConditionName(@Param("conditionName") String conditionName, Pageable pageable);

    @Query("""
            select s from Story s
            left join s.likes l on l.createdAt >= :since
            group by s.id
            order by count(l) desc
            """)
    Page<Story> findTopStoriesSince(@Param("since") Instant since, Pageable pageable);

    @Query("""
            select s from Story s
            join s.conditions c
            left join s.likes l on l.createdAt >= :since
            where c.name = :conditionName
            group by s.id
            order by count(l) desc
            """)
    Page<Story> findTopByConditionNameSince(@Param("conditionName") String conditionName,
                                            @Param("since") Instant since,
                                            Pageable pageable);
    @Query("SELECT DISTINCT s FROM Story s LEFT JOIN s.conditions c " +
            "WHERE (:condNames IS NULL OR c.name IN :condNames) " +
            "AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(s.content) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Story> searchByQueryAndConditionNames(@Param("q") String query,
                                               @Param("condNames") List<String> conditionNames,
                                               Pageable pageable);

    @Query("SELECT DISTINCT s FROM Story s " +
            "LEFT JOIN s.conditions c " +
            "WHERE (:condNames IS NULL OR c.name IN :condNames) " +
            "AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(s.content) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY SIZE(s.likes) DESC")
    Page<Story> searchByQueryAndConditionNamesOrderByPopular(@Param("q") String query,
                                                              @Param("condNames") List<String> conditionNames,
                                                              Pageable pageable);

    @Query("SELECT DISTINCT s FROM Story s LEFT JOIN s.conditions c " +
            "WHERE (:condNames IS NULL OR c.name IN :condNames) " +
            "AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(s.content) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY s.createdAt DESC")
    Page<Story> searchByQueryAndConditionNamesOrderByNewest(@Param("q") String query,
                                                             @Param("condNames") List<String> conditionNames,
                                                             Pageable pageable);
}
