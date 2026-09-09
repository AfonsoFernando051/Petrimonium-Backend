-- Wallet/Academy/Health now each own their Pet (species locked per app via
-- AppContextEnum.defaultPetSpecie() — WALLET=DOG, ACADEMY=WOLF, HEALTH=FOX),
-- reversing the "one shared Pet per user" design from Stage 6
-- (BACKEND_MODULE_PLAN.md §14). jf_pets.user_id being unique on its own only
-- ever allowed one pet per user total, so it becomes a composite
-- (user_id, app_context) unique instead.
--
-- Existing single-pet rows are attributed to whichever app their current
-- specie maps to (DOG -> WALLET, WOLF -> ACADEMY, FOX -> HEALTH); a legacy
-- pick predating the species lock (CAT/BEAR/LION/OWL) defaults to WALLET —
-- no row is deleted, and the account simply gets a fresh pet prompt in the
-- other two apps going forward.
alter table jf_pets add column app_context varchar(20);

update jf_pets set app_context = 'WALLET' where specie = 'DOG';
update jf_pets set app_context = 'ACADEMY' where specie = 'WOLF';
update jf_pets set app_context = 'HEALTH' where specie = 'FOX';
update jf_pets set app_context = 'WALLET' where app_context is null;

alter table jf_pets alter column app_context set not null;

-- user_id's unique constraint was created inline and unnamed (V1), so its
-- real name is engine-generated and differs between H2/PostgreSQL — same
-- swap-column workaround V28 used for the specie check, this time replacing
-- it with a composite, explicitly-named constraint.
alter table jf_pets add column user_id_new bigint;
update jf_pets set user_id_new = user_id;
alter table jf_pets drop column user_id;
alter table jf_pets rename column user_id_new to user_id;
alter table jf_pets alter column user_id set not null;
alter table jf_pets add constraint fk_pets_user foreign key (user_id) references jf_users (user_id);
alter table jf_pets add constraint uq_pets_user_app unique (user_id, app_context);
