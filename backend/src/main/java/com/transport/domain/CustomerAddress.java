package com.transport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_address")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAddress extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, unique = true)
	private Member member;

	@Column(nullable = false, length = 50)
	private String label;

	@Column(nullable = false)
	private String originAddress;

	private String originDetail;

	@Column(nullable = false)
	private String destAddress;

	private String destDetail;

	public void update(String label, String originAddress, String originDetail,
			String destAddress, String destDetail) {
		this.label = label;
		this.originAddress = originAddress;
		this.originDetail = originDetail;
		this.destAddress = destAddress;
		this.destDetail = destDetail;
	}
}
