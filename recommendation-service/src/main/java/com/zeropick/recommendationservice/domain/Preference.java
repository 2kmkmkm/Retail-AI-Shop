package com.zeropick.recommendationservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "preference")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Preference {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Builder.Default
    @Column(name = "price_min", nullable = false)
    private Integer priceMin = 0;

    @Builder.Default
    @Column(name = "price_max", nullable = false)
    private Integer priceMax = 100000;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "preference", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrefCategory> categories = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "preference", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrefExcludedSweetener> excludedSweeteners = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "preference", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrefAllergen> allergens = new ArrayList<>();
}