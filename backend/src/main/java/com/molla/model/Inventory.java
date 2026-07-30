package com.molla.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private  Branch branch;

    @ManyToOne
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    private LocalDateTime lastUpdated;

    @PreUpdate
    public void onUpdate(){
        lastUpdated=LocalDateTime.now();
    }






}
