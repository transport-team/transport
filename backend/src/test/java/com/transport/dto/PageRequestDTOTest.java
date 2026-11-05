package com.transport.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

// 쿼리 파라미터 바인딩 · 기본값 · 범위 검사 (보안 필터는 끄고 확인)
@WebMvcTest(PageRequestDTOTest.TestController.class)  // 지정하지 않으면 모든 컨트롤러를 불러와 서비스 빈이 없다고 실패한다
@AutoConfigureMockMvc(addFilters = false)
@Import(PageRequestDTOTest.TestController.class)
class PageRequestDTOTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void bindsQueryParams() throws Exception {
		mockMvc.perform(get("/test/page").param("page", "2").param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(2))
				.andExpect(jsonPath("$.size").value(5));
	}

	@Test
	void defaults() throws Exception {
		mockMvc.perform(get("/test/page"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(10));
	}

	@Test
	void rejectsOutOfRange() throws Exception {
		mockMvc.perform(get("/test/page").param("page", "0"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/test/page").param("size", "101"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void pageableStartsFromZero() {
		Pageable pageable = PageRequestDTO.builder().page(3).build().getPageable(Sort.by("id").descending());

		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(10);
		assertThat(pageable.getSort()).isEqualTo(Sort.by("id").descending());
	}

	@RestController
	static class TestController {

		@GetMapping("/test/page")
		PageRequestDTO page(@Valid PageRequestDTO pageRequestDTO) {
			return pageRequestDTO;
		}
	}
}
