# Known limitations

A frank list of what's deferred or wouldn't hold up in production.

- No tests — zero backend, zero Android unit, zero UI.
- No ProGuard/R8 rules. A release build will likely crash on Compose/Hilt/Retrofit reflection.
- Location search is limited to 12 hardcoded localities. OSM Nominatim's public endpoint blocks
  automated traffic, so free-text geocoding would need self-hosting or a paid service.
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

### Deployment realities

- **Cold start on Render's free tier.** After 15 min idle, the container spins down. First request wakes it up in 30-60 s; a tenant opening the app after a quiet stretch sees a long loading spinner before anything renders. Subsequent requests are fast (~200 ms). Mitigation: cron-ping the health endpoint every 10 min. Unimplemented.
- **Supabase auto-pauses after 7 days of inactivity.** Free-tier projects pause; the DB is unreachable until manual unpause from the Supabase dashboard. Same cron-ping mitigation applies. Unimplemented.
- **Session pooler over Direct.** Supabase's Direct connection is IPv6-default; Render is IPv4-only. HikariCP connects through Supabase's Session pooler (PgBouncer). Works transparently but adds a small connection setup cost per new pool member.
- **Legacy JWT `service_role` for Storage.** Supabase Storage API still requires the old JWT-format key, not the newer `sb_secret_*` API keys. This will likely change; the code path is a single env var swap.
- **Test data may exist in production Supabase.** Portfolio-demo signups + test listings live in the production DB unless deliberately cleaned. Not sensitive, but visible if someone browses the app.
- **No CI.** Push to main triggers Render's auto-deploy. There's no test/lint check before build. A commit that compiles but breaks behavior will deploy.
- **No crash reporting.** Android production crashes go unnoticed. Firebase Crashlytics would be a natural add.
- **Debug keystore only.** APK is signed with the default debug keystore. A real Play Store release needs a production keystore + Play Store signing config.

### Auth & profile

- **No Google sign-in.** Requires Firebase Google OAuth setup, SHA-1 keystore
  fingerprint registration, and federated sign-in UI. Deferred.
- **No OTP verification.** Firebase doesn't natively support email OTP (it uses
  email links instead). Phone OTP requires enabling Firebase Phone Authentication,
  SHA-1 fingerprints, and SMS-capable testing devices. ~1-2 days of work plus
  rate-limited free tier. Deferred.
- **Login is email + password only.** No "login with phone" — depends on phone OTP.
- **No profile editing after signup.** Name, phone, gender, and DOB are captured
  at signup but can't be edited later.
- **Phone numbers stored as-entered.** Signup accepts both `+919876543210` and
  `9876543210` and stores whichever format the user typed. A user could in
  principle sign up twice with the same physical number in different formats.
  Fix: normalize at the DB boundary before insert.

### Communications

- **Password reset emails land in spam.** Firebase's email template can't be
  customized on this project's plan, and new Firebase projects have low sender
  reputation by default. The forgot-password screen mentions checking the spam
  folder.
- **No transactional email beyond password reset.** No welcome email, no email
  verification (Firebase supports it but isn't wired up), no inquiry-confirmation
  emails.
