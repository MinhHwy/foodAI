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

function setRecognitionImage(file) {
	if (state.selectedImage?.startsWith("blob:")) URL.revokeObjectURL(state.selectedImage);
	state.selectedFile = file;
	state.selectedImage = URL.createObjectURL(file);
	state.recognitionSession = null;
}

function isMobileDevice() {
	return /Android|iPhone|iPad|iPod/i.test(navigator.userAgent) || window.innerWidth <= 768;
}

function stopWebcamStream() {
	if (state.cameraStream) {
		state.cameraStream.getTracks().forEach((track) => track.stop());
		state.cameraStream = null;
	}
	state.cameraOn = false;
	state.webcamStarting = false;
}

function cameraErrorMessage(error) {
	if (error?.name === "NotAllowedError" || error?.name === "PermissionDeniedError") {
		return "Bạn chưa cấp quyền camera. Hãy cho phép truy cập camera trong trình duyệt rồi thử lại.";
	}
	if (error?.name === "NotFoundError" || error?.name === "DevicesNotFoundError") {
		return "Không tìm thấy webcam trên thiết bị này.";
	}
	if (error?.name === "NotReadableError" || error?.name === "TrackStartError") {
		return "Webcam đang được ứng dụng khác sử dụng. Hãy đóng ứng dụng đó rồi thử lại.";
	}
	if (!navigator.mediaDevices?.getUserMedia) {
		return "Trình duyệt không hỗ trợ webcam hoặc trang chưa được mở qua kết nối an toàn (HTTPS).";
	}
	return "Không thể mở camera. Hãy kiểm tra quyền truy cập và thử lại.";
}

async function openCamera() {
	if (state.webcamStarting || state.cameraStream) return;
	if (isMobileDevice()) {
		navigate("recognition-upload");
		cameraFileInput.value = "";
		cameraFileInput.click();
		return;
	}
	if (!navigator.mediaDevices?.getUserMedia) {
		showToast(cameraErrorMessage());
		return;
	}

	state.webcamStarting = true;
	navigate("recognition-camera");
	let stream;
	try {
		stream = await navigator.mediaDevices.getUserMedia({ video: true });
		if (state.view !== "recognition-camera") {
			stream.getTracks().forEach((track) => track.stop());
			return;
		}
		state.cameraStream = stream;
		state.cameraOn = true;
		state.webcamStarting = false;
		render();
		const video = document.querySelector("#webcam-video");
		if (!video) {
			stopWebcamStream();
			throw new Error("Không thể khởi tạo khung xem trước webcam.");
		}
		video.srcObject = stream;
		video.play().catch(() => showToast("Không thể phát hình ảnh từ webcam."));
	} catch (error) {
		if (stream) stream.getTracks().forEach((track) => track.stop());
		const cameraViewIsOpen = state.view === "recognition-camera";
		stopWebcamStream();
		if (cameraViewIsOpen) {
			navigate("recognition-upload");
			showToast(error.message === "Không thể khởi tạo khung xem trước webcam."
				? error.message
				: cameraErrorMessage(error));
		}
	}
}

async function captureWebcamImage() {
	const video = document.querySelector("#webcam-video");
	if (!video || !state.cameraStream || !video.videoWidth || !video.videoHeight) {
		showToast("Camera chưa sẵn sàng. Hãy đợi hình ảnh xuất hiện rồi thử lại.");
		return;
	}

	const canvas = document.createElement("canvas");
	canvas.width = video.videoWidth;
	canvas.height = video.videoHeight;
	const context = canvas.getContext("2d");
	if (!context) {
		showToast("Không thể tạo ảnh từ webcam. Hãy thử lại.");
		return;
	}
	let image;
	try {
		context.drawImage(video, 0, 0, canvas.width, canvas.height);
		image = await new Promise((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.9));
	} catch {
		showToast("Không thể chụp ảnh từ webcam. Hãy thử lại.");
		return;
	}
	if (!image) {
		showToast("Không thể chụp ảnh từ webcam. Hãy thử lại.");
		return;
	}

	const file = new File([image], `foodai-camera-${Date.now()}.jpg`, { type: "image/jpeg" });
	stopWebcamStream();
	if (!isValidRecognitionImage(file)) {
		navigate("recognition-upload");
		return;
	}
	await uploadAndCreateRecognitionSession(file);
}

function isValidRecognitionImage(file) {
	if (!file?.type.startsWith("image/")) {
		showToast("Hãy chọn tệp ảnh JPG hoặc PNG.");
		return false;
	}
	if (file.size > 10 * 1024 * 1024) {
		showToast("Ảnh phải có dung lượng tối đa 10MB.");
		return false;
	}
	return true;
}

async function loadRecognitionNutrition(session) {
	const items = Array.isArray(session?.items) ? session.items : [];
	await Promise.all(items.map(async (item) => {
		const foodId = item.selectedFoodId || item.predictedFoodId;
		const grams = Number(item.estimatedGrams);
		if (!foodId || !Number.isFinite(grams) || grams <= 0) {
			item.nutrition = null;
			return;
		}
		try {
			item.nutrition = await nutritionApi.calculate(foodId, grams);
		} catch (error) {
			item.nutrition = null;
		}
	}));
}

