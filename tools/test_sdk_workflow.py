#!/usr/bin/env python3
"""Teste offline da etapa de instalação Android SDK no GitHub Actions.

Lê o YAML sem dependências externas e executa o comando com um SDK Manager
simulado. Não instala pacotes e não exige Android SDK disponível.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/android.yml"


def sdk_script():
    lines = WORKFLOW.read_text(encoding="utf-8").splitlines()
    matches = [i for i, line in enumerate(lines) if line.strip() == "- name: Instalar SDK API 37"]
    if len(matches) != 1:
        raise AssertionError("Etapa Instalar SDK API 37 não encontrada exatamente uma vez")
    i = matches[0] + 1
    step = []
    while i < len(lines) and not lines[i].startswith("      - "):
        step.append(lines[i])
        i += 1
    if "        shell: bash" not in step:
        raise AssertionError("Etapa SDK deve fixar shell: bash")
    try:
        first = step.index("        run: |")
    except ValueError as exc:
        raise AssertionError("Etapa SDK exige bloco literal run: | (não YAML dobrado)") from exc
    commands = []
    for line in step[first + 1:]:
        if not line.strip():
            if commands:
                commands.append("")
            continue
        if not line.startswith("          "):
            break
        commands.append(line[10:])
    shell = "\n".join(commands).strip() + "\n"
    assert "sdkmanager --install 'platforms;android-37.0' 'build-tools;36.0.0'" in shell
    assert 'test -s "${ANDROID_HOME}/platforms/android-37.0/android.jar"' in shell
    assert shell.splitlines()[0] == "set -euo pipefail", "Falta ativar tratamento de falha"
    assert len([x for x in shell.splitlines() if x.strip()]) >= 3, "Comandos combinados na mesma linha"
    if subprocess.run(["bash", "-n"], input=shell, text=True, capture_output=True).returncode:
        raise AssertionError("Sintaxe Bash inválida na etapa SDK")
    return shell


def mock_run(shell, create_jar):
    with tempfile.TemporaryDirectory(prefix="bf-sdk-step-") as folder:
        temp = Path(folder)
        bin_dir = temp / "bin"
        bin_dir.mkdir()
        script = bin_dir / "sdkmanager"
        script.write_text("""#!/usr/bin/env bash
set -euo pipefail
printf '%s\\n' "$@" > "$MOCK_ARGS"
[[ "$#" -eq 3 ]] || { echo 'Número de argumentos errado'; exit 59; }
[[ "$1" == '--install' ]] || exit 59
[[ "$2" == 'platforms;android-37.0' ]] || exit 59
[[ "$3" == 'build-tools;36.0.0' ]] || exit 59
if [[ "$MOCK_CREATE_JAR" == 1 ]]; then
  mkdir -p "$ANDROID_HOME/platforms/android-37.0"
  printf 'android-jar-simulado\\n' > "$ANDROID_HOME/platforms/android-37.0/android.jar"
fi
""", encoding="utf-8")
        script.chmod(0o755)
        sdk = temp / "sdk"
        sdk.mkdir()
        args_file = temp / "args"
        env = dict(os.environ,
            PATH=f"{bin_dir}:{os.environ.get('PATH', '')}",
            ANDROID_HOME=str(sdk),
            MOCK_CREATE_JAR="1" if create_jar else "0",
            MOCK_ARGS=str(args_file),
        )
        process = subprocess.run(["bash", "-e", "-o", "pipefail"],
                                 input=shell, text=True, capture_output=True, env=env)
        assert args_file.is_file(), "O sdkmanager não foi executado"
        assert args_file.read_text(encoding="utf-8").splitlines() == [
            "--install", "platforms;android-37.0", "build-tools;36.0.0"
        ], "Parâmetros de instalação incorretos"
        jar = sdk / "platforms" / "android-37.0" / "android.jar"
        if create_jar:
            assert process.returncode == 0 and jar.is_file(), (
                f"Falhou instalação simulada: {process.stderr} {process.stdout}")
        else:
            assert process.returncode != 0 and not jar.exists(), (
                "Etapa não detectou a ausência do android.jar")
            assert "Android SDK Platform 37.0 não instalado" in (process.stdout + process.stderr), (
                "Falha sem diagnóstico claro do Android SDK")


if __name__ == "__main__":
    script = sdk_script()
    print("OK: workflow separa instalação SDK da verificação em run: |")
    mock_run(script, True)
    print("OK: sdkmanager simulado recebe apenas os dois pacotes corretos")
    mock_run(script, False)
    print("OK: SDK incompleto encerra a etapa com erro explícito")
