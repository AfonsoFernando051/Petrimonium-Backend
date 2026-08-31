-- Closes a real race condition: RegisterUserUseCaseImpl only did
-- check-then-act (findByEmail, then save) with no DB-level guarantee, so two
-- concurrent registrations with the same email could both succeed. This
-- unique constraint is the actual guarantee; the check-then-act in the use
-- case stays as the fast, friendly path for the common case, and now catches
-- the constraint violation on the rare race-lost insert.

alter table jf_users add constraint uq_jf_users_email unique (email);
