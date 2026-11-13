package com.alumni.searcher.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.alumni.searcher.client.LinkedInProfileClient;
import com.alumni.searcher.client.ScrapedProfile;
import com.alumni.searcher.dto.AlumniProfileResponse;
import com.alumni.searcher.dto.AlumniSearchRequest;
import com.alumni.searcher.entity.AlumniProfile;
import com.alumni.searcher.repository.AlumniProfileRepository;

@Service
public class AlumniService {

    private final LinkedInProfileClient profileClient;
    private final AlumniProfileRepository repository;

    public AlumniService(LinkedInProfileClient profileClient, AlumniProfileRepository repository) {
        this.profileClient = profileClient;
        this.repository = repository;
    }

    public List<AlumniProfileResponse> search(AlumniSearchRequest request) {
        List<ScrapedProfile> scraped = profileClient.search(request);
        if (scraped.isEmpty()) {
            return List.of();
        }
        return toResponses(repository.saveAll(mergeWithSaved(scraped)));
    }

    public List<AlumniProfileResponse> getAll() {
        return toResponses(repository.findAll());
    }

    private List<AlumniProfile> mergeWithSaved(List<ScrapedProfile> scraped) {
        Map<String, ScrapedProfile> byUrl = new LinkedHashMap<>();
        List<ScrapedProfile> withoutUrl = new ArrayList<>();
        for (ScrapedProfile profile : scraped) {
            if (profile.profileUrl() == null) {
                withoutUrl.add(profile);
            } else {
                byUrl.putIfAbsent(profile.profileUrl(), profile);
            }
        }

        Map<String, AlumniProfile> saved = repository.findByProfileUrlIn(byUrl.keySet()).stream()
                .collect(Collectors.toMap(AlumniProfile::getProfileUrl, Function.identity()));

        List<AlumniProfile> merged = new ArrayList<>();
        byUrl.forEach((url, profile) -> merged.add(apply(profile, saved.getOrDefault(url, new AlumniProfile()))));
        withoutUrl.forEach(profile -> merged.add(apply(profile, new AlumniProfile())));
        return merged;
    }

    private AlumniProfile apply(ScrapedProfile source, AlumniProfile target) {
        target.setName(source.name());
        target.setCurrentRole(source.currentRole());
        target.setUniversity(source.university());
        target.setLocation(source.location());
        target.setLinkedinHeadline(source.headline());
        target.setPassoutYear(source.passoutYear());
        target.setProfileUrl(source.profileUrl());
        return target;
    }

    private List<AlumniProfileResponse> toResponses(List<AlumniProfile> profiles) {
        return profiles.stream().map(AlumniProfileResponse::from).toList();
    }
}
