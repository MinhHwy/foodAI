# FoodAI Frontend

Giao diện được chuyển sang HTML, CSS và JavaScript thuần. Không dùng React hoặc framework UI. Mở `index.html` trực tiếp trong trình duyệt; ảnh tham chiếu Figma đã được lưu trong `images/`.

## Cấu trúc mã

- `index.html`: một điểm vào với mount `#app`, file input và vùng thông báo.
- `css/style.css`: aggregator; nạp tokens, base, layout, shared components, CSS theo trang và responsive theo cascade.
- `css/tokens.css`, `base.css`, `layout.css`, `components.css`, `responsive.css`: màu/font, reset, shell, primitive dùng chung và breakpoint.
- `css/pages/`: style riêng cho auth, dashboard, nhận diện, nhật ký, gợi ý, hồ sơ/thiết lập và admin.
- `js/data/foods.js`: dữ liệu món ăn và ánh xạ ảnh cục bộ.
- `js/core/state.js`, `storage.js`: state UI và persistence nhật ký bằng `localStorage`.
- `js/shared/components.js`, `shell.js`, `dialogs.js`: helper HTML, shell điều hướng, dialog và UI dùng chung.
- `js/pages/`: renderer theo các luồng auth, dashboard, recognition, diary, recommendations, profile, setup và admin.
- `js/router.js`, `events.js`: chọn renderer/điều hướng và đăng ký tương tác; `js/script.js` giữ vai trò bootstrap hiện có.

Các file JavaScript là classic scripts có `defer`, được nạp theo thứ tự trong `index.html`; không dùng `import`, bundler hay React. `shell.js` tạo sidebar/topbar/mobile navigation một lần cho workspace rồi bọc view hiện hành, thay vì lặp markup hoặc include kiểu React. Các trang auth dùng layout riêng theo thiết kế Figma.

## Pages Figma đã kiểm kê

File: [FoodAI - Smart Food Recognition & Calories Tracking](https://www.figma.com/design/qnOLOC8yC4N2e86U7ZyirW/FoodAI-%E2%80%93-Smart-Food-Recognition---Calories-Tracking?node-id=0-1&m=dev)

- `00 - Design System`: 19 frame màu Primary/Neutral/Semantic; typography FoodAI nằm trong các text styles của page.
- `UI Main Food AI`, `UI Main Food AI 2`, `UI Main Food AI 3`, `UI Main Food AI 4`, `UI Main Food AI 5`: cùng 23 layout (115 frame placements); các node ID của cả năm page đều có trong bộ chọn.
- `02 - Trang chủ & Hồ sơ người dùng`: dashboard, 3 bước thiết lập mục tiêu và hồ sơ người dùng.
- `Hồ sơ người dùng bản 2`: 5 frame Home/Health Profile/Personal Goal/Daily Calorie Target/Edit Profile cho desktop và mobile (10 frame).
- `07`: dashboard mobile, hồ sơ cá nhân, mục tiêu calo/macro, cài đặt/đồng bộ và cập nhật sinh trắc học; đồng thời có hai bộ Food Diary & Progress cho desktop/mobile (24 frame).
- `08 - Nhóm Food Diary & Theo dõi tiến độ`: nhật ký và thống kê tổng quan, cả desktop lẫn mobile.
- `03 - Food Recognition`: 8 trạng thái nhận diện desktop và 12 biến thể mobile trong hai bộ frame.
- `04 - Nutrition`: 9 trạng thái nhận diện/ghi nhật ký desktop (upload, ảnh chọn, camera, phân tích, kết quả đơn/nhiều món, độ tin cậy thấp, lưu và đổi món).
- `05 - Recommendation`: gợi ý món và chi tiết cơ sở đề xuất AI trên mobile.
- `06 - Admin`: quản lý cơ sở dữ liệu món ăn cùng form thêm/sửa/xóa, desktop và mobile.

## Bộ màn hình

Thông tin pages/frames Figma được lưu trong README để tham khảo; ứng dụng không có bộ chọn frame trong giao diện.

23 layout duy nhất trên 5 page UI Main: Đăng ký; Đăng nhập; Quên mật khẩu; Xác thực email đăng ký; Xác thực đặt lại mật khẩu; Đặt lại mật khẩu; Bảng điều khiển; Hồ sơ sức khỏe; Mục tiêu cá nhân; Mục tiêu calo mỗi ngày; Hồ sơ người dùng; Tải ảnh lên nhận diện; Ảnh đã chọn; Camera trực tiếp; Đang xử lý AI; Kết quả món đơn; Kết quả nhiều món; Lưu vào Nhật ký; Tìm kiếm/đổi món; Nhật ký dinh dưỡng; Thống kê/xu hướng; Gợi ý thực đơn; Chi tiết món và cơ sở đề xuất AI.

Các frame cùng luồng hoặc bản thiết kế lặp được ánh xạ về một view đại diện, không phải bản sao pixel-perfect cho từng node ID. View mobile bật chế độ xem khung hẹp; tại viewport nhỏ, điều hướng tự chuyển thành bottom navigation.

## Tương tác mẫu

- Đăng nhập, đăng ký, xác thực mã và đặt lại mật khẩu có luồng chuyển màn hình.
- Tải/thả ảnh để xem trước, giả lập camera, trạng thái phân tích, kết quả nhận diện và lưu món vào nhật ký.
- Nhật ký, thống kê, gợi ý món, chỉnh mục tiêu/hồ sơ và màn hình quản trị có điều hướng mẫu.
- Nhật ký mẫu được lưu cục bộ bằng `localStorage`; các thao tác backend/API thật chưa được nối.

## Tokens chính

- Primary: `#16A34A`, `#15803D`, `#166534`; Semantic: success `#16A34A`, warning `#F59E0B`, error `#DC2626`, info `#2563EB`.
- Neutral: `#FFFFFF`, `#F9FAFB`, `#E5E7EB`, `#9CA3AF`, `#4B5563`, `#1F2937`, `#111827`.
- Typography: Inter cho UI và Plus Jakarta Sans cho headings/data emphasis.
