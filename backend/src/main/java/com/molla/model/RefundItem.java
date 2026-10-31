package com.molla.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    private Refund refund;

    @ManyToOne(optional = false)
    private OrderItem orderItem;

    @ManyToOne(optional = false)
    private Product product;

    private Integer quantity;
    private Double unitPrice;
    private Double amount;
}