function localOffsetDateTime(dateValue) {
	const date = new Date(`${dateValue}T12:00:00`);
	const offsetMinutes = -date.getTimezoneOffset();
	const sign = offsetMinutes >= 0 ? "+" : "-";
	const offset = Math.abs(offsetMinutes);
	const pad = (value) => String(value).padStart(2, "0");
	return `${dateValue}T12:00:00${sign}${pad(Math.floor(offset / 60))}:${pad(offset % 60)}`;
}

function selectedMealType() {
	return {
		"Bữa sáng": "BREAKFAST",
		"Bữa trưa": "LUNCH",
		"Bữa tối": "DINNER",
		"Bữa phụ / Ăn vặt": "SNACK"
	}[state.mealChoice];
}

async function uploadAndCreateRecognitionSession(file) {
	if (!file || state.recognitionLoading) return;
	setRecognitionImage(file);
	state.recognitionLoading = true;
	fileInput.disabled = true;
	navigate("recognition-processing");

	try {
		const upload = await foodImageApi.upload(file);
		if (!upload?.secureUrl) throw new Error("Máy chủ không trả về URL ảnh đã tải lên.");

		const session = await recognitionApi.createSession({
			imageUrl: upload.secureUrl,
			cloudinaryPublicId: upload.publicId || null,
			mimeType: file.type || null,
			fileSizeBytes: file.size
		});
		if (!session?.sessionId || !session.status) throw new Error("Máy chủ trả về phiên nhận diện không hợp lệ.");
		state.recognitionSession = session;
		if (session.status === "COMPLETED") await loadRecognitionNutrition(session);
		state.recognitionLoading = false;
		if (state.recognitionSession?.status === "COMPLETED" && state.recognitionSession.items?.length) {
			navigate(state.recognitionSession.items.length > 1 ? "recognition-multi" : "recognition-single");
		} else {
			navigate("recognition-processing");
			const statusMessage = state.recognitionSession.status === "FAILED"
				? "Phiên nhận diện thất bại."
				: state.recognitionSession.status === "REJECTED"
					? "Ảnh bị từ chối xử lý."
					: state.recognitionSession.status === "COMPLETED"
						? "AI đã hoàn tất nhưng chưa trả về món ăn."
						: "Đang chờ kết quả AI.";
			showToast(`Phiên nhận diện #${state.recognitionSession.sessionId}: ${statusMessage}`);
		}
	} catch (error) {
		state.recognitionLoading = false;
		navigate("recognition-selected");
		showToast(error.message || "Không thể tải ảnh hoặc tạo phiên nhận diện.");
	} finally {
		fileInput.disabled = false;
	}
}

