package com.zeropick.recommendationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceRequest {

    private Long memberId;
    private Integer priceMin;
    private Integer priceMax;
    private List<String> categories;
    private List<String> excludedSweeteners;
    private List<String> allergens;
}