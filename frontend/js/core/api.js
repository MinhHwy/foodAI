const API_BASE = "http://localhost:8080";
const TOKEN_KEY = "foodai-token";

const SEX_LABELS = { MALE: "Nam", FEMALE: "Nữ", UNSPECIFIED: "Chưa xác định" };
const ACTIVITY_LABELS = {
	SEDENTARY: "Ít vận động",
	LIGHT: "Vận động nhẹ (1-3 ngày / tuần)",
	MODERATE: "Vận động vừa (3-5 ngày / tuần)",
	ACTIVE: "Vận động nhiều (6-7 ngày / tuần)",
	VERY_ACTIVE: "Vận động rất nhiều (2 lần / ngày)"
};
const GOAL_LABELS = { LOSE: "Giảm cân", MAINTAIN: "Duy trì cân nặng", GAIN: "Tăng cân & Cơ" };

function getToken() {
	try { return localStorage.getItem(TOKEN_KEY); } catch (error) { return null; }
}

function setToken(token) {
	try { if (token) localStorage.setItem(TOKEN_KEY, token); else localStorage.removeItem(TOKEN_KEY); } catch (error) { /* blocked storage */ }
}

async function apiFetch(path, options = {}) {
	const method = options.method || "GET";
	const auth = options.auth !== false;
	const headers = {};
	if (options.body !== undefined) headers["Content-Type"] = "application/json";
	const token = auth ? getToken() : null;
	if (token) headers["Authorization"] = `Bearer ${token}`;
	let response;
	try {
		response = await fetch(`${API_BASE}${path}`, {
			method, headers,
			body: options.body === undefined ? undefined : JSON.stringify(options.body)
		});
	} catch (error) {
		throw new Error("Không kết nối được máy chủ FoodAI. Hãy đảm bảo backend đang chạy trên " + API_BASE + ".");
	}
	let payload = null;
	try { payload = await response.json(); } catch (error) { payload = null; }
	if (!response.ok) {
		const message = (payload && (payload.detail || payload.title)) || `Lỗi ${response.status} từ máy chủ.`;
		throw new Error(message);
	}
	return payload;
}

const authApi = {
	register: (data) => apiFetch("/api/v1/auth/register", { method: "POST", body: data, auth: false }),
	login: (data) => apiFetch("/api/v1/auth/login", { method: "POST", body: data, auth: false })
};

const profileApi = {
	patchProfile: (data) => apiFetch("/api/v1/me/profile", { method: "PATCH", body: data }),
	patchSettings: (data) => apiFetch("/api/v1/me/settings", { method: "PATCH", body: data }),
	putGoals: (goalType) => apiFetch("/api/v1/me/goals", { method: "PUT", body: { goalType: goalType } })
};

async function fetchMe() {
	const me = await apiFetch("/api/v1/me");
	state.user = me.user || null;
	state.me = me;
	return me;
}

function fmtNumber(value) {
	return Number(value).toLocaleString("en-US");
}

function initials(fullName) {
	const part = String(fullName || "Minh Anh").trim().split(/\s+/).filter(Boolean);
	return (part.map((item) => item[0].toUpperCase()).join("") || "MA").slice(0, 3);
}

function profileDisplay() {
	const profile = state.me?.profile;
	const goals = state.me?.goals;
	const goalType = state.me?.goals?.goalType || (state.setupGoal != null ? ["LOSE", "MAINTAIN", "GAIN"][state.setupGoal] : null) || "LOSE";
	const joined = state.user?.createdAt && !isNaN(new Date(state.user.createdAt)) ? new Date(state.user.createdAt) : null;
	if (!profile || !goals) {
		return {
			name: state.user?.fullName || "Minh Anh",
			email: state.user?.email || "minhanh@gmail.com",
			joined: joined ? `${String(joined.getMonth() + 1).padStart(2, "0")}/${joined.getFullYear()}` : "01/2026",
			heightCm: "170", weightKg: "65.0", ageYears: "21", sex: "Nam",
			activity: "Vận động nhẹ (1-3 ngày / tuần)", daily: "2,000", bmr: "1,650", tdee: "2,200",
			protein: "150g", carbs: "225g", fat: "55g", goalLabel: "Giảm cân",
			updated: "Hôm qua, 07:30"
		};
	}
	const dayNote = goalType === "LOSE" ? "-500 kcal/ngày" : goalType === "GAIN" ? "+300 kcal/ngày" : "duy trì theo TDEE";
	return {
		name: state.user?.fullName || "Minh Anh",
		email: state.user?.email || "minhanh@gmail.com",
		joined: joined ? `${String(joined.getMonth() + 1).padStart(2, "0")}/${joined.getFullYear()}` : "01/2026",
		heightCm: profile.heightCm != null ? profile.heightCm : "—",
		weightKg: profile.currentWeightKg != null ? Number(profile.currentWeightKg).toFixed(1) : "—",
		ageYears: profile.ageYears != null ? profile.ageYears : "—",
		sex: SEX_LABELS[profile.biologicalSex] || "Chưa xác định",
		activity: ACTIVITY_LABELS[profile.activityLevel] || "Chưa xác định",
		daily: fmtNumber(goals.dailyCaloriesKcal ?? 2000),
		bmr: fmtNumber(goals.estimatedBmrKcal ?? 0),
		tdee: fmtNumber(goals.estimatedTdeeKcal ?? 0),
		protein: `${fmtNumber(goals.proteinTargetG ?? 0)}g`,
		carbs: `${fmtNumber(goals.carbsTargetG ?? 0)}g`,
		fat: `${fmtNumber(goals.fatTargetG ?? 0)}g`,
		goalLabel: GOAL_LABELS[goalType] || "Giảm cân",
		updated: profile.updatedAt ? new Date(profile.updatedAt).toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit" }) : "Hôm qua, 07:30",
		dayNote
	};
}
