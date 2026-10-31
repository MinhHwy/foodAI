# BE-06: AI recognition integration

## Status

**Integration framework implemented; real AI integration pending API contract/service availability.**

The repository does not contain an AI service URL, endpoint, request/response example, OpenAPI definition, deployed service configuration, or Jira CSV describing BE-06. The database SQL mentions a Python/YOLOv8 service, and the schema has recognition session/item/model-class mappings, but neither establishes an HTTP protocol. The Backend currently has no Cloudinary upload endpoint or recognition business controller. The static Frontend keeps the selected image locally and simulates analysis results.

The Backend now has a protected `POST /api/v1/recognitions` endpoint that accepts one multipart field named `image`. It forwards the file directly to the AI service only when the multipart integration settings are explicitly complete. The raw JSON response is passed through without claiming an unconfirmed response schema. No image is uploaded to Cloudinary, no recognition session is persisted, and no calories are inferred. In the default configuration the endpoint returns HTTP 503 with code `AI_SERVICE_NOT_CONFIGURED`; it never returns a fabricated recognition result.

The multipart mode is a conditional adapter, not an agreed AI contract. Do not configure it for a live AI service until the AI team confirms the request format, endpoint, and response contract. If the confirmed service expects an image URL, JSON/base64, extra request fields, or another protocol, adapt the client rather than sending an incompatible request.

## Conditional configuration

| Environment variable | Purpose | Default |
|---|---|---|
| `AI_SERVICE_BASE_URL` | AI service HTTP(S) base URL | unset; disabled |
| `AI_SERVICE_RECOGNITION_PATH` | Recognition path, beginning with `/` | unset; disabled |
| `AI_SERVICE_REQUEST_MODE` | Must be `MULTIPART` to enable this adapter | unset; disabled |
| `AI_SERVICE_IMAGE_PART_NAME` | Multipart file field name required by the AI contract | unset; disabled |
| `AI_SERVICE_CONNECT_TIMEOUT` | HTTP connect timeout | `2s` |
| `AI_SERVICE_RESPONSE_TIMEOUT` | HTTP response/read timeout | `30s` |
| `RECOGNITION_MAX_IMAGE_BYTES` | Backend image size limit | `10485760` |
| `RECOGNITION_MAX_IMAGE_SIZE` | Servlet multipart file limit | `10MB` |
| `RECOGNITION_MAX_REQUEST_SIZE` | Servlet multipart request limit | `11MB` |

No secret or AI API key is currently configured. If the service requires authentication, the team must agree how the credential is provisioned and rotated before enabling production calls. Credentials must remain server-side.

## Current Backend behavior

- `POST /api/v1/recognitions` consumes `multipart/form-data` with `image`; it requires authentication under the existing stateless security policy.
- Empty files return HTTP 400, non-image media types return HTTP 415, and oversized files return HTTP 413.
- AI HTTP errors map to safe 502 responses; connection failures map to 503; timeouts map to 504; malformed/empty JSON maps to 502. Upstream bodies, stack traces, image data, and tokens are not returned or logged.
- The client does not retry and does not persist data.
- Valid JSON is returned as opaque JSON until the AI response schema is agreed. Low-confidence semantics and no-detection status cannot be interpreted yet.

## Information required from the AI team

1. Service environment/base URL for local development, test, and deployment.
2. Recognition endpoint path, HTTP method, supported protocol/version, and availability/health endpoint.
3. Request transport: multipart file, signed image URL, JSON/base64, or another format. If multipart, exact part name, accepted media types, required fields, and whether one or multiple images are supported.
4. Maximum image bytes, image dimensions/formats, and expected processing latency.
5. Authentication method (if any), credential distribution, and rotation requirements.
6. Exact response JSON schema with examples for one detection, multiple detections, no detections, and low confidence.
7. Class-label vocabulary and versioning, confidence scale/range and threshold policy.
8. Bounding-box coordinate convention/normalization and whether boxes are always present.
9. Error status codes and JSON error schema, including invalid image, unsupported format, model unavailable, and rate limiting.
10. Whether an empty result differs from a model/service failure, and how the model version is reported.
11. Retry/idempotency and rate-limit guidance, if the service expects any.

After these details are confirmed, replace the provisional opaque JSON boundary with typed request/response DTOs, response validation, class mapping, and confidence/no-detection handling. Recognition persistence and food confirmation should be added only against the agreed BE-07/BE-08 workflow and existing schema.

## Tests

Run the transport tests without MySQL or a running AI service:

```powershell
.\mvnw.cmd '-Dtest=RestClientAiRecognitionClientTest' test
```

These tests use a mocked HTTP server. They verify transport/error handling only; they do not prove that the real AI service is available or that its contract matches the provisional multipart adapter.
