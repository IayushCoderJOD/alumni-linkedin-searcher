package com.alumni.searcher.client.phantombuster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.alumni.searcher.client.ScrapedProfile;
import com.alumni.searcher.config.PhantomBusterProperties;
import com.alumni.searcher.dto.AlumniSearchRequest;
import com.alumni.searcher.exception.ProfileSourceException;
import com.fasterxml.jackson.databind.ObjectMapper;

class PhantomBusterClientTest {

    private static final String BASE_URL = "https://phantom.test/api/v2";
    private static final String STATUS_URL = BASE_URL + "/containers/fetch?id=c-1";
    private static final String RESULT_URL = BASE_URL + "/containers/fetch-result-object?id=c-1";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AlumniSearchRequest request = new AlumniSearchRequest("University of XYZ", "Software Engineer", 2020);

    private MockRestServiceServer server;

    @Test
    void launchesAgentWaitsForItAndMapsTheResult() throws Exception {
        PhantomBusterClient client = clientWith(configured(Duration.ofSeconds(5)));
        String profiles = """
                [
                  {"fullName": "John Doe", "jobTitle": "Senior Software Engineer", "headline": "Engineer at XYZ Corp",
                   "location": "New York, NY", "school": "University of XYZ", "schoolDateRange": "2016 - 2020",
                   "profileUrl": "https://www.linkedin.com/in/john-doe", "connectionDegree": "2nd"},
                  {"firstName": "Jane", "lastName": "Smith", "location": "San Francisco, CA",
                   "profileUrl": "https://www.linkedin.com/in/jane-smith"},
                  {"fullName": "Old Grad", "schoolDateRange": "2001 - 2005"},
                  {"error": "No more results"}
                ]
                """;

        server.expect(requestTo(BASE_URL + "/agents/launch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Phantombuster-Key", "key-123"))
                .andExpect(jsonPath("$.id").value("agent-1"))
                .andExpect(jsonPath("$.argument.sessionCookie").value("cookie-abc"))
                .andExpect(jsonPath("$.argument.search", Matchers.allOf(
                        Matchers.startsWith("https://www.linkedin.com/search/results/people/"),
                        Matchers.containsString("University%20of%20XYZ"),
                        Matchers.containsString("Software%20Engineer"),
                        Matchers.containsString("keywords=2020"))))
                .andRespond(withSuccess("{\"containerId\": \"c-1\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess("{\"status\": \"running\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess("{\"status\": \"finished\", \"exitCode\": 0}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(RESULT_URL))
                .andRespond(withSuccess(resultObject(profiles), MediaType.APPLICATION_JSON));

        List<ScrapedProfile> result = client.search(request);

        server.verify();
        assertThat(result).containsExactly(
                new ScrapedProfile("John Doe", "Senior Software Engineer", "University of XYZ", "New York, NY",
                        "Engineer at XYZ Corp", 2020, "https://www.linkedin.com/in/john-doe"),
                new ScrapedProfile("Jane Smith", "Software Engineer", "University of XYZ", "San Francisco, CA",
                        null, 2020, "https://www.linkedin.com/in/jane-smith"));
    }

    @Test
    void returnsEmptyListWhenAgentProducesNoResult() {
        PhantomBusterClient client = clientWith(configured(Duration.ofSeconds(5)));
        expectLaunch();
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess("{\"status\": \"finished\", \"exitCode\": 0}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(RESULT_URL))
                .andRespond(withSuccess("{\"resultObject\": null}", MediaType.APPLICATION_JSON));

        assertThat(client.search(request)).isEmpty();
    }

    @Test
    void failsWith503WhenCredentialsAreMissing() {
        PhantomBusterClient client = clientWith(new PhantomBusterProperties(
                BASE_URL, "", "", "", 25, Duration.ofMillis(1), Duration.ofSeconds(5)));

        assertThatThrownBy(() -> client.search(request))
                .isInstanceOfSatisfying(ProfileSourceException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        server.verify();
    }

    @Test
    void failsWith502WhenPhantomBusterRejectsTheRequest() {
        PhantomBusterClient client = clientWith(configured(Duration.ofSeconds(5)));
        server.expect(requestTo(BASE_URL + "/agents/launch"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.search(request))
                .isInstanceOfSatisfying(ProfileSourceException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(ex.getMessage()).contains("401");
                });
    }

    @Test
    void failsWith502WhenAgentExitsWithError() {
        PhantomBusterClient client = clientWith(configured(Duration.ofSeconds(5)));
        expectLaunch();
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess("{\"status\": \"finished\", \"exitCode\": 1}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search(request))
                .isInstanceOfSatisfying(ProfileSourceException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void failsWith504WhenAgentDoesNotFinishInTime() {
        PhantomBusterClient client = clientWith(configured(Duration.ZERO));
        expectLaunch();
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess("{\"status\": \"running\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search(request))
                .isInstanceOfSatisfying(ProfileSourceException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }

    @Test
    void failsWith502WhenResultIsNotReadable() {
        PhantomBusterClient client = clientWith(configured(Duration.ofSeconds(5)));
        expectLaunch();
        server.expect(requestTo(STATUS_URL))
                .andRespond(withSuccess("{\"status\": \"finished\", \"exitCode\": 0}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(RESULT_URL))
                .andRespond(withSuccess("{\"resultObject\": \"not json\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search(request))
                .isInstanceOfSatisfying(ProfileSourceException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    private void expectLaunch() {
        server.expect(requestTo(BASE_URL + "/agents/launch"))
                .andRespond(withSuccess("{\"containerId\": \"c-1\"}", MediaType.APPLICATION_JSON));
    }

    private String resultObject(String profilesJson) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("resultObject", profilesJson));
    }

    private PhantomBusterProperties configured(Duration timeout) {
        return new PhantomBusterProperties(
                BASE_URL, "key-123", "agent-1", "cookie-abc", 25, Duration.ofMillis(1), timeout);
    }

    private PhantomBusterClient clientWith(PhantomBusterProperties properties) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new PhantomBusterClient(builder, properties, new PhantomProfileMapper(), objectMapper);
    }
}
