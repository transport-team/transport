package com.transport.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size; // @Column 대신 @Size 사용
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentRequestDTO {

    // 차종 및 화물 종류 PK (Shipment 엔티티 연관관계 매핑용)
    @NotNull(message = "요청 차종 선택은 필수입니다.")
    private Long vehicleTypeId;

    @NotNull(message = "화물 유형 선택은 필수입니다.")
    private Long cargoTypeId;

    // 출발지 정보
    @NotBlank(message = "출발지 주소는 필수입니다.")
    private String originAddress;

    private String originDetail;

    private Double originLat;
    private Double originLng;

    // 도착지 정보
    @NotBlank(message = "도착지 주소는 필수입니다.")
    private String destAddress;

    private String destDetail;

    private Double destLat;
    private Double destLng;

    private Double distanceKm;   // 거리 (운임 검증용)
    private Integer durationMin; // 예상 소요 시간

    // 화물 상세 정보
    @NotBlank(message = "화물명은 필수입니다.")
    private String cargoName;

    @NotNull(message = "화물 무게는 필수입니다.")
    @Min(value = 1, message = "화물 무게는 1kg 이상이어야 합니다.")
    private Integer cargoWeightKg;

    @NotNull(message = "희망 상차 일시는 필수입니다.")
    private LocalDateTime pickupAt;

    // 받는 사람 정보
    @NotBlank(message = "받는 사람 이름은 필수입니다.")
    private String receiverName;

    @NotBlank(message = "받는 사람 연락처는 필수입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    private String receiverPhone;

    @Size(max = 500, message = "요청사항은 500자 이하로 작성해주세요.") // @Column -> @Size 변경
    private String memo; // 요청사항

    @NotNull(message = "최종 결제 예정 금액은 필수입니다.")
    private Integer fare; // 예상 운임
}