package com.zeropick.recommendationservice.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pref_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PrefCategory {

    @EmbeddedId
    private PrefCategoryId id;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Preference preference;

    public String getCategory() {
        return id != null ? id.getCategory() : null;
    }
}