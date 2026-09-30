# Persistent Emoji Category — Development

GoreeCloud Keyboard now restores the last explicitly selected emoji category after the IME process restarts. The stored value is only the bounded `EmojiCategory` enum name and lives in Android `MODE_PRIVATE` preferences alongside the existing local emoji-recents domain.

Unknown, missing, or malformed stored values fail closed to Smileys. Selecting a category updates the local preference; opening recents or clearing recents does not overwrite the remembered category.

No new permission, network path, telemetry, account dependency, cloud synchronization, or learned-language/personality profile is introduced.

This slice is a device-local presentation convenience only and does not establish backup/sync acceptance, cross-device personalization, or Stable qualification.
