package org.example.hackathon_de05.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_resources", uniqueConstraints = {
        @UniqueConstraint(name = "uk_parking_resource_type", columnNames = "resource_type")
})
@Getter
@Setter
@NoArgsConstructor
public class ParkingResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_type", nullable = false, length = 60)
    private String resourceType;

    @Column(nullable = false)
    private int capacity;

    public ParkingResource(String resourceType, int capacity) {
        this.resourceType = resourceType;
        this.capacity = capacity;
    }
}
