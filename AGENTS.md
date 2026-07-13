# AniSync Plus Agent Instructions

Scope: entire repository.

1. Read `ProjectContext.md` before making any change.
2. Treat repository code as source of truth. Do not invent paths, classes, fields, GraphQL operations, DOM markers, or product requirements.
3. Ask a concrete question when a product or technical decision is ambiguous and not already answered in `ProjectContext.md`.
4. Keep AniSync upstream compatibility: avoid broad refactors, package moves, full-file reformatting, or changes to unrelated files.
5. Prefer small, themed commits. Document every change to original upstream files.
6. Run relevant tests/checks before committing. If a check cannot run because of the environment, document that limitation.
7. Update the relevant documentation when requirements, implementation plans, integration points, or decisions change.
8. AniWorld calendar data must be fetched only at app runtime on the device. Tests and CI must not make live AniWorld requests; parser tests must use local HTML fixtures.
9. Do not implement Cloudflare/CAPTCHA/authentication/rate-limit bypasses, video/player/streamhoster features, or store credentials/tokens in code, logs, or tests.
10. In AniSync Plus target surfaces, do not fall back visibly or logically to AniList airing dates/episode numbers when AniWorld data is absent.
