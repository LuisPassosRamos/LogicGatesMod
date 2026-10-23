"""Run create/restart integration in a disposable universe, preserving reports and artifact identity."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import uuid
import zipfile

from project_config import ROOT, configuration, production_jar


def remove_test_universe(work):
    """Never clean a configured server or a path outside this runner's temporary root."""
    base = (ROOT / "build/tmp/server-integration").resolve()
    target = work.resolve()
    if work.is_symlink() or target == base or not target.is_relative_to(base):
        raise ValueError(f"Refusing to remove unexpected test universe: {work}")
    shutil.rmtree(target)


def validate_test_plugin(path, mod_version, hytale_version):
    """Reject a stale integration plugin before booting a server that cannot load it."""
    with zipfile.ZipFile(path) as archive:
        manifest = json.loads(archive.read("manifest.json"))
    if (manifest.get("Version") != mod_version
            or manifest.get("ServerVersion") != "=" + hytale_version
            or manifest.get("Dependencies", {}).get("sisso:LogicGates") != "=" + mod_version):
        raise ValueError("Integration plugin version is stale; run gradlew serverTestJar again.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--production-jar", type=Path, default=production_jar())
    parser.add_argument("--server-jar", type=Path)
    parser.add_argument("--assets", type=Path)
    parser.add_argument("--timeout", type=int, default=180, help="Seconds per server process")
    parser.add_argument("--keep-world", action="store_true", help="Keep the successful temporary universe")
    args = parser.parse_args()
    config = configuration()
    target_version = config["hytaleVersion"]
    server = args.server_jar
    if server is None:
        cache = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
        candidates = sorted((cache / f"caches/modules-2/files-2.1/com.hypixel.hytale/Server/{target_version}")
                            .glob(f"*/Server-{target_version}.jar"))
        if not candidates:
            raise SystemExit(f"Resolve the {target_version} dependency with gradlew build first.")
        server = candidates[0]
    assets = args.assets
    if assets is None:
        if os.environ.get("HYTALE_ASSETS"):
            assets = Path(os.environ["HYTALE_ASSETS"])
        elif os.environ.get("APPDATA"):
            assets = Path(os.environ["APPDATA"]) / "Hytale/install/release/package/game/latest/Assets.zip"
        else:
            raise SystemExit("Pass --assets /absolute/path/to/Assets.zip or set HYTALE_ASSETS.")
    server, assets, production = server.resolve(), assets.resolve(), args.production_jar.resolve()
    test_mod = ROOT / "build/server-tests/logicgates-integration-tests.jar"
    for path in (server, assets, production, test_mod):
        if not path.is_file():
            raise SystemExit(f"Missing {path}; run gradlew build serverTestJar.")
    try:
        validate_test_plugin(test_mod, config["version"], target_version)
    except ValueError as error:
        raise SystemExit(str(error)) from error
    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java")) if java_home else shutil.which("java")
    if not java:
        raise SystemExit("Java 25 is required; set JAVA_HOME or add java to PATH.")
    version = subprocess.run([java, "-jar", str(server), "--version"],
                             capture_output=True, text=True, check=True).stdout
    if f"v{target_version}" not in version:
        raise SystemExit(f"The supplied server JAR is not Hytale {target_version}.")
    with zipfile.ZipFile(production) as archive:
        identity = dict(line.split("=", 1) for line in archive.read("logicgates-build.properties").decode().splitlines())
    if identity["version"] != config["version"] or identity["hytaleVersion"] != target_version:
        raise SystemExit("Build identity differs from the project manifest; run gradlew build again.")
    stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S") + "-" + uuid.uuid4().hex[:6]
    work = ROOT / "build/tmp/server-integration" / stamp
    reports = ROOT / "build/reports/server-integration" / stamp
    reports.mkdir(parents=True)
    mods = work / "mods"
    mods.mkdir(parents=True)
    shutil.copy2(production, mods / production.name)
    shutil.copy2(test_mod, mods / "LogicGatesIntegrationTests.jar")
    summary = {"build": identity, "sha256": hashlib.sha256(production.read_bytes()).hexdigest(),
               "hytaleVersion": target_version, "passed": False, "phases": []}
    print(f"Testing {identity['buildId']}; isolated universe: {work}", flush=True)
    try:
        for phase in ("create", "reload"):
            log = reports / f"{phase}.log"
            command = [java, "-Xmx2G", "-jar", str(server),
                       "--assets", str(assets), "--auth-mode", "offline",
                       "--bind", "127.0.0.1:5534", "--disable-sentry",
                       "--disable-file-watcher", "--disable-cpb-build",
                       "--boot-command", f"logic-selftest {phase}"]
            print(f"Running {phase}...", flush=True)
            with log.open("w", encoding="utf-8") as output:
                try:
                    completed = subprocess.run(command, cwd=work, stdout=output,
                                               stderr=subprocess.STDOUT, timeout=args.timeout)
                except subprocess.TimeoutExpired as error:
                    raise RuntimeError(f"Server test timed out; see {log}") from error
            result_file = work / f"selftest-{phase}.json"
            if not result_file.exists():
                raise RuntimeError(f"No test result (exit {completed.returncode}); see {log}")
            shutil.copy2(result_file, reports / result_file.name)
            result = json.loads(result_file.read_text(encoding="utf-8"))
            summary["phases"].append(result)
            if completed.returncode or not result["passed"]:
                raise RuntimeError(f"Server test failed; see {log}")
            print(f"PASS: {phase}", flush=True)
        summary["passed"] = True
    except (RuntimeError, OSError, ValueError, KeyError) as error:
        summary["error"] = str(error)
        raise SystemExit(f"{error}\nTemporary universe retained at {work}") from error
    finally:
        (reports / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    if not args.keep_world:
        remove_test_universe(work)
    print(f"PASS: create + real restart. Reports: {reports}", flush=True)


if __name__ == "__main__":
    main()
