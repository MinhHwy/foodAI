const app = document.querySelector("#app");
const fileInput = document.querySelector("#food-file");
const cameraFileInput = document.querySelector("#camera-file");
const toastRegion = document.querySelector("#toast-region");

initializeEvents();

async function initializeApp() {
	const initialView = state.view;
	const token = getToken();
	if (!token) {
		state.authenticated = false;
		state.user = null;
		state.me = null;
		navigate("auth-login", false);
		return;
	}

	state.view = "auth-login";
	render();
	try {
		await fetchMe();
		navigate(initialView.startsWith("auth-") ? "dashboard" : initialView, false);
	} catch (error) {
		state.authenticated = false;
		state.user = null;
		state.me = null;
		if (error.status === 401 || error.status === 403) setToken(null);
		navigate("auth-login", false);
	}
}

initializeApp();
