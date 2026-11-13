package com.alumni.searcher.client;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.alumni.searcher.dto.AlumniSearchRequest;

/**
 * Fixed sample data for running without PhantomBuster. Searches filter this list, so a
 * search for one university must not return alumni of the other.
 */
@Component
@Profile("mock")
public class MockLinkedInProfileClient implements LinkedInProfileClient {

    private static final List<ScrapedProfile> SAMPLE_PROFILES = List.of(
            new ScrapedProfile("John Doe", "Software Engineer", "IIT Delhi", "New York, NY",
                    "Passionate Software Engineer at XYZ Corp", 2020,
                    "https://www.linkedin.com/in/john-doe-iit-delhi"),
            new ScrapedProfile("Jane Smith", "Software Engineer", "IIT Delhi", "San Francisco, CA",
                    "Software Engineer | AI Enthusiast", 2020,
                    "https://www.linkedin.com/in/jane-smith-iit-delhi"),
            new ScrapedProfile("Rahul Verma", "Software Engineer", "IIT Delhi", "Bengaluru, India",
                    "Software Engineer at Acme Labs", 2020,
                    "https://www.linkedin.com/in/rahul-verma-iit-delhi"),
            new ScrapedProfile("Priya Sharma", "Software Engineer", "IIT Roorkee", "Hyderabad, India",
                    "Software Engineer at Globex", 2020,
                    "https://www.linkedin.com/in/priya-sharma-iit-roorkee"),
            new ScrapedProfile("Arjun Mehta", "Software Engineer", "IIT Roorkee", "Pune, India",
                    "Software Engineer | Backend and Distributed Systems", 2020,
                    "https://www.linkedin.com/in/arjun-mehta-iit-roorkee"),
            new ScrapedProfile("Sneha Gupta", "Software Engineer", "IIT Roorkee", "London, UK",
                    "Software Engineer at Initech", 2020,
                    "https://www.linkedin.com/in/sneha-gupta-iit-roorkee"));

    @Override
    public List<ScrapedProfile> search(AlumniSearchRequest request) {
        return SAMPLE_PROFILES.stream()
                .filter(profile -> containsIgnoreCase(profile.university(), request.university()))
                .filter(profile -> containsIgnoreCase(profile.currentRole(), request.designation()))
                .filter(profile -> request.passoutYear() == null
                        || request.passoutYear().equals(profile.passoutYear()))
                .toList();
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }
}
