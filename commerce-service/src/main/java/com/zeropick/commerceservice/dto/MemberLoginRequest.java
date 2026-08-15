package com.zeropick.commerceservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberLoginRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 120, message = "이메일은 120자 이하여야 합니다.")
        String email,
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(max = 72, message = "비밀번호는 72자 이하여야 합니다.")
        String password
) {
    public MemberLoginRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
