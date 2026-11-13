package com.alumni.searcher.client;

import java.util.List;

import com.alumni.searcher.dto.AlumniSearchRequest;

public interface LinkedInProfileClient {

    List<ScrapedProfile> search(AlumniSearchRequest request);
}
