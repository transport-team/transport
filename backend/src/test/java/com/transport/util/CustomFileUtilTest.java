package com.transport.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

// CustomFileUtil 단위 테스트 (이슈 #52 — 검증 실패가 CustomException(400)으로 나가는지 확인)
class CustomFileUtilTest {

	private final CustomFileUtil fileUtil = new CustomFileUtil();

	@Test
	@DisplayName("검증 실패: 파일이 비어있음 (INVALID_INPUT)")
	void validateImage_emptyFile() {
		MockMultipartFile empty = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);

		assertThatThrownBy(() -> fileUtil.validateImage(empty))
				.isInstanceOf(CustomException.class)
				.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT))
				.hasMessage("파일이 비어있습니다.");
	}

	@ParameterizedTest
	@DisplayName("검증 실패: 허용되지 않는 확장자")
	@ValueSource(strings = { "evil.exe", "script.jsp", "photo.svg", "noimage" })
	void validateImage_badExtension(String fileName) {
		MockMultipartFile file = new MockMultipartFile("file", fileName, "image/jpeg", new byte[] { 1, 2, 3 });

		assertThatThrownBy(() -> fileUtil.validateImage(file))
				.isInstanceOf(CustomException.class)
				.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
	}

	@Test
	@DisplayName("검증 실패: MIME 타입이 이미지가 아님")
	void validateImage_badMimeType() {
		MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "text/html", new byte[] { 1, 2, 3 });

		assertThatThrownBy(() -> fileUtil.validateImage(file))
				.isInstanceOf(CustomException.class)
				.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
	}

	@Test
	@DisplayName("검증 실패: 내용이 이미지가 아님 (매직 바이트)")
	void validateImage_notRealImage() throws IOException {
		// 확장자·MIME은 jpg지만 내용이 이미지가 아닌 파일
		MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", "not an image".getBytes());

		assertThatThrownBy(() -> fileUtil.validateImage(file))
				.isInstanceOf(CustomException.class)
				.satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
	}

	@Test
	@DisplayName("확장자 추출: 대문자 소문자화, 점 없으면 null")
	void extractExtension() {
		assertThat(fileUtil.extractExtension("IMG_1234.JPG")).isEqualTo("jpg");
		assertThat(fileUtil.extractExtension("no-ext")).isNull();
		assertThat(fileUtil.extractExtension(null)).isNull();
		assertThat(fileUtil.extractExtension(".hidden")).isNull(); // 숨김 파일 (점이 맨 앞)
	}

	@Test
	@DisplayName("타입 정규화: null·빈값은 ETC")
	void normalizeType() {
		assertThat(fileUtil.normalizeType("pickup")).isEqualTo("PICKUP");
		assertThat(fileUtil.normalizeType(null)).isEqualTo("ETC");
		assertThat(fileUtil.normalizeType(" ")).isEqualTo("ETC");
	}
}
