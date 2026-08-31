-- Schema-per-bounded-context split (docs/BACKEND_MODULE_PLAN.md §5-6).
--
-- Every ALTER TABLE ... SET SCHEMA below is a metadata-only catalog
-- operation in PostgreSQL: no row is rewritten, no data is copied, existing
-- indexes/constraints/sequences move with the table automatically, and FKs
-- that cross schema boundaries (every table here still points at
-- identity.jf_users) remain valid without being redefined. This is why the
-- split is safe to run against a database with real user/portfolio data.
--
-- jf_investments/jf_finances go to real_portfolio, not a split
-- real/simulated pair: there is no simulated-money data anywhere in this
-- schema today (the Financial Lab stores only XP, never a position) — see
-- docs/BACKEND_MODULE_PLAN.md §2.

create schema if not exists identity;
create schema if not exists education;
create schema if not exists real_portfolio;
create schema if not exists gamification;
create schema if not exists pet;
create schema if not exists ai;

-- identity
alter table jf_users set schema identity;
alter table jf_password_reset_tokens set schema identity;
alter table jf_refresh_tokens set schema identity;

-- education
alter table academy_domains set schema education;
alter table academy_domain_translations set schema education;
alter table academy_schools set schema education;
alter table academy_school_translations set schema education;
alter table academy_school_prerequisites set schema education;
alter table learning_modules set schema education;
alter table academy_module_translations set schema education;
alter table academy_module_prerequisites set schema education;
alter table learning_lessons set schema education;
alter table academy_lesson_translations set schema education;
alter table academy_lesson_steps set schema education;
alter table academy_lesson_step_translations set schema education;
alter table academy_choice_question_options set schema education;
alter table academy_choice_question_option_translations set schema education;
alter table academy_lesson_step_takeaways set schema education;
alter table academy_lesson_step_takeaway_translations set schema education;
alter table academy_lesson_portfolio_concepts set schema education;
alter table lesson_progress set schema education;

-- real_portfolio
alter table jf_finances set schema real_portfolio;
alter table jf_investments set schema real_portfolio;

-- gamification
alter table xp_events set schema gamification;
alter table achievement_unlocks set schema gamification;
alter table activity_log set schema gamification;
alter table mission_completions set schema gamification;

-- pet
alter table jf_pets set schema pet;

-- ai
alter table jf_mentor_conversations set schema ai;
alter table jf_mentor_messages set schema ai;
