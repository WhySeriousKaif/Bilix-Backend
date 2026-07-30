package com.molla.model;

import com.molla.domain.StoreStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;

    @Column( nullable = false)
    public  String brand;

    @OneToOne
    private User storeAdmin; //one store has one admin

    private LocalDateTime createdAt;
    private  LocalDateTime updatedAt;

    private String Description;

    private String storeType;

    @Enumerated(EnumType.STRING)
    private StoreStatus storeStatus;



    private  StoreContact contact=new StoreContact();

    @PrePersist
    protected void onCreate(){
        createdAt=LocalDateTime.now();
        storeStatus= StoreStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt=LocalDateTime.now();
    }


}
