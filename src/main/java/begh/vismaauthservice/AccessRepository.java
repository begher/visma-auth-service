package begh.vismaauthservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccessRepository extends JpaRepository<Access, Integer> {
    Optional<Access> findAccessByCompany(String company);
}
