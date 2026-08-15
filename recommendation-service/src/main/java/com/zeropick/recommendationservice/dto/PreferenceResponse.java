package com.zeropick.recommendationservice.dto;

import com.zeropick.recommendationservice.domain.Preference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceResponse {

    private Long memberId;
    private Integer priceMin;
    private Integer priceMax;
    private List<String> categories;
    private List<String> excludedSweeteners;
    private List<String> allergens;
    private LocalDateTime updatedAt;

    public static PreferenceResponse from(Preference preference) {
        return PreferenceResponse.builder()
                .memberId(preference.getMemberId())
                .priceMin(preference.getPriceMin())
                .priceMax(preference.getPriceMax())
                .categories(preference.getCategories().stream()
                        .map(c -> c.getId().getCategory())
                        .toList())
                .excludedSweeteners(preference.getExcludedSweeteners().stream()
                        .map(s -> s.getId().getSweetener())
                        .toList())
                .allergens(preference.getAllergens().stream()
                        .map(a -> a.getId().getAllergen())
                        .toList())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}