package com.transport.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

import net.coobird.thumbnailator.Thumbnails;

import lombok.extern.log4j.Log4j2;

// 파일 유틸리티 (확장자 검증, 썸네일 생성 등) — LocalFileStorage가 주입받으므로 빈으로 등록
@Component
@Log4j2
public class CustomFileUtil {

	// 허용 이미지 확장자
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
	// 허용 MIME 타입
	private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
			"image/jpeg", "image/png", "image/webp", "image/gif"
	);

	/**
	 * 이미지 파일 검증 (확장자 + MIME 타입)
	 * @param file 업로드 파일
	 * @throws CustomException 검증 실패 시 (400 INVALID_INPUT — 이슈 #52)
	 */
	public void validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "파일이 비어있습니다.");
		}

		// 1. 확장자 검증
		String extension = extractExtension(file.getOriginalFilename());
		if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "허용되지 않는 파일 형식입니다. (jpg, jpeg, png, webp, gif만 가능)");
		}

		// 2. MIME 타입 검증
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "허용되지 않는 파일 형식입니다.");
		}

		// 3. 매직 바이트 검증 (실제 파일 내용 확인)
		try {
			BufferedImage image = ImageIO.read(file.getInputStream());
			if (image == null) {
				throw new CustomException(ErrorCode.INVALID_INPUT, "유효하지 않은 이미지 파일입니다.");
			}
		} catch (IOException e) {
			log.error("이미지 검증 중 오류", e);
			throw new CustomException(ErrorCode.INVALID_INPUT, "이미지 파일 검증 실패");
		}
	}

	/**
	 * 썸네일 생성 (Thumbnailator 사용)
	 * @param source 원본 파일 경로
	 * @param target 썸네일 저장 경로
	 * @param width 썸네일 너비 (높이는 비율 유지)
	 */
	public void generateThumbnail(Path source, Path target, int width) {
		try {
			Thumbnails.of(source.toFile())
					.size(width, width)
					.keepAspectRatio(true)
					.outputQuality(0.8)
					.toFile(target.toFile());
			log.debug("썸네일 생성 완료: {}", target.getFileName());
		} catch (IOException e) {
			log.error("썸네일 생성 실패: {}", source.getFileName(), e);
			// 썸네일 생성 실패해도 원본 저장은 유지 (예외 전파 안 함)
		}
	}

	/**
	 * 파일명에서 확장자 추출
	 * @param fileName 파일명
	 * @return 확장자 (점 제외, 소문자) 또는 null
	 */
	public String extractExtension(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return null;
		}
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
			return null;
		}
		return fileName.substring(dotIndex + 1).toLowerCase();
	}

	/**
	 * 파일 타입 폴더명 정규화
	 * @param type 원본 타입 (PICKUP, RECEIPT, ETC 등)
	 * @return 대문자 폴더명
	 */
	public String normalizeType(String type) {
		if (type == null || type.isBlank()) {
			return "ETC";
		}
		return type.toUpperCase();
	}
}