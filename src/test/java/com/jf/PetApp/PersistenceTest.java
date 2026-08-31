package com.jf.PetApp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;

@DataJpaTest
class PersistenceTest {

	@Autowired
	private SpringUserJpaRepository repository;

	@Test
	void should_save_and_find_user_by_email() {
		// given
		User user = new User();
		user.setUsername("ana");
		user.setEmail("ana@test.com");
		user.setPassword("123");

		UserJpaEntity userRepoEntity = UserJpaEntity.fromDomain(user);

		// when
		repository.save(userRepoEntity);

		// then
		var found = repository.findByEmail("ana@test.com");

		assertThat(found).isPresent();
		assertThat(found.get().toDomain().getUsername()).isEqualTo("ana");
	}

}
