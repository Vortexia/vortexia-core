Bản tổng hợp vai trò và cấu hình giữa hai phía Server và Client:

🖥️ 1. Phía Server (Plugin — Paper / Proxy)
Vai trò: Giữ quyền lực tối cao (Single Source of Truth), quản lý Database trung tâm (CCCD, mã PIN, Shadow Inventory), kiểm soát logic máy móc và quyết định giới hạn an toàn.

Cơ chế mạng:

Chỉ gửi gói tin nhị phân siêu nhẹ (Delta Sync) qua kênh vortexia:sync khi thông số máy vượt ngưỡng thay đổi.

Chỉ truyền dữ liệu cho người chơi đang nhìn vào máy (Subscription); tự động ngắt quét khi người chơi đi xa hoặc tắt HUD.

Fallback Vanilla: Tự động hạ cấp hiển thị qua Actionbar / Text Display nếu người chơi không cài mod.

Nội dung cấu hình (config.yml):

Tầm nhìn soi máy tối đa (max-raycast-distance: 5 blocks).

Tần suất gửi gói tin tối thiểu (min-sync-interval-ticks).

Ngưỡng biến động dữ liệu (delta-thresholds cho điện năng, tiến độ).

Thiết lập hiển thị cho Vanilla (hud-fallback: kiểu hiển thị, tần suất cập nhật).

Khóa bảo mật Failsafe PIN (số lần thử, thời gian đóng băng).

🎮 2. Phía Client (Mod — Fabric / NeoForge)
Vai trò: Là lớp hiển thị cá nhân hóa (View Layer), không can thiệp logic game hay dữ liệu gốc; chuyên vẽ giao diện mượt mà theo tần số quét màn hình.

Cơ chế hoạt động:

Bắt gói tin từ kênh vortexia:sync, lưu vào bộ đệm RAM cục bộ để render HUD/WAILA độc lập mà không gây tụt FPS.

Mở màn hình giao diện nhập PIN riêng (Custom Screen) thay vì gõ lệnh chat.

Gửi tín hiệu đăng ký / hủy đăng ký (subscribe/unsubscribe) theo hướng nhìn của tâm ngắm.

Nội dung cấu hình (vortexia-client.json):

Bật / tắt hiển thị HUD hoặc toàn bộ mod.

Tọa độ, neo vị trí HUD trên màn hình (anchor, offset_x, offset_y).

Ẩn / hiện các thành phần chi tiết (tên khối, mã CCCD chủ sở hữu, thanh năng lượng).

Tùy biến đồ họa (độ trong suốt nền, tỉ lệ hiển thị scale, mã màu sắc).

Bật / tắt màn hình nhập PIN riêng (use_custom_pin_screen)