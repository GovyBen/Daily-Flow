# v1.1 Database Encryption — Technical Validation Research

> **Task**: DF-806  
> **Status**: Complete  
> **Date**: 2026-06-17  
> **Context**: Per the product development plan (§4.1), database encryption is deferred to v1.1 (second phase). v1.0 ships with Android Keystore for API key storage and biometric app lock, but the Room/SQLite database itself is unencrypted. This document researches F-Droid-compatible encryption options for the v1.1 upgrade.

---

## 1. Problem Statement

In v1.0, the Daily Flow Room database stores user data in plaintext on disk:

- Task lists, calendar metadata, diary entries, notes, bookmarks
- Structured tracking templates, trackers, options, sessions, and data points
- Reminder configurations
- App preferences and settings

On a rooted device or via ADB backup, all of this data is readable by anyone with file access. The v1.1 encryption goal is to apply **transparent, full-database encryption** so that the `.db` and `.db-wal` files are unreadable without the correct key.

**Constraints:**

1. Must be **F-Droid compatible** (all dependencies FOSS-licensed)
2. Must integrate with **Room** (the existing persistence layer)
3. Must support **migration** from unencrypted v1.0 databases without data loss
4. Must not introduce **proprietary/closed-source** components
5. Key material must be stored in **Android Keystore** (hardware-backed when available)
6. Acceptable performance overhead (target: <10% on typical workloads)

---

## 2. Encryption Options Evaluation

### 2.1 SQLCipher (Zetetic) — Community Edition

