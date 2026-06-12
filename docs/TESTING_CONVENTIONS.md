# Testing Conventions

P0 establishes deterministic JVM fixtures in `core:util`:

- Fixed instants instead of the current clock
- Explicit `Asia/Shanghai` time zone instead of the host default
- Sequential stable IDs instead of random UUIDs

New tests must inject or pass time, zone and ID sources where behavior depends
on them. Tests must not rely on the machine date, default time zone, locale,
network access or random identifiers.

Entity factories should be added beside the first real entity tests rather
than creating unused factories in advance.
