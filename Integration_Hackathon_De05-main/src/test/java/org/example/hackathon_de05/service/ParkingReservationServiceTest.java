package org.example.hackathon_de05.service;

import org.example.hackathon_de05.model.constant.ApprovalDecision;
import org.example.hackathon_de05.model.constant.MembershipGroup;
import org.example.hackathon_de05.model.constant.ReservationStatus;
import org.example.hackathon_de05.model.entity.ParkingResource;
import org.example.hackathon_de05.model.entity.ReservationRequest;
import org.example.hackathon_de05.model.entity.ReservationUser;
import org.example.hackathon_de05.repository.ParkingResourceRepository;
import org.example.hackathon_de05.repository.ReservationRequestRepository;
import org.example.hackathon_de05.repository.ReservationUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingReservationServiceTest {
    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 3);

    @Mock
    private ParkingResourceRepository resourceRepository;
    @Mock
    private ReservationRequestRepository requestRepository;
    @Mock
    private ReservationUserRepository userRepository;
    @InjectMocks
    private ParkingReservationService service;

    private ParkingResource resource;

    @BeforeEach
    void setUp() {
        resource = new ParkingResource("CAR", 5);
        resource.setId(10L);
    }

    @Test
    void availabilityCountsPendingAndApprovedReservationsForEachDate() {
        ReservationRequest pending = request(2, START, START.plusDays(1), ReservationStatus.PENDING);
        ReservationRequest approved = request(1, START.plusDays(1), END, ReservationStatus.APPROVED);
        when(resourceRepository.findByResourceTypeIgnoreCase("car")).thenReturn(Optional.of(resource));
        when(requestRepository.findOverlappingRequests(any(), any(), any(), any()))
                .thenReturn(List.of(pending, approved));

        ParkingReservationService.AvailabilityResult result =
                service.getParkingAvailability(" car ", START, END);

        assertEquals(List.of(3, 2, 4), result.days().stream()
                .map(ParkingReservationService.DailyAvailability::availableSpaces).toList());
    }

    @Test
    void availabilityRejectsReversedDatesBeforeDatabaseAccess() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getParkingAvailability("CAR", END, START));
        verify(resourceRepository, never()).findByResourceTypeIgnoreCase(any());
    }

    @Test
    void creationPersistsPendingRequestAndReturnsItsIdentifier() {
        ReservationUser user = new ReservationUser("user-1", MembershipGroup.STANDARD);
        when(resourceRepository.findByResourceTypeForUpdate("CAR")).thenReturn(Optional.of(resource));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(requestRepository.findOverlappingRequests(any(), any(), any(), any())).thenReturn(List.of());
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingReservationService.ReservationResult result =
                service.createParkingReservationRequest("user-1", "CAR", START, END, 3,
                        "Business event parking");

        assertEquals(ReservationStatus.PENDING, result.status());
        org.junit.jupiter.api.Assertions.assertNotNull(result.requestId());
        assertEquals("Đã tạo yêu cầu PENDING cho CAR từ 2026-10-01 đến 2026-10-03, 3 người.",
                result.summary());
        verify(requestRepository).save(any(ReservationRequest.class));
    }

    @Test
    void premiumMemberMustRequestAtLeastTwoParticipants() {
        when(resourceRepository.findByResourceTypeForUpdate("CAR")).thenReturn(Optional.of(resource));
        when(userRepository.findById("premium-1"))
                .thenReturn(Optional.of(new ReservationUser("premium-1", MembershipGroup.PREMIUM)));

        assertThrows(IllegalArgumentException.class, () -> service.createParkingReservationRequest(
                "premium-1", "CAR", START, END, 1, "Business event parking"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void reservationCannotExceedFourteenInclusiveDays() {
        assertThrows(IllegalArgumentException.class, () -> service.createParkingReservationRequest(
                "user-1", "CAR", START, START.plusDays(14), 1, "Business event parking"));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void reservationCannotOverbookAnyDayInTheRange() {
        ReservationRequest existing = request(4, START.plusDays(1), END, ReservationStatus.PENDING);
        when(resourceRepository.findByResourceTypeForUpdate("CAR")).thenReturn(Optional.of(resource));
        when(userRepository.findById("user-1"))
                .thenReturn(Optional.of(new ReservationUser("user-1", MembershipGroup.STANDARD)));
        when(requestRepository.findOverlappingRequests(any(), any(), any(), any()))
                .thenReturn(List.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.createParkingReservationRequest(
                "user-1", "CAR", START, END, 2, "Business event parking"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void approvalRechecksCapacityAndKeepsRequestPendingWhenNoLongerAvailable() {
        ReservationRequest request = request(2, START, END, ReservationStatus.PENDING);
        request.setRequestId("request-1");
        when(requestRepository.findByRequestIdForUpdate("request-1")).thenReturn(Optional.of(request));
        when(resourceRepository.findByResourceTypeForUpdate("CAR")).thenReturn(Optional.of(resource));
        when(requestRepository.findOverlappingRequestsExcluding(any(), any(), any(), any(), any()))
                .thenReturn(List.of(request(4, START, END, ReservationStatus.APPROVED)));

        assertThrows(IllegalArgumentException.class, () -> service.decideRequest(
                "request-1", ApprovalDecision.APPROVE, "approved"));
        assertEquals(ReservationStatus.PENDING, request.getStatus());
    }

    @Test
    void onlyPendingRequestsCanBeProcessed() {
        ReservationRequest request = request(1, START, END, ReservationStatus.APPROVED);
        request.setRequestId("request-1");
        when(requestRepository.findByRequestIdForUpdate("request-1")).thenReturn(Optional.of(request));

        assertThrows(IllegalArgumentException.class, () -> service.decideRequest(
                "request-1", ApprovalDecision.REJECT, null));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidPurposeBeforeAccessingDatabase() {
        assertThrows(IllegalArgumentException.class, () -> service.createParkingReservationRequest(
                "user-1", "CAR", START, END, 1, "short"));
        verify(resourceRepository, never()).findByResourceTypeForUpdate(any());
    }

    private ReservationRequest request(int participants, LocalDate start, LocalDate end,
                                       ReservationStatus status) {
        ReservationRequest request = new ReservationRequest();
        request.setRequestId("existing-" + participants + "-" + start);
        request.setResource(resource);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setParticipantCount(participants);
        request.setStatus(status);
        request.setPurpose("Business event parking");
        request.setCreatedAt(Instant.now());
        return request;
    }
}
