package com.zeropick.recommendationservice.domain;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PrefAllergenId implements Serializable {
    private Long memberId;
    private String allergen;
}