# ──────────────────────────────────────────────────────────────────────
# Tuned — R8 / ProGuard rules
# ──────────────────────────────────────────────────────────────────────
# Most libraries (Room, Media3, OkHttp, Coil, DataStore, Ktor,
# kotlinx.serialization, Compose, etc.) ship their own consumer
# ProGuard rules that R8 merges automatically. Only project-specific
# rules that those consumer files cannot cover belong here.
#
# To audit what R8 actually sees, inspect:
#   app/build/outputs/mapping/release/configuration.txt
# ──────────────────────────────────────────────────────────────────────

# Currently empty — all dependencies provide adequate consumer rules.
# Add rules here only when R8 reports missing_rules.txt or runtime
# reflection / serialization failures occur.
