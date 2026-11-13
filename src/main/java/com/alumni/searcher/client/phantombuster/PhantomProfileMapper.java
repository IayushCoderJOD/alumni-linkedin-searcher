package com.alumni.searcher.client.phantombuster;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alumni.searcher.client.ScrapedProfile;
import com.alumni.searcher.dto.AlumniSearchRequest;

@Component
class PhantomProfileMapper {

    private static final Pattern YEAR = Pattern.compile("\\b(19|20)\\d{2}\\b");

    ScrapedProfile toScrapedProfile(PhantomProfile profile, AlumniSearchRequest request) {
        Integer graduationYear = lastYearIn(profile.schoolDateRange());
        return new ScrapedProfile(
                resolveName(profile),
                firstNonBlank(profile.jobTitle(), request.designation().trim()),
                firstNonBlank(profile.school(), request.university().trim()),
                profile.location(),
                profile.headline(),
                graduationYear != null ? graduationYear : request.passoutYear(),
                StringUtils.hasText(profile.profileUrl()) ? profile.profileUrl().trim() : null);
    }

    private String resolveName(PhantomProfile profile) {
        if (StringUtils.hasText(profile.fullName())) {
            return profile.fullName().trim();
        }
        String name = (nullToEmpty(profile.firstName()) + " " + nullToEmpty(profile.lastName())).trim();
        return name.isEmpty() ? null : name;
    }

    private Integer lastYearIn(String dateRange) {
        if (dateRange == null) {
            return null;
        }
        Integer year = null;
        Matcher matcher = YEAR.matcher(dateRange);
        while (matcher.find()) {
            year = Integer.valueOf(matcher.group());
        }
        return year;
    }

    private String firstNonBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
