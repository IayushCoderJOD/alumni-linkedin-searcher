package com.alumni.searcher.client;

public record ScrapedProfile(
        String name,
        String currentRole,
        String university,
        String location,
        String headline,
        Integer passoutYear,
        String profileUrl) {
}
