package est.secretary.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import est.secretary.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByProviderAndProviderId(String provider, String providerId);

	Optional<Member> findByEmail(String email);
}
