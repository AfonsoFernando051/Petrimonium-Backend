-- PetSpecieEnum (core/domain/enums/PetSpecieEnum.java) has 7 values including OWL, and the
-- Academy/Wallet onboarding species picker offers all 7 — but V1's check constraint on
-- jf_pets.specie only ever allowed the original 6, so picking OWL fails at the database with
-- a check-constraint violation (Demanda #65).
--
-- The constraint was created inline and unnamed, so its real name is engine-generated and
-- differs between H2 (dev) and PostgreSQL (prod) — not safe to DROP CONSTRAINT by a guessed
-- name. Swapping the column instead (add the correctly-constrained column, copy the data,
-- drop the old column, rename) needs no constraint name and is portable across both engines
-- (verified against the project's own H2 2.4.240). Unqualified, same reasoning as
-- V21__add_app_context_to_refresh_tokens.sql: resolved via search_path in both environments.

alter table jf_pets add column specie_new varchar(255)
    check (specie_new in ('DOG', 'CAT', 'WOLF', 'FOX', 'BEAR', 'LION', 'OWL'));

update jf_pets set specie_new = specie;

alter table jf_pets drop column specie;

alter table jf_pets rename column specie_new to specie;
