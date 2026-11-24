package com.transport.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.transport.domain.VehicleDeletionRequest;
import com.transport.domain.enums.VehicleRegistrationStatus;

public interface VehicleDeletionRequestRepository extends JpaRepository<VehicleDeletionRequest, Long> {
    boolean existsByVehicle_IdAndStatus(Long vehicleId, VehicleRegistrationStatus status);

    @EntityGraph(attributePaths = {"driver", "driver.member", "vehicle", "vehicle.vehicleType"})
    List<VehicleDeletionRequest> findAllByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = {"vehicle", "vehicle.vehicleType"})
    List<VehicleDeletionRequest> findByDriver_Member_IdOrderByCreatedAtDescIdDesc(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from VehicleDeletionRequest r where r.id = :id")
    Optional<VehicleDeletionRequest> findForUpdate(@Param("id") Long id);
}
