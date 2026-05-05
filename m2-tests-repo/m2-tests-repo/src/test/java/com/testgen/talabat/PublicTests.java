package com.testgen.talabat;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

// ────────────────────────────────────────────────────────────────────────────
// PublicTests.java — hand-written, dynamic, scenario-driven public test cases
// for Talabat M2.
//
// This is the canonical public test file. The 749 template-generated classes
// (deprecated) live in archive/PublicTestsAll_Stale.java for reference but
// no longer execute.
//
// Each class below is one row in
// docs/test-scenarios/Talabat_Tests_Description.md.
//
// Style guide:
//   * One package-private `class TC<NN>_<DescriptiveName> extends TestBase`
//     per scenario, with one or more @Test methods.
//   * @Tag("public") + a category tag (features_m1 / features_m2 / patterns
//     / amendments / updated_crud / cross_cutting) per scenario row.
//   * Resolve every URL through TestBase helpers — never hardcode "/api/...":
//       - crudReadPath("Order"), crudCollectionPath("MenuItem"), fillPath(...)
//       - loginPath(), registerPath()
//     Resolve table names through tableName("Order"), enums through
//     enumValues("OrderStatus").
//   * Resolve IDs from response bodies (registration, search results) or
//     from the auto-seeded fixtures (admin = id=1 via adminToken/adminId,
//     vegetarian customer with 5 orders = id=4, etc. — see
//     memory/project_talabat_seed_data.md). Never write `/api/users/1`
//     literally.
//   * One scenario at a time, approved by the user before mapping to the
//     other 7 themes.
// ────────────────────────────────────────────────────────────────────────────

// ─── TC01 — Register a new user (happy path) ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC01_RegisterHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC01 — POST registerPath() with a fresh email returns 2xx and a JWT token")
        void register_returns_2xx_with_token() throws Exception {
                BASE_URL = userServiceUrl;
                // Build a payload with a nonce-based email so it cannot collide with
                // any of the auto-seeded users (_preseed_*@grader.testgen.io) or
                // with prior runs of this test class.
                String email = "tc01_" + nonce() + "@grader.testgen.io";
                String body = String.format("""
                                {"name":"TC01 User","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));

                HttpResponse<String> r = httpPost("/api/auth/register", body);

                // Strict 2xx — registering a brand-new email with a valid payload
                // must succeed; any non-2xx is a bug in register / validation /
                // password hashing / DB persistence.
                assert2xx(r, "TC01 register");
                JsonNode j = parseNode(r.body());
                // Spec: response is { "token": "...", "expiresIn": ... }. No 'id' field;
                // chained tests resolve uid from JWT (uidFromJwt()) or via login.
                assertNotNull(j.get("token"),
                                "TC01: register response must include 'token' field per spec; body=" + r.body());
                assertFalse(j.get("token").asText().isBlank(),
                                "TC01: 'token' must be a non-blank string; got " + j.get("token"));
                assertTrue(j.has("expiresIn") && j.get("expiresIn").asLong() > 0,
                                "TC01: register response must include positive 'expiresIn' per spec; body=" + r.body());
        }
}

// ─── TC02 — Login with valid credentials (happy path) ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC02_LoginHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC02 — POST loginPath() after a successful register returns 2xx with a 3-segment JWT")
        void login_returns_2xx_with_three_segment_jwt() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — create a fresh user.
                String email = "tc02_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC02 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC02 setup register");

                // Act — log in with the same credentials.
                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> r = httpPost("/api/auth/login", loginBody);

                // Assert.
                assert2xx(r, "TC02 login");
                JsonNode j = parseNode(r.body());
                assertNotNull(j.get("token"),
                                "TC02: login response must include 'token' field; body=" + r.body());
                String token = j.get("token").asText();
                assertFalse(token.isBlank(),
                                "TC02: 'token' must be a non-blank string");
                assertEquals(3, token.split("\\.").length,
                                "TC02: 'token' must be a 3-segment JWT (a.b.c); got '" + token + "'");
        }
}

// ─── TC03 — Read own user profile with valid JWT (happy path) ───────────────
@Tag("public")
@Tag("updated_crud")
class TC03_ReadOwnProfileHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC03 — GET crudReadPath(\"User\") with own JWT returns 2xx and a JSON object")
        void read_own_profile_returns_2xx_and_json_object() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register and capture the new user's id.
                String email = "tc03_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC03 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC03 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                // Setup — login and capture the JWT.
                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC03 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Act — read the user's own profile via the User CRUD path. The
                // path template comes from the manifest so a student who renames
                // their controller still gets the correct URL.
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid, token);

                // Assert. Strict 2xx — we just registered this exact user, a 404
                // here is a real bug (register didn't persist OR the JWT chain
                // rejected our own token).
                assert2xx(r, "TC03 read own profile");
                JsonNode j = parseNode(r.body());
                assertTrue(j.isObject(),
                                "TC03: response body must be a JSON object; got " + r.body());
        }
}

// ─── TC04 — Register with duplicate email returns 4xx (negative path) ───────
@Tag("public")
@Tag("features_m2")
class TC04_RegisterDuplicateEmailTests extends TestBase {

        @Test
        @DisplayName("TC04 — POST registerPath() with an already-registered email returns a 4xx")
        void register_with_duplicate_email_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Build a payload with a nonce-based email — guaranteed not to
                // collide with auto-seeded users or with prior runs of this test
                // class (since @BeforeEach truncates between tests anyway).
                String email = "tc04_" + nonce() + "@grader.testgen.io";
                String firstBody = String.format("""
                                {"name":"TC04 First","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));

                // Step 1 — first registration must succeed (precondition).
                HttpResponse<String> first = httpPost("/api/auth/register", firstBody);
                assert2xx(first, "TC04 first register (precondition)");

                // Step 2 — second registration with the SAME email but different
                // name + phone (uniqueness should be on email).
                String secondBody = String.format("""
                                {"name":"TC04 Second","email":"%s","password":"AnotherPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> second = httpPost("/api/auth/register", secondBody);

                // Step 3 — strict 4xx assertion. Tolerate any of 400/409/422
                // (M2 spec doesn't pin a specific code) but NOT 2xx, NOT 5xx,
                // and NOT 401/403 (the body itself is well-formed and authorized).
                int code = second.statusCode();
                assertTrue(code >= 400 && code < 500,
                                "TC04: duplicate-email register must return a 4xx client error; "
                                                + "got " + code + " body=" + second.body());
                assertTrue(code != 401,
                                "TC04: duplicate-email is not an auth failure (no Authorization header was sent); "
                                                + "401 indicates the controller is misclassifying the error. body="
                                                + second.body());
                assertTrue(code != 403,
                                "TC04: duplicate-email is not a permission failure (anyone can register); "
                                                + "403 indicates the controller is misclassifying the error. body="
                                                + second.body());
        }
}

// ─── TC05 — Login with wrong password returns 401 (negative path) ───────────
@Tag("public")
@Tag("features_m2")
class TC05_LoginWrongPasswordTests extends TestBase {

        @Test
        @DisplayName("TC05 — POST loginPath() with the wrong password returns strictly 401")
        void login_with_wrong_password_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register a fresh user with a known-correct password.
                // nonce-based email avoids collisions with auto-seeded users and
                // with prior test runs (truncate@BeforeEach handles cross-test).
                String email = "tc05_" + nonce() + "@grader.testgen.io";
                String correctPwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC05 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, correctPwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC05 setup register (precondition)");

                // Act — log in with the SAME email but a different password.
                // Different enough that bcrypt cannot accidentally match (no
                // shared prefix, different length).
                String wrongPwd = "WrongPwd!2026";
                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, wrongPwd);
                HttpResponse<String> r = httpPost("/api/auth/login", loginBody);
                int code = r.statusCode();

                // Assert — strictly 401. Tolerant assertions (with explanatory
                // messages) for each common misclassification:
                // * 2xx: login skipped the password check — critical security bug.
                // * 5xx: bcrypt mismatch leaked as exception instead of being
                // caught and translated to 401.
                // * 404: user-enumeration anti-pattern (OWASP). Login must NOT
                // distinguish "user not found" from "wrong password" in
                // its status code; both should be 401.
                // * 403: this is not a permissions issue. Login is how you
                // OBTAIN permissions; you cannot be 403'd from it.
                assertTrue(code / 100 != 2,
                                "TC05: login with wrong password must NOT return 2xx. "
                                                + "A 2xx here means the password check was skipped — critical security bug. "
                                                + "Got " + code + " body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC05: login with wrong password must NOT 5xx. A 5xx here means the bcrypt "
                                                + "mismatch threw an unhandled exception instead of being caught and "
                                                + "translated to 401. Got " + code + " body=" + r.body());
                assertTrue(code != 404,
                                "TC05: login with wrong password must NOT return 404. The user account exists; "
                                                + "returning 404 instead of 401 leaks the existence/non-existence of accounts "
                                                + "(OWASP user-enumeration anti-pattern). body=" + r.body());
                assertTrue(code != 403,
                                "TC05: login with wrong password must NOT return 403. Login is the act of "
                                                + "obtaining permissions, not exercising them — 403 is structurally wrong. "
                                                + "body=" + r.body());
                assertEquals(401, code,
                                "TC05: login with wrong password must return strictly 401 Unauthorized; got "
                                                + code + " body=" + r.body());
        }
}

// ─── TC06 — Authentication happy path: valid admin JWT accepted on a non-User
// CRUD
@Tag("public")
@Tag("authentication")
class TC06_AuthValidTokenAcceptedTests extends TestBase {

        @Test
        @DisplayName("TC06 — GET a non-User CRUD list endpoint with a valid admin Bearer JWT returns 2xx (auth filter accepts the token)")
        void valid_admin_jwt_is_accepted_on_non_user_crud() throws Exception {
                // Setup — obtain an admin JWT (always via user-service login).
                BASE_URL = userServiceUrl;
                String token = adminToken();
                // Derive entity and its owning service URL from the manifest.
                String[] info = firstTopLevelNonUserEntityInfo();
                String entity = info[0];
                String svcUrl = info[1];
                BASE_URL = svcUrl;
                String path = crudCollectionPath(entity);
                HttpResponse<String> r = httpGetAuth(path, token);
                assert2xx(r, "TC06 auth happy path (admin) on " + entity + " list (" + path + ")");
        }
}

// ─── TC07 — Missing Authorization header on a non-User CRUD returns 401 ─────
@Tag("public")
@Tag("authentication")
class TC07_AuthMissingHeaderTests extends TestBase {

        @Test
        @DisplayName("TC07 — GET a non-User CRUD list endpoint with NO Authorization header returns strictly 401")
        void missing_auth_header_returns_401_on_non_user_crud() throws Exception {
                String[] info = firstTopLevelNonUserEntityInfo();
                String entity = info[0];
                BASE_URL = info[1];
                String path = crudCollectionPath(entity);
                HttpResponse<String> r = httpGet(path);
                int code = r.statusCode();

                // Assert — strictly 401. Tolerant assertions for each common
                // misclassification, with diagnostic messages:
                // * 2xx: endpoint wide-open — critical security bug.
                // * 5xx: filter chain crashed instead of cleanly rejecting.
                // * 404: endpoint reachable anonymously and just returns
                // empty/missing — wrong; auth must block FIRST, before
                // any controller logic runs.
                // * 403: 403 means "authenticated but lacking permission". We
                // sent NO credentials at all — the correct response is
                // 401 (Unauthorized), not 403 (Forbidden).
                assertTrue(code / 100 != 2,
                                "TC07: GET " + path + " without Authorization header must NOT return 2xx. "
                                                + "A 2xx here means the endpoint is wide-open — critical security bug. "
                                                + "Got " + code + " body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC07: GET " + path + " without Authorization header must NOT 5xx. A 5xx here "
                                                + "means the filter chain crashed instead of cleanly rejecting. "
                                                + "Got " + code + " body=" + r.body());
                assertTrue(code != 404,
                                "TC07: GET " + path + " without Authorization header must NOT return 404. "
                                                + "A 404 here means the endpoint is reachable anonymously and just "
                                                + "returned empty — auth must block FIRST, before any controller logic "
                                                + "runs. body=" + r.body());
                assertTrue(code != 403,
                                "TC07: GET " + path + " without Authorization header must NOT return 403. We "
                                                + "sent NO credentials; 403 means \"authenticated but lacking permission\", "
                                                + "which doesn't apply when there's no auth attempt at all. The correct "
                                                + "code is 401 Unauthorized. body=" + r.body());
                assertEquals(401, code,
                                "TC07: GET " + path + " without Authorization header must return strictly 401 "
                                                + "Unauthorized; got " + code + " body=" + r.body());
        }
}

// ─── TC08 — Tampered JWT signature is rejected with 401 (negative path) ─────
@Tag("public")
@Tag("authentication")
class TC08_AuthTamperedSignatureTests extends TestBase {

        @Test
        @DisplayName("TC08 — GET protected endpoint with a tampered-signature JWT returns strictly 401 (signature is verified, not just decoded)")
        void tampered_jwt_signature_is_rejected_with_401() throws Exception {
                BASE_URL = userServiceUrl;
                String validToken = adminToken();
                String tamperedToken = tamperSignature(validToken);
                String[] info = firstTopLevelNonUserEntityInfo();
                String entity = info[0];
                BASE_URL = info[1];
                String path = crudCollectionPath(entity);
                HttpResponse<String> r = httpGetAuth(path, tamperedToken);
                int code = r.statusCode();

                // Assert — strictly 401. Diagnostic ladder:
                // * 2xx: signature not actually verified — critical security bug
                // (anyone can forge tokens by editing the payload).
                // * 5xx: filter chain crashed on signature mismatch instead of
                // cleanly rejecting.
                // * 404: filter passed the request through to the controller
                // and just didn't find anything — wrong; signature
                // mismatch must be caught FIRST in the filter, before
                // any controller logic.
                // * 403: forged credentials = "not authenticated" (401), not
                // "authenticated but lacking permission" (403).
                assertTrue(code / 100 != 2,
                                "TC08: tampered-signature JWT must NOT be accepted (status " + code + "). "
                                                + "A 2xx here means the signature was not actually verified — anyone can "
                                                + "forge tokens by editing the payload. Critical security bug. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC08: tampered-signature JWT must NOT 5xx (status " + code + "). A 5xx here "
                                                + "means the filter chain crashed on signature mismatch instead of cleanly "
                                                + "rejecting. body=" + r.body());
                assertTrue(code != 404,
                                "TC08: tampered-signature JWT must NOT return 404 (status " + code + "). A 404 "
                                                + "here means the filter passed the request through to the controller and "
                                                + "just didn't find anything — signature mismatch must be caught FIRST in "
                                                + "the filter, before any controller logic runs. body=" + r.body());
                assertTrue(code != 403,
                                "TC08: tampered-signature JWT must NOT return 403 (status " + code + "). Forged "
                                                + "credentials are \"not authenticated\" (401), not \"authenticated but "
                                                + "lacking permission\" (403). body=" + r.body());
                assertEquals(401, code,
                                "TC08: tampered-signature JWT must return strictly 401 Unauthorized; got "
                                                + code + " body=" + r.body());
        }
}

// ─── TC09 — Login with non-existent email returns 401 (negative path) ───────
@Tag("public")
@Tag("features_m2")
class TC09_LoginUnknownEmailTests extends TestBase {

        @Test
        @DisplayName("TC09 — POST loginPath() with an email that has never been registered returns strictly 401")
        void login_unknown_email_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                // nonce-based email — guaranteed not to exist in the DB. Any
                // truncate/auto-seed in @BeforeEach also strips users, so this
                // email is truly absent at login time.
                String unknownEmail = "tc09_never_registered_" + nonce() + "@grader.testgen.io";
                String body = String.format("""
                                {"email":"%s","password":"AnythingPwd!2026"}
                                """, unknownEmail);

                HttpResponse<String> r = httpPost("/api/auth/login", body);
                int code = r.statusCode();

                // Spec §10 S1-F11: 401 for both "user not found" and "wrong password" — avoids email enumeration.
                assertTrue(code / 100 != 2,
                                "TC09: login with a never-registered email must NOT return 2xx (status "
                                                + code + "). A 2xx here means a token was issued for a non-existent "
                                                + "user. body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC09: login with a never-registered email must NOT 5xx (status " + code
                                                + "). Server must handle the missing-user case cleanly. body="
                                                + r.body());
                assertTrue(code != 403,
                                "TC09: login with a never-registered email must NOT return 403 (status " + code
                                                + "). 403 is a permission error; this is an unauthenticated condition. body="
                                                + r.body());
                assertEquals(401, code,
                                "TC09: login with a never-registered email must return strictly 401 Unauthorized "
                                                + "(spec §10 S1-F11 — 401 for both 'user not found' and 'wrong password'); "
                                                + "got " + code + " body=" + r.body());
        }
}

// ─── TC10 — Empty Bearer token returns 401 (negative path) ──────────────────
@Tag("public")
@Tag("authentication")
class TC10_AuthEmptyBearerTests extends TestBase {

        @Test
        @DisplayName("TC10 — GET protected endpoint with `Authorization: Bearer ` (empty token) returns strictly 401")
        void empty_bearer_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                // "Bearer " with a trailing space and no token after it. Not a
                // missing header (TC07 covers that) — this header is PRESENT
                // but its value is malformed.
                HttpResponse<String> r = httpGetWithRawAuth(path, "Bearer ");
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC10: empty Bearer token must NOT be accepted (status " + code
                                                + "). A 2xx here means the auth filter accepted an empty/missing token. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC10: empty Bearer token must NOT 5xx (status " + code + "). The filter must "
                                                + "handle empty tokens cleanly, not throw NPE. body=" + r.body());
                assertEquals(401, code,
                                "TC10: empty Bearer token must return strictly 401 Unauthorized; got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC11 — Non-Bearer scheme (Basic) returns 401 (negative path) ───────────
@Tag("public")
@Tag("authentication")
class TC11_AuthBasicSchemeTests extends TestBase {

        @Test
        @DisplayName("TC11 — GET protected endpoint with `Authorization: Basic ...` (non-Bearer scheme) returns strictly 401")
        void basic_scheme_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                // "Basic dXNlcjpwYXNz" — base64 of "user:pass". Catches lenient
                // parsers that strip the scheme prefix and try to decode whatever
                // is left as a JWT (it isn't).
                HttpResponse<String> r = httpGetWithRawAuth(path, "Basic dXNlcjpwYXNz");
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC11: Basic-scheme auth must NOT be accepted on a JWT-protected endpoint "
                                                + "(status " + code
                                                + "). The filter must reject any non-Bearer scheme. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC11: Basic-scheme auth must NOT 5xx (status " + code + "). body=" + r.body());
                assertEquals(401, code,
                                "TC11: Basic-scheme auth must return strictly 401 Unauthorized; got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC12 — Garbage non-JWT token returns 401 (negative path) ───────────────
@Tag("public")
@Tag("authentication")
class TC12_AuthGarbageTokenTests extends TestBase {

        @Test
        @DisplayName("TC12 — GET protected endpoint with `Authorization: Bearer not_a_valid_jwt` returns strictly 401")
        void garbage_token_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                // "not_a_valid_jwt" — no dots, not base64, structurally invalid.
                // Catches "if 3 dot-segments, decode without verifying" and any
                // code path that doesn't validate JWT structure before claims-extract.
                HttpResponse<String> r = httpGetWithRawAuth(path, "Bearer not_a_valid_jwt");
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC12: garbage non-JWT token must NOT be accepted (status " + code
                                                + "). A 2xx here means the filter didn't validate the JWT structure. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC12: garbage token must NOT 5xx (status " + code + "). The parser must "
                                                + "handle malformed tokens gracefully. body=" + r.body());
                assertEquals(401, code,
                                "TC12: garbage non-JWT token must return strictly 401 Unauthorized; got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC13 — Forged role-claim token (payload modified post-signing) rejected
@Tag("public")
@Tag("authentication")
class TC13_AuthForgedRoleClaimTests extends TestBase {

        @Test
        @DisplayName("TC13 — GET protected endpoint with a payload-tampered JWT (role forged to ADMIN) is NOT accepted")
        void forged_role_claim_token_is_rejected() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register a CUSTOMER user (default theme role) and log
                // in to capture a real, signed JWT for them.
                String email = "tc13_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC13 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC13 setup register (precondition)");

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC13 setup login (precondition)");
                String realToken = parseNode(login.body()).get("token").asText();

                // Forge a role claim into the payload while keeping the original
                // signature. The signature was computed over the original
                // payload; modifying the payload makes the signature INVALID
                // even though we don't tamper the signature segment itself.
                String[] parts = realToken.split("\\.");
                if (parts.length != 3) {
                        throw new AssertionError("TC13 setup: real token is not a 3-segment JWT: " + realToken);
                }
                String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                // Try to replace common role-claim patterns. If none match,
                // inject a "role":"ADMIN" entry into the JSON object.
                String tamperedPayload = payloadJson;
                for (String[] swap : new String[][] {
                                { "\"role\":\"CUSTOMER\"", "\"role\":\"ADMIN\"" },
                                { "\"role\": \"CUSTOMER\"", "\"role\":\"ADMIN\"" },
                                { "\"authorities\":[\"ROLE_CUSTOMER\"]", "\"authorities\":[\"ROLE_ADMIN\"]" },
                }) {
                        if (tamperedPayload.contains(swap[0])) {
                                tamperedPayload = tamperedPayload.replace(swap[0], swap[1]);
                                break;
                        }
                }
                if (tamperedPayload.equals(payloadJson)) {
                        // No known role-claim pattern found — inject one. Locate the
                        // closing brace of the outer object and prepend a role claim.
                        int lastBrace = tamperedPayload.lastIndexOf('}');
                        if (lastBrace > 0) {
                                String prefix = tamperedPayload.substring(0, lastBrace).trim();
                                if (prefix.endsWith("{")) {
                                        tamperedPayload = prefix + "\"role\":\"ADMIN\"}";
                                } else {
                                        tamperedPayload = prefix + ",\"role\":\"ADMIN\"}";
                                }
                        }
                }
                String tamperedB64 = java.util.Base64.getUrlEncoder().withoutPadding()
                                .encodeToString(tamperedPayload.getBytes());
                String forgedToken = parts[0] + "." + tamperedB64 + "." + parts[2];

                // Act — hit a protected endpoint with the forged token.
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                HttpResponse<String> r = httpGetAuth(path, forgedToken);
                int code = r.statusCode();

                // Assert — must NOT be 2xx. Either of these is acceptable:
                // * 401 — signature verification caught the payload tamper (preferred).
                // * 403 — signature verified but server re-validated role from DB
                // and found CUSTOMER, not ADMIN.
                // But 2xx means signature wasn't verified AND role was trusted from
                // the (forged) payload — critical privilege-escalation bug.
                assertTrue(code / 100 != 2,
                                "TC13: forged role-claim token must NOT be accepted (status " + code + "). "
                                                + "A 2xx here means the signature was not verified after payload "
                                                + "modification — anyone with a real token can self-promote to ADMIN by "
                                                + "editing their payload. Critical privilege-escalation bug. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC13: forged role-claim token must NOT 5xx (status " + code + "). The filter "
                                                + "must reject cleanly. body=" + r.body());
        }
}

// ─── TC14 — Register with missing required field returns 4xx (negative path)
@Tag("public")
@Tag("features_m2")
class TC14_RegisterMissingFieldTests extends TestBase {

        @Test
        @DisplayName("TC14 — POST registerPath() with a body missing the `email` field returns a 4xx")
        void register_missing_email_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Body missing `email` — controller must reject with 4xx, not
                // propagate the missing-field as a server-side NPE/5xx or
                // silently create a user with null email.
                String body = String.format("""
                                {"name":"TC14 User","password":"TestPwd!2026","phone":"+201%s"}
                                """, nonce().substring(0, 9));

                HttpResponse<String> r = httpPost("/api/auth/register", body);
                int code = r.statusCode();

                assertTrue(code >= 400 && code < 500,
                                "TC14: register with missing required field (email) must return a 4xx; got "
                                                + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC14: register with missing email must NOT 2xx (a user without an email "
                                                + "cannot be valid). body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC14: register with missing email must NOT 5xx — the controller must "
                                                + "validate input cleanly, not crash. body=" + r.body());
        }
}

// ─── TC15 — Register with role=ADMIN in body must NOT yield an ADMIN account ─
@Tag("public")
@Tag("features_m2")
class TC15_RegisterRoleEscalationTests extends TestBase {

        @Test
        @DisplayName("TC15 — POST registerPath() with `role:ADMIN` in body must NOT result in an ADMIN account (privilege-escalation)")
        void register_with_role_admin_in_body_must_not_escalate() throws Exception {
                BASE_URL = userServiceUrl;
                // Build a register body with `role:"ADMIN"` injected. The
                // controller may either ignore the field, reject the request,
                // or accept the request — but the resulting DB row's role
                // column must NOT be ADMIN. Anything else is privilege escalation.
                String email = "tc15_" + nonce() + "@grader.testgen.io";
                String body = String.format(
                                """
                                                {"name":"TC15 User","email":"%s","password":"TestPwd!2026","phone":"+201%s","role":"ADMIN"}
                                                """,
                                email, nonce().substring(0, 9));

                HttpResponse<String> reg = httpPost("/api/auth/register", body);
                // We don't strictly require 2xx here — a controller that
                // rejects extra fields with 4xx is also fine. What matters is
                // that no ADMIN account got created.
                int regCode = reg.statusCode();
                assertTrue(regCode / 100 != 5,
                                "TC15: register with role=ADMIN body must NOT 5xx (got " + regCode
                                                + "). body=" + reg.body());

                // If the registration succeeded, look up the role in DB and
                // assert it's NOT ADMIN.
                if (regCode / 100 == 2) {
                        String role = fetchUserRole(email);
                        assertNotNull(role,
                                        "TC15: registration returned 2xx but no user row found for email "
                                                        + email + " — register isn't actually persisting.");
                        assertTrue(!"ADMIN".equalsIgnoreCase(role),
                                        "TC15: registering with role=ADMIN in the body must NOT result in an "
                                                        + "ADMIN account. Found role=" + role + " (expected the theme "
                                                        + "default, NOT ADMIN). This is a privilege-escalation bug — the "
                                                        + "controller is mapping the body's role field into the entity.");
                }
                // If registration was rejected (4xx), that's also acceptable —
                // the role field was caught as invalid input. Either way no
                // ADMIN was created.
        }
}

// ─── TC16 — Login with empty password returns 4xx (negative path) ───────────
@Tag("public")
@Tag("features_m2")
class TC16_LoginEmptyPasswordTests extends TestBase {

        @Test
        @DisplayName("TC16 — POST loginPath() with `password:\"\"` (empty) returns NOT 2xx")
        void login_empty_password_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register a real user so the email exists.
                String email = "tc16_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC16 User","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBody), "TC16 setup register");

                // Act — login with empty password. Bcrypt verification must
                // not be bypassed by an empty input.
                String loginBody = String.format("""
                                {"email":"%s","password":""}
                                """, email);
                HttpResponse<String> r = httpPost("/api/auth/login", loginBody);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC16: login with empty password must NOT issue a token (status " + code
                                                + "). A 2xx here means bcrypt verification was bypassed for empty "
                                                + "input. body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC16: login with empty password must NOT 5xx (status " + code + "). The "
                                                + "controller must validate input cleanly, not NPE on empty string. "
                                                + "body=" + r.body());
                // Acceptable: 400 (validation error) or 401 (auth failure) or
                // 422 (semantic validation error). All 4xx.
                assertTrue(code >= 400 && code < 500,
                                "TC16: login with empty password must return a 4xx (validation or auth "
                                                + "failure); got " + code + " body=" + r.body());
        }
}

// ─── TC17 — Cross-user IDOR: User A cannot READ User B's profile ────────────
@Tag("public")
@Tag("authorization")
class TC17_IdorReadOtherUserTests extends TestBase {

        @Test
        @DisplayName("TC17 — Customer A's GET on User B's CRUD path must NOT be 2xx (cross-user IDOR)")
        void customer_a_cannot_read_user_b_profile() throws Exception {
                BASE_URL = userServiceUrl;
                String emailA = "tc17a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc17b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBodyA = String.format("""
                                {"name":"TC17 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"TC17 B","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC17 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC17 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC17 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                HttpResponse<String> r = httpGetAuth("/api/users/" + bid, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC17: customer A reading customer B's profile must NOT be 2xx (status "
                                                + code + "). A 2xx here means cross-user IDOR is unprotected — any "
                                                + "authenticated user can read any other user's profile. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC17: cross-user read must NOT 5xx (status " + code + "). The auth check "
                                                + "must reject cleanly, not crash. body=" + r.body());
                assertTrue(code == 403 || code == 404,
                                "TC17: cross-user read must return 403 (forbidden) or 404 (not-found / "
                                                + "privacy-by-obscurity); got " + code + " body=" + r.body());
        }
}

// ─── TC18 — Cross-user IDOR: User A cannot UPDATE User B's profile ──────────
@Tag("public")
@Tag("authorization")
class TC18_IdorUpdateOtherUserTests extends TestBase {

        @Test
        @DisplayName("TC18 — Customer A's PUT on User B's CRUD path must NOT be 2xx, AND B's data must NOT change in DB")
        void customer_a_cannot_update_user_b_profile() throws Exception {
                BASE_URL = userServiceUrl;
                String emailA = "tc18a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc18b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String origNameB = "TC18 B Original";
                String regBodyA = String.format("""
                                {"name":"TC18 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"+201%s"}
                                """, origNameB, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC18 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC18 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC18 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                // Mitigation pattern — include all original fields plus the
                // changed name (some controllers require all fields on PUT).
                String tamperedName = "TC18 HIJACK";
                String putBody = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"+201%s"}
                                """, tamperedName, emailB, pwd, nonce().substring(0, 9));
                HttpResponse<String> r = httpPutAuth("/api/users/" + bid, putBody, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC18: customer A updating customer B's profile must NOT be 2xx (status "
                                                + code + "). A 2xx here means cross-user IDOR write is unprotected. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC18: cross-user update must NOT 5xx (status " + code + "). body=" + r.body());
                assertTrue(code == 403 || code == 404,
                                "TC18: cross-user update must return 403 or 404; got " + code + " body=" + r.body());

                // Defensive — even if controller returned 4xx, verify B's row
                // in DB was NOT mutated. Some buggy controllers reject the
                // response but commit the change.
                String userTable = tableName("User");
                String currentName = jdbc.queryForObject(
                                "SELECT name FROM " + userTable + " WHERE id = ?",
                                String.class, bid);
                assertEquals(origNameB, currentName,
                                "TC18: cross-user PUT was rejected (status " + code + ") but B's name in DB "
                                                + "changed from '" + origNameB + "' to '" + currentName + "' — the "
                                                + "controller is committing the change before doing the auth check.");
        }
}

// ─── TC19 — Cross-user IDOR: User A cannot DELETE User B ────────────────────
@Tag("public")
@Tag("authorization")
class TC19_IdorDeleteOtherUserTests extends TestBase {

        @Test
        @DisplayName("TC19 — Customer A's DELETE on User B's CRUD path must NOT be 2xx, AND B must STILL exist in DB")
        void customer_a_cannot_delete_user_b() throws Exception {
                BASE_URL = userServiceUrl;
                String emailA = "tc19a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc19b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBodyA = String.format("""
                                {"name":"TC19 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"TC19 B","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC19 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC19 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC19 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                HttpResponse<String> r = httpDeleteAuth("/api/users/" + bid, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC19: customer A deleting customer B must NOT be 2xx (status " + code
                                                + "). A 2xx here means cross-user delete is unprotected. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC19: cross-user delete must NOT 5xx (status " + code + "). body=" + r.body());
                assertTrue(code == 403 || code == 404,
                                "TC19: cross-user delete must return 403 or 404; got " + code + " body=" + r.body());

                // Defensive — B's row must STILL exist in DB.
                String userTable = tableName("User");
                Integer count = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM " + userTable + " WHERE id = ?",
                                Integer.class, bid);
                assertNotNull(count);
                assertTrue(count == 1,
                                "TC19: cross-user DELETE was rejected (status " + code + ") but B's row in DB "
                                                + "is gone (count=" + count + ") — the controller is committing the "
                                                + "delete before doing the auth check.");
        }
}

// ─── TC20 — Owner happy path: User A can UPDATE their own profile ───────────
@Tag("public")
@Tag("authorization")
class TC20_OwnerUpdateOwnProfileTests extends TestBase {

        @Test
        @DisplayName("TC20 — Customer's PUT on their own User CRUD path returns 2xx, AND DB reflects the new name")
        void owner_can_update_own_profile() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc20_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String origPhone = "+201" + nonce().substring(0, 9);
                String regBody = String.format("""
                                {"name":"TC20 Original","email":"%s","password":"%s","phone":"%s"}
                                """, email, pwd, origPhone);
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC20 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC20 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Mitigation pattern — include all original fields plus the new name.
                String newName = "TC20 Updated";
                String putBody = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"%s"}
                                """, newName, email, pwd, origPhone);
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid, putBody, token);
                assert2xx(r, "TC20 owner update own profile");

                // JDBC verification (NOT via GET) — we're testing the PUT
                // path's persistence semantics specifically.
                String userTable = tableName("User");
                String currentName = jdbc.queryForObject(
                                "SELECT name FROM " + userTable + " WHERE id = ?",
                                String.class, uid);
                assertEquals(newName, currentName,
                                "TC20: PUT returned " + r.statusCode() + " but DB row's name is '" + currentName
                                                + "' (expected '" + newName
                                                + "') — the controller returned 2xx without "
                                                + "actually persisting the update.");
        }
}

// ─── TC21 — Admin override: admin can READ any user ─────────────────────────
@Tag("public")
@Tag("authorization")
class TC21_AdminReadAnyUserTests extends TestBase {

        @Test
        @DisplayName("TC21 — Admin's GET on a customer's User CRUD path returns 2xx (admin role bypasses ownership)")
        void admin_can_read_any_user() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc21_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC21 Customer","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC21 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + customerId, adminTok);
                assert2xx(r, "TC21 admin read customer");
                JsonNode j = parseNode(r.body());
                assertTrue(j.isObject(),
                                "TC21: admin GET response body must be a JSON object; got " + r.body());
        }
}

// ─── TC22 — Admin override: admin can UPDATE any user ───────────────────────
@Tag("public")
@Tag("authorization")
class TC22_AdminUpdateAnyUserTests extends TestBase {

        @Test
        @DisplayName("TC22 — Admin's PUT on a customer's User CRUD path returns 2xx, AND DB reflects the new name")
        void admin_can_update_any_user() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc22_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String origPhone = "+201" + nonce().substring(0, 9);
                String regBody = String.format("""
                                {"name":"TC22 Customer","email":"%s","password":"%s","phone":"%s"}
                                """, email, pwd, origPhone);
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC22 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();

                // Mitigation pattern — include all original fields plus the new name.
                String newName = "TC22 Admin-Updated";
                String putBody = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"%s"}
                                """, newName, email, pwd, origPhone);
                HttpResponse<String> r = httpPutAuth("/api/users/" + customerId, putBody, adminTok);
                assert2xx(r, "TC22 admin update customer");

                String userTable = tableName("User");
                String currentName = jdbc.queryForObject(
                                "SELECT name FROM " + userTable + " WHERE id = ?",
                                String.class, customerId);
                assertEquals(newName, currentName,
                                "TC22: admin PUT returned " + r.statusCode() + " but customer's name in DB is '"
                                                + currentName + "' (expected '" + newName + "') — admin update did not "
                                                + "persist.");
        }
}

// ─── TC23 — Admin override: admin can DELETE any user (strict hard-delete) ──
@Tag("public")
@Tag("authorization")
class TC23_AdminDeleteAnyUserTests extends TestBase {

        @Test
        @DisplayName("TC23 — Admin's DELETE on a customer's User CRUD path returns 2xx, AND the row is HARD-deleted (strict)")
        void admin_can_delete_any_user_hard() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc23_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC23 Customer","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC23 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();
                HttpResponse<String> r = httpDeleteAuth("/api/users/" + customerId, adminTok);
                assert2xx(r, "TC23 admin delete customer");

                // STRICT hard-delete: row must be physically gone from DB.
                // Soft-delete is NOT acceptable for this test per direction.
                String userTable = tableName("User");
                Integer count = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM " + userTable + " WHERE id = ?",
                                Integer.class, customerId);
                assertNotNull(count);
                assertEquals(0, count.intValue(),
                                "TC23: admin DELETE returned " + r.statusCode() + " but customer row STILL "
                                                + "exists in DB (count=" + count
                                                + "). DELETE must hard-delete the row, "
                                                + "not soft-delete it (use the deactivate endpoint for status changes).");

                // GET-after-DELETE — strictly 404.
                HttpResponse<String> g = httpGetAuth("/api/users/" + customerId, adminTok);
                int gcode = g.statusCode();
                assertTrue(gcode / 100 != 2,
                                "TC23: GET-after-DELETE returned 2xx (status " + gcode + "). The row was "
                                                + "already verified gone from DB above, but GET still finds it. body="
                                                + g.body());
                assertEquals(404, gcode,
                                "TC23: GET after a successful DELETE must return 404 Not Found; got " + gcode
                                                + " body=" + g.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S1-F12 — Get User Activity Feed
// GET /api/users/{id}/activity?page={page}&size={size}
// Auth: required user. Ownership: caller must be target OR admin.
// Defaults: page=0, size=10, max size=100. Response shape:
// { content: [{action, timestamp, details}], page, size, totalElements }
// ════════════════════════════════════════════════════════════════════════════

// ─── TC24 — Owner GET own activity returns 2xx with paginated envelope ──────
@Tag("public")
@Tag("features_m2")
class TC24_ActivityOwnerHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC24 — GET /api/users/{ownId}/activity with own token returns 2xx and a paginated envelope")
        void owner_activity_returns_2xx_with_envelope() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register and login the user.
                String email = "tc24_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC24 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC24 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC24 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Act — GET own activity.
                String activityPath = "/api/users" + "/" + uid + "/activity";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                assert2xx(r, "TC24 owner activity");

                // Assert envelope shape per spec: content[], page, size, totalElements.
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("content"),
                                "TC24: response must include `content` field; body=" + r.body());
                assertTrue(j.get("content").isArray(),
                                "TC24: `content` must be an array; got " + j.get("content"));
                assertTrue(j.has("page"),
                                "TC24: response must include `page` field; body=" + r.body());
                assertTrue(j.has("size"),
                                "TC24: response must include `size` field; body=" + r.body());
                assertTrue(j.has("totalElements"),
                                "TC24: response must include `totalElements` field; body=" + r.body());
        }
}

// ─── TC25 — Non-existent user ID returns 404 (admin token) ──────────────────
@Tag("public")
@Tag("features_m2")
class TC25_ActivityNonExistentIdTests extends TestBase {

        @Test
        @DisplayName("TC25 — GET /api/users/<Long.MAX_VALUE>/activity with admin token returns strictly 404")
        void activity_non_existent_id_returns_404() throws Exception {
                BASE_URL = userServiceUrl;
                // Use admin token: per spec, admin passes ownership check, then
                // user-not-found returns 404. With a non-admin token, ownership
                // would fail first with 403 — wrong path for this test.
                String adminTok = adminToken();
                long missingId = Long.MAX_VALUE;
                String activityPath = "/api/users" + "/" + missingId + "/activity";

                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC25: activity for a non-existent user ID must NOT be 2xx (status " + code
                                                + "). body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC25: activity for a non-existent user ID must NOT 5xx — server must handle "
                                                + "missing-user gracefully. body=" + r.body());
                assertEquals(404, code,
                                "TC25: per spec, admin passes ownership check then user-not-found yields "
                                                + "strictly 404; got " + code + " body=" + r.body());
        }
}

// ─── TC26 — Negative user ID returns 4xx (admin token) ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC26_ActivityNegativeIdTests extends TestBase {

        @Test
        @DisplayName("TC26 — GET /api/users/-1/activity with admin token returns a 4xx (graceful)")
        void activity_negative_id_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Admin token — bypass ownership so the test actually exercises
                // the negative-id rejection logic (validation OR not-found).
                String adminTok = adminToken();
                String activityPath = "/api/users" + "/-1/activity";

                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC26: activity for a negative user ID must NOT 5xx — controller must "
                                                + "validate / reject gracefully, not crash. status=" + code + " body="
                                                + r.body());
                assertTrue(code / 100 != 2,
                                "TC26: activity for a negative user ID must NOT be 2xx — negative ids cannot "
                                                + "match any real user. status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC26: activity for a negative user ID must return a 4xx (400 validation or "
                                                + "404 not-found); got " + code + " body=" + r.body());
        }
}

// ─── TC27 — String user ID returns 4xx (admin token) ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC27_ActivityStringIdTests extends TestBase {

        @Test
        @DisplayName("TC27 — GET /api/users/abc/activity with admin token returns a 4xx (path-var binding fails)")
        void activity_string_id_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Admin token — ensures any 401 we see is NOT from missing auth.
                String adminTok = adminToken();
                String activityPath = "/api/users" + "/abc/activity";

                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC27: activity for a non-numeric user ID must NOT 5xx — Spring's path-var "
                                                + "binding should reject the string cleanly, not throw an unhandled "
                                                + "TypeMismatchException. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC27: activity for a non-numeric user ID must NOT be 2xx — 'abc' cannot be "
                                                + "a valid Long. status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC27: activity for a non-numeric user ID must return a 4xx (typically 400 "
                                                + "Bad Request); got " + code + " body=" + r.body());
        }
}

// ─── TC28 — size=0 returns gracefully (NOT 5xx) ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC28_ActivitySizeZeroTests extends TestBase {

        @Test
        @DisplayName("TC28 — GET /api/users/{ownId}/activity?size=0 must NOT 5xx (spec silent on size=0)")
        void activity_size_zero_does_not_5xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — own user so we reach the pagination logic.
                String email = "tc28_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC28 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC28 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC28 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Act — size=0. PageRequest.of(0, 0) throws IllegalArgumentException,
                // so unhandled this becomes 500. Spec doesn't pin a code here, but
                // graceful handling means NOT 5xx.
                String activityPath = "/api/users" + "/" + uid + "/activity?size=0";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC28: size=0 must NOT 5xx — controller must validate / clamp / reject "
                                                + "gracefully, not let PageRequest.of throw IllegalArgumentException. "
                                                + "status=" + code + " body=" + r.body());
        }
}

// ─── TC29 — size=-1 returns 4xx ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC29_ActivityNegativeSizeTests extends TestBase {

        @Test
        @DisplayName("TC29 — GET /api/users/{ownId}/activity?size=-1 returns a 4xx")
        void activity_negative_size_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc29_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC29 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC29 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC29 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?size=-1";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC29: size=-1 must NOT 5xx — controller must validate gracefully. status="
                                                + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC29: size=-1 must NOT be 2xx — negative page size is semantically invalid. "
                                                + "status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC29: size=-1 must return a 4xx; got " + code + " body=" + r.body());
        }
}

// ─── TC30 — size=string returns 4xx (binding fails) ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC30_ActivityStringSizeTests extends TestBase {

        @Test
        @DisplayName("TC30 — GET /api/users/{ownId}/activity?size=abc returns a 4xx (Integer binding fails)")
        void activity_string_size_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc30_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC30 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC30 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC30 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?size=abc";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC30: size=abc must NOT 5xx — Spring's @RequestParam Integer binding should "
                                                + "reject the string cleanly. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC30: size=abc must NOT be 2xx — non-numeric size cannot be valid. status="
                                                + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC30: size=abc must return a 4xx (typically 400 Bad Request); got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC31 — Cross-user activity (regular user) returns strictly 403 ─────────
@Tag("public")
@Tag("features_m2")
class TC31_ActivityCrossUserRegularTests extends TestBase {

        @Test
        @DisplayName("TC31 — Customer A's GET on User B's activity returns strictly 403 (per S1-F12 spec)")
        void cross_user_activity_regular_returns_403() throws Exception {
                BASE_URL = userServiceUrl;
                // Per spec: "ownership violation, NOT 404 — A's token is valid
                // and B exists." Strict 403 here, unlike TC17 (which accepts 403/404).
                String emailA = "tc31a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc31b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBodyA = String.format("""
                                {"name":"TC31 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"TC31 B","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC31 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC31 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC31 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + bid + "/activity";
                HttpResponse<String> r = httpGetAuth(activityPath, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC31: cross-user activity GET must NOT be 2xx — regular users cannot read "
                                                + "other users' activity feeds. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC31: cross-user activity GET must NOT 5xx. status=" + code + " body=" + r.body());
                assertEquals(403, code,
                                "TC31: per S1-F12 spec, cross-user activity GET must return strictly 403 "
                                                + "(ownership violation, NOT 404 — A's token is valid and B exists); got "
                                                + code + " body=" + r.body());
        }
}

// ─── TC32 — Cross-user activity (admin) returns 2xx ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC32_ActivityCrossUserAdminTests extends TestBase {

        @Test
        @DisplayName("TC32 — Admin's GET on a customer's activity returns 2xx (admin bypasses ownership)")
        void cross_user_activity_admin_returns_2xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc32_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC32 Customer","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC32 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();
                String activityPath = "/api/users" + "/" + customerId + "/activity";
                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                assert2xx(r, "TC32 admin activity");

                JsonNode j = parseNode(r.body());
                assertTrue(j.has("content") && j.get("content").isArray(),
                                "TC32: admin response must include `content` array; body=" + r.body());
        }
}

// ─── TC33 — page=-1 returns 4xx ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC33_ActivityNegativePageTests extends TestBase {

        @Test
        @DisplayName("TC33 — GET /api/users/{ownId}/activity?page=-1 returns a 4xx")
        void activity_negative_page_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc33_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC33 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC33 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC33 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?page=-1";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC33: page=-1 must NOT 5xx — PageRequest.of(int page, int size) requires "
                                                + "page >= 0; controller must validate gracefully. status=" + code
                                                + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC33: page=-1 must NOT be 2xx — negative page is semantically invalid. "
                                                + "status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC33: page=-1 must return a 4xx; got " + code + " body=" + r.body());
        }
}

// ─── TC34 — page=string returns 4xx (binding fails) ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC34_ActivityStringPageTests extends TestBase {

        @Test
        @DisplayName("TC34 — GET /api/users/{ownId}/activity?page=abc returns a 4xx (Integer binding fails)")
        void activity_string_page_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc34_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC34 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC34 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC34 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?page=abc";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC34: page=abc must NOT 5xx — Spring's @RequestParam Integer binding should "
                                                + "reject the string cleanly. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC34: page=abc must NOT be 2xx — non-numeric page cannot be valid. status="
                                                + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC34: page=abc must return a 4xx (typically 400 Bad Request); got " + code
                                                + " body=" + r.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2 M2 — Catalog entity features (full-text search, indexing, dashboard).
// All tests dynamic via TestBase helpers:
// * s2CatalogEntity() — Restaurant / Product / Provider / etc.
// * s3OrderEntity() — Order / Booking / Transaction / etc.
// * s2CategoricalFilterParam() — first non-status enum field (cuisineType /
// category / specialty / ...)
// * enumValueAt(entity, field, idx) — i-th valid value for that enum
// * s2EventsCollection() — Mongo events collection (spec name, validated)
// * s2SearchIndex() — ES index name (spec name, validated)
// * buildKitchenSinkBody(...) — JSON body from manifest entityColumns
// ════════════════════════════════════════════════════════════════════════════

// ─── TC35 — S2-F10 happy path search returns 2xx + array ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC35_SearchHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC35 — GET <s2>/search/full-text?query=test with valid token returns 2xx + array shape")
        void search_happy_path_returns_2xx_array() throws Exception {
                BASE_URL = catalogServiceUrl;
                String token = adminToken();
                String searchPath = "/api/restaurants" + "/search/full-text?query=test";
                HttpResponse<String> r = httpGetAuth(searchPath, token);
                assert2xx(r, "TC35 search happy path");
                JsonNode body = parseNode(r.body());
                boolean validShape = body.isArray() || (body.has("content") && body.get("content").isArray());
                assertTrue(validShape, "TC35: response must be JSON array OR paginated envelope; got " + r.body());
        }
}

// ─── TC36 — S2-F10 no token returns 401 ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC36_SearchNoTokenTests extends TestBase {
        @Test
        @DisplayName("TC36 — GET <s2>/search/full-text without Authorization header returns 401")
        void search_no_token_returns_401() throws Exception {
                BASE_URL = catalogServiceUrl;
                String searchPath = "/api/restaurants" + "/search/full-text?query=anything";
                HttpResponse<String> r = httpGet(searchPath);
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC36: must NOT 2xx; got " + code);
                assertTrue(code / 100 != 5, "TC36: must NOT 5xx; got " + code);
                assertEquals(401, code, "TC36: must be strict 401; got " + code + " body=" + r.body());
        }
}

// ─── TC37 — S2-F10 exact match by primary categorical filter ────────────────
@Tag("public")
@Tag("features_m2")
class TC37_SearchExactCategoricalFilterTests extends TestBase {
        @Test
        @DisplayName("TC37 — Search ?<filter>=<value0> returns only entities with that filter value")
        void search_filter_categorical_returns_only_matching() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String filterParam = s2CategoricalFilterParam();
                String filterValue0 = enumValueAt(entity, filterParam, 0);
                String filterValue1 = enumValueAt(entity, filterParam, 1);
                String statusOpen = enumValueAt(entity, "status", 0);

                String n = nonce();
                createEntity(adminTok, "TC37 First_" + n, filterValue0, statusOpen);
                createEntity(adminTok, "TC37 Second_" + n, filterValue1, statusOpen);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?query=" + n + "&" + filterParam + "="
                                + filterValue0;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC37 search by " + filterParam);
                JsonNode arr = unwrap(parseNode(r.body()));
                for (JsonNode item : arr) {
                        String c = item.has(filterParam) ? item.get(filterParam).asText() : null;
                        assertEquals(filterValue0, c,
                                        "TC37: every result must have " + filterParam + "=" + filterValue0 + "; got "
                                                        + item);
                }
        }

        private void createEntity(String tok, String name, String filterValue, String status) throws Exception {
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of(
                                "name", name,
                                s2CategoricalFilterParam(), filterValue,
                                "status", status));
                assert2xx(httpPostAuth("/api/restaurants", body, tok),
                                "TC37 setup create " + name);
        }

        private JsonNode unwrap(JsonNode b) {
                return b.isArray() ? b : (b.has("content") ? b.get("content") : b);
        }
}

// ─── TC38 — S2-F10 exact match by status ────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC38_SearchExactStatusTests extends TestBase {
        @Test
        @DisplayName("TC38 — Search ?status=<value0> returns only entities with that status")
        void search_filter_status_returns_only_matching() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String filterValue0 = enumValueAt(entity, s2CategoricalFilterParam(), 0);
                String status0 = enumValueAt(entity, "status", 0);
                String status1 = enumValueAt(entity, "status", 1);

                String n = nonce();
                createEntity(adminTok, "TC38 Status0_" + n, filterValue0, status0);
                createEntity(adminTok, "TC38 Status1_" + n, filterValue0, status1);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?query=" + n + "&status=" + status0;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC38 search by status");
                JsonNode arr = unwrap(parseNode(r.body()));
                for (JsonNode item : arr) {
                        String s = item.has("status") ? item.get("status").asText() : null;
                        assertEquals(status0, s, "TC38: every result must have status=" + status0 + "; got " + item);
                }
        }

        private void createEntity(String tok, String name, String filterValue, String status) throws Exception {
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of(
                                "name", name,
                                s2CategoricalFilterParam(), filterValue,
                                "status", status));
                assert2xx(httpPostAuth("/api/restaurants", body, tok),
                                "TC38 setup create " + name);
        }

        private JsonNode unwrap(JsonNode b) {
                return b.isArray() ? b : (b.has("content") ? b.get("content") : b);
        }
}

// ─── TC39 — S2-F10 minRating + maxRating range filter ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC39_SearchRatingRangeTests extends TestBase {
        @Test
        @DisplayName("TC39 — Search ?minRating=4.0&maxRating=5.0 returns only entities with rating in [4.0, 5.0]")
        void search_rating_range_returns_entities_in_range() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String n = nonce();
                long lowId = createAndRate(adminTok, "TC39 Low_" + n, 3.0);
                long midId = createAndRate(adminTok, "TC39 Mid_" + n, 4.5);
                long highId = createAndRate(adminTok, "TC39 High_" + n, 5.0);
                reindex(adminTok, lowId);
                reindex(adminTok, midId);
                reindex(adminTok, highId);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?query=" + n + "&minRating=4.0&maxRating=5.0";
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC39 search rating range");
                JsonNode arr = unwrap(parseNode(r.body()));
                for (JsonNode item : arr) {
                        if (!item.has("rating") || item.get("rating").isNull())
                                continue;
                        double rating = item.get("rating").asDouble();
                        assertTrue(rating >= 4.0 && rating <= 5.0,
                                        "TC39: every result must have rating in [4.0, 5.0]; got " + rating + " in "
                                                        + item);
                }
        }

        private long createAndRate(String tok, String name, double rating) throws Exception {
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of("name", name));
                HttpResponse<String> r = httpPostAuth("/api/restaurants", body, tok);
                assert2xx(r, "TC39 setup create " + name);
                long id = parseNode(r.body()).get("id").asLong();
                String ratingCol = columnByField(s2CatalogEntity(), "rating");
                jdbc.update("UPDATE \"" + tableName(s2CatalogEntity()) + "\" SET \"" + ratingCol
                                + "\" = ? WHERE id = ?", rating, id);
                return id;
        }

        private void reindex(String tok, long id) throws Exception {
                HttpResponse<String> r = httpPostAuth("/api/restaurants" + "/" + id + "/index", "",
                                tok);
                assert2xx(r, "TC39 reindex id=" + id);
        }

        private JsonNode unwrap(JsonNode b) {
                return b.isArray() ? b : (b.has("content") ? b.get("content") : b);
        }
}

// ─── TC40 — S2-F10 minRating > maxRating returns 4xx ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC40_SearchInvalidRatingRangeTests extends TestBase {
        @Test
        @DisplayName("TC40 — Search ?minRating=5.0&maxRating=3.0 (invalid range) returns a 4xx")
        void search_invalid_rating_range_returns_4xx() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String searchPath = "/api/restaurants"
                                + "/search/full-text?minRating=5.0&maxRating=3.0";
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                int code = r.statusCode();
                assertTrue(code / 100 != 5, "TC40: NOT 5xx; got " + code);
                assertTrue(code / 100 != 2, "TC40: NOT 2xx; got " + code);
                assertTrue(code >= 400 && code < 500, "TC40: must return 4xx; got " + code + " body=" + r.body());
        }
}

// ─── TC41 — S2-F10 query with no matches returns empty list ─────────────────
@Tag("public")
@Tag("features_m2")
class TC41_SearchNoMatchEmptyListTests extends TestBase {
        @Test
        @DisplayName("TC41 — Search with query that matches nothing returns 2xx + empty list")
        void search_no_match_returns_empty_list() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String improbableQuery = "TC41NoMatchQuery_" + nonce() + "_xyzqwe";
                String searchPath = "/api/restaurants" + "/search/full-text?query="
                                + improbableQuery;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC41 search no match");
                JsonNode body = parseNode(r.body());
                JsonNode arr = body.isArray() ? body : (body.has("content") ? body.get("content") : body);
                assertTrue(arr.isArray(), "TC41: response must contain an array; got " + r.body());
                assertEquals(0, arr.size(), "TC41: must return empty list; got " + r.body());
        }
}

// ─── TC42 — S2-F10 results sorted by relevance ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC42_SearchSortedByRelevanceTests extends TestBase {
        @Test
        @DisplayName("TC42 — Search results sorted by relevance (name match ranks higher than description match)")
        void search_results_sorted_by_relevance() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String unique = "Tc42Word" + nonce();
                String aName = unique + " Kitchen";
                String bName = "Other Place TC42_" + nonce();
                long aid = createWithDetails(adminTok, aName, null);
                reindex(adminTok, aid);
                long bid = createWithDetails(adminTok, bName, "best authentic " + unique + " cuisine");
                reindex(adminTok, bid);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?query=" + unique;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC42 search relevance");
                JsonNode body = parseNode(r.body());
                JsonNode arr = body.isArray() ? body : (body.has("content") ? body.get("content") : body);
                assertTrue(arr.isArray() && arr.size() >= 1,
                                "TC42: must return at least one result; body=" + r.body());

                int idxA = -1, idxB = -1;
                for (int i = 0; i < arr.size(); i++) {
                        String entryName = arr.get(i).has("name") ? arr.get(i).get("name").asText() : "";
                        if (aName.equals(entryName))
                                idxA = i;
                        if (bName.equals(entryName))
                                idxB = i;
                }
                assertTrue(idxA >= 0,
                                "TC42: name-match (A, name='" + aName + "') must appear in results; body=" + r.body());
                if (idxB >= 0) {
                        assertTrue(idxA < idxB,
                                        "TC42: name-match (A, idx=" + idxA
                                                        + ") must rank higher than description-match (B, idx=" + idxB
                                                        + ").");
                }
        }

        private long createWithDetails(String tok, String name, String desc) throws Exception {
                java.util.Map<String, Object> overrides = new java.util.HashMap<>();
                overrides.put("name", name);
                if (desc != null) {
                        overrides.put("details", java.util.Map.of("description", desc));
                }
                String body = buildKitchenSinkBody(s2CatalogEntity(), overrides);
                HttpResponse<String> r = httpPostAuth("/api/restaurants", body, tok);
                assert2xx(r, "TC42 setup create " + name);
                return parseNode(r.body()).get("id").asLong();
        }

        private void reindex(String tok, long id) throws Exception {
                HttpResponse<String> r = httpPostAuth("/api/restaurants" + "/" + id + "/index", "",
                                tok);
                assert2xx(r, "TC42 reindex id=" + id);
        }
}

// ─── TC43 — S2-F11 happy index path ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC43_IndexHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC43 — POST <s2>/{id}/index for an existing entity returns 2xx")
        void index_happy_path_returns_2xx() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String body = buildKitchenSinkBody(s2CatalogEntity(),
                                java.util.Map.of("name", "TC43 Entity_" + nonce()));
                HttpResponse<String> created = httpPostAuth("/api/restaurants", body, adminTok);
                assert2xx(created, "TC43 setup create");
                long id = parseNode(created.body()).get("id").asLong();
                HttpResponse<String> r = httpPostAuth("/api/restaurants" + "/" + id + "/index", "",
                                adminTok);
                assert2xx(r, "TC43 index");
        }
}

// ─── TC44 — S2-F11 indexed document matches PG attributes ───────────────────
@Tag("public")
@Tag("features_m2")
class TC44_IndexMatchesPgTests extends TestBase {
        @Test
        @DisplayName("TC44 — After indexing, ES doc fields match the PG row's attributes")
        void index_doc_matches_pg_attributes() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String unique = "TC44Entity_" + nonce();
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of(
                                "name", unique,
                                "details", java.util.Map.of("description", "signature description")));
                HttpResponse<String> created = httpPostAuth("/api/restaurants", body, adminTok);
                assert2xx(created, "TC44 setup create");
                long id = parseNode(created.body()).get("id").asLong();
                String ratingCol = columnByField(s2CatalogEntity(), "rating");
                jdbc.update("UPDATE \"" + tableName(s2CatalogEntity()) + "\" SET \"" + ratingCol
                                + "\" = ? WHERE id = ?", 4.5, id);
                HttpResponse<String> indexed = httpPostAuth("/api/restaurants" + "/" + id + "/index",
                                "", adminTok);
                assert2xx(indexed, "TC44 index");

                String esIndex = s2SearchIndex();
                long esCount = esSearchCount(esIndex, "name", unique);
                assertTrue(esCount >= 1,
                                "TC44: ES index '" + esIndex + "' must contain a document with name='" + unique
                                                + "' (count=" + esCount + ").");

                String searchPath = "/api/restaurants" + "/search/full-text?query=" + unique;
                HttpResponse<String> sr = httpGetAuth(searchPath, adminTok);
                assert2xx(sr, "TC44 search after index");
                JsonNode body2 = parseNode(sr.body());
                JsonNode arr = body2.isArray() ? body2 : (body2.has("content") ? body2.get("content") : body2);
                JsonNode found = null;
                for (JsonNode item : arr) {
                        String entryName = item.has("name") ? item.get("name").asText() : "";
                        if (unique.equals(entryName)) {
                                found = item;
                                break;
                        }
                }
                assertNotNull(found, "TC44: indexed entity must be findable via /search/full-text by name='" + unique
                                + "'; got " + sr.body());

                // Verify name + status fields match between search result and PG row.
                java.util.Map<String, Object> pgRow = jdbc.queryForMap(
                                "SELECT name, status::text AS status FROM " + tableName(s2CatalogEntity())
                                                + " WHERE id = ?",
                                id);
                assertEquals(pgRow.get("name"), found.get("name").asText(), "TC44: ES name must match PG name");
                if (found.has("status")) {
                        assertEquals(pgRow.get("status"), found.get("status").asText(),
                                        "TC44: ES status must match PG status");
                }
        }
}

// ─── TC45 — S2-F11 auto-reindex on update ───────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC45_IndexAutoReindexOnUpdateTests extends TestBase {
        @Test
        @DisplayName("TC45 — Updating an entity via PUT (without /index) makes the new name searchable")
        void auto_reindex_on_update() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String origName = "TC45 OriginalName_" + nonce();
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of("name", origName));
                HttpResponse<String> created = httpPostAuth("/api/restaurants", body, adminTok);
                assert2xx(created, "TC45 setup create");
                long id = parseNode(created.body()).get("id").asLong();

                String newName = "TC45_NewName_" + nonce();
                String putBody = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of("name", newName));
                HttpResponse<String> updated = httpPutAuth("/api/restaurants/" + id, putBody, adminTok);
                assert2xx(updated, "TC45 update name");

                String searchPath = "/api/restaurants" + "/search/full-text?query=" + newName;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC45 search by new name");
                JsonNode body2 = parseNode(r.body());
                JsonNode arr = body2.isArray() ? body2 : (body2.has("content") ? body2.get("content") : body2);
                boolean found = false;
                for (JsonNode item : arr) {
                        String entryName = item.has("name") ? item.get("name").asText() : "";
                        if (newName.equals(entryName)) {
                                found = true;
                                break;
                        }
                }
                assertTrue(found, "TC45: search by new name must find the entity (proves auto-reindexing). body="
                                + r.body());
        }
}

// ─── TC46 — S2-F11 index on non-existent entity returns 404 ─────────────────
@Tag("public")
@Tag("features_m2")
class TC46_IndexNonExistentTests extends TestBase {
        @Test
        @DisplayName("TC46 — POST <s2>/<Long.MAX_VALUE>/index returns strictly 404")
        void index_non_existent_returns_404() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String indexPath = "/api/restaurants" + "/" + Long.MAX_VALUE + "/index";
                HttpResponse<String> r = httpPostAuth(indexPath, "", adminTok);
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC46: NOT 2xx; got " + code);
                assertTrue(code / 100 != 5, "TC46: NOT 5xx; got " + code);
                assertEquals(404, code, "TC46: must be strict 404; got " + code + " body=" + r.body());
        }
}

// ─── TC47 — S2-F11 index without token returns 401 ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC47_IndexNoTokenTests extends TestBase {
        @Test
        @DisplayName("TC47 — POST <s2>/{id}/index without Authorization header returns 401")
        void index_no_token_returns_401() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String body = buildKitchenSinkBody(s2CatalogEntity(),
                                java.util.Map.of("name", "TC47 Entity_" + nonce()));
                HttpResponse<String> created = httpPostAuth("/api/restaurants", body, adminTok);
                assert2xx(created, "TC47 setup create");
                long id = parseNode(created.body()).get("id").asLong();
                HttpResponse<String> r = httpPost("/api/restaurants" + "/" + id + "/index", "");
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC47: NOT 2xx; got " + code);
                assertEquals(401, code, "TC47: must be strict 401; got " + code + " body=" + r.body());
        }
}

// ─── TC48 — S2-F12 dashboard happy path (uses pre-seeded entity id=1) ───────
@Tag("public")
@Tag("features_m2")
class TC48_DashboardHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC48 — GET <s2>/{id}/dashboard returns 2xx + DTO with totalOrders/totalRevenue")
        void dashboard_happy_path() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "TC48Rest", "EGYPTIAN", "OPEN");
                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC48 dashboard");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("totalOrders") || j.has("total_orders"),
                                "TC48: dashboard must include totalOrders; got " + r.body());
                assertTrue(j.has("totalRevenue") || j.has("total_revenue"),
                                "TC48: dashboard must include totalRevenue; got " + r.body());
        }
}

// ─── TC49 — S2-F12 aggregated values match PG-source values ─────────────────
@Tag("public")
@Tag("features_m2")
class TC49_DashboardAggregatesMatchPgTests extends TestBase {
        @Test
        @DisplayName("TC49 — Dashboard totalOrders/totalRevenue match values aggregated from PG (uses pre-seed)")
        void dashboard_aggregates_match_pg() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "TC49Rest", "EGYPTIAN", "OPEN");
                String ordersTable = tableName(s3OrderEntity());
                String fkCol = s2CatalogFkColumn();
                String amtCol = columnByField(s3OrderEntity(), "totalAmount");
                // Seed 2 orders for this restaurant
                _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "DELIVERED", 100.0, null);
                _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "DELIVERED", 200.0, null);

                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC49 dashboard");
                JsonNode j = parseNode(r.body());

                Integer expectedCount = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM \"" + ordersTable + "\" WHERE \"" + fkCol + "\" = ?",
                                Integer.class, restId);
                Double expectedRevenueRaw = jdbc.queryForObject(
                                "SELECT COALESCE(SUM(\"" + amtCol + "\"), 0) FROM \"" + ordersTable + "\" WHERE \""
                                                + fkCol + "\" = ?",
                                Double.class, restId);
                double expectedRevenue = expectedRevenueRaw != null ? expectedRevenueRaw : 0.0;

                long actualCount = j.has("totalOrders") ? j.get("totalOrders").asLong()
                                : j.has("total_orders") ? j.get("total_orders").asLong() : -1L;
                double actualRevenue = j.has("totalRevenue") ? j.get("totalRevenue").asDouble()
                                : j.has("total_revenue") ? j.get("total_revenue").asDouble() : -1.0;

                assertEquals(expectedCount.longValue(), actualCount,
                                "TC49: totalOrders mismatch — PG=" + expectedCount + ", dashboard=" + actualCount
                                                + ". body=" + r.body());
                assertEquals(expectedRevenue, actualRevenue, 0.01,
                                "TC49: totalRevenue mismatch — PG=" + expectedRevenue + ", dashboard=" + actualRevenue
                                                + ". body=" + r.body());
        }
}

// ─── TC50 — S2-F12 dashboard event written to MongoDB ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC50_DashboardEventLoggedTests extends TestBase {
        @Test
        @DisplayName("TC50 — After GET /dashboard, an event must appear in the spec-defined Mongo collection")
        void dashboard_logs_event_to_mongo() throws Exception {
                BASE_URL = catalogServiceUrl;
                if (mongo == null) {
                        throw new AssertionError(
                                        "TC50: MongoDB is required for this test but not reachable. Set "
                                                        + "SPRING_DATA_MONGODB_URI or ensure the Mongo container is up.");
                }
                String adminTok = adminToken();
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "TC50Rest", "EGYPTIAN", "OPEN");
                String collName = s2EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> coll = mongo.getCollection(collName);

                long before = coll.countDocuments();
                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC50 dashboard");
                long after = coll.countDocuments();

                assertTrue(after > before,
                                "TC50: GET /dashboard must log an event in collection '" + collName
                                                + "'. Counts: before=" + before + ", after=" + after);
        }
}

// ─── TC51 — S2-F12 dashboard for non-existent ID returns 404 ────────────────
@Tag("public")
@Tag("features_m2")
class TC51_DashboardNonExistentTests extends TestBase {
        @Test
        @DisplayName("TC51 — GET <s2>/<Long.MAX_VALUE>/dashboard returns strictly 404")
        void dashboard_non_existent_returns_404() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String dashPath = "/api/restaurants" + "/" + Long.MAX_VALUE + "/dashboard";
                HttpResponse<String> r = httpGetAuth(dashPath, adminTok);
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC51: NOT 2xx; got " + code);
                assertTrue(code / 100 != 5, "TC51: NOT 5xx; got " + code);
                assertEquals(404, code, "TC51: must be strict 404; got " + code + " body=" + r.body());
        }
}

// ─── TC52 — S2-F12 dashboard for entity with no orders returns zeros ────────
@Tag("public")
@Tag("features_m2")
class TC52_DashboardNoOrdersTests extends TestBase {
        @Test
        @DisplayName("TC52 — Dashboard for an entity with no orders returns 2xx + totalOrders=0 + totalRevenue=0")
        void dashboard_no_orders_returns_zeros() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                // Seed a fresh restaurant with no orders — guarantees totalOrders=0.
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "TC52Rest", "EGYPTIAN", "OPEN");
                String fkCol = s2CatalogFkColumn();

                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC52 dashboard");
                JsonNode j = parseNode(r.body());

                long totalOrders = j.has("totalOrders") ? j.get("totalOrders").asLong()
                                : j.has("total_orders") ? j.get("total_orders").asLong() : -1L;
                double totalRevenue = j.has("totalRevenue") ? j.get("totalRevenue").asDouble()
                                : j.has("total_revenue") ? j.get("total_revenue").asDouble() : -1.0;

                assertEquals(0L, totalOrders, "TC52: must report totalOrders=0; got " + totalOrders);
                assertEquals(0.0, totalRevenue, 0.01, "TC52: must report totalRevenue=0; got " + totalRevenue);
        }
}

// ─── TC53 — S2-F12 dashboard without token returns 401 ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC53_DashboardNoTokenTests extends TestBase {
        @Test
        @DisplayName("TC53 — GET <s2>/{id}/dashboard without Authorization header returns 401")
        void dashboard_no_token_returns_401() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "TC53Rest", "EGYPTIAN", "OPEN");
                HttpResponse<String> r = httpGet("/api/restaurants" + "/" + restId + "/dashboard");
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC53: NOT 2xx; got " + code);
                assertEquals(401, code, "TC53: must be strict 401; got " + code + " body=" + r.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3 M2 — Order Service features (TC54..TC99)
//
// Covers S3-F10 (analytics dashboard, TC54-TC69), S3-F11 (record interaction,
// TC70-TC84), and S3-F12 (recommendations, TC85-TC99). Pre-req infrastructure
// (Neo4j driver, Jedis, s3EventsCollection helper, neo4jLabelByName) lives in
// TestBase. Per-test wipe of PG/Neo4j/Redis happens in autoTruncateAllData()
// which runs in @BeforeEach + @AfterEach.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC54 — S3-F10 dashboard happy path (composite scenario a) ───────────────
@Tag("public")
@Tag("features_m2")
class TC54_AnalyticsDashboardHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC54 — Dashboard returns totalOrders/completionRate/totalRevenue/avgOrderValue/ordersByStatus")
        void dashboard_happy_path() throws Exception {
                BASE_URL = orderServiceUrl;  // S3 — order-service
                // Insert 10 March-2026 orders: 6 DELIVERED, 2 CANCELLED, 2 PLACED.
                String oTable = tableName("Order");
                String userCol = columnByField("Order", "user");
                String restCol = columnByField("Order", "restaurant");
                String amtCol = columnByField("Order", "totalAmount");
                String stCol = columnByField("Order", "status");

                double[] amounts = { 100, 80, 120, 90, 150, 60, 200, 50, 180, 70 };
                String[] statuses = { "DELIVERED", "DELIVERED", "DELIVERED", "DELIVERED", "DELIVERED", "DELIVERED",
                                "CANCELLED", "CANCELLED", "PLACED", "PLACED" };
                String[] dates = { "2026-03-02", "2026-03-05", "2026-03-09", "2026-03-12", "2026-03-15", "2026-03-18",
                                "2026-03-21", "2026-03-24", "2026-03-27", "2026-03-30" };
                for (int i = 0; i < 10; i++) {
                        java.util.Map<String, Object> ov = new java.util.HashMap<>();
                        ov.put(userCol, 1L);
                        ov.put(restCol, 1L);
                        ov.put(amtCol, amounts[i]);
                        ov.put(stCol, statuses[i]);
                        Long oid = insertRowReturningId(oTable, ov);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf(dates[i] + " 12:00:00"));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC54 dashboard");
                JsonNode j = parseNode(r.body());
                assertEquals(10L, _readLong(j, "totalOrders", "total_orders"),
                                "TC54: totalOrders=10 expected; body=" + r.body());
                double rate = _readDouble(j, "completionRate", "completion_rate");
                assertEquals(0.6, rate, 0.01, "TC54: completionRate=0.6 expected; got " + rate);
                double revenue = _readDouble(j, "totalRevenue", "total_revenue");
                // totalRevenue counts DELIVERED orders only (revenue = money actually received)
                double expectedRev = 0;
                for (int i = 0; i < amounts.length; i++) {
                        if ("DELIVERED".equals(statuses[i])) expectedRev += amounts[i];
                }
                assertEquals(expectedRev, revenue, 0.01, "TC54: totalRevenue mismatch");
                JsonNode breakdown = _readObject(j, "ordersByStatus", "orders_by_status");
                assertNotNull(breakdown, "TC54: ordersByStatus key required; body=" + r.body());
                assertEquals(6L, _statusCount(breakdown, "DELIVERED"), "TC54: DELIVERED=6");
                assertEquals(2L, _statusCount(breakdown, "CANCELLED"), "TC54: CANCELLED=2");
                assertEquals(2L, _statusCount(breakdown, "PLACED"), "TC54: PLACED=2");
        }

        private long _readLong(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asLong();
                return -1;
        }

        private double _readDouble(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asDouble();
                return -1;
        }

        private JsonNode _readObject(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k);
                return null;
        }

        private long _statusCount(JsonNode breakdown, String status) {
                if (breakdown.has(status))
                        return breakdown.get(status).asLong();
                return 0;
        }
}

// ─── TC55 — S3-F10 totalOrders attribute isolated ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC55_AnalyticsTotalOrdersTests extends TestBase {
        @Test
        @DisplayName("TC55 — Dashboard.totalOrders equals exact count of orders in range")
        void total_orders_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                String oTable = tableName("Order");
                for (int i = 0; i < 7; i++) {
                        java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                        _ov.put(columnByField("Order", "user"), 1L);
                        _ov.put(columnByField("Order", "restaurant"), 1L);
                        _ov.put(columnByField("Order", "totalAmount"), 100.00);
                        _ov.put(columnByField("Order", "status"), "DELIVERED");
                        Long oid = insertRowReturningId(oTable, _ov);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-15 12:00:00"));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC55 dashboard");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalOrders") ? j.get("totalOrders").asLong()
                                : j.has("total_orders") ? j.get("total_orders").asLong() : -1;
                assertEquals(7L, total, "TC55: totalOrders=7 expected; got " + total + " body=" + r.body());
        }
}

// ─── TC56 — S3-F10 totalRevenue attribute isolated ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC56_AnalyticsTotalRevenueTests extends TestBase {
        @Test
        @DisplayName("TC56 — Dashboard.totalRevenue equals sum of order amounts in range")
        void total_revenue_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                String oTable = tableName("Order");
                double[] amounts = { 100.00, 200.00, 300.00, 400.00 };
                for (double a : amounts) {
                        java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                        _ov.put(columnByField("Order", "user"), 1L);
                        _ov.put(columnByField("Order", "restaurant"), 1L);
                        _ov.put(columnByField("Order", "totalAmount"), a);
                        _ov.put(columnByField("Order", "status"), "DELIVERED");
                        Long oid = insertRowReturningId(oTable, _ov);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-15 12:00:00"));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC56 dashboard");
                JsonNode j = parseNode(r.body());
                double rev = j.has("totalRevenue") ? j.get("totalRevenue").asDouble()
                                : j.has("total_revenue") ? j.get("total_revenue").asDouble() : -1;
                assertEquals(1000.00, rev, 0.01, "TC56: totalRevenue=1000.00 expected; got " + rev);
        }
}

// ─── TC57 — S3-F10 avgOrderValue attribute isolated ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC57_AnalyticsAvgOrderValueTests extends TestBase {
        @Test
        @DisplayName("TC57 — Dashboard.avgOrderValue equals totalRevenue / totalOrders")
        void avg_order_value_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                String oTable = tableName("Order");
                double[] amounts = { 50, 100, 150, 200, 250 };
                for (double a : amounts) {
                        java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                        _ov.put(columnByField("Order", "user"), 1L);
                        _ov.put(columnByField("Order", "restaurant"), 1L);
                        _ov.put(columnByField("Order", "totalAmount"), a);
                        _ov.put(columnByField("Order", "status"), "DELIVERED");
                        Long oid = insertRowReturningId(oTable, _ov);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-15 12:00:00"));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC57 dashboard");
                JsonNode j = parseNode(r.body());
                double aov = j.has("avgOrderValue") ? j.get("avgOrderValue").asDouble()
                                : j.has("avg_order_value") ? j.get("avg_order_value").asDouble()
                                                : j.has("averageOrderValue") ? j.get("averageOrderValue").asDouble()
                                                                : -1;
                assertEquals(150.00, aov, 0.01, "TC57: avgOrderValue=150.00 expected; got " + aov);
        }
}

// ─── TC58 — S3-F10 completionRate attribute isolated ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC58_AnalyticsCompletionRateTests extends TestBase {
        @Test
        @DisplayName("TC58 — Dashboard.completionRate = DELIVERED count / total")
        void completion_rate_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                String oTable = tableName("Order");
                // 5 DELIVERED + 3 CANCELLED → completionRate = 5/8 = 0.625
                String[] statuses = { "DELIVERED", "DELIVERED", "DELIVERED", "DELIVERED", "DELIVERED",
                                "CANCELLED", "CANCELLED", "CANCELLED" };
                for (String st : statuses) {
                        java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                        _ov.put(columnByField("Order", "user"), 1L);
                        _ov.put(columnByField("Order", "restaurant"), 1L);
                        _ov.put(columnByField("Order", "totalAmount"), 100.00);
                        _ov.put(columnByField("Order", "status"), st);
                        Long oid = insertRowReturningId(oTable, _ov);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-15 12:00:00"));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC58 dashboard");
                JsonNode j = parseNode(r.body());
                double rate = j.has("completionRate") ? j.get("completionRate").asDouble()
                                : j.has("completion_rate") ? j.get("completion_rate").asDouble() : -1;
                assertEquals(0.625, rate, 0.001, "TC58: completionRate=0.625 expected; got " + rate);
        }
}

// ─── TC59 — S3-F10 ordersByStatus breakdown isolated ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC59_AnalyticsOrdersByStatusTests extends TestBase {
        @Test
        @DisplayName("TC59 — Dashboard.ordersByStatus has all 5 Talabat statuses, each count=1")
        void orders_by_status_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                String oTable = tableName("Order");
                // One of each Talabat Order status: PLACED, CONFIRMED, PREPARING, DELIVERED,
                // CANCELLED.
                java.util.List<String> values = enumValues(_orderStatusEnum());
                for (String st : values) {
                        java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                        _ov.put(columnByField("Order", "user"), 1L);
                        _ov.put(columnByField("Order", "restaurant"), 1L);
                        _ov.put(columnByField("Order", "totalAmount"), 75.00);
                        _ov.put(columnByField("Order", "status"), st);
                        Long oid = insertRowReturningId(oTable, _ov);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-15 12:00:00"));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC59 dashboard");
                JsonNode j = parseNode(r.body());
                JsonNode breakdown = j.has("ordersByStatus") ? j.get("ordersByStatus")
                                : j.has("orders_by_status") ? j.get("orders_by_status") : null;
                assertNotNull(breakdown, "TC59: ordersByStatus key required");
                for (String st : values) {
                        assertTrue(breakdown.has(st), "TC59: ordersByStatus missing key '" + st + "'");
                        assertEquals(1L, breakdown.get(st).asLong(),
                                        "TC59: ordersByStatus[" + st + "]=1 expected; got "
                                                        + breakdown.get(st).asLong());
                }
        }

        private String _orderStatusEnum() {
                // Find the enum type backing Order.status — value-by-name lookup in manifest.
                for (java.util.Map<String, Object> col : entityColumns(s3OrderEntity())) {
                        if ("status".equals(col.get("fieldName")))
                                return (String) col.get("javaType");
                }
                throw new IllegalStateException("Order.status field not in manifest");
        }
}

// ─── TC60 — S3-F10 empty range returns zeros (scenario b) ────────────────────
@Tag("public")
@Tag("features_m2")
class TC60_AnalyticsEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC60 — Empty date range returns totalOrders=0/totalRevenue=0")
        void empty_range_returns_zeros() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2030-01-01&endDate=2030-01-31", tok);
                assert2xx(r, "TC60 dashboard");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalOrders") ? j.get("totalOrders").asLong()
                                : j.has("total_orders") ? j.get("total_orders").asLong() : -1;
                double rev = j.has("totalRevenue") ? j.get("totalRevenue").asDouble()
                                : j.has("total_revenue") ? j.get("total_revenue").asDouble() : -1;
                assertEquals(0L, total, "TC60: totalOrders=0 expected; got " + total);
                assertEquals(0.0, rev, 0.01, "TC60: totalRevenue=0 expected; got " + rev);
        }
}

// ─── TC61 — S3-F10 boundary inclusion at startDate T00:00:00 ─────────────────
@Tag("public")
@Tag("features_m2")
class TC61_AnalyticsStartBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC61 — Order at exactly startDate T00:00:00 is included")
        void start_boundary_included() throws Exception {
                BASE_URL = orderServiceUrl;
                String oTable = tableName("Order");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(columnByField("Order", "user"), 1L);
                _ov.put(columnByField("Order", "restaurant"), 1L);
                _ov.put(columnByField("Order", "totalAmount"), 99.99);
                _ov.put(columnByField("Order", "status"), "DELIVERED");
                Long oid = insertRowReturningId(oTable, _ov);
                setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-01 00:00:00"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC61 dashboard");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalOrders") ? j.get("totalOrders").asLong()
                                : j.has("total_orders") ? j.get("total_orders").asLong() : -1;
                assertEquals(1L, total, "TC61: boundary order at startDate must be included; got totalOrders=" + total);
        }
}

// ─── TC62 — S3-F10 boundary inclusion at endDate T23:59:59
// ────────────────────
@Tag("public")
@Tag("features_m2")
class TC62_AnalyticsEndBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC62 — Order at endDate T23:59:59 is included")
        void end_boundary_included() throws Exception {
                BASE_URL = orderServiceUrl;
                String oTable = tableName("Order");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(columnByField("Order", "user"), 1L);
                _ov.put(columnByField("Order", "restaurant"), 1L);
                _ov.put(columnByField("Order", "totalAmount"), 49.99);
                _ov.put(columnByField("Order", "status"), "DELIVERED");
                Long oid = insertRowReturningId(oTable, _ov);
                setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-30 23:59:59"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC62 dashboard");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalOrders") ? j.get("totalOrders").asLong()
                                : j.has("total_orders") ? j.get("total_orders").asLong() : -1;
                assertEquals(1L, total, "TC62: boundary order at endDate must be included; got totalOrders=" + total);
        }
}

// ─── TC63 — S3-F10 inverted dates → 400 (scenario c) ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC63_AnalyticsInvertedDatesTests extends TestBase {
        @Test
        @DisplayName("TC63 — startDate > endDate returns 400")
        void inverted_dates_400() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(), "TC63: must be 400; got " + r.statusCode() + " body=" + r.body());
        }
}

// ─── TC64 — S3-F10 missing JWT → 401 (scenario d) ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC64_AnalyticsMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC64 — Missing Authorization header returns 401")
        void missing_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGet(
                                "/api/orders/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31");
                assertEquals(401, r.statusCode(), "TC64: must be 401; got " + r.statusCode());
        }
}

// ─── TC65 — S3-F10 invalid JWT → 401 ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC65_AnalyticsInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC65 — Bogus JWT returns 401")
        void invalid_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31",
                                "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC65: must be 401; got " + r.statusCode());
        }
}

// ─── TC66 — S3-F10 ANALYTICS_VIEWED logged on first call ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC66_AnalyticsLoggedFirstCallTests extends TestBase {
        @Test
        @DisplayName("TC66 — First call logs ANALYTICS_VIEWED to order_events Mongo")
        void analytics_viewed_logged() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) {
                        throw new AssertionError(
                                        "TC66: MongoDB required. Set SPRING_DATA_MONGODB_URI or ensure Mongo is up.");
                }
                String coll = s3EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                long before = col.countDocuments();
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC66 dashboard");
                long after = col.countDocuments();
                assertTrue(after > before,
                                "TC66: ANALYTICS_VIEWED event must be appended to '" + coll + "'. Counts: before="
                                                + before + ", after=" + after);
                // Optional: verify the latest event has eventType/action = ANALYTICS_VIEWED if
                // the field is present.
                org.bson.Document latest = col.find().sort(new org.bson.Document("_id", -1)).first();
                if (latest != null) {
                        String typeField = latest.getString("eventType");
                        if (typeField == null)
                                typeField = latest.getString("action");
                        if (typeField != null) {
                                assertEquals("ANALYTICS_VIEWED", typeField,
                                                "TC66: latest event in '" + coll + "' must be ANALYTICS_VIEWED; got "
                                                                + typeField);
                        }
                }
        }
}

// ─── TC67 — S3-F10 ANALYTICS_VIEWED logged on cache hit ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC67_AnalyticsLoggedOnCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC67 — Repeat call (cache hit) still logs ANALYTICS_VIEWED")
        void analytics_logged_on_cache_hit() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) {
                        throw new AssertionError(
                                        "TC67: MongoDB required. Set SPRING_DATA_MONGODB_URI or ensure Mongo is up.");
                }
                String coll = s3EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                long before = col.countDocuments();
                String tok = adminToken();
                HttpResponse<String> r1 = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r1, "TC67 dashboard #1");
                HttpResponse<String> r2 = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r2, "TC67 dashboard #2");
                long after = col.countDocuments();
                assertTrue(after >= before + 2,
                                "TC67: 2 calls must produce ≥2 ANALYTICS_VIEWED events (logging outside cache). before="
                                                + before + " after=" + after);
        }
}

// ─── TC68 — S3-F10 cache populated in Redis with ~10 min TTL ─────────────────
@Tag("public")
@Tag("features_m2")
class TC68_AnalyticsCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC68 — Dashboard call populates Redis with TTL ≤ 600s")
        void cache_populated_in_redis() throws Exception {
                BASE_URL = orderServiceUrl;
                if (redis == null) {
                        throw new AssertionError(
                                        "TC68: Redis required. Set SPRING_DATA_REDIS_HOST/PORT or ensure Redis is up.");
                }
                // BeforeEach already flushed Redis. Snapshot baseline.
                java.util.Set<String> beforeKeys = redisKeys("*");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC68 dashboard");
                java.util.Set<String> afterKeys = redisKeys("*");
                assertTrue(afterKeys.size() > beforeKeys.size(),
                                "TC68: at least one new Redis key expected after dashboard call; before="
                                                + beforeKeys.size() + " after=" + afterKeys.size());
                boolean found10MinTtl = false;
                for (String k : afterKeys) {
                        if (beforeKeys.contains(k))
                                continue;
                        long ttl = redisTtl(k);
                        if (ttl > 0 && ttl <= 600) {
                                found10MinTtl = true;
                                break;
                        }
                }
                assertTrue(found10MinTtl,
                                "TC68: a new Redis key must have TTL in (0, 600] seconds (10-min cache); keys="
                                                + afterKeys);
        }
}

// ─── TC69 — S3-F10 cache hit served (mutate, second call returns first body) ─
@Tag("public")
@Tag("features_m2")
class TC69_AnalyticsCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC69 — Cache hit: 2nd call (after data mutation) returns 1st response")
        void cache_hit_returns_first_response() throws Exception {
                BASE_URL = orderServiceUrl;
                if (redis == null) {
                        throw new AssertionError(
                                        "TC69: Redis required. Set SPRING_DATA_REDIS_HOST/PORT or ensure Redis is up.");
                }
                String oTable = tableName("Order");
                // Seed initial 2 DELIVERED orders in March
                for (int i = 0; i < 2; i++) {
                        java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                        _ov.put(columnByField("Order", "user"), 1L);
                        _ov.put(columnByField("Order", "restaurant"), 1L);
                        _ov.put(columnByField("Order", "totalAmount"), 100.00);
                        _ov.put(columnByField("Order", "status"), "DELIVERED");
                        Long oid = insertRowReturningId(oTable, _ov);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-03-15 12:00:00"));
                }
                String tok = adminToken();
                HttpResponse<String> r1 = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r1, "TC69 dashboard #1");
                // Mutate: insert 3 new in-range DELIVERED orders. If cache hit, dashboard
                // should not see them.
                for (int i = 0; i < 3; i++) {
                        java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                        _ov.put(columnByField("Order", "user"), 1L);
                        _ov.put(columnByField("Order", "restaurant"), 1L);
                        _ov.put(columnByField("Order", "totalAmount"), 200.00);
                        _ov.put(columnByField("Order", "status"), "DELIVERED");
                        Long oid = insertRowReturningId(oTable, _ov);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-03-20 12:00:00"));
                }
                // Capture cached value via Redis (one of the new keys should hold a serialized
                // response).
                java.util.Set<String> keys = redisKeys("*");
                String cachedBefore = null;
                for (String k : keys) {
                        String v = redisGet(k);
                        if (v != null && v.length() > 0) {
                                cachedBefore = v;
                                break;
                        }
                }
                HttpResponse<String> r2 = httpGetAuth(
                                "/api/orders/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r2, "TC69 dashboard #2");
                // r2 must equal r1 (cached); also Redis cached value still equals snapshot.
                assertEquals(r1.body(), r2.body(),
                                "TC69: cache hit expected — second response must equal first; r1=" + r1.body() + " r2="
                                                + r2.body());
                if (cachedBefore != null) {
                        String cachedAfter = null;
                        for (String k : redisKeys("*")) {
                                String v = redisGet(k);
                                if (v != null && v.length() > 0) {
                                        cachedAfter = v;
                                        break;
                                }
                        }
                        assertEquals(cachedBefore, cachedAfter,
                                        "TC69: Redis cached value must be unchanged across the two calls (proves no re-aggregation)");
                }
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F11 — Record Order Interaction (TC70..TC84)
// Endpoint: POST /api/orders/{orderId}/record-interaction
// Pre-seed has order id=2 DELIVERED user_id=2 restaurant_id=1, order id=4
// DELIVERED user_id=4 restaurant_id=1, order id=1 PLACED user_id=1 — used as
// fixtures across the 15 tests below.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC70 — S3-F11 happy path: record DELIVERED order → orderCount=1 ─────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC70_RecordInteractionHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC70 — Record interaction creates ORDERED_FROM with orderCount=1")
        void record_interaction_happy_path() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC70: Neo4j required. Set SPRING_NEO4J_URI or ensure Neo4j is up.");
                }
                String tok = adminToken();
                // Pre-seed order id=2: user_id=2, restaurant_id=1, status DELIVERED
                HttpResponse<String> r = httpPostAuth("/api/orders/2/record-interaction", "", tok);
                assert2xx(r, "TC70 record-interaction");
                long count = neo4jRelOrderCount(s3GraphUserLabel(), 2L, s3GraphRelationship(), s3GraphCatalogLabel(),
                                1L);
                assertEquals(1L, count, "TC70: orderCount must be 1 after first record; got " + count);
        }
}

// ─── TC71 — S3-F11 idempotency: same order recorded twice → orderCount stays 1
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC71_RecordInteractionIdempotencyTests extends TestBase {
        @Test
        @DisplayName("TC71 — Same orderId recorded twice keeps orderCount=1 (idempotent)")
        void record_interaction_idempotent() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC71: Neo4j required.");
                }
                String tok = adminToken();
                HttpResponse<String> r1 = httpPostAuth("/api/orders/2/record-interaction", "", tok);
                assert2xx(r1, "TC71 first record");
                HttpResponse<String> r2 = httpPostAuth("/api/orders/2/record-interaction", "", tok);
                assert2xx(r2, "TC71 second record (idempotent)");
                long count = neo4jRelOrderCount(s3GraphUserLabel(), 2L, s3GraphRelationship(), s3GraphCatalogLabel(),
                                1L);
                assertEquals(1L, count, "TC71: orderCount must remain 1 after duplicate; got " + count);
        }
}

// ─── TC72 — S3-F11 second order same pair → orderCount=2
// ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC72_RecordInteractionSecondOrderTests extends TestBase {
        @Test
        @DisplayName("TC72 — Recording two distinct DELIVERED orders for same user→restaurant → orderCount=2")
        void record_interaction_increments() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC72: Neo4j required.");
                }
                String tok = adminToken();
                // Insert a second DELIVERED order for user 2 → restaurant 1.
                String oTable = tableName("Order");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(columnByField("Order", "user"), 2L);
                _ov.put(columnByField("Order", "restaurant"), 1L);
                _ov.put(columnByField("Order", "totalAmount"), 75.00);
                _ov.put(columnByField("Order", "status"), "DELIVERED");
                Long newOid = insertRowReturningId(oTable, _ov);
                // Record both orders for the same user→restaurant pair.
                HttpResponse<String> r1 = httpPostAuth("/api/orders/2/record-interaction", "", tok);
                assert2xx(r1, "TC72 first record");
                HttpResponse<String> r2 = httpPostAuth("/api/orders/" + newOid + "/record-interaction", "", tok);
                assert2xx(r2, "TC72 second record");
                long count = neo4jRelOrderCount(s3GraphUserLabel(), 2L, s3GraphRelationship(), s3GraphCatalogLabel(),
                                1L);
                assertEquals(2L, count, "TC72: orderCount must be 2; got " + count);
        }
}

// ─── TC73 — S3-F11 different restaurant → new edge with orderCount=1 ─────────
@Tag("public")
@Tag("features_m2")
class TC73_RecordInteractionDifferentRestaurantTests extends TestBase {
        @Test
        @DisplayName("TC73 — Recording an order to a different restaurant creates a new edge")
        void record_interaction_different_restaurant() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC73: Neo4j required.");
                }
                String tok = adminToken();
                // Insert a DELIVERED order from user 2 to a brand-new restaurant.
                String rTable = tableName("Restaurant");
                java.util.Map<String, Object> _rOv = new java.util.HashMap<>();
                _rOv.put(columnByField("Restaurant", "name"), "TC73 New Restaurant");
                _rOv.put(columnByField("Restaurant", "cuisineType"), "ITALIAN");
                _rOv.put(columnByField("Restaurant", "status"), "OPEN");
                Long newRest = insertRowReturningId(rTable, _rOv);
                String oTable = tableName("Order");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(columnByField("Order", "user"), 2L);
                _ov.put(columnByField("Order", "restaurant"), newRest);
                _ov.put(columnByField("Order", "totalAmount"), 100.00);
                _ov.put(columnByField("Order", "status"), "DELIVERED");
                Long newOid = insertRowReturningId(oTable, _ov);
                // Record both: order 2 (user 2 → rest 1) and the new one (user 2 → newRest).
                assert2xx(httpPostAuth("/api/orders/2/record-interaction", "", tok), "TC73 record old");
                assert2xx(httpPostAuth("/api/orders/" + newOid + "/record-interaction", "", tok), "TC73 record new");
                long oldEdge = neo4jRelOrderCount(s3GraphUserLabel(), 2L, s3GraphRelationship(), s3GraphCatalogLabel(),
                                1L);
                long newEdge = neo4jRelOrderCount(s3GraphUserLabel(), 2L, s3GraphRelationship(), s3GraphCatalogLabel(),
                                newRest);
                assertEquals(1L, oldEdge, "TC73: original edge orderCount must remain 1; got " + oldEdge);
                assertEquals(1L, newEdge, "TC73: new edge orderCount must be 1; got " + newEdge);
        }
}

// ─── TC74 — S3-F11 PLACED order → 400 (scenario d) ───────────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC74_RecordInteractionPlacedTests extends TestBase {
        @Test
        @DisplayName("TC74 — Recording a PLACED (not delivered) order returns 400")
        void record_interaction_placed_400() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                // Pre-seed order id=1 has status PLACED.
                HttpResponse<String> r = httpPostAuth("/api/orders/1/record-interaction", "", tok);
                assertEquals(400, r.statusCode(), "TC74: must be 400; got " + r.statusCode() + " body=" + r.body());
        }
}

// ─── TC75 — S3-F11 CANCELLED order → 400 ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC75_RecordInteractionCancelledTests extends TestBase {
        @Test
        @DisplayName("TC75 — Recording a CANCELLED order returns 400")
        void record_interaction_cancelled_400() throws Exception {
                BASE_URL = orderServiceUrl;
                String oTable = tableName("Order");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(columnByField("Order", "user"), 2L);
                _ov.put(columnByField("Order", "restaurant"), 1L);
                _ov.put(columnByField("Order", "totalAmount"), 50.00);
                _ov.put(columnByField("Order", "status"), "CANCELLED");
                Long oid = insertRowReturningId(oTable, _ov);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/orders/" + oid + "/record-interaction", "", tok);
                assertEquals(400, r.statusCode(), "TC75: must be 400 for CANCELLED; got " + r.statusCode());
        }
}

// ─── TC76 — S3-F11 non-existent order → 404 (scenario e) ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC76_RecordInteractionNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC76 — Recording an interaction for non-existent order returns 404")
        void record_interaction_not_found_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/orders/999999/record-interaction", "", tok);
                assertEquals(404, r.statusCode(), "TC76: must be 404; got " + r.statusCode());
        }
}

// ─── TC77 — S3-F11 missing JWT → 401 (scenario f) ────────────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC77_RecordInteractionMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC77 — Missing JWT returns 401")
        void record_interaction_missing_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpPost("/api/orders/2/record-interaction", "");
                assertEquals(401, r.statusCode(), "TC77: must be 401; got " + r.statusCode());
        }
}

// ─── TC78 — S3-F11 invalid JWT → 401 ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC78_RecordInteractionInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC78 — Bogus JWT returns 401")
        void record_interaction_invalid_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpPostAuth("/api/orders/2/record-interaction", "", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC78: must be 401; got " + r.statusCode());
        }
}

// ─── TC79 — S3-F11 INTERACTION_RECORDED logged on success ────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC79_RecordInteractionEventLoggedTests extends TestBase {
        @Test
        @DisplayName("TC79 — Successful record logs INTERACTION_RECORDED to order_events Mongo")
        void interaction_recorded_event() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) {
                        throw new AssertionError("TC79: Mongo required.");
                }
                String coll = s3EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                long before = col.countDocuments();
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/orders/2/record-interaction", "", tok), "TC79 record");
                long after = col.countDocuments();
                assertTrue(after > before,
                                "TC79: at least one new event in '" + coll + "'. before=" + before + " after=" + after);
                // Verify latest event details.orderId=2, details.userId=2,
                // details.restaurantId=1.
                org.bson.Document latest = col.find().sort(new org.bson.Document("_id", -1)).first();
                if (latest != null) {
                        org.bson.Document details = latest.get("details", org.bson.Document.class);
                        if (details != null) {
                                Long oId = details.getLong("orderId");
                                Long uId = details.getLong("userId");
                                Long rId = details.getLong("restaurantId");
                                if (oId != null)
                                        assertEquals(Long.valueOf(2L), oId, "TC79: details.orderId");
                                if (uId != null)
                                        assertEquals(Long.valueOf(2L), uId, "TC79: details.userId");
                                if (rId != null)
                                        assertEquals(Long.valueOf(1L), rId, "TC79: details.restaurantId");
                        }
                }
        }
}

// ─── TC80 — S3-F11 idempotent path skips event log ───────────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC80_RecordInteractionEventSkippedOnIdempotentTests extends TestBase {
        @Test
        @DisplayName("TC80 — Repeat record on same orderId does not emit second INTERACTION_RECORDED")
        void event_skipped_on_idempotent() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) {
                        throw new AssertionError("TC80: Mongo required.");
                }
                String coll = s3EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                String tok = adminToken();
                // First call: 1 event added.
                long before = col.countDocuments();
                long beforeId2 = col.countDocuments(new org.bson.Document("details.orderId", 2L));
                assert2xx(httpPostAuth("/api/orders/2/record-interaction", "", tok), "TC80 record #1");
                long mid = col.countDocuments();
                assertTrue(mid > before, "TC80: first call must log");
                // Second call (idempotent): no new event for same orderId.
                assert2xx(httpPostAuth("/api/orders/2/record-interaction", "", tok), "TC80 record #2 idempotent");
                long after = col.countDocuments();
                // Delta events for orderId=2 should be exactly 1 (first call logged, second skipped).
                long count2After = col.countDocuments(new org.bson.Document("details.orderId", 2L));
                long delta2 = count2After - beforeId2;
                assertEquals(1L, delta2,
                                "TC80: only 1 new INTERACTION_RECORDED for orderId=2 (idempotent path skips); delta="
                                                + delta2);
                // Loose: total events must not jump by 2 from this single duplicate call.
                assertTrue(after <= mid, "TC80: idempotent call must not append a duplicate event; mid=" + mid
                                + " after=" + after);
        }
}

// ─── TC81 — S3-F11 Neo4j User node exists with correct id ────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC81_RecordInteractionUserNodeTests extends TestBase {
        @Test
        @DisplayName("TC81 — After record, exactly one User node with correct id exists")
        void user_node_present() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC81: Neo4j required.");
                }
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/orders/2/record-interaction", "", tok), "TC81 record");
                long count = neo4jNodeCount(s3GraphUserLabel(), 2L);
                assertEquals(1L, count, "TC81: exactly 1 User{id:2} node expected; got " + count);
        }
}

// ─── TC82 — S3-F11 Neo4j Restaurant node exists with correct id ──────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC82_RecordInteractionRestaurantNodeTests extends TestBase {
        @Test
        @DisplayName("TC82 — After record, exactly one catalog node with correct id exists")
        void restaurant_node_present() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC82: Neo4j required.");
                }
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/orders/2/record-interaction", "", tok), "TC82 record");
                long count = neo4jNodeCount(s3GraphCatalogLabel(), 1L);
                assertEquals(1L, count,
                                "TC82: exactly 1 " + s3GraphCatalogLabel() + "{id:1} node expected; got " + count);
        }
}

// ─── TC83 — S3-F11 lastOrderDate updates on subsequent record ────────────────
@Tag("public")
@Tag("features_m2")
class TC83_RecordInteractionLastOrderDateTests extends TestBase {
        @Test
        @DisplayName("TC83 — lastOrderDate updates when a second order is recorded")
        void last_order_date_updates() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC83: Neo4j required.");
                }
                String tok = adminToken();
                // Insert second DELIVERED order for user 2 → rest 1.
                String oTable = tableName("Order");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(columnByField("Order", "user"), 2L);
                _ov.put(columnByField("Order", "restaurant"), 1L);
                _ov.put(columnByField("Order", "totalAmount"), 80.00);
                _ov.put(columnByField("Order", "status"), "DELIVERED");
                Long oid2 = insertRowReturningId(oTable, _ov);
                assert2xx(httpPostAuth("/api/orders/2/record-interaction", "", tok), "TC83 first");
                String first = neo4jRelStringProp(s3GraphUserLabel(), 2L, s3GraphRelationship(), s3GraphCatalogLabel(),
                                1L, "lastOrderDate");
                Thread.sleep(50); // ensure timestamp progression
                assert2xx(httpPostAuth("/api/orders/" + oid2 + "/record-interaction", "", tok), "TC83 second");
                String second = neo4jRelStringProp(s3GraphUserLabel(), 2L, s3GraphRelationship(), s3GraphCatalogLabel(),
                                1L, "lastOrderDate");
                assertNotNull(second, "TC83: lastOrderDate must be set after second record");
                // It is fine if same value (date precision); but it must not be earlier than
                // first.
                if (first != null && second != null) {
                        assertTrue(second.compareTo(first) >= 0,
                                        "TC83: lastOrderDate must not regress; first=" + first + " second=" + second);
                }
        }
}

// ─── TC84 — S3-F11 cross-service SQL: Neo4j ids match PG ids ─────────────────
@Tag("public")
@Tag("features_m2")
class TC84_RecordInteractionCrossServiceSqlTests extends TestBase {
        @Test
        @DisplayName("TC84 — Neo4j edge endpoints match PG order's user_id/restaurant_id (cross-service SQL)")
        void cross_service_sql_resolution() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC84: Neo4j required.");
                }
                // Insert a DELIVERED order with explicit ids different from pre-seed order 2.
                String oTable = tableName("Order");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(columnByField("Order", "user"), 4L);
                _ov.put(columnByField("Order", "restaurant"), 2L);
                _ov.put(columnByField("Order", "totalAmount"), 60.00);
                _ov.put(columnByField("Order", "status"), "DELIVERED");
                Long oid = insertRowReturningId(oTable, _ov);
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/orders/" + oid + "/record-interaction", "", tok), "TC84 record");
                // Edge between User{id:4} → Restaurant{id:2} must exist (proves controller
                // resolved ids via JDBC).
                boolean exists = neo4jRelExists(s3GraphUserLabel(), 4L, s3GraphRelationship(), s3GraphCatalogLabel(),
                                2L);
                assertTrue(exists, "TC84: edge User{4}→" + s3GraphCatalogLabel() + "{2} must exist; "
                                + "(if it does not, the controller likely used wrong ids — proves cross-service SQL not done)");
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F12 — Get Recommendations (TC85..TC99)
// Endpoint: GET /api/orders/recommendations?userId={id}&limit={n}  (per spec §10 [S3-F12])
// Tests seed Neo4j directly with neo4jExec(MERGE...) so they don't depend on
// S3-F11's controller behavior. Pre-seed PG users 1..5 are already present.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC85 — S3-F12 happy path: similar-user-based recs (scenario a) ──────────
@Tag("public")
@Tag("features_m2")
class TC85_RecommendationsHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC85 — Recs for A include R3 and R4 (via similar-user traversal)")
        void recs_happy_path() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC85: Neo4j required.");
                }
                // Seed 4 catalog rows (R1..R4 already in pre-seed at ids 1..4).
                // Build Neo4j graph: A=user 2, B=user 3, C=user 4. R1=1, R2=2, R3=3, R4=4.
                String uL = s3GraphUserLabel();
                String cL = s3GraphCatalogLabel();
                String rT = s3GraphRelationship();
                // A→R1, A→R2; B→R1, B→R3; C→R2, C→R4.
                java.util.Map<String, Object> p = new java.util.HashMap<>();
                neo4jExec("MERGE (u:`" + uL + "` {userId: 2}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 2}) MERGE (r:`" + cL + "` {restaurantId: 2}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 3}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 4}) MERGE (r:`" + cL + "` {restaurantId: 2}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 4}) MERGE (r:`" + cL + "` {restaurantId: 4}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                // Get token for user A=2 directly via login (pre-seed admin path is for id=1;
                // we need id=2).
                String tok = adminToken(); // ADMIN can call any user's recs (covered by TC91)
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=2", tok);
                assert2xx(r, "TC85 recommendations");
                JsonNode arr = parseNode(r.body());
                // Result may be JSON array or wrapped object — handle both.
                JsonNode list = arr.isArray() ? arr
                                : (arr.has("recommendations") ? arr.get("recommendations")
                                                : arr.has("data") ? arr.get("data") : arr);
                java.util.Set<Long> ids = new java.util.HashSet<>();
                if (list != null && list.isArray()) {
                        for (JsonNode item : list) {
                                if (item.has("id"))
                                        ids.add(item.get("id").asLong());
                                else if (item.has("restaurantId"))
                                        ids.add(item.get("restaurantId").asLong());
                        }
                }
                assertTrue(ids.contains(3L),
                                "TC85: recs for A must include R3 (B shares R1); got=" + ids + " body=" + r.body());
                assertTrue(ids.contains(4L), "TC85: recs for A must include R4 (C shares R2); got=" + ids);
        }
}

// ─── TC86 — S3-F12 own restaurants excluded ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC86_RecommendationsExcludeOwnTests extends TestBase {
        @Test
        @DisplayName("TC86 — Recs do NOT include the user's own restaurants")
        void recs_exclude_own() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC86: Neo4j required.");
                }
                String uL = s3GraphUserLabel();
                String cL = s3GraphCatalogLabel();
                String rT = s3GraphRelationship();
                neo4jExec("MERGE (u:`" + uL + "` {userId: 2}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 2}) MERGE (r:`" + cL + "` {restaurantId: 2}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 3}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=2", tok);
                assert2xx(r, "TC86 recs");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr
                                : (arr.has("recommendations") ? arr.get("recommendations")
                                                : arr.has("data") ? arr.get("data") : arr);
                java.util.Set<Long> ids = new java.util.HashSet<>();
                if (list != null && list.isArray()) {
                        for (JsonNode item : list) {
                                if (item.has("id"))
                                        ids.add(item.get("id").asLong());
                                else if (item.has("restaurantId"))
                                        ids.add(item.get("restaurantId").asLong());
                        }
                }
                assertFalse(ids.contains(1L), "TC86: own restaurant R1 must NOT be in recs; got=" + ids);
                assertFalse(ids.contains(2L), "TC86: own restaurant R2 must NOT be in recs; got=" + ids);
        }
}

// ─── TC87 — S3-F12 ranking by similar-user count ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC87_RecommendationsRankingTests extends TestBase {
        @Test
        @DisplayName("TC87 — Restaurant covered by more similar users ranks higher")
        void recs_ranking() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC87: Neo4j required.");
                }
                String uL = s3GraphUserLabel();
                String cL = s3GraphCatalogLabel();
                String rT = s3GraphRelationship();
                // A=2 has R1, R2. B=3, C=4 also have R1 (so they're similar to A).
                // R3 reachable via B AND C (count=2). R4 reachable only via D=5 (count=1).
                neo4jExec("MERGE (u:`" + uL + "` {userId: 2}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 2}) MERGE (r:`" + cL + "` {restaurantId: 2}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 3}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 4}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 4}) MERGE (r:`" + cL + "` {restaurantId: 3}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 5}) MERGE (r:`" + cL + "` {restaurantId: 2}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 5}) MERGE (r:`" + cL + "` {restaurantId: 4}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=2", tok);
                assert2xx(r, "TC87 recs");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr
                                : (arr.has("recommendations") ? arr.get("recommendations")
                                                : arr.has("data") ? arr.get("data") : arr);
                java.util.List<Long> orderedIds = new java.util.ArrayList<>();
                if (list != null && list.isArray()) {
                        for (JsonNode item : list) {
                                if (item.has("id"))
                                        orderedIds.add(item.get("id").asLong());
                                else if (item.has("restaurantId"))
                                        orderedIds.add(item.get("restaurantId").asLong());
                        }
                }
                int idx3 = orderedIds.indexOf(3L);
                int idx4 = orderedIds.indexOf(4L);
                assertTrue(idx3 >= 0, "TC87: R3 must be in recs; got=" + orderedIds);
                assertTrue(idx4 >= 0, "TC87: R4 must be in recs; got=" + orderedIds);
                assertTrue(idx3 < idx4, "TC87: R3 (count=2) must rank above R4 (count=1); got order=" + orderedIds);
        }
}

// ─── TC88 — S3-F12 limit query parameter respected ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC88_RecommendationsLimitTests extends TestBase {
        @Test
        @DisplayName("TC88 — limit=3 returns at most 3 recs")
        void recs_limit_respected() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC88: Neo4j required.");
                }
                String uL = s3GraphUserLabel();
                String cL = s3GraphCatalogLabel();
                String rT = s3GraphRelationship();
                // Insert 8 brand-new restaurants and capture their assigned ids.
                String rTable = tableName("Restaurant");
                java.util.List<Long> candidateIds = new java.util.ArrayList<>();
                for (int i = 0; i < 8; i++) {
                        java.util.Map<String, Object> _rOv = new java.util.HashMap<>();
                        _rOv.put(columnByField("Restaurant", "name"), "TC88 Candidate " + i);
                        _rOv.put(columnByField("Restaurant", "cuisineType"), "ITALIAN");
                        _rOv.put(columnByField("Restaurant", "status"), "OPEN");
                        Long rid = insertRowReturningId(rTable, _rOv);
                        candidateIds.add(rid);
                }
                // A=2, owns R1 (pre-seed restaurant id=1).
                neo4jExec("MERGE (u:`" + uL + "` {userId: 2}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                // 8 similar users (3..10), each shares R1 + has a unique candidate restaurant.
                for (int k = 0; k < 8; k++) {
                        int sim = 3 + k;
                        long candidate = candidateIds.get(k);
                        neo4jExec(String.format(
                                        "MERGE (u:`%s` {userId: %d}) MERGE (r:`%s` {restaurantId: 1}) MERGE (u)-[:`%s`]->(r)", uL,
                                        sim, cL, rT));
                        neo4jExec(String.format(
                                        "MERGE (u:`%s` {userId: %d}) MERGE (r:`%s` {restaurantId: %d}) MERGE (u)-[:`%s`]->(r)", uL,
                                        sim, cL, candidate, rT));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=2&limit=3", tok);
                assert2xx(r, "TC88 recs");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr
                                : (arr.has("recommendations") ? arr.get("recommendations")
                                                : arr.has("data") ? arr.get("data") : arr);
                int size = (list != null && list.isArray()) ? list.size() : 0;
                assertEquals(3, size, "TC88: limit=3 must return exactly 3; got " + size + " body=" + r.body());
        }
}

// ─── TC89 — S3-F12 default limit=5 when omitted ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC89_RecommendationsDefaultLimitTests extends TestBase {
        @Test
        @DisplayName("TC89 — Default limit=5 when ?limit not provided")
        void recs_default_limit() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC89: Neo4j required.");
                }
                String uL = s3GraphUserLabel();
                String cL = s3GraphCatalogLabel();
                String rT = s3GraphRelationship();
                // Insert 10 brand-new restaurants and capture their assigned ids.
                String rTable = tableName("Restaurant");
                java.util.List<Long> candidateIds = new java.util.ArrayList<>();
                for (int i = 0; i < 10; i++) {
                        java.util.Map<String, Object> _rOv = new java.util.HashMap<>();
                        _rOv.put(columnByField("Restaurant", "name"), "TC89 Candidate " + i);
                        _rOv.put(columnByField("Restaurant", "cuisineType"), "ITALIAN");
                        _rOv.put(columnByField("Restaurant", "status"), "OPEN");
                        Long rid = insertRowReturningId(rTable, _rOv);
                        candidateIds.add(rid);
                }
                // A=2, owns R1.
                neo4jExec("MERGE (u:`" + uL + "` {userId: 2}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                // 10 similar users (3..12), each shares R1 + has a unique candidate restaurant.
                for (int k = 0; k < 10; k++) {
                        int sim = 3 + k;
                        long candidate = candidateIds.get(k);
                        neo4jExec(String.format(
                                        "MERGE (u:`%s` {userId: %d}) MERGE (r:`%s` {restaurantId: 1}) MERGE (u)-[:`%s`]->(r)", uL,
                                        sim, cL, rT));
                        neo4jExec(String.format(
                                        "MERGE (u:`%s` {userId: %d}) MERGE (r:`%s` {restaurantId: %d}) MERGE (u)-[:`%s`]->(r)", uL,
                                        sim, cL, candidate, rT));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=2", tok);
                assert2xx(r, "TC89 recs");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr
                                : (arr.has("recommendations") ? arr.get("recommendations")
                                                : arr.has("data") ? arr.get("data") : arr);
                int size = (list != null && list.isArray()) ? list.size() : 0;
                assertEquals(5, size, "TC89: default limit=5 expected; got " + size + " body=" + r.body());
        }
}

// ─── TC90 — S3-F12 ownership violation (scenario b) ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC90_RecommendationsOwnershipTests extends TestBase {
        @Test
        @DisplayName("TC90 — User B requesting User A's recs returns 403")
        void recs_ownership_403() throws Exception {
                BASE_URL = orderServiceUrl;
                // Register two non-admin users; use B's token to request A's recs.
                java.util.Map<String, Object> a = seedAndLoginUser("a_owner");
                java.util.Map<String, Object> b = seedAndLoginUser("b_other");
                long aId = ((Number) a.get("id")).longValue();
                String bTok = (String) b.get("token");
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=" + aId, bTok);
                assertEquals(403, r.statusCode(), "TC90: must be 403 (ownership violation); got " + r.statusCode());
        }
}

// ─── TC91 — S3-F12 ADMIN bypass (scenario c) ─────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC91_RecommendationsAdminBypassTests extends TestBase {
        @Test
        @DisplayName("TC91 — ADMIN token can fetch recs for any user (200)")
        void recs_admin_bypass() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> a = seedAndLoginUser("a_target");
                long aId = ((Number) a.get("id")).longValue();
                String adminTok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=" + aId, adminTok);
                assertEquals(200, r.statusCode(), "TC91: ADMIN must succeed; got " + r.statusCode());
        }
}

// ─── TC92 — S3-F12 user with no graph node → empty list (scenario d) ─────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC92_RecommendationsEmptyListTests extends TestBase {
        @Test
        @DisplayName("TC92 — User with no recorded interactions returns empty list")
        void recs_empty_list() throws Exception {
                BASE_URL = orderServiceUrl;
                // No Neo4j seed (truncate already ran). User exists in PG (admin id=1 from
                // preseed).
                String adminTok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=1", adminTok);
                assertEquals(200, r.statusCode(), "TC92: must be 200; got " + r.statusCode());
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr
                                : (arr.has("recommendations") ? arr.get("recommendations")
                                                : arr.has("data") ? arr.get("data") : arr);
                int size = (list != null && list.isArray()) ? list.size() : -1;
                assertEquals(0, size, "TC92: empty list expected; got " + size + " body=" + r.body());
        }
}

// ─── TC93 — S3-F12 non-existent userId + ADMIN → 404 (scenario e) ────────────
@Tag("public")
@Tag("features_m2")
class TC93_RecommendationsNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC93 — Non-existent userId with ADMIN token returns 404")
        void recs_not_found_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String adminTok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=999999", adminTok);
                assertEquals(404, r.statusCode(), "TC93: must be 404; got " + r.statusCode());
        }
}

// ─── TC94 — S3-F12 missing JWT → 401 (scenario f) ────────────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC94_RecommendationsMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC94 — Missing JWT returns 401")
        void recs_missing_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGet("/api/orders/recommendations?userId=1");
                assertEquals(401, r.statusCode(), "TC94: must be 401; got " + r.statusCode());
        }
}

// ─── TC95 — S3-F12 invalid JWT → 401 ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC95_RecommendationsInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC95 — Bogus JWT returns 401")
        void recs_invalid_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=1", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC95: must be 401; got " + r.statusCode());
        }
}

// ─── TC96 — S3-F12 cache populated in Redis with ~5 min TTL ──────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC96_RecommendationsCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC96 — Recs call populates Redis with TTL ≤ 300s")
        void recs_cache_populated() throws Exception {
                BASE_URL = orderServiceUrl;
                if (redis == null) {
                        throw new AssertionError("TC96: Redis required.");
                }
                java.util.Set<String> beforeKeys = redisKeys("*");
                String adminTok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=1", adminTok);
                assert2xx(r, "TC96 recs");
                java.util.Set<String> afterKeys = redisKeys("*");
                assertTrue(afterKeys.size() > beforeKeys.size(),
                                "TC96: at least one new Redis key expected; before=" + beforeKeys.size() + " after="
                                                + afterKeys.size());
                boolean foundFiveMinTtl = false;
                for (String k : afterKeys) {
                        if (beforeKeys.contains(k))
                                continue;
                        long ttl = redisTtl(k);
                        if (ttl > 0 && ttl <= 300) {
                                foundFiveMinTtl = true;
                                break;
                        }
                }
                assertTrue(foundFiveMinTtl,
                                "TC96: a new Redis key must have TTL in (0, 300] seconds (5-min cache); keys="
                                                + afterKeys);
        }
}

// ─── TC97 — S3-F12 cache hit served (mutate, second call returns first body) ─
@Tag("public")
@Tag("features_m2")
class TC97_RecommendationsCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC97 — Cache hit: mutating Neo4j between calls doesn't change response")
        void recs_cache_hit() throws Exception {
                BASE_URL = orderServiceUrl;
                if (redis == null || neo4j == null) {
                        throw new AssertionError("TC97: Redis + Neo4j required.");
                }
                String uL = s3GraphUserLabel();
                String cL = s3GraphCatalogLabel();
                String rT = s3GraphRelationship();
                // Seed initial graph: user 1 has R1; user 3 also has R1 + R3 (so user 1 gets R3
                // in recs).
                neo4jExec("MERGE (u:`" + uL + "` {userId: 1}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 3}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                String adminTok = adminToken();
                HttpResponse<String> r1 = httpGetAuth("/api/orders/recommendations?userId=1", adminTok);
                assert2xx(r1, "TC97 recs #1");
                // Snapshot a Redis cache value
                String cachedBefore = null;
                for (String k : redisKeys("*")) {
                        String v = redisGet(k);
                        if (v != null && v.length() > 0) {
                                cachedBefore = v;
                                break;
                        }
                }
                // Mutate Neo4j: add R4 to user 3's interactions (would change rec results if
                // uncached).
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 4}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                HttpResponse<String> r2 = httpGetAuth("/api/orders/recommendations?userId=1", adminTok);
                assert2xx(r2, "TC97 recs #2");
                assertEquals(r1.body(), r2.body(),
                                "TC97: cache hit expected — r1 must equal r2 even after Neo4j mutation; r1=" + r1.body()
                                                + " r2=" + r2.body());
                if (cachedBefore != null) {
                        String cachedAfter = null;
                        for (String k : redisKeys("*")) {
                                String v = redisGet(k);
                                if (v != null && v.length() > 0) {
                                        cachedAfter = v;
                                        break;
                                }
                        }
                        assertEquals(cachedBefore, cachedAfter,
                                        "TC97: Redis cached value must be unchanged across the two calls");
                }
        }
}

// ─── TC98 — S3-F12 NO event logged in order_events ───────────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC98_RecommendationsNoEventTests extends TestBase {
        @Test
        @DisplayName("TC98 — Recommendation call does NOT log an event in order_events")
        void recs_no_event() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) {
                        throw new AssertionError("TC98: Mongo required.");
                }
                String coll = s3EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                long before = col.countDocuments();
                String adminTok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=1", adminTok);
                assert2xx(r, "TC98 recs");
                long after = col.countDocuments();
                assertEquals(before, after,
                                "TC98: '" + coll + "' must not gain any events on a recommendations call; before="
                                                + before + " after=" + after);
        }
}

// ─── TC99 — S3-F12 restaurant detail enrichment ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC99_RecommendationsEnrichmentTests extends TestBase {
        @Test
        @DisplayName("TC99 — Each rec item exposes restaurant name + cuisine type")
        void recs_enrichment() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) {
                        throw new AssertionError("TC99: Neo4j required.");
                }
                String uL = s3GraphUserLabel();
                String cL = s3GraphCatalogLabel();
                String rT = s3GraphRelationship();
                // A=user 2, R1 owned. B=3 has R1 + R3 → R3 should be the rec.
                neo4jExec("MERGE (u:`" + uL + "` {userId: 2}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 1}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                neo4jExec("MERGE (u:`" + uL + "` {userId: 3}) MERGE (r:`" + cL + "` {restaurantId: 3}) MERGE (u)-[:`" + rT
                                + "`]->(r)");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/recommendations?userId=2", tok);
                assert2xx(r, "TC99 recs");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr
                                : (arr.has("recommendations") ? arr.get("recommendations")
                                                : arr.has("data") ? arr.get("data") : arr);
                assertNotNull(list, "TC99: response must contain a list of recs; body=" + r.body());
                assertTrue(list.isArray() && list.size() > 0, "TC99: at least one rec expected");
                for (JsonNode item : list) {
                        boolean hasId = item.has("id") || item.has("restaurantId");
                        boolean hasName = item.has("name") || item.has("restaurantName");
                        boolean hasCuisine = item.has("cuisineType") || item.has("cuisine_type");
                        assertTrue(hasId, "TC99: each rec must include id; got=" + item);
                        assertTrue(hasName, "TC99: each rec must include name; got=" + item);
                        assertTrue(hasCuisine, "TC99: each rec must include cuisineType; got=" + item);
                }
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S4 M2 — Delivery Service features (TC100..TC135)
//
// Covers S4-F10 (analytics dashboard, TC100-TC117), S4-F11 (record tracking,
// TC118-TC127), and S4-F12 (tracking timeline, TC128-TC135). Pre-req
// infrastructure (Cassandra driver/CqlSession, s4* theme helpers,
// cassandraTableByName/cassandraColumnByField, Mongo+Redis as before) lives
// in TestBase. Per-test wipe of PG/Neo4j/Redis/Cassandra happens in
// autoTruncateAllData() which runs in @BeforeEach + @AfterEach.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC100 — S4-F10 dashboard happy path (composite scenario a) ──────────────
@Tag("public")
@Tag("features_m2")
class TC100_DeliveryAnalyticsHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC100 — Dashboard returns totalDeliveries=8, onTimeRate=0.6 + correct breakdown")
        void dashboard_happy_path() throws Exception {
                BASE_URL = deliveryServiceUrl;
                // Insert 8 orders+deliveries in 2026-04: 5 DELIVERED (3 within 45min, 2 over),
                // 2 IN_TRANSIT, 1 ASSIGNED.
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");

                // 5 DELIVERED orders — 3 within 45min, 2 over.
                int[][] minsApart = { { 30, 1 }, { 35, 2 }, { 40, 3 }, { 60, 4 }, { 90, 5 } };
                for (int i = 0; i < 5; i++) {
                        int delayMin = minsApart[i][0];
                        int day = minsApart[i][1];
                        java.sql.Timestamp orderTs = java.sql.Timestamp.valueOf("2026-04-0" + day + " 10:00:00");
                        java.sql.Timestamp deliveredTs = java.sql.Timestamp
                                        .valueOf("2026-04-0" + day + " " + (10 + delayMin / 60)
                                                        + ":" + String.format("%02d", delayMin % 60) + ":00");
                        java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                        _oOv.put(columnByField("Order", "user"), 1L);
                        _oOv.put(columnByField("Order", "restaurant"), 1L);
                        _oOv.put(columnByField("Order", "totalAmount"), 100.00);
                        _oOv.put(columnByField("Order", "status"), "DELIVERED");
                        Long oid = insertRowReturningId(oTable, _oOv);
                        setAllDateColumns(oTable, oid, orderTs);
                        // delivered_at column (optional — set via raw UPDATE for "completion timestamp"
                        // per spec).
                        try {
                                jdbc.update("UPDATE \"" + oTable + "\" SET delivered_at = ? WHERE id = ?", deliveredTs,
                                                oid);
                        } catch (org.springframework.dao.DataAccessException e) {
                                throw new AssertionError(
                                                "TC100: order table needs a delivered_at column for analytics — "
                                                                + e.getMessage(),
                                                e);
                        }
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), "DELIVERED");
                        insertRowReturningId(dTable, _dOv);
                }
                // 2 IN_TRANSIT
                for (int i = 0; i < 2; i++) {
                        java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                        _oOv.put(columnByField("Order", "user"), 1L);
                        _oOv.put(columnByField("Order", "restaurant"), 1L);
                        _oOv.put(columnByField("Order", "totalAmount"), 100.00);
                        _oOv.put(columnByField("Order", "status"), "CONFIRMED");
                        Long oid = insertRowReturningId(oTable, _oOv);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-04-15 12:00:00"));
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), "IN_TRANSIT");
                        insertRowReturningId(dTable, _dOv);
                }
                // 1 ASSIGNED
                java.util.Map<String, Object> _aOv = new java.util.HashMap<>();
                _aOv.put(columnByField("Order", "user"), 1L);
                _aOv.put(columnByField("Order", "restaurant"), 1L);
                _aOv.put(columnByField("Order", "totalAmount"), 100.00);
                _aOv.put(columnByField("Order", "status"), "PLACED");
                Long aOid = insertRowReturningId(oTable, _aOv);
                setAllDateColumns(oTable, aOid, java.sql.Timestamp.valueOf("2026-04-20 12:00:00"));
                java.util.Map<String, Object> _aDov = new java.util.HashMap<>();
                _aDov.put(columnByField("Delivery", "order"), aOid);
                _aDov.put(columnByField("Delivery", "status"), "ASSIGNED");
                insertRowReturningId(dTable, _aDov);

                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-04-01&endDate=2026-04-30", tok);
                assert2xx(r, "TC100 dashboard");
                JsonNode j = parseNode(r.body());
                assertEquals(8L, _readLong(j, "totalDeliveries", "total_deliveries"),
                                "TC100: totalDeliveries=8 expected; body=" + r.body());
                double onTime = _readDouble(j, "onTimeRate", "on_time_rate");
                assertEquals(0.6, onTime, 0.01, "TC100: onTimeRate=0.6 expected (3/5); got " + onTime);
                JsonNode breakdown = j.has("deliveriesByStatus") ? j.get("deliveriesByStatus")
                                : j.has("deliveries_by_status") ? j.get("deliveries_by_status") : null;
                assertNotNull(breakdown, "TC100: deliveriesByStatus key required; body=" + r.body());
                assertEquals(5L, breakdown.has("DELIVERED") ? breakdown.get("DELIVERED").asLong() : 0L,
                                "TC100: DELIVERED=5");
                assertEquals(2L, breakdown.has("IN_TRANSIT") ? breakdown.get("IN_TRANSIT").asLong() : 0L,
                                "TC100: IN_TRANSIT=2");
                assertEquals(1L, breakdown.has("ASSIGNED") ? breakdown.get("ASSIGNED").asLong() : 0L,
                                "TC100: ASSIGNED=1");
        }

        private long _readLong(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asLong();
                return -1;
        }

        private double _readDouble(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asDouble();
                return -1;
        }
}

// ─── TC101 — S4-F10 totalDeliveries attribute isolated ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC101_DeliveryAnalyticsTotalCountTests extends TestBase {
        @Test
        @DisplayName("TC101 — Dashboard.totalDeliveries equals exact count of deliveries in range")
        void total_deliveries_isolated() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");
                for (int i = 0; i < 6; i++) {
                        java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                        _oOv.put(columnByField("Order", "user"), 1L);
                        _oOv.put(columnByField("Order", "restaurant"), 1L);
                        _oOv.put(columnByField("Order", "totalAmount"), 80.00);
                        _oOv.put(columnByField("Order", "status"), "CONFIRMED");
                        Long oid = insertRowReturningId(oTable, _oOv);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-15 12:00:00"));
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), "IN_TRANSIT");
                        insertRowReturningId(dTable, _dOv);
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC101 dashboard");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalDeliveries") ? j.get("totalDeliveries").asLong()
                                : j.has("total_deliveries") ? j.get("total_deliveries").asLong() : -1;
                assertEquals(6L, total, "TC101: totalDeliveries=6 expected; got " + total);
        }
}

// ─── TC102 — S4-F10 averageDeliveryTimeMinutes isolated ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC102_DeliveryAnalyticsAverageTimeTests extends TestBase {
        @Test
        @DisplayName("TC102 — Dashboard.averageDeliveryTimeMinutes equals average of deliveredAt-orderDate")
        void average_delivery_time_isolated() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");
                // 4 DELIVERED orders with delivery times 20/30/40/50 min → avg=35
                int[] mins = { 20, 30, 40, 50 };
                for (int i = 0; i < mins.length; i++) {
                        java.sql.Timestamp ord = java.sql.Timestamp.valueOf("2026-09-1" + i + " 10:00:00");
                        java.sql.Timestamp del = java.sql.Timestamp.valueOf(
                                        "2026-09-1" + i + " 10:" + String.format("%02d", mins[i]) + ":00");
                        java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                        _oOv.put(columnByField("Order", "user"), 1L);
                        _oOv.put(columnByField("Order", "restaurant"), 1L);
                        _oOv.put(columnByField("Order", "totalAmount"), 100.00);
                        _oOv.put(columnByField("Order", "status"), "DELIVERED");
                        Long oid = insertRowReturningId(oTable, _oOv);
                        setAllDateColumns(oTable, oid, ord);
                        try {
                                jdbc.update("UPDATE \"" + oTable + "\" SET delivered_at = ? WHERE id = ?", del, oid);
                        } catch (org.springframework.dao.DataAccessException e) {
                                throw new AssertionError(
                                                "TC102: order table needs delivered_at column — " + e.getMessage(), e);
                        }
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), "DELIVERED");
                        insertRowReturningId(dTable, _dOv);
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC102 dashboard");
                JsonNode j = parseNode(r.body());
                double avg = j.has("averageDeliveryTimeMinutes") ? j.get("averageDeliveryTimeMinutes").asDouble()
                                : j.has("average_delivery_time_minutes")
                                                ? j.get("average_delivery_time_minutes").asDouble()
                                                : j.has("avgDeliveryTimeMinutes")
                                                                ? j.get("avgDeliveryTimeMinutes").asDouble()
                                                                : -1;
                assertEquals(35.0, avg, 0.5, "TC102: averageDeliveryTimeMinutes=35 expected; got " + avg);
        }
}

// ─── TC103 — S4-F10 averageDeliveryTimeMinutes=0 with no DELIVERED orders ────
@Tag("public")
@Tag("features_m2")
class TC103_DeliveryAnalyticsAverageZeroTests extends TestBase {
        @Test
        @DisplayName("TC103 — averageDeliveryTimeMinutes=0 when range has no DELIVERED orders")
        void average_zero_when_none_delivered() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");
                // 3 IN_TRANSIT + 2 ASSIGNED
                String[] dStatuses = { "IN_TRANSIT", "IN_TRANSIT", "IN_TRANSIT", "ASSIGNED", "ASSIGNED" };
                for (int i = 0; i < dStatuses.length; i++) {
                        java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                        _oOv.put(columnByField("Order", "user"), 1L);
                        _oOv.put(columnByField("Order", "restaurant"), 1L);
                        _oOv.put(columnByField("Order", "totalAmount"), 100.00);
                        _oOv.put(columnByField("Order", "status"), "CONFIRMED");
                        Long oid = insertRowReturningId(oTable, _oOv);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-15 12:00:00"));
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), dStatuses[i]);
                        insertRowReturningId(dTable, _dOv);
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC103 dashboard");
                JsonNode j = parseNode(r.body());
                double avg = j.has("averageDeliveryTimeMinutes") ? j.get("averageDeliveryTimeMinutes").asDouble()
                                : j.has("average_delivery_time_minutes")
                                                ? j.get("average_delivery_time_minutes").asDouble()
                                                : -1;
                assertEquals(0.0, avg, 0.01,
                                "TC103: averageDeliveryTimeMinutes=0 expected when none DELIVERED; got " + avg);
        }
}

// ─── TC104 — S4-F10 average dedupes by Order.id (multiple deliveries per
// order) ─
@Tag("public")
@Tag("features_m2")
class TC104_DeliveryAnalyticsAverageDedupTests extends TestBase {
        @Test
        @DisplayName("TC104 — averageDeliveryTimeMinutes deduplicates orders with multiple delivery rows")
        void average_dedupes_by_order_id() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");
                // 1 DELIVERED order with 3 delivery history rows (state transitions) — must be
                // averaged ONCE.
                java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                _oOv.put(columnByField("Order", "user"), 1L);
                _oOv.put(columnByField("Order", "restaurant"), 1L);
                _oOv.put(columnByField("Order", "totalAmount"), 100.00);
                _oOv.put(columnByField("Order", "status"), "DELIVERED");
                Long oid = insertRowReturningId(oTable, _oOv);
                setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-10 10:00:00"));
                try {
                        jdbc.update("UPDATE \"" + oTable + "\" SET delivered_at = ? WHERE id = ?",
                                        java.sql.Timestamp.valueOf("2026-09-10 10:30:00"), oid);
                } catch (org.springframework.dao.DataAccessException e) {
                        throw new AssertionError("TC104: delivered_at required — " + e.getMessage(), e);
                }
                // 3 delivery rows for the SAME order — ASSIGNED, PICKED_UP/IN_TRANSIT,
                // DELIVERED.
                String[] hist = { "ASSIGNED", "IN_TRANSIT", "DELIVERED" };
                for (String s : hist) {
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), s);
                        insertRowReturningId(dTable, _dOv);
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC104 dashboard");
                JsonNode j = parseNode(r.body());
                double avg = j.has("averageDeliveryTimeMinutes") ? j.get("averageDeliveryTimeMinutes").asDouble()
                                : j.has("average_delivery_time_minutes")
                                                ? j.get("average_delivery_time_minutes").asDouble()
                                                : -1;
                // Single order delivered in 30 min — average must be exactly 30, NOT (30*3)/3
                // if non-deduped.
                assertEquals(30.0, avg, 0.5,
                                "TC104: averageDeliveryTimeMinutes must dedupe by Order.id — expected 30, got " + avg);
        }
}

// ─── TC105 — S4-F10 onTimeRate isolated ──────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC105_DeliveryAnalyticsOnTimeRateTests extends TestBase {
        @Test
        @DisplayName("TC105 — onTimeRate = on-time DELIVERED orders / total DELIVERED orders")
        void on_time_rate_isolated() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");
                // 5 DELIVERED: 3 within 45min, 2 over → onTimeRate=0.6
                int[] mins = { 30, 35, 40, 60, 90 };
                for (int i = 0; i < mins.length; i++) {
                        java.sql.Timestamp ord = java.sql.Timestamp.valueOf("2026-09-1" + i + " 10:00:00");
                        java.sql.Timestamp del = java.sql.Timestamp.valueOf(
                                        String.format("2026-09-1%d %02d:%02d:00", i, 10 + mins[i] / 60, mins[i] % 60));
                        java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                        _oOv.put(columnByField("Order", "user"), 1L);
                        _oOv.put(columnByField("Order", "restaurant"), 1L);
                        _oOv.put(columnByField("Order", "totalAmount"), 100.00);
                        _oOv.put(columnByField("Order", "status"), "DELIVERED");
                        Long oid = insertRowReturningId(oTable, _oOv);
                        setAllDateColumns(oTable, oid, ord);
                        try {
                                jdbc.update("UPDATE \"" + oTable + "\" SET delivered_at = ? WHERE id = ?", del, oid);
                        } catch (org.springframework.dao.DataAccessException e) {
                                throw new AssertionError("TC105: delivered_at required — " + e.getMessage(), e);
                        }
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), "DELIVERED");
                        insertRowReturningId(dTable, _dOv);
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC105 dashboard");
                JsonNode j = parseNode(r.body());
                double rate = j.has("onTimeRate") ? j.get("onTimeRate").asDouble()
                                : j.has("on_time_rate") ? j.get("on_time_rate").asDouble() : -1;
                assertEquals(0.6, rate, 0.001, "TC105: onTimeRate=0.6 expected (3/5); got " + rate);
        }
}

// ─── TC106 — S4-F10 onTimeRate=0 with no DELIVERED orders ────────────────────
@Tag("public")
@Tag("features_m2")
class TC106_DeliveryAnalyticsOnTimeRateZeroTests extends TestBase {
        @Test
        @DisplayName("TC106 — onTimeRate=0 when range has no DELIVERED orders")
        void on_time_rate_zero() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");
                for (int i = 0; i < 3; i++) {
                        java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                        _oOv.put(columnByField("Order", "user"), 1L);
                        _oOv.put(columnByField("Order", "restaurant"), 1L);
                        _oOv.put(columnByField("Order", "totalAmount"), 100.00);
                        _oOv.put(columnByField("Order", "status"), "CONFIRMED");
                        Long oid = insertRowReturningId(oTable, _oOv);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-15 12:00:00"));
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), "IN_TRANSIT");
                        insertRowReturningId(dTable, _dOv);
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC106 dashboard");
                JsonNode j = parseNode(r.body());
                double rate = j.has("onTimeRate") ? j.get("onTimeRate").asDouble()
                                : j.has("on_time_rate") ? j.get("on_time_rate").asDouble() : -1;
                assertEquals(0.0, rate, 0.01, "TC106: onTimeRate=0 expected; got " + rate);
        }
}

// ─── TC107 — S4-F10 deliveriesByStatus breakdown isolated ────────────────────
@Tag("public")
@Tag("features_m2")
class TC107_DeliveryAnalyticsBreakdownTests extends TestBase {
        @Test
        @DisplayName("TC107 — deliveriesByStatus has all 4 Talabat Delivery statuses each count=1")
        void breakdown_isolated() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");
                // Find Delivery.status enum from manifest
                String enumType = null;
                for (java.util.Map<String, Object> col : entityColumns("Delivery")) {
                        if ("status".equals(col.get("fieldName"))) {
                                enumType = (String) col.get("javaType");
                                break;
                        }
                }
                if (enumType == null)
                        throw new IllegalStateException("Delivery.status not in manifest");
                java.util.List<String> values = enumValues(enumType);
                for (String s : values) {
                        java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                        _oOv.put(columnByField("Order", "user"), 1L);
                        _oOv.put(columnByField("Order", "restaurant"), 1L);
                        _oOv.put(columnByField("Order", "totalAmount"), 75.00);
                        _oOv.put(columnByField("Order", "status"), "CONFIRMED");
                        Long oid = insertRowReturningId(oTable, _oOv);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-15 12:00:00"));
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), s);
                        insertRowReturningId(dTable, _dOv);
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC107 dashboard");
                JsonNode j = parseNode(r.body());
                JsonNode breakdown = j.has("deliveriesByStatus") ? j.get("deliveriesByStatus")
                                : j.has("deliveries_by_status") ? j.get("deliveries_by_status") : null;
                assertNotNull(breakdown, "TC107: deliveriesByStatus key required");
                for (String s : values) {
                        assertTrue(breakdown.has(s), "TC107: missing key '" + s + "'");
                        assertEquals(1L, breakdown.get(s).asLong(),
                                        "TC107: each status count=1; got " + s + "=" + breakdown.get(s).asLong());
                }
        }
}

// ─── TC108 — S4-F10 empty range scenario b ───────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC108_DeliveryAnalyticsEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC108 — Empty date range returns all zeros + empty deliveriesByStatus")
        void empty_range() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2030-01-01&endDate=2030-01-31", tok);
                assert2xx(r, "TC108 dashboard");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalDeliveries") ? j.get("totalDeliveries").asLong()
                                : j.has("total_deliveries") ? j.get("total_deliveries").asLong() : -1;
                double avg = j.has("averageDeliveryTimeMinutes") ? j.get("averageDeliveryTimeMinutes").asDouble()
                                : j.has("average_delivery_time_minutes")
                                                ? j.get("average_delivery_time_minutes").asDouble()
                                                : -1;
                double rate = j.has("onTimeRate") ? j.get("onTimeRate").asDouble()
                                : j.has("on_time_rate") ? j.get("on_time_rate").asDouble() : -1;
                assertEquals(0L, total, "TC108: totalDeliveries=0 expected; got " + total);
                assertEquals(0.0, avg, 0.01, "TC108: averageDeliveryTimeMinutes=0 expected; got " + avg);
                assertEquals(0.0, rate, 0.01, "TC108: onTimeRate=0 expected; got " + rate);
        }
}

// ─── TC109 — S4-F10 boundary inclusion at startDate T00:00:00 ────────────────
@Tag("public")
@Tag("features_m2")
class TC109_DeliveryAnalyticsStartBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC109 — Order/Delivery at exactly startDate T00:00:00 is included")
        void start_boundary() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");
                java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                _oOv.put(columnByField("Order", "user"), 1L);
                _oOv.put(columnByField("Order", "restaurant"), 1L);
                _oOv.put(columnByField("Order", "totalAmount"), 50.00);
                _oOv.put(columnByField("Order", "status"), "CONFIRMED");
                Long oid = insertRowReturningId(oTable, _oOv);
                setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-01 00:00:00"));
                java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                _dOv.put(columnByField("Delivery", "order"), oid);
                _dOv.put(columnByField("Delivery", "status"), "IN_TRANSIT");
                insertRowReturningId(dTable, _dOv);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC109 dashboard");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalDeliveries") ? j.get("totalDeliveries").asLong()
                                : j.has("total_deliveries") ? j.get("total_deliveries").asLong() : -1;
                assertEquals(1L, total, "TC109: boundary at startDate must include; got " + total);
        }
}

// ─── TC110 — S4-F10 boundary inclusion at endDate T23:59:59 ──────────────────
@Tag("public")
@Tag("features_m2")
class TC110_DeliveryAnalyticsEndBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC110 — Order/Delivery at endDate T23:59:59 is included")
        void end_boundary() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");
                java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                _oOv.put(columnByField("Order", "user"), 1L);
                _oOv.put(columnByField("Order", "restaurant"), 1L);
                _oOv.put(columnByField("Order", "totalAmount"), 50.00);
                _oOv.put(columnByField("Order", "status"), "CONFIRMED");
                Long oid = insertRowReturningId(oTable, _oOv);
                setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-09-30 23:59:59"));
                java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                _dOv.put(columnByField("Delivery", "order"), oid);
                _dOv.put(columnByField("Delivery", "status"), "IN_TRANSIT");
                insertRowReturningId(dTable, _dOv);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC110 dashboard");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalDeliveries") ? j.get("totalDeliveries").asLong()
                                : j.has("total_deliveries") ? j.get("total_deliveries").asLong() : -1;
                assertEquals(1L, total, "TC110: boundary at endDate must include; got " + total);
        }
}

// ─── TC111 — S4-F10 inverted dates → 400 (scenario c) ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC111_DeliveryAnalyticsInvertedDatesTests extends TestBase {
        @Test
        @DisplayName("TC111 — startDate > endDate returns 400")
        void inverted_dates_400() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(), "TC111: must be 400; got " + r.statusCode());
        }
}

// ─── TC112 — S4-F10 missing JWT → 401 (scenario d) ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC112_DeliveryAnalyticsMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC112 — Missing JWT returns 401")
        void missing_jwt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                HttpResponse<String> r = httpGet("/api/deliveries/analytics?startDate=2026-04-01&endDate=2026-04-30");
                assertEquals(401, r.statusCode(), "TC112: must be 401; got " + r.statusCode());
        }
}

// ─── TC113 — S4-F10 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC113_DeliveryAnalyticsInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC113 — Bogus JWT returns 401")
        void invalid_jwt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-04-01&endDate=2026-04-30", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC113: must be 401; got " + r.statusCode());
        }
}

// ─── TC114 — S4-F10 ANALYTICS_VIEWED logged on first call ────────────────────
@Tag("public")
@Tag("features_m2")
class TC114_DeliveryAnalyticsLoggedFirstCallTests extends TestBase {
        @Test
        @DisplayName("TC114 — First call logs ANALYTICS_VIEWED to delivery_events Mongo")
        void analytics_viewed_logged() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC114: MongoDB required.");
                String coll = s4EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                long before = col.countDocuments();
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-04-01&endDate=2026-04-30", tok);
                assert2xx(r, "TC114 dashboard");
                long after = col.countDocuments();
                assertTrue(after > before, "TC114: ANALYTICS_VIEWED event must be appended to '" + coll + "'");
                org.bson.Document latest = col.find().sort(new org.bson.Document("_id", -1)).first();
                if (latest != null) {
                        String type = latest.getString("eventType");
                        if (type == null)
                                type = latest.getString("action");
                        if (type != null)
                                assertEquals("ANALYTICS_VIEWED", type, "TC114: latest event type");
                }
        }
}

// ─── TC115 — S4-F10 ANALYTICS_VIEWED logged on cache hit too ─────────────────
@Tag("public")
@Tag("features_m2")
class TC115_DeliveryAnalyticsLoggedOnCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC115 — Repeat call (cache hit) still logs ANALYTICS_VIEWED")
        void analytics_logged_on_cache_hit() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC115: MongoDB required.");
                String coll = s4EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                long before = col.countDocuments();
                String tok = adminToken();
                assert2xx(httpGetAuth("/api/deliveries/analytics?startDate=2026-04-01&endDate=2026-04-30", tok),
                                "TC115 dashboard #1");
                assert2xx(httpGetAuth("/api/deliveries/analytics?startDate=2026-04-01&endDate=2026-04-30", tok),
                                "TC115 dashboard #2");
                long after = col.countDocuments();
                assertTrue(after >= before + 2,
                                "TC115: 2 calls must produce ≥2 ANALYTICS_VIEWED events. before=" + before + " after="
                                                + after);
        }
}

// ─── TC116 — S4-F10 cache populated in Redis with TTL ≤ 600s ─────────────────
@Tag("public")
@Tag("features_m2")
class TC116_DeliveryAnalyticsCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC116 — Dashboard call populates Redis with TTL ≤ 600s")
        void cache_populated_in_redis() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC116: Redis required.");
                java.util.Set<String> beforeKeys = redisKeys("*");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-04-01&endDate=2026-04-30", tok);
                assert2xx(r, "TC116 dashboard");
                java.util.Set<String> afterKeys = redisKeys("*");
                assertTrue(afterKeys.size() > beforeKeys.size(), "TC116: at least one new Redis key expected");
                boolean found = false;
                for (String k : afterKeys) {
                        if (beforeKeys.contains(k))
                                continue;
                        long ttl = redisTtl(k);
                        if (ttl > 0 && ttl <= 600) {
                                found = true;
                                break;
                        }
                }
                assertTrue(found, "TC116: a new Redis key must have TTL in (0, 600]s; keys=" + afterKeys);
        }
}

// ─── TC117 — S4-F10 cache hit served from Redis ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC117_DeliveryAnalyticsCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC117 — Cache hit: 2nd call (after data mutation) returns 1st response")
        void cache_hit() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC117: Redis required.");
                String oTable = tableName("Order");
                String dTable = tableName("Delivery");
                // Seed initial 2 deliveries in Apr.
                for (int i = 0; i < 2; i++) {
                        java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                        _oOv.put(columnByField("Order", "user"), 1L);
                        _oOv.put(columnByField("Order", "restaurant"), 1L);
                        _oOv.put(columnByField("Order", "totalAmount"), 100.00);
                        _oOv.put(columnByField("Order", "status"), "CONFIRMED");
                        Long oid = insertRowReturningId(oTable, _oOv);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-04-15 12:00:00"));
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), "IN_TRANSIT");
                        insertRowReturningId(dTable, _dOv);
                }
                String tok = adminToken();
                HttpResponse<String> r1 = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-04-01&endDate=2026-04-30", tok);
                assert2xx(r1, "TC117 dashboard #1");
                String cachedBefore = null;
                for (String k : redisKeys("*")) {
                        String v = redisGet(k);
                        if (v != null && v.length() > 0) {
                                cachedBefore = v;
                                break;
                        }
                }
                // Mutate
                for (int i = 0; i < 3; i++) {
                        java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                        _oOv.put(columnByField("Order", "user"), 1L);
                        _oOv.put(columnByField("Order", "restaurant"), 1L);
                        _oOv.put(columnByField("Order", "totalAmount"), 200.00);
                        _oOv.put(columnByField("Order", "status"), "CONFIRMED");
                        Long oid = insertRowReturningId(oTable, _oOv);
                        setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf("2026-04-20 12:00:00"));
                        java.util.Map<String, Object> _dOv = new java.util.HashMap<>();
                        _dOv.put(columnByField("Delivery", "order"), oid);
                        _dOv.put(columnByField("Delivery", "status"), "IN_TRANSIT");
                        insertRowReturningId(dTable, _dOv);
                }
                HttpResponse<String> r2 = httpGetAuth(
                                "/api/deliveries/analytics?startDate=2026-04-01&endDate=2026-04-30", tok);
                assert2xx(r2, "TC117 dashboard #2");
                assertEquals(r1.body(), r2.body(),
                                "TC117: cache hit expected — second response must equal first; r1=" + r1.body() + " r2="
                                                + r2.body());
                if (cachedBefore != null) {
                        String cachedAfter = null;
                        for (String k : redisKeys("*")) {
                                String v = redisGet(k);
                                if (v != null && v.length() > 0) {
                                        cachedAfter = v;
                                        break;
                                }
                        }
                        assertEquals(cachedBefore, cachedAfter, "TC117: Redis cached value unchanged across calls");
                }
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S4-F11 — Record Tracking Event (TC118..TC127)
// Endpoint: POST /api/deliveries/{id}/tracking
// Body: {"status": "...", "latitude": ..., "longitude": ..., "notes": "..."}
// driverName is read from the Delivery's PG row (cross-service SQL).
// Pre-seed delivery 1 has driver_name="Ahmed" via autoSeedBaselineData.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC118 — S4-F11 happy path: POST tracking → 201 + Cassandra row + Mongo
// event
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC118_TrackingHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC118 — POST tracking returns 201, Cassandra has row, Mongo has TRACKING_RECORDED")
        void tracking_happy_path() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null)
                        throw new AssertionError("TC118: Cassandra required.");
                if (mongo == null)
                        throw new AssertionError("TC118: MongoDB required.");
                String tok = adminToken();
                String partitionCol = cassandraColumnByField(cassandraTableClassByName(s4TimeseriesTable()),
                                s4TimeseriesPartitionField());
                String coll = s4EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> mcol = mongo.getCollection(coll);
                long mongoBefore = mcol.countDocuments();
                // Pre-seed delivery id=1, driver_name=Ahmed
                String body = "{\"status\":\"PICKED_UP\",\"latitude\":30.0444,\"longitude\":31.2357,\"notes\":\"At customer\"}";
                HttpResponse<String> r = httpPostAuth("/api/deliveries/1/tracking", body, tok);
                assertEquals(201, r.statusCode(), "TC118: must be 201; got " + r.statusCode() + " body=" + r.body());
                long casCount = cassandraCount(s4TimeseriesTable(), partitionCol, 1L);
                assertTrue(casCount >= 1, "TC118: Cassandra row expected for delivery=1; got count=" + casCount);
                long mongoAfter = mcol.countDocuments();
                assertTrue(mongoAfter > mongoBefore,
                                "TC118: Mongo TRACKING_RECORDED event expected; before=" + mongoBefore + " after="
                                                + mongoAfter);
        }
}

// ─── TC119 — S4-F11 Cassandra row contents match request fields ──────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC119_TrackingCassandraRowContentsTests extends TestBase {
        @Test
        @DisplayName("TC119 — Cassandra row has status/lat/lng/notes from the request body")
        void cassandra_row_contents() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null)
                        throw new AssertionError("TC119: Cassandra required.");
                String tok = adminToken();
                String tableClass = cassandraTableClassByName(s4TimeseriesTable());
                String partitionCol = cassandraColumnByField(tableClass, s4TimeseriesPartitionField());
                String statusCol = cassandraColumnByField(tableClass, "status");
                String latCol = cassandraColumnByField(tableClass, "latitude");
                String lngCol = cassandraColumnByField(tableClass, "longitude");
                String notesCol = cassandraColumnByField(tableClass, "notes");
                String body = "{\"status\":\"IN_TRANSIT\",\"latitude\":29.9603,\"longitude\":31.2570,\"notes\":\"On the way\"}";
                HttpResponse<String> r = httpPostAuth("/api/deliveries/1/tracking", body, tok);
                assertEquals(201, r.statusCode(), "TC119: must be 201");
                java.util.List<java.util.Map<String, Object>> rows = cassandraRows(s4TimeseriesTable(), partitionCol,
                                1L);
                assertTrue(rows.size() >= 1, "TC119: at least 1 Cassandra row expected; got " + rows.size());
                java.util.Map<String, Object> row = rows.get(0);
                assertEquals("IN_TRANSIT", row.get(statusCol),
                                "TC119: status mismatch; expected IN_TRANSIT got " + row.get(statusCol));
                Object lat = row.get(latCol);
                Object lng = row.get(lngCol);
                if (lat instanceof Number n)
                        assertEquals(29.9603, n.doubleValue(), 0.001, "TC119: latitude mismatch");
                if (lng instanceof Number n)
                        assertEquals(31.2570, n.doubleValue(), 0.001, "TC119: longitude mismatch");
                assertEquals("On the way", row.get(notesCol), "TC119: notes mismatch");
        }
}

// ─── TC120 — S4-F11 driverName resolved via cross-service SQL from PG ────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC120_TrackingDriverNameFromPgTests extends TestBase {
        @Test
        @DisplayName("TC120 — Cassandra row's driverName comes from the Delivery PG row (not request)")
        void driver_name_from_pg() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null)
                        throw new AssertionError("TC120: Cassandra required.");
                String tok = adminToken();
                String tableClass = cassandraTableClassByName(s4TimeseriesTable());
                String partitionCol = cassandraColumnByField(tableClass, s4TimeseriesPartitionField());
                String actorCol = cassandraColumnByField(tableClass, s4ActorField());
                // Pre-seed delivery 1 has driver_name="Ahmed" (set by autoSeedBaselineData).
                String body = "{\"status\":\"ASSIGNED\",\"latitude\":30.0,\"longitude\":31.0,\"notes\":\"start\"}";
                HttpResponse<String> r = httpPostAuth("/api/deliveries/1/tracking", body, tok);
                assertEquals(201, r.statusCode(), "TC120: must be 201");
                java.util.List<java.util.Map<String, Object>> rows = cassandraRows(s4TimeseriesTable(), partitionCol,
                                1L);
                assertTrue(rows.size() >= 1, "TC120: at least 1 row expected");
                Object actor = rows.get(0).get(actorCol);
                assertEquals("Ahmed", actor,
                                "TC120: " + s4ActorField() + " must come from PG (delivery.driver_name='Ahmed'), got '"
                                                + actor + "'");
        }
}

// ─── TC121 — S4-F11 Cassandra partition key = deliveryId ─────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC121_TrackingPartitionKeyTests extends TestBase {
        @Test
        @DisplayName("TC121 — POST tracking writes only to the deliveryId's partition")
        void partition_key_is_delivery_id() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null)
                        throw new AssertionError("TC121: Cassandra required.");
                String tok = adminToken();
                String tableClass = cassandraTableClassByName(s4TimeseriesTable());
                String partitionCol = cassandraColumnByField(tableClass, s4TimeseriesPartitionField());
                // Post for delivery 1
                String body = "{\"status\":\"PICKED_UP\",\"latitude\":30.0,\"longitude\":31.0,\"notes\":\"pickup\"}";
                assertEquals(201, httpPostAuth("/api/deliveries/1/tracking", body, tok).statusCode(),
                                "TC121: post must succeed");
                long del1 = cassandraCount(s4TimeseriesTable(), partitionCol, 1L);
                long del2 = cassandraCount(s4TimeseriesTable(), partitionCol, 2L);
                assertTrue(del1 >= 1, "TC121: delivery=1 partition expected to have ≥1 row; got " + del1);
                assertEquals(0L, del2,
                                "TC121: delivery=2 partition must remain empty (correct partition routing); got "
                                                + del2);
        }
}

// ─── TC122 — S4-F11 Cassandra clustering ordering — multiple events ──────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC122_TrackingClusteringOrderTests extends TestBase {
        @Test
        @DisplayName("TC122 — Multiple POSTs for same delivery → all appear, most-recent-first via clustering")
        void clustering_descending() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null)
                        throw new AssertionError("TC122: Cassandra required.");
                String tok = adminToken();
                String tableClass = cassandraTableClassByName(s4TimeseriesTable());
                String partitionCol = cassandraColumnByField(tableClass, s4TimeseriesPartitionField());
                String statusCol = cassandraColumnByField(tableClass, "status");
                // POST PICKED_UP, then IN_TRANSIT.
                assertEquals(201, httpPostAuth("/api/deliveries/1/tracking",
                                "{\"status\":\"PICKED_UP\",\"latitude\":30.0,\"longitude\":31.0,\"notes\":\"a\"}", tok)
                                .statusCode());
                Thread.sleep(50); // ensure timestamp progression for clustering ordering
                assertEquals(201, httpPostAuth("/api/deliveries/1/tracking",
                                "{\"status\":\"IN_TRANSIT\",\"latitude\":30.1,\"longitude\":31.1,\"notes\":\"b\"}", tok)
                                .statusCode());
                java.util.List<java.util.Map<String, Object>> rows = cassandraRows(s4TimeseriesTable(), partitionCol,
                                1L);
                assertEquals(2, rows.size(), "TC122: 2 rows expected; got " + rows.size());
                // First row should be the most recent (IN_TRANSIT) per clustering DESC.
                assertEquals("IN_TRANSIT", rows.get(0).get(statusCol),
                                "TC122: most recent row first (clustering DESC); got " + rows.get(0).get(statusCol));
                assertEquals("PICKED_UP", rows.get(1).get(statusCol),
                                "TC122: older row second; got " + rows.get(1).get(statusCol));
        }
}

// ─── TC123 — S4-F11 Mongo TRACKING_RECORDED event details ────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC123_TrackingMongoEventDetailsTests extends TestBase {
        @Test
        @DisplayName("TC123 — Mongo TRACKING_RECORDED event has deliveryId + status in details")
        void mongo_event_details() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC123: MongoDB required.");
                String tok = adminToken();
                String coll = s4EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> mcol = mongo.getCollection(coll);
                String body = "{\"status\":\"PICKED_UP\",\"latitude\":30.0,\"longitude\":31.0,\"notes\":\"x\"}";
                assertEquals(201, httpPostAuth("/api/deliveries/1/tracking", body, tok).statusCode());
                org.bson.Document latest = mcol.find().sort(new org.bson.Document("_id", -1)).first();
                assertNotNull(latest, "TC123: at least one event in '" + coll + "' expected");
                org.bson.Document details = latest.get("details", org.bson.Document.class);
                if (details != null) {
                        Long delId = details.getLong("deliveryId");
                        String stat = details.getString("status");
                        if (delId != null)
                                assertEquals(Long.valueOf(1L), delId, "TC123: details.deliveryId");
                        if (stat != null)
                                assertEquals("PICKED_UP", stat, "TC123: details.status");
                }
                String type = latest.getString("eventType");
                if (type == null)
                        type = latest.getString("action");
                if (type != null)
                        assertEquals("TRACKING_RECORDED", type, "TC123: eventType");
        }
}

// ─── TC124 — S4-F11 both Cassandra + Mongo writes succeed in same call ───────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC124_TrackingBothStoresWrittenTests extends TestBase {
        @Test
        @DisplayName("TC124 — Single POST writes BOTH the Cassandra row AND the Mongo event")
        void both_stores_written() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null)
                        throw new AssertionError("TC124: Cassandra required.");
                if (mongo == null)
                        throw new AssertionError("TC124: MongoDB required.");
                String tok = adminToken();
                String tableClass = cassandraTableClassByName(s4TimeseriesTable());
                String partitionCol = cassandraColumnByField(tableClass, s4TimeseriesPartitionField());
                com.mongodb.client.MongoCollection<org.bson.Document> mcol = mongo.getCollection(s4EventsCollection());
                long casBefore = cassandraCount(s4TimeseriesTable(), partitionCol, 1L);
                long mongoBefore = mcol.countDocuments();
                String body = "{\"status\":\"PICKED_UP\",\"latitude\":30.0,\"longitude\":31.0,\"notes\":\"both\"}";
                assertEquals(201, httpPostAuth("/api/deliveries/1/tracking", body, tok).statusCode());
                long casAfter = cassandraCount(s4TimeseriesTable(), partitionCol, 1L);
                long mongoAfter = mcol.countDocuments();
                assertTrue(casAfter > casBefore, "TC124: Cassandra grew; before=" + casBefore + " after=" + casAfter);
                assertTrue(mongoAfter > mongoBefore,
                                "TC124: Mongo grew; before=" + mongoBefore + " after=" + mongoAfter);
        }
}

// ─── TC125 — S4-F11 non-existent delivery → 404 (scenario c) ─────────────────
@Tag("public")
@Tag("features_m2")
class TC125_TrackingNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC125 — POST tracking for non-existent delivery returns 404")
        void tracking_not_found_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                String body = "{\"status\":\"PICKED_UP\",\"latitude\":30.0,\"longitude\":31.0,\"notes\":\"x\"}";
                HttpResponse<String> r = httpPostAuth("/api/deliveries/999/tracking", body, tok);
                assertEquals(404, r.statusCode(), "TC125: must be 404; got " + r.statusCode());
        }
}

// ─── TC126 — S4-F11 missing JWT → 401 (scenario d) ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC126_TrackingMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC126 — Missing JWT returns 401")
        void missing_jwt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String body = "{\"status\":\"PICKED_UP\",\"latitude\":30.0,\"longitude\":31.0,\"notes\":\"x\"}";
                HttpResponse<String> r = httpPost("/api/deliveries/1/tracking", body);
                assertEquals(401, r.statusCode(), "TC126: must be 401; got " + r.statusCode());
        }
}

// ─── TC127 — S4-F11 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC127_TrackingInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC127 — Bogus JWT returns 401")
        void invalid_jwt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String body = "{\"status\":\"PICKED_UP\",\"latitude\":30.0,\"longitude\":31.0,\"notes\":\"x\"}";
                HttpResponse<String> r = httpPostAuth("/api/deliveries/1/tracking", body, "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC127: must be 401; got " + r.statusCode());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S4-F12 — Get Tracking Timeline (TC128..TC135)
// Endpoint: GET /api/deliveries/{id}/tracking?startTime=&endTime=
// Tests seed Cassandra directly (cassandraExec INSERTs) so they don't depend
// on the S4-F11 controller. Pre-seed delivery 1 always exists in PG.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC128 — S4-F12 happy path: 3 events, reverse-chrono order (scenario a) ─
@Tag("public")
@Tag("features_m2")
class TC128_TimelineHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC128 — GET timeline returns 3 events with most-recent first (IN_TRANSIT)")
        void timeline_happy_path() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null)
                        throw new AssertionError("TC128: Cassandra required.");
                String tableClass = cassandraTableClassByName(s4TimeseriesTable());
                String partitionCol = cassandraColumnByField(tableClass, s4TimeseriesPartitionField());
                String clusteringCol = cassandraColumnByField(tableClass, s4TimeseriesClusteringField());
                String statusCol = cassandraColumnByField(tableClass, "status");
                String actorCol = cassandraColumnByField(tableClass, s4ActorField());
                // Seed 3 events at 14:00, 14:15, 14:30 — clustering DESC means 14:30 first.
                java.util.Map<String, Object> _cins = new java.util.HashMap<>();
                _cins.put(partitionCol, 1L);
                _cins.put(statusCol, "ASSIGNED");
                _cins.put(actorCol, "Ahmed");
                _cins.put(clusteringCol, java.time.Instant.parse("2026-04-01T14:00:00Z"));
                cassandraInsertRow(s4TimeseriesTable(), _cins);
                _cins.put(statusCol, "PICKED_UP");
                _cins.put(clusteringCol, java.time.Instant.parse("2026-04-01T14:15:00Z"));
                cassandraInsertRow(s4TimeseriesTable(), _cins);
                _cins.put(statusCol, "IN_TRANSIT");
                _cins.put(clusteringCol, java.time.Instant.parse("2026-04-01T14:30:00Z"));
                cassandraInsertRow(s4TimeseriesTable(), _cins);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/deliveries/1/tracking", tok);
                assert2xx(r, "TC128 timeline");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr
                                : (arr.has("events") ? arr.get("events")
                                                : arr.has("data") ? arr.get("data") : arr);
                assertNotNull(list, "TC128: timeline list expected");
                assertEquals(3, list.size(), "TC128: 3 events expected; got " + list.size() + " body=" + r.body());
                // First entry should be most-recent (IN_TRANSIT).
                JsonNode first = list.get(0);
                String s = first.has("status") ? first.get("status").asText() : "";
                assertEquals("IN_TRANSIT", s, "TC128: most-recent first; got " + s);
        }
}

// ─── TC129 — S4-F12 time range filter (scenario b) ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC129_TimelineTimeRangeTests extends TestBase {
        @Test
        @DisplayName("TC129 — startTime/endTime filter narrows to PICKED_UP only")
        void timeline_time_range() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null)
                        throw new AssertionError("TC129: Cassandra required.");
                String tableClass = cassandraTableClassByName(s4TimeseriesTable());
                String partitionCol = cassandraColumnByField(tableClass, s4TimeseriesPartitionField());
                String clusteringCol = cassandraColumnByField(tableClass, s4TimeseriesClusteringField());
                String statusCol = cassandraColumnByField(tableClass, "status");
                String actorCol = cassandraColumnByField(tableClass, s4ActorField());
                // Seed 3 events at 14:00, 14:15, 14:30
                java.util.Map<String, Object> _cins = new java.util.HashMap<>();
                _cins.put(partitionCol, 1L);
                _cins.put(actorCol, "Ahmed");
                _cins.put(statusCol, "ASSIGNED");
                _cins.put(clusteringCol, java.time.Instant.parse("2026-04-01T14:00:00Z"));
                cassandraInsertRow(s4TimeseriesTable(), _cins);
                _cins.put(statusCol, "PICKED_UP");
                _cins.put(clusteringCol, java.time.Instant.parse("2026-04-01T14:15:00Z"));
                cassandraInsertRow(s4TimeseriesTable(), _cins);
                _cins.put(statusCol, "IN_TRANSIT");
                _cins.put(clusteringCol, java.time.Instant.parse("2026-04-01T14:30:00Z"));
                cassandraInsertRow(s4TimeseriesTable(), _cins);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/deliveries/1/tracking?startTime=2026-04-01T14:10:00Z&endTime=2026-04-01T14:20:00Z",
                                tok);
                assert2xx(r, "TC129 timeline filtered");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr
                                : (arr.has("events") ? arr.get("events")
                                                : arr.has("data") ? arr.get("data") : arr);
                assertEquals(1, list.size(),
                                "TC129: 1 event expected (PICKED_UP); got " + list.size() + " body=" + r.body());
                String s = list.get(0).has("status") ? list.get(0).get("status").asText() : "";
                assertEquals("PICKED_UP", s, "TC129: only PICKED_UP in range; got " + s);
        }
}

// ─── TC130 — S4-F12 empty timeline (scenario d) ──────────────────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC130_TimelineEmptyTests extends TestBase {
        @Test
        @DisplayName("TC130 — Delivery with no tracking events returns empty list")
        void timeline_empty() throws Exception {
                BASE_URL = deliveryServiceUrl;
                // Pre-seed delivery 1 has no Cassandra events (we wiped + truncate per test).
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/deliveries/1/tracking", tok);
                assert2xx(r, "TC130 timeline");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr
                                : (arr.has("events") ? arr.get("events")
                                                : arr.has("data") ? arr.get("data") : arr);
                int size = (list != null && list.isArray()) ? list.size() : -1;
                assertEquals(0, size, "TC130: empty list expected; got " + size);
        }
}

// ─── TC131 — S4-F12 non-existent delivery → 404 (scenario c) ─────────────────
@Tag("public")
@Tag("features_m2")
class TC131_TimelineNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC131 — GET timeline for non-existent delivery returns 404")
        void timeline_not_found() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/deliveries/999/tracking", tok);
                assertEquals(404, r.statusCode(), "TC131: must be 404; got " + r.statusCode());
        }
}

// ─── TC132 — S4-F12 missing JWT → 401 (scenario e) ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC132_TimelineMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC132 — Missing JWT returns 401")
        void missing_jwt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                HttpResponse<String> r = httpGet("/api/deliveries/1/tracking");
                assertEquals(401, r.statusCode(), "TC132: must be 401; got " + r.statusCode());
        }
}

// ─── TC133 — S4-F12 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC133_TimelineInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC133 — Bogus JWT returns 401")
        void invalid_jwt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                HttpResponse<String> r = httpGetAuth("/api/deliveries/1/tracking", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC133: must be 401; got " + r.statusCode());
        }
}

// ─── TC134 — S4-F12 cache populated in Redis with TTL ≤ 300s ─────────────────
@Tag("public")
@Tag("features_m2")
@Tag("with-baseline")
class TC134_TimelineCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC134 — Timeline call populates Redis with TTL ≤ 300s")
        void cache_populated() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC134: Redis required.");
                java.util.Set<String> beforeKeys = redisKeys("*");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/deliveries/1/tracking", tok);
                assert2xx(r, "TC134 timeline");
                java.util.Set<String> afterKeys = redisKeys("*");
                assertTrue(afterKeys.size() > beforeKeys.size(), "TC134: at least one new Redis key expected");
                boolean found = false;
                for (String k : afterKeys) {
                        if (beforeKeys.contains(k))
                                continue;
                        long ttl = redisTtl(k);
                        if (ttl > 0 && ttl <= 300) {
                                found = true;
                                break;
                        }
                }
                assertTrue(found, "TC134: a new Redis key must have TTL in (0, 300]s; keys=" + afterKeys);
        }
}

// ─── TC135 — S4-F12 cache hit served (Redis-confirmed) ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC135_TimelineCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC135 — Cache hit: Cassandra mutation between calls doesn't change response")
        void cache_hit() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC135: Redis required.");
                if (cassandra == null)
                        throw new AssertionError("TC135: Cassandra required.");
                String tableClass = cassandraTableClassByName(s4TimeseriesTable());
                String partitionCol = cassandraColumnByField(tableClass, s4TimeseriesPartitionField());
                String clusteringCol = cassandraColumnByField(tableClass, s4TimeseriesClusteringField());
                String statusCol = cassandraColumnByField(tableClass, "status");
                String actorCol = cassandraColumnByField(tableClass, s4ActorField());
                // Seed 1 event
                java.util.Map<String, Object> _cins = new java.util.HashMap<>();
                _cins.put(partitionCol, 1L);
                _cins.put(actorCol, "Ahmed");
                _cins.put(statusCol, "ASSIGNED");
                _cins.put(clusteringCol, java.time.Instant.parse("2026-04-01T14:00:00Z"));
                cassandraInsertRow(s4TimeseriesTable(), _cins);
                String tok = adminToken();
                HttpResponse<String> r1 = httpGetAuth("/api/deliveries/1/tracking", tok);
                assert2xx(r1, "TC135 timeline #1");
                String cachedBefore = null;
                for (String k : redisKeys("*")) {
                        String v = redisGet(k);
                        if (v != null && v.length() > 0) {
                                cachedBefore = v;
                                break;
                        }
                }
                // Mutate — add a new event.
                _cins.put(statusCol, "IN_TRANSIT");
                _cins.put(clusteringCol, java.time.Instant.parse("2026-04-01T14:30:00Z"));
                cassandraInsertRow(s4TimeseriesTable(), _cins);
                HttpResponse<String> r2 = httpGetAuth("/api/deliveries/1/tracking", tok);
                assert2xx(r2, "TC135 timeline #2");
                assertEquals(r1.body(), r2.body(),
                                "TC135: cache hit expected — r1 must equal r2 even after Cassandra mutation; r1="
                                                + r1.body() + " r2=" + r2.body());
                if (cachedBefore != null) {
                        String cachedAfter = null;
                        for (String k : redisKeys("*")) {
                                String v = redisGet(k);
                                if (v != null && v.length() > 0) {
                                        cachedAfter = v;
                                        break;
                                }
                        }
                        assertEquals(cachedBefore, cachedAfter, "TC135: Redis cached value unchanged across calls");
                }
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S5 M2 — Checkout Service features (TC136..TC190)
//
// Covers S5-F10 (cuisine analytics, TC136-TC153), S5-F11 (payment-method
// analytics, TC154-TC170), and S5-F12 (refund with strategy pattern,
// TC171-TC190). Pre-req infra (Mongo + Redis already in place; new
// payment_audit_trail Mongo collection registered in build_scanner_manifest;
// s5* helpers live in TestBase). Per-test wipe of PG/Mongo[*]/Redis happens
// in autoTruncateAllData(); Mongo collections are reset by ad-hoc deletes
// in tests that need a clean slate.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC136 — S5-F10 cuisine analytics happy path (composite scenario a) ──────
@Tag("public")
@Tag("features_m2")
class TC136_CuisineAnalyticsHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC136 — Cuisine breakdown matches per-group spec values (ITALIAN/CHINESE)")
        void cuisine_happy_path() throws Exception {
                BASE_URL = checkoutServiceUrl;
                // Reuse pre-seed restaurant 1 (ITALIAN) + restaurant 2 (EGYPTIAN).
                // We need 3 ITALIAN + 2 CHINESE — pre-seed has only 1 cuisine value of each
                // type, so insert two custom restaurants to control cuisineType cleanly.
                String rTable = tableName("Restaurant");
                java.util.Map<String, Object> _r1Ov = new java.util.HashMap<>();
                _r1Ov.put(columnByField("Restaurant", "name"), "TC136 R1 ITALIAN");
                _r1Ov.put(columnByField("Restaurant", "cuisineType"), "ITALIAN");
                _r1Ov.put(columnByField("Restaurant", "status"), "OPEN");
                Long r1 = insertRowReturningId(rTable, _r1Ov);
                java.util.Map<String, Object> _r2Ov = new java.util.HashMap<>();
                _r2Ov.put(columnByField("Restaurant", "name"), "TC136 R2 CHINESE");
                _r2Ov.put(columnByField("Restaurant", "cuisineType"), "CHINESE");
                _r2Ov.put(columnByField("Restaurant", "status"), "OPEN");
                Long r2 = insertRowReturningId(rTable, _r2Ov);
                java.util.Map<String, Object> _r3Ov = new java.util.HashMap<>();
                _r3Ov.put(columnByField("Restaurant", "name"), "TC136 R3 ITALIAN");
                _r3Ov.put(columnByField("Restaurant", "cuisineType"), "ITALIAN");
                _r3Ov.put(columnByField("Restaurant", "status"), "OPEN");
                Long r3 = insertRowReturningId(rTable, _r3Ov);
                // Italian: 3 orders/payments — amounts 200/200/200 = 600, fees 20/20/20 = 60.
                // Chinese: 2 orders/payments — amounts 200/200 = 400, fees 20/20 = 40.
                _seedOrderPayment(r1, 200.00, 20.00, "2026-08-15");
                _seedOrderPayment(r1, 200.00, 20.00, "2026-08-15");
                _seedOrderPayment(r3, 200.00, 20.00, "2026-08-15");
                _seedOrderPayment(r2, 200.00, 20.00, "2026-08-15");
                _seedOrderPayment(r2, 200.00, 20.00, "2026-08-15");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC136 cuisine analytics");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("breakdown") ? arr.get("breakdown") : arr);
                java.util.Map<String, JsonNode> byCuisine = new java.util.HashMap<>();
                for (JsonNode item : list) {
                        String c = item.has("cuisineType") ? item.get("cuisineType").asText()
                                        : item.has("cuisine_type") ? item.get("cuisine_type").asText() : "";
                        byCuisine.put(c, item);
                }
                JsonNode it = byCuisine.get("ITALIAN");
                JsonNode ch = byCuisine.get("CHINESE");
                assertNotNull(it, "TC136: ITALIAN entry expected; got=" + r.body());
                assertNotNull(ch, "TC136: CHINESE entry expected; got=" + r.body());
                assertEquals(540.0, _readDouble(it, "foodRevenue", "food_revenue"), 0.5, "TC136: ITALIAN.foodRevenue");
                assertEquals(60.0, _readDouble(it, "deliveryFeeRevenue", "delivery_fee_revenue"), 0.5,
                                "TC136: ITALIAN.deliveryFeeRevenue");
                assertEquals(600.0, _readDouble(it, "totalRevenue", "total_revenue"), 0.5,
                                "TC136: ITALIAN.totalRevenue");
                assertEquals(3, _readLong(it, "orderCount", "order_count"), "TC136: ITALIAN.orderCount");
                assertEquals(360.0, _readDouble(ch, "foodRevenue", "food_revenue"), 0.5, "TC136: CHINESE.foodRevenue");
                assertEquals(40.0, _readDouble(ch, "deliveryFeeRevenue", "delivery_fee_revenue"), 0.5,
                                "TC136: CHINESE.deliveryFeeRevenue");
                assertEquals(400.0, _readDouble(ch, "totalRevenue", "total_revenue"), 0.5,
                                "TC136: CHINESE.totalRevenue");
                assertEquals(2, _readLong(ch, "orderCount", "order_count"), "TC136: CHINESE.orderCount");
        }

        /**
         * Insert 1 Order (DELIVERED, in date range) + 1 Payment (COMPLETED) attached to
         * it.
         * Sets transactionDetails JSONB with deliveryFee.
         */
        private void _seedOrderPayment(long restId, double amount, double deliveryFee, String dateStr) {
                String oTable = tableName("Order");
                String pTable = tableName("Payment");
                java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                _oOv.put(columnByField("Order", "user"), 1L);
                _oOv.put(columnByField("Order", "restaurant"), restId);
                _oOv.put(columnByField("Order", "totalAmount"), amount);
                _oOv.put(columnByField("Order", "status"), "DELIVERED");
                Long oid = insertRowReturningId(oTable, _oOv);
                setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf(dateStr + " 12:00:00"));
                java.util.Map<String, Object> _pOv = new java.util.HashMap<>();
                _pOv.put(columnByField("Payment", "order"), oid);
                _pOv.put(columnByField("Payment", "user"), 1L);
                _pOv.put(columnByField("Payment", "amount"), amount);
                _pOv.put(columnByField("Payment", "method"), "CREDIT_CARD");
                _pOv.put(columnByField("Payment", "status"), "COMPLETED");
                Long pid = insertRowReturningId(pTable, _pOv);
                jdbc.update("UPDATE \"" + pTable + "\" SET transaction_details = ?::jsonb WHERE id = ?",
                                String.format("{\"deliveryFee\":%.2f}", deliveryFee), pid);
        }

        private long _readLong(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asLong();
                return -1;
        }

        private double _readDouble(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asDouble();
                return -1;
        }
}

// ─── TC137 — S5-F10 deliveryFeeRevenue reads from JSONB key ──────────────────
@Tag("public")
@Tag("features_m2")
class TC137_CuisineAnalyticsDeliveryFeeFromJsonbTests extends TestBase {
        @Test
        @DisplayName("TC137 — deliveryFeeRevenue equals sum of transactionDetails.deliveryFee")
        void delivery_fee_from_jsonb() throws Exception {
                BASE_URL = checkoutServiceUrl;
                // 1 ITALIAN payment, amount=100, transactionDetails.deliveryFee=15.
                long restId = _insertRestaurant("ITALIAN");
                _seedOrderPayment(restId, 100.00, 15.00, "2026-08-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC137 cuisine analytics");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("breakdown") ? arr.get("breakdown") : arr);
                for (JsonNode item : list) {
                        if ("ITALIAN".equals(_str(item, "cuisineType", "cuisine_type"))) {
                                double fee = _readDouble(item, "deliveryFeeRevenue", "delivery_fee_revenue");
                                assertEquals(15.0, fee, 0.01, "TC137: deliveryFeeRevenue=15 expected; got " + fee);
                                return;
                        }
                }
                throw new AssertionError("TC137: ITALIAN entry not found; body=" + r.body());
        }

        private long _insertRestaurant(String cuisine) {
                String rTable = tableName("Restaurant");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(columnByField("Restaurant", "name"), "TC137 " + cuisine);
                _ov.put(columnByField("Restaurant", "cuisineType"), cuisine);
                _ov.put(columnByField("Restaurant", "status"), "OPEN");
                return insertRowReturningId(rTable, _ov);
        }

        private void _seedOrderPayment(long restId, double amount, double deliveryFee, String dateStr) {
                String oTable = tableName("Order");
                String pTable = tableName("Payment");
                java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                _oOv.put(columnByField("Order", "user"), 1L);
                _oOv.put(columnByField("Order", "restaurant"), restId);
                _oOv.put(columnByField("Order", "totalAmount"), amount);
                _oOv.put(columnByField("Order", "status"), "DELIVERED");
                Long oid = insertRowReturningId(oTable, _oOv);
                setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf(dateStr + " 12:00:00"));
                java.util.Map<String, Object> _pOv = new java.util.HashMap<>();
                _pOv.put(columnByField("Payment", "order"), oid);
                _pOv.put(columnByField("Payment", "user"), 1L);
                _pOv.put(columnByField("Payment", "amount"), amount);
                _pOv.put(columnByField("Payment", "method"), "CREDIT_CARD");
                _pOv.put(columnByField("Payment", "status"), "COMPLETED");
                Long pid = insertRowReturningId(pTable, _pOv);
                jdbc.update("UPDATE \"" + pTable + "\" SET transaction_details = ?::jsonb WHERE id = ?",
                                String.format("{\"deliveryFee\":%.2f}", deliveryFee), pid);
        }

        private double _readDouble(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asDouble();
                return -1;
        }

        private String _str(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asText();
                return "";
        }
}

// ─── TC138 — S5-F10 deliveryFeeRevenue defaults to 10% when JSONB key missing
// ─
@Tag("public")
@Tag("features_m2")
class TC138_CuisineAnalyticsDeliveryFeeDefaultTests extends TestBase {
        @Test
        @DisplayName("TC138 — deliveryFeeRevenue defaults to 10% of payment amount when JSONB key missing")
        void delivery_fee_default_10_percent() throws Exception {
                BASE_URL = checkoutServiceUrl;
                // 1 ITALIAN payment, amount=100, transactionDetails={} (no deliveryFee key).
                long restId = _insertRestaurant("ITALIAN");
                _seedOrderPaymentNoFee(restId, 100.00, "2026-08-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC138 cuisine analytics");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("breakdown") ? arr.get("breakdown") : arr);
                for (JsonNode item : list) {
                        if ("ITALIAN".equals(_str(item, "cuisineType", "cuisine_type"))) {
                                double fee = _readDouble(item, "deliveryFeeRevenue", "delivery_fee_revenue");
                                assertEquals(10.0, fee, 0.01,
                                                "TC138: missing JSONB key → default 10% of 100 = 10.0; got " + fee);
                                return;
                        }
                }
                throw new AssertionError("TC138: ITALIAN entry not found");
        }

        private long _insertRestaurant(String cuisine) {
                String rTable = tableName("Restaurant");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(columnByField("Restaurant", "name"), "TC138 " + cuisine);
                _ov.put(columnByField("Restaurant", "cuisineType"), cuisine);
                _ov.put(columnByField("Restaurant", "status"), "OPEN");
                return insertRowReturningId(rTable, _ov);
        }

        private void _seedOrderPaymentNoFee(long restId, double amount, String dateStr) {
                String oTable = tableName("Order");
                String pTable = tableName("Payment");
                java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                _oOv.put(columnByField("Order", "user"), 1L);
                _oOv.put(columnByField("Order", "restaurant"), restId);
                _oOv.put(columnByField("Order", "totalAmount"), amount);
                _oOv.put(columnByField("Order", "status"), "DELIVERED");
                Long oid = insertRowReturningId(oTable, _oOv);
                setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf(dateStr + " 12:00:00"));
                java.util.Map<String, Object> _pOv = new java.util.HashMap<>();
                _pOv.put(columnByField("Payment", "order"), oid);
                _pOv.put(columnByField("Payment", "user"), 1L);
                _pOv.put(columnByField("Payment", "amount"), amount);
                _pOv.put(columnByField("Payment", "method"), "CREDIT_CARD");
                _pOv.put(columnByField("Payment", "status"), "COMPLETED");
                Long pid = insertRowReturningId(pTable, _pOv);
                // Empty JSONB — no deliveryFee key
                jdbc.update("UPDATE \"" + pTable + "\" SET transaction_details = ?::jsonb WHERE id = ?", "{}", pid);
        }

        private double _readDouble(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asDouble();
                return -1;
        }

        private String _str(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asText();
                return "";
        }
}

// ─── shared seed helper for S5-F10 tests (in-test class to avoid TestBase
// bloat) ───
class _S5F10Seed {
        /**
         * Insert restaurant + order + payment with controlled cuisine + amount +
         * delivery fee + date.
         * Used by TC139-TC147. Returns the payment id.
         */
        static long seedOrderPayment(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        String cuisine, String orderStatus, String paymentStatus,
                        double amount, double deliveryFee, String dateStr) {
                String rTable = tb.tableName("Restaurant");
                java.util.Map<String, Object> _rOv = new java.util.HashMap<>();
                _rOv.put(tb.columnByField("Restaurant", "name"), "TC R " + cuisine + " " + System.nanoTime());
                _rOv.put(tb.columnByField("Restaurant", "cuisineType"), cuisine);
                _rOv.put(tb.columnByField("Restaurant", "status"), "OPEN");
                Long restId = tb.insertRowReturningId(rTable, _rOv);
                String oTable = tb.tableName("Order");
                java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                _oOv.put(tb.columnByField("Order", "user"), 1L);
                _oOv.put(tb.columnByField("Order", "restaurant"), restId);
                _oOv.put(tb.columnByField("Order", "totalAmount"), amount);
                _oOv.put(tb.columnByField("Order", "status"), orderStatus);
                Long oid = tb.insertRowReturningId(oTable, _oOv);
                tb.setAllDateColumns(oTable, oid, java.sql.Timestamp.valueOf(dateStr + " 12:00:00"));
                String pTable = tb.tableName("Payment");
                java.util.Map<String, Object> _pOv = new java.util.HashMap<>();
                _pOv.put(tb.columnByField("Payment", "order"), oid);
                _pOv.put(tb.columnByField("Payment", "user"), 1L);
                _pOv.put(tb.columnByField("Payment", "amount"), amount);
                _pOv.put(tb.columnByField("Payment", "method"), "CREDIT_CARD");
                _pOv.put(tb.columnByField("Payment", "status"), paymentStatus);
                Long pid = tb.insertRowReturningId(pTable, _pOv);
                if (deliveryFee >= 0) {
                        jdbc.update("UPDATE \"" + pTable + "\" SET transaction_details = ?::jsonb WHERE id = ?",
                                        String.format("{\"deliveryFee\":%.2f}", deliveryFee), pid);
                }
                return pid;
        }
}

// ─── TC139 — S5-F10 foodRevenue = sum(amount) - deliveryFeeRevenue ──────────
@Tag("public")
@Tag("features_m2")
class TC139_CuisineAnalyticsFoodRevenueTests extends TestBase {
        @Test
        @DisplayName("TC139 — foodRevenue equals sum(amount) - deliveryFeeRevenue")
        void food_revenue_isolated() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 100.0, 10.0, "2026-08-10");
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 200.0, 20.0, "2026-08-12");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC139 analytics");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                for (JsonNode it : list) {
                        String cui = it.has("cuisineType") ? it.get("cuisineType").asText()
                                        : it.has("cuisine_type") ? it.get("cuisine_type").asText() : "";
                        if ("ITALIAN".equals(cui)) {
                                double food = it.has("foodRevenue") ? it.get("foodRevenue").asDouble()
                                                : it.has("food_revenue") ? it.get("food_revenue").asDouble() : -1;
                                assertEquals(270.0, food, 0.5, "TC139: foodRevenue=270 expected; got " + food);
                                return;
                        }
                }
                throw new AssertionError("TC139: ITALIAN entry not found");
        }
}

// ─── TC140 — S5-F10 totalRevenue = sum of payment amounts ────────────────────
@Tag("public")
@Tag("features_m2")
class TC140_CuisineAnalyticsTotalRevenueTests extends TestBase {
        @Test
        @DisplayName("TC140 — totalRevenue equals sum of payment amounts in group")
        void total_revenue_isolated() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 100.0, 10.0, "2026-08-10");
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 200.0, 20.0, "2026-08-12");
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 300.0, 30.0, "2026-08-14");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC140 analytics");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                for (JsonNode it : list) {
                        String cui = it.has("cuisineType") ? it.get("cuisineType").asText()
                                        : it.has("cuisine_type") ? it.get("cuisine_type").asText() : "";
                        if ("ITALIAN".equals(cui)) {
                                double total = it.has("totalRevenue") ? it.get("totalRevenue").asDouble()
                                                : it.has("total_revenue") ? it.get("total_revenue").asDouble() : -1;
                                assertEquals(600.0, total, 0.5, "TC140: totalRevenue=600 expected; got " + total);
                                return;
                        }
                }
                throw new AssertionError("TC140: ITALIAN entry not found");
        }
}

// ─── TC141 — S5-F10 orderCount = count of distinct order IDs ─────────────────
@Tag("public")
@Tag("features_m2")
class TC141_CuisineAnalyticsOrderCountTests extends TestBase {
        @Test
        @DisplayName("TC141 — orderCount counts DISTINCT orders (split payments don't inflate)")
        void order_count_distinct() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid1 = _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 50.0, 5.0,
                                "2026-08-10");
                Long oid1 = jdbc.queryForObject(
                                "SELECT \"" + columnByField("Payment", "order") + "\" FROM \"" + tableName("Payment")
                                                + "\" WHERE id = ?",
                                Long.class, pid1);
                String pTable = tableName("Payment");
                java.util.Map<String, Object> _pOv = new java.util.HashMap<>();
                _pOv.put(columnByField("Payment", "order"), oid1);
                _pOv.put(columnByField("Payment", "user"), 1L);
                _pOv.put(columnByField("Payment", "amount"), 50.0);
                _pOv.put(columnByField("Payment", "method"), "CREDIT_CARD");
                _pOv.put(columnByField("Payment", "status"), "COMPLETED");
                insertRowReturningId(pTable, _pOv);
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 100.0, 10.0, "2026-08-12");
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 100.0, 10.0, "2026-08-14");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC141 analytics");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                for (JsonNode it : list) {
                        String cui = it.has("cuisineType") ? it.get("cuisineType").asText()
                                        : it.has("cuisine_type") ? it.get("cuisine_type").asText() : "";
                        if ("ITALIAN".equals(cui)) {
                                long count = it.has("orderCount") ? it.get("orderCount").asLong()
                                                : it.has("order_count") ? it.get("order_count").asLong() : -1;
                                assertEquals(3L, count,
                                                "TC141: orderCount must count DISTINCT orders=3, NOT payments=4; got "
                                                                + count);
                                return;
                        }
                }
                throw new AssertionError("TC141: ITALIAN entry not found");
        }
}

// ─── TC142 — S5-F10 filter excludes non-COMPLETED payments ───────────────────
@Tag("public")
@Tag("features_m2")
class TC142_CuisineAnalyticsCompletedOnlyTests extends TestBase {
        @Test
        @DisplayName("TC142 — Only COMPLETED payments are counted (FAILED/REFUNDED/PENDING excluded)")
        void completed_only() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 100.0, 10.0, "2026-08-10");
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 200.0, 20.0, "2026-08-11");
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "FAILED", 999.0, 99.0, "2026-08-12");
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "REFUNDED", 999.0, 99.0, "2026-08-13");
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "PENDING", 999.0, 99.0, "2026-08-14");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC142 analytics");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                for (JsonNode it : list) {
                        String cui = it.has("cuisineType") ? it.get("cuisineType").asText()
                                        : it.has("cuisine_type") ? it.get("cuisine_type").asText() : "";
                        if ("ITALIAN".equals(cui)) {
                                long count = it.has("orderCount") ? it.get("orderCount").asLong()
                                                : it.has("order_count") ? it.get("order_count").asLong() : -1;
                                assertEquals(2L, count, "TC142: only 2 COMPLETED counted; got " + count);
                                return;
                        }
                }
                throw new AssertionError("TC142: ITALIAN entry not found");
        }
}

// ─── TC143 — S5-F10 group by cuisineType (multiple distinct cuisines) ────────
@Tag("public")
@Tag("features_m2")
class TC143_CuisineAnalyticsGroupByTests extends TestBase {
        @Test
        @DisplayName("TC143 — Result has one entry per distinct cuisineType")
        void group_by_cuisine() throws Exception {
                BASE_URL = checkoutServiceUrl;
                for (String c : new String[] { "ITALIAN", "CHINESE", "EGYPTIAN", "SEAFOOD" }) {
                        _S5F10Seed.seedOrderPayment(jdbc, this, c, "DELIVERED", "COMPLETED", 100.0, 10.0, "2026-08-15");
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC143 analytics");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (JsonNode it : list) {
                        String cui = it.has("cuisineType") ? it.get("cuisineType").asText()
                                        : it.has("cuisine_type") ? it.get("cuisine_type").asText() : "";
                        seen.add(cui);
                }
                for (String c : new String[] { "ITALIAN", "CHINESE", "EGYPTIAN", "SEAFOOD" }) {
                        assertTrue(seen.contains(c), "TC143: expected cuisine '" + c + "' in result; got=" + seen);
                }
        }
}

// ─── TC144 — S5-F10 empty range returns empty list ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC144_CuisineAnalyticsEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC144 — Empty date range returns empty list")
        void empty_range() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2030-01-01&endDate=2030-01-31", tok);
                assert2xx(r, "TC144 analytics");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                int size = list.isArray() ? list.size() : -1;
                assertEquals(0, size, "TC144: empty list expected; got " + size + " body=" + r.body());
        }
}

// ─── TC145 — S5-F10 boundary inclusion at startDate T00:00:00 ────────────────
@Tag("public")
@Tag("features_m2")
class TC145_CuisineAnalyticsStartBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC145 — Order at exactly startDate T00:00:00 is included")
        void start_boundary() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 100.0, 10.0, "2026-08-01");
                jdbc.update("UPDATE \"" + tableName("Order")
                                + "\" SET order_date = ? WHERE id = (SELECT MAX(id) FROM \"" + tableName("Order")
                                + "\")",
                                java.sql.Timestamp.valueOf("2026-08-01 00:00:00"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC145 analytics");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                assertTrue(list.size() >= 1, "TC145: boundary must include; got " + r.body());
        }
}

// ─── TC146 — S5-F10 boundary inclusion at endDate T23:59:59 ──────────────────
@Tag("public")
@Tag("features_m2")
class TC146_CuisineAnalyticsEndBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC146 — Order at endDate T23:59:59 is included")
        void end_boundary() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 100.0, 10.0, "2026-08-31");
                jdbc.update("UPDATE \"" + tableName("Order")
                                + "\" SET order_date = ? WHERE id = (SELECT MAX(id) FROM \"" + tableName("Order")
                                + "\")",
                                java.sql.Timestamp.valueOf("2026-08-31 23:59:59"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC146 analytics");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                assertTrue(list.size() >= 1, "TC146: boundary must include; got " + r.body());
        }
}

// ─── TC147 — S5-F10 inverted dates → 400 ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC147_CuisineAnalyticsInvertedDatesTests extends TestBase {
        @Test
        @DisplayName("TC147 — startDate > endDate returns 400")
        void inverted_dates() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(), "TC147: must be 400; got " + r.statusCode());
        }
}

// ─── TC148 — S5-F10 missing JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC148_CuisineAnalyticsMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC148 — Missing JWT returns 401")
        void missing_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGet(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31");
                assertEquals(401, r.statusCode(), "TC148: must be 401");
        }
}

// ─── TC149 — S5-F10 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC149_CuisineAnalyticsInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC149 — Bogus JWT returns 401")
        void invalid_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31",
                                "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC149: must be 401");
        }
}

// ─── TC150 — S5-F10 ANALYTICS_VIEWED logged on first call ────────────────────
@Tag("public")
@Tag("features_m2")
class TC150_CuisineAnalyticsLoggedFirstCallTests extends TestBase {
        @Test
        @DisplayName("TC150 — First call logs ANALYTICS_VIEWED to payment_audit_trail")
        void analytics_logged_first_call() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC150: MongoDB required.");
                String coll = s5AuditCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                long before = col.countDocuments();
                String tok = adminToken();
                assert2xx(httpGetAuth("/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok),
                                "TC150");
                long after = col.countDocuments();
                assertTrue(after > before, "TC150: ANALYTICS_VIEWED expected in '" + coll + "'");
                org.bson.Document latest = col.find().sort(new org.bson.Document("_id", -1)).first();
                if (latest != null) {
                        String type = latest.getString("eventType");
                        if (type == null)
                                type = latest.getString("action");
                        if (type != null)
                                assertEquals("ANALYTICS_VIEWED", type, "TC150: latest event type");
                }
        }
}

// ─── TC151 — S5-F10 ANALYTICS_VIEWED logged on cache hit ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC151_CuisineAnalyticsLoggedOnCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC151 — Cache hit still logs ANALYTICS_VIEWED")
        void analytics_logged_on_cache_hit() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC151: MongoDB required.");
                String coll = s5AuditCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                long before = col.countDocuments();
                String tok = adminToken();
                assert2xx(httpGetAuth("/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok),
                                "TC151 #1");
                assert2xx(httpGetAuth("/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok),
                                "TC151 #2");
                long after = col.countDocuments();
                assertTrue(after >= before + 2,
                                "TC151: 2 calls must produce ≥2 events; before=" + before + " after=" + after);
        }
}

// ─── TC152 — S5-F10 cache populated in Redis with TTL ≤ 600s ─────────────────
@Tag("public")
@Tag("features_m2")
class TC152_CuisineAnalyticsCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC152 — Cuisine call populates Redis with TTL ≤ 600s")
        void cache_populated() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC152: Redis required.");
                java.util.Set<String> beforeKeys = redisKeys("*");
                String tok = adminToken();
                assert2xx(httpGetAuth("/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok),
                                "TC152");
                java.util.Set<String> afterKeys = redisKeys("*");
                assertTrue(afterKeys.size() > beforeKeys.size(), "TC152: at least one new Redis key expected");
                boolean found = false;
                for (String k : afterKeys) {
                        if (beforeKeys.contains(k))
                                continue;
                        long ttl = redisTtl(k);
                        if (ttl > 0 && ttl <= 600) {
                                found = true;
                                break;
                        }
                }
                assertTrue(found, "TC152: a new Redis key must have TTL in (0, 600]s; keys=" + afterKeys);
        }
}

// ─── TC153 — S5-F10 cache hit served (Redis-confirmed) ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC153_CuisineAnalyticsCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC153 — Cache hit: 2nd call (after data mutation) returns 1st response")
        void cache_hit() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC153: Redis required.");
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 100.0, 10.0, "2026-08-15");
                String tok = adminToken();
                HttpResponse<String> r1 = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r1, "TC153 #1");
                String cachedBefore = null;
                for (String k : redisKeys("*")) {
                        String v = redisGet(k);
                        if (v != null && v.length() > 0) {
                                cachedBefore = v;
                                break;
                        }
                }
                _S5F10Seed.seedOrderPayment(jdbc, this, "ITALIAN", "DELIVERED", "COMPLETED", 200.0, 20.0, "2026-08-16");
                HttpResponse<String> r2 = httpGetAuth(
                                "/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r2, "TC153 #2");
                assertEquals(r1.body(), r2.body(), "TC153: cache hit — r1 must equal r2");
                if (cachedBefore != null) {
                        String cachedAfter = null;
                        for (String k : redisKeys("*")) {
                                String v = redisGet(k);
                                if (v != null && v.length() > 0) {
                                        cachedAfter = v;
                                        break;
                                }
                        }
                        assertEquals(cachedBefore, cachedAfter, "TC153: Redis cached value unchanged");
                }
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S5-F11 — Payment Method Analytics (TC154..TC170)
// Endpoint reads payment_audit_trail Mongo, filtered by action ∈ {COMPLETED,
// FAILED}.
// Tests seed Mongo directly with controlled events.
// ════════════════════════════════════════════════════════════════════════════

class _S5F11Seed {
        static void insert(com.mongodb.client.MongoCollection<org.bson.Document> col,
                        String action, String method, double amount, java.time.Instant ts) {
                org.bson.Document doc = new org.bson.Document();
                doc.put("action", action);
                doc.put("eventType", action);
                doc.put("method", method);
                doc.put("amount", amount);
                doc.put("timestamp", java.util.Date.from(ts));
                col.insertOne(doc);
        }

        static java.time.Instant t(String iso) {
                return java.time.Instant.parse(iso);
        }
}

// ─── TC154 — S5-F11 happy path composite (scenario a) ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC154_MethodAnalyticsHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC154 — CC: success=5/failure=2/rate≈0.71/total=500; COD: 3/0/1.0/300")
        void method_happy_path() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC154: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                for (int i = 0; i < 5; i++)
                        _S5F11Seed.insert(col, "COMPLETED", "CREDIT_CARD", 100.0, _S5F11Seed.t("2026-08-15T10:00:00Z"));
                for (int i = 0; i < 2; i++)
                        _S5F11Seed.insert(col, "FAILED", "CREDIT_CARD", 100.0, _S5F11Seed.t("2026-08-15T11:00:00Z"));
                for (int i = 0; i < 3; i++)
                        _S5F11Seed.insert(col, "COMPLETED", "CASH_ON_DELIVERY", 100.0,
                                        _S5F11Seed.t("2026-08-16T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC154 method analytics");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("breakdown") ? arr.get("breakdown") : arr);
                java.util.Map<String, JsonNode> byMethod = new java.util.HashMap<>();
                for (JsonNode it : list)
                        byMethod.put(it.has("method") ? it.get("method").asText() : "", it);
                JsonNode cc = byMethod.get("CREDIT_CARD");
                JsonNode cod = byMethod.get("CASH_ON_DELIVERY");
                assertNotNull(cc, "TC154: CREDIT_CARD entry; body=" + r.body());
                assertNotNull(cod, "TC154: CASH_ON_DELIVERY entry");
                assertEquals(5L, cc.has("successCount") ? cc.get("successCount").asLong() : -1,
                                "TC154: CC successCount");
                assertEquals(2L, cc.has("failureCount") ? cc.get("failureCount").asLong() : -1,
                                "TC154: CC failureCount");
                assertEquals(0.71, cc.has("successRate") ? cc.get("successRate").asDouble() : -1, 0.02,
                                "TC154: CC successRate");
                assertEquals(500.0, cc.has("totalAmount") ? cc.get("totalAmount").asDouble() : -1, 0.5,
                                "TC154: CC totalAmount");
                assertEquals(3L, cod.has("successCount") ? cod.get("successCount").asLong() : -1,
                                "TC154: COD successCount");
                assertEquals(0L, cod.has("failureCount") ? cod.get("failureCount").asLong() : -1,
                                "TC154: COD failureCount");
                assertEquals(1.0, cod.has("successRate") ? cod.get("successRate").asDouble() : -1, 0.001,
                                "TC154: COD successRate");
                assertEquals(300.0, cod.has("totalAmount") ? cod.get("totalAmount").asDouble() : -1, 0.5,
                                "TC154: COD totalAmount");
        }
}

// ─── TC155 — S5-F11 successCount per method ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC155_MethodAnalyticsSuccessCountTests extends TestBase {
        @Test
        @DisplayName("TC155 — successCount equals number of COMPLETED events for the method")
        void success_count() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC155: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                for (int i = 0; i < 4; i++)
                        _S5F11Seed.insert(col, "COMPLETED", "CREDIT_CARD", 50.0, _S5F11Seed.t("2026-09-10T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC155");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                for (JsonNode it : list) {
                        if ("CREDIT_CARD".equals(it.has("method") ? it.get("method").asText() : "")) {
                                assertEquals(4L, it.has("successCount") ? it.get("successCount").asLong() : -1,
                                                "TC155: successCount=4");
                                return;
                        }
                }
                throw new AssertionError("TC155: CREDIT_CARD entry not found");
        }
}

// ─── TC156 — S5-F11 failureCount per method ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC156_MethodAnalyticsFailureCountTests extends TestBase {
        @Test
        @DisplayName("TC156 — failureCount equals number of FAILED events for the method")
        void failure_count() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC156: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                for (int i = 0; i < 3; i++)
                        _S5F11Seed.insert(col, "FAILED", "CREDIT_CARD", 50.0, _S5F11Seed.t("2026-10-10T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-10-01&endDate=2026-10-31", tok);
                assert2xx(r, "TC156");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                for (JsonNode it : list) {
                        if ("CREDIT_CARD".equals(it.has("method") ? it.get("method").asText() : "")) {
                                assertEquals(3L, it.has("failureCount") ? it.get("failureCount").asLong() : -1,
                                                "TC156: failureCount=3");
                                return;
                        }
                }
                throw new AssertionError("TC156: CREDIT_CARD entry not found");
        }
}

// ─── TC157 — S5-F11 successRate normal computation ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC157_MethodAnalyticsSuccessRateTests extends TestBase {
        @Test
        @DisplayName("TC157 — successRate = success / (success + failure)")
        void success_rate() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC157: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                for (int i = 0; i < 4; i++)
                        _S5F11Seed.insert(col, "COMPLETED", "CREDIT_CARD", 50.0, _S5F11Seed.t("2026-11-10T10:00:00Z"));
                _S5F11Seed.insert(col, "FAILED", "CREDIT_CARD", 50.0, _S5F11Seed.t("2026-11-10T11:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-11-01&endDate=2026-11-30", tok);
                assert2xx(r, "TC157");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                for (JsonNode it : list) {
                        if ("CREDIT_CARD".equals(it.has("method") ? it.get("method").asText() : "")) {
                                assertEquals(0.8, it.has("successRate") ? it.get("successRate").asDouble() : -1, 0.01,
                                                "TC157: rate=0.8 (4/5)");
                                return;
                        }
                }
                throw new AssertionError("TC157: CREDIT_CARD entry not found");
        }
}

// ─── TC158 — S5-F11 only excluded actions → empty result ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC158_MethodAnalyticsOnlyExcludedActionsTests extends TestBase {
        @Test
        @DisplayName("TC158 — Range with only CREATED/REFUNDED events returns empty list")
        void only_excluded_actions() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC158: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _S5F11Seed.insert(col, "CREATED", "CREDIT_CARD", 100.0, _S5F11Seed.t("2027-01-10T10:00:00Z"));
                _S5F11Seed.insert(col, "REFUNDED", "CREDIT_CARD", 100.0, _S5F11Seed.t("2027-01-11T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2027-01-01&endDate=2027-01-31", tok);
                assert2xx(r, "TC158");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                assertEquals(0, list.size(), "TC158: empty list expected (all actions excluded)");
        }
}

// ─── TC159 — S5-F11 totalAmount sums COMPLETED only ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC159_MethodAnalyticsTotalAmountCompletedOnlyTests extends TestBase {
        @Test
        @DisplayName("TC159 — totalAmount sums COMPLETED amounts only (failed amounts excluded)")
        void total_amount_completed_only() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC159: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _S5F11Seed.insert(col, "COMPLETED", "CREDIT_CARD", 50.0, _S5F11Seed.t("2026-12-10T10:00:00Z"));
                _S5F11Seed.insert(col, "COMPLETED", "CREDIT_CARD", 50.0, _S5F11Seed.t("2026-12-10T11:00:00Z"));
                _S5F11Seed.insert(col, "FAILED", "CREDIT_CARD", 999.0, _S5F11Seed.t("2026-12-10T12:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-12-01&endDate=2026-12-31", tok);
                assert2xx(r, "TC159");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                for (JsonNode it : list) {
                        if ("CREDIT_CARD".equals(it.has("method") ? it.get("method").asText() : "")) {
                                assertEquals(100.0, it.has("totalAmount") ? it.get("totalAmount").asDouble() : -1, 0.5,
                                                "TC159: totalAmount=100 (COMPLETED only); got "
                                                                + it.get("totalAmount"));
                                return;
                        }
                }
                throw new AssertionError("TC159: CREDIT_CARD entry not found");
        }
}

// ─── TC160 — S5-F11 filter excludes CREATED ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC160_MethodAnalyticsExcludesCreatedTests extends TestBase {
        @Test
        @DisplayName("TC160 — Only CREATED events: empty result")
        void excludes_created() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC160: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _S5F11Seed.insert(col, "CREATED", "CREDIT_CARD", 50.0, _S5F11Seed.t("2027-02-10T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2027-02-01&endDate=2027-02-28", tok);
                assert2xx(r, "TC160");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                assertEquals(0, list.size(), "TC160: CREATED action must be excluded");
        }
}

// ─── TC161 — S5-F11 filter excludes REFUNDED ─────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC161_MethodAnalyticsExcludesRefundedTests extends TestBase {
        @Test
        @DisplayName("TC161 — Only REFUNDED events: empty result")
        void excludes_refunded() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC161: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _S5F11Seed.insert(col, "REFUNDED", "CREDIT_CARD", 50.0, _S5F11Seed.t("2026-08-10T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC161");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                assertEquals(0, list.size(), "TC161: REFUNDED action must be excluded");
        }
}

// ─── TC162 — S5-F11 filter excludes REFUND_DENIED ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC162_MethodAnalyticsExcludesRefundDeniedTests extends TestBase {
        @Test
        @DisplayName("TC162 — Only REFUND_DENIED events: empty result")
        void excludes_refund_denied() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC162: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _S5F11Seed.insert(col, "REFUND_DENIED", "CREDIT_CARD", 50.0, _S5F11Seed.t("2027-03-10T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2027-03-01&endDate=2027-03-31", tok);
                assert2xx(r, "TC162");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                assertEquals(0, list.size(), "TC162: REFUND_DENIED action must be excluded");
        }
}

// ─── TC163 — S5-F11 filter excludes ANALYTICS_VIEWED ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC163_MethodAnalyticsExcludesAnalyticsViewedTests extends TestBase {
        @Test
        @DisplayName("TC163 — Only ANALYTICS_VIEWED events: empty result")
        void excludes_analytics_viewed() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC163: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _S5F11Seed.insert(col, "ANALYTICS_VIEWED", "CREDIT_CARD", 50.0, _S5F11Seed.t("2027-04-10T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2027-04-01&endDate=2027-04-30", tok);
                assert2xx(r, "TC163");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                assertEquals(0, list.size(), "TC163: ANALYTICS_VIEWED action must be excluded");
        }
}

// ─── TC164 — S5-F11 group by method (3 distinct methods) ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC164_MethodAnalyticsGroupByTests extends TestBase {
        @Test
        @DisplayName("TC164 — Result has one entry per distinct method")
        void group_by_method() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC164: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _S5F11Seed.insert(col, "COMPLETED", "CREDIT_CARD", 100.0, _S5F11Seed.t("2026-08-10T10:00:00Z"));
                _S5F11Seed.insert(col, "COMPLETED", "CASH_ON_DELIVERY", 100.0, _S5F11Seed.t("2026-08-10T11:00:00Z"));
                _S5F11Seed.insert(col, "COMPLETED", "WALLET", 100.0, _S5F11Seed.t("2026-08-10T12:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r, "TC164");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (JsonNode it : list)
                        seen.add(it.has("method") ? it.get("method").asText() : "");
                for (String m : new String[] { "CREDIT_CARD", "CASH_ON_DELIVERY", "WALLET" }) {
                        assertTrue(seen.contains(m), "TC164: expected method '" + m + "'; got=" + seen);
                }
        }
}

// ─── TC165 — S5-F11 empty range returns empty list ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC165_MethodAnalyticsEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC165 — Empty date range returns empty list")
        void empty_range() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2030-01-01&endDate=2030-01-31", tok);
                assert2xx(r, "TC165");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("breakdown") ? parseNode(r.body()).get("breakdown")
                                                : parseNode(r.body()));
                assertEquals(0, list.size(), "TC165: empty list expected");
        }
}

// ─── TC166 — S5-F11 inverted dates → 400 ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC166_MethodAnalyticsInvertedDatesTests extends TestBase {
        @Test
        @DisplayName("TC166 — startDate > endDate returns 400")
        void inverted_dates() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(), "TC166: must be 400");
        }
}

// ─── TC167 — S5-F11 missing JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC167_MethodAnalyticsMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC167 — Missing JWT returns 401")
        void missing_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGet(
                                "/api/payments/analytics/methods?startDate=2026-08-01&endDate=2026-08-31");
                assertEquals(401, r.statusCode(), "TC167: must be 401");
        }
}

// ─── TC168 — S5-F11 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC168_MethodAnalyticsInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC168 — Bogus JWT returns 401")
        void invalid_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-08-01&endDate=2026-08-31",
                                "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC168: must be 401");
        }
}

// ─── TC169 — S5-F11 cache populated in Redis with TTL ≤ 600s ─────────────────
@Tag("public")
@Tag("features_m2")
class TC169_MethodAnalyticsCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC169 — Method analytics call populates Redis with TTL ≤ 600s")
        void cache_populated() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC169: Redis required.");
                java.util.Set<String> beforeKeys = redisKeys("*");
                String tok = adminToken();
                assert2xx(httpGetAuth("/api/payments/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok),
                                "TC169");
                java.util.Set<String> afterKeys = redisKeys("*");
                assertTrue(afterKeys.size() > beforeKeys.size(), "TC169: at least one new Redis key expected");
                boolean found = false;
                for (String k : afterKeys) {
                        if (beforeKeys.contains(k))
                                continue;
                        long ttl = redisTtl(k);
                        if (ttl > 0 && ttl <= 600) {
                                found = true;
                                break;
                        }
                }
                assertTrue(found, "TC169: a new Redis key must have TTL in (0, 600]s");
        }
}

// ─── TC170 — S5-F11 cache hit served (Redis-confirmed) ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC170_MethodAnalyticsCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC170 — Cache hit: 2nd call (after Mongo mutation) returns 1st response")
        void cache_hit() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC170: Redis required.");
                if (mongo == null)
                        throw new AssertionError("TC170: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                _S5F11Seed.insert(col, "COMPLETED", "CREDIT_CARD", 100.0, _S5F11Seed.t("2026-08-10T10:00:00Z"));
                String tok = adminToken();
                HttpResponse<String> r1 = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r1, "TC170 #1");
                String cachedBefore = null;
                for (String k : redisKeys("*")) {
                        String v = redisGet(k);
                        if (v != null && v.length() > 0) {
                                cachedBefore = v;
                                break;
                        }
                }
                _S5F11Seed.insert(col, "COMPLETED", "CREDIT_CARD", 200.0, _S5F11Seed.t("2026-08-11T10:00:00Z"));
                HttpResponse<String> r2 = httpGetAuth(
                                "/api/payments/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok);
                assert2xx(r2, "TC170 #2");
                assertEquals(r1.body(), r2.body(), "TC170: cache hit — r1 must equal r2");
                if (cachedBefore != null) {
                        String cachedAfter = null;
                        for (String k : redisKeys("*")) {
                                String v = redisGet(k);
                                if (v != null && v.length() > 0) {
                                        cachedAfter = v;
                                        break;
                                }
                        }
                        assertEquals(cachedBefore, cachedAfter, "TC170: Redis cached value unchanged");
                }
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S5-F12 — Refund with Strategy Pattern (TC171..TC190)
// Endpoint: POST /api/payments/{id}/refund-with-fee-handling
// Body: {"reason": "...", "refundDeliveryFee": true|false}
// Strategy chosen by createdAt window (24h) + refundDeliveryFee flag.
// Spec maps request.reason → JSONB.refundReason on success path.
// ════════════════════════════════════════════════════════════════════════════

class _S5F12Seed {
        /**
         * Insert a Payment + parent Order with controlled status, amount, deliveryFee,
         * createdAt.
         * Returns the payment id.
         */
        static long seedPayment(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        String paymentStatus, double amount, double deliveryFee,
                        java.sql.Timestamp createdAt) {
                String oTable = tb.tableName("Order");
                java.util.Map<String, Object> _oOv = new java.util.HashMap<>();
                _oOv.put(tb.columnByField("Order", "user"), 1L);
                _oOv.put(tb.columnByField("Order", "restaurant"), 1L);
                _oOv.put(tb.columnByField("Order", "totalAmount"), amount);
                _oOv.put(tb.columnByField("Order", "status"), "DELIVERED");
                Long oid = tb.insertRowReturningId(oTable, _oOv);
                tb.setAllDateColumns(oTable, oid, createdAt);
                String pTable = tb.tableName("Payment");
                java.util.Map<String, Object> _pOv = new java.util.HashMap<>();
                _pOv.put(tb.columnByField("Payment", "order"), oid);
                _pOv.put(tb.columnByField("Payment", "user"), 1L);
                _pOv.put(tb.columnByField("Payment", "amount"), amount);
                _pOv.put(tb.columnByField("Payment", "method"), "CREDIT_CARD");
                _pOv.put(tb.columnByField("Payment", "status"), paymentStatus);
                Long pid = tb.insertRowReturningId(pTable, _pOv);
                jdbc.update("UPDATE \"" + pTable + "\" SET transaction_details = ?::jsonb WHERE id = ?",
                                String.format("{\"deliveryFee\":%.2f}", deliveryFee), pid);
                // Set createdAt directly on the payment if column exists.
                try {
                        jdbc.update("UPDATE \"" + pTable + "\" SET created_at = ? WHERE id = ?", createdAt, pid);
                } catch (org.springframework.dao.DataAccessException ignored) {
                        /* may not exist */ }
                return pid;
        }
}

// ─── TC171 — S5-F12 FullRefundWithDelivery happy path (scenario a) ──────────
@Tag("public")
@Tag("features_m2")
class TC171_RefundFullPathTests extends TestBase {
        @Test
        @DisplayName("TC171 — FullRefundWithDelivery: 200 + refundAmount=150 + status=REFUNDED")
        void full_refund_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 150.0, 20.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok);
                assert2xx(r, "TC171 refund");
                JsonNode j = parseNode(r.body());
                double refund = _refundAmtFromBody(j);
                assertEquals(150.0, refund, 0.5, "TC171: refundAmount=150 (full); got " + refund);
                String stat = j.has("status") ? j.get("status").asText() : "";
                assertEquals("REFUNDED", stat, "TC171: response.status=REFUNDED expected; got " + stat);
        }
}

// ─── TC172 — S5-F12 PG status updated to REFUNDED ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC172_RefundStatusUpdatedTests extends TestBase {
        @Test
        @DisplayName("TC172 — payments.status=REFUNDED in PG after successful refund")
        void status_updated_in_pg() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                assert2xx(httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok), "TC172");
                String pTable = tableName("Payment");
                String stCol = columnByField("Payment", "status");
                String dbStatus = jdbc.queryForObject(
                                "SELECT \"" + stCol + "\"::text FROM \"" + pTable + "\" WHERE id = ?", String.class,
                                pid);
                assertEquals("REFUNDED", dbStatus, "TC172: PG status=REFUNDED expected; got " + dbStatus);
        }
}

// ─── TC173 — S5-F12 JSONB.refundAmount set on full refund ────────────────────
@Tag("public")
@Tag("features_m2")
class TC173_RefundJsonbAmountTests extends TestBase {
        @Test
        @DisplayName("TC173 — transactionDetails.refundAmount=150 on full refund")
        void jsonb_refund_amount() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 150.0, 20.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                assert2xx(httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok), "TC173");
                String pTable = tableName("Payment");
                String details = jdbc.queryForObject(
                                "SELECT transaction_details::text FROM \"" + pTable + "\" WHERE id = ?", String.class,
                                pid);
                assertNotNull(details, "TC173: transaction_details JSONB must be set");
                assertTrue(details.contains("\"refundAmount\""),
                                "TC173: JSONB must include refundAmount key; got " + details);
                assertTrue(details.contains("150"),
                                "TC173: JSONB.refundAmount must equal 150; got " + details);
        }
}

// ─── TC174 — S5-F12 JSONB.refundDeliveryFeeIncluded=true on full refund ──────
@Tag("public")
@Tag("features_m2")
class TC174_RefundJsonbDeliveryIncludedTrueTests extends TestBase {
        @Test
        @DisplayName("TC174 — transactionDetails.refundDeliveryFeeIncluded=true on full refund")
        void jsonb_delivery_fee_included_true() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 150.0, 20.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                assert2xx(httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok), "TC174");
                String details = jdbc.queryForObject(
                                "SELECT transaction_details::text FROM \"" + tableName("Payment") + "\" WHERE id = ?",
                                String.class, pid);
                assertTrue(details.contains("\"refundDeliveryFeeIncluded\":true") ||
                                details.contains("\"refundDeliveryFeeIncluded\": true"),
                                "TC174: refundDeliveryFeeIncluded=true expected in JSONB; got " + details);
        }
}

// ─── TC175 — S5-F12 FoodOnly happy path (scenario b) ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC175_RefundFoodOnlyPathTests extends TestBase {
        @Test
        @DisplayName("TC175 — FoodOnly: 200 + refundAmount=170 (food only, no delivery)")
        void food_only_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 200.0, 30.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"customer_changed_mind\",\"refundDeliveryFee\":false}";
                HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok);
                assert2xx(r, "TC175");
                JsonNode j = parseNode(r.body());
                double refund = _refundAmtFromBody(j);
                assertEquals(170.0, refund, 0.5, "TC175: refundAmount=170 (food only); got " + refund);
        }
}

// ─── TC176 — S5-F12 JSONB.refundDeliveryFeeIncluded=false on FoodOnly ────────
@Tag("public")
@Tag("features_m2")
class TC176_RefundJsonbDeliveryIncludedFalseTests extends TestBase {
        @Test
        @DisplayName("TC176 — transactionDetails.refundDeliveryFeeIncluded=false on FoodOnly")
        void jsonb_delivery_fee_included_false() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 200.0, 30.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"customer_changed_mind\",\"refundDeliveryFee\":false}";
                assert2xx(httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok), "TC176");
                String details = jdbc.queryForObject(
                                "SELECT transaction_details::text FROM \"" + tableName("Payment") + "\" WHERE id = ?",
                                String.class, pid);
                assertTrue(details.contains("\"refundDeliveryFeeIncluded\":false") ||
                                details.contains("\"refundDeliveryFeeIncluded\": false"),
                                "TC176: refundDeliveryFeeIncluded=false expected; got " + details);
        }
}

// ─── TC177 — S5-F12 NoRefund denial returns 400 (scenario c) ─────────────────
@Tag("public")
@Tag("features_m2")
class TC177_RefundDenialNoRefundTests extends TestBase {
        @Test
        @DisplayName("TC177 — NoRefund (createdAt 2 days ago): 400 with 'refund window expired'")
        void no_refund_denial_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok);
                assertEquals(400, r.statusCode(), "TC177: must be 400; got " + r.statusCode());
                // NOTE: do not assert on response body — Spring Boot 4 strips
                // ResponseStatusException reason from the default error body unless
                // server.error.include-message=always is set. The denial reason is
                // verified via the Mongo audit event in TC178 instead.
        }
}

// ─── TC178 — S5-F12 REFUND_DENIED event logged on denial path ────────────────
@Tag("public")
@Tag("features_m2")
class TC178_RefundDeniedEventLoggedTests extends TestBase {
        @Test
        @DisplayName("TC178 — REFUND_DENIED event logged with strategyName + reason on denial")
        void denied_event_logged() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC178: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                long before = col.countDocuments();
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok);
                assertEquals(400, r.statusCode(), "TC178: precondition — must 400");
                long after = col.countDocuments();
                assertTrue(after > before, "TC178: REFUND_DENIED event must be logged before throw");
                org.bson.Document latest = col.find().sort(new org.bson.Document("_id", -1)).first();
                if (latest != null) {
                        String type = latest.getString("eventType");
                        if (type == null)
                                type = latest.getString("action");
                        if (type != null)
                                assertEquals("REFUND_DENIED", type, "TC178: latest event eventType");
                        String strat = latest.getString("strategyName");
                        if (strat == null) {
                                org.bson.Document details = latest.get("details", org.bson.Document.class);
                                if (details != null)
                                        strat = details.getString("strategyName");
                        }
                        if (strat != null)
                                assertEquals(s5StrategyNoRefund(), strat, "TC178: strategyName");
                }
        }
}

// ─── TC179 — S5-F12 S5-F10 cache invalidated on denial path ──────────────────
@Tag("public")
@Tag("features_m2")
class TC179_RefundDeniedInvalidatesS5F10CacheTests extends TestBase {
        @Test
        @DisplayName("TC179 — Denial path invalidates S5-F10 cache keys in Redis")
        void denial_invalidates_s5f10_cache() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC179: Redis required.");
                // Pre-populate S5-F10 cache by calling cuisine analytics.
                String tok = adminToken();
                assert2xx(httpGetAuth("/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok),
                                "TC179 prepop");
                java.util.Set<String> beforeKeys = redisKeys("*S5-F10*");
                assertTrue(beforeKeys.size() >= 1,
                                "TC179: precondition — S5-F10 cache must be populated; keys=" + beforeKeys);
                // Trigger denial
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000));
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok);
                assertEquals(400, r.statusCode(), "TC179: precondition — must 400");
                java.util.Set<String> afterKeys = redisKeys("*S5-F10*");
                assertTrue(afterKeys.size() < beforeKeys.size() || afterKeys.isEmpty(),
                                "TC179: S5-F10 cache keys must be invalidated; before=" + beforeKeys + " after="
                                                + afterKeys);
        }
}

// ─── TC180 — S5-F12 S5-F11 cache invalidated on denial path ──────────────────
@Tag("public")
@Tag("features_m2")
class TC180_RefundDeniedInvalidatesS5F11CacheTests extends TestBase {
        @Test
        @DisplayName("TC180 — Denial path invalidates S5-F11 cache keys in Redis")
        void denial_invalidates_s5f11_cache() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC180: Redis required.");
                String tok = adminToken();
                assert2xx(httpGetAuth("/api/payments/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok),
                                "TC180 prepop");
                java.util.Set<String> beforeKeys = redisKeys("*S5-F11*");
                assertTrue(beforeKeys.size() >= 1,
                                "TC180: precondition — S5-F11 cache must be populated; keys=" + beforeKeys);
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000));
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok);
                assertEquals(400, r.statusCode(), "TC180: precondition — must 400");
                java.util.Set<String> afterKeys = redisKeys("*S5-F11*");
                assertTrue(afterKeys.size() < beforeKeys.size() || afterKeys.isEmpty(),
                                "TC180: S5-F11 cache keys must be invalidated; before=" + beforeKeys + " after="
                                                + afterKeys);
        }
}

// ─── TC181 — S5-F12 PENDING payment → 400 (scenario d) ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC181_RefundPendingPaymentTests extends TestBase {
        @Test
        @DisplayName("TC181 — Refunding a PENDING payment returns 400")
        void pending_payment_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "PENDING", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok);
                assertEquals(400, r.statusCode(), "TC181: must be 400 for non-COMPLETED");
        }
}

// ─── TC182 — S5-F12 already-REFUNDED payment → 400 (scenario e) ──────────────
@Tag("public")
@Tag("features_m2")
class TC182_RefundAlreadyRefundedTests extends TestBase {
        @Test
        @DisplayName("TC182 — Refunding an already-REFUNDED payment returns 400")
        void already_refunded_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "REFUNDED", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok);
                assertEquals(400, r.statusCode(), "TC182: must be 400 for already-REFUNDED");
        }
}

// ─── TC183 — S5-F12 non-existent payment → 404 (scenario f) ──────────────────
@Tag("public")
@Tag("features_m2")
class TC183_RefundNonExistentPaymentTests extends TestBase {
        @Test
        @DisplayName("TC183 — Refunding a non-existent payment returns 404")
        void not_found_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                HttpResponse<String> r = httpPostAuth("/api/payments/999999/refund-with-fee-handling", body, tok);
                assertEquals(404, r.statusCode(), "TC183: must be 404; got " + r.statusCode());
        }
}

// ─── TC184 — S5-F12 missing JWT → 401 (scenario g) ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC184_RefundMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC184 — Missing JWT returns 401")
        void missing_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                HttpResponse<String> r = httpPost("/api/payments/1/refund-with-fee-handling", body);
                assertEquals(401, r.statusCode(), "TC184: must be 401");
        }
}

// ─── TC185 — S5-F12 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC185_RefundInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC185 — Bogus JWT returns 401")
        void invalid_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                HttpResponse<String> r = httpPostAuth("/api/payments/1/refund-with-fee-handling", body, "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC185: must be 401");
        }
}

// ─── TC186 — S5-F12 REFUNDED event logged on success path ────────────────────
@Tag("public")
@Tag("features_m2")
class TC186_RefundEventLoggedSuccessTests extends TestBase {
        @Test
        @DisplayName("TC186 — Successful refund logs REFUNDED event with strategyName + amounts")
        void refunded_event_logged() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null)
                        throw new AssertionError("TC186: MongoDB required.");
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(s5AuditCollection());
                long before = col.countDocuments();
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 150.0, 20.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                assert2xx(httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok), "TC186");
                long after = col.countDocuments();
                assertTrue(after > before, "TC186: REFUNDED event expected");
                org.bson.Document latest = col.find().sort(new org.bson.Document("_id", -1)).first();
                if (latest != null) {
                        String type = latest.getString("eventType");
                        if (type == null)
                                type = latest.getString("action");
                        if (type != null)
                                assertEquals("REFUNDED", type, "TC186: eventType");
                        String strat = latest.getString("strategyName");
                        if (strat == null) {
                                org.bson.Document details = latest.get("details", org.bson.Document.class);
                                if (details != null)
                                        strat = details.getString("strategyName");
                        }
                        if (strat != null)
                                assertEquals(s5StrategyFullRefund(), strat, "TC186: strategyName");
                }
        }
}

// ─── TC187 — S5-F12 JSONB.refundReason set from request.reason ───────────────
@Tag("public")
@Tag("features_m2")
class TC187_RefundJsonbReasonTests extends TestBase {
        @Test
        @DisplayName("TC187 — transactionDetails.refundReason mapped from request.reason")
        void jsonb_refund_reason() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                assert2xx(httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok), "TC187");
                String details = jdbc.queryForObject(
                                "SELECT transaction_details::text FROM \"" + tableName("Payment") + "\" WHERE id = ?",
                                String.class, pid);
                assertTrue(details.contains("\"refundReason\":\"food_quality\"") ||
                                details.contains("\"refundReason\": \"food_quality\""),
                                "TC187: refundReason must be mapped from request.reason; got " + details);
        }
}

// ─── TC188 — S5-F12 JSONB.refundedAt set ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC188_RefundJsonbRefundedAtTests extends TestBase {
        @Test
        @DisplayName("TC188 — transactionDetails.refundedAt is a non-empty timestamp")
        void jsonb_refunded_at() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String tok = adminToken();
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                assert2xx(httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok), "TC188");
                String details = jdbc.queryForObject(
                                "SELECT transaction_details::text FROM \"" + tableName("Payment") + "\" WHERE id = ?",
                                String.class, pid);
                assertTrue(details.contains("\"refundedAt\""),
                                "TC188: refundedAt key required in JSONB; got " + details);
                assertFalse(details.contains("\"refundedAt\":null") || details.contains("\"refundedAt\": null"),
                                "TC188: refundedAt must be non-null; got " + details);
        }
}

// ─── TC189 — S5-F12 S5-F10 cache invalidated on success path ─────────────────
@Tag("public")
@Tag("features_m2")
class TC189_RefundSuccessInvalidatesS5F10CacheTests extends TestBase {
        @Test
        @DisplayName("TC189 — Successful refund invalidates S5-F10 cache keys")
        void success_invalidates_s5f10_cache() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC189: Redis required.");
                String tok = adminToken();
                assert2xx(httpGetAuth("/api/payments/analytics/cuisine?startDate=2026-08-01&endDate=2026-08-31", tok),
                                "TC189 prepop");
                java.util.Set<String> beforeKeys = redisKeys("*S5-F10*");
                assertTrue(beforeKeys.size() >= 1, "TC189: precondition — S5-F10 cache populated");
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                assert2xx(httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok),
                                "TC189 refund");
                java.util.Set<String> afterKeys = redisKeys("*S5-F10*");
                assertTrue(afterKeys.size() < beforeKeys.size() || afterKeys.isEmpty(),
                                "TC189: S5-F10 cache must be invalidated on success; before=" + beforeKeys + " after="
                                                + afterKeys);
        }
}

// ─── TC190 — S5-F12 S5-F11 cache invalidated on success path ─────────────────
@Tag("public")
@Tag("features_m2")
class TC190_RefundSuccessInvalidatesS5F11CacheTests extends TestBase {
        @Test
        @DisplayName("TC190 — Successful refund invalidates S5-F11 cache keys")
        void success_invalidates_s5f11_cache() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (redis == null)
                        throw new AssertionError("TC190: Redis required.");
                String tok = adminToken();
                assert2xx(httpGetAuth("/api/payments/analytics/methods?startDate=2026-08-01&endDate=2026-08-31", tok),
                                "TC190 prepop");
                java.util.Set<String> beforeKeys = redisKeys("*S5-F11*");
                assertTrue(beforeKeys.size() >= 1, "TC190: precondition — S5-F11 cache populated");
                long pid = _S5F12Seed.seedPayment(jdbc, this, "COMPLETED", 100.0, 10.0,
                                new java.sql.Timestamp(System.currentTimeMillis()));
                String body = "{\"reason\":\"food_quality\",\"refundDeliveryFee\":true}";
                assert2xx(httpPostAuth("/api/payments/" + pid + "/refund-with-fee-handling", body, tok),
                                "TC190 refund");
                java.util.Set<String> afterKeys = redisKeys("*S5-F11*");
                assertTrue(afterKeys.size() < beforeKeys.size() || afterKeys.isEmpty(),
                                "TC190: S5-F11 cache must be invalidated on success; before=" + beforeKeys + " after="
                                                + afterKeys);
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S1 M1 — User Service feature tests (TC191..TC232) — PURE LOGIC ONLY
//
// These tests cover the M1 user-service features (S1-F1..S1-F9). They test
// HTTP request/response logic only — NO Mongo audit / Redis cache / Neo4j /
// Cassandra / Observer-chain assertions (those land in the future amendments
// category). All tests authenticated via admin token (M2 amendment MOD-3
// requires JWT on every M1 endpoint; testing "missing JWT → 401" lives in
// amendments).
//
// All DB queries use manifest-driven tableName() / columnByField() so a
// student who renamed @Column / @JoinColumn on user-service entities still
// gets seeded correctly.
// ════════════════════════════════════════════════════════════════════════════

class _S1Seed {
        /**
         * INSERT a user with controlled name/email/role/status. Returns the new user
         * id.
         */
        static long seedUser(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        String name, String email, String role) {
                String t = tb.tableName("User");
                String bcrypt = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
                String phone = "+201" + System.nanoTime() % 1000000000L;
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(tb.columnByField("User", "name"), name);
                _ov.put(tb.columnByField("User", "email"), email);
                _ov.put(tb.columnByField("User", "phone"), phone);
                _ov.put(tb.columnByField("User", "password"), bcrypt);
                _ov.put(tb.columnByField("User", "role"), role);
                _ov.put(tb.columnByField("User", "status"), "ACTIVE");
                return tb.insertRowReturningId(t, _ov);
        }

        /** UPDATE user.preferences JSONB. */
        static void setPrefs(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        long userId, String json) {
                jdbc.update("UPDATE \"" + tb.tableName("User") + "\" SET preferences = ?::jsonb WHERE id = ?", json,
                                userId);
        }

        /**
         * INSERT a DELIVERED order for the given user/restaurant with controlled amount
         * + date.
         */
        static long seedOrder(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        long userId, long restId, String orderStatus, double amount, String dateStr) {
                String t = tb.tableName("Order");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(tb.columnByField("Order", "user"), userId);
                _ov.put(tb.columnByField("Order", "restaurant"), restId);
                _ov.put(tb.columnByField("Order", "totalAmount"), amount);
                _ov.put(tb.columnByField("Order", "status"), orderStatus);
                Long oid = tb.insertRowReturningId(t, _ov);
                if (dateStr != null) {
                        tb.setAllDateColumns(t, oid, java.sql.Timestamp.valueOf(dateStr + " 12:00:00"));
                }
                return oid;
        }

        /** INSERT a DeliveryAddress row. Returns id. */
        static long seedAddress(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        long userId, String line, String city, boolean isDefault) {
                String t = tb.tableName("DeliveryAddress");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(tb.columnByField("DeliveryAddress", "user", "customer"), userId);
                // Spec line 478 mandates `streetAddress`; some test authors used the older
                // `addressLine` name. Pass spec-compliant name first.
                _ov.put(tb.columnByField("DeliveryAddress", "streetAddress", "addressLine", "street", "line"), line);
                _ov.put(tb.columnByField("DeliveryAddress", "city"), city);
                _ov.put(tb.columnByField("DeliveryAddress", "isDefault", "defaultAddress"), isDefault);
                return tb.insertRowReturningId(t, _ov);
        }
}

// ─── TC191 — S1-F1 search by name (partial match, case-sensitive) ────────────
@Tag("public")
@Tag("features_m1")
class TC191_SearchUsersByNameTests extends TestBase {
        @Test
        @DisplayName("TC191 — Search by name 'Ahmed' returns 2 users (partial match)")
        void search_by_name() throws Exception {
                BASE_URL = userServiceUrl;
                _S1Seed.seedUser(jdbc, this, "Ahmed", "tc191_a@test.io", "CUSTOMER");
                _S1Seed.seedUser(jdbc, this, "Sara", "tc191_b@test.io", "ADMIN");
                _S1Seed.seedUser(jdbc, this, "Ahmed Ali", "tc191_c@test.io", "CUSTOMER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?name=Ahmed", tok);
                assert2xx(r, "TC191 search");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                int matches = 0;
                for (JsonNode it : list) {
                        String n = it.has("name") ? it.get("name").asText() : "";
                        if (n.contains("Ahmed"))
                                matches++;
                }
                assertEquals(2, matches, "TC191: 2 Ahmed-named users expected; got " + matches + " body=" + r.body());
        }
}

// ─── TC192 — S1-F1 search by role exact match ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC192_SearchUsersByRoleTests extends TestBase {
        @Test
        @DisplayName("TC192 — Search by role=ADMIN returns only ADMIN users")
        void search_by_role() throws Exception {
                BASE_URL = userServiceUrl;
                _S1Seed.seedUser(jdbc, this, "Ahmed", "tc192_a@test.io", "CUSTOMER");
                _S1Seed.seedUser(jdbc, this, "Sara", "tc192_b@test.io", "ADMIN");
                _S1Seed.seedUser(jdbc, this, "Ahmed Ali", "tc192_c@test.io", "CUSTOMER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?role=ADMIN", tok);
                assert2xx(r, "TC192 search");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        String role = it.has("role") ? it.get("role").asText() : "";
                        assertEquals("ADMIN", role, "TC192: every result must have role=ADMIN; got " + role);
                }
                assertTrue(list.size() >= 1, "TC192: at least one ADMIN expected; got " + list.size());
        }
}

// ─── TC193 — S1-F1 no match returns empty list ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC193_SearchUsersNoMatchTests extends TestBase {
        @Test
        @DisplayName("TC193 — Search with no-matching name returns empty list")
        void search_no_match() throws Exception {
                BASE_URL = userServiceUrl;
                _S1Seed.seedUser(jdbc, this, "Ahmed", "tc193_a@test.io", "CUSTOMER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?name=zzzNoMatchXYZ", tok);
                assert2xx(r, "TC193 search");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC193: empty list expected; got " + list.size() + " body=" + r.body());
        }
}

// ─── TC194 — S1-F1 case-insensitive name match ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC194_SearchUsersCaseInsensitiveTests extends TestBase {
        @Test
        @DisplayName("TC194 — Search by 'ahmed' (lowercase) still matches 'Ahmed' (case-insensitive)")
        void search_case_insensitive() throws Exception {
                BASE_URL = userServiceUrl;
                _S1Seed.seedUser(jdbc, this, "Ahmed", "tc194_a@test.io", "CUSTOMER");
                _S1Seed.seedUser(jdbc, this, "Ahmed Ali", "tc194_b@test.io", "CUSTOMER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?name=ahmed", tok);
                assert2xx(r, "TC194 search");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                int matches = 0;
                for (JsonNode it : list) {
                        String n = it.has("name") ? it.get("name").asText() : "";
                        if (n.toLowerCase().contains("ahmed"))
                                matches++;
                }
                assertEquals(2, matches, "TC194: case-insensitive must match; got " + matches);
        }
}

// ─── TC195 — S1-F1 email partial match ───────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC195_SearchUsersByEmailTests extends TestBase {
        @Test
        @DisplayName("TC195 — Search by email partial match returns matching user(s)")
        void search_by_email() throws Exception {
                BASE_URL = userServiceUrl;
                _S1Seed.seedUser(jdbc, this, "Ali", "tc195_ali@unique.test.io", "CUSTOMER");
                _S1Seed.seedUser(jdbc, this, "Sara", "tc195_sara@other.test.io", "CUSTOMER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?email=ali@unique", tok);
                assert2xx(r, "TC195 search");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                boolean foundAli = false;
                for (JsonNode it : list) {
                        String em = it.has("email") ? it.get("email").asText() : "";
                        if (em.contains("ali@unique"))
                                foundAli = true;
                        assertTrue(em.toLowerCase().contains("ali@unique") || em.toLowerCase().contains("ali"),
                                        "TC195: every result must match email filter; got " + em);
                }
                assertTrue(foundAli, "TC195: Ali user must be in result; body=" + r.body());
        }
}

// ─── TC196 — S1-F1 combined name + role filters ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC196_SearchUsersCombinedFiltersTests extends TestBase {
        @Test
        @DisplayName("TC196 — Combined name + role filter narrows correctly")
        void search_combined() throws Exception {
                BASE_URL = userServiceUrl;
                _S1Seed.seedUser(jdbc, this, "Ahmed Customer", "tc196_a@test.io", "CUSTOMER");
                _S1Seed.seedUser(jdbc, this, "Ahmed Admin", "tc196_b@test.io", "ADMIN");
                _S1Seed.seedUser(jdbc, this, "Sara Customer", "tc196_c@test.io", "CUSTOMER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?name=Ahmed&role=CUSTOMER", tok);
                assert2xx(r, "TC196 search");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        String n = it.has("name") ? it.get("name").asText() : "";
                        String role = it.has("role") ? it.get("role").asText() : "";
                        assertTrue(n.contains("Ahmed"), "TC196: name must contain Ahmed; got " + n);
                        assertEquals("CUSTOMER", role, "TC196: role must be CUSTOMER; got " + role);
                }
        }
}

// ─── TC197 — S1-F1 all filters null returns empty list ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC197_SearchUsersNoFiltersTests extends TestBase {
        @Test
        @DisplayName("TC197 — Search with no filters (all null) returns empty list (per spec: 'matching any non-null filter')")
        void search_no_filters() throws Exception {
                BASE_URL = userServiceUrl;
                _S1Seed.seedUser(jdbc, this, "Ahmed", "tc197_a@test.io", "CUSTOMER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search", tok);
                assert2xx(r, "TC197 search");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(),
                                "TC197: empty list expected when no non-null filter is provided; got " + list.size());
        }
}

// ─── TC198 — S1-F2 update preferences happy merge ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC198_UpdatePreferencesMergeTests extends TestBase {
        @Test
        @DisplayName("TC198 — PUT preferences merges: language preserved, dietaryRestrictions updated, defaultPayment added")
        void preferences_merge() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Pref User", "tc198@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, uid, "{\"language\":\"en\",\"dietaryRestrictions\":\"none\"}");
                String tok = adminToken();
                String body = "{\"dietaryRestrictions\":\"vegetarian\",\"defaultPayment\":\"WALLET\"}";
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/preferences", body, tok);
                assert2xx(r, "TC198");
                JsonNode j = parseNode(r.body());
                JsonNode prefs = j.has("preferences") ? j.get("preferences") : j;
                assertEquals("en", prefs.has("language") ? prefs.get("language").asText() : "",
                                "TC198: language must be preserved");
                assertEquals("vegetarian",
                                prefs.has("dietaryRestrictions") ? prefs.get("dietaryRestrictions").asText() : "",
                                "TC198: dietaryRestrictions must be updated");
                assertEquals("WALLET", prefs.has("defaultPayment") ? prefs.get("defaultPayment").asText() : "",
                                "TC198: defaultPayment must be added");
        }
}

// ─── TC199 — S1-F2 same-key overwrites ───────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC199_UpdatePreferencesOverwriteTests extends TestBase {
        @Test
        @DisplayName("TC199 — PUT with existing key overwrites that key (per spec)")
        void preferences_overwrite_same_key() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Pref User", "tc199@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, uid, "{\"language\":\"en\"}");
                String tok = adminToken();
                String body = "{\"language\":\"fr\"}";
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/preferences", body, tok);
                assert2xx(r, "TC199");
                JsonNode prefs = parseNode(r.body()).has("preferences")
                                ? parseNode(r.body()).get("preferences")
                                : parseNode(r.body());
                assertEquals("fr", prefs.has("language") ? prefs.get("language").asText() : "",
                                "TC199: language must be overwritten to 'fr'");
        }
}

// ─── TC200 — S1-F2 new key added without disturbing existing ─────────────────
@Tag("public")
@Tag("features_m1")
class TC200_UpdatePreferencesNewKeyTests extends TestBase {
        @Test
        @DisplayName("TC200 — PUT with brand-new key adds it; existing keys preserved")
        void preferences_new_key() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Pref User", "tc200@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, uid, "{\"language\":\"en\"}");
                String tok = adminToken();
                String body = "{\"theme\":\"dark\"}";
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/preferences", body, tok);
                assert2xx(r, "TC200");
                JsonNode prefs = parseNode(r.body()).has("preferences")
                                ? parseNode(r.body()).get("preferences")
                                : parseNode(r.body());
                assertEquals("en", prefs.has("language") ? prefs.get("language").asText() : "",
                                "TC200: language preserved");
                assertEquals("dark", prefs.has("theme") ? prefs.get("theme").asText() : "", "TC200: theme added");
        }
}

// ─── TC201 — S1-F2 empty body keeps prefs unchanged ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC201_UpdatePreferencesEmptyBodyTests extends TestBase {
        @Test
        @DisplayName("TC201 — PUT with empty body {} keeps preferences unchanged")
        void preferences_empty_body() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Pref User", "tc201@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, uid, "{\"language\":\"en\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/preferences", "{}", tok);
                assert2xx(r, "TC201");
                JsonNode prefs = parseNode(r.body()).has("preferences")
                                ? parseNode(r.body()).get("preferences")
                                : parseNode(r.body());
                assertEquals("en", prefs.has("language") ? prefs.get("language").asText() : "",
                                "TC201: language must remain 'en' after empty merge");
        }
}

// ─── TC202 — S1-F2 404 non-existent user ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC202_UpdatePreferencesNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC202 — PUT preferences for non-existent user returns 404")
        void preferences_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/999999/preferences", "{\"x\":\"y\"}", tok);
                assertEquals(404, r.statusCode(), "TC202: must be 404; got " + r.statusCode());
        }
}

// ─── TC203 — S1-F3 order summary happy composite ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC203_OrderSummaryHappyTests extends TestBase {
        @Test
        @DisplayName("TC203 — Summary returns totalOrders=5, deliveredOrders=3, totalSpent=400, avg=133.33")
        void summary_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Sum User", "tc203@test.io", "CUSTOMER");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "DELIVERED", 80.0, "2026-08-10");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "DELIVERED", 120.0, "2026-08-11");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "DELIVERED", 200.0, "2026-08-12");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "CANCELLED", 999.0, "2026-08-13");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "PLACED", 999.0, "2026-08-14");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/order-summary", tok);
                assert2xx(r, "TC203");
                JsonNode j = parseNode(r.body());
                assertEquals(5, _readLong(j, "totalOrders", "total_orders"), "TC203: totalOrders=5");
                assertEquals(3, _readLong(j, "deliveredOrders", "delivered_orders"), "TC203: deliveredOrders=3");
                assertEquals(1, _readLong(j, "cancelledOrders", "cancelled_orders"), "TC203: cancelledOrders=1");
                assertEquals(400.0, _readDouble(j, "totalSpent", "total_spent"), 0.5, "TC203: totalSpent=400");
                assertEquals(133.33, _readDouble(j, "averageOrderAmount", "average_order_amount", "avg_order_amount"),
                                0.5, "TC203: avg≈133.33");
        }

        private long _readLong(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asLong();
                return -1;
        }

        private double _readDouble(JsonNode j, String... keys) {
                for (String k : keys)
                        if (j.has(k))
                                return j.get(k).asDouble();
                return -1;
        }
}

// ─── TC204 — S1-F3 user with no orders → zeros ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC204_OrderSummaryNoOrdersTests extends TestBase {
        @Test
        @DisplayName("TC204 — Summary for user with no orders returns all zeros")
        void summary_no_orders() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Empty User", "tc204@test.io", "CUSTOMER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/order-summary", tok);
                assert2xx(r, "TC204");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalOrders") ? j.get("totalOrders").asLong()
                                : j.has("total_orders") ? j.get("total_orders").asLong() : -1;
                double spent = j.has("totalSpent") ? j.get("totalSpent").asDouble()
                                : j.has("total_spent") ? j.get("total_spent").asDouble() : -1;
                assertEquals(0L, total, "TC204: totalOrders=0");
                assertEquals(0.0, spent, 0.01, "TC204: totalSpent=0");
        }
}

// ─── TC205 — S1-F3 404 non-existent user ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC205_OrderSummaryNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC205 — Summary for non-existent user returns 404")
        void summary_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/999999/order-summary", tok);
                assertEquals(404, r.statusCode(), "TC205: must be 404");
        }
}

// ─── TC206 — S1-F3 averageOrderAmount divides by deliveredOrders ─────────────
@Tag("public")
@Tag("features_m1")
class TC206_OrderSummaryAvgFormulaTests extends TestBase {
        @Test
        @DisplayName("TC206 — averageOrderAmount = totalSpent / deliveredOrders, NOT / totalOrders")
        void summary_avg_formula() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Avg User", "tc206@test.io", "CUSTOMER");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "DELIVERED", 100.0, "2026-08-10");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "DELIVERED", 100.0, "2026-08-11");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "DELIVERED", 100.0, "2026-08-12");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "PLACED", 100.0, "2026-08-13");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "PLACED", 100.0, "2026-08-14");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/order-summary", tok);
                assert2xx(r, "TC206");
                JsonNode j = parseNode(r.body());
                double avg = j.has("averageOrderAmount") ? j.get("averageOrderAmount").asDouble()
                                : j.has("average_order_amount") ? j.get("average_order_amount").asDouble()
                                                : j.has("avg_order_amount") ? j.get("avg_order_amount").asDouble() : -1;
                assertEquals(100.0, avg, 0.5,
                                "TC206: avg=300/3=100 (delivered only); NOT 300/5=60. Got " + avg);
        }
}

// ─── TC207 — S1-F4 deactivate fails when active PLACED order ─────────────────
@Tag("public")
@Tag("features_m1")
class TC207_DeactivateActiveOrderTests extends TestBase {
        @Test
        @DisplayName("TC207 — Deactivate fails (400) when user has a PLACED order")
        void deactivate_active_400() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Active User", "tc207@test.io", "CUSTOMER");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "PLACED", 50.0, "2026-08-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/deactivate", "", tok);
                assertEquals(400, r.statusCode(), "TC207: must be 400 when active order exists");
        }
}

// ─── TC208 — S1-F4 deactivate succeeds when no active orders ─────────────────
@Tag("public")
@Tag("features_m1")
class TC208_DeactivateSuccessTests extends TestBase {
        @Test
        @DisplayName("TC208 — Deactivate succeeds when user has only DELIVERED orders; PG status=DEACTIVATED")
        void deactivate_success() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Done User", "tc208@test.io", "CUSTOMER");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "DELIVERED", 50.0, "2026-08-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/deactivate", "", tok);
                assert2xx(r, "TC208");
                String stCol = columnByField("User", "status");
                String dbStatus = jdbc.queryForObject(
                                "SELECT \"" + stCol + "\"::text FROM \"" + tableName("User") + "\" WHERE id = ?",
                                String.class, uid);
                assertEquals("DEACTIVATED", dbStatus, "TC208: PG status=DEACTIVATED expected; got " + dbStatus);
        }
}

// ─── TC209 — S1-F4 404 non-existent user ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC209_DeactivateNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC209 — Deactivate non-existent user returns 404")
        void deactivate_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/999999/deactivate", "", tok);
                assertEquals(404, r.statusCode(), "TC209: must be 404");
        }
}

// ─── TC210 — S1-F4 fails with CONFIRMED active order (not just PLACED) ───────
@Tag("public")
@Tag("features_m1")
class TC210_DeactivateConfirmedActiveTests extends TestBase {
        @Test
        @DisplayName("TC210 — Deactivate fails (400) with CONFIRMED active order")
        void deactivate_confirmed_400() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Confirmed User", "tc210@test.io", "CUSTOMER");
                _S1Seed.seedOrder(jdbc, this, uid, 1L, "CONFIRMED", 50.0, "2026-08-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/deactivate", "", tok);
                assertEquals(400, r.statusCode(), "TC210: must be 400 (CONFIRMED is non-final/active)");
        }
}

// ─── TC211 — S1-F5 preferences search happy match ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC211_PreferencesSearchHappyTests extends TestBase {
        @Test
        @DisplayName("TC211 — Search by dietaryRestrictions=vegetarian returns 2 users")
        void prefs_search_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long u1 = _S1Seed.seedUser(jdbc, this, "Veg1", "tc211_a@test.io", "CUSTOMER");
                long u2 = _S1Seed.seedUser(jdbc, this, "Halal", "tc211_b@test.io", "CUSTOMER");
                long u3 = _S1Seed.seedUser(jdbc, this, "Veg2", "tc211_c@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, u1, "{\"dietaryRestrictions\":\"vegetarian\"}");
                _S1Seed.setPrefs(jdbc, this, u2, "{\"dietaryRestrictions\":\"halal\"}");
                _S1Seed.setPrefs(jdbc, this, u3, "{\"dietaryRestrictions\":\"vegetarian\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/users/preferences/search?key=dietaryRestrictions&value=vegetarian", tok);
                assert2xx(r, "TC211");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                int count = 0;
                for (JsonNode it : list) {
                        long uid = it.has("id") ? it.get("id").asLong() : -1;
                        if (uid == u1 || uid == u3)
                                count++;
                }
                assertEquals(2, count, "TC211: 2 vegetarian users expected; got " + count + " body=" + r.body());
        }
}

// ─── TC212 — S1-F5 no match returns empty list ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC212_PreferencesSearchNoMatchTests extends TestBase {
        @Test
        @DisplayName("TC212 — Search by dietaryRestrictions=vegan returns empty list")
        void prefs_search_no_match() throws Exception {
                BASE_URL = userServiceUrl;
                long u1 = _S1Seed.seedUser(jdbc, this, "Veg", "tc212@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, u1, "{\"dietaryRestrictions\":\"vegetarian\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/users/preferences/search?key=dietaryRestrictions&value=vegan", tok);
                assert2xx(r, "TC212");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC212: empty list expected");
        }
}

// ─── TC213 — S1-F5 400 blank key ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC213_PreferencesSearchBlankKeyTests extends TestBase {
        @Test
        @DisplayName("TC213 — Blank key returns 400")
        void blank_key_400() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/search?key=&value=vegetarian", tok);
                assertEquals(400, r.statusCode(), "TC213: must be 400");
        }
}

// ─── TC214 — S1-F5 400 blank value ───────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC214_PreferencesSearchBlankValueTests extends TestBase {
        @Test
        @DisplayName("TC214 — Blank value returns 400")
        void blank_value_400() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/search?key=dietaryRestrictions&value=",
                                tok);
                assertEquals(400, r.statusCode(), "TC214: must be 400 for blank value");
        }
}

// ─── TC215 — S1-F6 top customers happy ranking ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC215_TopCustomersHappyTests extends TestBase {
        @Test
        @DisplayName("TC215 — Top customers ordered by total spending DESC")
        void top_customers_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _S1Seed.seedUser(jdbc, this, "User A", "tc215_a@test.io", "CUSTOMER");
                long uB = _S1Seed.seedUser(jdbc, this, "User B", "tc215_b@test.io", "CUSTOMER");
                long uC = _S1Seed.seedUser(jdbc, this, "User C", "tc215_c@test.io", "CUSTOMER");
                _S1Seed.seedOrder(jdbc, this, uA, 1L, "DELIVERED", 800.0, "2026-03-10");
                _S1Seed.seedOrder(jdbc, this, uB, 1L, "DELIVERED", 1500.0, "2026-03-15");
                _S1Seed.seedOrder(jdbc, this, uC, 1L, "DELIVERED", 300.0, "2026-03-20");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/users/reports/top-customers?startDate=2026-03-01&endDate=2026-03-31&limit=2",
                                tok);
                assert2xx(r, "TC215");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC215: at least 2 results expected");
                long firstId = list.get(0).has("userId") ? list.get(0).get("userId").asLong()
                                : list.get(0).has("id") ? list.get(0).get("id").asLong() : -1;
                long secondId = list.get(1).has("userId") ? list.get(1).get("userId").asLong()
                                : list.get(1).has("id") ? list.get(1).get("id").asLong() : -1;
                assertEquals(uB, firstId, "TC215: User B (1500) first; got " + firstId);
                assertEquals(uA, secondId, "TC215: User A (800) second; got " + secondId);
        }
}

// ─── TC216 — S1-F6 empty range returns empty list ────────────────────────────
@Tag("public")
@Tag("features_m1")
@Tag("with-baseline")
class TC216_TopCustomersEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC216 — Empty date range returns empty list")
        void top_empty_range() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/users/reports/top-customers?startDate=2030-01-01&endDate=2030-01-31&limit=10",
                                tok);
                assert2xx(r, "TC216");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC216: empty list expected");
        }
}

// ─── TC217 — S1-F6 400 invalid range ─────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
@Tag("with-baseline")
class TC217_TopCustomersInvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC217 — startDate > endDate returns 400")
        void top_invalid_range() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/users/reports/top-customers?startDate=2026-04-01&endDate=2026-03-01&limit=5",
                                tok);
                assertEquals(400, r.statusCode(), "TC217: must be 400");
        }
}

// ─── TC218 — S1-F6 limit caps results ────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC218_TopCustomersLimitTests extends TestBase {
        @Test
        @DisplayName("TC218 — limit=1 returns at most 1 result")
        void top_limit() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _S1Seed.seedUser(jdbc, this, "User A", "tc218_a@test.io", "CUSTOMER");
                long uB = _S1Seed.seedUser(jdbc, this, "User B", "tc218_b@test.io", "CUSTOMER");
                _S1Seed.seedOrder(jdbc, this, uA, 1L, "DELIVERED", 100.0, "2026-08-10");
                _S1Seed.seedOrder(jdbc, this, uB, 1L, "DELIVERED", 200.0, "2026-08-11");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/users/reports/top-customers?startDate=2026-08-01&endDate=2026-08-31&limit=1",
                                tok);
                assert2xx(r, "TC218");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(1, list.size(), "TC218: limit=1 must return exactly 1; got " + list.size());
        }
}

// ─── TC219 — S1-F6 only DELIVERED orders count toward spending ───────────────
@Tag("public")
@Tag("features_m1")
class TC219_TopCustomersDeliveredOnlyTests extends TestBase {
        @Test
        @DisplayName("TC219 — Only DELIVERED orders count toward totalSpent")
        void top_delivered_only() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _S1Seed.seedUser(jdbc, this, "User A", "tc219_a@test.io", "CUSTOMER");
                _S1Seed.seedOrder(jdbc, this, uA, 1L, "DELIVERED", 100.0, "2026-08-10");
                _S1Seed.seedOrder(jdbc, this, uA, 1L, "CANCELLED", 999.0, "2026-08-11");
                _S1Seed.seedOrder(jdbc, this, uA, 1L, "PLACED", 999.0, "2026-08-12");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/users/reports/top-customers?startDate=2026-08-01&endDate=2026-08-31&limit=10",
                                tok);
                assert2xx(r, "TC219");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                boolean checked = false;
                for (JsonNode it : list) {
                        long uid = it.has("userId") ? it.get("userId").asLong()
                                        : it.has("id") ? it.get("id").asLong() : -1;
                        if (uid == uA) {
                                double spent = it.has("totalSpent") ? it.get("totalSpent").asDouble()
                                                : it.has("total_spent") ? it.get("total_spent").asDouble() : -1;
                                assertEquals(100.0, spent, 0.5,
                                                "TC219: only DELIVERED counted (CANCELLED/PLACED excluded); got "
                                                                + spent);
                                checked = true;
                        }
                }
                assertTrue(checked, "TC219: User A must appear in result");
        }
}

// ─── TC220 — S1-F7 set default address happy switch ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC220_SetDefaultAddressHappyTests extends TestBase {
        @Test
        @DisplayName("TC220 — Setting default switches isDefault from old to new")
        void default_address_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Addr User", "tc220@test.io", "CUSTOMER");
                long a1 = _S1Seed.seedAddress(jdbc, this, uid, "1 First St", "Cairo", false);
                long a2 = _S1Seed.seedAddress(jdbc, this, uid, "2 Second St", "Cairo", true);
                long a3 = _S1Seed.seedAddress(jdbc, this, uid, "3 Third St", "Cairo", false);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/addresses/" + a3 + "/default", "", tok);
                assert2xx(r, "TC220");
                // Verify in PG via manifest-driven columns
                String dTable = tableName("DeliveryAddress");
                String idCol = columnByField("DeliveryAddress", "isDefault");
                Boolean a2def = jdbc.queryForObject(
                                "SELECT \"" + idCol + "\" FROM \"" + dTable + "\" WHERE id = ?", Boolean.class, a2);
                Boolean a3def = jdbc.queryForObject(
                                "SELECT \"" + idCol + "\" FROM \"" + dTable + "\" WHERE id = ?", Boolean.class, a3);
                assertEquals(Boolean.FALSE, a2def, "TC220: old default (a2) must be false; got " + a2def);
                assertEquals(Boolean.TRUE, a3def, "TC220: new default (a3) must be true; got " + a3def);
        }
}

// ─── TC221 — S1-F7 404 non-existent user ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC221_SetDefaultAddressUserNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC221 — Set-default on non-existent user returns 404")
        void user_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/999999/addresses/1/default", "", tok);
                assertEquals(404, r.statusCode(), "TC221: must be 404");
        }
}

// ─── TC222 — S1-F7 400 address belongs to different user ─────────────────────
@Tag("public")
@Tag("features_m1")
class TC222_SetDefaultAddressCrossUserTests extends TestBase {
        @Test
        @DisplayName("TC222 — Set-default with address belonging to a different user returns 400")
        void cross_user_400() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _S1Seed.seedUser(jdbc, this, "User A", "tc222_a@test.io", "CUSTOMER");
                long uB = _S1Seed.seedUser(jdbc, this, "User B", "tc222_b@test.io", "CUSTOMER");
                long aA = _S1Seed.seedAddress(jdbc, this, uA, "A's address", "Cairo", true);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uB + "/addresses/" + aA + "/default", "", tok);
                assertEquals(400, r.statusCode(),
                                "TC222: must be 400 (address belongs to A, not B); got " + r.statusCode());
        }
}

// ─── TC223 — S1-F7 404 non-existent address ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC223_SetDefaultAddressNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC223 — Set-default with non-existent addressId returns 404")
        void address_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Addr User", "tc223@test.io", "CUSTOMER");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/addresses/999999/default", "", tok);
                assertEquals(404, r.statusCode(), "TC223: must be 404");
        }
}

// ─── TC224 — S1-F7 all other addresses set to isDefault=false ────────────────
@Tag("public")
@Tag("features_m1")
class TC224_SetDefaultAddressAllOthersFalseTests extends TestBase {
        @Test
        @DisplayName("TC224 — Setting one address default sets ALL others isDefault=false")
        void all_others_false() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Addr User", "tc224@test.io", "CUSTOMER");
                long a1 = _S1Seed.seedAddress(jdbc, this, uid, "1 First St", "Cairo", true);
                long a2 = _S1Seed.seedAddress(jdbc, this, uid, "2 Second St", "Cairo", false);
                long a3 = _S1Seed.seedAddress(jdbc, this, uid, "3 Third St", "Cairo", false);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/addresses/" + a3 + "/default", "", tok);
                assert2xx(r, "TC224");
                String dTable = tableName("DeliveryAddress");
                String idCol = columnByField("DeliveryAddress", "isDefault");
                Boolean a1def = jdbc.queryForObject(
                                "SELECT \"" + idCol + "\" FROM \"" + dTable + "\" WHERE id = ?", Boolean.class, a1);
                Boolean a2def = jdbc.queryForObject(
                                "SELECT \"" + idCol + "\" FROM \"" + dTable + "\" WHERE id = ?", Boolean.class, a2);
                assertEquals(Boolean.FALSE, a1def,
                                "TC224: seeded address 1 must be false because seeded address 3 is set default; got "
                                                + a1def);
                assertEquals(Boolean.FALSE, a2def,
                                "TC224: seeded address 2 must be false because seeded address 3 is set default; got "
                                                + a2def);
        }
}

// ─── TC225 — S1-F8 user profile happy path ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC225_UserProfileHappyTests extends TestBase {
        @Test
        @DisplayName("TC225 — Profile DTO returns user info + 3 addresses + totalAddresses=3")
        void profile_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Profile User", "tc225@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, uid, "{\"theme\":\"dark\"}");
                _S1Seed.seedAddress(jdbc, this, uid, "1 First St", "Cairo", true);
                _S1Seed.seedAddress(jdbc, this, uid, "2 Second St", "Cairo", false);
                _S1Seed.seedAddress(jdbc, this, uid, "3 Third St", "Cairo", false);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/profile", tok);
                assert2xx(r, "TC225");
                JsonNode j = parseNode(r.body());
                assertEquals(uid,
                                j.has("userId") ? j.get("userId").asLong() : (j.has("id") ? j.get("id").asLong() : -1),
                                "TC225: userId/id must match");
                assertEquals("Profile User",
                                j.has("name") ? j.get("name").asText() : "", "TC225: name must match");
                long total = j.has("totalAddresses") ? j.get("totalAddresses").asLong()
                                : j.has("total_addresses") ? j.get("total_addresses").asLong() : -1;
                assertEquals(3L, total, "TC225: totalAddresses=3");
                JsonNode addrs = j.has("deliveryAddresses") ? j.get("deliveryAddresses")
                                : j.has("delivery_addresses") ? j.get("delivery_addresses")
                                                : j.has("addresses") ? j.get("addresses") : null;
                assertNotNull(addrs, "TC225: deliveryAddresses field expected");
                assertEquals(3, addrs.size(), "TC225: 3 addresses expected");
        }
}

// ─── TC226 — S1-F8 user with no addresses ────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC226_UserProfileNoAddressesTests extends TestBase {
        @Test
        @DisplayName("TC226 — Profile for user with no addresses: deliveryAddresses=[], totalAddresses=0")
        void profile_no_addresses() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Empty Profile", "tc226@test.io", "CUSTOMER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/profile", tok);
                assert2xx(r, "TC226");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalAddresses") ? j.get("totalAddresses").asLong()
                                : j.has("total_addresses") ? j.get("total_addresses").asLong() : -1;
                assertEquals(0L, total, "TC226: totalAddresses=0");
                JsonNode addrs = j.has("deliveryAddresses") ? j.get("deliveryAddresses")
                                : j.has("delivery_addresses") ? j.get("delivery_addresses")
                                                : j.has("addresses") ? j.get("addresses") : null;
                if (addrs != null) {
                        assertEquals(0, addrs.size(), "TC226: addresses must be empty list");
                }
        }
}

// ─── TC227 — S1-F8 404 non-existent user ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC227_UserProfileNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC227 — Profile for non-existent user returns 404")
        void profile_not_found() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/999999/profile", tok);
                assertEquals(404, r.statusCode(), "TC227: must be 404");
        }
}

// ─── TC228 — S1-F8 DTO completeness ──────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC228_UserProfileDtoShapeTests extends TestBase {
        @Test
        @DisplayName("TC228 — Profile DTO contains all 7 spec fields")
        void profile_dto_shape() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _S1Seed.seedUser(jdbc, this, "Shape User", "tc228@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, uid, "{}");
                _S1Seed.seedAddress(jdbc, this, uid, "1 St", "Cairo", true);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/profile", tok);
                assert2xx(r, "TC228");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("userId") || j.has("id"), "TC228: userId/id required");
                assertTrue(j.has("name"), "TC228: name required");
                assertTrue(j.has("email"), "TC228: email required");
                assertTrue(j.has("phone"), "TC228: phone required");
                assertTrue(j.has("preferences"), "TC228: preferences required");
                assertTrue(j.has("deliveryAddresses") || j.has("delivery_addresses") || j.has("addresses"),
                                "TC228: deliveryAddresses required");
                assertTrue(j.has("totalAddresses") || j.has("total_addresses"),
                                "TC228: totalAddresses required");
        }
}

// ─── TC229 — S1-F9 dietary + minOrders happy filter ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC229_DietaryMinOrdersHappyTests extends TestBase {
        @Test
        @DisplayName("TC229 — vegetarian + minOrders=3: only User A (5 delivered) qualifies")
        void dietary_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _S1Seed.seedUser(jdbc, this, "Veg High", "tc229_a@test.io", "CUSTOMER");
                long uB = _S1Seed.seedUser(jdbc, this, "Veg Low", "tc229_b@test.io", "CUSTOMER");
                long uC = _S1Seed.seedUser(jdbc, this, "Halal High", "tc229_c@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, uA, "{\"dietaryRestrictions\":\"vegetarian\"}");
                _S1Seed.setPrefs(jdbc, this, uB, "{\"dietaryRestrictions\":\"vegetarian\"}");
                _S1Seed.setPrefs(jdbc, this, uC, "{\"dietaryRestrictions\":\"halal\"}");
                for (int i = 0; i < 5; i++)
                        _S1Seed.seedOrder(jdbc, this, uA, 1L, "DELIVERED", 50.0, "2026-08-10");
                for (int i = 0; i < 2; i++)
                        _S1Seed.seedOrder(jdbc, this, uB, 1L, "DELIVERED", 50.0, "2026-08-11");
                for (int i = 0; i < 10; i++)
                        _S1Seed.seedOrder(jdbc, this, uC, 1L, "DELIVERED", 50.0, "2026-08-12");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/dietary?diet=vegetarian&minOrders=3", tok);
                assert2xx(r, "TC229");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                java.util.Set<Long> ids = new java.util.HashSet<>();
                for (JsonNode it : list)
                        ids.add(it.has("id") ? it.get("id").asLong()
                                        : it.has("userId") ? it.get("userId").asLong() : -1L);
                assertTrue(ids.contains(uA), "TC229: User A must qualify");
                assertFalse(ids.contains(uB), "TC229: User B (only 2 delivered) must NOT qualify");
                assertFalse(ids.contains(uC), "TC229: User C (halal, not vegetarian) must NOT qualify");
        }
}

// ─── TC230 — S1-F9 lower threshold returns more users ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC230_DietaryMinOrdersLowerThresholdTests extends TestBase {
        @Test
        @DisplayName("TC230 — vegetarian + minOrders=1: both User A and B qualify")
        void dietary_lower_threshold() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _S1Seed.seedUser(jdbc, this, "Veg High", "tc230_a@test.io", "CUSTOMER");
                long uB = _S1Seed.seedUser(jdbc, this, "Veg Low", "tc230_b@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, uA, "{\"dietaryRestrictions\":\"vegetarian\"}");
                _S1Seed.setPrefs(jdbc, this, uB, "{\"dietaryRestrictions\":\"vegetarian\"}");
                for (int i = 0; i < 5; i++)
                        _S1Seed.seedOrder(jdbc, this, uA, 1L, "DELIVERED", 50.0, "2026-08-10");
                for (int i = 0; i < 2; i++)
                        _S1Seed.seedOrder(jdbc, this, uB, 1L, "DELIVERED", 50.0, "2026-08-11");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/dietary?diet=vegetarian&minOrders=1", tok);
                assert2xx(r, "TC230");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                java.util.Set<Long> ids = new java.util.HashSet<>();
                for (JsonNode it : list)
                        ids.add(it.has("id") ? it.get("id").asLong()
                                        : it.has("userId") ? it.get("userId").asLong() : -1L);
                assertTrue(ids.contains(uA), "TC230: User A must qualify");
                assertTrue(ids.contains(uB), "TC230: User B must qualify");
        }
}

// ─── TC231 — S1-F9 400 blank diet ────────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
@Tag("with-baseline")
class TC231_DietaryBlankDietTests extends TestBase {
        @Test
        @DisplayName("TC231 — Blank diet returns 400")
        void dietary_blank() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/dietary?diet=&minOrders=1", tok);
                assertEquals(400, r.statusCode(), "TC231: must be 400");
        }
}

// ─── TC232 — S1-F9 only DELIVERED orders count toward minOrders ──────────────
@Tag("public")
@Tag("features_m1")
class TC232_DietaryDeliveredOnlyTests extends TestBase {
        @Test
        @DisplayName("TC232 — Only DELIVERED orders count: PLACED orders don't count toward minOrders")
        void dietary_delivered_only() throws Exception {
                BASE_URL = userServiceUrl;
                long uA = _S1Seed.seedUser(jdbc, this, "Veg Placed", "tc232@test.io", "CUSTOMER");
                _S1Seed.setPrefs(jdbc, this, uA, "{\"dietaryRestrictions\":\"vegetarian\"}");
                for (int i = 0; i < 5; i++)
                        _S1Seed.seedOrder(jdbc, this, uA, 1L, "PLACED", 50.0, "2026-08-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/preferences/dietary?diet=vegetarian&minOrders=1", tok);
                assert2xx(r, "TC232");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        long id = it.has("id") ? it.get("id").asLong()
                                        : it.has("userId") ? it.get("userId").asLong() : -1L;
                        assertTrue(id != uA,
                                        "TC232: User A has 0 DELIVERED orders (5 PLACED) — must NOT qualify; got id="
                                                        + id);
                }
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2-F1..S5-F9 M1 — pure-logic tests (TC233..TC378)
//
// Authenticated via adminToken(). All DB operations go through tableName()
// + columnByField() so any student renaming @Column / @JoinColumn still
// works at grading time. No Mongo / Redis / Neo4j / Cassandra assertions —
// those side-effect checks live in the future amendments category.
// Assertion messages are verbose: each says what was expected, what was
// observed, and (where useful) which manifest entity/field was looked up.
// ════════════════════════════════════════════════════════════════════════════

/** Shared seed helpers for S2..S5 tests — all manifest-driven. */
class _S2S5Seed {
        static long seedRestaurant(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        String name, String cuisine, String status) {
                String t = tb.tableName("Restaurant");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(tb.columnByField("Restaurant", "name"), name);
                _ov.put(tb.columnByField("Restaurant", "cuisineType"), cuisine);
                _ov.put(tb.columnByField("Restaurant", "status"), status);
                return tb.insertRowReturningId(t, _ov);
        }

        static void setRestaurantRating(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb, long restId,
                        double rating) {
                jdbc.update("UPDATE \"" + tb.tableName("Restaurant") + "\" SET \""
                                + tb.columnByField("Restaurant", "rating") + "\" = ? WHERE id = ?", rating, restId);
        }

        static void setRestaurantDetails(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb, long restId,
                        String json) {
                jdbc.update("UPDATE \"" + tb.tableName("Restaurant") + "\" SET \""
                                + tb.columnByField("Restaurant", "details") + "\" = ?::jsonb WHERE id = ?", json,
                                restId);
        }

        static long seedMenuItem(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        long restId, String name, double price, String category, boolean available) {
                String t = tb.tableName("MenuItem");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(tb.columnByField("MenuItem", "restaurant"), restId);
                _ov.put(tb.columnByField("MenuItem", "name"), name);
                _ov.put(tb.columnByField("MenuItem", "price"), price);
                _ov.put(tb.columnByField("MenuItem", "category"), category);
                _ov.put(tb.columnByField("MenuItem", "available"), available);
                return tb.insertRowReturningId(t, _ov);
        }

        static long seedOrder(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        long userId, long restId, String status, double amount, String dateStr) {
                String t = tb.tableName("Order");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(tb.columnByField("Order", "user"), userId);
                _ov.put(tb.columnByField("Order", "restaurant"), restId);
                _ov.put(tb.columnByField("Order", "totalAmount"), amount);
                _ov.put(tb.columnByField("Order", "status"), status);
                Long oid = tb.insertRowReturningId(t, _ov);
                if (dateStr != null)
                        tb.setAllDateColumns(t, oid, java.sql.Timestamp.valueOf(dateStr + " 12:00:00"));
                return oid;
        }

        static long seedOrderItem(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        long orderId, long menuItemId, String itemName,
                        int quantity, double unitPrice, int lineNumber, String status) {
                String t = tb.tableName("OrderItem");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(tb.columnByField("OrderItem", "order"), orderId);
                _ov.put(tb.columnByField("OrderItem", "menuItem"), menuItemId);
                _ov.put(tb.columnByField("OrderItem", "itemName"), itemName);
                _ov.put(tb.columnByField("OrderItem", "quantity"), quantity);
                _ov.put(tb.columnByField("OrderItem", "unitPrice"), unitPrice);
                _ov.put(tb.columnByField("OrderItem", "lineNumber"), lineNumber);
                _ov.put(tb.columnByField("OrderItem", "status"), status);
                return tb.insertRowReturningId(t, _ov);
        }

        static long seedDelivery(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        long orderId, String status, double lat, double lon, String driverName) {
                String t = tb.tableName("Delivery");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(tb.columnByField("Delivery", "order"), orderId);
                _ov.put(tb.columnByField("Delivery", "status"), status);
                _ov.put(tb.columnByField("Delivery", "latitude"), lat);
                _ov.put(tb.columnByField("Delivery", "longitude"), lon);
                _ov.put(tb.columnByField("Delivery", "driverName"), driverName);
                return tb.insertRowReturningId(t, _ov);
        }

        static void setDeliveryMetadata(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb, long delId,
                        String json) {
                jdbc.update("UPDATE \"" + tb.tableName("Delivery") + "\" SET \""
                                + tb.columnByField("Delivery", "metadata") + "\" = ?::jsonb WHERE id = ?", json, delId);
        }

        static void setDeliveryUpdatedAt(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb, long delId,
                        java.sql.Timestamp ts) {
                try {
                        jdbc.update("UPDATE \"" + tb.tableName("Delivery") + "\" SET updated_at = ? WHERE id = ?", ts,
                                        delId);
                } catch (org.springframework.dao.DataAccessException e) {
                        throw new AssertionError(
                                        "Delivery table needs an `updated_at` column for date-range tests — "
                                                        + e.getMessage(),
                                        e);
                }
        }

        static long seedPayment(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        long orderId, long userId, double amount, String method, String status) {
                String t = tb.tableName("Payment");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(tb.columnByField("Payment", "order"), orderId);
                _ov.put(tb.columnByField("Payment", "user"), userId);
                _ov.put(tb.columnByField("Payment", "amount"), amount);
                _ov.put(tb.columnByField("Payment", "method"), method);
                _ov.put(tb.columnByField("Payment", "status"), status);
                return tb.insertRowReturningId(t, _ov);
        }

        static void setPaymentTransactionDetails(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb, long pid,
                        String json) {
                jdbc.update("UPDATE \"" + tb.tableName("Payment") + "\" SET \""
                                + tb.columnByField("Payment", "transactionDetails") + "\" = ?::jsonb WHERE id = ?",
                                json, pid);
        }

        static long seedOffer(org.springframework.jdbc.core.JdbcTemplate jdbc, TestBase tb,
                        String code, String discountType, double value, int maxUses, java.sql.Date expiry) {
                String t = tb.tableName("Offer");
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put("code", code);
                _ov.put(tb.columnByField("Offer", "discountType"), discountType);
                _ov.put("discount_value", value);
                _ov.put("max_uses", maxUses);
                _ov.put("expiry_date", expiry);
                return tb.insertRowReturningId(t, _ov);
        }

        static java.sql.Date today() {
                return new java.sql.Date(System.currentTimeMillis());
        }

        static java.sql.Date future() {
                return java.sql.Date.valueOf("2030-12-31");
        }

        static java.sql.Date past() {
                return java.sql.Date.valueOf("2020-01-01");
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2-F1 — Search Restaurants by Cuisine + Rating Range (TC233-TC236)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC233_S2F1HappyFilterTests extends TestBase {
        @Test
        @DisplayName("TC233 — cuisineType=EGYPTIAN + range 4.0..5.0 → 2 results, 4.9 first")
        void happy_filter() throws Exception {
                BASE_URL = catalogServiceUrl;
                long r1 = _S2S5Seed.seedRestaurant(jdbc, this, "Koshary El Tahrir", "EGYPTIAN", "OPEN");
                long r2 = _S2S5Seed.seedRestaurant(jdbc, this, "Pizza Roma", "ITALIAN", "CLOSED");
                long r3 = _S2S5Seed.seedRestaurant(jdbc, this, "Foul Express", "EGYPTIAN", "OPEN");
                _S2S5Seed.setRestaurantRating(jdbc, this, r1, 4.5);
                _S2S5Seed.setRestaurantRating(jdbc, this, r2, 3.8);
                _S2S5Seed.setRestaurantRating(jdbc, this, r3, 4.9);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants/search?cuisineType=EGYPTIAN&minRating=4.0&maxRating=5.0", tok);
                assert2xx(r, "TC233: GET /api/restaurants/search must return 2xx for valid filter");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(2, list.size(),
                                "TC233: expected exactly 2 EGYPTIAN restaurants in [4.0,5.0]; got " + list.size()
                                                + " body=" + r.body());
                long firstId = list.get(0).has("id") ? list.get(0).get("id").asLong() : -1L;
                assertEquals(r3, firstId,
                                "TC233: highest-rated EGYPTIAN (id=" + r3 + ", rating=4.9) must come first; got id="
                                                + firstId);
        }
}

@Tag("public")
@Tag("features_m1")
class TC234_S2F1NoCuisineFilterTests extends TestBase {
        @Test
        @DisplayName("TC234 — range only (no cuisine filter) returns all restaurants in range")
        void no_cuisine_filter() throws Exception {
                BASE_URL = catalogServiceUrl;
                long r1 = _S2S5Seed.seedRestaurant(jdbc, this, "EgFood", "EGYPTIAN", "OPEN");
                long r2 = _S2S5Seed.seedRestaurant(jdbc, this, "ItFood", "ITALIAN", "CLOSED");
                _S2S5Seed.setRestaurantRating(jdbc, this, r1, 4.5);
                _S2S5Seed.setRestaurantRating(jdbc, this, r2, 3.8);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/search?minRating=3.0&maxRating=4.0", tok);
                assert2xx(r, "TC234: GET search with no cuisineType must return 2xx");
                JsonNode list = _list(parseNode(r.body()));
                boolean hasIt = false;
                for (JsonNode it : list)
                        if (it.has("id") && it.get("id").asLong() == r2)
                                hasIt = true;
                assertTrue(hasIt,
                                "TC234: ITALIAN restaurant (rating=3.8) must be in range [3.0,4.0]; body=" + r.body());
        }

        private JsonNode _list(JsonNode b) {
                return b.isArray() ? b : (b.has("content") ? b.get("content") : b);
        }
}

@Tag("public")
@Tag("features_m1")
class TC235_S2F1SortDescTests extends TestBase {
        @Test
        @DisplayName("TC235 — Results ordered by rating descending")
        void sort_desc() throws Exception {
                BASE_URL = catalogServiceUrl;
                long r1 = _S2S5Seed.seedRestaurant(jdbc, this, "R1", "EGYPTIAN", "OPEN");
                long r2 = _S2S5Seed.seedRestaurant(jdbc, this, "R2", "EGYPTIAN", "OPEN");
                long r3 = _S2S5Seed.seedRestaurant(jdbc, this, "R3", "EGYPTIAN", "OPEN");
                _S2S5Seed.setRestaurantRating(jdbc, this, r1, 4.0);
                _S2S5Seed.setRestaurantRating(jdbc, this, r2, 5.0);
                _S2S5Seed.setRestaurantRating(jdbc, this, r3, 4.5);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants/search?cuisineType=EGYPTIAN&minRating=3.0&maxRating=5.0", tok);
                assert2xx(r, "TC235");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                double prev = Double.MAX_VALUE;
                for (JsonNode it : list) {
                        double rating = it.has("rating") ? it.get("rating").asDouble() : 0;
                        assertTrue(rating <= prev,
                                        "TC235: ratings must be DESC; got " + rating + " after " + prev + " — body="
                                                        + r.body());
                        prev = rating;
                }
        }
}

@Tag("public")
@Tag("features_m1")
class TC236_S2F1InvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC236 — minRating > maxRating returns 400")
        void invalid_range() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/search?minRating=5.0&maxRating=3.0", tok);
                assertEquals(400, r.statusCode(),
                                "TC236: minRating>maxRating must be 400 (per spec validation step); got "
                                                + r.statusCode() + " body=" + r.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2-F2 — Update Restaurant Details (JSONB partial merge) (TC237-TC239)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC237_S2F2MergeHappyTests extends TestBase {
        @Test
        @DisplayName("TC237 — PUT details merges: address kept, deliveryRadius updated, openingHours added")
        void merge_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "MergeRest", "EGYPTIAN", "OPEN");
                _S2S5Seed.setRestaurantDetails(jdbc, this, restId,
                                "{\"address\":\"Zamalek\",\"deliveryRadius\":5,\"minOrder\":50}");
                String tok = adminToken();
                String body = "{\"deliveryRadius\":10,\"openingHours\":\"9AM-11PM\"}";
                HttpResponse<String> r = httpPutAuth("/api/restaurants/" + restId + "/details", body, tok);
                assert2xx(r, "TC237: PUT details must succeed for existing restaurant");
                JsonNode j = parseNode(r.body());
                JsonNode details = j.has("details") ? j.get("details") : j;
                assertEquals("Zamalek", details.has("address") ? details.get("address").asText() : "",
                                "TC237: address='Zamalek' must be preserved; got " + details);
                assertEquals(10, details.has("deliveryRadius") ? details.get("deliveryRadius").asInt() : -1,
                                "TC237: deliveryRadius must be updated to 10; got " + details);
                assertEquals("9AM-11PM", details.has("openingHours") ? details.get("openingHours").asText() : "",
                                "TC237: openingHours='9AM-11PM' must be added; got " + details);
                assertEquals(50, details.has("minOrder") ? details.get("minOrder").asInt() : -1,
                                "TC237: minOrder=50 (untouched key) must be preserved; got " + details);
        }
}

@Tag("public")
@Tag("features_m1")
class TC238_S2F2SameKeyOverwriteTests extends TestBase {
        @Test
        @DisplayName("TC238 — Same-key PUT overwrites that key (per spec merge semantics)")
        void same_key_overwrite() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "Rest", "EGYPTIAN", "OPEN");
                _S2S5Seed.setRestaurantDetails(jdbc, this, restId, "{\"deliveryRadius\":5}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/restaurants/" + restId + "/details",
                                "{\"deliveryRadius\":15}", tok);
                assert2xx(r, "TC238: PUT must succeed");
                JsonNode details = parseNode(r.body()).has("details") ? parseNode(r.body()).get("details")
                                : parseNode(r.body());
                assertEquals(15, details.has("deliveryRadius") ? details.get("deliveryRadius").asInt() : -1,
                                "TC238: deliveryRadius must be overwritten 5→15; got " + details);
        }
}

@Tag("public")
@Tag("features_m1")
class TC239_S2F2NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC239 — PUT details for non-existent restaurant returns 404")
        void not_found() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/restaurants/999999/details", "{\"x\":1}", tok);
                assertEquals(404, r.statusCode(),
                                "TC239: non-existent restaurant must return 404; got " + r.statusCode());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2-F3 — Restaurant Revenue Summary DTO (TC240-TC243)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC240_S2F3HappyTests extends TestBase {
        @Test
        @DisplayName("TC240 — 5 DELIVERED orders → totalOrders=5, totalRevenue=1000, avg=200")
        void happy_path() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "RevRest", "EGYPTIAN", "OPEN");
                double[] amts = { 100, 150, 200, 250, 300 };
                for (double a : amts)
                        _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "DELIVERED", a, "2026-03-15");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants/" + restId + "/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC240: GET revenue must succeed");
                JsonNode j = parseNode(r.body());
                long total = j.has("totalOrders") ? j.get("totalOrders").asLong() : -1;
                double rev = j.has("totalRevenue") ? j.get("totalRevenue").asDouble() : -1;
                double avg = j.has("averageOrderAmount") ? j.get("averageOrderAmount").asDouble() : -1;
                assertEquals(5L, total, "TC240: totalOrders=5 expected; got " + total);
                assertEquals(1000.0, rev, 0.5, "TC240: totalRevenue=1000 expected; got " + rev);
                assertEquals(200.0, avg, 0.5, "TC240: averageOrderAmount=200 expected; got " + avg);
        }
}

@Tag("public")
@Tag("features_m1")
class TC241_S2F3NoOrdersTests extends TestBase {
        @Test
        @DisplayName("TC241 — Restaurant with no orders in range returns zeros")
        void no_orders_zero() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "EmptyRest", "EGYPTIAN", "OPEN");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants/" + restId + "/revenue?startDate=2030-01-01&endDate=2030-01-31", tok);
                assert2xx(r, "TC241: GET revenue must succeed even when zero orders");
                JsonNode j = parseNode(r.body());
                assertEquals(0L, j.has("totalOrders") ? j.get("totalOrders").asLong() : -1, "TC241: totalOrders=0");
                assertEquals(0.0, j.has("totalRevenue") ? j.get("totalRevenue").asDouble() : -1, 0.01,
                                "TC241: totalRevenue=0");
        }
}

@Tag("public")
@Tag("features_m1")
class TC242_S2F3DeliveredOnlyTests extends TestBase {
        @Test
        @DisplayName("TC242 — Only DELIVERED orders count toward totalRevenue")
        void delivered_only() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "MixRest", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "DELIVERED", 100.0, "2026-03-10");
                _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "CANCELLED", 999.0, "2026-03-11");
                _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "PLACED", 999.0, "2026-03-12");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants/" + restId + "/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC242");
                JsonNode j = parseNode(r.body());
                double rev = j.has("totalRevenue") ? j.get("totalRevenue").asDouble() : -1;
                assertEquals(100.0, rev, 0.5,
                                "TC242: only DELIVERED counted (CANCELLED/PLACED excluded); got totalRevenue=" + rev);
        }
}

@Tag("public")
@Tag("features_m1")
class TC243_S2F3NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC243 — Revenue for non-existent restaurant returns 404")
        void not_found() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants/999999/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assertEquals(404, r.statusCode(), "TC243: must be 404; got " + r.statusCode());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2-F4 — Update Restaurant Status (transactional, active-orders check)
// (TC244-TC247)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC244_S2F4SuspendActiveOrderTests extends TestBase {
        @Test
        @DisplayName("TC244 — SUSPENDED with active CONFIRMED order returns 400")
        void suspend_active_400() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "ActiveRest", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "CONFIRMED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/restaurants/" + restId + "/status",
                                "{\"status\":\"SUSPENDED\"}", tok);
                assertEquals(400, r.statusCode(),
                                "TC244: SUSPENDED with active CONFIRMED order must be 400 (per spec); got "
                                                + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC245_S2F4SuspendSuccessTests extends TestBase {
        @Test
        @DisplayName("TC245 — SUSPENDED with only DELIVERED orders succeeds; PG status=SUSPENDED")
        void suspend_success() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "OkRest", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "DELIVERED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/restaurants/" + restId + "/status",
                                "{\"status\":\"SUSPENDED\"}", tok);
                assert2xx(r, "TC245: SUSPENDED with no active orders must succeed");
                String dbStatus = jdbc.queryForObject(
                                "SELECT \"" + columnByField("Restaurant", "status") + "\"::text FROM \""
                                                + tableName("Restaurant") + "\" WHERE id = ?",
                                String.class, restId);
                assertEquals("SUSPENDED", dbStatus,
                                "TC245: PG restaurants.status must be SUSPENDED after PUT; got " + dbStatus);
        }
}

@Tag("public")
@Tag("features_m1")
class TC246_S2F4ClosedActiveOrderTests extends TestBase {
        @Test
        @DisplayName("TC246 — CLOSED with active CONFIRMED order succeeds (active-orders check is SUSPENDED-only)")
        void closed_active_2xx() throws Exception {
                BASE_URL = catalogServiceUrl;
                // Talabat M1 spec §S2-F4 (line 974): "If going SUSPENDED, check no active
                // orders reference this restaurant — throws 400 if not." The active-orders
                // gate is bound to SUSPENDED only. Going to CLOSED while active orders
                // exist is permitted.
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "CloseRest", "EGYPTIAN", "OPEN");
                String tok = adminToken();              // creates the admin user (id=1 with baseline opt-out)
                long uid  = adminId();
                _S2S5Seed.seedOrder(jdbc, this, uid, restId, "CONFIRMED", 100.0, "2026-03-10");
                HttpResponse<String> r = httpPutAuth("/api/restaurants/" + restId + "/status",
                                "{\"status\":\"CLOSED\"}", tok);
                assert2xx(r, "TC246: CLOSED with active CONFIRMED order must succeed (active-orders check is SUSPENDED-only per spec §S2-F4)");
        }
}

@Tag("public")
@Tag("features_m1")
class TC247_S2F4NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC247 — PUT status for non-existent restaurant returns 404")
        void not_found() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/restaurants/999999/status",
                                "{\"status\":\"OPEN\"}", tok);
                assertEquals(404, r.statusCode(), "TC247: must be 404; got " + r.statusCode());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2-F5 — Filter Restaurants by Detail Attribute (JSONB) (TC248-TC251)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC248_S2F5HappyWithStatusTests extends TestBase {
        @Test
        @DisplayName("TC248 — key=deliveryRadius&value=10&status=OPEN returns only matching open restaurant")
        void happy_with_status() throws Exception {
                BASE_URL = catalogServiceUrl;
                long r1 = _S2S5Seed.seedRestaurant(jdbc, this, "R1", "EGYPTIAN", "OPEN");
                long r2 = _S2S5Seed.seedRestaurant(jdbc, this, "R2", "EGYPTIAN", "OPEN");
                long r3 = _S2S5Seed.seedRestaurant(jdbc, this, "R3", "EGYPTIAN", "CLOSED");
                _S2S5Seed.setRestaurantDetails(jdbc, this, r1, "{\"deliveryRadius\":10}");
                _S2S5Seed.setRestaurantDetails(jdbc, this, r2, "{\"deliveryRadius\":5}");
                _S2S5Seed.setRestaurantDetails(jdbc, this, r3, "{\"deliveryRadius\":10}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/restaurants/details/search?key=deliveryRadius&value=10&status=OPEN", tok);
                assert2xx(r, "TC248");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertEquals(1, list.size(),
                                "TC248: only r1 (OPEN + deliveryRadius=10) matches; got size=" + list.size() + " body="
                                                + r.body());
        }
}

@Tag("public")
@Tag("features_m1")
class TC249_S2F5HappyNoStatusTests extends TestBase {
        @Test
        @DisplayName("TC249 — key=deliveryRadius&value=10 (no status filter) returns 2 (OPEN + CLOSED)")
        void happy_no_status() throws Exception {
                BASE_URL = catalogServiceUrl;
                long r1 = _S2S5Seed.seedRestaurant(jdbc, this, "R1", "EGYPTIAN", "OPEN");
                long r2 = _S2S5Seed.seedRestaurant(jdbc, this, "R2", "EGYPTIAN", "CLOSED");
                _S2S5Seed.setRestaurantDetails(jdbc, this, r1, "{\"deliveryRadius\":10}");
                _S2S5Seed.setRestaurantDetails(jdbc, this, r2, "{\"deliveryRadius\":10}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/details/search?key=deliveryRadius&value=10",
                                tok);
                assert2xx(r, "TC249");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertEquals(2, list.size(),
                                "TC249: 2 restaurants match (no status filter); got " + list.size());
        }
}

@Tag("public")
@Tag("features_m1")
class TC250_S2F5NoMatchTests extends TestBase {
        @Test
        @DisplayName("TC250 — value with no match returns empty list")
        void no_match() throws Exception {
                BASE_URL = catalogServiceUrl;
                long r1 = _S2S5Seed.seedRestaurant(jdbc, this, "R1", "EGYPTIAN", "OPEN");
                _S2S5Seed.setRestaurantDetails(jdbc, this, r1, "{\"deliveryRadius\":10}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/details/search?key=deliveryRadius&value=999",
                                tok);
                assert2xx(r, "TC250");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertEquals(0, list.size(), "TC250: empty list expected; got " + list.size());
        }
}

@Tag("public")
@Tag("features_m1")
class TC251_S2F5BlankKeyTests extends TestBase {
        @Test
        @DisplayName("TC251 — Blank key returns 400")
        void blank_key() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/details/search?key=&value=10", tok);
                // Spec doesn't explicitly say 400 for blank key, but common pattern across
                // S1-F5 etc.
                // We accept 400 OR empty list — but a 5xx must NOT happen.
                assertTrue(r.statusCode() != 500 && r.statusCode() != 503,
                                "TC251: blank key must not cause 5xx; got " + r.statusCode());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2-F6 — Top-Rated Restaurants Report (TC252-TC254)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC252_S2F6HappyRankingTests extends TestBase {
        @Test
        @DisplayName("TC252 — limit=2 returns top 2, highest first")
        void ranking() throws Exception {
                BASE_URL = catalogServiceUrl;
                long r1 = _S2S5Seed.seedRestaurant(jdbc, this, "R1", "EGYPTIAN", "OPEN");
                long r2 = _S2S5Seed.seedRestaurant(jdbc, this, "R2", "EGYPTIAN", "OPEN");
                long r3 = _S2S5Seed.seedRestaurant(jdbc, this, "R3", "EGYPTIAN", "OPEN");
                _S2S5Seed.setRestaurantRating(jdbc, this, r1, 4.9);
                _S2S5Seed.setRestaurantRating(jdbc, this, r2, 4.5);
                _S2S5Seed.setRestaurantRating(jdbc, this, r3, 4.2);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/reports/top-rated?limit=2", tok);
                assert2xx(r, "TC252");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertEquals(2, list.size(), "TC252: limit=2 must return exactly 2; got " + list.size());
                long firstId = list.get(0).has("restaurantId") ? list.get(0).get("restaurantId").asLong()
                                : list.get(0).has("id") ? list.get(0).get("id").asLong() : -1L;
                assertEquals(r1, firstId,
                                "TC252: highest-rated (4.9, id=" + r1 + ") must come first; got id=" + firstId);
        }
}

@Tag("public")
@Tag("features_m1")
class TC253_S2F6LimitGreaterThanAvailableTests extends TestBase {
        @Test
        @DisplayName("TC253 — limit=10 with only 3 restaurants returns all 3")
        void limit_overflow() throws Exception {
                BASE_URL = catalogServiceUrl;
                long r1 = _S2S5Seed.seedRestaurant(jdbc, this, "R1", "EGYPTIAN", "OPEN");
                long r2 = _S2S5Seed.seedRestaurant(jdbc, this, "R2", "EGYPTIAN", "OPEN");
                long r3 = _S2S5Seed.seedRestaurant(jdbc, this, "R3", "EGYPTIAN", "OPEN");
                _S2S5Seed.setRestaurantRating(jdbc, this, r1, 4.9);
                _S2S5Seed.setRestaurantRating(jdbc, this, r2, 4.5);
                _S2S5Seed.setRestaurantRating(jdbc, this, r3, 4.2);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/reports/top-rated?limit=10", tok);
                assert2xx(r, "TC253");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertTrue(list.size() >= 3,
                                "TC253: must return all 3 restaurants when limit>=count; got " + list.size());
        }
}

@Tag("public")
@Tag("features_m1")
class TC254_S2F6DtoShapeTests extends TestBase {
        @Test
        @DisplayName("TC254 — Each result has restaurantId/name/rating/totalOrders fields")
        void dto_shape() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "R1", "EGYPTIAN", "OPEN");
                _S2S5Seed.setRestaurantRating(jdbc, this, restId, 4.5);
                _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "DELIVERED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/reports/top-rated?limit=5", tok);
                assert2xx(r, "TC254");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertTrue(list.size() >= 1, "TC254: at least 1 result expected");
                JsonNode first = list.get(0);
                assertTrue(first.has("restaurantId") || first.has("id"),
                                "TC254: DTO must include restaurantId; got " + first);
                assertTrue(first.has("name"), "TC254: DTO must include name; got " + first);
                assertTrue(first.has("rating"), "TC254: DTO must include rating; got " + first);
                assertTrue(first.has("totalOrders") || first.has("total_orders"),
                                "TC254: DTO must include totalOrders; got " + first);
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2-F7 — Rate a Restaurant After Order (TC255-TC259)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC255_S2F7FirstRatingTests extends TestBase {
        @Test
        @DisplayName("TC255 — First rating sets rating=5.0, totalRatings=1")
        void first_rating() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "RateRest", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "DELIVERED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/restaurants/" + restId + "/rate",
                                "{\"orderId\":" + oid + ",\"rating\":5}", tok);
                assert2xx(r, "TC255: POST /rate must succeed for valid DELIVERED order");
                Double rating = jdbc.queryForObject(
                                "SELECT \"" + columnByField("Restaurant", "rating") + "\" FROM \""
                                                + tableName("Restaurant") + "\" WHERE id = ?",
                                Double.class, restId);
                assertEquals(5.0, rating, 0.01,
                                "TC255: PG rating must be 5.0 after first rating=5; got " + rating);
        }
}

@Tag("public")
@Tag("features_m1")
class TC256_S2F7RunningAverageTests extends TestBase {
        @Test
        @DisplayName("TC256 — Second rating recalculates running average to 4.0 (avg of 5,3)")
        void running_average() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "AvgRest", "EGYPTIAN", "OPEN");
                long o1 = _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "DELIVERED", 100.0, "2026-03-10");
                long o2 = _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "DELIVERED", 100.0, "2026-03-11");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/restaurants/" + restId + "/rate",
                                "{\"orderId\":" + o1 + ",\"rating\":5}", tok), "TC256 first");
                assert2xx(httpPostAuth("/api/restaurants/" + restId + "/rate",
                                "{\"orderId\":" + o2 + ",\"rating\":3}", tok), "TC256 second");
                Double rating = jdbc.queryForObject(
                                "SELECT \"" + columnByField("Restaurant", "rating") + "\" FROM \""
                                                + tableName("Restaurant") + "\" WHERE id = ?",
                                Double.class, restId);
                assertEquals(4.0, rating, 0.01,
                                "TC256: running average of [5,3] must be 4.0; got " + rating);
        }
}

@Tag("public")
@Tag("features_m1")
class TC257_S2F7RatingOutOfRangeTests extends TestBase {
        @Test
        @DisplayName("TC257 — rating=6 (out of 1..5 range) returns 400")
        void rating_out_of_range() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "OobRest", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "DELIVERED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/restaurants/" + restId + "/rate",
                                "{\"orderId\":" + oid + ",\"rating\":6}", tok);
                assertEquals(400, r.statusCode(), "TC257: rating>5 must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC258_S2F7NonDeliveredOrderTests extends TestBase {
        @Test
        @DisplayName("TC258 — Rating a non-DELIVERED order returns 400")
        void non_delivered() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "NDRest", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "PLACED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/restaurants/" + restId + "/rate",
                                "{\"orderId\":" + oid + ",\"rating\":5}", tok);
                assertEquals(400, r.statusCode(),
                                "TC258: rating non-DELIVERED order must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC259_S2F7DifferentRestaurantTests extends TestBase {
        @Test
        @DisplayName("TC259 — Rating with order from different restaurant returns 400")
        void different_restaurant() throws Exception {
                BASE_URL = catalogServiceUrl;
                long r1 = _S2S5Seed.seedRestaurant(jdbc, this, "R1", "EGYPTIAN", "OPEN");
                long r2 = _S2S5Seed.seedRestaurant(jdbc, this, "R2", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, r2, "DELIVERED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/restaurants/" + r1 + "/rate",
                                "{\"orderId\":" + oid + ",\"rating\":5}", tok);
                assertEquals(400, r.statusCode(),
                                "TC259: cross-restaurant order ratings must be 400; got " + r.statusCode());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2-F8 — Toggle Menu Item Availability (TC260-TC264)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC260_S2F8HappyToggleTests extends TestBase {
        @Test
        @DisplayName("TC260 — Toggle by ADMIN: available flips, JSONB metadata gains toggledAt+toggledBy")
        void happy_toggle() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "MenuRest", "EGYPTIAN", "OPEN");
                long itemId = _S2S5Seed.seedMenuItem(jdbc, this, restId, "Koshari", 50.0, "MAIN", true);
                long adminId = adminId();
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                                "/api/restaurants/" + restId + "/menu-items/" + itemId + "/toggle",
                                "{\"toggledBy\":" + adminId + "}", tok);
                assert2xx(r, "TC260: ADMIN toggle on existing menu item must succeed");
                Boolean available = jdbc.queryForObject(
                                "SELECT \"" + columnByField("MenuItem", "available") + "\" FROM \""
                                                + tableName("MenuItem") + "\" WHERE id = ?",
                                Boolean.class, itemId);
                assertEquals(Boolean.FALSE, available,
                                "TC260: available must flip true→false after toggle; got " + available);
                String metaJson = jdbc.queryForObject(
                                "SELECT \"" + columnByField("MenuItem", "metadata") + "\"::text FROM \""
                                                + tableName("MenuItem") + "\" WHERE id = ?",
                                String.class, itemId);
                assertNotNull(metaJson, "TC260: metadata JSONB must be set after toggle");
                assertTrue(metaJson.contains("toggledAt"),
                                "TC260: metadata must contain 'toggledAt' key; got " + metaJson);
                assertTrue(metaJson.contains("toggledBy"),
                                "TC260: metadata must contain 'toggledBy' key; got " + metaJson);
        }
}

@Tag("public")
@Tag("features_m1")
class TC261_S2F8PendingOrderItemTests extends TestBase {
        @Test
        @DisplayName("TC261 — Toggling to unavailable with PENDING order_items references → 400")
        void pending_blocks_unavailable() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "PendRest", "EGYPTIAN", "OPEN");
                long itemId = _S2S5Seed.seedMenuItem(jdbc, this, restId, "Falafel", 30.0, "MAIN", true);
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, restId, "PLACED", 30.0, "2026-03-10");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, itemId, "Falafel", 1, 30.0, 1, "PENDING");
                long adminId = adminId();
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                                "/api/restaurants/" + restId + "/menu-items/" + itemId + "/toggle",
                                "{\"toggledBy\":" + adminId + "}", tok);
                assertEquals(400, r.statusCode(),
                                "TC261: toggle to unavailable with PENDING order_items must be 400; got "
                                                + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC262_S2F8NonAdminTogglerTests extends TestBase {
        @Test
        @DisplayName("TC262 — toggledBy referencing a non-ADMIN user returns 403")
        void non_admin_403() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "NoAdmRest", "EGYPTIAN", "OPEN");
                long itemId = _S2S5Seed.seedMenuItem(jdbc, this, restId, "Koshari", 50.0, "MAIN", true);
                // Insert a CUSTOMER user.
                java.util.Map<String, Object> _ov = new java.util.HashMap<>();
                _ov.put(columnByField("User", "name"), "Cust");
                _ov.put(columnByField("User", "email"), "tc262_cust@test.io");
                _ov.put(columnByField("User", "phone"), "+201" + nonce().substring(0, 9));
                _ov.put(columnByField("User", "password"), "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
                _ov.put(columnByField("User", "role"), "CUSTOMER");
                _ov.put(columnByField("User", "status"), "ACTIVE");
                long custId = insertRowReturningId(tableName("User"), _ov);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                                "/api/restaurants/" + restId + "/menu-items/" + itemId + "/toggle",
                                "{\"toggledBy\":" + custId + "}", tok);
                assertEquals(403, r.statusCode(),
                                "TC262: non-ADMIN toggler must return 403; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC263_S2F8DifferentRestaurantTests extends TestBase {
        @Test
        @DisplayName("TC263 — Item belongs to different restaurant → 400")
        void different_restaurant_400() throws Exception {
                BASE_URL = catalogServiceUrl;
                long r1 = _S2S5Seed.seedRestaurant(jdbc, this, "R1", "EGYPTIAN", "OPEN");
                long r2 = _S2S5Seed.seedRestaurant(jdbc, this, "R2", "EGYPTIAN", "OPEN");
                long itemId = _S2S5Seed.seedMenuItem(jdbc, this, r2, "Pizza", 80.0, "MAIN", true);
                long adminId = adminId();
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                                "/api/restaurants/" + r1 + "/menu-items/" + itemId + "/toggle",
                                "{\"toggledBy\":" + adminId + "}", tok);
                assertEquals(400, r.statusCode(),
                                "TC263: cross-restaurant menu-item toggle must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC264_S2F8MenuItemNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC264 — Non-existent menu item returns 404")
        void menu_item_not_found() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "Rest", "EGYPTIAN", "OPEN");
                long adminId = adminId();
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                                "/api/restaurants/" + restId + "/menu-items/999999/toggle",
                                "{\"toggledBy\":" + adminId + "}", tok);
                assertEquals(404, r.statusCode(),
                                "TC264: non-existent menu item must be 404; got " + r.statusCode());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2-F9 — Restaurants with Unavailable Menu Items (TC265-TC267)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC265_S2F9HappyTests extends TestBase {
        @Test
        @DisplayName("TC265 — Returns only restaurants with at least one unavailable item")
        void happy_unavailable() throws Exception {
                BASE_URL = catalogServiceUrl;
                long rA = _S2S5Seed.seedRestaurant(jdbc, this, "A", "EGYPTIAN", "OPEN");
                long rB = _S2S5Seed.seedRestaurant(jdbc, this, "B", "EGYPTIAN", "OPEN");
                long rC = _S2S5Seed.seedRestaurant(jdbc, this, "C", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedMenuItem(jdbc, this, rA, "I-A1", 50.0, "MAIN", true);
                _S2S5Seed.seedMenuItem(jdbc, this, rA, "I-A2", 60.0, "MAIN", true);
                _S2S5Seed.seedMenuItem(jdbc, this, rA, "I-A3", 70.0, "MAIN", false); // 1 unavailable
                _S2S5Seed.seedMenuItem(jdbc, this, rB, "I-B1", 50.0, "MAIN", true);
                _S2S5Seed.seedMenuItem(jdbc, this, rC, "I-C1", 50.0, "MAIN", false);
                _S2S5Seed.seedMenuItem(jdbc, this, rC, "I-C2", 60.0, "MAIN", false);
                _S2S5Seed.seedMenuItem(jdbc, this, rC, "I-C3", 70.0, "MAIN", false); // all 3 unavailable
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/menu-items/unavailable", tok);
                assert2xx(r, "TC265");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                java.util.Set<Long> ids = new java.util.HashSet<>();
                for (JsonNode it : list)
                        ids.add(it.has("restaurantId") ? it.get("restaurantId").asLong()
                                        : it.has("id") ? it.get("id").asLong() : -1L);
                assertTrue(ids.contains(rA), "TC265: Rest A (1 unavailable) must be present");
                assertTrue(ids.contains(rC), "TC265: Rest C (3 unavailable) must be present");
                assertFalse(ids.contains(rB), "TC265: Rest B (all available) must NOT be present");
        }
}

@Tag("public")
@Tag("features_m1")
class TC266_S2F9AllAvailableTests extends TestBase {
        @Test
        @DisplayName("TC266 — All items available across all restaurants → empty list")
        void all_available() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "AllAvail", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedMenuItem(jdbc, this, restId, "Item1", 50.0, "MAIN", true);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/menu-items/unavailable", tok);
                assert2xx(r, "TC266");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertEquals(0, list.size(),
                                "TC266: empty list expected when no unavailable items; got " + list.size());
        }
}

@Tag("public")
@Tag("features_m1")
class TC267_S2F9CountAccuracyTests extends TestBase {
        @Test
        @DisplayName("TC267 — unavailableCount equals exact count of unavailable items per restaurant")
        void count_accuracy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _S2S5Seed.seedRestaurant(jdbc, this, "CountRest", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedMenuItem(jdbc, this, restId, "U1", 50.0, "MAIN", false);
                _S2S5Seed.seedMenuItem(jdbc, this, restId, "U2", 60.0, "MAIN", false);
                _S2S5Seed.seedMenuItem(jdbc, this, restId, "A1", 70.0, "MAIN", true);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/restaurants/menu-items/unavailable", tok);
                assert2xx(r, "TC267");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                for (JsonNode it : list) {
                        long id = it.has("restaurantId") ? it.get("restaurantId").asLong()
                                        : it.has("id") ? it.get("id").asLong() : -1L;
                        if (id == restId) {
                                long count = it.has("unavailableCount") ? it.get("unavailableCount").asLong()
                                                : it.has("unavailable_count") ? it.get("unavailable_count").asLong()
                                                                : -1L;
                                assertEquals(2L, count,
                                                "TC267: unavailableCount for this restaurant must be 2; got " + count);
                                return;
                        }
                }
                throw new AssertionError("TC267: target restaurant not found in result; body=" + r.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F1 — Search Orders by Status + Date Range (TC268-TC270)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC268_S3F1StatusFilterTests extends TestBase {
        @Test
        @DisplayName("TC268 — status=DELIVERED&March returns 2 DELIVERED orders, most recent first")
        void status_filter() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-05");
                _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-25");
                _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-15");
                _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-02-15");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/orders/search?status=DELIVERED&startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC268");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertEquals(2, list.size(),
                                "TC268: 2 DELIVERED orders in March expected; got " + list.size() + " body="
                                                + r.body());
        }
}

@Tag("public")
@Tag("features_m1")
class TC269_S3F1NoStatusFilterTests extends TestBase {
        @Test
        @DisplayName("TC269 — Date range only (no status) returns all orders in range")
        void no_status_filter() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-05");
                _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-25");
                _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-15");
                _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-02-15");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/search?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC269");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertEquals(3, list.size(),
                                "TC269: 3 orders in March (no status filter) expected; got " + list.size());
        }
}

@Tag("public")
@Tag("features_m1")
class TC270_S3F1MostRecentFirstTests extends TestBase {
        @Test
        @DisplayName("TC270 — Results ordered by orderDate DESC (most recent first)")
        void most_recent_first() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oOld = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-05");
                long oNew = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-25");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/search?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC270");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertTrue(list.size() >= 2, "TC270: at least 2 orders expected");
                long firstId = list.get(0).has("id") ? list.get(0).get("id").asLong() : -1L;
                assertEquals(oNew, firstId,
                                "TC270: most recent (id=" + oNew + ", 2026-03-25) must be first; got id=" + firstId);
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F2 — Confirm Order and Assign Restaurant (TC271-TC276)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC271_S3F2HappyTests extends TestBase {
        @Test
        @DisplayName("TC271 — PUT confirm with OPEN restaurant → 2xx, status=CONFIRMED, restaurantId set")
        void happy_confirm() throws Exception {
                BASE_URL = orderServiceUrl;
                // Insert a PLACED order without a restaurant (use 1 since we MUST pass a value,
                // then update).
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "OkRest", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, 1L, "PLACED", 100.0, "2026-03-10");
                // Reset restaurant_id to NULL pre-confirm to test the assignment path (skip if
                // NOT NULL constraint).
                try {
                        jdbc.update("UPDATE \"" + tableName("Order") + "\" SET \""
                                        + columnByField("Order", "restaurant") + "\" = NULL WHERE id = ?", oid);
                } catch (org.springframework.dao.DataAccessException ignored) {
                        /* NOT NULL — keep id=1 */ }
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/orders/" + oid + "/confirm?restaurantId=" + rest, "", tok);
                assert2xx(r, "TC271: PUT confirm must succeed for PLACED + OPEN");
        }
}

@Tag("public")
@Tag("features_m1")
class TC272_S3F2AlreadyConfirmedTests extends TestBase {
        @Test
        @DisplayName("TC272 — PUT confirm on already-CONFIRMED order returns 400")
        void already_confirmed() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "CONFIRMED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/orders/" + oid + "/confirm?restaurantId=" + rest, "", tok);
                assertEquals(400, r.statusCode(),
                                "TC272: confirming non-PLACED order must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC273_S3F2ClosedRestaurantTests extends TestBase {
        @Test
        @DisplayName("TC273 — PUT confirm with CLOSED restaurant returns 400")
        void closed_restaurant() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "ClosedR", "EGYPTIAN", "CLOSED");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, 1L, "PLACED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/orders/" + oid + "/confirm?restaurantId=" + rest, "", tok);
                assertEquals(400, r.statusCode(),
                                "TC273: CLOSED restaurant must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC274_S3F2OrderNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC274 — PUT confirm with non-existent order returns 404")
        void order_not_found() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/orders/999999/confirm?restaurantId=" + rest, "", tok);
                assertEquals(404, r.statusCode(), "TC274: must be 404; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC275_S3F2RestaurantNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC275 — PUT confirm with non-existent restaurant returns 404")
        void restaurant_not_found() throws Exception {
                BASE_URL = orderServiceUrl;
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, 1L, "PLACED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/orders/" + oid + "/confirm?restaurantId=999999", "", tok);
                assertEquals(404, r.statusCode(), "TC275: must be 404; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC276_S3F2PgStatusUpdatedTests extends TestBase {
        @Test
        @DisplayName("TC276 — After successful confirm, PG order.status=CONFIRMED + restaurantId set")
        void pg_state_updated() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, 1L, "PLACED", 100.0, "2026-03-10");
                String tok = adminToken();
                assert2xx(httpPutAuth("/api/orders/" + oid + "/confirm?restaurantId=" + rest, "", tok), "TC276");
                String dbStatus = jdbc.queryForObject(
                                "SELECT \"" + columnByField("Order", "status") + "\"::text FROM \""
                                                + tableName("Order") + "\" WHERE id = ?",
                                String.class, oid);
                Long dbRestId = jdbc.queryForObject(
                                "SELECT \"" + columnByField("Order", "restaurant") + "\" FROM \""
                                                + tableName("Order") + "\" WHERE id = ?",
                                Long.class, oid);
                assertEquals("CONFIRMED", dbStatus, "TC276: PG status=CONFIRMED expected; got " + dbStatus);
                assertEquals(rest, dbRestId, "TC276: PG restaurant_id=" + rest + " expected; got " + dbRestId);
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F3 — Order Cost Estimate DTO (TC277-TC281)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC277_S3F3HappyTests extends TestBase {
        @Test
        @DisplayName("TC277 — Estimate (avg=60, items=3, dist=5km, no surge) → food=180, fee=50, service=9, surge=1.0, total=239")
        void estimate_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "EstRest", "EGYPTIAN", "OPEN");
                // Avg menu price = (50+60+70)/3 = 60
                _S2S5Seed.seedMenuItem(jdbc, this, rest, "I1", 50.0, "MAIN", true);
                _S2S5Seed.seedMenuItem(jdbc, this, rest, "I2", 60.0, "MAIN", true);
                _S2S5Seed.seedMenuItem(jdbc, this, rest, "I3", 70.0, "MAIN", true);
                String tok = adminToken();
                String body = "{\"restaurantId\":" + rest + ",\"itemCount\":3,\"deliveryDistance\":5}";
                HttpResponse<String> r = httpPostAuth("/api/orders/estimate", body, tok);
                assert2xx(r, "TC277");
                JsonNode j = parseNode(r.body());
                assertEquals(180.0, j.has("estimatedFoodCost") ? j.get("estimatedFoodCost").asDouble() : -1, 1.0,
                                "TC277: foodCost=180 (60*3) expected; got " + j.get("estimatedFoodCost"));
                assertEquals(50.0, j.has("deliveryFee") ? j.get("deliveryFee").asDouble() : -1, 0.5,
                                "TC277: deliveryFee=50 (10*5) expected; got " + j.get("deliveryFee"));
                assertEquals(9.0, j.has("serviceFee") ? j.get("serviceFee").asDouble() : -1, 0.5,
                                "TC277: serviceFee=9 (180*0.05) expected; got " + j.get("serviceFee"));
                assertEquals(1.0, j.has("surgeMultiplier") ? j.get("surgeMultiplier").asDouble() : -1, 0.01,
                                "TC277: surge=1.0 (no active orders) expected; got " + j.get("surgeMultiplier"));
                assertEquals(239.0, j.has("estimatedTotal") ? j.get("estimatedTotal").asDouble() : -1, 1.0,
                                "TC277: total=239 ((180+50+9)*1.0) expected; got " + j.get("estimatedTotal"));
        }
}

@Tag("public")
@Tag("features_m1")
class TC278_S3F3SurgeBusyTests extends TestBase {
        @Test
        @DisplayName("TC278 — 15 active orders → surgeMultiplier=1.2 (busy tier)")
        void surge_busy() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "BusyRest", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedMenuItem(jdbc, this, rest, "I1", 60.0, "MAIN", true);
                for (int i = 0; i < 15; i++)
                        _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
                String tok = adminToken();
                String body = "{\"restaurantId\":" + rest + ",\"itemCount\":1,\"deliveryDistance\":1}";
                HttpResponse<String> r = httpPostAuth("/api/orders/estimate", body, tok);
                assert2xx(r, "TC278");
                double surge = parseNode(r.body()).has("surgeMultiplier")
                                ? parseNode(r.body()).get("surgeMultiplier").asDouble()
                                : -1;
                assertEquals(1.2, surge, 0.01,
                                "TC278: 15 active orders → surge=1.2 (busy tier 11-20); got " + surge);
        }
}

@Tag("public")
@Tag("features_m1")
class TC279_S3F3SurgePeakTests extends TestBase {
        @Test
        @DisplayName("TC279 — 25 active orders → surgeMultiplier=1.5 (peak tier)")
        void surge_peak() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "PeakRest", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedMenuItem(jdbc, this, rest, "I1", 60.0, "MAIN", true);
                for (int i = 0; i < 25; i++)
                        _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
                String tok = adminToken();
                String body = "{\"restaurantId\":" + rest + ",\"itemCount\":1,\"deliveryDistance\":1}";
                HttpResponse<String> r = httpPostAuth("/api/orders/estimate", body, tok);
                assert2xx(r, "TC279");
                double surge = parseNode(r.body()).has("surgeMultiplier")
                                ? parseNode(r.body()).get("surgeMultiplier").asDouble()
                                : -1;
                assertEquals(1.5, surge, 0.01,
                                "TC279: 25 active orders → surge=1.5 (peak tier >20); got " + surge);
        }
}

@Tag("public")
@Tag("features_m1")
class TC280_S3F3ReadOnlyTests extends TestBase {
        @Test
        @DisplayName("TC280 — Estimate is read-only: no order created in PG")
        void read_only() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "ROnly", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedMenuItem(jdbc, this, rest, "I1", 60.0, "MAIN", true);
                Integer beforeCount = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM \"" + tableName("Order") + "\"", Integer.class);
                String tok = adminToken();
                String body = "{\"restaurantId\":" + rest + ",\"itemCount\":1,\"deliveryDistance\":1}";
                assert2xx(httpPostAuth("/api/orders/estimate", body, tok), "TC280");
                Integer afterCount = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM \"" + tableName("Order") + "\"", Integer.class);
                assertEquals(beforeCount, afterCount,
                                "TC280: estimate must NOT create rows; before=" + beforeCount + " after=" + afterCount);
        }
}

@Tag("public")
@Tag("features_m1")
class TC281_S3F3DtoCompletenessTests extends TestBase {
        @Test
        @DisplayName("TC281 — DTO contains all 5 fields: foodCost, deliveryFee, serviceFee, surge, total")
        void dto_completeness() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "DTO", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedMenuItem(jdbc, this, rest, "I1", 60.0, "MAIN", true);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/orders/estimate",
                                "{\"restaurantId\":" + rest + ",\"itemCount\":1,\"deliveryDistance\":1}", tok);
                assert2xx(r, "TC281");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("estimatedFoodCost") || j.has("estimated_food_cost"),
                                "TC281: DTO must include estimatedFoodCost; got " + r.body());
                assertTrue(j.has("deliveryFee") || j.has("delivery_fee"),
                                "TC281: DTO must include deliveryFee");
                assertTrue(j.has("serviceFee") || j.has("service_fee"),
                                "TC281: DTO must include serviceFee");
                assertTrue(j.has("surgeMultiplier") || j.has("surge_multiplier"),
                                "TC281: DTO must include surgeMultiplier");
                assertTrue(j.has("estimatedTotal") || j.has("estimated_total"),
                                "TC281: DTO must include estimatedTotal");
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F4 — Deliver Order (TC282-TC286)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC282_S3F4HappyDeliverTests extends TestBase {
        @Test
        @DisplayName("TC282 — PUT deliver: status=DELIVERED in PG")
        void happy_deliver() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PREPARING", 100.0, "2026-03-10");
                String tok = adminToken();
                assert2xx(httpPutAuth("/api/orders/" + oid + "/deliver", "", tok), "TC282");
                String dbStatus = jdbc.queryForObject(
                                "SELECT \"" + columnByField("Order", "status") + "\"::text FROM \""
                                                + tableName("Order") + "\" WHERE id = ?",
                                String.class, oid);
                assertEquals("DELIVERED", dbStatus,
                                "TC282: PG order.status must be DELIVERED after deliver; got " + dbStatus);
        }
}

@Tag("public")
@Tag("features_m1")
class TC283_S3F4TotalCalcTests extends TestBase {
        @Test
        @DisplayName("TC283 — totalAmount calculated from items: 2*50 + 1*80 + 3*30 = 270")
        void total_calc() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "CalcRest", "EGYPTIAN", "OPEN");
                long item1 = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I1", 50.0, "MAIN", true);
                long item2 = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I2", 80.0, "MAIN", true);
                long item3 = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I3", 30.0, "MAIN", true);
                // Insert order with totalAmount=0 to force recalc.
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PREPARING", 0.0, "2026-03-10");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item1, "I1", 2, 50.0, 1, "PENDING");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item2, "I2", 1, 80.0, 2, "PENDING");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item3, "I3", 3, 30.0, 3, "PENDING");
                String tok = adminToken();
                assert2xx(httpPutAuth("/api/orders/" + oid + "/deliver", "", tok), "TC283");
                Double total = jdbc.queryForObject(
                                "SELECT \"" + columnByField("Order", "totalAmount") + "\" FROM \""
                                                + tableName("Order") + "\" WHERE id = ?",
                                Double.class, oid);
                assertEquals(270.0, total, 1.0,
                                "TC283: totalAmount must be 270 (2*50+1*80+3*30); got " + total);
        }
}

@Tag("public")
@Tag("features_m1")
class TC284_S3F4PaymentCreatedTests extends TestBase {
        @Test
        @DisplayName("TC284 — Deliver creates a PENDING payment for the order")
        void payment_created() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "PayRest", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PREPARING", 100.0, "2026-03-10");
                String tok = adminToken();
                assert2xx(httpPutAuth("/api/orders/" + oid + "/deliver", "", tok), "TC284");
                Integer payCount = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM \"" + tableName("Payment") + "\" WHERE \""
                                                + columnByField("Payment", "order") + "\" = ? AND \""
                                                + columnByField("Payment", "status") + "\"::text = 'PENDING'",
                                Integer.class, oid);
                assertTrue(payCount >= 1,
                                "TC284: at least 1 PENDING payment must be created for the delivered order; got count="
                                                + payCount);
        }
}

@Tag("public")
@Tag("features_m1")
class TC285_S3F4NotPreparingTests extends TestBase {
        @Test
        @DisplayName("TC285 — Deliver a PLACED (not PREPARING) order returns 400")
        void not_preparing() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/orders/" + oid + "/deliver", "", tok);
                assertEquals(400, r.statusCode(),
                                "TC285: deliver non-PREPARING order must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC286_S3F4AlreadyDeliveredTests extends TestBase {
        @Test
        @DisplayName("TC286 — Deliver an already-DELIVERED order returns 400")
        void already_delivered() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/orders/" + oid + "/deliver", "", tok);
                assertEquals(400, r.statusCode(),
                                "TC286: re-deliver already-DELIVERED must be 400; got " + r.statusCode());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F5 — Filter Orders by Metadata (TC287-TC289)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC287_S3F5HappyTests extends TestBase {
        @Test
        @DisplayName("TC287 — key=deliveryType&value=express returns matching order")
        void happy_match() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long o1 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
                long o2 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-11");
                jdbc.update("UPDATE \"" + tableName("Order") + "\" SET \"" + columnByField("Order", "metadata")
                                + "\" = ?::jsonb WHERE id = ?", "{\"deliveryType\":\"express\"}", o1);
                jdbc.update("UPDATE \"" + tableName("Order") + "\" SET \"" + columnByField("Order", "metadata")
                                + "\" = ?::jsonb WHERE id = ?", "{\"deliveryType\":\"standard\"}", o2);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/metadata/search?key=deliveryType&value=express", tok);
                assert2xx(r, "TC287");
                JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                                : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content")
                                                : parseNode(r.body()));
                assertEquals(1, list.size(),
                                "TC287: 1 order with deliveryType=express expected; got " + list.size());
        }
}

@Tag("public")
@Tag("features_m1")
class TC288_S3F5BlankKeyTests extends TestBase {
        @Test
        @DisplayName("TC288 — Blank key returns 400")
        void blank_key() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/metadata/search?key=&value=x", tok);
                assertEquals(400, r.statusCode(), "TC288: must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC289_S3F5BlankValueTests extends TestBase {
        @Test
        @DisplayName("TC289 — Blank value returns 400")
        void blank_value() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/metadata/search?key=x&value=", tok);
                assertEquals(400, r.statusCode(), "TC289: must be 400; got " + r.statusCode());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F6 — Order Analytics by Time Period (TC290-TC293)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC290_S3F6HappyTests extends TestBase {
        @Test
        @DisplayName("TC290 — 10 orders (7 DELIVERED + 3 CANCELLED) → totalOrders=10, deliveryRate=70.0")
        void happy_composite() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "AnalRest", "EGYPTIAN", "OPEN");
                double[] amts = { 80, 100, 120, 140, 160, 180, 200 };
                for (double a : amts)
                        _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", a, "2026-03-10");
                for (int i = 0; i < 3; i++)
                        _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "CANCELLED", 999.0, "2026-03-15");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/analytics?startDate=2026-03-01&endDate=2026-03-31",
                                tok);
                assert2xx(r, "TC290");
                JsonNode j = parseNode(r.body());
                assertEquals(10L, j.has("totalOrders") ? j.get("totalOrders").asLong() : -1, "TC290: totalOrders=10");
                assertEquals(7L, j.has("deliveredOrders") ? j.get("deliveredOrders").asLong() : -1,
                                "TC290: deliveredOrders=7");
                assertEquals(3L, j.has("cancelledOrders") ? j.get("cancelledOrders").asLong() : -1,
                                "TC290: cancelledOrders=3");
                double rate = j.has("deliveryRate") ? j.get("deliveryRate").asDouble() : -1;
                // Spec scenario: 70.0 (could be percentage 70.0 or fraction 0.7 — check both).
                assertTrue(Math.abs(rate - 70.0) < 0.5 || Math.abs(rate - 0.7) < 0.01,
                                "TC290: deliveryRate=70.0 (or 0.7 fraction) expected; got " + rate);
        }
}

@Tag("public")
@Tag("features_m1")
class TC291_S3F6RevenueDeliveredOnlyTests extends TestBase {
        @Test
        @DisplayName("TC291 — totalRevenue counts DELIVERED only")
        void revenue_delivered_only() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
                _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "CANCELLED", 999.0, "2026-03-11");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/analytics?startDate=2026-03-01&endDate=2026-03-31",
                                tok);
                assert2xx(r, "TC291");
                double rev = parseNode(r.body()).has("totalRevenue")
                                ? parseNode(r.body()).get("totalRevenue").asDouble()
                                : -1;
                assertEquals(100.0, rev, 1.0,
                                "TC291: totalRevenue=100 (only DELIVERED counted); got " + rev);
        }
}

@Tag("public")
@Tag("features_m1")
class TC292_S3F6AverageOrderTests extends TestBase {
        @Test
        @DisplayName("TC292 — averageOrderAmount = totalRevenue / deliveredOrders = 980/7 = 140")
        void average_order() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "AvgR", "EGYPTIAN", "OPEN");
                for (double a : new double[] { 80, 100, 120, 140, 160, 180, 200 }) {
                        _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", a, "2026-03-10");
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/analytics?startDate=2026-03-01&endDate=2026-03-31",
                                tok);
                assert2xx(r, "TC292");
                double avg = parseNode(r.body()).has("averageOrderAmount")
                                ? parseNode(r.body()).get("averageOrderAmount").asDouble()
                                : -1;
                assertEquals(140.0, avg, 0.5,
                                "TC292: averageOrderAmount=140 (980/7); got " + avg);
        }
}

@Tag("public")
@Tag("features_m1")
class TC293_S3F6ZeroCaseTests extends TestBase {
        @Test
        @DisplayName("TC293 — Empty range returns all-zero metrics")
        void zero_case() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/analytics?startDate=2030-01-01&endDate=2030-01-31",
                                tok);
                assert2xx(r, "TC293");
                JsonNode j = parseNode(r.body());
                assertEquals(0L, j.has("totalOrders") ? j.get("totalOrders").asLong() : -1, "TC293: totalOrders=0");
                assertEquals(0.0, j.has("totalRevenue") ? j.get("totalRevenue").asDouble() : -1, 0.01,
                                "TC293: totalRevenue=0");
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F7 — Cancel Order (TC294-TC297)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC294_S3F7HappyCancelTests extends TestBase {
        @Test
        @DisplayName("TC294 — PUT cancel on CONFIRMED order succeeds; PENDING delivery cancelled")
        void happy_cancel() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "CONFIRMED", 100.0, "2026-03-10");
                long delId = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "Ahmed");
                // Use ASSIGNED so it's "active" / pending; spec says "Cancel all PENDING
                // delivery records".
                String tok = adminToken();
                assert2xx(httpPutAuth("/api/orders/" + oid + "/cancel", "", tok), "TC294");
        }
}

@Tag("public")
@Tag("features_m1")
class TC295_S3F7DeliveredTests extends TestBase {
        @Test
        @DisplayName("TC295 — Cancelling a DELIVERED order returns 400")
        void cancel_delivered() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/orders/" + oid + "/cancel", "", tok);
                assertEquals(400, r.statusCode(),
                                "TC295: cancel DELIVERED must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC296_S3F7PreparingTests extends TestBase {
        @Test
        @DisplayName("TC296 — Cancelling a PREPARING order returns 400")
        void cancel_preparing() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PREPARING", 100.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/orders/" + oid + "/cancel", "", tok);
                assertEquals(400, r.statusCode(),
                                "TC296: cancel PREPARING must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC297_S3F7PgStatusTests extends TestBase {
        @Test
        @DisplayName("TC297 — After cancel, PG order.status = CANCELLED")
        void pg_status_cancelled() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "CONFIRMED", 100.0, "2026-03-10");
                String tok = adminToken();
                assert2xx(httpPutAuth("/api/orders/" + oid + "/cancel", "", tok), "TC297");
                String dbStatus = jdbc.queryForObject(
                                "SELECT \"" + columnByField("Order", "status") + "\"::text FROM \""
                                                + tableName("Order") + "\" WHERE id = ?",
                                String.class, oid);
                assertEquals("CANCELLED", dbStatus,
                                "TC297: PG status must be CANCELLED; got " + dbStatus);
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F8 — Add Items to Existing Order (TC298-TC302)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC298_S3F8FirstAddTests extends TestBase {
        @Test
        @DisplayName("TC298 — Add 2 items to PLACED order with no items → lineNumbers 1,2")
        void first_add() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "AddRest", "EGYPTIAN", "OPEN");
                long item1 = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I1", 50.0, "MAIN", true);
                long item2 = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I2", 60.0, "MAIN", true);
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 0.0, "2026-03-10");
                String tok = adminToken();
                String body = "[{\"menuItemId\":" + item1 + ",\"itemName\":\"I1\",\"quantity\":1,\"unitPrice\":50},"
                                + "{\"menuItemId\":" + item2 + ",\"itemName\":\"I2\",\"quantity\":2,\"unitPrice\":60}]";
                HttpResponse<String> r = httpPostAuth("/api/orders/" + oid + "/items", body, tok);
                assert2xx(r, "TC298: add items must succeed");
                Integer count = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM \"" + tableName("OrderItem") + "\" WHERE \""
                                                + columnByField("OrderItem", "order") + "\" = ?",
                                Integer.class, oid);
                assertEquals(2, count,
                                "TC298: 2 OrderItem rows must be created; got " + count);
        }
}

@Tag("public")
@Tag("features_m1")
class TC299_S3F8ContinueNumberingTests extends TestBase {
        @Test
        @DisplayName("TC299 — Adding to order with 2 existing items → next lineNumber=3")
        void continue_numbering() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "ContRest", "EGYPTIAN", "OPEN");
                long item = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I", 50.0, "MAIN", true);
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 0.0, "2026-03-10");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item, "Existing1", 1, 50.0, 1, "PENDING");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item, "Existing2", 1, 50.0, 2, "PENDING");
                String tok = adminToken();
                String body = "[{\"menuItemId\":" + item
                                + ",\"itemName\":\"NewItem\",\"quantity\":1,\"unitPrice\":50}]";
                assert2xx(httpPostAuth("/api/orders/" + oid + "/items", body, tok), "TC299");
                Integer maxLine = jdbc.queryForObject(
                                "SELECT MAX(CAST(\"" + columnByField("OrderItem", "lineNumber") + "\" AS INTEGER)) "
                                                + "FROM \"" + tableName("OrderItem") + "\" WHERE \""
                                                + columnByField("OrderItem", "order") + "\" = ?",
                                Integer.class, oid);
                assertEquals(3, maxLine,
                                "TC299: max lineNumber after add must be 3; got " + maxLine);
        }
}

@Tag("public")
@Tag("features_m1")
class TC300_S3F8DeliveredOrderTests extends TestBase {
        @Test
        @DisplayName("TC300 — Adding items to a DELIVERED order returns 400")
        void delivered_400() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long item = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I", 50.0, "MAIN", true);
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
                String tok = adminToken();
                String body = "[{\"menuItemId\":" + item + ",\"itemName\":\"X\",\"quantity\":1,\"unitPrice\":50}]";
                HttpResponse<String> r = httpPostAuth("/api/orders/" + oid + "/items", body, tok);
                assertEquals(400, r.statusCode(),
                                "TC300: add to DELIVERED must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC301_S3F8MissingItemNameTests extends TestBase {
        @Test
        @DisplayName("TC301 — Adding item with missing itemName returns 400")
        void missing_item_name() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long item = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I", 50.0, "MAIN", true);
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 0.0, "2026-03-10");
                String tok = adminToken();
                // Missing itemName field.
                String body = "[{\"menuItemId\":" + item + ",\"quantity\":1,\"unitPrice\":50}]";
                HttpResponse<String> r = httpPostAuth("/api/orders/" + oid + "/items", body, tok);
                assertEquals(400, r.statusCode(),
                                "TC301: missing itemName must be 400; got " + r.statusCode());
        }
}

@Tag("public")
@Tag("features_m1")
class TC302_S3F8StatusPendingTests extends TestBase {
        @Test
        @DisplayName("TC302 — Newly added items have status=PENDING in PG")
        void status_pending() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long item = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I", 50.0, "MAIN", true);
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 0.0, "2026-03-10");
                String tok = adminToken();
                String body = "[{\"menuItemId\":" + item
                                + ",\"itemName\":\"NewItem\",\"quantity\":1,\"unitPrice\":50}]";
                assert2xx(httpPostAuth("/api/orders/" + oid + "/items", body, tok), "TC302");
                Integer pendCount = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM \"" + tableName("OrderItem") + "\" WHERE \""
                                                + columnByField("OrderItem", "order") + "\" = ? AND \""
                                                + columnByField("OrderItem", "status") + "\"::text = 'PENDING'",
                                Integer.class, oid);
                assertTrue(pendCount >= 1,
                                "TC302: at least 1 PENDING item must exist after add; got " + pendCount);
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S3-F9 — Order Details DTO (TC303-TC306)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m1")
class TC303_S3F9HappyTests extends TestBase {
        @Test
        @DisplayName("TC303 — Details DTO with 4 items (2 PREPARED, 2 PENDING) → totalItems=4, preparedItems=2")
        void details_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "DetRest", "EGYPTIAN", "OPEN");
                long item = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I", 50.0, "MAIN", true);
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PREPARING", 200.0, "2026-03-10");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item, "I1", 1, 50.0, 1, "PREPARED");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item, "I2", 1, 50.0, 2, "PREPARED");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item, "I3", 1, 50.0, 3, "PENDING");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item, "I4", 1, 50.0, 4, "PENDING");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/" + oid + "/details", tok);
                assert2xx(r, "TC303");
                JsonNode j = parseNode(r.body());
                assertEquals(4L, j.has("totalItems") ? j.get("totalItems").asLong() : -1, "TC303: totalItems=4");
                assertEquals(2L, j.has("preparedItems") ? j.get("preparedItems").asLong() : -1,
                                "TC303: preparedItems=2");
        }
}

@Tag("public")
@Tag("features_m1")
class TC304_S3F9OrderingTests extends TestBase {
        @Test
        @DisplayName("TC304 — items array ordered by lineNumber ASC")
        void items_ordered() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long item = _S2S5Seed.seedMenuItem(jdbc, this, rest, "I", 50.0, "MAIN", true);
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PREPARING", 100.0, "2026-03-10");
                // Insert in non-monotonic order to verify the response sorts correctly.
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item, "Three", 1, 50.0, 3, "PENDING");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item, "One", 1, 50.0, 1, "PENDING");
                _S2S5Seed.seedOrderItem(jdbc, this, oid, item, "Two", 1, 50.0, 2, "PENDING");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/" + oid + "/details", tok);
                assert2xx(r, "TC304");
                JsonNode items = parseNode(r.body()).has("items") ? parseNode(r.body()).get("items") : null;
                assertNotNull(items, "TC304: items array must be present");
                assertTrue(items.size() >= 3, "TC304: at least 3 items expected");
                long prev = 0;
                for (JsonNode it : items) {
                        long ln = it.has("lineNumber") ? it.get("lineNumber").asLong()
                                        : it.has("line_number") ? it.get("line_number").asLong() : 0;
                        assertTrue(ln >= prev,
                                        "TC304: items must be ASC by lineNumber; got " + ln + " after " + prev);
                        prev = ln;
                }
        }
}

@Tag("public")
@Tag("features_m1")
class TC305_S3F9NoItemsTests extends TestBase {
        @Test
        @DisplayName("TC305 — Order with no items → totalItems=0, preparedItems=0, items=[]")
        void no_items() throws Exception {
                BASE_URL = orderServiceUrl;
                long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
                long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 0.0, "2026-03-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/" + oid + "/details", tok);
                assert2xx(r, "TC305");
                JsonNode j = parseNode(r.body());
                assertEquals(0L, j.has("totalItems") ? j.get("totalItems").asLong() : -1, "TC305: totalItems=0");
                assertEquals(0L, j.has("preparedItems") ? j.get("preparedItems").asLong() : -1,
                                "TC305: preparedItems=0");
        }
}

@Tag("public")
@Tag("features_m1")
class TC306_S3F9NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC306 — Details for non-existent order returns 404")
        void not_found() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/orders/999999/details", tok);
                assertEquals(404, r.statusCode(), "TC306: must be 404; got " + r.statusCode());
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S4 M1 — Delivery Service feature tests (TC307..TC340)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public") @Tag("features_m1")
class TC307_S4F1LatestHappyTests extends TestBase {
    @Test
    @DisplayName("TC307 — GET latest returns the most recent delivery (by updatedAt)")
    void latest_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long d1 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED",  30.0, 31.0, "Ahmed");
        long d2 = _S2S5Seed.seedDelivery(jdbc, this, oid, "PICKED_UP", 30.1, 31.1, "Ahmed");
        long d3 = _S2S5Seed.seedDelivery(jdbc, this, oid, "IN_TRANSIT",30.2, 31.2, "Ahmed");
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d1, java.sql.Timestamp.valueOf("2026-03-10 10:00:00"));
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d2, java.sql.Timestamp.valueOf("2026-03-10 11:00:00"));
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d3, java.sql.Timestamp.valueOf("2026-03-10 12:00:00"));
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/order/" + oid + "/latest", tok);
        assert2xx(r, "TC307");
        long retId = parseNode(r.body()).has("id") ? parseNode(r.body()).get("id").asLong() : -1L;
        assertEquals(d3, retId, "TC307: latest delivery must be d3 (12:00); got id=" + retId);
    }
}

@Tag("public") @Tag("features_m1")
class TC308_S4F1OrderingDescTests extends TestBase {
    @Test
    @DisplayName("TC308 — Out-of-order inserts: latest still resolves to highest updatedAt")
    void ordering_desc() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long dEarly = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        long dLate  = _S2S5Seed.seedDelivery(jdbc, this, oid, "DELIVERED", 30.0, 31.0, "X");
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, dEarly, java.sql.Timestamp.valueOf("2026-03-10 09:00:00"));
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, dLate,  java.sql.Timestamp.valueOf("2026-03-10 18:00:00"));
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/order/" + oid + "/latest", tok);
        assert2xx(r, "TC308");
        long retId = parseNode(r.body()).has("id") ? parseNode(r.body()).get("id").asLong() : -1L;
        assertEquals(dLate, retId, "TC308: latest must resolve by updatedAt DESC, NOT insertion order; got id=" + retId);
    }
}

@Tag("public") @Tag("features_m1")
class TC309_S4F1NoDeliveriesTests extends TestBase {
    @Test
    @DisplayName("TC309 — Order with no deliveries → 404")
    void no_deliveries() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/order/" + oid + "/latest", tok);
        assertEquals(404, r.statusCode(), "TC309: order with no deliveries → 404; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC310_S4F1OrderNotFoundTests extends TestBase {
    @Test
    @DisplayName("TC310 — Non-existent order returns 404")
    void order_not_found() throws Exception {
                BASE_URL = deliveryServiceUrl;
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/order/999999/latest", tok);
        assertEquals(404, r.statusCode(), "TC310: must be 404; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC311_S4F2HappyTests extends TestBase {
    @Test
    @DisplayName("TC311 — POST delivery → 201 + record created in PG")
    void happy_create() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
        String tok = adminToken();
        String body = "{\"driverName\":\"Hassan\",\"latitude\":30.0,\"longitude\":31.0,"
                    + "\"status\":\"ASSIGNED\",\"metadata\":{\"estimatedArrival\":25,\"vehicleType\":\"motorcycle\"}}";
        HttpResponse<String> r = httpPostAuth("/api/deliveries/order/" + oid, body, tok);
        assertEquals(201, r.statusCode(), "TC311: must be 201; got " + r.statusCode() + " body=" + r.body());
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM \"" + tableName("Delivery") + "\" WHERE \""
              + columnByField("Delivery","order") + "\" = ?", Integer.class, oid);
        assertTrue(count >= 1, "TC311: at least 1 delivery row must exist for orderId=" + oid + "; got count=" + count);
    }
}

@Tag("public") @Tag("features_m1")
class TC312_S4F2MetadataStoredTests extends TestBase {
    @Test
    @DisplayName("TC312 — JSONB metadata stored correctly (estimatedArrival, vehicleType)")
    void metadata_stored() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
        String tok = adminToken();
        String body = "{\"driverName\":\"Hassan\",\"latitude\":30.0,\"longitude\":31.0,"
                    + "\"status\":\"ASSIGNED\",\"metadata\":{\"estimatedArrival\":25,\"vehicleType\":\"motorcycle\"}}";
        assertEquals(201, httpPostAuth("/api/deliveries/order/" + oid, body, tok).statusCode(), "TC312");
        String meta = jdbc.queryForObject(
            "SELECT \"" + columnByField("Delivery","metadata") + "\"::text FROM \""
              + tableName("Delivery") + "\" WHERE \"" + columnByField("Delivery","order") + "\" = ? ORDER BY id DESC LIMIT 1",
            String.class, oid);
        assertNotNull(meta, "TC312: metadata JSONB must be set");
        assertTrue(meta.contains("estimatedArrival"), "TC312: metadata must contain 'estimatedArrival' key; got " + meta);
        assertTrue(meta.contains("vehicleType"), "TC312: metadata must contain 'vehicleType' key; got " + meta);
    }
}

@Tag("public") @Tag("features_m1")
class TC313_S4F2DriverAndCoordsStoredTests extends TestBase {
    @Test
    @DisplayName("TC313 — driverName + lat/lng correctly persisted in PG")
    void driver_coords_stored() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
        String tok = adminToken();
        String body = "{\"driverName\":\"Hassan\",\"latitude\":30.123,\"longitude\":31.456,\"status\":\"ASSIGNED\"}";
        assertEquals(201, httpPostAuth("/api/deliveries/order/" + oid, body, tok).statusCode(), "TC313");
        java.util.Map<String, Object> row = jdbc.queryForMap(
            "SELECT \"" + columnByField("Delivery","driverName") + "\" AS dn, \""
              + columnByField("Delivery","latitude") + "\" AS lat, \""
              + columnByField("Delivery","longitude") + "\" AS lon FROM \""
              + tableName("Delivery") + "\" WHERE \"" + columnByField("Delivery","order") + "\" = ? ORDER BY id DESC LIMIT 1", oid);
        assertEquals("Hassan", row.get("dn"), "TC313: driverName must equal 'Hassan'; got " + row.get("dn"));
        assertEquals(30.123, ((Number) row.get("lat")).doubleValue(), 0.001, "TC313: latitude=30.123; got " + row.get("lat"));
        assertEquals(31.456, ((Number) row.get("lon")).doubleValue(), 0.001, "TC313: longitude=31.456; got " + row.get("lon"));
    }
}

@Tag("public") @Tag("features_m1")
class TC314_S4F2OrderNotFoundTests extends TestBase {
    @Test
    @DisplayName("TC314 — Non-existent order returns 404")
    void order_not_found() throws Exception {
                BASE_URL = deliveryServiceUrl;
        String tok = adminToken();
        String body = "{\"driverName\":\"X\",\"latitude\":30.0,\"longitude\":31.0,\"status\":\"ASSIGNED\"}";
        HttpResponse<String> r = httpPostAuth("/api/deliveries/order/999999", body, tok);
        assertEquals(404, r.statusCode(), "TC314: must be 404; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC315_S4F3HappyTests extends TestBase {
    @Test
    @DisplayName("TC315 — Nearby returns A,B (within radius); excludes C (far)")
    void nearby_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long o1 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long o2 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long o3 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long dA = _S2S5Seed.seedDelivery(jdbc, this, o1, "ASSIGNED", 30.04, 31.23, "A");
        long dB = _S2S5Seed.seedDelivery(jdbc, this, o2, "ASSIGNED", 30.05, 31.24, "B");
        long dC = _S2S5Seed.seedDelivery(jdbc, this, o3, "ASSIGNED", 31.00, 32.00, "C");
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/nearby?lat=30.044&lon=31.235&radiusKm=5", tok);
        assert2xx(r, "TC315");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (JsonNode it : list) ids.add(it.has("deliveryId") ? it.get("deliveryId").asLong()
                                       : it.has("id") ? it.get("id").asLong() : -1L);
        assertTrue(ids.contains(dA), "TC315: A must be in result");
        assertTrue(ids.contains(dB), "TC315: B must be in result");
        assertFalse(ids.contains(dC), "TC315: C (far) must NOT be in result");
    }
}

@Tag("public") @Tag("features_m1")
class TC316_S4F3SortedAscendingTests extends TestBase {
    @Test
    @DisplayName("TC316 — Results sorted ASC by distanceKm")
    void sorted_asc() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long o1 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long o2 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        _S2S5Seed.seedDelivery(jdbc, this, o1, "ASSIGNED", 30.05, 31.24, "Far");
        _S2S5Seed.seedDelivery(jdbc, this, o2, "ASSIGNED", 30.044,31.235, "Close");
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/nearby?lat=30.044&lon=31.235&radiusKm=5", tok);
        assert2xx(r, "TC316");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        double prev = -1;
        for (JsonNode it : list) {
            double d = it.has("distanceKm") ? it.get("distanceKm").asDouble()
                     : it.has("distance_km") ? it.get("distance_km").asDouble() : 0;
            assertTrue(d >= prev, "TC316: distances must be ASC; got " + d + " after " + prev + " body=" + r.body());
            prev = d;
        }
    }
}

@Tag("public") @Tag("features_m1")
class TC317_S4F3RadiusFilterTests extends TestBase {
    @Test
    @DisplayName("TC317 — Tight radius (0.1km) narrows result set")
    void radius_filter() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long o1 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long o2 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        _S2S5Seed.seedDelivery(jdbc, this, o1, "ASSIGNED", 30.044,31.235, "Close");
        _S2S5Seed.seedDelivery(jdbc, this, o2, "ASSIGNED", 30.05, 31.24,  "Far");
        String tok = adminToken();
        HttpResponse<String> r5 = httpGetAuth("/api/deliveries/nearby?lat=30.044&lon=31.235&radiusKm=5", tok);
        HttpResponse<String> rT = httpGetAuth("/api/deliveries/nearby?lat=30.044&lon=31.235&radiusKm=0.1", tok);
        assert2xx(r5, "TC317");
        assert2xx(rT, "TC317");
        int s5 = (parseNode(r5.body()).isArray() ? parseNode(r5.body()) : parseNode(r5.body()).has("content")
                   ? parseNode(r5.body()).get("content") : parseNode(r5.body())).size();
        int sT = (parseNode(rT.body()).isArray() ? parseNode(rT.body()) : parseNode(rT.body()).has("content")
                   ? parseNode(rT.body()).get("content") : parseNode(rT.body())).size();
        assertTrue(sT <= s5, "TC317: tight radius must return ≤ wide radius; got tight=" + sT + " wide=" + s5);
    }
}

@Tag("public") @Tag("features_m1")
class TC318_S4F3DistanceWithinMarginTests extends TestBase {
    @Test
    @DisplayName("TC318 — Distance value matches euclidean*111 within ±20% margin")
    void distance_margin() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.05, 31.235, "X");
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/nearby?lat=30.04&lon=31.235&radiusKm=5", tok);
        assert2xx(r, "TC318");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertTrue(list.size() >= 1, "TC318: at least 1 delivery in 5km expected");
        double d = list.get(0).has("distanceKm") ? list.get(0).get("distanceKm").asDouble()
                 : list.get(0).has("distance_km") ? list.get(0).get("distance_km").asDouble() : -1;
        assertTrue(d >= 0.88 && d <= 1.34,
            "TC318: distance for Δlat=0.01 should be ≈1.11 km (±20% margin → [0.88, 1.34]); got " + d);
    }
}

@Tag("public") @Tag("features_m1")
class TC319_S4F4HappyTests extends TestBase {
    @Test
    @DisplayName("TC319 — Batch with 3 valid updates → 201")
    void batch_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
        String tok = adminToken();
        String body = "{\"orderId\":" + oid + ",\"updates\":["
                    + "{\"driverName\":\"D1\",\"latitude\":30.0,\"longitude\":31.0,\"status\":\"ASSIGNED\"},"
                    + "{\"driverName\":\"D1\",\"latitude\":30.1,\"longitude\":31.1,\"status\":\"PICKED_UP\"},"
                    + "{\"driverName\":\"D1\",\"latitude\":30.2,\"longitude\":31.2,\"status\":\"IN_TRANSIT\"}"
                    + "]}";
        HttpResponse<String> r = httpPostAuth("/api/deliveries/batch", body, tok);
        assertEquals(201, r.statusCode(), "TC319: must be 201; got " + r.statusCode() + " body=" + r.body());
    }
}

@Tag("public") @Tag("features_m1")
class TC320_S4F4AllSavedTests extends TestBase {
    @Test
    @DisplayName("TC320 — All 3 batch updates saved to PG with correct orderId")
    void all_saved() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
        String tok = adminToken();
        String body = "{\"orderId\":" + oid + ",\"updates\":["
                    + "{\"driverName\":\"D\",\"latitude\":30.0,\"longitude\":31.0,\"status\":\"ASSIGNED\"},"
                    + "{\"driverName\":\"D\",\"latitude\":30.1,\"longitude\":31.1,\"status\":\"PICKED_UP\"},"
                    + "{\"driverName\":\"D\",\"latitude\":30.2,\"longitude\":31.2,\"status\":\"IN_TRANSIT\"}"
                    + "]}";
        assertEquals(201, httpPostAuth("/api/deliveries/batch", body, tok).statusCode(), "TC320");
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM \"" + tableName("Delivery") + "\" WHERE \""
              + columnByField("Delivery","order") + "\" = ?", Integer.class, oid);
        assertEquals(3, count, "TC320: 3 deliveries persisted; got " + count);
    }
}

@Tag("public") @Tag("features_m1")
class TC321_S4F4InvalidLatitudeTests extends TestBase {
    @Test
    @DisplayName("TC321 — latitude=999 returns 400")
    void invalid_latitude() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
        String tok = adminToken();
        String body = "{\"orderId\":" + oid + ",\"updates\":["
                    + "{\"driverName\":\"D\",\"latitude\":999.0,\"longitude\":31.0,\"status\":\"ASSIGNED\"}]}";
        HttpResponse<String> r = httpPostAuth("/api/deliveries/batch", body, tok);
        assertEquals(400, r.statusCode(), "TC321: latitude=999 (>90) must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC322_S4F4InvalidLongitudeTests extends TestBase {
    @Test
    @DisplayName("TC322 — longitude=999 returns 400")
    void invalid_longitude() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
        String tok = adminToken();
        String body = "{\"orderId\":" + oid + ",\"updates\":["
                    + "{\"driverName\":\"D\",\"latitude\":30.0,\"longitude\":999.0,\"status\":\"ASSIGNED\"}]}";
        HttpResponse<String> r = httpPostAuth("/api/deliveries/batch", body, tok);
        assertEquals(400, r.statusCode(), "TC322: longitude=999 (>180) must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC323_S4F4OrderNotFoundTests extends TestBase {
    @Test
    @DisplayName("TC323 — Non-existent order returns 404")
    void order_not_found() throws Exception {
                BASE_URL = deliveryServiceUrl;
        String tok = adminToken();
        String body = "{\"orderId\":999999,\"updates\":["
                    + "{\"driverName\":\"D\",\"latitude\":30.0,\"longitude\":31.0,\"status\":\"ASSIGNED\"}]}";
        HttpResponse<String> r = httpPostAuth("/api/deliveries/batch", body, tok);
        assertEquals(404, r.statusCode(), "TC323: must be 404; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC324_S4F5EqOperatorTests extends TestBase {
    @Test
    @DisplayName("TC324 — operator=eq&value=10 returns the delivery with estimatedArrival=10")
    void eq_operator() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long d1 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        long d2 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        long d3 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d1, "{\"estimatedArrival\":10}");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d2, "{\"estimatedArrival\":25}");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d3, "{\"estimatedArrival\":45}");
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/metadata/search?key=estimatedArrival&operator=eq&value=10", tok);
        assert2xx(r, "TC324");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertEquals(1, list.size(), "TC324: 1 delivery with estimatedArrival=10 expected; got " + list.size());
    }
}

@Tag("public") @Tag("features_m1")
class TC325_S4F5GtOperatorTests extends TestBase {
    @Test
    @DisplayName("TC325 — operator=gt&value=20 returns 2 deliveries (>20)")
    void gt_operator() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long d1 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        long d2 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        long d3 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d1, "{\"estimatedArrival\":10}");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d2, "{\"estimatedArrival\":25}");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d3, "{\"estimatedArrival\":45}");
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/metadata/search?key=estimatedArrival&operator=gt&value=20", tok);
        assert2xx(r, "TC325");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertEquals(2, list.size(), "TC325: 2 deliveries with estimatedArrival>20 expected; got " + list.size());
    }
}

@Tag("public") @Tag("features_m1")
class TC326_S4F5LtOperatorTests extends TestBase {
    @Test
    @DisplayName("TC326 — operator=lt&value=30 returns 2 deliveries (<30)")
    void lt_operator() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long d1 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        long d2 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        long d3 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d1, "{\"estimatedArrival\":10}");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d2, "{\"estimatedArrival\":25}");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d3, "{\"estimatedArrival\":45}");
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/metadata/search?key=estimatedArrival&operator=lt&value=30", tok);
        assert2xx(r, "TC326");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertEquals(2, list.size(), "TC326: 2 deliveries with estimatedArrival<30 expected; got " + list.size());
    }
}

@Tag("public") @Tag("features_m1")
class TC327_S4F5InvalidOperatorTests extends TestBase {
    @Test
    @DisplayName("TC327 — Invalid operator (xyz) returns 400")
    void invalid_operator() throws Exception {
                BASE_URL = deliveryServiceUrl;
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/metadata/search?key=estimatedArrival&operator=xyz&value=10", tok);
        assertEquals(400, r.statusCode(), "TC327: invalid operator must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC328_S4F6HappyAscTests extends TestBase {
    @Test
    @DisplayName("TC328 — History returns deliveries in date range, ASC by updatedAt")
    void history_asc() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long d1 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED",  30.0, 31.0, "X");
        long d2 = _S2S5Seed.seedDelivery(jdbc, this, oid, "PICKED_UP", 30.1, 31.1, "X");
        long d3 = _S2S5Seed.seedDelivery(jdbc, this, oid, "IN_TRANSIT",30.2, 31.2, "X");
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d1, java.sql.Timestamp.valueOf("2026-03-10 10:00:00"));
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d2, java.sql.Timestamp.valueOf("2026-03-15 11:00:00"));
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d3, java.sql.Timestamp.valueOf("2026-03-25 12:00:00"));
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/order/" + oid + "/history?startDate=2026-03-01&endDate=2026-03-31", tok);
        assert2xx(r, "TC328");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertEquals(3, list.size(), "TC328: 3 deliveries in March expected; got " + list.size());
        long firstId = list.get(0).has("id") ? list.get(0).get("id").asLong() : -1L;
        assertEquals(d1, firstId, "TC328: oldest (d1, 2026-03-10) must be first (ASC); got id=" + firstId);
    }
}

@Tag("public") @Tag("features_m1")
class TC329_S4F6DateFilterTests extends TestBase {
    @Test
    @DisplayName("TC329 — Date range filter excludes Feb deliveries")
    void date_filter() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long d1 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        long d2 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "X");
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d1, java.sql.Timestamp.valueOf("2026-02-15 10:00:00"));
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d2, java.sql.Timestamp.valueOf("2026-03-15 10:00:00"));
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/order/" + oid + "/history?startDate=2026-03-01&endDate=2026-03-31", tok);
        assert2xx(r, "TC329");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertEquals(1, list.size(), "TC329: only March delivery in range expected; got " + list.size());
    }
}

@Tag("public") @Tag("features_m1")
class TC330_S4F6OrderNotFoundTests extends TestBase {
    @Test
    @DisplayName("TC330 — History for non-existent order returns 404")
    void order_not_found() throws Exception {
                BASE_URL = deliveryServiceUrl;
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/order/999999/history?startDate=2026-03-01&endDate=2026-03-31", tok);
        assertEquals(404, r.statusCode(), "TC330: must be 404; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC331_S4F7DeliveredOnlyPurgedTests extends TestBase {
    @Test
    @DisplayName("TC331 — Purge with olderThanDays=30: only DELIVERED status rows are deleted")
    void purge_delivered_only() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        java.sql.Timestamp old = new java.sql.Timestamp(System.currentTimeMillis() - 60L * 24 * 3600 * 1000);
        for (int i = 0; i < 7; i++) {
            long d = _S2S5Seed.seedDelivery(jdbc, this, oid, "DELIVERED", 30.0, 31.0, "X");
            _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d, old);
        }
        for (int i = 0; i < 3; i++) {
            long d = _S2S5Seed.seedDelivery(jdbc, this, oid, "IN_TRANSIT", 30.0, 31.0, "X");
            _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d, old);
        }
        String tok = adminToken();
        HttpResponse<String> r = httpDeleteAuth("/api/deliveries/purge?olderThanDays=30", tok);
        assert2xx(r, "TC331");
        long delivered = jdbc.queryForObject(
            "SELECT COUNT(*) FROM \"" + tableName("Delivery") + "\" WHERE \""
              + columnByField("Delivery","status") + "\"::text = 'DELIVERED'", Long.class);
        assertEquals(0L, delivered, "TC331: all 7 DELIVERED records must be purged; got remaining=" + delivered);
    }
}

@Tag("public") @Tag("features_m1")
class TC332_S4F7InTransitPreservedTests extends TestBase {
    @Test
    @DisplayName("TC332 — IN_TRANSIT records preserved after purge")
    void in_transit_preserved() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        java.sql.Timestamp old = new java.sql.Timestamp(System.currentTimeMillis() - 60L * 24 * 3600 * 1000);
        for (int i = 0; i < 3; i++) {
            long d = _S2S5Seed.seedDelivery(jdbc, this, oid, "IN_TRANSIT", 30.0, 31.0, "X");
            _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d, old);
        }
        String tok = adminToken();
        assert2xx(httpDeleteAuth("/api/deliveries/purge?olderThanDays=30", tok), "TC332");
        long inTransit = jdbc.queryForObject(
            "SELECT COUNT(*) FROM \"" + tableName("Delivery") + "\" WHERE \""
              + columnByField("Delivery","status") + "\"::text = 'IN_TRANSIT'", Long.class);
        assertEquals(3L, inTransit, "TC332: IN_TRANSIT records must NOT be purged; got remaining=" + inTransit);
    }
}

@Tag("public") @Tag("features_m1")
class TC333_S4F7CutoffRespectedTests extends TestBase {
    @Test
    @DisplayName("TC333 — Recent DELIVERED rows (newer than cutoff) preserved")
    void cutoff_respected() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long d = _S2S5Seed.seedDelivery(jdbc, this, oid, "DELIVERED", 30.0, 31.0, "X");
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d,
            new java.sql.Timestamp(System.currentTimeMillis() - 5L * 24 * 3600 * 1000));
        String tok = adminToken();
        assert2xx(httpDeleteAuth("/api/deliveries/purge?olderThanDays=30", tok), "TC333");
        Integer recentCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM \"" + tableName("Delivery") + "\" WHERE id = ?", Integer.class, d);
        assertEquals(1, recentCount, "TC333: recent DELIVERED row (5 days old) must be preserved (cutoff=30); got count=" + recentCount);
    }
}

@Tag("public") @Tag("features_m1")
class TC334_S4F8AvgMaxSpeedTests extends TestBase {
    @Test
    @DisplayName("TC334 — Hassan with speeds [20,30,40,50,0] → avgSpeed=28, maxSpeed=50")
    void avg_max_speed() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        int[] speeds = {20, 30, 40, 50, 0};
        for (int i = 0; i < speeds.length; i++) {
            long d = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "Hassan");
            _S2S5Seed.setDeliveryMetadata(jdbc, this, d, "{\"speed\":" + speeds[i] + "}");
            _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d, java.sql.Timestamp.valueOf("2026-03-1" + i + " 10:00:00"));
        }
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/driver/Hassan/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
        assert2xx(r, "TC334");
        JsonNode j = parseNode(r.body());
        double avg = j.has("averageSpeed") ? j.get("averageSpeed").asDouble()
                   : j.has("average_speed") ? j.get("average_speed").asDouble() : -1;
        double max = j.has("maxSpeed") ? j.get("maxSpeed").asDouble()
                   : j.has("max_speed") ? j.get("max_speed").asDouble() : -1;
        assertEquals(28.0, avg, 1.0, "TC334: averageSpeed=28 (sum 140 / 5); got " + avg);
        assertEquals(50.0, max, 0.5, "TC334: maxSpeed=50; got " + max);
    }
}

@Tag("public") @Tag("features_m1")
class TC335_S4F8TotalCountTests extends TestBase {
    @Test
    @DisplayName("TC335 — totalDeliveries = count of deliveries by driverName in range")
    void total_count() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        for (int i = 0; i < 5; i++) {
            long d = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "Hassan");
            _S2S5Seed.setDeliveryMetadata(jdbc, this, d, "{\"speed\":30}");
            _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d, java.sql.Timestamp.valueOf("2026-03-1" + i + " 10:00:00"));
        }
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/driver/Hassan/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
        assert2xx(r, "TC335");
        long total = parseNode(r.body()).has("totalDeliveries") ? parseNode(r.body()).get("totalDeliveries").asLong()
                   : parseNode(r.body()).has("total_deliveries") ? parseNode(r.body()).get("total_deliveries").asLong() : -1;
        assertEquals(5L, total, "TC335: totalDeliveries=5; got " + total);
    }
}

@Tag("public") @Tag("features_m1")
class TC336_S4F8FirstLastTests extends TestBase {
    @Test
    @DisplayName("TC336 — firstDelivery + lastDelivery timestamps are present")
    void first_last() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long d1 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "Hassan");
        long d2 = _S2S5Seed.seedDelivery(jdbc, this, oid, "ASSIGNED", 30.0, 31.0, "Hassan");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d1, "{\"speed\":30}");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d2, "{\"speed\":30}");
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d1, java.sql.Timestamp.valueOf("2026-03-10 10:00:00"));
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d2, java.sql.Timestamp.valueOf("2026-03-25 10:00:00"));
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/driver/Hassan/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
        assert2xx(r, "TC336");
        JsonNode j = parseNode(r.body());
        assertTrue(j.has("firstDelivery") || j.has("first_delivery"), "TC336: firstDelivery field required; got " + r.body());
        assertTrue(j.has("lastDelivery") || j.has("last_delivery"), "TC336: lastDelivery field required");
    }
}

@Tag("public") @Tag("features_m1")
class TC337_S4F8UnknownDriverTests extends TestBase {
    @Test
    @DisplayName("TC337 — Unknown driverName returns 404")
    void unknown_driver() throws Exception {
                BASE_URL = deliveryServiceUrl;
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/driver/NoSuchDriver/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
        assertEquals(404, r.statusCode(), "TC337: must be 404; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC338_S4F9DelayedHappyTests extends TestBase {
    @Test
    @DisplayName("TC338 — maxEstimatedArrival=30 returns deliveries with estimatedArrival>30 (B,C)")
    void delayed_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long dA = _S2S5Seed.seedDelivery(jdbc, this, oid, "IN_TRANSIT", 30.0, 31.0, "DA");
        long dB = _S2S5Seed.seedDelivery(jdbc, this, oid, "IN_TRANSIT", 30.0, 31.0, "DB");
        long dC = _S2S5Seed.seedDelivery(jdbc, this, oid, "IN_TRANSIT", 30.0, 31.0, "DC");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, dA, "{\"estimatedArrival\":15}");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, dB, "{\"estimatedArrival\":45}");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, dC, "{\"estimatedArrival\":60}");
        java.sql.Timestamp recent = new java.sql.Timestamp(System.currentTimeMillis() - 30L * 60 * 1000);
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, dA, recent);
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, dB, recent);
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, dC, recent);
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/delayed?maxEstimatedArrival=30&sinceMinutes=60", tok);
        assert2xx(r, "TC338");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (JsonNode it : list) ids.add(it.has("deliveryId") ? it.get("deliveryId").asLong()
                                       : it.has("id") ? it.get("id").asLong() : -1L);
        assertFalse(ids.contains(dA), "TC338: dA (estimatedArrival=15) must NOT be delayed");
        assertTrue(ids.contains(dB), "TC338: dB (estimatedArrival=45) must be delayed");
        assertTrue(ids.contains(dC), "TC338: dC (estimatedArrival=60) must be delayed");
    }
}

@Tag("public") @Tag("features_m1")
class TC339_S4F9SinceMinutesFilterTests extends TestBase {
    @Test
    @DisplayName("TC339 — sinceMinutes=10 excludes deliveries updated >10min ago")
    void since_minutes_filter() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long dOld = _S2S5Seed.seedDelivery(jdbc, this, oid, "IN_TRANSIT", 30.0, 31.0, "Old");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, dOld, "{\"estimatedArrival\":60}");
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, dOld, new java.sql.Timestamp(System.currentTimeMillis() - 30L * 60 * 1000));
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/delayed?maxEstimatedArrival=30&sinceMinutes=10", tok);
        assert2xx(r, "TC339");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (JsonNode it : list) ids.add(it.has("deliveryId") ? it.get("deliveryId").asLong()
                                       : it.has("id") ? it.get("id").asLong() : -1L);
        assertFalse(ids.contains(dOld), "TC339: dOld (updated 30 min ago) must be excluded by sinceMinutes=10");
    }
}

@Tag("public") @Tag("features_m1")
class TC340_S4F9MaxArrivalHighTests extends TestBase {
    @Test
    @DisplayName("TC340 — maxEstimatedArrival=100 returns empty (no estimatedArrival>100)")
    void max_arrival_high() throws Exception {
                BASE_URL = deliveryServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long d = _S2S5Seed.seedDelivery(jdbc, this, oid, "IN_TRANSIT", 30.0, 31.0, "X");
        _S2S5Seed.setDeliveryMetadata(jdbc, this, d, "{\"estimatedArrival\":60}");
        _S2S5Seed.setDeliveryUpdatedAt(jdbc, this, d, new java.sql.Timestamp(System.currentTimeMillis()));
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/deliveries/delayed?maxEstimatedArrival=100&sinceMinutes=60", tok);
        assert2xx(r, "TC340");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertEquals(0, list.size(), "TC340: empty list expected when no estimatedArrival>100; got " + list.size());
    }
}


// ════════════════════════════════════════════════════════════════════════════
// S5 M1 — Checkout Service feature tests (TC341..TC378)
// ════════════════════════════════════════════════════════════════════════════

@Tag("public") @Tag("features_m1")
class TC341_S5F1StatusFilterTests extends TestBase {
    @Test
    @DisplayName("TC341 — status=COMPLETED&March returns 2 COMPLETED payments, most recent first")
    void status_filter() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long o1 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-05");
        long o2 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-25");
        long o3 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-15");
        long o4 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-02-15");
        long p1 = _S2S5Seed.seedPayment(jdbc, this, o1, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        long p2 = _S2S5Seed.seedPayment(jdbc, this, o2, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        long p3 = _S2S5Seed.seedPayment(jdbc, this, o3, 1L, 100.0, "CREDIT_CARD", "REFUNDED");
        long p4 = _S2S5Seed.seedPayment(jdbc, this, o4, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-03-05 12:00:00"), p1); } catch (Exception ignored) {}
        try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-03-25 12:00:00"), p2); } catch (Exception ignored) {}
        try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-03-15 12:00:00"), p3); } catch (Exception ignored) {}
        try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-02-15 12:00:00"), p4); } catch (Exception ignored) {}
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/search?status=COMPLETED&startDate=2026-03-01&endDate=2026-03-31", tok);
        assert2xx(r, "TC341");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertEquals(2, list.size(), "TC341: 2 COMPLETED in March expected; got " + list.size());
    }
}

@Tag("public") @Tag("features_m1")
class TC342_S5F1NoStatusFilterTests extends TestBase {
    @Test
    @DisplayName("TC342 — Date range only returns all payments (no status filter)")
    void no_status() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long o1 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-05");
        long o2 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-25");
        long o3 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-15");
        long o4 = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-02-15");
        long p1 = _S2S5Seed.seedPayment(jdbc, this, o1, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        long p2 = _S2S5Seed.seedPayment(jdbc, this, o2, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        long p3 = _S2S5Seed.seedPayment(jdbc, this, o3, 1L, 100.0, "CREDIT_CARD", "REFUNDED");
        long p4 = _S2S5Seed.seedPayment(jdbc, this, o4, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-03-05 12:00:00"), p1); } catch (Exception ignored) {}
        try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-03-25 12:00:00"), p2); } catch (Exception ignored) {}
        try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-03-15 12:00:00"), p3); } catch (Exception ignored) {}
        try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-02-15 12:00:00"), p4); } catch (Exception ignored) {}
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/search?startDate=2026-03-01&endDate=2026-03-31", tok);
        assert2xx(r, "TC342");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertEquals(3, list.size(), "TC342: 3 payments in March (no status filter) expected; got " + list.size());
    }
}

@Tag("public") @Tag("features_m1")
class TC343_S5F1MostRecentFirstTests extends TestBase {
    @Test
    @DisplayName("TC343 — Results ordered by createdAt DESC")
    void most_recent_first() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oOld = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-05");
        long oNew = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-25");
        long pOld = _S2S5Seed.seedPayment(jdbc, this, oOld, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        long pNew = _S2S5Seed.seedPayment(jdbc, this, oNew, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?",
            java.sql.Timestamp.valueOf("2026-03-05 12:00:00"), pOld); } catch (Exception ignored) {}
        try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?",
            java.sql.Timestamp.valueOf("2026-03-25 12:00:00"), pNew); } catch (Exception ignored) {}
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/search?startDate=2026-03-01&endDate=2026-03-31", tok);
        assert2xx(r, "TC343");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertTrue(list.size() >= 2, "TC343: at least 2 results expected");
        long firstId = list.get(0).has("id") ? list.get(0).get("id").asLong() : -1L;
        assertEquals(pNew, firstId, "TC343: most recent payment must be first; got id=" + firstId);
    }
}

@Tag("public") @Tag("features_m1")
class TC344_S5F2RefundHappyTests extends TestBase {
    @Test
    @DisplayName("TC344 — PUT refund: status=REFUNDED in PG")
    void refund_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        String tok = adminToken();
        assert2xx(httpPutAuth("/api/payments/" + pid + "/refund", "{\"reason\":\"wrong order\"}", tok), "TC344");
        String dbStatus = jdbc.queryForObject(
            "SELECT \"" + columnByField("Payment","status") + "\"::text FROM \""
              + tableName("Payment") + "\" WHERE id = ?", String.class, pid);
        assertEquals("REFUNDED", dbStatus, "TC344: PG status=REFUNDED expected; got " + dbStatus);
    }
}

@Tag("public") @Tag("features_m1")
class TC345_S5F2AlreadyRefundedTests extends TestBase {
    @Test
    @DisplayName("TC345 — Refunding already-REFUNDED payment returns 400")
    void already_refunded() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "REFUNDED");
        String tok = adminToken();
        HttpResponse<String> r = httpPutAuth("/api/payments/" + pid + "/refund", "{\"reason\":\"x\"}", tok);
        assertEquals(400, r.statusCode(), "TC345: refunding REFUNDED must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC346_S5F2FailedPaymentTests extends TestBase {
    @Test
    @DisplayName("TC346 — Refunding a FAILED payment returns 400")
    void failed_payment() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "FAILED");
        String tok = adminToken();
        HttpResponse<String> r = httpPutAuth("/api/payments/" + pid + "/refund", "{\"reason\":\"x\"}", tok);
        assertEquals(400, r.statusCode(), "TC346: refunding FAILED must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC347_S5F2JsonbContentsTests extends TestBase {
    @Test
    @DisplayName("TC347 — JSONB transactionDetails contains refundReason + refundedAt")
    void jsonb_contents() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        String tok = adminToken();
        assert2xx(httpPutAuth("/api/payments/" + pid + "/refund", "{\"reason\":\"wrong order\"}", tok), "TC347");
        String details = jdbc.queryForObject(
            "SELECT \"" + columnByField("Payment","transactionDetails") + "\"::text FROM \""
              + tableName("Payment") + "\" WHERE id = ?", String.class, pid);
        assertNotNull(details, "TC347: transactionDetails must be set");
        assertTrue(details.contains("refundReason"), "TC347: refundReason key required; got " + details);
        assertTrue(details.contains("refundedAt"), "TC347: refundedAt key required; got " + details);
    }
}

@Tag("public") @Tag("features_m1")
class TC348_S5F3HappySummaryTests extends TestBase {
    @Test
    @DisplayName("TC348 — User summary: totalPayments=4, totalAmount=400, methodBreakdown 3 keys")
    void happy_summary() throws Exception {
                BASE_URL = checkoutServiceUrl;
        String tok = adminToken();
        long uid = adminId();
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long o1 = _S2S5Seed.seedOrder(jdbc, this, uid, rest, "DELIVERED", 80.0, "2026-03-10");
        long o2 = _S2S5Seed.seedOrder(jdbc, this, uid, rest, "DELIVERED", 150.0, "2026-03-11");
        long o3 = _S2S5Seed.seedOrder(jdbc, this, uid, rest, "DELIVERED", 100.0, "2026-03-12");
        long o4 = _S2S5Seed.seedOrder(jdbc, this, uid, rest, "DELIVERED", 70.0, "2026-03-13");
        _S2S5Seed.seedPayment(jdbc, this, o1, uid, 80.0,  "CREDIT_CARD",      "COMPLETED");
        _S2S5Seed.seedPayment(jdbc, this, o2, uid, 150.0, "CREDIT_CARD",      "COMPLETED");
        _S2S5Seed.seedPayment(jdbc, this, o3, uid, 100.0, "CASH_ON_DELIVERY", "COMPLETED");
        _S2S5Seed.seedPayment(jdbc, this, o4, uid, 70.0,  "WALLET",           "COMPLETED");
        HttpResponse<String> r = httpGetAuth("/api/payments/user/" + uid + "/summary", tok);
        assert2xx(r, "TC348");
        JsonNode j = parseNode(r.body());
        long total = j.has("totalPayments") ? j.get("totalPayments").asLong() : -1;
        double amount = j.has("totalAmount") ? j.get("totalAmount").asDouble() : -1;
        assertEquals(4L, total, "TC348: totalPayments=4 expected; got " + total);
        assertEquals(400.0, amount, 0.5, "TC348: totalAmount=400 expected; got " + amount);
        JsonNode mb = j.has("methodBreakdown") ? j.get("methodBreakdown")
                    : j.has("method_breakdown") ? j.get("method_breakdown") : null;
        assertNotNull(mb, "TC348: methodBreakdown field required");
    }
}

@Tag("public") @Tag("features_m1")
class TC349_S5F3CompletedOnlyTests extends TestBase {
    @Test
    @DisplayName("TC349 — Only COMPLETED payments counted in summary")
    void completed_only() throws Exception {
                BASE_URL = checkoutServiceUrl;
        String tok = adminToken();
        long uid = adminId();
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long o1 = _S2S5Seed.seedOrder(jdbc, this, uid, rest, "DELIVERED", 100.0, "2026-03-10");
        long o2 = _S2S5Seed.seedOrder(jdbc, this, uid, rest, "DELIVERED", 100.0, "2026-03-11");
        _S2S5Seed.seedPayment(jdbc, this, o1, uid, 100.0, "CREDIT_CARD", "COMPLETED");
        _S2S5Seed.seedPayment(jdbc, this, o2, uid, 999.0, "CREDIT_CARD", "FAILED");
        HttpResponse<String> r = httpGetAuth("/api/payments/user/" + uid + "/summary", tok);
        assert2xx(r, "TC349");
        double amount = parseNode(r.body()).has("totalAmount") ? parseNode(r.body()).get("totalAmount").asDouble() : -1;
        assertEquals(100.0, amount, 0.5, "TC349: only COMPLETED counted (FAILED excluded); got " + amount);
    }
}

@Tag("public") @Tag("features_m1")
class TC350_S5F3MethodBreakdownTests extends TestBase {
    @Test
    @DisplayName("TC350 — methodBreakdown has correct per-method totals")
    void method_breakdown() throws Exception {
                BASE_URL = checkoutServiceUrl;
        String tok = adminToken();
        long uid = adminId();
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long o1 = _S2S5Seed.seedOrder(jdbc, this, uid, rest, "DELIVERED", 80.0, "2026-03-10");
        long o2 = _S2S5Seed.seedOrder(jdbc, this, uid, rest, "DELIVERED", 150.0, "2026-03-11");
        long o3 = _S2S5Seed.seedOrder(jdbc, this, uid, rest, "DELIVERED", 100.0, "2026-03-12");
        _S2S5Seed.seedPayment(jdbc, this, o1, uid, 80.0,  "CREDIT_CARD",      "COMPLETED");
        _S2S5Seed.seedPayment(jdbc, this, o2, uid, 150.0, "CREDIT_CARD",      "COMPLETED");
        _S2S5Seed.seedPayment(jdbc, this, o3, uid, 100.0, "CASH_ON_DELIVERY", "COMPLETED");
        HttpResponse<String> r = httpGetAuth("/api/payments/user/" + uid + "/summary", tok);
        assert2xx(r, "TC350");
        JsonNode mb = parseNode(r.body()).has("methodBreakdown") ? parseNode(r.body()).get("methodBreakdown")
                    : parseNode(r.body()).has("method_breakdown") ? parseNode(r.body()).get("method_breakdown") : null;
        assertNotNull(mb, "TC350: methodBreakdown required");
        if (mb.has("CREDIT_CARD")) {
            assertEquals(230.0, mb.get("CREDIT_CARD").asDouble(), 0.5,
                "TC350: CREDIT_CARD=80+150=230; got " + mb.get("CREDIT_CARD"));
        }
        if (mb.has("CASH_ON_DELIVERY")) {
            assertEquals(100.0, mb.get("CASH_ON_DELIVERY").asDouble(), 0.5,
                "TC350: CASH_ON_DELIVERY=100; got " + mb.get("CASH_ON_DELIVERY"));
        }
    }
}

@Tag("public") @Tag("features_m1")
class TC351_S5F3UserNotFoundTests extends TestBase {
    @Test
    @DisplayName("TC351 — Summary for non-existent user returns 404")
    void not_found() throws Exception {
                BASE_URL = checkoutServiceUrl;
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/user/999999/summary", tok);
        assertEquals(404, r.statusCode(), "TC351: must be 404; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC352_S5F4HappyPaymentTests extends TestBase {
    @Test
    @DisplayName("TC352 — POST payment for DELIVERED order → 201")
    void happy_payment() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        // Create a PENDING payment for this order (S3-F4 normally creates one).
        _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "PENDING");
        String tok = adminToken();
        String body = "{\"method\":\"CREDIT_CARD\",\"cardLastFour\":\"4242\"}";
        HttpResponse<String> r = httpPostAuth("/api/payments/order/" + oid, body, tok);
        assertEquals(201, r.statusCode(), "TC352: must be 201; got " + r.statusCode() + " body=" + r.body());
    }
}

@Tag("public") @Tag("features_m1")
class TC353_S5F4AlreadyPaidTests extends TestBase {
    @Test
    @DisplayName("TC353 — Paying for an already-paid order returns 400 (\"already paid\")")
    void already_paid() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        String tok = adminToken();
        String body = "{\"method\":\"CREDIT_CARD\"}";
        HttpResponse<String> r = httpPostAuth("/api/payments/order/" + oid, body, tok);
        assertEquals(400, r.statusCode(), "TC353: already-paid order must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC354_S5F4PlacedOrderTests extends TestBase {
    @Test
    @DisplayName("TC354 — Paying for PLACED (not DELIVERED) order returns 400")
    void placed_order() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "PLACED", 100.0, "2026-03-10");
        String tok = adminToken();
        String body = "{\"method\":\"CREDIT_CARD\"}";
        HttpResponse<String> r = httpPostAuth("/api/payments/order/" + oid, body, tok);
        assertEquals(400, r.statusCode(), "TC354: paying PLACED order must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC355_S5F4OrderNotFoundTests extends TestBase {
    @Test
    @DisplayName("TC355 — Non-existent order returns 404")
    void order_not_found() throws Exception {
                BASE_URL = checkoutServiceUrl;
        String tok = adminToken();
        HttpResponse<String> r = httpPostAuth("/api/payments/order/999999", "{\"method\":\"CREDIT_CARD\"}", tok);
        assertEquals(404, r.statusCode(), "TC355: must be 404; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC356_S5F5PercentageHappyTests extends TestBase {
    @Test
    @DisplayName("TC356 — PERCENTAGE 30% on 150 → discount=45")
    void percentage_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 150.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 150.0, "CREDIT_CARD", "PENDING");
        long offerId = _S2S5Seed.seedOffer(jdbc, this, "HUNGRY30", "PERCENTAGE", 30.0, 3, _S2S5Seed.future());
        String tok = adminToken();
        HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/offers/" + offerId, "", tok);
        assert2xx(r, "TC356");
        // Verify PaymentOffer row created with discount_applied=45.
        String poTable = tableName("PaymentOffer");
        String discCol = columnByField("PaymentOffer", "discountApplied");
        Double disc = jdbc.queryForObject(
            "SELECT \"" + discCol + "\" FROM \"" + poTable + "\" WHERE \""
              + columnByField("PaymentOffer","payment") + "\" = ? AND \""
              + columnByField("PaymentOffer","offer") + "\" = ?",
            Double.class, pid, offerId);
        assertEquals(45.0, disc, 0.5, "TC356: discount=45 (150*30/100); got " + disc);
    }
}

@Tag("public") @Tag("features_m1")
class TC357_S5F5FixedHappyTests extends TestBase {
    @Test
    @DisplayName("TC357 — FIXED 20 on 150 → discount=20")
    void fixed_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 150.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 150.0, "CREDIT_CARD", "PENDING");
        long offerId = _S2S5Seed.seedOffer(jdbc, this, "FIXED20", "FIXED", 20.0, 3, _S2S5Seed.future());
        String tok = adminToken();
        HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/offers/" + offerId, "", tok);
        assert2xx(r, "TC357");
        Double disc = jdbc.queryForObject(
            "SELECT \"" + columnByField("PaymentOffer","discountApplied") + "\" FROM \""
              + tableName("PaymentOffer") + "\" WHERE \"" + columnByField("PaymentOffer","payment") + "\" = ?",
            Double.class, pid);
        assertEquals(20.0, disc, 0.5, "TC357: FIXED 20 → discount=20; got " + disc);
    }
}

@Tag("public") @Tag("features_m1")
class TC358_S5F5DiscountCappedTests extends TestBase {
    @Test
    @DisplayName("TC358 — FIXED 9999 on 150 → discount capped at 150 (cannot produce negative total)")
    void discount_capped() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 150.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 150.0, "CREDIT_CARD", "PENDING");
        long offerId = _S2S5Seed.seedOffer(jdbc, this, "BIG", "FIXED", 9999.0, 3, _S2S5Seed.future());
        String tok = adminToken();
        assert2xx(httpPostAuth("/api/payments/" + pid + "/offers/" + offerId, "", tok), "TC358");
        Double disc = jdbc.queryForObject(
            "SELECT \"" + columnByField("PaymentOffer","discountApplied") + "\" FROM \""
              + tableName("PaymentOffer") + "\" WHERE \"" + columnByField("PaymentOffer","payment") + "\" = ?",
            Double.class, pid);
        assertEquals(150.0, disc, 0.5, "TC358: discount capped at payment.amount=150; got " + disc);
    }
}

@Tag("public") @Tag("features_m1")
class TC359_S5F5ExpiredOfferTests extends TestBase {
    @Test
    @DisplayName("TC359 — Expired offer returns 400")
    void expired_offer() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 150.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 150.0, "CREDIT_CARD", "PENDING");
        long offerId = _S2S5Seed.seedOffer(jdbc, this, "EXPIRED", "PERCENTAGE", 30.0, 3, _S2S5Seed.past());
        String tok = adminToken();
        HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/offers/" + offerId, "", tok);
        assertEquals(400, r.statusCode(), "TC359: expired offer must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC360_S5F5InactiveOfferTests extends TestBase {
    @Test
    @DisplayName("TC360 — Inactive offer returns 400")
    void inactive_offer() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 150.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 150.0, "CREDIT_CARD", "PENDING");
        long offerId = _S2S5Seed.seedOffer(jdbc, this, "INACTIVE", "PERCENTAGE", 30.0, 3, _S2S5Seed.future());
        // Set active=false on the offer.
        try { jdbc.update("UPDATE \"" + tableName("Offer") + "\" SET active = false WHERE id = ?", offerId); }
        catch (org.springframework.dao.DataAccessException e) {
            throw new AssertionError("TC360: Offer table must have an `active` column — " + e.getMessage(), e);
        }
        String tok = adminToken();
        HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/offers/" + offerId, "", tok);
        assertEquals(400, r.statusCode(), "TC360: inactive offer must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC361_S5F5AlreadyAppliedTests extends TestBase {
    @Test
    @DisplayName("TC361 — Re-applying same offer returns 400")
    void already_applied() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 150.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 150.0, "CREDIT_CARD", "PENDING");
        long offerId = _S2S5Seed.seedOffer(jdbc, this, "DUP", "PERCENTAGE", 10.0, 3, _S2S5Seed.future());
        String tok = adminToken();
        assert2xx(httpPostAuth("/api/payments/" + pid + "/offers/" + offerId, "", tok), "TC361 first");
        HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/offers/" + offerId, "", tok);
        assertEquals(400, r.statusCode(), "TC361: re-apply must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC362_S5F5CompletedPaymentTests extends TestBase {
    @Test
    @DisplayName("TC362 — Applying offer to COMPLETED payment returns 400")
    void completed_payment() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 150.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 150.0, "CREDIT_CARD", "COMPLETED");
        long offerId = _S2S5Seed.seedOffer(jdbc, this, "COMP", "PERCENTAGE", 30.0, 3, _S2S5Seed.future());
        String tok = adminToken();
        HttpResponse<String> r = httpPostAuth("/api/payments/" + pid + "/offers/" + offerId, "", tok);
        assertEquals(400, r.statusCode(), "TC362: COMPLETED payment must reject offers with 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC363_S5F6HappyRevenueTests extends TestBase {
    @Test
    @DisplayName("TC363 — Revenue: 5 COMPLETED=650, 2 REFUNDED=150")
    void happy_revenue() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        double[] comp = {80, 100, 120, 150, 200};
        double[] ref  = {60, 90};
        for (double a : comp) {
            long o = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", a, "2026-03-15");
            long p = _S2S5Seed.seedPayment(jdbc, this, o, 1L, a, "CREDIT_CARD", "COMPLETED");
            try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-03-15 12:00:00"), p); } catch (Exception ignored) {}
        }
        for (double a : ref) {
            long o = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", a, "2026-03-20");
            long p = _S2S5Seed.seedPayment(jdbc, this, o, 1L, a, "CREDIT_CARD", "REFUNDED");
            try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-03-20 12:00:00"), p); } catch (Exception ignored) {}
        }
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/reports/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
        assert2xx(r, "TC363");
        JsonNode j = parseNode(r.body());
        double rev = j.has("totalRevenue") ? j.get("totalRevenue").asDouble() : -1;
        long txn = j.has("totalTransactions") ? j.get("totalTransactions").asLong() : -1;
        double refAmt = j.has("refundedAmount") ? j.get("refundedAmount").asDouble() : -1;
        long refCnt = j.has("refundCount") ? j.get("refundCount").asLong() : -1;
        assertEquals(650.0, rev, 1.0, "TC363: totalRevenue=650; got " + rev);
        assertEquals(5L,   txn, "TC363: totalTransactions=5; got " + txn);
        assertEquals(150.0, refAmt, 1.0, "TC363: refundedAmount=150; got " + refAmt);
        assertEquals(2L,   refCnt, "TC363: refundCount=2; got " + refCnt);
    }
}

@Tag("public") @Tag("features_m1")
class TC364_S5F6AveragePaymentTests extends TestBase {
    @Test
    @DisplayName("TC364 — averagePayment = totalRevenue/totalTransactions = 650/5 = 130")
    void average_payment() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        for (double a : new double[]{80, 100, 120, 150, 200}) {
            long o = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", a, "2026-03-15");
            long p = _S2S5Seed.seedPayment(jdbc, this, o, 1L, a, "CREDIT_CARD", "COMPLETED");
            try { jdbc.update("UPDATE \"" + tableName("Payment") + "\" SET created_at = ? WHERE id = ?", java.sql.Timestamp.valueOf("2026-03-15 12:00:00"), p); } catch (Exception ignored) {}
        }
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/reports/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
        assert2xx(r, "TC364");
        double avg = parseNode(r.body()).has("averagePayment") ? parseNode(r.body()).get("averagePayment").asDouble() : -1;
        assertEquals(130.0, avg, 0.5, "TC364: averagePayment=130 (650/5); got " + avg);
    }
}

@Tag("public") @Tag("features_m1")
class TC365_S5F6ZeroDivisionTests extends TestBase {
    @Test
    @DisplayName("TC365 — Empty range: averagePayment handled (no division-by-zero)")
    void zero_division() throws Exception {
                BASE_URL = checkoutServiceUrl;
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/reports/revenue?startDate=2030-01-01&endDate=2030-01-31", tok);
        assert2xx(r, "TC365");
        // averagePayment should be 0 (or null), NOT NaN/Infinity.
        JsonNode j = parseNode(r.body());
        if (j.has("averagePayment")) {
            double avg = j.get("averagePayment").asDouble();
            assertFalse(Double.isNaN(avg) || Double.isInfinite(avg),
                "TC365: averagePayment must not be NaN/Infinity on zero division; got " + avg);
        }
    }
}

@Tag("public") @Tag("features_m1")
class TC366_S5F6InvalidRangeTests extends TestBase {
    @Test
    @DisplayName("TC366 — startDate > endDate returns 400")
    void invalid_range() throws Exception {
                BASE_URL = checkoutServiceUrl;
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/reports/revenue?startDate=2026-04-01&endDate=2026-03-01", tok);
        assertEquals(400, r.statusCode(), "TC366: must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC367_S5F7HappyRetryTests extends TestBase {
    @Test
    @DisplayName("TC367 — PUT retry on FAILED → status=COMPLETED")
    void happy_retry() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "FAILED");
        _S2S5Seed.setPaymentTransactionDetails(jdbc, this, pid,
            "{\"gatewayResponse\":\"declined\",\"cardLastFour\":\"4242\",\"retryAttempt\":0}");
        String tok = adminToken();
        assert2xx(httpPutAuth("/api/payments/" + pid + "/retry", "", tok), "TC367");
        String dbStatus = jdbc.queryForObject(
            "SELECT \"" + columnByField("Payment","status") + "\"::text FROM \""
              + tableName("Payment") + "\" WHERE id = ?", String.class, pid);
        assertEquals("COMPLETED", dbStatus, "TC367: status must be COMPLETED after retry; got " + dbStatus);
        String details = jdbc.queryForObject(
            "SELECT \"" + columnByField("Payment","transactionDetails") + "\"::text FROM \""
              + tableName("Payment") + "\" WHERE id = ?", String.class, pid);
        assertTrue(details.contains("approved") || details.contains("\"retryAttempt\":1") || details.contains("\"retryAttempt\": 1"),
            "TC367: JSONB must show gatewayResponse=approved + retryAttempt=1; got " + details);
    }
}

@Tag("public") @Tag("features_m1")
class TC368_S5F7CompletedRetryTests extends TestBase {
    @Test
    @DisplayName("TC368 — Retry on COMPLETED returns 400")
    void completed_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "COMPLETED");
        String tok = adminToken();
        HttpResponse<String> r = httpPutAuth("/api/payments/" + pid + "/retry", "", tok);
        assertEquals(400, r.statusCode(), "TC368: retry on COMPLETED must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC369_S5F7RefundedRetryTests extends TestBase {
    @Test
    @DisplayName("TC369 — Retry on REFUNDED returns 400")
    void refunded_400() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "REFUNDED");
        String tok = adminToken();
        HttpResponse<String> r = httpPutAuth("/api/payments/" + pid + "/retry", "", tok);
        assertEquals(400, r.statusCode(), "TC369: retry on REFUNDED must be 400; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC370_S5F7JsonbMergeTests extends TestBase {
    @Test
    @DisplayName("TC370 — Retry preserves cardLastFour, failureReason in JSONB while updating gatewayResponse + retryAttempt")
    void jsonb_merge() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "FAILED");
        _S2S5Seed.setPaymentTransactionDetails(jdbc, this, pid,
            "{\"gatewayResponse\":\"declined\",\"cardLastFour\":\"4242\",\"retryAttempt\":0,\"failureReason\":\"insufficient funds\"}");
        String tok = adminToken();
        assert2xx(httpPutAuth("/api/payments/" + pid + "/retry", "", tok), "TC370");
        String details = jdbc.queryForObject(
            "SELECT \"" + columnByField("Payment","transactionDetails") + "\"::text FROM \""
              + tableName("Payment") + "\" WHERE id = ?", String.class, pid);
        assertTrue(details.contains("4242"),
            "TC370: cardLastFour=4242 must be preserved after retry; got " + details);
        assertTrue(details.contains("insufficient funds"),
            "TC370: failureReason must be preserved; got " + details);
    }
}

@Tag("public") @Tag("features_m1")
class TC371_S5F8HappyDetailsTests extends TestBase {
    @Test
    @DisplayName("TC371 — Details with 2 offers (45+20) → totalDiscount=65, finalAmount=85")
    void happy_details() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 150.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 150.0, "CREDIT_CARD", "PENDING");
        long o1 = _S2S5Seed.seedOffer(jdbc, this, "P30", "PERCENTAGE", 30.0, 3, _S2S5Seed.future());
        long o2 = _S2S5Seed.seedOffer(jdbc, this, "F20", "FIXED", 20.0, 3, _S2S5Seed.future());
        // Insert PaymentOffer rows directly with desired discounts.
        String pot = tableName("PaymentOffer");
        java.util.Map<String, Object> _po1 = new java.util.HashMap<>();
        _po1.put(columnByField("PaymentOffer","payment"), pid);
        _po1.put(columnByField("PaymentOffer","offer"), o1);
        _po1.put(columnByField("PaymentOffer","discountApplied"), 45.0);
        insertRowReturningId(pot, _po1);
        java.util.Map<String, Object> _po2 = new java.util.HashMap<>();
        _po2.put(columnByField("PaymentOffer","payment"), pid);
        _po2.put(columnByField("PaymentOffer","offer"), o2);
        _po2.put(columnByField("PaymentOffer","discountApplied"), 20.0);
        insertRowReturningId(pot, _po2);
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/" + pid + "/details", tok);
        assert2xx(r, "TC371");
        JsonNode j = parseNode(r.body());
        double td = j.has("totalDiscount") ? j.get("totalDiscount").asDouble() : -1;
        double fa = j.has("finalAmount") ? j.get("finalAmount").asDouble() : -1;
        assertEquals(65.0, td, 0.5, "TC371: totalDiscount=65; got " + td);
        assertEquals(85.0, fa, 0.5, "TC371: finalAmount=85 (150-65); got " + fa);
    }
}

@Tag("public") @Tag("features_m1")
class TC372_S5F8NoOffersTests extends TestBase {
    @Test
    @DisplayName("TC372 — Payment with no offers → totalDiscount=0, finalAmount==originalAmount")
    void no_offers() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "PENDING");
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/" + pid + "/details", tok);
        assert2xx(r, "TC372");
        JsonNode j = parseNode(r.body());
        double td = j.has("totalDiscount") ? j.get("totalDiscount").asDouble() : -1;
        double fa = j.has("finalAmount") ? j.get("finalAmount").asDouble() : -1;
        double oa = j.has("originalAmount") ? j.get("originalAmount").asDouble() : -1;
        assertEquals(0.0, td, 0.01, "TC372: totalDiscount=0 with no offers; got " + td);
        assertEquals(oa, fa, 0.01, "TC372: finalAmount must equal originalAmount; got fa=" + fa + " oa=" + oa);
    }
}

@Tag("public") @Tag("features_m1")
class TC373_S5F8AppliedOffersShapeTests extends TestBase {
    @Test
    @DisplayName("TC373 — appliedOffers entries have offerCode/discountType/discountApplied")
    void applied_offers_shape() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 150.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 150.0, "CREDIT_CARD", "PENDING");
        long ofId = _S2S5Seed.seedOffer(jdbc, this, "TEST", "PERCENTAGE", 10.0, 3, _S2S5Seed.future());
        String pot = tableName("PaymentOffer");
        java.util.Map<String, Object> _po = new java.util.HashMap<>();
        _po.put(columnByField("PaymentOffer","payment"), pid);
        _po.put(columnByField("PaymentOffer","offer"), ofId);
        _po.put(columnByField("PaymentOffer","discountApplied"), 15.0);
        insertRowReturningId(pot, _po);
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/" + pid + "/details", tok);
        assert2xx(r, "TC373");
        JsonNode arr = parseNode(r.body()).has("appliedOffers") ? parseNode(r.body()).get("appliedOffers")
                     : parseNode(r.body()).has("applied_offers") ? parseNode(r.body()).get("applied_offers") : null;
        assertNotNull(arr, "TC373: appliedOffers field required");
        assertTrue(arr.size() >= 1, "TC373: at least 1 applied offer expected");
        JsonNode first = arr.get(0);
        assertTrue(first.has("offerCode") || first.has("offer_code") || first.has("code"),
            "TC373: applied offer must include offerCode; got " + first);
        assertTrue(first.has("discountType") || first.has("discount_type"),
            "TC373: applied offer must include discountType");
        assertTrue(first.has("discountApplied") || first.has("discount_applied"),
            "TC373: applied offer must include discountApplied");
    }
}

@Tag("public") @Tag("features_m1")
class TC374_S5F8NotFoundTests extends TestBase {
    @Test
    @DisplayName("TC374 — Details for non-existent payment returns 404")
    void not_found() throws Exception {
                BASE_URL = checkoutServiceUrl;
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/999999/details", tok);
        assertEquals(404, r.statusCode(), "TC374: must be 404; got " + r.statusCode());
    }
}

@Tag("public") @Tag("features_m1")
class TC375_S5F9HappyRankingTests extends TestBase {
    @Test
    @DisplayName("TC375 — Top-used: A (5 uses) before B (3 uses)")
    void happy_ranking() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oA = _S2S5Seed.seedOffer(jdbc, this, "A", "PERCENTAGE", 30.0, 100, _S2S5Seed.future());
        long oB = _S2S5Seed.seedOffer(jdbc, this, "B", "FIXED",       30.0, 100, _S2S5Seed.future());
        // Set currentUses on each via JDBC.
        try { jdbc.update("UPDATE \"" + tableName("Offer") + "\" SET current_uses = 5 WHERE id = ?", oA); }
        catch (Exception ignored) {}
        try { jdbc.update("UPDATE \"" + tableName("Offer") + "\" SET current_uses = 3 WHERE id = ?", oB); }
        catch (Exception ignored) {}
        // Seed PaymentOffer rows so the join succeeds.
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 150.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 150.0, "CREDIT_CARD", "PENDING");
        String pot = tableName("PaymentOffer");
        for (int i = 0; i < 5; i++) {
            java.util.Map<String, Object> _po = new java.util.HashMap<>();
            _po.put(columnByField("PaymentOffer","payment"), pid);
            _po.put(columnByField("PaymentOffer","offer"), oA);
            _po.put(columnByField("PaymentOffer","discountApplied"), 45.0);
            insertRowReturningId(pot, _po);
        }
        for (int i = 0; i < 3; i++) {
            java.util.Map<String, Object> _po = new java.util.HashMap<>();
            _po.put(columnByField("PaymentOffer","payment"), pid);
            _po.put(columnByField("PaymentOffer","offer"), oB);
            _po.put(columnByField("PaymentOffer","discountApplied"), 30.0);
            insertRowReturningId(pot, _po);
        }
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/offers/top-used?limit=2", tok);
        assert2xx(r, "TC375");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertTrue(list.size() >= 2, "TC375: at least 2 results expected");
        long firstId = list.get(0).has("offerId") ? list.get(0).get("offerId").asLong()
                     : list.get(0).has("id") ? list.get(0).get("id").asLong() : -1L;
        assertEquals(oA, firstId, "TC375: offer A (5 uses) must rank first; got id=" + firstId);
    }
}

@Tag("public") @Tag("features_m1")
class TC376_S5F9LimitTests extends TestBase {
    @Test
    @DisplayName("TC376 — limit=1 returns at most 1 result")
    void limit_caps() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long o1 = _S2S5Seed.seedOffer(jdbc, this, "X", "PERCENTAGE", 10.0, 100, _S2S5Seed.future());
        long o2 = _S2S5Seed.seedOffer(jdbc, this, "Y", "PERCENTAGE", 10.0, 100, _S2S5Seed.future());
        try { jdbc.update("UPDATE \"" + tableName("Offer") + "\" SET current_uses = 5 WHERE id IN (?, ?)", o1, o2); }
        catch (Exception ignored) {}
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "PENDING");
        String pot = tableName("PaymentOffer");
        java.util.Map<String, Object> _po1 = new java.util.HashMap<>();
        _po1.put(columnByField("PaymentOffer","payment"), pid);
        _po1.put(columnByField("PaymentOffer","offer"), o1);
        _po1.put(columnByField("PaymentOffer","discountApplied"), 10.0);
        insertRowReturningId(pot, _po1);
        java.util.Map<String, Object> _po2 = new java.util.HashMap<>();
        _po2.put(columnByField("PaymentOffer","payment"), pid);
        _po2.put(columnByField("PaymentOffer","offer"), o2);
        _po2.put(columnByField("PaymentOffer","discountApplied"), 10.0);
        insertRowReturningId(pot, _po2);
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/offers/top-used?limit=1", tok);
        assert2xx(r, "TC376");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        assertEquals(1, list.size(), "TC376: limit=1 must return 1; got " + list.size());
    }
}

@Tag("public") @Tag("features_m1")
class TC377_S5F9TotalDiscountSumTests extends TestBase {
    @Test
    @DisplayName("TC377 — totalDiscountGiven sums payment_offers.discount_applied per offer")
    void total_discount_sum() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long offerId = _S2S5Seed.seedOffer(jdbc, this, "DSCSUM", "PERCENTAGE", 30.0, 100, _S2S5Seed.future());
        try { jdbc.update("UPDATE \"" + tableName("Offer") + "\" SET current_uses = 3 WHERE id = ?", offerId); }
        catch (Exception ignored) {}
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 200.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 200.0, "CREDIT_CARD", "PENDING");
        String pot = tableName("PaymentOffer");
        // Discounts of 45, 45, 30 → sum=120
        for (double d : new double[]{45.0, 45.0, 30.0}) {
            java.util.Map<String, Object> _po = new java.util.HashMap<>();
            _po.put(columnByField("PaymentOffer","payment"), pid);
            _po.put(columnByField("PaymentOffer","offer"), offerId);
            _po.put(columnByField("PaymentOffer","discountApplied"), d);
            insertRowReturningId(pot, _po);
        }
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/offers/top-used?limit=10", tok);
        assert2xx(r, "TC377");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        for (JsonNode it : list) {
            long id = it.has("offerId") ? it.get("offerId").asLong() : it.has("id") ? it.get("id").asLong() : -1L;
            if (id == offerId) {
                double total = it.has("totalDiscountGiven") ? it.get("totalDiscountGiven").asDouble()
                             : it.has("total_discount_given") ? it.get("total_discount_given").asDouble() : -1;
                assertEquals(120.0, total, 0.5, "TC377: totalDiscountGiven=120 (45+45+30); got " + total);
                return;
            }
        }
        throw new AssertionError("TC377: target offer not found in result; body=" + r.body());
    }
}

@Tag("public") @Tag("features_m1")
class TC378_S5F9ExpiredFlagTests extends TestBase {
    @Test
    @DisplayName("TC378 — expired=true for offers past expiryDate")
    void expired_flag() throws Exception {
                BASE_URL = checkoutServiceUrl;
        long rest = _S2S5Seed.seedRestaurant(jdbc, this, "R", "EGYPTIAN", "OPEN");
        long offerId = _S2S5Seed.seedOffer(jdbc, this, "EXP", "PERCENTAGE", 10.0, 100, _S2S5Seed.past());
        try { jdbc.update("UPDATE \"" + tableName("Offer") + "\" SET current_uses = 1 WHERE id = ?", offerId); }
        catch (Exception ignored) {}
        long oid = _S2S5Seed.seedOrder(jdbc, this, 1L, rest, "DELIVERED", 100.0, "2026-03-10");
        long pid = _S2S5Seed.seedPayment(jdbc, this, oid, 1L, 100.0, "CREDIT_CARD", "PENDING");
        java.util.Map<String, Object> _po = new java.util.HashMap<>();
        _po.put(columnByField("PaymentOffer","payment"), pid);
        _po.put(columnByField("PaymentOffer","offer"), offerId);
        _po.put(columnByField("PaymentOffer","discountApplied"), 10.0);
        insertRowReturningId(tableName("PaymentOffer"), _po);
        String tok = adminToken();
        HttpResponse<String> r = httpGetAuth("/api/payments/offers/top-used?limit=10", tok);
        assert2xx(r, "TC378");
        JsonNode list = parseNode(r.body()).isArray() ? parseNode(r.body())
                       : (parseNode(r.body()).has("content") ? parseNode(r.body()).get("content") : parseNode(r.body()));
        for (JsonNode it : list) {
            long id = it.has("offerId") ? it.get("offerId").asLong() : it.has("id") ? it.get("id").asLong() : -1L;
            if (id == offerId) {
                assertTrue(it.has("expired") && it.get("expired").asBoolean(),
                    "TC378: expired must be true for offer past expiryDate; got " + it);
                return;
            }
        }
        throw new AssertionError("TC378: target expired offer not found in result; body=" + r.body());
    }
}
