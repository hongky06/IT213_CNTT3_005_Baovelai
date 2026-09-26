package org.example.hackathon_de05.repository;

import jakarta.persistence.LockModeType;
import org.example.hackathon_de05.model.entity.ParkingResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParkingResourceRepository extends JpaRepository<ParkingResource, Long> {
    Optional<ParkingResource> findByResourceTypeIgnoreCase(String resourceType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select resource from ParkingResource resource where lower(resource.resourceType) = lower(:resourceType)")
    Optional<ParkingResource> findByResourceTypeForUpdate(@Param("resourceType") String resourceType);
}
