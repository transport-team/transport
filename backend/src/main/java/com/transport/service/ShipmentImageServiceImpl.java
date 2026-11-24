package com.transport.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentImage;
import com.transport.domain.ShipmentImage;
import com.transport.domain.enums.ShipmentImageType;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.ShipmentImageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ShipmentImageRepository;
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

	private final ShipmentService shipmentService;  // 배송 = 팀원 3 약속 메서드 (저장소 직접 사용 정리, #94)
	private final ShipmentImageRepository shipmentImageRepository;
	private final com.transport.repository.ShipmentRepository shipmentRepository;
	private final DispatchService dispatchService;
	private final FileStorage fileStorage;
	private final CustomFileUtil customFileUtil;

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentImageResponseDTO> getImages(Long shipmentId, Long memberId, MemberRole role) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));
        boolean admin = role == MemberRole.ADMIN;
        boolean customer = shipment.getCustomer() != null && shipment.getCustomer().getId().equals(memberId);
        Long assignedDriverId = dispatchService.getActiveDriver(shipmentId);
        boolean driver = assignedDriverId != null && assignedDriverId.equals(memberId);
        List<ShipmentImage> images = shipmentImageRepository.findByShipmentIdOrderByTypeAscCreatedAtAsc(shipmentId);
        if (admin || customer || driver) {
            return images.stream().map(ShipmentImageResponseDTO::from).toList();
        }
        // 배차가 해제된 지난 운송이어도, 과거에 본인이 올린 증빙은 조회 가능하다.
        // 다른 기사가 재배차 후 업로드한 사진까지 공개하지는 않는다.
        if (role == MemberRole.DRIVER) {
            List<ShipmentImageResponseDTO> ownImages = images.stream()
                    .filter(image -> image.getUploadedBy() != null
                            && memberId.equals(image.getUploadedBy().getId()))
                    .map(ShipmentImageResponseDTO::from)
                    .toList();
            if (!ownImages.isEmpty()) {
                return ownImages;
            }
        }
        throw new CustomException(ErrorCode.FORBIDDEN, "본인이 관련된 배송 사진만 조회할 수 있습니다.");
    }

	@Override
	public List<ShipmentImageResponseDTO> upload(Long shipmentId, List<MultipartFile> files,
			ShipmentImageType type, Long memberId) {
		// 1. 배송 조회 (없으면 SHIPMENT_NOT_FOUND)
		Shipment shipment = shipmentService.getShipment(shipmentId);

		// 2. 배정 기사인지 확인 (getActiveDriver는 기사의 member.id를 반환 — 약속 표 5주차)
		Long assignedDriverId = dispatchService.getActiveDriver(shipmentId);
		if (assignedDriverId == null || !assignedDriverId.equals(memberId)) {
			throw new CustomException(ErrorCode.NOT_ASSIGNED_DRIVER);
		}

		// 3. 파일 개수 확인 (빈 요청 거절)
		if (files == null || files.isEmpty()) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "사진 파일이 없습니다.");
		}

        // 저장된 증빙은 변경/삭제하지 않는다. 사진 추가도 해당 단계에서만 허용한다.
        // 기사가 배송 완료를 등록하면(DELIVERED) 고객 확인 전이라도 사진을 잠근다.
        ShipmentStatus status = shipment.getStatus();
        boolean uploadAllowed = switch (type) {
            case PICKUP -> status == ShipmentStatus.ASSIGNED;
            case RECEIPT -> status == ShipmentStatus.ARRIVED;
            case ETC -> status == ShipmentStatus.ASSIGNED
                    || status == ShipmentStatus.PICKED_UP
                    || status == ShipmentStatus.IN_TRANSIT
                    || status == ShipmentStatus.ARRIVED;
        };
        if (!uploadAllowed) {
            throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE,
                    "현재 배송 단계에서는 사진을 추가할 수 없습니다. 단계 확정 전 사진을 모두 등록해 주세요.");
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