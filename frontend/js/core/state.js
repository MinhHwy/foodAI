const state = {
	view: location.hash.slice(1) || "dashboard",
	mobilePreview: false,
	selectedImage: null,
	cameraOn: false,
	mealChoice: "Bữa trưa",
	diaryTab: "today",
	search: "",
	user: null,
	me: null,
	setupGoal: null
};

const mealNames = ["Bữa sáng", "Bữa trưa", "Bữa tối", "Bữa phụ / Ăn vặt"];
