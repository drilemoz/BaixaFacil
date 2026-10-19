#!/usr/bin/env python3
"""Copy the four Gradle-generated ABI APKs using output-metadata.json.

Avoid assuming Gradle's output file capitalization or order. Refuse universal,
missing, stale, or incorrectly filtered APKs; validate their native libraries.
"""
import argparse
import json
import shutil
import sys
import zipfile
from pathlib import Path

ABIS = ('armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64')


def collect(root: Path, variant: str, version: str, code: int, output: Path) -> list[Path]:
    src_dir = root / 'app' / 'build' / 'outputs' / 'apk' / variant
    metadata_path = src_dir / 'output-metadata.json'
    metadata = json.loads(metadata_path.read_text(encoding='utf-8'))
    if metadata.get('variantName') != variant or metadata.get('applicationId') != 'br.com.baixafacil':
        raise ValueError('Variante ou applicationId não corresponde ao Baixa Fácil')
    elements = metadata.get('elements', [])
    by_abi = {}
    for e in elements:
        filters = e.get('filters') or []
        if len(filters) != 1 or filters[0].get('filterType') != 'ABI':
            raise ValueError('APK universal ou filtro inesperado na saída do Gradle')
        abi = filters[0].get('value')
        if abi not in ABIS or abi in by_abi:
            raise ValueError(f'ABI desconhecida ou repetida: {abi}')
        if e.get('versionCode') != code or e.get('versionName') != version:
            raise ValueError(f'Versão incorreta no metadata do APK {abi}')
        name = e.get('outputFile', '')
        if not name.endswith('.apk') or '/' in name or '\\' in name:
            raise ValueError(f'Nome inválido no metadata: {name}')
        by_abi[abi] = src_dir / name
    if set(by_abi) != set(ABIS):
        raise ValueError(f'Esperados {ABIS}, recebidos {tuple(by_abi)}')

    # Validate *all* inputs before touching the destination.
    for abi, path in by_abi.items():
        if not path.is_file() or path.stat().st_size == 0:
            raise ValueError(f'APK ausente ou vazio: {path}')
        with zipfile.ZipFile(path) as archive:
            libs = {n.split('/')[1] for n in archive.namelist()
                    if n.startswith('lib/') and n.endswith('.so') and len(n.split('/')) >= 3}
            if libs != {abi}:
                raise ValueError(f'Bibliotecas nativas de {abi} inválidas: {libs}')
    output.mkdir(parents=True, exist_ok=True)
    if list(output.glob('*.apk')):
        raise ValueError('A pasta de destino não pode conter APKs antigos')
    result = []
    for abi in ABIS:
        dst = output / f'Baixa-Facil-v{version}-{abi}.apk'
        shutil.copy2(by_abi[abi], dst)
        result.append(dst)
        print(f'ABI {abi}: {dst.name} ({dst.stat().st_size:,} bytes)')
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--variant', choices=('debug', 'release'), required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--code', type=int, required=True)
    parser.add_argument('--output', type=Path, default=Path('dist'))
    args = parser.parse_args()
    try:
        collect(args.root, args.variant, args.version, args.code, args.output)
    except (ValueError, OSError, KeyError, zipfile.BadZipFile) as exc:
        print(f'::error::Separação por ABI: {exc}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
