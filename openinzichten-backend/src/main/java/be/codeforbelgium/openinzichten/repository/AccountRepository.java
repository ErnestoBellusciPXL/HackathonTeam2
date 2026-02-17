package be.codeforbelgium.openinzichten.repository;

import be.codeforbelgium.openinzichten.domain.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);

    Optional<Account> findByUsername(String username);

    Page<Account> findAll(Pageable pageable);

    @Query("select a from Account a left join fetch a.conditions where a.username = :username")
    Optional<Account> findWithConditionsByUsername(@Param("username") String username);

    @Query("SELECT z.munCode, z.gemeente, COUNT(DISTINCT a.id) FROM Account a JOIN a.conditions c, Zipcode z WHERE c.name = :conditionName AND z.code = a.zipcode GROUP BY z.munCode, z.gemeente")
    List<Object[]> countAccountsBySpecificConditionGroupedByMunicipality(@Param("conditionName") String conditionName);

    @Query("SELECT z.munCode, z.gemeente, c.name, COUNT(DISTINCT a.id) FROM Account a JOIN a.conditions c, Zipcode z WHERE z.code = a.zipcode GROUP BY z.munCode, z.gemeente, c.name")
    List<Object[]> countAccountsByAllConditionsGroupedByMunicipality();

    @Query("SELECT z.munCode, z.gemeente, COUNT(DISTINCT a.id) FROM Account a JOIN a.conditions c, Zipcode z WHERE z.code = a.zipcode GROUP BY z.munCode, z.gemeente")
    List<Object[]> countDistinctAccountsWithConditionsGroupedByMunicipality();
}
