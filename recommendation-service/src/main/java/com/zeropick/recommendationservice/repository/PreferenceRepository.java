package com.zeropick.recommendationservice.repository;

import com.zeropick.recommendationservice.domain.Preference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceRepository extends JpaRepository<Preference, Long> {
}