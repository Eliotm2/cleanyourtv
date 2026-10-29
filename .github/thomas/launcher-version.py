#!/usr/bin/env python3
"""Fork (Thomas): after merging an upstream launcher release, set launcher/app/build.gradle to
<upstream versionName>-thomas.1 with a versionCode above both sides.

The only conflict allowed in that file is the version block. Anything else: exit 1.
Usage: launcher-version.py <upstream versionName, e.g. 3.2>
"""
import re
import sys

base = sys.argv[1]
path = "launcher/app/build.gradle"
text = open(path, encoding="utf-8").read()
code_re = re.compile(r"versionCode\s+(\d+)")
allowed = re.compile(r"\s*(versionCode\s|versionName\s|//)")

blocks = list(re.finditer(r"<<<<<<< [^\n]*\n(.*?)=======\n(.*?)>>>>>>> [^\n]*\n", text, re.S))
if len(blocks) > 1:
    sys.exit("more than one conflict in " + path)
if blocks:
    m = blocks[0]
    for line in (m.group(1) + m.group(2)).splitlines():
        if line.strip() and not allowed.match(line):
            sys.exit("a conflict outside the version lines in " + path + ": " + line.strip())
    codes = [int(c) for c in code_re.findall(m.group(0))]
    start, end = m.start(), m.end()
else:
    found = re.search(r"(?:[ \t]*//[^\n]*\n)?[ \t]*versionCode\s+\d+\n[ \t]*versionName\s+'[^']*'\n", text)
    if not found:
        sys.exit("no version block in " + path)
    codes = [int(c) for c in code_re.findall(found.group(0))]
    start, end = found.start(), found.end()

code = max(codes) + 1
name = f"{base}-thomas.1"
new = ("        // Fork (Thomas): above upstream's code so the phone app offers the update; <upstream>-thomas.N names the fork.\n"
       f"        versionCode {code}\n        versionName '{name}'\n")
open(path, "w", encoding="utf-8").write(text[:start] + new + text[end:])
print(f"{name} ({code})")
