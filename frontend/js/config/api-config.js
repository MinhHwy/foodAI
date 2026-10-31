window.FoodAIConfig = Object.freeze({
	apiBaseUrl: (window.FOODAI_API_BASE_URL || "http://localhost:8080").replace(/\/+$/, ""),
	requestTimeoutMs: 15000
});
