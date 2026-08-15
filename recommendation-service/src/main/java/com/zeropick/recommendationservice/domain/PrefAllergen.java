package com.zeropick.recommendationservice.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pref_allergen")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PrefAllergen {

    @EmbeddedId
    private PrefAllergenId id;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Preference preference;

    public String getAllergen() {
        return id != null ? id.getAllergen() : null;
    }
}