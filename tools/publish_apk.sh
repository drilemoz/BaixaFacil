#!/usr/bin/env bash
# Publish exactly three supported ABI-specific APKs, then verify the SHA-256 readback.
set -euo pipefail
: "${APK_DIR:?APK_DIR ausente}"
: "${VERSION:?VERSION ausente}"
: "${GH_TOKEN:?GH_TOKEN ausente}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY ausente}"
tag="v${VERSION}"
abis=(armeabi-v7a arm64-v8a x86_64)
files=()
for abi in "${abis[@]}"; do
  file="${APK_DIR}/Baixa-Facil-v${VERSION}-${abi}.apk"
  [[ -s "$file" ]] || { echo "::error::APK ausente: $file"; exit 1; }
  files+=("$file")
done
shopt -s nullglob
all_apks=("$APK_DIR"/*.apk)
[[ ${#all_apks[@]} -eq 3 ]] || { echo '::error::Pasta com APKs extras ou incompleta'; exit 1; }
shopt -u nullglob

max_attempts="${RELEASE_MAX_ATTEMPTS:-4}"
[[ "$max_attempts" =~ ^[1-4]$ ]] || { echo '::error::Tentativas inválidas'; exit 1; }
uploaded=0
for ((attempt=1; attempt<=max_attempts; attempt++)); do
  if gh release view "$tag" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
    if gh release upload "$tag" "${files[@]}" --repo "$GITHUB_REPOSITORY" --clobber; then
      uploaded=1; break
    fi
  else
    if gh release create "$tag" "${files[@]}" \
        --repo "$GITHUB_REPOSITORY" --title "Baixa Fácil ${VERSION}" \
        --notes-file RELEASE_NOTES.md --target "${GITHUB_SHA:-HEAD}" \
        --prerelease --latest=false; then
      uploaded=1; break
    fi
  fi
  [[ "$attempt" -eq "$max_attempts" ]] && break
  sleep "$((attempt * 5))"
done
[[ "$uploaded" -eq 1 ]] || { echo '::error::Falha ao publicar os três APKs'; exit 1; }

# Check exact set of release APK assets before trusting anything downloaded.
asset_names="$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json assets --jq '.assets[].name')"
apk_count="$(printf '%s\n' "$asset_names" | grep -Ec '\.apk$' || true)"
[[ "$apk_count" -eq 3 ]] || { echo "::error::Release tem $apk_count APKs; esperado 3"; exit 1; }
for file in "${files[@]}"; do
  [[ "$(printf '%s\n' "$asset_names" | grep -Fxc -- "$(basename "$file")")" -eq 1 ]] || {
    echo "::error::Asset ausente ou repetido: $(basename "$file")"; exit 1;
  }
done

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
verified=0
for ((attempt=1; attempt<=max_attempts; attempt++)); do
  rm -f "$tmp"/*.apk
  if gh release download "$tag" --repo "$GITHUB_REPOSITORY" \
        --pattern "Baixa-Facil-v${VERSION}-*.apk" --dir "$tmp" >/dev/null 2>&1; then
    match=1
    for file in "${files[@]}"; do
      downloaded="$tmp/$(basename "$file")"
      if [[ ! -s "$downloaded" || "$(sha256sum "$downloaded" | cut -d' ' -f1)" != "$(sha256sum "$file" | cut -d' ' -f1)" ]]; then
        match=0
        break
      fi
    done
    if [[ "$match" -eq 1 ]]; then verified=1; break; fi
    echo "::warning::SHA-256 divergente ou APK ausente (tentativa $attempt)"
  fi
  [[ "$attempt" -eq "$max_attempts" ]] && break
  sleep "$((attempt * 5))"
done
[[ "$verified" -eq 1 ]] || { echo '::error::Release não corresponde aos três APKs locais (SHA-256)'; exit 1; }
release_url="$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json url --jq '.url')"
echo "Publicação de três arquiteturas verificada: $release_url"
for file in "${files[@]}"; do sha256sum "$file"; done
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo '### Baixa Fácil, APKs por arquitetura'
    echo "- [Release com os três APKs]($release_url)"
    echo "- Assinatura: \`${SIGNING_MODE:-não informada}\`"
    for file in "${files[@]}"; do
      echo "- \`$(basename "$file")\`: SHA-256 \`$(sha256sum "$file" | cut -d' ' -f1)\`"
    done
    echo '- Para celulares Android ARM modernos, prefira arm64-v8a; confira a ABI do aparelho.'
    echo '- ZIP de código-fonte não é Artifact de workflow.'
  } >> "$GITHUB_STEP_SUMMARY"
fi