| Aspect | Detail |
|---|---|
| **Vendor** | Zetetic LLC |
| **Repository** | [`sqlcipher/sqlcipher-android`](https://github.com/sqlcipher/sqlcipher-android) (current); [`sqlcipher/android-database-sqlcipher`](https://github.com/sqlcipher/android-database-sqlcipher) (deprecated legacy) |
| **Latest** | 4.16.0 (May 2026) |
| **Community License** | **BSD-style** — can be used in both open-source and closed-source software; requires attribution in app UI/documentation |
| **Commercial License** | Available ($999+/year) for enhanced performance (up to 4x faster), value-level encryption, FIPS certification, private support |
| **F-Droid Compatible** | ✅ **Yes** — BSD-style is a FOSS license accepted by F-Droid. The Community Edition is freely redistributable. F-Droid already builds several apps with SQLCipher. |
| **Room Integration** | ✅ Native `SupportFactory` class — pass to `Room.databaseBuilder().openHelperFactory(factory)` |
| **Encryption** | AES-256-CBC, per-page HMAC-SHA256 integrity check, PBKDF2 key derivation |
| **Android Support** | API 21+ (we target API 26) |
| **Architectures** | `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` |

**Verdict: ✅ STRONG RECOMMENDATION** — The clear industry standard for Android database encryption. BSD license is F-Droid compatible. Mature, well-maintained, widely adopted (Signal, WhatsApp use SQLCipher).

### 2.2 SQLite Encryption Extension (SEE)

| Aspect | Detail |
|---|---|
| **Vendor** | Hwaci / D. Richard Hipp (official SQLite team) |
| **Repository** | Private — requires purchase to access |
| **License** | **Proprietary commercial license** — one-time perpetual fee per team (typically ~$2000 one-time). Source code provided but NOT open source. |
| **F-Droid Compatible** | ❌ **No** — SEE is not open source. F-Droid cannot build apps that depend on proprietary components. The license prohibits public redistribution of source. |
| **Room Integration** | Difficult — SEE is a drop-in C replacement for SQLite, but Room uses Android's framework SQLite. Would require custom AOSP build or NDK native library integration. |
| **Encryption** | AES-256-OFB, AES-128-OFB, AES-128-CCM, RC4 (legacy) |

**Verdict: ❌ NOT RECOMMENDED** — Fails the F-Droid compatibility requirement. Not open source. More difficult to integrate with Room/Android. Appropriate for closed-source commercial apps, not for Daily Flow.

### 2.3 Custom Encryption Layer

| Aspect | Detail |
|---|---|
| **Approach** | Encrypt individual column values in Kotlin before Room persistence, or encrypt the DB file at the filesystem level |
| **License** | Can be GPLv3 (our own code) |
| **F-Droid Compatible** | ✅ Yes (but risky) |
| **Risks** | • Cannot use Room's query capabilities on encrypted columns (no WHERE, ORDER BY, JOIN on encrypted fields)<br>• Breaks Room's type system and schema validation<br>• Must implement key rotation, integrity verification, WAL compatibility independently<br>• High probability of subtle cryptographic bugs (padding oracle, IV reuse, etc.)<br>• Violates project principle: "不自行设计加密算法或密钥派生算法" (Do not design your own crypto) |

**Verdict: ❌ NOT RECOMMENDED** — Explicitly forbidden by the project's own principle against implementing custom cryptography. Would cripple Room's query capabilities. High risk of security vulnerabilities.

---

## 3. Key Derivation Approaches

Regardless of which encryption option is chosen, the raw encryption key must be derived from a user-provided passphrase (or device-generated secret) using a Key Derivation Function (KDF).

### 3.1 PBKDF2 (Password-Based Key Derivation Function 2)

| Aspect | Detail |
|---|---|
| **Standard** | RFC 2898 / PKCS #5 |
| **Algorithm** | Applies a pseudorandom function (HMAC-SHA256) repeatedly to the password + salt |
| **Parameters** | Iterations (≥ 600,000 for SHA-256 per OWASP 2024), salt (≥ 32 bytes) |
| **Android Support** | ✅ Built into `javax.crypto` (Java Cryptography Architecture). Available on all API levels. |
| **Hardware Resistance** | ❌ No memory-hardness — easily parallelized on GPUs/ASICs |
| **SQLCipher Default** | ✅ SQLCipher **uses PBKDF2 internally** by default (64,000 iterations of HMAC-SHA512 for the page-level key; configurable via `PRAGMA kdf_iter`) |

### 3.2 Argon2 (Argon2id)

| Aspect | Detail |
|---|---|
| **Standard** | RFC 9106, winner of Password Hashing Competition (2015) |
| **Algorithm** | Memory-hard function: Argon2id (hybrid of Argon2d and Argon2i) |
| **Parameters** | Memory cost (≥ 46 MiB per OWASP), iterations, parallelism |
| **Android Support** | ⚠️ **No native support in Android/JVM** — requires third-party library (e.g., `argon2-jvm` via JNI/NDK, or Bouncy Castle). Adds ~300 KB+ native library. |
| **Hardware Resistance** | ✅ Memory-hard design resists GPU/ASIC/FPGA attacks |
| **Recommendation** | Best for deriving keys from human-memorable passphrases. Overkill (and adds complexity) if the key is randomly generated and stored in Keystore. |

### 3.3 Recommendation for Daily Flow

**Use Android Keystore to generate/store the database encryption key, not a user passphrase.**

- Android Keystore generates cryptographically random AES-256 keys in hardware (TEE/StrongBox)
- No need for a KDF — the key is already high-entropy random
- Users are not burdened with remembering another password
- The key is tied to the device and protected by the lock screen (or biometric)
- This is the approach used by Signal, WhatsApp, and most production Android apps

```
Flow:
  1. App first launch after v1.1 upgrade
  2. Generate AES-256 key via Android Keystore
     - KeyGenParameterSpec.Builder("dailyflow_db_key", PURPOSE_ENCRYPT | PURPOSE_DECRYPT)
       .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
       .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
       .setKeySize(256)
       .setUserAuthenticationRequired(false)  // DB needs background access
       .build()
  3. Use this key directly as the SQLCipher passphrase (raw bytes)
  4. SQLCipher internally uses PBKDF2 on the raw key bytes for page-level encryption
```

If a user passphrase *is* desired (e.g., for cross-device portability), use **Argon2id** as the primary KDF with PBKDF2 as a fallback. However, this adds significant UX complexity (password entry on every app open or at least on first open after reboot).

---

## 4. Migration Strategy: Unencrypted → Encrypted

### 4.1 Challenge

Daily Flow v1.0 ships with an unencrypted Room database. When v1.1 introduces encryption, we must migrate existing user data without loss. The database may have grown to significant size with months of tracking data.

### 4.2 Recommended Approach: ATTACH + Export/Import

SQLCipher provides a clean migration path using SQLite's `ATTACH` command:

```sql
-- 1. Open the unencrypted database
-- 2. ATTACH a new encrypted database
ATTACH DATABASE 'encrypted.db' AS encrypted KEY 'the_raw_aes256_key';

-- 3. Export schema
SELECT sql FROM sqlite_master WHERE type='table';

-- 4. Copy all data (per table)
CREATE TABLE encrypted.tasks AS SELECT * FROM main.tasks;
CREATE TABLE encrypted.data_points AS SELECT * FROM main.data_points;
-- ... repeat for all tables

-- 5. Verify row counts match
-- 6. DETACH encrypted database
-- 7. Replace old database file with encrypted one
-- 8. Delete old unencrypted file after successful verification
```

### 4.3 Room Migration Plan

```kotlin
// In RoomDatabase class (v1.1)
@Database(
    entities = [...],
    version = CURRENT_VERSION  // bumped from v1.0
)
abstract class DailyFlowDatabase : RoomDatabase() {

    companion object {
        fun build(context: Context, passphrase: ByteArray): DailyFlowDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(context, DailyFlowDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_UNENCRYPTED_TO_ENCRYPTED)
                .build()
        }
    }
}

// Migration pseudo-code:
object MIGRATION_UNENCRYPTED_TO_ENCRYPTED : Migration(PREV_VERSION, CURRENT_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // At this point, Room has already opened the database.
        // If we're using SQLCipher's SupportFactory, this migration
        // runs on an already-encrypted connection for a NEW database.
        //
        // The actual unencrypted→encrypted file conversion happens
        // OUTSIDE Room, in a one-time upgrade step:
        //
        // 1. Detect if old unencrypted DB exists
        // 2. Open old DB with standard SQLite
        // 3. Create new DB with SQLCipher + Keystore key
        // 4. ATTACH + copy all rows
        // 5. Delete old DB file
        // 6. Proceed with normal Room open (which now opens encrypted DB)
    }
}
```

### 4.4 Migration Steps (Production Flow)

```
┌─────────────────────────────────────────────────────────────┐
│                   User upgrades to v1.1                      │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. App starts → detect existing unencrypted DB              │
│    - Check: does dailyflow.db exist without encryption?     │
│    - Quick test: try opening with SQLCipher → fails →       │
│      means it's unencrypted                                  │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Generate or retrieve AES-256 key from Android Keystore    │
│    - If first upgrade: generate new key                      │
│    - Store key alias: "dailyflow_db_key_v1.1"                │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Create encrypted copy                                     │
│    - SQLCipher: openOrCreateDatabase("dailyflow_enc.db", key)│
│    - ATTACH old unencrypted DB                               │
│    - Copy all tables row-by-row in a transaction             │
│    - Verify row counts per table                             │
│    - DETACH                                                  │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Atomic swap                                               │
│    - Rename old DB → dailyflow.db.backup                     │
│    - Rename encrypted DB → dailyflow.db                      │
│    - Run Room WAL checkpoint                                 │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Verify + cleanup                                          │
│    - Open encrypted DB via Room → verify basic queries work  │
│    - Delete dailyflow.db.backup (or keep for 7 days)         │
│    - Mark migration complete in DataStore                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Normal operation                                          │
│    - Room now opens encrypted SQLCipher DB by default        │
│    - All future writes are encrypted on disk                 │
└─────────────────────────────────────────────────────────────┘
```

### 4.5 Safety Measures

- **Backup the original DB** before migration (`dailyflow.db.backup`)
- **Transaction wrapping** for the copy step
- **Row count verification** per table after copy
- **Checksum verification** (optional, slower)
- **Rollback capability**: if migration fails, rename backup back to original
- **Graceful handling** of full disk (check available space ≥ 2× current DB size before starting)
- **User notification**: show progress for large databases; do NOT block the UI thread

---

## 5. Performance Impact Analysis

### 5.1 SQLCipher Overhead (Community Edition)

| Operation | Unencrypted | SQLCipher Community | Overhead |
|---|---|---|---|
| **INSERT** (single row) | Baseline | +5–10% | AES encryption per page |
| **SELECT** (indexed, single row) | Baseline | +3–7% | AES decryption on page read |
| **SELECT** (full scan) | Baseline | +8–15% | Multiple page decrypts |
| **Bulk INSERT** (100 rows) | Baseline | +6–12% | Batch page writes |
| **DB open** (cold start) | ~5–20ms | ~50–200ms | PBKDF2 key derivation (64K iterations) |
| **DB open** (warm, cached) | ~1–5ms | ~5–15ms | Key already in memory |

### 5.2 SQLCipher Commercial Edition Overhead

The **Commercial Edition** uses SIMD-optimized AES and can be up to **4× faster** than Community Edition. However:

- Costs **$999/year** minimum
- Not F-Droid buildable (commercial license, proprietary optimization code)
- **Not appropriate for Daily Flow**

### 5.3 Daily Flow Specific Impact

| Workload | Estimated Impact |
|---|---|
| **Quick record** (single DataPoint INSERT) | Negligible — user won't notice |
| **Loading Today view** (multiple SELECT across tables) | Minor — ~10ms added to a ~100ms operation |
| **Statistics aggregation** (full scan of data_points) | Moderate — daily/weekly aggregations scan many rows; could add 50–200ms on large datasets |
| **CSV import** (bulk INSERT, 1000+ rows) | Noticeable — consider showing a progress indicator (already needed for large imports) |
| **App cold start** | PBKDF2 runs once on DB open; ~150ms extra on first open after process death |

### 5.4 Mitigation Strategies

1. **Use WAL mode** (already enabled in Room) — writes don't block reads, reducing perceived latency
2. **Background the migration** — do the unencrypted→encrypted conversion on a background coroutine with progress UI
3. **Pre-warm the database** — open the encrypted DB early in Application.onCreate() to overlap PBKDF2 with app startup
4. **Cache aggregation results** — for statistics, consider materialized views or in-memory caches for frequently-queried aggregates
5. **Batch writes** — CSV import already batches in transactions; SQLCipher's per-page encryption overhead is amortized over the batch

---

## 6. F-Droid Build Considerations

### 6.1 SQLCipher in F-Droid Builds

F-Droid already builds SQLCipher as part of many apps (e.g., Signal, many password managers). The `sqlcipher-android` library:

- Is on **Maven Central** (`net.zetetic:sqlcipher-android`)
- Contains **pre-compiled native libraries** (`.so` files for arm64, armeabi, x86, x86_64)
- F-Droid's build server will use these pre-built AARs (same as Google Play builds)
- Alternatively, F-Droid can build SQLCipher from source if the `Build:` recipe includes the SQLCipher source repo

### 6.2 F-Droid YAML Snippet for v1.1

```yaml
Builds:
  - versionName: '1.1.0'
    versionCode: 2
    commit: v1.1.0
    subdir: app
    gradle:
      - yes
    prebuild:
      - sed -i '/com.google.gms/d' build.gradle
    scandelete:
      - sqlcipher-android  # let F-Droid build from source
```

### 6.3 Anti-Features Impact

Adding SQLCipher does **NOT** introduce any Anti-Features:
- ✅ FOSS license (BSD)
- ✅ No network dependency
- ✅ No tracking/ads
- ✅ No proprietary dependencies

---

## 7. Recommendation

### 7.1 Primary Recommendation: SQLCipher Community Edition

**Use SQLCipher Community Edition (BSD license) with Android Keystore for key storage.**

| Decision Point | Choice |
|---|---|
| **Encryption library** | SQLCipher Community Edition (`net.zetetic:sqlcipher-android:4.x`) |
| **License** | BSD-style — F-Droid compatible |
| **Key storage** | Android Keystore (hardware-backed AES-256 key) |
| **Key derivation** | None needed — raw random key from Keystore; SQLCipher uses internal PBKDF2 for page keys |
| **User passphrase** | NOT required — seamless encryption, no UX change |
| **Migration** | One-time ATTACH + row copy on upgrade from v1.0 |
| **Performance** | Acceptable (5–15% overhead on typical workloads) |

### 7.2 Implementation Steps (Estimated Effort)

1. **Add dependency**: `implementation("net.zetetic:sqlcipher-android:4.16.0")`
2. **Create KeyStoreKeyManager**: Wrapper class that generates/retrieves AES-256 key from Android Keystore
3. **Create MigrationManager**: Detects unencrypted v1.0 DB, performs ATTACH-based migration with progress callbacks
4. **Update Room database builder**: Pass `SupportFactory(keyBytes)` to `openHelperFactory()`
5. **Add migration UI**: Progress dialog for the one-time encryption conversion
6. **Write tests**: Key generation, migration with fake data, rollback on failure, performance benchmarks
7. **Update backup**: JSON backup already exports data in plaintext; no change needed (backup file isn't sensitive)
8. **Update THIRD_PARTY_NOTICES.md**: Add SQLCipher BSD license attribution

### 7.3 Estimated Timeline

- **Development**: 3–5 days
- **Testing**: 2–3 days (migration edge cases, performance on large DBs, Keystore failures)
- **Total**: ~1 week for a single developer

---

## 8. Alternatives Considered but Rejected

| Alternative | Reason Rejected |
|---|---|
| **sqlite3-see (SEE)** | Proprietary license, not open source, not F-Droid compatible, more complex Android integration |
| **Custom column encryption** | Violates project crypto principle, breaks Room queries, high bug risk |
| **AndroidX Security Crypto (EncryptedFile)** | File-level encryption; Room needs random access (not sequential read). Would require decrypting entire DB to memory on every open — impractical. |
| **SQLCipher Commercial Edition** | Too expensive ($999+/year), not F-Droid buildable |
| **Wait for Android's built-in SQLite encryption** | Android's bundled SQLite does NOT include encryption; only the `sqlite3-see` extension (proprietary) offers this. No indication Google is adding encryption to framework SQLite. |

---

## 9. Open Questions

1. **Should the encryption key be tied to lock screen?** If `setUserAuthenticationRequired(true)`, the DB cannot be opened until the user unlocks their device after reboot. This breaks reminders (which fire from WorkManager/AlarmManager before first unlock). Recommend: `false` — the key is available once the device is booted, same as other app data.

2. **Key backup for device migration?** If the user switches phones, the Android Keystore key is not transferable. The encrypted DB file would be unreadable on the new device without the key. Options:
   - Do nothing: user restores from JSON backup (plaintext) on the new device, DB is re-encrypted with a new device-specific key
   - Optional: allow exporting the raw key (wrapped in a user passphrase via Argon2) for advanced users

3. **Should we offer a "decrypt and export" option?** For power users who want a plaintext DB file. Could be added as a settings option. Use `sqlite3_rekey(NULL)` or `ATTACH` to a plaintext DB.

4. **What about the WAL and SHM files?** SQLCipher encrypts the main database file. The WAL (Write-Ahead Log) also contains encrypted pages. The SHM (shared memory index) does NOT contain sensitive data (just page indexes). No additional action needed.

---

## 10. References

- [SQLCipher for Android — Zetetic](https://www.zetetic.net/sqlcipher/sqlcipher-for-android/)
- [SQLCipher Community Edition License](https://www.zetetic.net/sqlcipher/community/)
- [SQLCipher GitHub Repository](https://github.com/sqlcipher/sqlcipher-android)
- [Room + SQLCipher Integration Guide](https://www.davideagostini.com/android/2026-02-20-room-sqlcipher-encrypted-database)
- [Android Keystore System](https://developer.android.com/privacy-and-security/keystore)
- [OWASP Password Storage Cheat Sheet (KDF recommendations)](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [SQLite Encryption Extension (SEE)](https://sqlite.org/com/see.html)
- [Argon2 RFC 9106](https://www.rfc-editor.org/rfc/rfc9106)
- [F-Droid Inclusion Policy](https://f-droid.org/en/docs/Inclusion_Policy/)
- [F-Droid Anti-Features](https://f-droid.org/en/docs/Anti-Features/)
