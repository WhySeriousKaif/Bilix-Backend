package com.molla.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;

    @Column( nullable = false)
    public String name;

    public String description;

    @Column( nullable = false, unique = true)
    public String sku;


    public Double mrp;
    public Double sellingPrice;
    
    @Column(name = "price")
    public Double price; // Database column that may be redundant with sellingPrice

    @Column(name = "quantity")
    public Integer quantity; // Product stock quantity

    public String brand;
    public String image;

    // public Category category;

    @ManyToOne
    private Store store; //one store has many products

    @ManyToOne
    private Category category; //one category has many products


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt=LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt=LocalDateTime.now();
    }
    
}
