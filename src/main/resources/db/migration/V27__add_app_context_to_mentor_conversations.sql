-- Stage 6 (Pet/XP/Mentor context separation): scopes each Mentor conversation to the app_context
-- it was created under, so a Wallet session's conversation list/history can never surface an
-- Academy thread (built from simulated-portfolio + lesson content) and vice versa.
--
-- Nullable and unqualified, same reasoning as V21__add_app_context_to_refresh_tokens.sql: this
-- table lives in the `ai` schema in Postgres and unqualified in H2, both resolved via
-- search_path. Existing rows get NULL — a conversation created before this claim existed was
-- built from the old, unscoped prompt (which could mix real portfolio and Academy content in the
-- same thread) and is not retroactively assignable to either context, so it simply stops being
-- reachable through the now context-filtered list/get/rename/delete endpoints rather than being
-- guessed into one side or the other.

alter table jf_mentor_conversations add column app_context varchar(20);
