package com.molla.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    private String phone;

    private String address;

    private String email;

    @ElementCollection // ElementCollection means - the workingDays is stored as a list of strings in
                       // the database
    private List<String> workingDays;

    private LocalTime openTime;
    private LocalTime closeTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    private Store store; // many branches belong to one store

    @OneToOne
    private User manager; // one branch has one manager

    @PreUpdate // PreUpdate means - before the branch is updated, the updatedAt is set to the
               // current time
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PrePersist // PrePersist means - before the branch is saved, the createdAt is set to the
                // current time
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Branch branch = (Branch) o;
        return id != null && id.equals(branch.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
