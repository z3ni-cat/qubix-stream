# Dependency Matrix — Qubix Stream

Dipetakan sebelum push pertama (pelajaran dari Qubix Music).
compileSdk mengikuti syarat TERTINGGI, bukan downgrade library.

| Library | Version | Source | compileSdk req | Verified |
|---------|---------|--------|----------------|----------|
| Compose BOM | 2024.12.01 | Google Maven | 35 (OK ≤ 37) | ✓ dl.google.com |
| Media3 (exoplayer, session) | 1.9.3 | Google Maven | 35 (OK ≤ 37) | ✓ dl.google.com (Cloudstream proven) |
| Coil3 (coil-compose, coil-network-okhttp) | 3.3.0 | Maven Central | 35 (OK ≤ 37) | ✓ repo.maven.apache.org (Cloudstream pins strictly) |
| Jsoup | 1.22.1 | Maven Central | n/a (pure Java) | ✓ repo.maven.apache.org (Cloudstream proven) |
| Room (runtime, ktx, compiler) | 2.8.5 | Google Maven | 35 (OK ≤ 37) | ✓ dl.google.com |
| Retrofit | 2.11.0 | Maven Central | n/a (pure Java) | ✓ repo.maven.apache.org |
| Gson converter | 2.11.0 | Maven Central | n/a | ✓ repo.maven.apache.org |
| Coroutines (android) | 1.11.0 | Maven Central | n/a | ✓ repo.maven.apache.org (Cloudstream) |
| Lifecycle runtime | 2.8.6 | Google Maven | 34 (OK ≤ 37) | ✓ dl.google.com |
| Activity Compose | 1.9.2 | Google Maven | 34 (OK ≤ 37) | ✓ dl.google.com |
| Core KTX | 1.15.0 | Google Maven | 34 (OK ≤ 37) | ✓ dl.google.com |

**Keputusan:**
- `compileSdk = 35` (syarat tertinggi: Compose/Media3/Coil3/Room butuh ≥35; 35 (API 37 belum rilis; 35 memenuhi syarat 226535 semua library)
- `targetSdk = 36`, `minSdk = 26`
- AGP 8.5.2, Kotlin 2.0.20, JVM 17
- Retrofit 2.11.0 (BUKAN 3.0.0 — major baru, hindari breaking API)
- Coil 3.3.0 strictly (Cloudstream: "Later versions require jvmTarget 11 or later")
- Media3 1.9.3 (Cloudstream proven; 1.11.1 butuh 36 — OK juga, tapi 1.9.3 lebih aman)

**Workflow:** install `platforms;android-37` + `build-tools;35.0.0` via sdkmanager di GitHub Actions.
