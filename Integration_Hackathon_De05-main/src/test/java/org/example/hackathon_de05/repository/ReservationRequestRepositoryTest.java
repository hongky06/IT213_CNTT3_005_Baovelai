package org.example.hackathon_de05.repository;

import org.example.hackathon_de05.model.constant.MembershipGroup;
import org.example.hackathon_de05.model.constant.ReservationStatus;
import org.example.hackathon_de05.model.entity.ParkingResource;
import org.example.hackathon_de05.model.entity.ReservationRequest;
import org.example.hackathon_de05.model.entity.ReservationUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class ReservationRequestRepositoryTest {
    @Autowired
    private ReservationRequestRepository requestRepository;
    @Autowired
    private ParkingResourceRepository resourceRepository;
    @Autowired
    private ReservationUserRepository userRepository;

    @Test
    void findsOverlappingRequestsAndLocksRequestsByTheirPublicIdentifier() {
        ParkingResource resource = resourceRepository.saveAndFlush(new ParkingResource("CAR", 5));
        ReservationUser user = userRepository.saveAndFlush(
                new ReservationUser("user-1", MembershipGroup.STANDARD));
        ReservationRequest pending = new ReservationRequest();
        pending.setRequestId("request-1");
        pending.setUser(user);
        pending.setResource(resource);
        pending.setStartDate(LocalDate.of(2026, 10, 1));
        pending.setEndDate(LocalDate.of(2026, 10, 3));
        pending.setParticipantCount(2);
        pending.setPurpose("Business event parking");
        pending.setStatus(ReservationStatus.PENDING);
        pending.setCreatedAt(Instant.now());
        requestRepository.saveAndFlush(pending);

        List<ReservationRequest> overlaps = requestRepository.findOverlappingRequests(
                resource.getId(), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 4),
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED));

        assertEquals(1, overlaps.size());
        assertEquals("request-1", requestRepository.findByRequestIdForUpdate("request-1")
                .orElseThrow().getRequestId());
        assertEquals(0, requestRepository.findOverlappingRequestsExcluding(
                resource.getId(), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 4),
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED), "request-1").size());
    }
}
