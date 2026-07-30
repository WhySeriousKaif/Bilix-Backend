package com.molla.model;

import jakarta.persistence.*;
import lombok.*;

import com.molla.domain.PaymentType;
import com.molla.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Double totalAmount;

    private LocalDateTime createdAt;

    @ManyToOne
    private Branch branch; // Many orders can belong to one branch

    @ManyToOne
    private User cashier; // The user who processed the order -- one cashier can process many orders

    @ManyToOne
    private Customer customer; // The customer who made the order -- one customer can have many orders

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems; // One order can have many order items
    
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }
}
