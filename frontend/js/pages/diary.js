const diaryMealTypes = ["BREAKFAST", "LUNCH", "DINNER", "SNACK"];
const diaryMealGoals = [500, 700, 550, 250];
const diaryMealIcons = ["☀", "☼", "☾", "✦"];
const diaryNutritionKeys = [
	["caloriesKcal", 2000, "kcal"],
	["proteinG", 130, "g"],
	["carbsG", 230, "g"],
	["fatG", 65, "g"]
];

function diaryNutritionValue(nutrition, key) {
	return Number(nutrition?.[key] || 0);
}

function formatDiaryDate(dateValue) {
	const date = new Date(`${dateValue}T12:00:00`);
	const weekday = new Intl.DateTimeFormat("vi-VN", { weekday: "long" })
		.format(date)
		.replace(/^./u, (character) => character.toLocaleUpperCase("vi-VN"));
	const formattedDate = new Intl.DateTimeFormat("vi-VN", {
		day: "2-digit",
		month: "2-digit",
		year: "numeric"
	}).format(date);
	return `${dateValue === localDateInputValue() ? "Hôm nay, " : ""}${weekday} - ${formattedDate}`;
}

async function loadDiaryForDate(dateValue = state.diaryDate) {
	const requestId = ++state.diaryRequestId;
	state.diaryDate = dateValue;
	state.diaryMeals = [];
	state.diaryTotal = null;
	state.diaryError = null;
	state.diaryLoading = true;
	if (state.view === "diary") render();

	try {
		const history = await diaryApi.getDaily(dateValue);
		if (!Array.isArray(history?.meals)) {
			throw new Error("Phản hồi nhật ký từ máy chủ không đúng định dạng.");
		}
		if (requestId !== state.diaryRequestId || dateValue !== state.diaryDate || state.view !== "diary") return;
		state.diaryMeals = history.meals;
		state.diaryTotal = history.total || null;
	} catch (error) {
		if (requestId !== state.diaryRequestId || dateValue !== state.diaryDate || state.view !== "diary") return;
		state.diaryError = error.message || "Không thể tải nhật ký.";
		showToast(state.diaryError);
	} finally {
		if (requestId === state.diaryRequestId && state.view === "diary") {
			state.diaryLoading = false;
			render();
		}
	}
}

