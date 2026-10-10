function render() {
	const auth = state.view.startsWith("auth-");
	let content;
	if (auth) content = authView(state.view);
	else if (state.view === "dashboard") content = dashboardView();
	else if (state.view.startsWith("recognition")) content = recognitionView(state.view);
	else if (state.view === "diary") content = diaryView(false);
	else if (state.view === "trends") content = diaryView(true);
	else if (state.view === "diary-empty") content = `${pageHeading("Nhật ký ăn uống", "Một bữa ăn đầu tiên, một khởi đầu nhỏ.")}<section class="surface upload-zone"><div><div class="upload-icon">${icon("utensils", 24)}</div><h2>Chưa có dữ liệu ăn uống</h2><p>Hãy ghi nhận bữa ăn đầu tiên để bắt đầu theo dõi dinh dưỡng.</p><div class="upload-actions">${button("Thêm bữa ăn", "recognition-upload", "primary", "plus")}${button("Chụp ảnh món ăn", "recognition-upload", "secondary", "camera")}</div></div></section>`;
	else if (state.view === "calorie-status") content = `${pageHeading("Trạng thái calo", "Theo dõi mức năng lượng đã nạp trong ngày.")}<div class="diary-meals">${[["Trong mục tiêu", "Ngày mẫu độc lập", "1,850", "92.5% mục tiêu · Còn lại: 150 kcal", "#16a34a", "circle-check"], ["Gần mục tiêu", "Ngày mẫu độc lập", "1,950", "97.5% mục tiêu · Còn lại: 50 kcal", "#f59e0b", "clock-3"], ["Trên mục tiêu", "Ngày mẫu độc lập", "2,250", "112.5% mục tiêu · Đã vượt: 250 kcal", "#dc2626", "circle-alert"]].map(([label, date, value, note, color, iconName]) => `<section class="surface surface-pad"><div class="notice" style="border:0;background:${color === "#16a34a" ? "#f0fdf4" : color === "#f59e0b" ? "#fffbeb" : "#fef2f2"};color:${color};font-size:13px;font-weight:700">${icon(iconName, 18)} ${label}</div><p class="metric-note" style="margin-top:12px">${date}</p><div class="metric-value">${value}<small>/ 2,000 kcal</small></div><div class="metric-meter"><span style="width:${Math.min(Number(value) / 20, 100)}%;background:${color}"></span></div><strong style="display:block;margin-top:10px;color:${color};font-size:11px">${note}</strong></section>`).join("")}</div>`;
	else if (state.view === "recommendations") content = recommendationsView(false);
	else if (state.view === "recommend-detail") content = recommendationsView(true);
	else if (state.view === "profile") content = profileView(false);
	else if (state.view === "settings") content = settingsView();
	else if (state.view.startsWith("setup-")) content = setupView(state.view);
	else if (state.view === "admin") content = adminView(false);
	else if (state.view === "admin-edit") content = adminView(true);
	else content = dashboardView();
	app.innerHTML = auth ? content : appShell(content);
	if (state.view === "recognition-save") app.insertAdjacentHTML("beforeend", saveDialog());
	if (window.lucide?.createIcons) window.lucide.createIcons();
}

function navigate(view, mobilePreview = state.mobilePreview) {
	state.view = view;
	state.mobilePreview = mobilePreview;
	state.selectedImage = state.selectedImage || null;
	history.replaceState(null, "", `#${view}`);
	render();
	if (view === "recognition-processing") {
		window.setTimeout(() => {
			if (state.view === "recognition-processing") navigate("recognition-single", state.mobilePreview);
		}, 1800);
	}
	window.scrollTo({ top: 0, behavior: "smooth" });
}
