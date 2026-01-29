package next.OAuth.demoOAuth.repository;

import next.OAuth.demoOAuth.model.User;
import next.OAuth.demoOAuth.model.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserProviderRepository extends JpaRepository<UserProvider, Long> {
    Optional<UserProvider> findByProviderAndProviderId(String provider, String providerId);
    Optional<UserProvider> findByUserAndProvider(User user, String provider);
    boolean existsByUserAndProvider(User user, String provider);
    List<UserProvider> findByUser(User user);
}