function initializeEvents() {
app.addEventListener("click", async (event) => {
	const target = event.target.closest("[data-action]");
	if (!target) return;
	const action = target.dataset.action;
	if (action.startsWith("auth-")) { navigate(action); return; }
	if (action === "logout") { setToken(null); state.authenticated = false; state.user = null; state.me = null; state.setupGoal = null; showToast("Đã đăng xuất khỏi FoodAI."); navigate("auth-login", false); return; }
	if (action === "browse") { fileInput.click(); return; }
	if (action === "recognition-processing") {
		if (state.selectedFile) await uploadAndCreateRecognitionSession(state.selectedFile);
		else navigate(action);
		return;
	}
	if (action === "refresh-recognition" && state.recognitionSession?.sessionId) {
		try {
			const session = await recognitionApi.getSession(state.recognitionSession.sessionId);
			if (!session?.sessionId || !session.status) throw new Error("Máy chủ trả về phiên nhận diện không hợp lệ.");
			state.recognitionSession = session;
			if (session.status === "COMPLETED") await loadRecognitionNutrition(session);
			if (state.recognitionSession.status === "COMPLETED" && state.recognitionSession.items?.length) {
				navigate(state.recognitionSession.items.length > 1 ? "recognition-multi" : "recognition-single");
			} else {
				render();
				const statusMessage = state.recognitionSession.status === "FAILED"
					? "Phiên nhận diện thất bại. Hãy thử tải ảnh khác."
					: state.recognitionSession.status === "REJECTED"
						? "Ảnh bị từ chối xử lý."
						: state.recognitionSession.status === "COMPLETED"
							? "AI đã hoàn tất nhưng chưa trả về món ăn."
							: "Chưa có kết quả mới từ AI.";
				showToast(statusMessage);
			}
		} catch (error) {
			showToast(error.message || "Không thể tải trạng thái phiên nhận diện.");
		}
		return;
	}
	if (action === "retry-recognition") {
		if (state.selectedFile) await uploadAndCreateRecognitionSession(state.selectedFile);
		else showToast("Hãy chọn lại ảnh để thử nhận diện.");
		return;
	}
	if (action === "open-camera") { await openCamera(); return; }
	if (action === "webcam-close" || (action === "webcam-overlay-close" && event.target === target)) {
		stopWebcamStream();
		navigate("recognition-upload");
		return;
	}
	if (action === "webcam-capture") { await captureWebcamImage(); return; }
	if (action === "remove-image") {
		if (state.selectedImage?.startsWith("blob:")) URL.revokeObjectURL(state.selectedImage);
		state.selectedImage = null; state.selectedFile = null; state.recognitionSession = null;
		navigate("recognition-upload"); return;
	}
	if (action === "meal-choice") { state.mealChoice = target.dataset.meal; render(); return; }
	if (action === "close-modal") { if (event.target === target || target.classList.contains("icon-button")) { document.querySelector(".modal-overlay")?.remove(); } return; }
	if (action === "save-meal") {
		const saveButton = target;
		if (saveButton.disabled) return;
		saveButton.disabled = true;
		try {
			const session = state.recognitionSession;
			const items = Array.isArray(session?.items) ? session.items : [];
			if (session?.status !== "COMPLETED" || !items.length) {
				throw new Error("Chưa có kết quả nhận diện để lưu.");
			}
			const dateInput = document.querySelector(".modal-dialog input[type='date']");
			const date = dateInput?.value || localDateInputValue();
			const mealType = selectedMealType();
			if (!mealType) throw new Error("Hãy chọn loại bữa ăn hợp lệ.");

			const diaryItems = [];
			for (const item of items) {
				if (item.status === "REJECTED") continue;
				if (!item.id) throw new Error("Thiếu mã món nhận diện; không thể xác nhận món này.");
				const foodId = item.selectedFoodId || item.predictedFoodId;
				if (!foodId) throw new Error(`Chưa ánh xạ món "${item.predictedLabel || "không xác định"}" sang món ăn trong danh mục.`);
				const grams = Number(item.estimatedGrams);
				const review = item.status === "CONFIRMED"
					? null
					: await recognitionApi.reviewItem(session.sessionId, item.id, {
						status: "CONFIRMED",
						foodId,
						...(Number.isFinite(grams) && grams > 0 ? { consumedGrams: grams } : {})
					});
				if (review?.items) {
					const updated = review.items.find((result) => result.id === item.id);
					if (updated) Object.assign(item, updated);
				}
				const diaryItem = { recognitionItemId: item.id };
				if (Number.isFinite(grams) && grams > 0) diaryItem.consumedGrams = grams;
				diaryItems.push(diaryItem);
			}
			if (!diaryItems.length) throw new Error("Không có món nào được xác nhận để lưu.");

			await diaryApi.saveMeal({
				mealType,
				eatenAt: localOffsetDateTime(date),
				items: diaryItems
			});
			document.querySelector(".modal-overlay")?.remove();
			state.diaryDate = date;
			navigate("diary", false);
			showToast("Đã lưu bữa ăn vào nhật ký.");
		} catch (error) {
			showToast(error.message || "Không thể lưu bữa ăn vào nhật ký.");
		} finally {
			saveButton.disabled = false;
		}
		return;
	}
	if (action === "save-meal-dismiss") { document.querySelector(".modal-overlay")?.remove(); return; }
	if (action === "select-choice") {
		const cards = [...target.parentElement.querySelectorAll(".choice-card")];
		cards.forEach((item) => item.classList.remove("is-selected"));
		target.classList.add("is-selected");
		state.setupGoal = cards.indexOf(target);
		return;
	}
	if (action === "date-prev" || action === "date-next") {
		const date = new Date(`${state.diaryDate}T12:00:00`);
		date.setDate(date.getDate() + (action === "date-prev" ? -1 : 1));
		await loadDiaryForDate(localDateInputValue(date));
		return;
	}
	if (action === "diary-date-picker") {
		const pickerInput = document.querySelector("#diary-date-picker");
		if (!pickerInput) return;
		try {
			if (typeof pickerInput.showPicker === "function") pickerInput.showPicker();
			else pickerInput.click();
		} catch (error) {
			pickerInput.click();
		}
		return;
	}
	if (action === "diary-retry") { await loadDiaryForDate(); return; }
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
			state.authenticated = false;
			state.user = null;
			state.me = null;
			await fetchMe();
			showToast(`Chào mừng ${response.user?.fullName || "bạn"} quay trở lại FoodAI!`);
			navigate("dashboard", false);
		} catch (error) {
			setToken(null);
			state.authenticated = false;
			state.user = null;
			state.me = null;
			navigate("auth-login", false);
			showToast(error.message);
		}
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
	const diaryDate = event.target.closest("#diary-date-picker");
	if (diaryDate?.value) {
		loadDiaryForDate(diaryDate.value);
		return;
	}
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
	fileInput.value = "";
	if (!isValidRecognitionImage(file)) return;
	uploadAndCreateRecognitionSession(file);
});

cameraFileInput.addEventListener("change", () => {
	const file = cameraFileInput.files?.[0];
	cameraFileInput.value = "";
	if (!file) return;
	if (!isValidRecognitionImage(file)) {
		navigate("recognition-upload");
		return;
	}
	navigate("recognition-upload");
	uploadAndCreateRecognitionSession(file);
});

window.addEventListener("pagehide", stopWebcamStream);

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
	if (!isValidRecognitionImage(file)) return;
	uploadAndCreateRecognitionSession(file);
});
}
