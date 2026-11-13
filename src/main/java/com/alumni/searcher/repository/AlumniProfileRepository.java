package com.alumni.searcher.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alumni.searcher.entity.AlumniProfile;

public interface AlumniProfileRepository extends JpaRepository<AlumniProfile, Long> {

    List<AlumniProfile> findByProfileUrlIn(Collection<String> profileUrls);
}
