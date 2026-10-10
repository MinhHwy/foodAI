function recognitionTips() {
	return `<aside class="surface tips-panel"><div class="section-title"><h2>${icon("sun", 15)} Để nhận diện chính xác hơn</h2></div><ol class="tip-list"><li><span class="tip-number">1</span><span>Đảm bảo toàn bộ món ăn hiển thị rõ ràng bên trong khung hình chụp.</span></li><li><span class="tip-number">2</span><span>Ánh sáng tự nhiên, rõ nét giúp AI ước tính khẩu phần chính xác.</span></li><li><span class="tip-number">3</span><span>Tránh chụp ảnh bị mờ hoặc rung lắc để nhận diện tốt từng thành phần.</span></li><li><span class="tip-number">4</span><span>Tách riêng các món ăn xếp chồng lên nhau nếu có thể.</span></li></ol><p class="notice" style="margin:14px 0 0;border-color:#e7ecf3;background:#f8fafc;color:var(--quiet)">${icon("badge-check", 14)} Hỗ trợ nhận diện hơn 5.000 món Việt và quốc tế.</p></aside>`;
}

function recognitionView(view) {
	const titles = {
		"recognition-upload": ["Nhận diện Món ăn", "Tải lên hoặc chụp ảnh bữa ăn để FoodAI phân tích thành phần và dinh dưỡng tức thì."],
		"recognition-selected": ["Ảnh đã sẵn sàng", "Kiểm tra ảnh trước khi bắt đầu phân tích."],
		"recognition-camera": ["Camera chụp ảnh trực tiếp", "Căn chỉnh món ăn trong khung hình để nhận diện."],
		"recognition-processing": ["Đang phân tích bữa ăn", "AI đang xác định các món ăn và thành phần dinh dưỡng."],
		"recognition-single": ["Kết quả Quét AI", "Hoàn tất phân tích. Kiểm tra thông tin dinh dưỡng trước khi lưu."],
		"recognition-multi": ["Kết quả Quét AI", "FoodAI phát hiện các món riêng biệt trong ảnh."],
		"recognition-save": ["Lưu vào Nhật ký Dinh dưỡng", "Chọn bữa ăn để lưu món đã nhận diện."],
		"recognition-search": ["Thay đổi món đã nhận diện", "Tìm món trong cơ sở dữ liệu món ăn Việt Nam."]
	};
	const [title, description] = titles[view] || titles["recognition-upload"];
	let main;
	if (view === "recognition-upload") {
		main = `<section class="surface upload-zone" data-dropzone><div><div class="upload-icon">${icon("camera", 24)}</div><h2>Kéo và thả ảnh món ăn của bạn vào đây</h2><p>Hỗ trợ định dạng JPG, PNG, dung lượng tối đa 10MB</p><div class="upload-actions">${button("Duyệt ảnh từ máy", "browse", "secondary", "image")}${button("Dùng Camera", "recognition-camera", "primary", "camera")}</div></div></section>`;
	} else if (view === "recognition-camera") {
		main = `<section class="surface"><div class="preview-image"><img src="${photo(9)}" alt="Khung xem trước camera"><span class="hero-kicker" style="position:absolute;left:14px;top:14px">● ${state.cameraOn ? "ĐANG KẾT NỐI" : "FOODAI LENS"}</span></div><div class="preview-meta"><small>Căn chỉnh toàn bộ món ăn trong khung hình.</small><div class="hero-actions">${button(state.cameraOn ? "Chụp ảnh" : "Bật camera", "camera-toggle", "primary", state.cameraOn ? "camera" : "video")}</div></div></section>`;
	} else if (view === "recognition-processing") {
		main = `<section class="surface"><div class="preview-image"><img src="${state.selectedImage || photo(9)}" alt="Ảnh đang phân tích"><span class="hero-kicker" style="position:absolute;left:14px;top:14px">${icon("scan-line", 12)} Vision Model v2.4</span></div><div class="preview-meta"><small>● Đang phân tích: <b>vietnamese_com_tam.png</b></small><span class="metric-note">AI DEEP SCAN</span></div></section>`;
	} else if (view === "recognition-selected") {
		main = `<section class="surface"><div class="preview-image"><img src="${state.selectedImage || photo(9)}" alt="Ảnh món ăn đã chọn"></div><div class="preview-meta"><small>Ảnh đã sẵn sàng phân tích · 1.4 MB</small><div class="hero-actions"><button class="text-button" data-action="remove-image">Xóa</button>${button("Thay đổi ảnh", "browse", "secondary", "image")}${button("Phân tích món ăn", "recognition-processing", "primary", "flask-conical")}</div></div></section>`;
	} else if (view === "recognition-search") {
		const list = foods.filter((food) => food.name.toLowerCase().includes(state.search.toLowerCase())).map((food, index) => `<div class="meal-row"><div class="food-thumb"><img src="${photo(food.image)}" alt=""></div><div class="meal-info"><strong>${esc(food.name)}</strong><small>${food.calories} kcal · ${food.protein}g đạm · ${food.carbs}g carb</small></div><button class="secondary-button" data-action="choose-food" data-food="${index}">Chọn</button></div>`).join("");
		main = `<section class="surface surface-pad"><div class="field"><label for="food-search">Tìm món ăn trong cơ sở dữ liệu</label><input id="food-search" class="search-input" type="search" placeholder="Ví dụ: Phở, Cơm tấm, Bánh mì" value="${esc(state.search)}"></div><div class="diary-group-body">${list || `<p class="metric-note">Không tìm thấy món phù hợp. Hãy thử từ khóa khác.</p>`}</div></section>`;
	} else {
		const multi = view === "recognition-multi";
		const current = foods[0];
		main = `<section class="surface result-photo"><img src="${state.selectedImage || photo(current.image)}" alt="${esc(current.name)}"></section><section class="surface result-content"><span class="eyebrow">${multi ? "4 MÓN ĂN ĐƯỢC NHẬN DIỆN" : "ĐÃ NHẬN DIỆN MÓN ĂN"}</span><h2>${multi ? "Danh sách món ăn" : `${current.name} (Chicken Pho)`}</h2><p class="metric-note">Khẩu phần ước tính: ${multi ? "4 món" : "350 g"} · <span class="tag">92% độ tin cậy AI</span></p><div class="nutrient-total"><div class="upload-icon" style="width:40px;height:40px;margin:0">${icon("flame", 18)}</div><div><strong>${multi ? "820" : current.calories} kcal</strong><br><small>Tổng năng lượng ước tính</small></div></div><div class="nutrient-list"><div class="nutrient-row"><span>Chất đạm</span><span>${current.protein} g</span><strong>21%</strong><div class="metric-meter"><span style="width:48%;background:#3b82f6"></span></div></div><div class="nutrient-row"><span>Tinh bột</span><span>${current.carbs} g</span><strong>50%</strong><div class="metric-meter"><span style="width:70%;background:#f59e0b"></span></div></div><div class="nutrient-row"><span>Chất béo</span><span>${current.fat} g</span><strong>29%</strong><div class="metric-meter"><span style="width:40%;background:#f43f5e"></span></div></div></div><div class="result-actions">${button("Xác nhận & Lưu", "recognition-save", "primary", "check")}${button("Điều chỉnh khẩu phần", "portion", "secondary", "sliders-horizontal")}${button("Đổi món ăn", "recognition-search", "secondary", "replace")}</div></section>`;
	}
	return `${pageHeading(title, description)}<div class="workspace-grid ${view.startsWith("recognition-single") || view === "recognition-multi" ? "result-layout" : ""}">${main}${view === "recognition-search" ? "" : recognitionTips()}</div>`;
}
