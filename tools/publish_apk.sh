#!/usr/bin/env bash
# Publish only an APK binary in GitHub Releases, then verify bytes downloaded from GitHub.
set -euo pipefail
: "${APK_FILE:?APK_FILE ausente}"
: "${VERSION:?VERSION ausente}"
: "${GH_TOKEN:?GH_TOKEN ausente}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY ausente}"
tag="v${VERSION}"
name="Baixa-Facil-v${VERSION}.apk"
[[ -s "$APK_FILE" ]] || { echo "::error::APK ausente ou vazio: $APK_FILE"; exit 1; }
[[ "$(basename "$APK_FILE")" == "$name" ]] || { echo '::error::Nome do APK divergente da versão'; exit 1; }
local_sha="$(sha256sum "$APK_FILE" | cut -d' ' -f1)"

max_attempts="${RELEASE_MAX_ATTEMPTS:-4}"
[[ "$max_attempts" =~ ^[1-4]$ ]] || { echo "::error::Tentativas inválidas"; exit 1; }
uploaded=0
for ((attempt=1; attempt<=max_attempts; attempt++)); do
  if gh release view "$tag" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
    if gh release upload "$tag" "$APK_FILE" --repo "$GITHUB_REPOSITORY" --clobber; then
      uploaded=1; break
    fi
  else
    if gh release create "$tag" "$APK_FILE" \
        --repo "$GITHUB_REPOSITORY" --title "Baixa Fácil ${VERSION}" \
        --notes-file RELEASE_NOTES.md --target "${GITHUB_SHA:-HEAD}" \
        --prerelease --latest=false; then
      uploaded=1; break
    fi
  fi
  [[ "$attempt" -eq "$max_attempts" ]] && break
  sleep "$((attempt * 5))"
done
[[ "$uploaded" -eq 1 ]] || { echo '::error::Não foi possível publicar APK na Release'; exit 1; }

# A API da Release pode demorar a mostrar o asset após o upload.
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
verified=0
for ((attempt=1; attempt<=max_attempts; attempt++)); do
  rm -f "$tmp/$name"
  if gh release download "$tag" --repo "$GITHUB_REPOSITORY" \
        --pattern "$name" --dir "$tmp" > /dev/null 2>&1 && [[ -s "$tmp/$name" ]]; then
    published_sha="$(sha256sum "$tmp/$name" | cut -d' ' -f1)"
    if [[ "$published_sha" == "$local_sha" ]]; then verified=1; break; fi
    echo "::warning::SHA-256 diferente na tentativa ${attempt}; repetindo verificação."
  fi
  [[ "$attempt" -eq "$max_attempts" ]] && break
  sleep "$((attempt * 5))"
done
[[ "$verified" -eq 1 ]] || { echo '::error::APK publicado não confere com o APK compilado (SHA-256)'; exit 1; }

# A Release deve ter somente um asset APK. Os links ZIP/TAR.GZ gerados pelo
# GitHub são links automáticos de código-fonte, não Artifacts de workflow.
asset_names="$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json assets --jq '.assets[].name')"
apk_count="$(printf '%s\n' "$asset_names" | grep -Ec '\.apk$' || true)"
[[ "$apk_count" -eq 1 && "$asset_names" == *"$name"* ]] || {
  echo '::error::A Release possui APKs incorretos ou mais de um APK'; exit 1;
}
release_url="$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json url --jq '.url')"
printf 'APK publicado e conferido: %s\nSHA-256: %s\nRelease: %s\n' "$name" "$local_sha" "$release_url"
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo '### Baixa Fácil'
    echo "- [Baixar APK na Release]($release_url)"
    echo "- APK: \`$name\`"
    echo "- Assinatura: \`${SIGNING_MODE:-não informada}\`"
    echo "- SHA-256 verificado após download: \`$local_sha\`"
    echo '- O workflow não publica Artifact ZIP.'
  } >> "$GITHUB_STEP_SUMMARY"
fi
