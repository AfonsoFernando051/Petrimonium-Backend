# Academy content

This directory is the authored source of truth for the Academy curriculum
(domains, schools, modules, lessons, steps) in pt/en/es. `AcademyContentSeedRunner`
reads every file here and upserts it into the database on every application
boot — see its Javadoc for the exact algorithm.

## Rules for editing this content

- **Ids are permanent.** `domainId`, `schoolId`, `moduleId`, `lessonId` are
  shared with `lesson_progress`/`xp_events.source_id` (a user's completion +
  XP history). Never rename or reuse one — a renamed id resets progress for
  everyone who completed it, and a reused id can grant XP twice or never.
- **To discontinue a school/module**, set `"contentAvailable": false` in its
  JSON. Do not delete its entry.
- **One file per school**, under `schools/{domainId}/{schoolId}.json`, each
  carrying its full module → lesson → step tree. `domains.json` lists the
  top-level domains.
- Steps, options, takeaways, translations and prerequisites have no id
  shared with user progress — the seeder deletes and fully reinserts them
  for their parent on every boot, so edit them freely.
- This directory is generated once from the legacy hardcoded Dart catalog by
  `petapp_mobile/tool/generate_academy_seed_json.dart`. New content from now
  on is authored directly as JSON here — that script is not meant to be run
  again.

## Adding new content

1. **Decide the target**: a new lesson in an existing module, a new module in
   an existing school (many already exist as empty `contentAvailable: false`
   placeholders — check `schools/{domainId}/{schoolId}.json` before creating
   one), a new school, or (rarely) a new domain.
2. **Generate the JSON** — see "Using an AI to draft content" below for a
   ready-to-paste prompt.
3. **Save it**:
   - New lesson/module → edit the existing school's JSON file directly.
   - New school → new file at `schools/{domainId}/{schoolId}.json`.
   - New domain → add an entry to `domains.json`.
4. **Validate the JSON syntax**: `python3 -m json.tool schools/<path>.json > /dev/null` (or any JSON validator) — a syntax error fails the whole boot, not just this file.
5. **Flip `contentAvailable: true`** on the module (and the school, if this is
   its first real content) — otherwise it stays a "coming soon" placeholder.
6. **Run the backend locally** (`./mvnw spring-boot:run`, default/dev
   profile) and check the boot log for `AcademyContentSeedRunner`'s "Academy
   content seeded: N domains, M schools" line with no errors above it. Then
   call the API to confirm the new content is there:
   ```
   curl -s -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8081/api/v1/academy/catalog?lang=pt" | less
   ```
7. **Update `AcademyContentSeedRunnerTest`** — it asserts exact row counts
   (`domains=8, schools=19, modules=14, lessons=16, steps=80` as of this
   writing) against the real content in this directory. Adding anything
   changes those numbers; the test will fail until you update them.
8. **Deploy the backend as usual.** Nothing in the Flutter app needs to
   change or be released — it fetches this content over the network and
   picks up changes the next time it successfully calls
   `GET /api/v1/academy/catalog` (no manual cache invalidation needed; the
   client always revalidates in the background on load, see
   `AcademyController._loadCatalog`).

## Using an AI to draft content

Content is easiest to generate by prompting an AI (Claude, ChatGPT, etc.)
with the schema, the rules above, and a real example as a few-shot template,
then reviewing its output before saving it here. A ready-to-paste prompt
template is kept up to date in `docs/ACADEMY_CONTENT_AUTHORING.md` at the
repo root (outside this backend module, since it's relevant to the whole
project, not just this directory).
