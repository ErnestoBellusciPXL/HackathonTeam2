package be.codeforbelgium.openinzichten.domain;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "conditions") // avoid MySQL reserved keyword "CONDITION"
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Condition {

    @Id
    @Column(unique = true)
    @EqualsAndHashCode.Include
    private String name;

    @ManyToMany
    private Set<Community> communities;

    @ManyToMany(mappedBy = "conditions")
    private Set<Account> accounts;

}
