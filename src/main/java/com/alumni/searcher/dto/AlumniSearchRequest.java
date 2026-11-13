package com.alumni.searcher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlumniSearchRequest(
        @NotBlank(message = "university is required")
        @Size(max = 150, message = "university must be at most 150 characters")
        String university,

        @NotBlank(message = "designation is required")
        @Size(max = 150, message = "designation must be at most 150 characters")
        String designation,

        @Min(value = 1950, message = "passoutYear must be 1950 or later")
        @Max(value = 2100, message = "passoutYear must be 2100 or earlier")
        Integer passoutYear) {
}
