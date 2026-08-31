-- Adds curriculum metadata (learning objective, competency level, difficulty,
-- estimated duration, taxation-only regulatory fields) and backend-persisted
-- Mastery (perfect-first-try) — see docs/DECISIONS.md DECISION-025.
--
-- Additive-only: every column here is nullable or has a safe default, and no
-- existing table, column, or row is touched. AcademyContentSeedRunner's
-- upsert-by-id (learning_lessons/learning_modules) and
-- delete-and-reinsert-children (academy_lesson_translations) reconciliation
-- is unchanged by this — it simply carries a few more columns through the
-- same paths.

-- Per-lesson metadata. competency is nullable because AcademyContentSeedRunner
-- back-fills it for every lesson on the very next boot (see the seeder), but
-- the column itself must tolerate a lesson row existing before that runs.
alter table learning_lessons add column competency varchar(16)
    check (competency in ('RECOGNIZE', 'EXPLAIN', 'CALCULATE', 'INTERPRET', 'COMPARE', 'APPLY', 'DECIDE', 'INTEGRATE'));
alter table learning_lessons add column estimated_minutes integer;

-- Taxation-only regulatory metadata (DECISION-025) — null for every
-- non-Taxation lesson, and left null even for Taxation's own lessons unless
-- someone has actually verified the claim against a current source; the
-- schema must not silently imply a false "verified" state.
alter table learning_lessons add column jurisdiction varchar(8);
alter table learning_lessons add column effective_date date;
alter table learning_lessons add column last_verified_at date;
alter table learning_lessons add column source varchar(255);

-- Per-module difficulty tier.
alter table learning_modules add column difficulty varchar(16)
    check (difficulty in ('FOUNDATION', 'BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'SPECIALIZATION'));

-- "What the learner can DO after this lesson" — title alone was never meant
-- to carry this; per-language like every other lesson-facing text.
alter table academy_lesson_translations add column learning_objective varchar(500);

-- Backend-persisted Mastery signal (DECISION-020 built this client-side
-- only; DECISION-025 now also persists it server-side). Monotonic by
-- construction in application code (CompleteLessonUseCaseImpl), not by a DB
-- constraint: a later perfect replay may set this true, a later missed
-- replay must never reset it to false.
alter table lesson_progress add column perfect_first_try boolean not null default false;
