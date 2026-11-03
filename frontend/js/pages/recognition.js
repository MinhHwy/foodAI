function recognitionTips() {
	return `<aside class="surface tips-panel"><div class="section-title"><h2>${icon("sun", 15)} Để nhận diện chính xác hơn</h2></div><ol class="tip-list"><li><span class="tip-number">1</span><span>Đảm bảo toàn bộ món ăn hiển thị rõ ràng bên trong khung hình chụp.</span></li><li><span class="tip-number">2</span><span>Ánh sáng tự nhiên, rõ nét giúp AI ước tính khẩu phần chính xác.</span></li><li><span class="tip-number">3</span><span>Tránh chụp ảnh bị mờ hoặc rung lắc để nhận diện tốt từng thành phần.</span></li><li><span class="tip-number">4</span><span>Tách riêng các món ăn xếp chồng lên nhau nếu có thể.</span></li></ol><p class="notice" style="margin:14px 0 0;border-color:#e7ecf3;background:#f8fafc;color:var(--quiet)">${icon("badge-check", 14)} Hỗ trợ nhận diện hơn 5.000 món Việt và quốc tế.</p></aside>`;
}

function recognitionView(view) {
	const titles = {
		"recognition-upload": ["Nhận diện Món ăn", "Tải lên hoặc chụp ảnh bữa ăn để FoodAI phân tích thành phần và dinh dưỡng tức thì."],
		"recognition-selected": ["Ảnh đã sẵn sàng", "Kiểm tra ảnh trước khi bắt đầu phân tích."],
		"recognition-camera": ["Camera chụp ảnh trực tiếp", "Căn chỉnh món ăn trong khung hình để nhận diện."],
		"recognition-processing": ["Đang phân tích bữa ăn", state.recognitionLoading
			? "Đang tải ảnh lên và khởi tạo phiên nhận diện."
			: state.recognitionSession?.status === "FAILED" ? "Phiên nhận diện thất bại. Hãy thử tải ảnh khác."
				: state.recognitionSession?.status === "REJECTED" ? "Ảnh bị từ chối xử lý."
					: state.recognitionSession?.status === "COMPLETED" ? "AI đã hoàn tất nhưng chưa trả về món ăn."
				: state.recognitionSession ? "Phiên nhận diện đã được tạo; đang chờ kết quả từ AI."
				: "AI đang xác định các món ăn và thành phần dinh dưỡng."],
		"recognition-single": ["Kết quả Quét AI", "Hoàn tất phân tích. Kiểm tra thông tin dinh dưỡng trước khi lưu."],
		"recognition-multi": ["Kết quả Quét AI", "FoodAI phát hiện các món riêng biệt trong ảnh."],
		"recognition-save": ["Lưu vào Nhật ký Dinh dưỡng", "Chọn bữa ăn để lưu món đã nhận diện."],
		"recognition-search": ["Thay đổi món đã nhận diện", "Tìm món trong cơ sở dữ liệu món ăn Việt Nam."]
	};
	const [title, description] = titles[view] || titles["recognition-upload"];
	let main;
	if (view === "recognition-upload") {
		main = `<section class="surface upload-zone" data-dropzone><div><div class="upload-icon">${icon("camera", 24)}</div><h2>Kéo và thả ảnh món ăn của bạn vào đây</h2><p>Hỗ trợ định dạng JPG, PNG, dung lượng tối đa 10MB</p><div class="upload-actions">${button("Duyệt ảnh từ máy", "browse", "secondary", "image")}${button("Dùng Camera", "open-camera", "primary", "camera")}</div></div></section>`;
	} else if (view === "recognition-camera") {
		main = `<div class="modal-overlay webcam-overlay" data-action="webcam-overlay-close"><section class="modal-dialog webcam-dialog" role="dialog" aria-modal="true" aria-labelledby="webcam-title"><div class="dialog-header"><div><h2 id="webcam-title">Chụp ảnh món ăn</h2><p>Căn chỉnh món ăn trong khung hình.</p></div><button type="button" class="icon-button" data-action="webcam-close" aria-label="Đóng">${icon("x", 16)}</button></div><div class="webcam-viewfinder">${state.cameraOn ? `<video id="webcam-video" autoplay playsinline muted></video>` : `<div class="webcam-status">${icon(state.webcamStarting ? "loader-circle" : "camera", 24)}<span>${state.webcamStarting ? "Đang kết nối camera..." : "Camera chưa sẵn sàng."}</span></div>`}</div><div class="webcam-actions"><button type="button" class="secondary-button" data-action="webcam-close">Đóng</button><button type="button" class="primary-button" data-action="webcam-capture" ${state.cameraOn ? "" : "disabled"}>${icon("camera", 15)} Chụp ảnh</button></div></section></div>`;
	} else if (view === "recognition-processing") {
		const processingLabel = state.recognitionLoading
			? "Đang tải ảnh và tạo phiên nhận diện..."
			: state.recognitionSession
				? `Phiên #${state.recognitionSession.sessionId} · ${state.recognitionSession.status}`
				: `Đang phân tích: ${state.selectedFile?.name || "ảnh món ăn"}`;
		const processingAction = state.recognitionLoading
			? `<button type="button" class="primary-button" disabled>Đang tải ảnh...</button>`
			: state.recognitionSession?.status === "FAILED"
				? button("Thử lại", "retry-recognition", "primary", "rotate-ccw")
			: state.recognitionSession && !["COMPLETED", "FAILED", "REJECTED"].includes(state.recognitionSession.status)
				? button("Kiểm tra kết quả", "refresh-recognition", "secondary", "refresh-cw") : "";
		const statusLabel = { FAILED: "THẤT BẠI", REJECTED: "BỊ TỪ CHỐI", COMPLETED: "HOÀN TẤT" };
		main = `<section class="surface"><div class="preview-image"><img src="${state.selectedImage || photo(9)}" alt="Ảnh đang phân tích"><span class="hero-kicker" style="position:absolute;left:14px;top:14px">${icon("scan-line", 12)} ${state.recognitionSession?.modelVersion ? esc(state.recognitionSession.modelVersion) : "FoodAI"}</span></div><div class="preview-meta"><small>● ${esc(processingLabel)}</small><span class="metric-note">${state.recognitionLoading ? "ĐANG TẢI LÊN" : statusLabel[state.recognitionSession?.status] || (state.recognitionSession ? "CHỜ KẾT QUẢ AI" : "AI DEEP SCAN")}</span></div>${processingAction ? `<div class="hero-actions">${processingAction}</div>` : ""}</section>`;
	} else if (view === "recognition-selected") {
		const fileSize = state.selectedFile ? `${(state.selectedFile.size / (1024 * 1024)).toFixed(1)} MB` : "";
		main = `<section class="surface"><div class="preview-image"><img src="${state.selectedImage || photo(9)}" alt="Ảnh món ăn đã chọn"></div><div class="preview-meta"><small>Ảnh đã sẵn sàng phân tích${fileSize ? ` · ${fileSize}` : ""}</small><div class="hero-actions"><button class="text-button" data-action="remove-image">Xóa</button>${button("Thay đổi ảnh", "browse", "secondary", "image")}${button("Thử tải và phân tích lại", "recognition-processing", "primary", "flask-conical")}</div></div></section>`;
	} else if (view === "recognition-search") {
		const list = foods.filter((food) => food.name.toLowerCase().includes(state.search.toLowerCase())).map((food, index) => `<div class="meal-row"><div class="food-thumb"><img src="${photo(food.image)}" alt=""></div><div class="meal-info"><strong>${esc(food.name)}</strong><small>${food.calories} kcal · ${food.protein}g đạm · ${food.carbs}g carb</small></div><button class="secondary-button" data-action="choose-food" data-food="${index}">Chọn</button></div>`).join("");
		main = `<section class="surface surface-pad"><div class="field"><label for="food-search">Tìm món ăn trong cơ sở dữ liệu</label><input id="food-search" class="search-input" type="search" placeholder="Ví dụ: Phở, Cơm tấm, Bánh mì" value="${esc(state.search)}"></div><div class="diary-group-body">${list || `<p class="metric-note">Không tìm thấy món phù hợp. Hãy thử từ khóa khác.</p>`}</div></section>`;
	} else if (state.recognitionSession && (view === "recognition-single" || view === "recognition-multi")) {
		const session = state.recognitionSession;
		const items = Array.isArray(session.items) ? session.items : [];
		const resultRows = items.map((item) => {
			const name = item.selectedFoodName || item.predictedFoodName || item.predictedLabel || "Món chưa xác định";
			const confidence = Number(item.confidenceScore);
			const confidenceLabel = Number.isFinite(confidence) ? `${Math.round(confidence * 100)}% độ tin cậy` : "Chưa có độ tin cậy";
			const portionLabel = item.estimatedGrams ? ` · ${item.estimatedGrams} g` : "";
			const nutrition = item.nutrition;
			const nutritionLabel = nutrition
				? `${fmtNumber(nutrition.caloriesKcal)} kcal · ${fmtNumber(nutrition.proteinG)}g đạm · ${fmtNumber(nutrition.carbsG)}g carb · ${fmtNumber(nutrition.fatG)}g béo`
				: "Chưa có dữ liệu dinh dưỡng cho món này.";
			return `<div class="meal-row"><div class="meal-info"><strong>${esc(name)}</strong><small>${esc(confidenceLabel)}${portionLabel}</small><small>${esc(nutritionLabel)}</small></div><span class="tag">${esc(item.status || "PREDICTED")}</span></div>`;
		}).join("");
		const totals = items.reduce((sum, item) => {
			if (!item.nutrition) return sum;
			sum.caloriesKcal += Number(item.nutrition.caloriesKcal) || 0;
			sum.proteinG += Number(item.nutrition.proteinG) || 0;
			sum.carbsG += Number(item.nutrition.carbsG) || 0;
			sum.fatG += Number(item.nutrition.fatG) || 0;
			sum.count++;
			return sum;
		}, { caloriesKcal: 0, proteinG: 0, carbsG: 0, fatG: 0, count: 0 });
		const mockWarning = session.isMocked === true
			? `<div role="alert" style="padding:12px 14px;margin-bottom:14px;border:1px solid #facc15;border-radius:10px;background:#fef9c3;color:#854d0e;font-weight:700">⚠️ Đang hiển thị kết quả mô phỏng (Mock Fallback)</div>`
			: "";
		const nutritionSummary = totals.count
			? `<div class="nutrient-total"><strong>${fmtNumber(totals.caloriesKcal)} kcal</strong><small>${fmtNumber(totals.proteinG)}g đạm · ${fmtNumber(totals.carbsG)}g carb · ${fmtNumber(totals.fatG)}g béo</small></div>`
			: "";
		const resultActions = session.status === "COMPLETED" && items.length
			? `<div class="result-actions">${button("Xác nhận & Lưu", "recognition-save", "primary", "check")}</div>`
			: "";
		main = `<section class="surface result-photo"><img src="${esc(session.imageUrl || state.selectedImage || photo(9))}" alt="Ảnh nhận diện"></section><section class="surface result-content">${mockWarning}<span class="eyebrow">PHIÊN #${esc(session.sessionId)}</span><h2>${items.length ? "Món ăn nhận diện được" : "Đang chờ kết quả nhận diện"}</h2><p class="metric-note">Trạng thái phiên: ${esc(session.status || "UNKNOWN")}</p>${nutritionSummary}${resultRows || `<p class="metric-note">Phiên đã được tạo nhưng backend chưa trả về món ăn.</p>`}${resultActions}</section>`;
	} else {
		const multi = view === "recognition-multi";
		const current = foods[0];
		main = `<section class="surface result-photo"><img src="${state.selectedImage || photo(current.image)}" alt="${esc(current.name)}"></section><section class="surface result-content"><span class="eyebrow">${multi ? "4 MÓN ĂN ĐƯỢC NHẬN DIỆN" : "ĐÃ NHẬN DIỆN MÓN ĂN"}</span><h2>${multi ? "Danh sách món ăn" : `${current.name} (Chicken Pho)`}</h2><p class="metric-note">Khẩu phần ước tính: ${multi ? "4 món" : "350 g"} · <span class="tag">92% độ tin cậy AI</span></p><div class="nutrient-total"><div class="upload-icon" style="width:40px;height:40px;margin:0">${icon("flame", 18)}</div><div><strong>${multi ? "820" : current.calories} kcal</strong><br><small>Tổng năng lượng ước tính</small></div></div><div class="nutrient-list"><div class="nutrient-row"><span>Chất đạm</span><span>${current.protein} g</span><strong>21%</strong><div class="metric-meter"><span style="width:48%;background:#3b82f6"></span></div></div><div class="nutrient-row"><span>Tinh bột</span><span>${current.carbs} g</span><strong>50%</strong><div class="metric-meter"><span style="width:70%;background:#f59e0b"></span></div></div><div class="nutrient-row"><span>Chất béo</span><span>${current.fat} g</span><strong>29%</strong><div class="metric-meter"><span style="width:40%;background:#f43f5e"></span></div></div></div><div class="result-actions">${button("Xác nhận & Lưu", "recognition-save", "primary", "check")}${button("Điều chỉnh khẩu phần", "portion", "secondary", "sliders-horizontal")}${button("Đổi món ăn", "recognition-search", "secondary", "replace")}</div></section>`;
	}
	return `${pageHeading(title, description)}<div class="workspace-grid ${view.startsWith("recognition-single") || view === "recognition-multi" ? "result-layout" : ""}">${main}${view === "recognition-search" ? "" : recognitionTips()}</div>`;
}
