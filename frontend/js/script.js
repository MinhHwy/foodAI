const app = document.querySelector("#app");
const fileInput = document.querySelector("#food-file");
const toastRegion = document.querySelector("#toast-region");

initializeEvents();
render();

if (getToken()) {
	fetchMe().then(render).catch(() => { setToken(null); });
}
