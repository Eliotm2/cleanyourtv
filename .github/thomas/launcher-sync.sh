#!/usr/bin/env bash
# Fork (Thomas): merge the latest upstream launcher release (Gathin23/cleanyourtv, tags vX.Y.Z) into the
# current branch (thomas), or stop. Exit 0 with "new=false" when there is nothing new, 0 with "new=true"
# when the merge is ready to build (not committed), 2 when a person must look ($RUNNER_TEMP/reason.md).
set -euo pipefail
out=${GITHUB_OUTPUT:-/dev/stdout}
reason=${RUNNER_TEMP:-/tmp}/reason.md

git remote get-url upstream >/dev/null 2>&1 || git remote add upstream https://github.com/Gathin23/cleanyourtv
git fetch --quiet --tags upstream
latest=$(git tag -l 'v[0-9]*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -1)
echo "upstream latest launcher release: $latest"
echo "tag=$latest" >> "$out"
if git merge-base --is-ancestor "$latest" HEAD; then
  echo "already merged"; echo "new=false" >> "$out"; exit 0
fi
base=$(git show "$latest:launcher/app/build.gradle" | sed -n "s/^ *versionName '\([^']*\)'.*/\1/p" | head -1)
[ -n "$base" ] || { echo "no versionName in $latest" > "$reason"; exit 2; }

git merge --no-ff --no-commit "$latest" || true

# The upstream workflows stay out of the fork; ours are thomas-*.
for f in $(git ls-files .github/workflows | sort -u); do
  case "$f" in .github/workflows/thomas-*) ;; *) git rm -q --force --ignore-unmatch "$f" ;; esac
done
# A file the fork deleted (layout-iw, layout-zh) stays deleted when upstream changed it.
for f in $(git diff --name-only --diff-filter=U | sort -u); do
  git cat-file -e "HEAD:$f" 2>/dev/null || git rm -q --force --ignore-unmatch "$f"
done
# Strings the fork removed from the default locale leave the translations too (lint ExtraTranslation).
for f in launcher/app/src/main/res/values-*/strings.xml; do
  grep -q '^<<<<<<<' "$f" && continue
  if grep -q 'name="stats_memory"\|name="stats_storage"' "$f"; then
    sed -i '/name="stats_memory"/d; /name="stats_storage"/d' "$f"; git add "$f"
  fi
done

if ! version=$(python3 .github/thomas/launcher-version.py "$base" 2>&1); then
  printf 'La version dans launcher/app/build.gradle ne se règle pas toute seule :\n\n    %s\n' "$version" > "$reason"
  exit 2
fi
git add launcher/app/build.gradle

conflicts=$(git diff --name-only --diff-filter=U | sort -u)
if [ -n "$conflicts" ]; then
  { echo "La reprise du launcher $latest bute sur des conflits entre l'original et nos ajouts :"; echo
    for f in $conflicts; do echo "- \`$f\`"; done; } > "$reason"
  exit 2
fi
echo "version=${version%% *}" >> "$out"
echo "new=true" >> "$out"
echo "merge of $latest ready: $version"
