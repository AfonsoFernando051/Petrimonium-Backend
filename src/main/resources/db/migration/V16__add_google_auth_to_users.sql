-- Adds Google Sign-In support to jf_users. `password` is already nullable
-- (see V1__init_schema.sql), so Google-only accounts simply never set it.
-- `provider_id` stores Google's `sub` claim and is null for local accounts;
-- the unique constraint only bites on real values (standard SQL NULL != NULL).

alter table jf_users add column provider varchar(255) not null default 'LOCAL' check (provider in ('LOCAL', 'GOOGLE'));
alter table jf_users add column provider_id varchar(255);
alter table jf_users add constraint uq_jf_users_provider_id unique (provider_id);
