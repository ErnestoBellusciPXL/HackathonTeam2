package be.codeforbelgium.openinzichten.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.*;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(min = 2, max = 40)
    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String zipcode;

    @NotEmpty
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "role", nullable = false)
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean hasCondition = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean disabled = false;

    @Column(name = "disabled_reason", nullable = true, length = 1000)
    private String disabledReason;

    @Column(name = "disabled_at", nullable = true)
    private Instant disabledAt;

    @ManyToMany
    @JoinTable(name = "account_communities", joinColumns = @JoinColumn(name = "account_id"), inverseJoinColumns = @JoinColumn(name = "community_name"))
    @Builder.Default
    private Set<Community> communities = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "account_conditions", joinColumns = @JoinColumn(name = "account_id"), inverseJoinColumns = @JoinColumn(name = "condition_name"))
    @Builder.Default
    private Set<Condition> conditions = new HashSet<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Story> stories = new HashSet<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PasswordResetToken> resetTokens = new HashSet<>();

    // Pending connection requests with state
    @ElementCollection
    @CollectionTable(name = "account_connection_requests", joinColumns = @JoinColumn(name = "account_id"))
    @Builder.Default
    private List<ConnectionRequest> connectionRequests = new ArrayList<>();

    // Connections using dedicated entity (new model)
    @OneToMany(mappedBy = "accountA", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Connection> connectionsInitiated = new HashSet<>();

    @OneToMany(mappedBy = "accountB", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Connection> connectionsReceived = new HashSet<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StoryLike> storyLikes = new HashSet<>();
}
