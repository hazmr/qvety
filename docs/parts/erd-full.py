#!/usr/bin/env python3
"""Build erd-full.md (one Mermaid diagram) from erd.md. Run after every erd.md change."""
import re, pathlib
src = pathlib.Path(__file__).with_name("erd.md").read_text()
blocks = re.findall(r"```mermaid\nerDiagram\n(.*?)```", src, re.S)
overview, details = blocks[0], blocks[1:]
entities = []
for b in details:
    entities += re.findall(r"^    \w+ \{\n.*?^    \}\n", b, re.S | re.M)
rel_re = re.compile(r'^\s*(\w+) (\S+) (\w+) : "(.*)"$')
seen, rels = {}, []
for b in [overview] + details:
    for l in b.splitlines():
        m = rel_re.match(l)
        if not m:
            continue
        a, card, c, label = m.groups()
        key = (a, card, c, label) if label else (a, card, c)
        # a labeled edge from a detail section wins over an unlabeled overview edge with the same ends
        if key in seen:
            continue
        if not label and any(k[:3] == (a, card, c) for k in seen):
            continue
        seen[key] = True
        rels.append(f'    {a} {card} {c} : "{label}"')
tables = len(entities)
out = f"""# Qvety — Full ERD (single diagram)

Generated from `erd.md` by `python3 erd-full.py`. Do not edit by hand; edit `erd.md` and regenerate.

All {tables} tables, all columns, all relationships in one diagram. In VS Code use Markdown Preview Mermaid Support and zoom; for a poster paste the block into <https://mermaid.live> and export SVG.

Legend: `PK` primary key, `FK` foreign key, `UK` unique. Platform-zone tables have no `practice_id`; every other table has one, an RLS policy, an audit trigger, and an entry in the practice export. `||--o{{` one-to-many, `||--o|` one-to-zero-or-one, `o|--o{{` optional-one-to-many.

```mermaid
erDiagram
{''.join(entities)}
{chr(10).join(rels)}
```
"""
pathlib.Path(__file__).with_name("erd-full.md").write_text(out)
print(f"erd-full.md: {tables} tables, {len(rels)} relationships")
