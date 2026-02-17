package be.codeforbelgium.openinzichten.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(optional = true)
    @JoinColumn(name = "reporter_id", nullable = true)
    private Account reporter;

    @ManyToOne(optional = true)
    @JoinColumn(name = "reportee_id", nullable = true)
    private Account reportee; // the user being reported

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TicketState state = TicketState.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportReason reportReason;

    @Column(name = "other_reason", length = 1000)
    private String otherReason;

    @ManyToOne
    @JoinColumn(name = "story_id")
    private Story story;

    @Column(name = "story_title_snapshot", length = 255)
    private String storyTitleSnapshot;

    @Column(name = "story_content_snapshot", columnDefinition = "text", length = 10_000)
    private String storyContentSnapshot;

    @Column(name = "story_conditions_snapshot", length = 2000)
    private String storyConditionsSnapshot;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
