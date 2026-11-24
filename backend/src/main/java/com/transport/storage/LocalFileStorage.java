package com.transport.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.util.CustomFileUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 로컬 파일 시스템 저장소 구현
@Service
@RequiredArgsConstructor
@Log4j2
public class LocalFileStorage implements FileStorage {

	private final CustomFileUtil fileUtil;

	@Value("${app.file.upload-dir:./upload}")
	private String uploadDir;

	@Value("${app.file.thumbnail-width:300}")
	private int thumbnailWidth;

	@Override
	public String save(MultipartFile file, String type) {
		// 1. 이미지 파일 검증
		fileUtil.validateImage(file);

		// 2. 타입별 폴더 경로 생성 (PICKUP, RECEIPT, ETC)
		Path typeDir = Paths.get(uploadDir, type.toUpperCase());
		try {
			Files.createDirectories(typeDir);
		} catch (IOException e) {
			log.error("업로드 디렉토리 생성 실패: {}", typeDir, e);
			throw new RuntimeException("파일 저장소 초기화 실패", e);
		}

		// 3. UUID_원본명 파일명 생성
		String originalName = file.getOriginalFilename();
		String extension = fileUtil.extractExtension(originalName);
		String storedName = UUID.randomUUID() + "_" + originalName;
		Path targetPath = typeDir.resolve(storedName);

		// 4. 파일 저장
		try {
			Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			log.error("파일 저장 실패: {}", storedName, e);
			throw new RuntimeException("파일 저장 실패", e);
		}

		// 5. 썸네일 생성 (s_ 접두어)
		Path thumbnailPath = typeDir.resolve("s_" + storedName);
		fileUtil.generateThumbnail(targetPath, thumbnailPath, thumbnailWidth);

		log.info("파일 저장 완료: type={}, fileName={}, thumb={}", type, storedName, thumbnailPath.getFileName());
		return storedName;
	}

	@Override
	public Resource load(String fileName) {
		// 썸네일인지 원본인지 확인 (s_ 접두어)
		boolean isThumbnail = fileName.startsWith("s_");
		String actualName = isThumbnail ? fileName.substring(2) : fileName;

		// 타입 폴더 찾기 (PICKUP, RECEIPT, ETC 순회)
		String[] types = {"PICKUP", "RECEIPT", "ETC"};
		for (String type : types) {
			Path filePath = Paths.get(uploadDir, type, fileName);
			if (Files.exists(filePath)) {
				try {
					Resource resource = new UrlResource(filePath.toUri());
					if (resource.exists() && resource.isReadable()) {
						return resource;
					}
				} catch (MalformedURLException e) {
					log.warn("파일 리소스 변환 실패: {}", filePath, e);
				}
			}
		}

		// 없는 파일은 404 (이슈 #52: RuntimeException이 500으로 나가던 것 수정)
		throw new CustomException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다.");
	}

	@Override
	public void delete(String fileName) {
		boolean isThumbnail = fileName.startsWith("s_");
		String actualName = isThumbnail ? fileName.substring(2) : fileName;

		String[] types = {"PICKUP", "RECEIPT", "ETC"};
		for (String type : types) {
			// 원본 파일 삭제
			Path originalPath = Paths.get(uploadDir, type, actualName);
			try {
				Files.deleteIfExists(originalPath);
			} catch (IOException e) {
				log.warn("원본 파일 삭제 실패: {}", originalPath, e);
			}

			// 썸네일 파일 삭제
			Path thumbPath = Paths.get(uploadDir, type, "s_" + actualName);
			try {
				Files.deleteIfExists(thumbPath);
			} catch (IOException e) {
				log.warn("썸네일 파일 삭제 실패: {}", thumbPath, e);
			}
		}
	}
}