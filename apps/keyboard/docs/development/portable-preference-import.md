# Portable Preference Import — Development Boundary

## Status

Development source foundation only. This checkpoint does **not** establish complete Keyboard restore, import, synchronization, Everkeep recovery, Privacy Shield acceptance, or production recovery.

## Purpose

`KeyboardPortablePreferenceImport` is the explicit application seam for applying a validated `goreecloud-keyboard-preferences/1` payload to the existing private on-device emoji-category preference.

The import path intentionally matches the narrow version 1 export schema. It can write exactly one value: the last emoji category explicitly selected by the user.

## Validation-before-write rule

The importer accepts encoded snapshot text, invokes `KeyboardPortablePreferences.decode`, and does not call its writer unless decoding succeeds completely.

This preserves the existing fail-closed guarantees for:

- format and version validation;
- exact record count;
- canonical line endings;
- snapshot size;
- checksum/integrity validation; and
- supported `EmojiCategory` values.

Malformed, tampered, expanded, or unsupported snapshots therefore result in zero preference writes.

## Write authority

The importer depends on the deliberately minimal `EmojiCategoryPreferenceWriter` interface:

`save(category: EmojiCategory)`

`LocalEmojiCategoryStore` implements that interface using the existing private `goreecloud_keyboard_local_preferences` SharedPreferences store and existing `emoji_category_v1` key. No second preference database, network store, account store, or synchronization channel is introduced.

This is a real but intentionally bounded local write authority. The Platform Contract and documentation must no longer describe the codec line as having no import authority at all. Instead, they must state that a one-field Development apply seam exists while complete restore remains unimplemented and unaccepted.

## Explicitly excluded authority

The importer and writer interface have no parameters or APIs for:

- typed text;
- composing or surrounding-editor context;
- suggestions or learned language state;
- emoji recents/frequency history;
- emoji search queries;
- clipboard contents/history;
- key-event history;
- sensitive-editor values;
- application/editor activity history;
- GoreeCloud Identity credentials or identifiers;
- telemetry identifiers;
- cryptographic keys or reusable secrets; or
- arbitrary SharedPreferences keys.

The importer does not open an `InputConnection`, inspect the editor, query the clipboard, read emoji recents, request network access, or contact Identity, Mesh, Privacy Shield, Wardveil Security, Everkeep, or another service.

## User-control and recovery boundary

No production user-facing import UI is wired by this checkpoint. The source seam itself does not establish:

- a file picker or export/import workflow;
- explicit production confirmation UX;
- account/device ownership validation;
- Everkeep backup lineage;
- cross-device synchronization;
- conflict resolution between local and incoming preferences;
- product-wide restore transactions;
- restore of any state beyond the selected category;
- production Privacy Shield consent/retention authority; or
- release/Stable acceptance.

A future user-facing recovery flow must explicitly authorize invocation of this seam and preserve the version 1 privacy-minimized scope unless a separately reviewed schema revision expands it.

## Durability boundary

`LocalEmojiCategoryStore.save` retains the existing Android SharedPreferences `.apply()` behavior. The import seam requests the local preference update but does not create a new synchronous durability acknowledgement or claim crash-safe product recovery semantics.

## Acceptance boundary

Unit tests prove that valid snapshots write exactly the decoded category and invalid snapshots invoke no writer. They also lock the writer interface to an `EmojiCategory`-only parameter surface. Those tests are source evidence only and do not establish production restore, Everkeep, Privacy Shield, representative-device, release, or Stable acceptance.