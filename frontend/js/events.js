async function saveSetupHealth() {
	if (!getToken()) { showToast("Đăng nhập để lưu hồ sơ sức khỏe vào FoodAI."); return true; }
	const panel = document.querySelector("#app .wizard-panel");
	if (!panel) return false;
	const numbers = [...panel.querySelectorAll("input[type='number']")].map((input) => Number(input.value));
	const selects = [...panel.querySelectorAll("select")];
	if (numbers.length < 3 || selects.length < 2) return false;
	const [heightCm, weightKg, ageYears] = numbers;
	const payload = {
		heightCm,
		weightKg,
		ageYears,
		biologicalSex: selects[0].selectedIndex === 1 ? "FEMALE" : "MALE",
		activityLevel: ["LIGHT", "SEDENTARY", "MODERATE", "ACTIVE"][selects[1].selectedIndex] || "SEDENTARY"
	};
	const notes = [...panel.querySelectorAll("input:not([type='number'])")].map((input) => input.value.trim()).filter(Boolean);
	if (notes.length) payload.dietaryNotes = notes.join(", ");
	try {
		state.me = await profileApi.patchProfile(payload);
		showToast("Đã lưu hồ sơ sức khỏe vào FoodAI.");
		return true;
	} catch (error) {
		showToast(error.message);
		return false;
	}
}

async function finishSetupGoal() {
	const order = ["LOSE", "MAINTAIN", "GAIN"];
	const active = state.me?.goals?.goalType;
	const index = state.setupGoal ?? (active ? order.indexOf(active) : 0);
	const goalType = order[Math.max(0, index)] || "LOSE";
	try {
		state.me = await profileApi.putGoals(goalType);
		showToast("Đã hoàn tất thiết lập mục tiêu calo FoodAI của bạn.");
		navigate("dashboard", false);
	} catch (error) {
		showToast(error.message);
	}
}

