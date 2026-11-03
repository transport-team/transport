package com.transport.controller.advice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

// 에러 응답 형식 확인 (보안 필터는 끄고 예외 처리만 본다)
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void customException() throws Exception {
		mockMvc.perform(get("/test/not-found"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value(ErrorCode.NOT_FOUND.getMessage()));
	}

	@Test
	void customExceptionWithMessage() throws Exception {
		mockMvc.perform(get("/test/custom-message"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"))
				.andExpect(jsonPath("$.message").value("본인 배송만 조회할 수 있습니다."));
	}

	@Test
	void validationFailure() throws Exception {
		mockMvc.perform(post("/test/valid").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.message").value("이름을 입력하세요."));
	}

	@Test
	void malformedJson() throws Exception {
		mockMvc.perform(post("/test/valid").contentType(MediaType.APPLICATION_JSON).content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void unknownUrl() throws Exception {
		mockMvc.perform(get("/test/unknown"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void unsupportedMethod() throws Exception {
		mockMvc.perform(delete("/test/not-found"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void unexpectedException() throws Exception {
		mockMvc.perform(get("/test/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
	}

	@Test
	void accessDeniedIsLeftToSpringSecurity() {
		assertThatThrownBy(() -> mockMvc.perform(get("/test/denied")))
				.hasCauseInstanceOf(AccessDeniedException.class);
	}

	@RestController
	static class TestController {

		@GetMapping("/test/not-found")
		void notFound() {
			throw new CustomException(ErrorCode.NOT_FOUND);
		}

		@GetMapping("/test/custom-message")
		void customMessage() {
			throw new CustomException(ErrorCode.FORBIDDEN, "본인 배송만 조회할 수 있습니다.");
		}

		@PostMapping("/test/valid")
		void valid(@Valid @RequestBody NameRequest request) {
		}

		@GetMapping("/test/boom")
		void boom() {
			throw new IllegalStateException("boom");
		}

		@GetMapping("/test/denied")
		void denied() {
			throw new AccessDeniedException("denied");
		}
	}

	record NameRequest(@NotBlank(message = "이름을 입력하세요.") String name) {
	}
}
