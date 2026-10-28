#!/usr/bin/env bash
# Offline test: 3 APKs on fresh release + replacing assets + rejecting corrupted bytes.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/bin" "$work/github" "$work/dist"
cat > "$work/bin/gh" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
cmd="${1:-}"; sub="${2:-}"; shift 2
state="${MOCK_GH_STATE:?}"
[[ "$cmd" == release ]] || exit 7
case "$sub" in
  view)
    [[ -f "$state/created" ]] || exit 1
    if [[ "$*" == *'--json assets'* ]]; then
      find "$state" -maxdepth 1 -type f -name '*.apk' -printf '%f\n' | sort
    elif [[ "$*" == *'--json url'* ]]; then
      echo 'https://example.invalid/releases/v0.1.0-alpha.67'
    fi
    ;;
  create|upload)
    for arg in "$@"; do
      if [[ "$arg" == *.apk ]]; then
        [[ -s "$arg" ]] || exit 9
        cp "$arg" "$state/$(basename "$arg")"
      fi
    done
    touch "$state/created"
    ;;
  download)
    dir=''
    while (($#)); do
      if [[ "$1" == '--dir' ]]; then dir="$2"; shift 2; else shift; fi
    done
    [[ -f "$state/created" && -n "$dir" ]] || exit 8
    for f in "$state"/*.apk; do cp "$f" "$dir/"; done
    if [[ "${MOCK_CORRUPT:-0}" == 1 ]]; then
      printf X >> "$dir/Baixa-Facil-v0.1.0-alpha.67-arm64-v8a.apk"
    fi
    ;;
  *) exit 8;;
esac
MOCK
chmod +x "$work/bin/gh"
abis=(armeabi-v7a arm64-v8a x86_64)
for abi in "${abis[@]}"; do printf 'conteudo-exemplo-%s\n' "$abi" > "$work/dist/Baixa-Facil-v0.1.0-alpha.67-${abi}.apk"; done
export PATH="$work/bin:$PATH" MOCK_GH_STATE="$work/github" GH_TOKEN=ficticio
export GITHUB_REPOSITORY=example/test GITHUB_SHA=abc123 RELEASE_MAX_ATTEMPTS=1
export VERSION=0.1.0-alpha.67 APK_DIR="$work/dist"
export GITHUB_STEP_SUMMARY="$work/summary.txt" SIGNING_MODE=debug
(cd "$root" && bash tools/publish_apk.sh) > "$work/first.log"
[[ -f "$work/github/created" ]] && [[ $(find "$work/github" -maxdepth 1 -name '*.apk' | wc -l) -eq 3 ]]
# A Release mostra o tamanho de cada arquitetura e o SHA-256 verificável.
[[ $(grep -c ' MiB' "$work/summary.txt") -eq 3 ]] || { echo 'ERRO: tamanhos dos tres APKs nao foram reportados'; exit 1; }

(cd "$root" && bash tools/publish_apk.sh) > "$work/second.log"
if (cd "$root" && MOCK_CORRUPT=1 bash tools/publish_apk.sh) > "$work/corrupt.log" 2>&1; then
  echo 'ERRO: SHA-256 divergente não foi detectado'; exit 1
fi
grep -q 'SHA-256' "$work/corrupt.log"
printf extra > "$work/dist/extra.apk"
if (cd "$root" && bash tools/publish_apk.sh) > "$work/extra.log" 2>&1; then
  echo 'ERRO: APK extra não foi detectado'; exit 1
fi
rm "$work/dist/extra.apk"
rm "$work/dist/Baixa-Facil-v0.1.0-alpha.67-x86_64.apk"
if (cd "$root" && bash tools/publish_apk.sh) > "$work/missing.log" 2>&1; then
  echo 'ERRO: APK ausente não foi detectado'; exit 1
fi
echo 'PASSOU: criação, atualização, três assets, SHA-256 divergente, APK extra e APK ausente.'
