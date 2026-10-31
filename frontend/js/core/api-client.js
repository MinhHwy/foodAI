(function (global) {
	"use strict";

	class ApiError extends Error {
		constructor(message, { kind, status = null, body = null, cause = null } = {}) {
			super(message);
			this.name = "ApiError";
			this.kind = kind;
			this.status = status;
			this.body = body;
			if (cause !== null) this.cause = cause;
		}
	}

	function buildUrl(path) {
		if (typeof path !== "string" || !path.trim()) {
			throw new TypeError("API path must be a non-empty string.");
		}

		const baseUrl = global.FoodAIConfig?.apiBaseUrl;
		if (typeof baseUrl !== "string" || !baseUrl.trim()) {
			throw new ApiError("API base URL is not configured.", { kind: "configuration" });
		}

		try {
			const base = new URL(baseUrl);
			const url = new URL(path.replace(/^\/+/, ""), `${base.href.replace(/\/?$/, "/")}`);
			if (!["http:", "https:"].includes(base.protocol) || url.origin !== base.origin) {
				throw new TypeError("API requests must use the configured HTTP(S) origin.");
			}
			return url;
		} catch (cause) {
			throw new ApiError("API base URL or path is invalid.", { kind: "configuration", cause });
		}
	}

	async function readResponseBody(response) {
		if (response.status === 204) return null;

		const text = await response.text();
		if (!text) return null;

		const contentType = response.headers.get("content-type") || "";
		const isJson = /\bapplication\/(?:[\w.-]+\+)?json\b/i.test(contentType);
		if (!isJson && contentType) return text;

		try {
			return JSON.parse(text);
		} catch (cause) {
			if (!isJson) return text;
			throw new ApiError("Backend returned invalid JSON.", {
				kind: "response",
				status: response.status,
				body: text,
				cause
			});
		}
	}

	function responseMessage(body, response) {
		if (body && typeof body === "object" && typeof body.message === "string" && body.message.trim()) {
			return body.message;
		}
		if (typeof body === "string" && body.trim()) return body;
		return response.statusText || `Request failed with HTTP ${response.status}.`;
	}

	async function request(path, options = {}) {
		const {
			method = "GET",
			headers: suppliedHeaders,
			body,
			signal: externalSignal,
			timeoutMs = global.FoodAIConfig?.requestTimeoutMs
		} = options;
		const headers = new Headers(suppliedHeaders || {});
		if (!headers.has("Accept")) headers.set("Accept", "application/json");
		const url = buildUrl(path);

		const controller = new AbortController();
		let timedOut = false;
		let timeoutId;
		const abortFromCaller = () => controller.abort(externalSignal.reason);

		if (externalSignal?.aborted) {
			abortFromCaller();
		} else if (externalSignal) {
			externalSignal.addEventListener("abort", abortFromCaller, { once: true });
		}

		if (Number.isFinite(timeoutMs) && timeoutMs > 0) {
			timeoutId = global.setTimeout(() => {
				timedOut = true;
				controller.abort();
			}, timeoutMs);
		}

		try {
			const response = await global.fetch(url, {
				method,
				headers,
				body,
				signal: controller.signal
			});
			const responseBody = await readResponseBody(response);

			if (!response.ok) {
				throw new ApiError(responseMessage(responseBody, response), {
					kind: "http",
					status: response.status,
					body: responseBody
				});
			}

			if (responseBody && typeof responseBody === "object" && responseBody.success === false) {
				throw new ApiError(responseMessage(responseBody, response), {
					kind: "application",
					status: response.status,
					body: responseBody
				});
			}

			return responseBody;
		} catch (error) {
			if (error instanceof ApiError) throw error;

			if (timedOut) {
				throw new ApiError("Request timed out.", { kind: "timeout", cause: error });
			}
			if (externalSignal?.aborted || controller.signal.aborted) {
				throw new ApiError("Request was aborted.", { kind: "abort", cause: error });
			}
			throw new ApiError("Unable to reach the backend.", { kind: "network", cause: error });
		} finally {
			if (timeoutId !== undefined) global.clearTimeout(timeoutId);
			externalSignal?.removeEventListener("abort", abortFromCaller);
		}
	}

	function jsonRequest(method, path, data, options = {}) {
		const headers = new Headers(options.headers || {});
		headers.set("Content-Type", "application/json");
		return request(path, {
			...options,
			method,
			headers,
			body: data === undefined ? undefined : JSON.stringify(data)
		});
	}

	function postFormData(path, formData, options = {}) {
		if (!(formData instanceof global.FormData)) {
			throw new TypeError("postFormData requires a FormData instance.");
		}

		const headers = new Headers(options.headers || {});
		headers.delete("Content-Type");
		return request(path, {
			...options,
			method: "POST",
			headers,
			body: formData
		});
	}

	global.FoodAIApi = Object.freeze({
		ApiError,
		request,
		get: (path, options) => request(path, { ...options, method: "GET" }),
		postJson: (path, data, options) => jsonRequest("POST", path, data, options),
		putJson: (path, data, options) => jsonRequest("PUT", path, data, options),
		patchJson: (path, data, options) => jsonRequest("PATCH", path, data, options),
		delete: (path, options) => request(path, { ...options, method: "DELETE" }),
		postFormData
	});
})(window);
