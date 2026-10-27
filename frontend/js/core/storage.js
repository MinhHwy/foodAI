const savedMeals = JSON.parse(localStorage.getItem("foodai-meals") || "null") || [
	{ name: "Phở bò tái nạm", meal: "Bữa sáng", calories: 420, protein: 28, carbs: 52, fat: 12, image: 2 },
	{ name: "Cơm tấm sườn nạc", meal: "Bữa trưa", calories: 620, protein: 35, carbs: 78, fat: 18, image: 3 }
];

function persistSavedMeals() {
	localStorage.setItem("foodai-meals", JSON.stringify(savedMeals));
}
