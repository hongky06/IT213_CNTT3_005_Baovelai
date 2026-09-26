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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class ParkingReservationService {
    private static final EnumSet<ReservationStatus> CAPACITY_CONSUMING_STATUSES =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);
    private static final int MAX_RESERVATION_DAYS = 14;

    private final ParkingResourceRepository resourceRepository;
    private final ReservationRequestRepository requestRepository;
    private final ReservationUserRepository userRepository;

    public ParkingReservationService(ParkingResourceRepository resourceRepository,
                                    ReservationRequestRepository requestRepository,
                                    ReservationUserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AvailabilityResult getParkingAvailability(String resourceType, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        ParkingResource resource = findResource(resourceType);
        List<ReservationRequest> overlaps = requestRepository.findOverlappingRequests(
                resource.getId(), startDate, endDate, CAPACITY_CONSUMING_STATUSES);
        return toAvailability(resource, startDate, endDate, overlaps);
    }

    @Transactional
    public ReservationResult createParkingReservationRequest(String userId, String resourceType,
                                                              LocalDate startDate, LocalDate endDate,
                                                              int participantCount, String purpose) {
        validateDateRange(startDate, endDate);
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_RESERVATION_DAYS) {
            throw new IllegalArgumentException("Thời gian đặt chỗ tối đa là 14 ngày.");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId không được để trống.");
        }
        if (participantCount < 1) {
            throw new IllegalArgumentException("participantCount phải lớn hơn 0.");
        }
        if (purpose == null || purpose.trim().length() < 10 || purpose.trim().length() > 200) {
            throw new IllegalArgumentException("purpose phải có độ dài từ 10 đến 200 ký tự.");
        }

        ParkingResource resource = findResourceForUpdate(resourceType);
        ReservationUser user = userRepository.findById(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy userId."));
        if (user.getMembershipGroup() == MembershipGroup.PREMIUM && participantCount < 2) {
            throw new IllegalArgumentException("Thành viên PREMIUM phải đặt chỗ cho ít nhất 2 người.");
        }

        List<ReservationRequest> overlaps = requestRepository.findOverlappingRequests(
                resource.getId(), startDate, endDate, CAPACITY_CONSUMING_STATUSES);
        ensureCapacity(resource, startDate, endDate, participantCount, overlaps);

        ReservationRequest request = new ReservationRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setUser(user);
        request.setResource(resource);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setParticipantCount(participantCount);
        request.setPurpose(purpose.trim());
        request.setStatus(ReservationStatus.PENDING);
        request.setCreatedAt(Instant.now());
        requestRepository.save(request);

        return new ReservationResult(request.getRequestId(), request.getStatus(),
                summarize(request));
    }

    @Transactional
    public OperationResult decideRequest(String requestId, ApprovalDecision decision, String note) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId không được để trống.");
        }
        if (decision == null) {
            throw new IllegalArgumentException("decision phải là APPROVE hoặc REJECT.");
        }
        if (note != null && note.length() > 255) {
            throw new IllegalArgumentException("note không được vượt quá 255 ký tự.");
        }

        ReservationRequest request = requestRepository.findByRequestIdForUpdate(requestId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy requestId."));
        if (request.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ xử lý được yêu cầu đang ở trạng thái PENDING.");
        }

        if (decision == ApprovalDecision.APPROVE) {
            ParkingResource resource = findResourceForUpdate(request.getResource().getResourceType());
            List<ReservationRequest> overlaps = requestRepository.findOverlappingRequestsExcluding(
                    resource.getId(), request.getStartDate(), request.getEndDate(),
                    CAPACITY_CONSUMING_STATUSES, request.getRequestId());
            ensureCapacity(resource, request.getStartDate(), request.getEndDate(),
                    request.getParticipantCount(), overlaps);
            request.setStatus(ReservationStatus.APPROVED);
        } else {
            request.setStatus(ReservationStatus.REJECTED);
        }
        request.setNote(note == null || note.isBlank() ? null : note.trim());
        requestRepository.save(request);

        return new OperationResult(request.getRequestId(), request.getStatus(), request.getNote());
    }

    private ParkingResource findResource(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType không được để trống.");
        }
        return resourceRepository.findByResourceTypeIgnoreCase(resourceType.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy resourceType."));
    }

    private ParkingResource findResourceForUpdate(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType không được để trống.");
        }
        return resourceRepository.findByResourceTypeForUpdate(resourceType.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy resourceType."));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate và endDate là bắt buộc.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate phải nhỏ hơn hoặc bằng endDate.");
        }
    }

    private AvailabilityResult toAvailability(ParkingResource resource, LocalDate startDate,
                                               LocalDate endDate, List<ReservationRequest> overlaps) {
        List<DailyAvailability> days = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> {
                    int reserved = overlaps.stream()
                            .filter(request -> !date.isBefore(request.getStartDate())
                                    && !date.isAfter(request.getEndDate()))
                            .mapToInt(ReservationRequest::getParticipantCount)
                            .sum();
                    int available = Math.max(0, resource.getCapacity() - reserved);
                    return new DailyAvailability(date, resource.getCapacity(), reserved, available);
                })
                .toList();
        return new AvailabilityResult(resource.getResourceType(), startDate, endDate, days);
    }

    private void ensureCapacity(ParkingResource resource, LocalDate startDate, LocalDate endDate,
                                int requestedParticipants, List<ReservationRequest> overlaps) {
        AvailabilityResult availability = toAvailability(resource, startDate, endDate, overlaps);
        boolean unavailable = availability.days().stream()
                .anyMatch(day -> day.availableSpaces() < requestedParticipants);
        if (unavailable) {
            throw new IllegalArgumentException("Không đủ chỗ trống cho toàn bộ khoảng ngày đã chọn.");
        }
    }

    private String summarize(ReservationRequest request) {
        return "Đã tạo yêu cầu PENDING cho " + request.getResource().getResourceType()
                + " từ " + request.getStartDate() + " đến " + request.getEndDate()
                + ", " + request.getParticipantCount() + " người.";
    }

    public record DailyAvailability(LocalDate date, int capacity, int reservedSpaces, int availableSpaces) {
    }

    public record AvailabilityResult(String resourceType, LocalDate startDate, LocalDate endDate,
                                     List<DailyAvailability> days) {
    }

    public record ReservationResult(String requestId, ReservationStatus status, String summary) {
    }

    public record OperationResult(String requestId, ReservationStatus status, String note) {
    }
}
