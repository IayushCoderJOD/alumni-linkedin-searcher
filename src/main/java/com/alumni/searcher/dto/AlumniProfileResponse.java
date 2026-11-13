package com.alumni.searcher.dto;

import com.alumni.searcher.entity.AlumniProfile;

public record AlumniProfileResponse(
        String name,
        String currentRole,
        String university,
        String location,
        String linkedinHeadline,
        Integer passoutYear) {

    public static AlumniProfileResponse from(AlumniProfile profile) {
        return new AlumniProfileResponse(
                profile.getName(),
                profile.getCurrentRole(),
                profile.getUniversity(),
                profile.getLocation(),
                profile.getLinkedinHeadline(),
                profile.getPassoutYear());
    }
}
