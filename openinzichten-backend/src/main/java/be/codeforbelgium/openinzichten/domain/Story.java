package be.codeforbelgium.openinzichten.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "stories")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    // the textual content of the story
    @NotBlank
    @Size(min = 30, max = 10_000)
    @Column(nullable = false, columnDefinition = "text", length = 10_000)
    private String content;

    // a short title for the story
    @NotBlank
    @Column(nullable = false, length = 255)
    private String title;
    @ManyToMany
    @JoinTable(name = "story_conditions", joinColumns = @JoinColumn(name = "story_id"), inverseJoinColumns = @JoinColumn(name = "condition_name"))
    private Set<Condition> conditions;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private Account owner;

    @Builder.Default
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StoryLike> likes = new HashSet<>();

    @Builder.Default
    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();
}
