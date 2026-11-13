package com.alumni.searcher.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.alumni.searcher.dto.AlumniProfileResponse;
import com.alumni.searcher.dto.AlumniSearchRequest;
import com.alumni.searcher.exception.ProfileSourceException;
import com.alumni.searcher.service.AlumniService;

@WebMvcTest(AlumniController.class)
class AlumniControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlumniService alumniService;

    private final AlumniProfileResponse john = new AlumniProfileResponse(
            "John Doe", "Software Engineer", "University of XYZ", "New York, NY",
            "Passionate Software Engineer at XYZ Corp", 2020);

    @Test
    void searchReturnsProfiles() throws Exception {
        when(alumniService.search(new AlumniSearchRequest("University of XYZ", "Software Engineer", 2020)))
                .thenReturn(List.of(john));

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"university": "University of XYZ", "designation": "Software Engineer", "passoutYear": "2020"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].name").value("John Doe"))
                .andExpect(jsonPath("$.data[0].currentRole").value("Software Engineer"))
                .andExpect(jsonPath("$.data[0].linkedinHeadline").value("Passionate Software Engineer at XYZ Corp"))
                .andExpect(jsonPath("$.data[0].passoutYear").value(2020));
    }

    @Test
    void searchWorksWithoutPassoutYear() throws Exception {
        when(alumniService.search(new AlumniSearchRequest("University of XYZ", "Software Engineer", null)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"university": "University of XYZ", "designation": "Software Engineer"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void searchRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"university": " ", "passoutYear": 1800}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errors.university").value("university is required"))
                .andExpect(jsonPath("$.errors.designation").value("designation is required"))
                .andExpect(jsonPath("$.errors.passoutYear").exists());

        verifyNoInteractions(alumniService);
    }

    @Test
    void searchRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"university\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void searchRejectsNonNumericPassoutYear() throws Exception {
        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"university": "University of XYZ", "designation": "Engineer", "passoutYear": "abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void searchSurfacesProfileSourceFailure() throws Exception {
        when(alumniService.search(any()))
                .thenThrow(new ProfileSourceException(HttpStatus.BAD_GATEWAY, "PhantomBuster responded with status 401"));

        mockMvc.perform(post("/api/alumni/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"university": "University of XYZ", "designation": "Engineer"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("PhantomBuster responded with status 401"));
    }

    @Test
    void unexpectedErrorsBecome500() throws Exception {
        when(alumniService.getAll()).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/alumni/all"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void getAllReturnsSavedProfiles() throws Exception {
        when(alumniService.getAll()).thenReturn(List.of(john));

        mockMvc.perform(get("/api/alumni/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].university").value("University of XYZ"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(put("/api/alumni/search"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
