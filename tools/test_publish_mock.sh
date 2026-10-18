#!/usr/bin/env bash
# Teste de contrato local: não usa GitHub e não publica nenhum APK real.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/bin" "$work/github"
cat > "$work/bin/gh" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
cmd="${1:-}"; sub="${2:-}"; shift 2
state="${MOCK_GH_STATE:?}"
if [[ "$cmd" != release ]]; then exit 7; fi
case "$sub" in
  view)
    [[ -f "$state/created" ]] || exit 1
    if [[ "$*" == *'--json assets'* ]]; then
      [[ -f "$state/asset.apk" ]] && echo 'Baixa-Facil-v0.1.0-alpha.41.apk'
    elif [[ "$*" == *'--json url'* ]]; then
      echo 'https://example.invalid/releases/v0.1.0-alpha.41'
    fi
    ;;
  create|upload)
    file=''
    for arg in "$@"; do [[ "$arg" == *.apk ]] && file="$arg"; done
    [[ -s "$file" ]] || exit 9
    cp "$file" "$state/asset.apk"
    touch "$state/created"
    ;;
  download)
    dir=''
    while (($#)); do
      if [[ "$1" == '--dir' ]]; then dir="$2"; shift 2; else shift; fi
    done
    [[ -f "$state/asset.apk" ]] || exit 8
    cp "$state/asset.apk" "$dir/Baixa-Facil-v0.1.0-alpha.41.apk"
    if [[ "${MOCK_CORRUPT:-0}" == 1 ]]; then printf X >> "$dir/Baixa-Facil-v0.1.0-alpha.41.apk"; fi
    ;;
  *) exit 8;;
esac
MOCK
chmod +x "$work/bin/gh"
printf 'apk-exemplo-da-versao-41\n' > "$work/Baixa-Facil-v0.1.0-alpha.41.apk"
export PATH="$work/bin:$PATH" MOCK_GH_STATE="$work/github" GH_TOKEN=ficticio
export GITHUB_REPOSITORY=example/test GITHUB_SHA=abc123 RELEASE_MAX_ATTEMPTS=1
export VERSION=0.1.0-alpha.41 APK_FILE="$work/Baixa-Facil-v0.1.0-alpha.41.apk"
export GITHUB_STEP_SUMMARY="$work/summary.txt" SIGNING_MODE=debug
(cd "$root" && bash tools/publish_apk.sh) > "$work/first.log"
[[ -f "$work/github/created" ]] && grep -q 'SHA-256' "$work/summary.txt"
(cd "$root" && bash tools/publish_apk.sh) > "$work/second.log"
if (cd "$root" && MOCK_CORRUPT=1 bash tools/publish_apk.sh) > "$work/corrupt.log" 2>&1; then
 echo 'ERRO: hash diferente passou'; exit 1
fi
grep -q 'SHA-256' "$work/corrupt.log"
echo 'PASSOU: nova Release, atualização de Release e falha por SHA-256 divergente.'
