#!/usr/bin/env bash
set -euo pipefail
# Assembled, unsigned review materials only. No credential access, merge or publication.
python3 scripts/check_manifest.py
python3 scripts/check_release.py
"$ANDROID_HOME/build-tools/36.0.0/zipalign" -c -P 16 4 app/build/outputs/apk/release/app-release-unsigned.apk
hayya_revision=$(git rev-parse HEAD)
hayya_output="release-review/$hayya_revision"
mkdir -p "$hayya_output"
cp app/build/outputs/apk/release/app-release-unsigned.apk "$hayya_output/"
cp app/build/outputs/bundle/release/app-release.aab "$hayya_output/"
git archive --format=tar.gz --prefix="hayya-source-$hayya_revision/" --output="$hayya_output/source.tar.gz" HEAD
cp app/build/reports/release/artifact.json "$hayya_output/"
cp -R app/build/outputs/mapping/release "$hayya_output/r8-mapping"
cp -R fastlane/metadata "$hayya_output/store-metadata"
cp PRIVACY.md docs/PROGRESS.md docs/RELEASE.md THIRD_PARTY_NOTICES.md "$hayya_output/"
printf '%s\n' 'UNSIGNED INTERNAL REVIEW MATERIALS. Physical/reviewer gates and production signing approval remain required.' > "$hayya_output/REVIEW_REQUIRED.txt"
(cd "$hayya_output" && find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS)
printf 'Prepared unsigned review kit at %s\n' "$hayya_output"
