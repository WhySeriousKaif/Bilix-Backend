package com.molla.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.molla.domain.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Order order;

    private String reason;

    private Double amount;

    @ManyToOne
    @JsonIgnore
    private ShiftReport shiftReport;

    @ManyToOne
    private User cashier;

    @ManyToOne
    private Branch branch;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefundItem> items;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
