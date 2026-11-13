package com.alumni.searcher.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.alumni.searcher.entity.AlumniProfile;

@DataJpaTest
class AlumniProfileRepositoryTest {

    @Autowired
    private AlumniProfileRepository repository;

    @Test
    void findsSavedProfilesByProfileUrl() {
        repository.saveAll(List.of(
                profile("John Doe", "https://www.linkedin.com/in/john-doe"),
                profile("Jane Smith", "https://www.linkedin.com/in/jane-smith")));

        List<AlumniProfile> found = repository.findByProfileUrlIn(
                List.of("https://www.linkedin.com/in/john-doe", "https://www.linkedin.com/in/unknown"));

        assertThat(found).extracting(AlumniProfile::getName).containsExactly("John Doe");
        assertThat(found.get(0).getId()).isNotNull();
        assertThat(found.get(0).getCreatedAt()).isNotNull();
    }

    private AlumniProfile profile(String name, String url) {
        AlumniProfile profile = new AlumniProfile();
        profile.setName(name);
        profile.setCurrentRole("Software Engineer");
        profile.setUniversity("University of XYZ");
        profile.setPassoutYear(2020);
        profile.setProfileUrl(url);
        return profile;
    }
}
