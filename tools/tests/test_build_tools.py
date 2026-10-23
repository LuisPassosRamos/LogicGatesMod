"""Regressions for artifact identity, package rejection and bounded cleanup; no game assets required."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import build_info
import run_server_tests
import verify_project


class BuildToolsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.config = {"version": "1.1.0", "hytaleVersion": "0.7.4"}

    def fixture_sources(self):
        for name in ("src/main/java/Plugin.java", "src/main/resources/manifest.json", "build.gradle.kts",
                     "settings.gradle.kts", "gradle.properties", "tools/build_info.py", "tools/project_config.py"):
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(name, encoding="utf-8")

    def identity(self):
        with patch.object(build_info, "ROOT", self.root), patch.object(build_info, "configuration", return_value=self.config), \
                patch.object(build_info, "git", side_effect=lambda *args: "abc123" if args[0] == "rev-parse" else ""):
            return build_info.identity()

    def test_identity_changes_with_production_sources_but_not_docs_or_tests(self):
        self.fixture_sources()
        before = self.identity()
        (self.root / "README.md").write_text("new documentation", encoding="utf-8")
        (self.root / "src/test").mkdir()
        (self.root / "src/test/Test.java").write_text("test", encoding="utf-8")
        self.assertEqual(before, self.identity())
        (self.root / "src/main/java/Plugin.java").write_text("changed", encoding="utf-8")
        self.assertNotEqual(before["buildId"], self.identity()["buildId"])

    def test_identity_includes_asset_paths_and_build_configuration(self):
        self.fixture_sources()
        before = self.identity()
        (self.root / "src/main/java/Plugin.java").rename(self.root / "src/main/java/Renamed.java")
        renamed = self.identity()
        self.assertNotEqual(before["sourceFingerprint"], renamed["sourceFingerprint"])
        (self.root / "gradle.properties").write_text("changed build", encoding="utf-8")
        self.assertNotEqual(renamed["buildId"], self.identity()["buildId"])

    def make_jar(self, extra=None, fingerprint="a" * 64):
        jar = self.root / "LogicGates-1.1.0.jar"
        info = dict(self.config, revision="abc123", dirty="true", sourceFingerprint=fingerprint,
                    buildId="1.1.0+" + fingerprint[:12])
        with zipfile.ZipFile(jar, "w") as archive:
            archive.writestr("manifest.json", json.dumps({"Version": "1.1.0", "ServerVersion": "=0.7.4"}))
            archive.writestr("logicgates-build.properties", "".join(f"{key}={value}\n" for key, value in info.items()))
            archive.writestr("sisso/logicsim/LogicSimPlugin.class", b"fixture")
            from workshop_material import ITEMS
            for item_id, settings in ITEMS.items():
                icon = f"Icons/LogicGates/{item_id}.png"
                archive.writestr(f"Server/Item/Items/Fixture/{item_id}.json", json.dumps({"Icon": icon, "Recipe": settings["recipe"]}))
                archive.writestr("Common/" + icon, b"fixture-icon")
            archive.writestr("Server/Item/Category/CreativeLibrary/LogicGates.json", "{}")
            for gate in verify_project.CATALOG["gates"]:
                for state in ("On", "Off"):
                    for mask in range(16):
                        if mask & ~gate["allowedMask"] == 0:
                            archive.writestr(f"Common/Items/LogicGates/{gate['folder']}/{gate['itemId']}_{state}_Connections_{mask}.blockymodel", "{}")
            if extra:
                archive.writestr(extra, b"forbidden")
        return jar

    def verify(self, jar):
        with patch.object(verify_project, "configuration", return_value=self.config), \
                patch.object(verify_project, "identity", return_value={"sourceFingerprint": "a" * 64}):
            verify_project.distribution(jar)

    def test_distribution_accepts_identity_and_rejects_authoring_and_placeholder_files(self):
        self.verify(self.make_jar())
        for extra in ("Common/NAND_port.bbmodel", "sisso/logicsim/components/EnergyNodeComponent.class",
                      "sisso/logicsim/integration/ServerIntegrationPlugin.class"):
            with self.subTest(extra=extra), self.assertRaises(ValueError):
                self.verify(self.make_jar(extra=extra))

    def test_distribution_rejects_stale_production_sources(self):
        with self.assertRaisesRegex(ValueError, "stale"):
            self.verify(self.make_jar(fingerprint="b" * 64))

    def test_metadata_and_checksum_identify_packaged_bytes(self):
        jar = self.make_jar()
        output = self.root / "metadata"
        with patch.object(sys, "argv", ["build_info.py", "--jar", str(jar), "--output", str(output)]):
            build_info.main()
        data = json.loads((output / "release.json").read_text())
        checksum = hashlib.sha256(jar.read_bytes()).hexdigest()
        self.assertEqual(checksum, data["sha256"])
        self.assertEqual(jar.stat().st_size, data["sizeBytes"])
        self.assertEqual("1.1.0+aaaaaaaaaaaa", data["buildId"])
        self.assertEqual(f"{checksum}  {jar.name}\n", jar.with_suffix(".jar.sha256").read_text())

    def test_release_rejects_missing_linked_documents_and_wrong_checksums(self):
        jar = self.make_jar()
        content = jar.read_bytes()
        checksum = hashlib.sha256(content).hexdigest()
        package = self.root / "release.zip"
        for missing, wrong in ((False, False), (True, False), (False, True)):
            metadata = {"file": jar.name, "sha256": "wrong" if wrong else checksum, "sizeBytes": len(content)}
            with zipfile.ZipFile(package, "w") as archive:
                archive.writestr(jar.name, content)
                archive.writestr(jar.name + ".sha256", f"{checksum}  {jar.name}\n")
                archive.writestr("release.json", json.dumps(metadata))
                archive.writestr("README.md", "[Documentation](docs/README.md)")
                archive.writestr("CHANGELOG.md", "changelog")
                archive.writestr("docs/README.md", "[Guide](guide.md)")
                if not missing:
                    archive.writestr("docs/guide.md", "guide")
            with self.subTest(missing=missing, wrong=wrong), patch.object(verify_project, "production_jar", return_value=jar):
                if missing or wrong:
                    with self.assertRaises(ValueError):
                        verify_project.release(package)
                else:
                    verify_project.release(package)

    def test_cleanup_refuses_root_and_development_server(self):
        with patch.object(run_server_tests, "ROOT", self.root):
            for path in (self.root, self.root / "devserver-074", self.root / "build/tmp/server-integration"):
                with self.subTest(path=path), self.assertRaises(ValueError):
                    run_server_tests.remove_test_universe(path)

    def test_cleanup_removes_only_the_selected_temporary_universe(self):
        work = self.root / "build/tmp/server-integration/run-one"
        other = work.with_name("run-two")
        work.mkdir(parents=True)
        other.mkdir()
        (work / "world.dat").write_text("test universe")
        with patch.object(run_server_tests, "ROOT", self.root):
            run_server_tests.remove_test_universe(work)
        self.assertFalse(work.exists())
        self.assertTrue(other.is_dir())


if __name__ == "__main__":
    unittest.main()
