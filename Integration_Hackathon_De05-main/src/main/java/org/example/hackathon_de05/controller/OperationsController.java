package org.example.hackathon_de05.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.hackathon_de05.model.constant.ApprovalDecision;
import org.example.hackathon_de05.service.ParkingReservationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class OperationsController {
    private final ParkingReservationService reservationService;

    public OperationsController(ParkingReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/approve-request")
    public ParkingReservationService.OperationResult decide(
            @Valid @RequestBody DecisionRequest request) {
        return reservationService.decideRequest(
                request.requestId(), request.decision(), request.note());
    }

    public record DecisionRequest(
            @NotBlank @Size(max = 36) String requestId,
            ApprovalDecision decision,
            @Size(max = 255) String note) {
    }
}
