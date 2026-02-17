package be.codeforbelgium.openinzichten.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne()
    private Account accountA;

    @ManyToOne()
    private Account accountB;

    @Enumerated(EnumType.STRING)
    private ConnectionState status;

    @Column(name = "chat_id", nullable = false, unique = true)
    @Builder.Default
    private UUID chatId = UUID.randomUUID();
}
