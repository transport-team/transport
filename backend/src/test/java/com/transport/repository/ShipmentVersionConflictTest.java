package com.transport.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CargoType;
import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.ShipmentStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// 배송 @Version 충돌이 실제 DB에서 ObjectOptimisticLockingFailureException으로 나오는지 확인한다
// (GlobalExceptionHandler가 이 예외를 409 CONCURRENT_UPDATE로 바꾼다 — Docker DB 필요, 테스트 끝나면 롤백)
@SpringBootTest
@Transactional
class ShipmentVersionConflictTest {

	@Autowired
	private ShipmentRepository shipmentRepository;

	@PersistenceContext
	private EntityManager em;

	@Test
	@DisplayName("먼저 읽은 배송(옛 version)을 다른 변경 뒤에 저장하면 낙관적 락 충돌")
	void staleShipmentSaveConflicts() {
		Shipment saved = persistShipment();
		em.flush();
		em.clear();

		// 사용자 A가 먼저 읽어 둔 배송 (version 0)
		Shipment stale = em.find(Shipment.class, saved.getId());
		em.detach(stale);

		// 사용자 B가 먼저 바꿔서 저장 (version 0 → 1)
		Shipment fresh = em.find(Shipment.class, saved.getId());
		fresh.changeStatus(ShipmentStatus.CANCELED);
		em.flush();

		// A가 옛 version으로 저장하면 충돌
		stale.changeStatus(ShipmentStatus.REQUESTED);
		assertThatThrownBy(() -> {
			shipmentRepository.save(stale);
			em.flush();
		}).isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	private Shipment persistShipment() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		Member customer = Member.builder().email("version-" + suffix + "@test.com").password("x")
				.name("충돌 확인 고객").phone("010-0000-0000").role(MemberRole.CUSTOMER).enabled(true).build();
		em.persist(customer);
		CargoType cargoType = CargoType.builder().name("충돌확인" + suffix).active(true).build();
		em.persist(cargoType);
		VehicleType vehicleType = VehicleType.builder().code("VER_" + suffix.toUpperCase()).name("충돌 확인 차량").active(true).build();
		em.persist(vehicleType);
		Shipment shipment = Shipment.builder()
				.customer(customer).vehicleType(vehicleType).cargoType(cargoType)
				.originAddress("서울특별시 중구 세종대로 110").destAddress("경기도 수원시 팔달구 효원로 1")
				.cargoName("충돌 확인").pickupAt(LocalDateTime.of(2001, 3, 1, 9, 0))
				.senderName("보내는 사람").senderPhone("010-1111-2222")
				.receiverName("받는 사람").receiverPhone("010-3333-4444")
				.fare(50_000).durationMin(60).status(ShipmentStatus.PENDING_PAYMENT)
				.build();
		em.persist(shipment);
		return shipment;
	}
}
