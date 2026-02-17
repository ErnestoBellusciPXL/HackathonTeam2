package be.codeforbelgium.openinzichten.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Community {
    @Id
    @Column(unique = true)
    @EqualsAndHashCode.Include
    private String name;

    @ManyToMany(mappedBy = "communities")
    private Set<Condition> conditions;

    @ManyToMany(mappedBy = "communities")
    private Set<Account> accounts;
}
