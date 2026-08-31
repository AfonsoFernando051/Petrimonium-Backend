-- Dev/test-only seed data — lives in db/migration-dev, a Flyway location
-- included ONLY in the default profile's `spring.flyway.locations` (see
-- application.properties). application-prod.properties deliberately points
-- Flyway at db/migration alone, so this file is never scanned, and this
-- account never gets created, in a production deploy. A predictable
-- admin/admin account is a well-known vulnerability pattern — fine for a
-- disposable local dev database, never acceptable seeded into a real one.
--
-- On the dev H2 in-memory database this re-applies every app restart (fresh
-- DB each run); if ever pointed at a persistent dev/staging database it
-- would instead apply once, the first time Flyway sees a fresh schema.

-- Default admin/admin test account.
-- Login is by EMAIL, not username (see LoginUseCaseImpl), so the email below
-- is what to sign in with; `admin` is just this account's display name.
-- Password hash is `admin` through the same BCryptPasswordEncoder the app
-- uses at runtime (BCryptPasswordEncoderAdapter, default strength 10) —
-- generated once, not derived at migration time, since Flyway SQL migrations
-- can't call into the running application.
-- Already marked as onboarded (with a profile and a pet) so this account is
-- immediately usable for exploring the app, not stuck on onboarding screens.
insert into jf_users (username, email, password, is_active, has_answered_onboarding, investor_profile, preferred_language, role)
values ('admin', 'admin@petinvest.local', '$2a$10$S0OAypGOVUjaaImMxjg3MO0aeRnUvzYwARWjMwbMjc9m9BPgwIOvu', true, true, 'TACTICIAN', 'pt', 'ADMIN');

insert into jf_pets (name, health, specie, user_id)
select 'Rex', 100, 'DOG', user_id from jf_users where email = 'admin@petinvest.local';
