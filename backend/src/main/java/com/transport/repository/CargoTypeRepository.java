package com.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.CargoType;

public interface CargoTypeRepository extends JpaRepository<CargoType, Long> {
	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);
}