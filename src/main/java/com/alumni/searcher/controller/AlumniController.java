package com.alumni.searcher.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.searcher.dto.AlumniProfileResponse;
import com.alumni.searcher.dto.AlumniSearchRequest;
import com.alumni.searcher.dto.ApiResponse;
import com.alumni.searcher.service.AlumniService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/alumni")
public class AlumniController {

    private final AlumniService alumniService;

    public AlumniController(AlumniService alumniService) {
        this.alumniService = alumniService;
    }

    @PostMapping("/search")
    public ApiResponse<List<AlumniProfileResponse>> search(@Valid @RequestBody AlumniSearchRequest request) {
        return ApiResponse.success(alumniService.search(request));
    }

    @GetMapping("/all")
    public ApiResponse<List<AlumniProfileResponse>> getAll() {
        return ApiResponse.success(alumniService.getAll());
    }
}
