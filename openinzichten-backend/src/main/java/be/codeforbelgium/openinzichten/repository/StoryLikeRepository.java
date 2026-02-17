package be.codeforbelgium.openinzichten.repository;

import be.codeforbelgium.openinzichten.domain.StoryLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryLikeRepository extends JpaRepository<StoryLike, UUID> {

    boolean existsByStoryIdAndAccountId(UUID storyId, UUID accountId);

    long countByStoryId(UUID storyId);

    Optional<StoryLike> findByStoryIdAndAccountId(UUID storyId, UUID accountId);
}
