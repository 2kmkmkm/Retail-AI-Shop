package com.zeropick.recommendationservice.service;

import com.zeropick.recommendationservice.domain.*;
import com.zeropick.recommendationservice.dto.PreferenceRequest;
import com.zeropick.recommendationservice.dto.PreferenceResponse;
import com.zeropick.recommendationservice.repository.PreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;

    // 선호도 등록 또는 갱신
    @Transactional
    public PreferenceResponse saveOrUpdatePreference(PreferenceRequest request) {
        // 1. 기존 선호도 조회 또는 신규 생성
        Preference preference = preferenceRepository.findById(request.getMemberId())
                .orElseGet(() -> Preference.builder()
                        .memberId(request.getMemberId())
                        .categories(new ArrayList<>())
                        .excludedSweeteners(new ArrayList<>())
                        .allergens(new ArrayList<>())
                        .build());

        // 2. 가격 범위 설정
        int priceMin = request.getPriceMin() != null ? request.getPriceMin() : 0;
        int priceMax = request.getPriceMax() != null ? request.getPriceMax() : 100000;

        // 3. 자식 컬럼 리스트 초기화 후 재할당 (Cascade & orphanRemoval 활용)
        preference.getCategories().clear();
        if (request.getCategories() != null) {
            for (String category : request.getCategories()) {
                preference.getCategories().add(
                        PrefCategory.builder()
                                .id(new PrefCategoryId(preference.getMemberId(), category))
                                .preference(preference)
                                .build()
                );
            }
        }

        preference.getExcludedSweeteners().clear();

        if (request.getExcludedSweeteners() != null) {
            for (String sweetener : request.getExcludedSweeteners()) {
                preference.getExcludedSweeteners().add(
                        PrefExcludedSweetener.builder()
                                .id(new PrefExcludedSweetenerId(preference.getMemberId(), sweetener))
                                .preference(preference)
                                .build()
                );
            }
        }

        preference.getAllergens().clear();

        if (request.getAllergens() != null) {
            for (String allergen : request.getAllergens()) {
                preference.getAllergens().add(
                        PrefAllergen.builder()
                                .id(new PrefAllergenId(preference.getMemberId(), allergen))
                                .preference(preference)
                                .build()
                );
            }
        }

        // 4. 빌더로 덮어쓰거나 영속화
        Preference toSave = Preference.builder()
                .memberId(preference.getMemberId())
                .priceMin(priceMin)
                .priceMax(priceMax)
                .categories(preference.getCategories())
                .excludedSweeteners(preference.getExcludedSweeteners())
                .allergens(preference.getAllergens())
                .build();

        Preference saved = preferenceRepository.save(toSave);
        return PreferenceResponse.from(saved);
    }

    // 선호도 조회
    public PreferenceResponse getPreference(Long memberId) {
        Preference preference = preferenceRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("해당 회원의 선호도 정보가 존재하지 않습니다: " + memberId));
        return PreferenceResponse.from(preference);
    }
}