-- Dev/test-only seed data — same `db/migration-dev` location as
-- V2__seed_default_data.sql, so this never runs against a production
-- database (see that file's header comment for the full rationale).
--
-- Second demo account, admin2/admin2, deliberately left in the fresh-signup
-- state: has_answered_onboarding = false and investor_profile = null (the
-- assessment hasn't been taken yet), no jf_pets row (pet setup is part of
-- onboarding) and no jf_investments rows. Useful for exercising the
-- onboarding flow and empty-portfolio UI, which the fully-populated
-- admin/admin account (V2/V3) can't exercise anymore.
-- Password hash is `admin2` through the same BCryptPasswordEncoder the app
-- uses at runtime (BCryptPasswordEncoderAdapter, default strength 10).

insert into jf_users (username, email, password, is_active, has_answered_onboarding, investor_profile, preferred_language, role)
values ('admin2', 'admin2@petinvest.local', '$2a$10$eVrzOFHezyH4sq.uuwna..nmtHL8chTEGZa8BdQRZmQpEjpGLAJVW', true, false, null, 'pt', 'ADMIN');
