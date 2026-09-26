package org.example.hackathon_de05.tools;

import org.example.hackathon_de05.service.ChatExecutionContext;
import org.example.hackathon_de05.service.ParkingReservationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ParkingReservationTools {
    private final ParkingReservationService reservationService;
    private final ChatExecutionContext executionContext;

    public ParkingReservationTools(ParkingReservationService reservationService,
                                   ChatExecutionContext executionContext) {
        this.reservationService = reservationService;
        this.executionContext = executionContext;
    }

    @Tool(name = "getParkingAvailability",
            description = "Tra cứu số chỗ đỗ còn trống theo từng ngày trong khoảng thời gian.")
    public ParkingReservationService.AvailabilityResult getParkingAvailability(
            @ToolParam(description = "Loại tài nguyên cần đặt") String resourceType,
            @ToolParam(description = "Ngày bắt đầu, định dạng YYYY-MM-DD") LocalDate startDate,
            @ToolParam(description = "Ngày kết thúc, định dạng YYYY-MM-DD") LocalDate endDate) {
        executionContext.recordTool("getParkingAvailability");
        return reservationService.getParkingAvailability(resourceType, startDate, endDate);
    }

    @Tool(name = "createParkingReservationRequest",
            description = "Tạo yêu cầu đặt chỗ PENDING nếu người dùng hợp lệ và còn đủ sức chứa.")
    public ParkingReservationService.ReservationResult createParkingReservationRequest(
            @ToolParam(description = "Mã người dùng đã đăng ký") String userId,
            @ToolParam(description = "Loại tài nguyên cần đặt") String resourceType,
            @ToolParam(description = "Ngày bắt đầu, định dạng YYYY-MM-DD") LocalDate startDate,
            @ToolParam(description = "Ngày kết thúc, định dạng YYYY-MM-DD") LocalDate endDate,
            @ToolParam(description = "Số người tham gia") int participantCount,
            @ToolParam(description = "Mục đích đặt chỗ, từ 10 đến 200 ký tự") String purpose) {
        executionContext.recordTool("createParkingReservationRequest");
        return reservationService.createParkingReservationRequest(
                userId, resourceType, startDate, endDate, participantCount, purpose);
    }
}
