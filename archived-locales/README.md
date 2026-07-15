# Archived locales

These `values-<code>/strings.xml` translations were removed from the app when it
narrowed to a **Europe-only** release (2026-07-15). They are kept here — outside
`app/src/main/res`, so they are **not** compiled into the app — purely so the work
is recoverable if we expand to these markets again.

| Folder | Language |
|--------|----------|
| `values-ar` | Arabic |
| `values-bn` | Bengali |
| `values-hi` | Hindi |
| `values-in` | Indonesian (Android legacy code for `id`) |
| `values-ja` | Japanese |
| `values-ko` | Korean |
| `values-tr` | Turkish |
| `values-uk` | Ukrainian |
| `values-ur` | Urdu |
| `values-vi` | Vietnamese |
| `values-zh` | Chinese |

## Restoring one

1. `git mv archived-locales/values-<code> app/src/main/res/values-<code>`
2. Re-add its entry to the `Language` enum in
   `app/src/main/java/com/budgetty/app/data/settings/AppSettings.kt`.

Note: these files reflect the string set as of the archive date and may be missing
keys added to `values/strings.xml` afterwards — reconcile against the base before shipping.
