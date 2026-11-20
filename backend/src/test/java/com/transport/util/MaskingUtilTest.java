package com.transport.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// 이름 가리기 (11-19 결정 4): 처음 · 마지막 글자만 남기고 가운데를 *로
class MaskingUtilTest {

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
			"김민수, 김*수",
			"남궁현석, 남**석",
			"이민, 이*",
			"김, *",
			"Tom Lee, T*****e",
	})
	@DisplayName("처음과 마지막 글자만 남긴다 (두 글자는 뒤를, 한 글자는 전부 가린다)")
	void maskName(String name, String expected) {
		assertThat(MaskingUtil.maskName(name)).isEqualTo(expected);
	}

	@Test
	@DisplayName("앞뒤 공백은 떼고 가린다")
	void stripsSpaces() {
		assertThat(MaskingUtil.maskName("  김민수 ")).isEqualTo("김*수");
	}

	@Test
	@DisplayName("char 2개짜리 글자(이모지)도 반으로 자르지 않고 한 글자로 센다")
	void countsCodePoints() {
		assertThat(MaskingUtil.maskName("김😀수")).isEqualTo("김*수");
		assertThat(MaskingUtil.maskName("😀민")).isEqualTo("😀*");
	}

	@Test
	@DisplayName("이름이 없거나 빈칸이면 그대로 돌려준다 (오류 없이)")
	void nullOrBlank() {
		assertThat(MaskingUtil.maskName(null)).isNull();
		assertThat(MaskingUtil.maskName("")).isEmpty();
		assertThat(MaskingUtil.maskName("   ")).isEqualTo("   ");
	}
}
