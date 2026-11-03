function localDateInputValue(date = new Date()) {
	const year = date.getFullYear();
	const month = String(date.getMonth() + 1).padStart(2, "0");
	const day = String(date.getDate()).padStart(2, "0");
	return `${year}-${month}-${day}`;
}

const state = {
	view: location.hash.slice(1) || "auth-login",
	mobilePreview: false,
	selectedImage: null,
	selectedFile: null,
	recognitionLoading: false,
	recognitionSession: null,
	cameraOn: false,
	cameraStream: null,
	webcamStarting: false,
	mealChoice: "Bữa trưa",
	diaryTab: "today",
	search: "",
	user: null,
	me: null,
	authenticated: false,
	setupGoal: null,
	diaryDate: localDateInputValue(),
	diaryMeals: [],
	diaryTotal: null,
	diaryLoading: false,
	diaryError: null,
	diaryRequestId: 0
};

const mealNames = ["Bữa sáng", "Bữa trưa", "Bữa tối", "Bữa phụ / Ăn vặt"];
