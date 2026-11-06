package com.transport.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecuritySwaggerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Swagger UI 및 API Docs 접속은 인증 없이 200 OK를 반환해야 한다.")
    void swaggerUrlPermitAllTest() throws Exception {
        // 1. OpenAPI Docs json 접근 테스트
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        // 2. Swagger UI 페이지 접근 테스트
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}