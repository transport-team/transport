package com.transport.service;

import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.dto.VehicleRequestDTO;
import com.transport.dto.VehicleResponseDTO;

public interface VehicleService {

	// 관리자 차량 목록 조회
	PageResponseDTO<VehicleResponseDTO> getVehicles(PageRequestDTO pageRequestDTO);

	// 관리자 차량 등록
	Long registerVehicle(VehicleRequestDTO vehicleRequestDTO);

	// 관리자 차량 정보 수정
	Long updateVehicle(Long id, VehicleRequestDTO vehicleRequestDTO);

	// 관리자 차량 비활성화
	Long deactivateVehicle(Long id);


}