function diaryView(trends = false) {
	const total = state.diaryTotal || {};
	const totalCalories = diaryNutritionValue(total, "caloriesKcal");
	const groups = mealNames.map((meal, index) => {
		const meals = state.diaryMeals.filter((entry) => entry.mealType === diaryMealTypes[index]);
		const items = meals.flatMap((entry) => entry.items || []);
		const calories = meals.reduce((sum, entry) => sum + diaryNutritionValue(entry.total, "caloriesKcal"), 0);
		return `<section class="surface diary-group"><div class="diary-group-header"><div><strong>${diaryMealIcons[index]} ${meal}</strong><br><small>${items.length} món · Mục tiêu ~${diaryMealGoals[index]} kcal</small></div><div class="hero-actions"><span class="tag">${Math.round(calories).toLocaleString("vi-VN")} kcal</span><button class="secondary-button" data-action="recognition-upload">${icon("plus", 13)} Thêm món</button></div></div><div class="diary-group-body">${items.length ? items.map((item) => {
			const nutrition = item.nutrition || {};
			const serving = item.servingLabel || (item.consumedGrams ? `${Number(item.consumedGrams).toLocaleString("vi-VN")} g` : "Khẩu phần");
			return `<div class="diary-food"><div class="food-thumb">${icon("utensils", 16)}</div><div class="meal-info"><strong>${esc(item.foodName || "Món ăn")}</strong><small>${esc(serving)} · ${diaryNutritionValue(nutrition, "proteinG")}g Đạm · ${diaryNutritionValue(nutrition, "carbsG")}g Carb</small></div><strong class="meal-calories">${Math.round(diaryNutritionValue(nutrition, "caloriesKcal")).toLocaleString("vi-VN")} kcal</strong></div>`;
		}).join("") : `<div class="diary-food"><small class="metric-note">Chưa có món ăn được ghi nhận.</small><button class="text-button" data-action="recognition-upload">Thêm món</button></div>`}</div></section>`;
	}).join("");
	if (trends) return `${pageHeading("Thống kê & Xu hướng dinh dưỡng", "Theo dõi biến thiên calo và tiến độ mục tiêu của bạn.", `<button class="secondary-button" data-action="diary">${icon("notebook-tabs", 14)} Nhật ký hôm nay</button>`)}<section class="surface surface-pad"><div class="section-title"><div><span class="eyebrow">PHÂN TÍCH XU HƯỚNG & HIỆU SUẤT</span><h2>Biến thiên Calo & Tuân thủ mục tiêu</h2></div><div class="segmented"><button class="is-active">Tuần này</button><button>Tháng này</button><button>3 tháng gần nhất</button></div></div><div class="chart-panel"><div class="chart-bars">${[73, 82, 68, 65, 77, 62, 57].map((height, index) => `<div class="chart-column"><b class="metric-note">${[1850, 2150, 1720, 1680, 1920, 1550, 1450][index]}</b><span style="height:${height}%"></span><small>${["T2", "T3", "T4", "T5", "T6", "T7", "CN"][index]}</small></div>`).join("")}</div><div class="chart-legend"><span>● Lượng calo thực nạp vào</span><span style="color:#dc2626">━ Ngưỡng mục tiêu: 2,000 kcal/ngày</span></div></div></section><section class="surface surface-pad" style="margin-top:14px"><div class="section-title"><h2>${icon("sparkles", 15)} Lời khuyên & Insight từ FoodAI Coach</h2><span class="tag">Xu hướng tích cực</span></div><p class="metric-note">Bạn đã duy trì lịch trình tốt trong 6/7 ngày. Hãy tiếp tục cân bằng lượng protein và chất xơ trong tuần tới.</p></section>`;
	const totalProtein = diaryNutritionValue(total, "proteinG");
	const totalCarbs = diaryNutritionValue(total, "carbsG");
	const totalFat = diaryNutritionValue(total, "fatG");
	const dateContent = state.diaryLoading
		? `<div class="diary-status">Đang tải nhật ký...</div>`
		: state.diaryError
			? `<div class="diary-status" role="alert">${esc(state.diaryError)} <button class="text-button" data-action="diary-retry">Thử lại</button></div>`
			: `<div class="diary-meals">${groups}</div>`;
	return `${pageHeading("Nhật ký & Tiến độ dinh dưỡng", "Theo dõi chi tiết bữa ăn hàng ngày và xu hướng hấp thụ calo, phân bổ macro khoa học.", button("Ghi nhanh bằng AI", "recognition-upload", "primary", "sparkles"))}<div class="segmented"><button class="is-active" data-action="diary">${icon("notebook-tabs", 14)} Nhật ký hôm nay</button><button data-action="trends">${icon("chart-no-axes-combined", 14)} Thống kê & Xu hướng</button></div><div class="surface date-toolbar"><div class="date-controls"><button type="button" data-action="date-prev" aria-label="Ngày trước">${icon("chevron-left", 14)}</button><button type="button" class="diary-date-control" data-action="diary-date-picker" aria-label="Chọn ngày nhật ký">${icon("calendar-days", 13)}<strong>${formatDiaryDate(state.diaryDate)}</strong></button><input id="diary-date-picker" type="date" value="${esc(state.diaryDate)}" aria-label="Chọn ngày nhật ký"><button type="button" data-action="date-next" aria-label="Ngày sau">${icon("chevron-right", 14)}</button></div><span class="tag">Dinh dưỡng cân đối</span></div><section class="surface macro-summary"><div class="macro-cell"><span>Tổng năng lượng · Còn lại</span><strong>${Math.round(totalCalories).toLocaleString("vi-VN")} / 2.000 kcal</strong><div class="metric-meter"><span style="width:${Math.min(100, totalCalories / 20)}%"></span></div></div>${[
		["Đạm (Protein)", totalProtein, diaryNutritionKeys[1], ""],
		["Tinh bột (Carbs)", totalCarbs, diaryNutritionKeys[2], ";background:#f59e0b"],
		["Chất béo (Fat)", totalFat, diaryNutritionKeys[3], ";background:#3686d9"]
	].map(([label, amount, [, target, unit], meterStyle]) => `<div class="macro-cell"><span>${label}</span><strong>${Number(amount).toLocaleString("vi-VN")} / ${target}${unit}</strong><div class="metric-meter"><span style="width:${Math.min(100, Number(amount) / target * 100)}%${meterStyle}"></span></div></div>`).join("")}</section>${dateContent}`;
}
