# Known limitations

A frank list of what's deferred or wouldn't hold up in production.

- Not deployed. The whole thing runs on a laptop. `BASE_URL` is `http://10.0.2.2:8080/` and photos
  live in `~/Desktop/nestblr-backend/uploads/`.
- No tests — zero backend, zero Android unit, zero UI.
- No ProGuard/R8 rules. A release build will likely crash on Compose/Hilt/Retrofit reflection.
- Location search is limited to 12 hardcoded localities. OSM Nominatim's public endpoint blocks
  automated traffic, so free-text geocoding would need self-hosting or a paid service.
- Photo storage is on the local filesystem — no S3/R2/CDN. Photos disappear if the backend's working
  directory changes or the container loses its mount.
- No real thumbnails. `thumbnail_url = url`, so Android loads full-size images even for list cards.
- Reviewer names default to "Anonymous" — there's no profile-name collection.
- Soft-deleted listings leave orphaned `favorites` rows. The schema has `ON DELETE CASCADE`, but the
  app soft-deletes rather than hard-deletes, so the cascade never fires.
- Single-machine concurrency. The review-aggregate recompute is a read-then-write under
  `REPEATABLE_READ`; write-skew is theoretically possible at scale, though it self-heals on the next
  write.
- Phone is the only owner contact — there's no in-app messaging.
- No reorder for non-cover photos. Set-as-cover swaps `display_order`, but there's no full
  drag-to-reorder.
