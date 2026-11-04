package com.transport.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Driver;
import com.transport.domain.Vehicle;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.VehicleStatus;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.dto.VehicleRequestDTO;
import com.transport.dto.VehicleResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleServiceImpl implements VehicleService {

	private final VehicleRepository vehicleRepository;
	private final VehicleTypeService vehicleTypeService;
	private final DriverService driverService;

	@Override
	public PageResponseDTO<VehicleResponseDTO> getVehicles(PageRequestDTO pageRequestDTO) {

		Page<Vehicle> result = vehicleRepository.findAll(pageRequestDTO.getPageable(Sort.by("id").descending()));

		List<VehicleResponseDTO> dtoList = result.getContent().stream().map(this::entityToDTO).toList();

		return PageResponseDTO.<VehicleResponseDTO>withAll().dtoList(dtoList).pageRequestDTO(pageRequestDTO)
				.totalCount(result.getTotalElements()).build();
	}

	private VehicleResponseDTO entityToDTO(Vehicle vehicle) {

		return VehicleResponseDTO.builder().id(vehicle.getId()).plateNo(vehicle.getPlateNo())
				.vehicleTypeId(vehicle.getVehicleType().getId()).vehicleTypeName(vehicle.getVehicleType().getName())
				.status(vehicle.getStatus()).driverId(vehicle.getDriver() != null ? vehicle.getDriver().getId() : null)
				.build();
	}

	@Override
	@Transactional
	public Long registerVehicle(VehicleRequestDTO vehicleRequestDTO) {

		// 1. 차량번호 중복 검사
		if (vehicleRepository.existsByPlateNo(vehicleRequestDTO.getPlateNo())) {
			throw new CustomException(ErrorCode.DUPLICATE_VEHICLE_PLATE);
		}

		// 2. 등록하려는 차종 조회
		VehicleType vehicleType = vehicleTypeService.findById(vehicleRequestDTO.getVehicleTypeId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

		// 3. 담당 기사가 입력되었다면 기사 정보 확인
		Driver driver = null;

		if (vehicleRequestDTO.getDriverId() != null) {

			driver = driverService.findById(vehicleRequestDTO.getDriverId())
					.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

			// 이미 다른 차량에 배정된 기사인지 확인
			if (vehicleRepository.existsByDriver(driver)) {
				throw new CustomException(ErrorCode.DRIVER_ALREADY_ASSIGNED);
			}
		}

		// 4. 차량 엔티티 생성
		Vehicle vehicle = Vehicle.builder().plateNo(vehicleRequestDTO.getPlateNo()).vehicleType(vehicleType)
				.status(vehicleRequestDTO.getStatus()).driver(driver).build();

		// 5. DB에 차량 저장 후 생성된 ID 반환
		return vehicleRepository.save(vehicle).getId();
	}

	@Override
	@Transactional
	public Long updateVehicle(Long id, VehicleRequestDTO vehicleRequestDTO) {

		// 1. 수정할 차량 조회
		Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

		// 2. 다른 차량과 차량번호가 중복되는지 확인
		if (vehicleRepository.existsByPlateNoAndIdNot(vehicleRequestDTO.getPlateNo(), id)) {

			throw new CustomException(ErrorCode.DUPLICATE_VEHICLE_PLATE);
		}

		// 3. 변경할 차종 조회
		VehicleType vehicleType = vehicleTypeService.findById(vehicleRequestDTO.getVehicleTypeId())
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

		// 4. 담당 기사 정보 확인
		Driver driver = null;

		if (vehicleRequestDTO.getDriverId() != null) {

			driver = driverService.findById(vehicleRequestDTO.getDriverId())
					.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

			// 다른 차량에 이미 배정된 기사인지 확인
			if (vehicleRepository.existsByDriverAndIdNot(driver, id)) {

				throw new CustomException(ErrorCode.DRIVER_ALREADY_ASSIGNED);
			}
		}

		// 5. 기존 차량 정보 변경
		vehicle.changeInfo(vehicleRequestDTO.getPlateNo(), vehicleType, vehicleRequestDTO.getStatus(), driver);

		// 6. 수정된 차량 ID 반환
		return vehicle.getId();
	}

	@Override
	@Transactional
	public Long deactivateVehicle(Long id) {

		// 1. 비활성화할 차량 조회
		Vehicle vehicle = vehicleRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

		// 2. 차량 상태를 INACTIVE로 변경
		vehicle.changeStatus(VehicleStatus.INACTIVE);

		// 3. 변경된 차량 ID 반환
		return vehicle.getId();
	}

}
