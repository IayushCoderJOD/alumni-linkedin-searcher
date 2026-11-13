package com.alumni.searcher.client.phantombuster;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record PhantomProfile(
        String fullName,
        String firstName,
        String lastName,
        String headline,
        @JsonAlias({"job", "currentJob"}) String jobTitle,
        String location,
        String school,
        String schoolDateRange,
        @JsonAlias("url") String profileUrl) {
}
