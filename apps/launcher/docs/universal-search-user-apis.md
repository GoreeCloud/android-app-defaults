# GoreeCloud Launcher — User-Owned Inline API Sources

**Requirement level:** Mandatory product direction  
**Product:** GoreeCloud Launcher  
**Lifecycle:** Development candidate  
**Implementation:** `core/launcher/LauncherUserApiSources.kt` and `ui/LauncherUserApiSourcesUi.kt`  
**Candidate branch:** `feature/launcher-byok-inline-api-search-20261006`  
**Integration authority:** Protected exact-head CI, review, and representative-device acceptance. Source presence is not release acceptance.

## Product requirement

Universal Search must let the user configure their own AI or search/chat API credentials from **Universal Search → Search Sources**, then view answers directly inside Launcher. Users must not need to open ChatGPT, Claude, Gemini, Perplexity, or a dedicated application when the provider's API is configured.

The first implementation slice adds four distinct **API** sources—ChatGPT/OpenAI, Claude/Anthropic, Gemini/Google, and Perplexity—and up to eight user-named **custom OpenAI-compatible chat-completions** endpoints. These are separate from app handoffs and from the opt-in Google Drive provider. No implication that a ChatGPT subscription automatically includes OpenAI API usage is permitted: account/API billing and provider retention are determined by each provider.

Each API connection needs a model ID and its own API key; custom endpoints additionally require a public HTTPS chat-completions URL. A saved connection can be enabled, disabled, edited without displaying the prior key, or disconnected (which deletes its saved key).

## Execution contract

- Typing in the Universal Search field **must not** transmit keystrokes to these APIs. Enabling an API source **must not** make it part of automatic local query aggregation.
- **Ask [provider]** is a separate, deliberate user tap. It sends only the currently visible question to the selected API, and the returned answer renders inside Universal Search with a selectable text body.
- Each request stands alone; there is no silent follow-up or external query fan-out.
- Queries and answers are transient UI state and are not persisted as Launcher history. Changing the query clears the current answer. There is no advertising, paid-placement ranking, analytics, or sponsored-provider behavior.
- Provider charges can accrue per deliberate request. Errors expose generic setup/network/quota guidance, not provider error bodies or credentials.

## Credential and transport boundary

- Source settings and API keys are encrypted together with AES-256-GCM using an Android Keystore key and stored under app-private `noBackupFilesDir`. Android cloud backup, device-transfer backup, portable preferences, Room workspace state, and normal Search preferences are not credential storage.
- Existing keys are never read back into editable UI controls. Empty key fields on edit preserve the encrypted existing key; disconnect explicitly deletes the entry.
- The provider-specific direct HTTPS adapters support OpenAI chat completions, Anthropic Messages, Gemini `generateContent`, and Perplexity's chat-completions format. The custom adapter supports the OpenAI-compatible JSON schema only; arbitrary proprietary custom API protocols are **not yet supported**.
- Custom endpoints reject HTTP, credentials in URLs, embedded query parameters, IP-literal/localhost/reserved-domain targets and non-default TLS ports. Public DNS-address preflight rejects loopback, link-local, site-local, shared/private, and multicast addresses. HTTP redirects are disabled. This is a minimum client boundary, not a substitute for a future hardened trusted broker or DNS-pinned transport if broader endpoint protocols are added.
- Requests have explicit bounded input, response size, answer length, and network timeouts. The source uses Android's normal TLS validation; it does not add permissive trust managers.
- Secrets must never appear in source control, log messages, tests, result labels, crash reporting, diagnostics, or network-query parameters.

## Current candidate acceptance requirements

Fresh exact-head validation must cover source-manifest verification, compilation, static/privacy guards, JVM URL/private-address policy tests, Android 16 runtime instrumentation, Network Security Config/cleartext behavior, cancellation, error handling, encrypted storage and restore isolation, and controlled real-API testing using dedicated expendable provider keys.

Representative-device testing is still required for onboarding, Search Sources connection/edit/disconnect affordances, Screen Reader/Switch Access, large-text/IME, short-screen scrolling, rotation/process recreation, empty/invalid credentials, network failure, permission/profile boundaries, performance/power, and developer signing/update continuity.

**Current limit:** General custom GET endpoints, non-OpenAI-compatible response schemas, multi-turn agent state, backend secret vault sync, and trusted server-side credential brokering remain future work. Do not present them as implemented.

**Dependency:** This branch is stacked on the current PR #277 Development candidate. Integration must not proceed while the base candidate has unresolved protected runtime or owner acceptance gates.
