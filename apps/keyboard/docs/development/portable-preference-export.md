# Portable preference export

Status: Development source only.

This slice adds the local read side of the existing `goreecloud-keyboard-preferences/1` format without broadening the approved portability schema.

## Read authority

`KeyboardPortablePreferenceExport` accepts only `EmojiCategoryPreferenceReader`. The interface exposes exactly one method returning the last explicitly selected `EmojiCategory`.

`LocalEmojiCategoryStore` implements this narrow reader using the existing private application preference. The export path has no generic SharedPreferences reader and no surface for editor, clipboard, recents, search, suggestion, telemetry, account, or secret state.

## Exported state

Version 1 still contains exactly one value:

- last explicitly selected emoji category.

The value is serialized by the existing strict `KeyboardPortablePreferences` codec. The export path does not add a second file format.

## Explicit exclusions

The export seam cannot read or serialize:

- typed text;
- composing or surrounding-editor context;
- suggestions or learned input;
- emoji recents or frequency history;
- emoji search queries;
- clipboard contents/history;
- key-event history;
- sensitive-editor content;
- application/editor identifiers derived from typing sessions;
- telemetry identifiers;
- GoreeCloud Identity data; or
- cryptographic keys or reusable secrets.

Device-local emoji recents remain separate usage-derived history and stay outside the portable schema.

## Recovery and platform boundary

This creates a real local v1 export seam paired with the existing bounded one-field import seam. It does not add a user-facing share/file workflow, provenance, account/device ownership proof, cross-device synchronization, Everkeep backup/restore, Privacy Shield acceptance, Wardveil Security acceptance, release, production approval, or Stable qualification.
