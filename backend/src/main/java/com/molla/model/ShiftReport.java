package com.molla.model;

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
public class ShiftReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private User cashier;

    @ManyToOne
    private Branch branch;

    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;

    private Double totalSales;
    private Double totalRefunds;
    private Double netSales;
    private Integer totalOrders;

    private LocalDateTime createdAt;

    @Transient
    private List<PaymentSummary> paymentSummaries;

    @Transient
    private List<Product> topSellingProducts;

    @Transient
    private List<Order> recentOrders;

    @OneToMany(mappedBy = "shiftReport", cascade = CascadeType.ALL)
    private List<Refund> refunds;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

