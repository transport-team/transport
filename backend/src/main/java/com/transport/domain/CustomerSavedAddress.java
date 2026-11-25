package com.transport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 한 고객이 출발지·도착지 구분 없이 여러 주소를 저장하는 주소록.
@Entity
@Table(name = "customer_saved_address")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSavedAddress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 200)
    private String detail;

    public void update(String label, String address, String detail) {
        this.label = label;
        this.address = address;
        this.detail = detail;
    }
}
