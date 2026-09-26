# Chính sách đặt chỗ

## Điều kiện tạo yêu cầu

Mỗi yêu cầu đặt chỗ phải gắn với một người dùng có trong cơ sở dữ liệu và một loại tài nguyên đã được cấu hình. Sức chứa và tình trạng chỗ trống lấy từ dữ liệu tài nguyên và các yêu cầu đang chờ hoặc đã được duyệt; không suy đoán số chỗ khi không có dữ liệu.

Thời gian đặt chỗ gồm cả ngày bắt đầu và ngày kết thúc, tối đa 14 ngày. Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc. Mục đích đặt chỗ phải dài từ 10 đến 200 ký tự. Thành viên PREMIUM cần đặt ít nhất 2 người.

Yêu cầu hợp lệ được tạo ở trạng thái PENDING và có requestId để tra cứu. Yêu cầu PENDING chưa phải là đặt chỗ đã được duyệt.

## Tra cứu chỗ trống

Kết quả availability thể hiện sức chứa, số chỗ đã được giữ bởi yêu cầu PENDING hoặc APPROVED, và số chỗ còn lại theo từng ngày. Không coi yêu cầu REJECTED là đang chiếm chỗ. Không trả lời còn chỗ nếu không có kết quả tra cứu dữ liệu phù hợp.

## Duyệt yêu cầu

Nhân viên vận hành chỉ được xử lý yêu cầu PENDING. Quyết định hợp lệ là APPROVE hoặc REJECT. Trước khi chuyển sang APPROVED, hệ thống phải kiểm tra lại sức chứa cho toàn bộ khoảng ngày vì tình trạng có thể thay đổi sau khi tạo yêu cầu. Yêu cầu đã được xử lý không được duyệt hoặc từ chối lần nữa.
