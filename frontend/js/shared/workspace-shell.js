function mobileNavigation() {
	return `<nav class="mobile-nav" aria-label="Điều hướng chính">
		<button data-action="dashboard" class="${state.view === "dashboard" ? "is-active" : ""}">${icon("house", 18)}<span>Trang chủ</span></button>
		<button data-action="diary" class="${state.view === "diary" || state.view === "trends" ? "is-active" : ""}">${icon("notebook-tabs", 18)}<span>Nhật ký</span></button>
		<button data-action="recognition-upload" class="scan-tab">${icon("camera", 21)}</button>
		<button data-action="recommendations" class="${state.view.startsWith("recommend") ? "is-active" : ""}">${icon("sparkles", 18)}<span>Gợi ý</span></button>
		<button data-action="profile" class="${state.view === "profile" ? "is-active" : ""}">${icon("user-round", 18)}<span>Cá nhân</span></button>
	</nav>`;
}

function appShell(content) {
	const titleMap = {
		dashboard: "Không gian làm việc › Dinh dưỡng cá nhân",
		"recognition-upload": "Nhận diện Món ăn",
		"recognition-selected": "Nhận diện Món ăn",
		"recognition-camera": "Nhận diện Món ăn",
		"recognition-processing": "Kết quả Quét AI",
		"recognition-single": "Kết quả Quét AI",
		"recognition-multi": "Kết quả Quét AI",
		"recognition-save": "Nhật ký dinh dưỡng",
		"recognition-search": "Thay đổi món ăn",
		diary: "Nhật ký & Tiến độ dinh dưỡng",
		trends: "Thống kê & Xu hướng",
		"diary-empty": "Nhật ký ăn uống",
		"calorie-status": "Trạng thái calo",
		recommendations: "Gợi ý thực đơn",
		"recommend-detail": "Chi tiết món gợi ý",
		profile: "Hồ sơ người dùng",
		settings: "Cài đặt FoodAI",
		"setup-health": "Thiết lập hồ sơ sức khỏe",
		"setup-goal": "Thiết lập mục tiêu",
		"setup-calories": "Thiết lập calo",
		admin: "Quản trị cơ sở dữ liệu",
		"admin-edit": "Quản lý món ăn"
	};
	const activeNav = state.view.startsWith("recognition")
		? "recognition-upload"
		: state.view.startsWith("recommend")
			? "recommendations"
			: state.view === "diary" || state.view === "trends"
				? "diary"
				: state.view.startsWith("setup") || state.view === "profile" || state.view === "settings"
					? "profile"
					: state.view.startsWith("admin")
						? "admin"
						: "dashboard";

	return `<div class="app-shell ${state.mobilePreview ? "mobile-preview" : ""}">
		<aside class="sidebar">
			<button class="brand" data-action="dashboard" style="width:100%;border:0;background:white;text-align:left;cursor:pointer">
				<span class="brand-mark">${icon("leaf", 19)}</span>
				<span class="brand-copy"><strong>FoodAI</strong><span>SMART NUTRITION</span></span>
			</button>
			<nav class="side-nav" aria-label="Điều hướng">
				<button class="nav-link ${activeNav === "dashboard" ? "is-active" : ""}" data-action="dashboard">${icon("layout-dashboard")}<span>Bảng điều khiển</span></button>
				<button class="nav-link ${activeNav === "recognition-upload" ? "is-active" : ""}" data-action="recognition-upload">${icon("scan-line")}<span>Quét & Nhận diện AI</span></button>
				<button class="nav-link ${activeNav === "diary" ? "is-active" : ""}" data-action="diary">${icon("notebook-tabs")}<span>Nhật ký & Thống kê</span></button>
				<button class="nav-link ${activeNav === "recommendations" ? "is-active" : ""}" data-action="recommendations">${icon("sparkles")}<span>Gợi ý thực đơn</span></button>
				<button class="nav-link ${activeNav === "profile" ? "is-active" : ""}" data-action="profile">${icon("settings-2")}<span>Cài đặt & Hồ sơ</span></button>
				<button class="nav-link ${activeNav === "admin" ? "is-active" : ""}" data-action="admin">${icon("database")}<span>Quản trị món ăn</span></button>
			</nav>
			<button class="sidebar-profile" data-action="profile">
				<span class="avatar">MA</span>
				<span class="sidebar-profile-copy"><strong>Minh Anh</strong><small>minhanh@gmail.com · Active Pro</small></span>
			</button>
		</aside>
		<section class="workspace">
			<header class="topbar">
				<div class="topbar-title">${titleMap[state.view] || "FoodAI Workspace"}</div>
				<div class="topbar-actions">
					<span class="streak">${icon("flame", 13)} 7 Ngày Streak</span>
					<button class="icon-button" aria-label="Thông báo" data-action="notifications">${icon("bell", 16)}<span class="notification-dot"></span></button>
					<button class="primary-button" data-action="recognition-upload">${icon("plus", 14)} Ghi nhận bữa ăn</button>
				</div>
			</header>
			<section class="content">${content}</section>
			${mobileNavigation()}
		</section>
	</div>`;
}
