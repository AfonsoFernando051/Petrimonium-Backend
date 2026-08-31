-- Dev/test-only seed data — same `db/migration-dev` location as
-- V2__seed_default_data.sql, so this never runs against a production
-- database (see that file's header comment for the full rationale).
--
-- Third demo account, admin3/admin3: a plain USER-role account (unlike
-- admin/admin2, both ADMIN) that behaves like any real user and — unlike
-- admin2 — is never wiped on login (see DemoAccountResetAdapter's
-- DEMO_USERNAMES; admin3 is deliberately not in that set). Useful for
-- testing flows that should persist across sessions without the noise of
-- resetting admin/admin's data or fighting admin2's on-every-login wipe.
-- Already onboarded (profile + pet) so it's immediately usable, same as
-- admin/admin.
-- Password hash is `admin3` through the same BCryptPasswordEncoder the app
-- uses at runtime (BCryptPasswordEncoderAdapter, default strength 10).

insert into jf_users (username, email, password, is_active, has_answered_onboarding, investor_profile, preferred_language, role)
values ('admin3', 'admin3@petinvest.local', '$2a$10$oNUNXZJ7FD3CTA1Vyifl6.A/vQPr6meF1EUs/1uA2oHJDEgecSdtK', true, true, 'GUARDIAN', 'pt', 'USER');

insert into jf_pets (name, health, specie, user_id)
select 'Fox', 100, 'FOX', user_id from jf_users where email = 'admin3@petinvest.local';
