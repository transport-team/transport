// AdminPaymentSearchDTO.java
package com.transport.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.transport.domain.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminPaymentSearchDTO extends PageRequestDTO {

	private PaymentStatus status;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate from;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate to;

	private String keyword;
}