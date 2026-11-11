package com.transport.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentImage;
import com.transport.domain.enums.ShipmentImageType;
import com.transport.dto.ShipmentImageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ShipmentImageRepository;
import com.transport.repository.ShipmentRepository;
import com.transport.storage.FileStorage;
import com.transport.util.CustomFileUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 배송 사진 서비스 구현 (팀원 4 — 5-1)
// 권한: 배정 기사만 (DispatchService.getActiveDriver — 5주차 약속, 기사 회원 ID 반환)
@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class ShipmentImageServiceImpl implements ShipmentImageService {

	private final ShipmentRepository shipmentRepository;
	private final ShipmentImageRepository shipmentImageRepository;
	private final DispatchService dispatchService;
	private final FileStorage fileStorage;
	private final CustomFileUtil customFileUtil;

	@Override
	public List<ShipmentImageResponseDTO> upload(Long shipmentId, List<MultipartFile> files,
			ShipmentImageType type, Long memberId) {
		// 1. 배송 조회
		Shipment shipment = shipmentRepository.findById(shipmentId)
				.orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));

		// 2. 배정 기사인지 확인 (getActiveDriver는 기사의 member.id를 반환 — 약속 표 5주차)
		Long assignedDriverId = dispatchService.getActiveDriver(shipmentId);
		if (assignedDriverId == null || !assignedDriverId.equals(memberId)) {
			throw new CustomException(ErrorCode.NOT_ASSIGNED_DRIVER);
		}

		// 3. 파일 개수 확인 (빈 요청 거절)
		if (files == null || files.isEmpty()) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "사진 파일이 없습니다.");
		}

		// 4. 파일별 검증 후 저장 (FileStorage: UUID_원본명 + s_ 썸네일)
		Member uploader = Member.builder().id(memberId).build(); // 업로더 기록용 최소 필드
		List<ShipmentImageResponseDTO> result = new ArrayList<>();
		for (MultipartFile file : files) {
			customFileUtil.validateImage(file); // 400 INVALID_INPUT (확장자·MIME·매직 바이트)
			String fileName = fileStorage.save(file, type.name());

			ShipmentImage saved = shipmentImageRepository.save(ShipmentImage.builder()
					.shipment(shipment)
					.type(type)
					.fileName(fileName)
					.uploadedBy(uploader)
					.build());

			result.add(ShipmentImageResponseDTO.from(saved));
			log.info("사진 업로드: shipmentId={}, type={}, fileName={}, memberId={}",
					shipmentId, type, fileName, memberId);
		}

		return result;
	}
}