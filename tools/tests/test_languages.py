"""Language regression checks use a temporary project and never edit real translations."""
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import verify_languages


class LanguagesTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)
        for locale in ('en-US','pt-BR'):
            path=self.root/'src/main/resources/Server/Languages'/locale/'server.lang'
            path.parent.mkdir(parents=True)
            path.write_text('logicgates.test = Value {value}\n',encoding='utf-8')
        self.target=self.root/'src/main/resources/Server/Languages/pt-BR/server.lang'

    def test_matching_translations_are_accepted(self):
        self.target.write_text('logicgates.test = Valor {value}\n',encoding='utf-8')
        self.assertEqual(1,verify_languages.check(self.root))

    def test_missing_unknown_duplicate_empty_and_wrong_parameters_are_rejected(self):
        for text in ('', 'other = unknown\n', 'logicgates.test = a\nlogicgates.test = b\n',
                     'logicgates.test = \n', 'logicgates.test = Valor {wrong}\n'):
            with self.subTest(text=text):
                self.target.write_text(text,encoding='utf-8')
                with self.assertRaises(ValueError): verify_languages.check(self.root)

    def test_java_literal_keys_must_exist(self):
        source=self.root/'src/main/java/Example.java'
        source.parent.mkdir(parents=True)
        source.write_text('text("missing");',encoding='utf-8')
        with self.assertRaisesRegex(ValueError,'Missing Java translation'): verify_languages.check(self.root)
