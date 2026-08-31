-- Seeds the "Money Fundamentals" / money_fundamentals module (School 1,
-- "Financial Life" — see DECISION-018), authored client-side in
-- petapp_mobile/lib/features/academy/domain/services/catalog/financial_life_catalog.dart
-- but never added to the server's validation catalog. Without this, every
-- `POST /api/v1/learning/lessons/{id}/complete` for a money_fundamentals_*
-- lesson throws "Unknown lesson id" server-side (CompleteLessonUseCaseImpl)
-- and that lesson's XP silently never reaches the authoritative ledger — the
-- client-side sync failure is swallowed as a best-effort offline case.
--
-- Ids and per-lesson XP (20 each) are copied verbatim from the Dart catalog.
-- Module XP reward (100) follows the same "Complete module" convention used
-- for investor_foundations in V4, per docs/FEATURES.md's XP table.
insert into learning_modules (module_id, xp_reward, module_order, lesson_count) values
    ('money_fundamentals', 100, 1, 10);

insert into learning_lessons (lesson_id, module_id, xp_reward, lesson_order) values
    ('money_fundamentals_what_is_money', 'money_fundamentals', 20, 1),
    ('money_fundamentals_income_and_expenses', 'money_fundamentals', 20, 2),
    ('money_fundamentals_needs_vs_wants', 'money_fundamentals', 20, 3),
    ('money_fundamentals_organizing_your_money', 'money_fundamentals', 20, 4),
    ('money_fundamentals_what_is_a_budget', 'money_fundamentals', 20, 5),
    ('money_fundamentals_building_your_first_budget', 'money_fundamentals', 20, 6),
    ('money_fundamentals_conscious_consumption', 'money_fundamentals', 20, 7),
    ('money_fundamentals_financial_goals', 'money_fundamentals', 20, 8),
    ('money_fundamentals_emergency_funds', 'money_fundamentals', 20, 9),
    ('money_fundamentals_review', 'money_fundamentals', 20, 10);
