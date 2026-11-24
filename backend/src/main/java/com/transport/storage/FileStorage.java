package com.transport.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

// 파일 저장소 인터페이스 (로컬/S3 교체 가능)
public interface FileStorage {

	/**
	 * 파일 저장
	 * @param file 업로드 파일
	 * @param type 파일 타입 (PICKUP, RECEIPT, ETC) - 폴더 분리용
	 * @return 저장된 파일명 (UUID_원본명)
	 */
	String save(MultipartFile file, String type);

	/**
	 * 파일 조회
	 * @param fileName 저장된 파일명 (UUID_원본명 또는 s_UUID_원본명)
	 * @return 리소스
	 */
	Resource load(String fileName);

	/**
	 * 파일 삭제
	 * @param fileName 저장된 파일명
	 */
	void delete(String fileName);
}