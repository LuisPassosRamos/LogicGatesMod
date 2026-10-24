"""Targeted Custom UI syntax regressions; actual rendering still requires the client."""
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_custom_ui import check_document


class CustomUiTest(unittest.TestCase):
    def test_rc5_search_and_next_order_are_rejected(self):
        for body in ('FlexWeight: 1; @Anchor = (Right: 8); MaxLength: 128;',
                     'FlexWeight: 1; @Text = %server.logicgates.classroom.next;'):
            with self.subTest(body=body), self.assertRaisesRegex(ValueError, 'must precede properties/children'):
                check_document('$C.@Control #Control { ' + body + ' }', 'Example.ui')

    def test_native_parameter_first_order_and_nested_blocks_are_accepted(self):
        check_document('$C = "../../Common.ui";\n'
                       '$C.@Container { Anchor: (Width: 960); #Content { LayoutMode: Top;\n'
                       '$C.@TextField #Search { @Anchor = (Right: 8); FlexWeight: 1; MaxLength: 128; }\n'
                       '$C.@SecondaryTextButton #Next { @Text = %server.logicgates.classroom.next; FlexWeight: 1; }\n'
                       '} }', 'Example.ui')

    def test_comments_literals_and_parameter_value_expressions_do_not_confuse_scopes(self):
        check_document('@Button = TextButton {\n'
                       '@Anchor /* comment: { ; } */ = (Width: 120, Right: 8);\n'
                       '@Text = "escaped \\\" ; } // @Anchor = ignored";\n'
                       'Style: (Default: (Background: "{;@Text = ignored}"));\n'
                       '// @Text = "ignored";\n'
                       'Text: @Text;\n'
                       '}', 'Example.ui')

    def test_multiline_diagnostic_and_child_content_are_checked(self):
        with self.assertRaisesRegex(ValueError, r'Example.ui:3:3: Custom UI parameter @Text'):
            check_document('Group {\n  Label { Text: "child"; }\n  @Text = "late";\n}', 'Example.ui')


if __name__ == '__main__':
    unittest.main()
