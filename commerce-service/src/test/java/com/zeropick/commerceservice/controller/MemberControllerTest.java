package com.zeropick.commerceservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeropick.commerceservice.entity.Member;
import com.zeropick.commerceservice.repository.MemberRepository;
import com.zeropick.commerceservice.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    void createsMemberWithHashedPassword() throws Exception {
        Map<String, String> request = Map.of(
                "email", "  USER@Example.com ",
                "password", "plain-password",
                "name", "제로픽"
        );

        mockMvc.perform(post("/commerce-service/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("제로픽"))
                .andExpect(jsonPath("$.password").doesNotExist());

        Member savedMember = memberRepository.findAll().get(0);
        assertThat(savedMember.getPassword()).isNotEqualTo("plain-password");
        assertThat(passwordEncoder.matches("plain-password", savedMember.getPassword())).isTrue();
    }

    @Test
    void returnsConflictWhenEmailAlreadyExists() throws Exception {
        memberRepository.save(Member.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password"))
                .name("기존 회원")
                .build());

        Map<String, String> request = Map.of(
                "email", "USER@example.com",
                "password", "another-password",
                "name", "새 회원"
        );

        mockMvc.perform(post("/commerce-service/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));

        assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    void returnsBadRequestForInvalidInput() throws Exception {
        Map<String, String> request = Map.of(
                "email", "invalid-email",
                "password", "",
                "name", ""
        );

        mockMvc.perform(post("/commerce-service/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void logsInWithNormalizedEmailAndReturnsMemberId() throws Exception {
        Member member = memberRepository.save(Member.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("correct-password"))
                .name("제로픽")
                .build());

        Map<String, String> request = Map.of(
                "email", "  USER@Example.com ",
                "password", "correct-password"
        );

        String responseBody = mockMvc.perform(post("/commerce-service/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(member.getId()))
                .andExpect(jsonPath("$.name").value("제로픽"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(responseBody).path("token").asText();
        Claims claims = jwtTokenProvider.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo(member.getId().toString());
        assertThat(((Number) claims.get("memberId")).longValue()).isEqualTo(member.getId());
        assertThat(claims.get("name", String.class)).isEqualTo("제로픽");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void returnsUnauthorizedWhenPasswordDoesNotMatch() throws Exception {
        memberRepository.save(Member.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("correct-password"))
                .name("제로픽")
                .build());

        Map<String, String> request = Map.of(
                "email", "user@example.com",
                "password", "wrong-password"
        );

        mockMvc.perform(post("/commerce-service/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void returnsUnauthorizedWhenEmailDoesNotExist() throws Exception {
        Map<String, String> request = Map.of(
                "email", "missing@example.com",
                "password", "any-password"
        );

        mockMvc.perform(post("/commerce-service/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
