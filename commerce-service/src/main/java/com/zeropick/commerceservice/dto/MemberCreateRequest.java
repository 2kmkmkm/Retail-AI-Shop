package com.zeropick.commerceservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberCreateRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 120, message = "이메일은 120자 이하여야 합니다.")
        String email,
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 72, message = "비밀번호는 72자 이하여야 합니다.")
        String password,
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 40, message = "이름은 40자 이하여야 합니다.")
        String name
) {
    public MemberCreateRequest {
        if (email != null) {
            email = email.trim();
        }
        if (name != null) {
            name = name.trim();
        }
    }
}
