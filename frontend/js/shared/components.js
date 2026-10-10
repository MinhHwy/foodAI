const icon = (name, size = 17) => `<i data-lucide="${name}" style="width:${size}px;height:${size}px"></i>`;

function esc(value) {
	return String(value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}

function button(label, action, kind = "primary", iconName = "") {
	return `<button type="button" class="${kind}-button" data-action="${action}">${iconName ? icon(iconName, 15) : ""}<span>${label}</span></button>`;
}

function pageHeading(title, description, action = "") {
	return `<header class="page-heading"><div><h1>${title}</h1><p>${description}</p></div>${action}</header>`;
}

function metric(label, value, suffix, note, percent, color = "#078b59") {
	return `<section class="surface metric-card"><div class="metric-label">${label}</div><div class="metric-value" style="color:${color}">${value}<small>${suffix}</small></div><div class="metric-note">${note}</div><div class="metric-meter"><span style="width:${percent}%;background:${color}"></span></div></section>`;
}

function foodCard(food, index, recommendation = false) {
	return `<article class="${recommendation ? "recommend-card" : "food-card"}"><div class="food-card-photo"><img src="${photo(food.image)}" alt="${esc(food.name)}" loading="lazy"><span class="hero-kicker" style="position:absolute;left:8px;top:8px">${food.calories} kcal</span></div><div class="food-card-body"><span class="meal-type">${recommendation ? "GỢI Ý PHÙ HỢP" : "MÓN VIỆT"}</span><h3>${esc(food.name)}</h3><p>${food.protein}g đạm · ${food.carbs}g carb · ${food.fat}g béo</p><div class="food-card-footer"><span>${recommendation ? `+${Math.max(80, 1070 - food.calories)} kcal còn lại` : `${food.calories} kcal / phần`}</span><button class="text-button" data-action="${recommendation ? "recommend-detail" : "food-pick"}" data-food="${index}">${recommendation ? "Xem món" : "Chọn món"}</button></div></div></article>`;
}

function showToast(message) {
	const toast = document.createElement("div");
	toast.className = "toast";
	toast.innerHTML = `${icon("circle-check", 15)}<span>${esc(message)}</span>`;
	toastRegion.append(toast);
	if (window.lucide?.createIcons) window.lucide.createIcons();
	window.setTimeout(() => toast.remove(), 3000);
}
