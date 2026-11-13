package com.alumni.searcher.client.phantombuster;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.alumni.searcher.client.LinkedInProfileClient;
import com.alumni.searcher.client.ScrapedProfile;
import com.alumni.searcher.config.PhantomBusterProperties;
import com.alumni.searcher.dto.AlumniSearchRequest;
import com.alumni.searcher.exception.ProfileSourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Profile("!mock")
public class PhantomBusterClient implements LinkedInProfileClient {

    private static final Logger log = LoggerFactory.getLogger(PhantomBusterClient.class);
    private static final String API_KEY_HEADER = "X-Phantombuster-Key";
    private static final String LINKEDIN_PEOPLE_SEARCH = "https://www.linkedin.com/search/results/people/";
    private static final TypeReference<List<PhantomProfile>> PROFILE_LIST = new TypeReference<>() {
    };

    private final RestClient restClient;
    private final PhantomBusterProperties properties;
    private final PhantomProfileMapper mapper;
    private final ObjectMapper objectMapper;

    public PhantomBusterClient(RestClient.Builder restClientBuilder, PhantomBusterProperties properties,
                               PhantomProfileMapper mapper, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader(API_KEY_HEADER, properties.apiKey())
                .build();
        this.properties = properties;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ScrapedProfile> search(AlumniSearchRequest request) {
        if (!properties.isConfigured()) {
            throw new ProfileSourceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "PhantomBuster is not configured: set PHANTOMBUSTER_API_KEY, PHANTOMBUSTER_AGENT_ID and LINKEDIN_SESSION_COOKIE");
        }
        try {
            String containerId = launch(request);
            log.info("Launched PhantomBuster agent {}, container {}", properties.agentId(), containerId);
            awaitCompletion(containerId);
            return fetchProfiles(containerId).stream()
                    .map(profile -> mapper.toScrapedProfile(profile, request))
                    .filter(profile -> profile.name() != null)
                    .filter(profile -> matchesYear(profile, request.passoutYear()))
                    .toList();
        } catch (RestClientResponseException ex) {
            throw new ProfileSourceException(HttpStatus.BAD_GATEWAY,
                    "PhantomBuster responded with status " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new ProfileSourceException(HttpStatus.BAD_GATEWAY, "Could not reach PhantomBuster", ex);
        }
    }

    private String launch(AlumniSearchRequest request) {
        Map<String, Object> argument = Map.of(
                "sessionCookie", properties.sessionCookie(),
                "category", "People",
                "search", buildSearchUrl(request),
                "numberOfResultsPerSearch", properties.maxResults(),
                "numberOfLinesPerLaunch", properties.maxResults());

        LaunchResponse response = restClient.post()
                .uri("/agents/launch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("id", properties.agentId(), "argument", argument))
                .retrieve()
                .body(LaunchResponse.class);

        if (response == null || !StringUtils.hasText(response.containerId())) {
            throw new ProfileSourceException(HttpStatus.BAD_GATEWAY, "PhantomBuster did not start the agent");
        }
        return response.containerId();
    }

    private void awaitCompletion(String containerId) {
        Instant deadline = Instant.now().plus(properties.timeout());
        while (true) {
            ContainerStatus status = restClient.get()
                    .uri("/containers/fetch?id={id}", containerId)
                    .retrieve()
                    .body(ContainerStatus.class);

            if (status != null && status.isFinished()) {
                if (status.isFailed()) {
                    throw new ProfileSourceException(HttpStatus.BAD_GATEWAY,
                            "PhantomBuster agent failed with exit code " + status.exitCode());
                }
                return;
            }
            if (!Instant.now().isBefore(deadline)) {
                throw new ProfileSourceException(HttpStatus.GATEWAY_TIMEOUT,
                        "PhantomBuster agent did not finish within " + properties.timeout().toSeconds() + " seconds");
            }
            pause();
        }
    }

    private List<PhantomProfile> fetchProfiles(String containerId) {
        ResultObjectResponse response = restClient.get()
                .uri("/containers/fetch-result-object?id={id}", containerId)
                .retrieve()
                .body(ResultObjectResponse.class);

        if (response == null || !StringUtils.hasText(response.resultObject())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(response.resultObject(), PROFILE_LIST).stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (JsonProcessingException ex) {
            throw new ProfileSourceException(HttpStatus.BAD_GATEWAY,
                    "PhantomBuster returned a result that could not be read", ex);
        }
    }

    private String buildSearchUrl(AlumniSearchRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(LINKEDIN_PEOPLE_SEARCH)
                .queryParam("schoolFreetext", "\"" + request.university().trim() + "\"")
                .queryParam("titleFreeText", request.designation().trim())
                .queryParam("origin", "FACETED_SEARCH");
        if (request.passoutYear() != null) {
            builder.queryParam("keywords", request.passoutYear());
        }
        return builder.encode().toUriString();
    }

    private boolean matchesYear(ScrapedProfile profile, Integer requestedYear) {
        return requestedYear == null || profile.passoutYear() == null || requestedYear.equals(profile.passoutYear());
    }

    private void pause() {
        try {
            Thread.sleep(properties.pollInterval());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ProfileSourceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Interrupted while waiting for PhantomBuster", ex);
        }
    }
}
