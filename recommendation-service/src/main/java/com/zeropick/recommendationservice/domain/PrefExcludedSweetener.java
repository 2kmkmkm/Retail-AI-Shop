package com.zeropick.recommendationservice.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pref_excluded_sweetener")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PrefExcludedSweetener {

    @EmbeddedId
    private PrefExcludedSweetenerId id;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Preference preference;

    public String getSweetener() {
        return id != null ? id.getSweetener() : null;
    }
}