function initializeEvents() {
app.addEventListener("click", async (event) => {
	const target = event.target.closest("[data-action]");
	if (!target) return;
	const action = target.dataset.action;
	if (action.startsWith("auth-")) { navigate(action); return; }
	if (action === "logout") { setToken(null); state.user = null; state.me = null; state.setupGoal = null; showToast("Đã đăng xuất khỏi FoodAI."); navigate("auth-login", false); return; }
	if (action === "browse") { fileInput.click(); return; }
	if (action === "camera-toggle") { state.cameraOn = !state.cameraOn; if (state.cameraOn) { showToast("Camera đã sẵn sàng"); } else { navigate("recognition-selected"); return; } render(); return; }
	if (action === "remove-image") { state.selectedImage = null; navigate("recognition-upload"); return; }
	if (action === "meal-choice") { state.mealChoice = target.dataset.meal; render(); return; }
	if (action === "close-modal") { if (event.target === target || target.classList.contains("icon-button")) { document.querySelector(".modal-overlay")?.remove(); } return; }
	if (action === "save-meal") {
		savedMeals.push({ name: "Phở gà ức bỏ da nước trong", meal: state.mealChoice, calories: 420, protein: 38, carbs: 52, fat: 12, image: 9 });
		persistSavedMeals();
		state.view = "diary"; state.mobilePreview = false; history.replaceState(null, "", "#diary"); render(); showToast("Đã lưu món ăn vào nhật ký."); return;
	}
	if (action === "save-meal-dismiss") { document.querySelector(".modal-overlay")?.remove(); return; }
	if (action === "select-choice") {
		const cards = [...target.parentElement.querySelectorAll(".choice-card")];
		cards.forEach((item) => item.classList.remove("is-selected"));
		target.classList.add("is-selected");
		state.setupGoal = cards.indexOf(target);
		return;
	}
	if (action === "date-prev" || action === "date-next") { showToast("Đang xem ngày trong nhật ký."); return; }
	if (action === "portion") { showToast("Bạn có thể điều chỉnh khẩu phần trong hồ sơ món ăn."); return; }
	if (action === "food-pick" || action === "choose-food") { showToast("Đã chọn món ăn."); navigate("recognition-single"); return; }
	if (action === "apply-recommendation") { state.mealChoice = "Bữa tối"; navigate("recognition-save"); return; }
	if (action === "connect") { showToast("Tính năng đồng bộ sẽ được kết nối với backend."); return; }
	if (action === "notifications") { showToast("Bạn đã cập nhật nhật ký hôm nay."); return; }
	if (action === "resend-code") { showToast("Mã xác thực mới đã được gửi."); return; }
	if (action === "export") { showToast("Bản xuất dữ liệu mẫu đã sẵn sàng."); return; }
	if (action === "admin-save") { showToast("Đã lưu thay đổi món ăn mẫu."); navigate("admin"); return; }
	if (action === "diary" || action === "trends") { navigate(action); return; }
	if (action === "setup-goal" && state.view === "setup-health") { const saved = await saveSetupHealth(); if (saved) navigate("setup-goal"); return; }
	if (action === "dashboard" && state.view === "setup-calories" && getToken()) { await finishSetupGoal(); return; }
	navigate(action);
});

app.addEventListener("submit", async (event) => {
	const form = event.target.closest("[data-auth-form]");
	if (!form) return;
	event.preventDefault();
	const data = new FormData(form);
	const view = form.dataset.authForm;
	if (view === "auth-register") {
		if (data.get("password") !== data.get("confirm")) { showToast("Mật khẩu xác nhận chưa trùng khớp."); return; }
		try {
			await authApi.register({ fullName: data.get("name"), email: data.get("email"), password: data.get("password") });
			showToast("Đăng ký thành công! Hãy đăng nhập với email vừa tạo.");
			navigate("auth-login", false);
		} catch (error) { showToast(error.message); }
	} else if (view === "auth-forgot") { showToast("Tính năng khôi phục mật khẩu sẽ kết nối backend ở giai đoạn sau."); navigate("auth-verify"); }
	else if (view === "auth-verify") navigate("auth-reset");
	else if (view === "auth-reset") {
		if (data.get("password") !== data.get("confirm")) { showToast("Mật khẩu xác nhận chưa trùng khớp."); return; }
		showToast("Đặt lại mật khẩu thành công."); navigate("auth-login");
	} else {
		if (!data.get("email") || !data.get("password")) { showToast("Hãy nhập email và mật khẩu."); return; }
		try {
			const response = await authApi.login({ email: data.get("email"), password: data.get("password") });
			setToken(response.accessToken);
			state.user = response.user;
			try { await fetchMe(); } catch (error) { state.me = null; }
			showToast(`Chào mừng ${response.user?.fullName || "bạn"} quay trở lại FoodAI!`);
			navigate("dashboard", false);
		} catch (error) { showToast(error.message); }
	}
});

app.addEventListener("input", (event) => {
	if (event.target.id === "food-search") {
		const position = event.target.selectionStart;
		state.search = event.target.value;
		render();
		const search = document.querySelector("#food-search");
		search?.focus(); search?.setSelectionRange(position, position);
	}
	if (event.target.matches("[data-admin-search]")) {
		const query = event.target.value.toLowerCase();
		document.querySelectorAll(".admin-table tbody tr").forEach((row) => { row.hidden = !row.textContent.toLowerCase().includes(query); });
	}
	if (event.target.closest(".otp-inputs")) {
		const inputs = [...document.querySelectorAll(".otp-inputs input")];
		const index = inputs.indexOf(event.target);
		if (event.target.value && index < inputs.length - 1) inputs[index + 1].focus();
	}
});

app.addEventListener("change", (event) => {
	const select = event.target.closest("[data-setting]");
	if (!select) return;
	if (!getToken()) { showToast("Đăng nhập để đồng bộ cài đặt với FoodAI."); return; }
	const payload = {};
	if (select.dataset.setting === "energyUnit") payload.energyUnit = select.value;
	else if (select.dataset.setting === "weightUnit") payload.weightUnit = select.value;
	else if (select.dataset.setting === "dailyRemindersEnabled") payload.dailyRemindersEnabled = select.value === "on";
	if (!Object.keys(payload).length) return;
	profileApi.patchSettings(payload).then((me) => { state.me = me; showToast("Đã đồng bộ cài đặt FoodAI."); }).catch((error) => showToast(error.message));
});

fileInput.addEventListener("change", () => {
	const file = fileInput.files?.[0];
	if (!file) return;
	if (!file.type.startsWith("image/")) { showToast("Hãy chọn tệp ảnh JPG hoặc PNG."); return; }
	if (file.size > 10 * 1024 * 1024) { showToast("Ảnh phải có dung lượng nhỏ hơn 10MB."); return; }
	state.selectedImage = URL.createObjectURL(file);
	navigate("recognition-selected");
});

app.addEventListener("dragover", (event) => {
	const zone = event.target.closest("[data-dropzone]");
	if (zone) { event.preventDefault(); zone.classList.add("is-dragging"); }
});
app.addEventListener("dragleave", (event) => event.target.closest("[data-dropzone]")?.classList.remove("is-dragging"));
app.addEventListener("drop", (event) => {
	const zone = event.target.closest("[data-dropzone]");
	if (!zone) return;
	event.preventDefault(); zone.classList.remove("is-dragging");
	const file = event.dataTransfer.files?.[0];
	if (file?.type.startsWith("image/")) { state.selectedImage = URL.createObjectURL(file); navigate("recognition-selected"); }
	else showToast("Hãy thả một tệp ảnh hợp lệ vào đây.");
});
}
