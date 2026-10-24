"""Check declaration order in Custom UI; this is not the native client markup parser."""
import re
from pathlib import Path


# Preserve token offsets for useful diagnostics; comments and quoted text are opaque.
TOKENS = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|@[A-Za-z_]\w*|[^\s]', re.DOTALL)
PARAMETER = re.compile(r'@[A-Za-z_]\w*\Z')


def check_document(text, name):
    """Require @parameter = declarations before properties/children in each block.

    Hytale 0.7.4 consumes leading declarations before element content. For example,
    its PointInspectorPage uses { @Anchor = (Right: 4); FlexWeight: 1; }.
    This targeted guard catches the rc.5 failure without claiming to validate all UI.
    """
    tokens = [(match.group(), match.start()) for match in TOKENS.finditer(text)
              if not match.group().startswith(('//', '/*'))]
    blocks = []
    for index, (token, offset) in enumerate(tokens):
        if token == '{':
            blocks.append(dict(content=False, start=True, depth=0))
            continue
        if token == '}':
            if blocks:
                blocks.pop()
            if blocks and blocks[-1]['depth'] == 0:
                blocks[-1]['start'] = True
            continue
        if not blocks:
            continue  # Imports and exported declarations are module scope.
        block = blocks[-1]
        if token == ';' and block['depth'] == 0:
            block['start'] = True
            continue
        if block['start'] and block['depth'] == 0:
            block['start'] = False
            declaration = PARAMETER.fullmatch(token) and index + 1 < len(tokens) and tokens[index + 1][0] == '='
            if declaration and block['content']:
                line = text.count('\n', 0, offset) + 1
                column = offset - text.rfind('\n', 0, offset)
                raise ValueError(f'{name}:{line}:{column}: Custom UI parameter {token} must precede properties/children')
            if not declaration:
                block['content'] = True
        if token in ('(', '['):
            block['depth'] += 1
        elif token in (')', ']'):
            block['depth'] -= 1


def check(root):
    documents = sorted((Path(root) / 'src/main/resources/Common/UI/Custom').rglob('*.ui'))
    for document in documents:
        check_document(document.read_text(encoding='utf-8-sig'), document.relative_to(root).as_posix())
    return len(documents)
