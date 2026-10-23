# Web endpoints the app is waiting for

Written 2026-10-23 from the app side. Both items are **optional-account** features: anonymous users keep working exactly as today. Everything below is for **students/users only** (JWT with `role=student`); the app never uses admin credentials.

## 1. `POST /api/auth/google-token` — Google sign-in for the Android app  ✅ DONE on the web (app wired)

_Implemented as `{ idToken, acceptedTerms }` → `StudentLoginResponse`; a new Google account without `acceptedTerms` gets `400` (the app then asks for consent and retries with the same token). Audience is `Google:ClientId`, so the Android client must request the token with that same **web** client ID (`GOOGLE_WEB_CLIENT_ID` in `local.properties`)._

**Why:** the web's Google callback sets an HttpOnly cookie and returns no token, so the app can't reuse it. The app will use Android Credential Manager (`GetGoogleIdOption`) to obtain a **Google ID token** and trade it for the same student JWT that `student-login` returns.

| | |
|---|---|
| Auth | `[AllowAnonymous]`, same `auth` rate limiter as the other auth endpoints |
| Request | `{ "idToken": "<Google ID token>", "acceptedTerms": true }` |
| Success `200` | `StudentLoginResponse(Token, DisplayName, Credits)` — identical to `student-login` |
| `400` | missing/invalid token → `{ message }` |
| `401` | token signature/audience/expiry invalid → `{ message }` |
| `403` `{ code: "terms_required" }` | the Google account has no app account yet and `acceptedTerms` is false (app then shows the terms checkbox and retries) |

**Server steps**
1. Validate with `GoogleJsonWebSignature.ValidateAsync(idToken, new() { Audience = [<web client id>, <android client id>] })` (`Google.Apis.Auth`). The audience of an ID token obtained through Credential Manager is the **web (server) client ID** you pass as `setServerClientId` in the app — so the existing web client ID is enough; no Android client ID is needed server-side.
2. Require `email_verified == true`.
3. Find the user by Google subject / email exactly like the cookie callback does (reuse that code path: link, or create with `EmailConfirmedAt = now`).
4. Return `_jwtService.GenerateStudentToken(user)` (so the `sst` security stamp, revocation and `logout-everywhere` keep working).

**What I need from you for the app side:** the **web OAuth client ID** (public value), and to register the app's package name + signing-cert SHA-1 (debug and release) as an *Android* OAuth client in the same Google Cloud project (Credential Manager requires it, even though the server only checks the web client ID as audience).

## 2. Gamification state — `GET/PUT /api/user/game-state`

**Decision (from the user):** when signed in the **server is the source of truth**; when anonymous the device stays authoritative (as now). On first sign-in the app merges (never loses local progress).

The app's gamification is a set of local singletons (points, shop purchases/cooldowns, backpack equips, streak days + freezes, pet state, per-question stats). Rather than six endpoints, I propose **one versioned document** — much less web work and the app can keep its current logic:

```jsonc
GET /api/user/game-state   → 200 { "revision": 12, "updatedAt": "2026-10-23T10:00:00Z", "state": { … } }
                             204 when the user has never stored any state
PUT /api/user/game-state   body { "baseRevision": 12, "state": { … } }
                           → 200 { "revision": 13, "updatedAt": "…" }
                           → 409 { "revision": 14, "state": { … } }   // someone else wrote first; client merges and retries
```

* Stored as a JSON column on the user (or a `UserGameState(UserId PK, Revision, Json, UpdatedAt)` table). Cap the body (e.g. 256 KB) and validate it is JSON.
* `revision` increments on every successful PUT; `baseRevision` must equal the stored one, else 409 (optimistic concurrency).
* Include it in `GET /api/user/export` and delete it with the account.
* The server treats `state` as opaque **except** that I'd like `state.points` (a number) mirrored into a column if you want leaderboards/credits later — optional.

**`state` shape (app-defined, versioned by `schema`)** — all fields optional so the app can evolve:

```jsonc
{
  "schema": 1,
  "points": { "value": 123.5, "totalEarned": 980, "inflationDivisor": 1 },
  "shop": { "purchased": { "<itemId>": "2026-10-01" }, "cooldownUntil": { "<itemId>": 1761300000000 } },
  "backpack": { "quote": "…", "pet": "…", "iconPack": "…", "theme": "…" },
  "streak": { "days": ["2026-10-21", "2026-10-22"], "freezes": 2, "usedFreezes": ["2026-10-20"], "milestones": [7, 30] },
  "pet": { "food": 5, "water": 5, "pets": { "<petId>": { "lastFed": 0, "lastWatered": 0, "growthDays": 3, "dead": false } } },
  "questions": { "<questionId>": { "correct": 3, "wrong": 1, "last": 1761290000000 } }
}
```

**Merge rules the app will apply (client side, on first sign-in and on 409):**

| Field | Rule |
|---|---|
| points.totalEarned, streak.days, shop.purchased, milestones, questions counters | union / max (never lose progress) |
| points.value | `max(local, server)` on first merge; afterwards the server value + local deltas since last sync |
| backpack equips, pet state | take the side with the newer `updatedAt` per section |
| freezes | `min(max(local, server), 3)` |

**Anti-cheat note:** with a client-written blob the user can edit their own points. If points ever gate something with real value (the AI-credit shop already converts points → credits!), award them through an **action endpoint** instead, e.g. `POST /api/user/points/claim { reason, ref }` with server-side rules, and keep only cosmetic state in the blob. Tell me if you want that split now — the shop's "buy AI credits with points" is the part that needs it.

## 3. Already done on the web (thanks) — no action

* `GET /api/user/continue-reading` — wired.
* `POST /api/user/read-progress`, `GET/POST/DELETE /api/user/bookmarks` — wired.

## 4. Nice-to-have

* `GET /api/user/stats` is not shown in the app yet (streak/badges per subject) — would fit the Profile screen.
* An endpoint listing which grade versions of a post exist (`GET /api/posts/{id}/versions`) would let the grade picker mark unavailable grades; today the server just returns the closest one.
