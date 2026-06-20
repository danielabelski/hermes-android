# Keep defaults for now. Add explicit keep rules for JS interfaces if introduced.

# Tink references optional compile-time annotations that are not needed at runtime.
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
