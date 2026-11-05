package com.transport.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.storage.FileStorage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

// 파일 조회 컨트롤러 (인증 없이 접근 가능 - <img> 태그용)
@Tag(name = "File API", description = "업로드된 파일 조회 (인증 불필요)")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

	private final FileStorage fileStorage;

	@Operation(summary = "파일 조회", description = "업로드된 이미지 파일 조회 (원본 또는 썸네일 s_ 접두어). 인증 없이 접근 가능.")
	@GetMapping("/{fileName}")
	public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
		Resource resource = fileStorage.load(fileName);

		// Content-Type 결정 (확장자 기준)
		String contentType = determineContentType(fileName);

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header("Cache-Control", "public, max-age=31536000") // 1년 캐시
				.body(resource);
	}

	private String determineContentType(String fileName) {
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
		return switch (ext) {
			case "jpg", "jpeg" -> "image/jpeg";
			case "png" -> "image/png";
			case "webp" -> "image/webp";
			case "gif" -> "image/gif";
			default -> "application/octet-stream";
		};
	}
}