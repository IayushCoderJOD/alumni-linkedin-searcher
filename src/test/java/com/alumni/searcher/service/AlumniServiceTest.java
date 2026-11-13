package com.alumni.searcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.alumni.searcher.client.LinkedInProfileClient;
import com.alumni.searcher.client.ScrapedProfile;
import com.alumni.searcher.dto.AlumniProfileResponse;
import com.alumni.searcher.dto.AlumniSearchRequest;
import com.alumni.searcher.entity.AlumniProfile;
import com.alumni.searcher.exception.ProfileSourceException;
import com.alumni.searcher.repository.AlumniProfileRepository;

@ExtendWith(MockitoExtension.class)
class AlumniServiceTest {

    private static final String JOHN_URL = "https://www.linkedin.com/in/john-doe";

    @Mock
    private LinkedInProfileClient profileClient;

    @Mock
    private AlumniProfileRepository repository;

    @InjectMocks
    private AlumniService alumniService;

    @Captor
    private ArgumentCaptor<List<AlumniProfile>> savedCaptor;

    private final AlumniSearchRequest request = new AlumniSearchRequest("University of XYZ", "Software Engineer", 2020);

    @Test
    void searchSavesScrapedProfilesAndReturnsThem() {
        when(profileClient.search(request)).thenReturn(List.of(
                scraped("John Doe", JOHN_URL),
                scraped("Jane Smith", "https://www.linkedin.com/in/jane-smith")));
        when(repository.findByProfileUrlIn(anyCollection())).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<AlumniProfileResponse> result = alumniService.search(request);

        assertThat(result).extracting(AlumniProfileResponse::name).containsExactly("John Doe", "Jane Smith");
        assertThat(result.get(0).passoutYear()).isEqualTo(2020);

        verify(repository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).extracting(AlumniProfile::getProfileUrl)
                .containsExactly(JOHN_URL, "https://www.linkedin.com/in/jane-smith");
    }

    @Test
    void searchUpdatesAlreadySavedProfileInsteadOfDuplicating() {
        AlumniProfile existing = new AlumniProfile();
        existing.setName("John Doe");
        existing.setCurrentRole("Intern");
        existing.setUniversity("University of XYZ");
        existing.setProfileUrl(JOHN_URL);

        when(profileClient.search(request)).thenReturn(List.of(scraped("John Doe", JOHN_URL)));
        when(repository.findByProfileUrlIn(anyCollection())).thenReturn(List.of(existing));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        alumniService.search(request);

        verify(repository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).hasSize(1);
        assertThat(savedCaptor.getValue().get(0)).isSameAs(existing);
        assertThat(existing.getCurrentRole()).isEqualTo("Software Engineer");
    }

    @Test
    void searchCollapsesDuplicateProfilesFromOneScrape() {
        when(profileClient.search(request)).thenReturn(List.of(
                scraped("John Doe", JOHN_URL),
                scraped("John Doe", JOHN_URL),
                scraped("No Url", null)));
        when(repository.findByProfileUrlIn(anyCollection())).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(alumniService.search(request)).hasSize(2);
    }

    @Test
    void searchWithNoResultsDoesNotTouchTheDatabase() {
        when(profileClient.search(request)).thenReturn(List.of());

        assertThat(alumniService.search(request)).isEmpty();
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void searchPropagatesProfileSourceFailure() {
        when(profileClient.search(request))
                .thenThrow(new ProfileSourceException(HttpStatus.BAD_GATEWAY, "down"));

        assertThatThrownBy(() -> alumniService.search(request)).isInstanceOf(ProfileSourceException.class);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void getAllReturnsEverySavedProfile() {
        AlumniProfile profile = new AlumniProfile();
        profile.setName("Jane Smith");
        profile.setCurrentRole("Data Scientist");
        profile.setUniversity("University of XYZ");
        profile.setLocation("San Francisco, CA");
        profile.setLinkedinHeadline("Data Scientist | AI Enthusiast");
        profile.setPassoutYear(2019);
        when(repository.findAll()).thenReturn(List.of(profile));

        assertThat(alumniService.getAll()).containsExactly(new AlumniProfileResponse(
                "Jane Smith", "Data Scientist", "University of XYZ", "San Francisco, CA",
                "Data Scientist | AI Enthusiast", 2019));
    }

    private ScrapedProfile scraped(String name, String url) {
        return new ScrapedProfile(name, "Software Engineer", "University of XYZ", "New York, NY",
                "Engineer at XYZ Corp", 2020, url);
    }
}
