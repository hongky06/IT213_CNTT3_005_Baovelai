package org.example.hackathon_de05.repository;

import org.example.hackathon_de05.model.constant.ReservationStatus;
import org.example.hackathon_de05.model.entity.ReservationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRequestRepository extends JpaRepository<ReservationRequest, Long> {
    Optional<ReservationRequest> findByRequestId(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ReservationRequest request where request.requestId = :requestId")
    Optional<ReservationRequest> findByRequestIdForUpdate(@Param("requestId") String requestId);

    @Query("""
            select request from ReservationRequest request
            where request.resource.id = :resourceId
              and request.status in :statuses
              and request.startDate <= :endDate
              and request.endDate >= :startDate
            """)
    List<ReservationRequest> findOverlappingRequests(
            @Param("resourceId") Long resourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<ReservationStatus> statuses);

    @Query("""
            select request from ReservationRequest request
            where request.resource.id = :resourceId
              and request.requestId <> :excludedRequestId
              and request.status in :statuses
              and request.startDate <= :endDate
              and request.endDate >= :startDate
            """)
    List<ReservationRequest> findOverlappingRequestsExcluding(
            @Param("resourceId") Long resourceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("excludedRequestId") String excludedRequestId);
}
