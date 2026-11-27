"""Builds companion.json for a release: one entry per jar in the dist/ folder.

File name: phase-companion-<loader>-<code line>-<mod version>.jar. The code line is the
adapter folder (<loader>/<code line>); one jar serves every game version listed in that
folder's gradle.properties as minecraft_versions (for example 1.21,1.21.1).
Phase reads this manifest and installs the mod for the host by the host's game version,
checking the size and BLAKE3.
"""
import json
import re
import sys
from pathlib import Path

import blake3

ROOT = Path(__file__).resolve().parent.parent


def game_versions(loader: str, line: str) -> list[str]:
    """Game versions of one adapter: minecraft_versions, or just the code line."""
    props = ROOT / loader / line / "gradle.properties"
    for raw in props.read_text(encoding="utf-8").splitlines():
        key, _, value = raw.partition("=")
        if key.strip() == "minecraft_versions":
            versions = [v.strip() for v in value.split(",") if v.strip()]
            if line not in versions:
                sys.exit(f"{props}: minecraft_versions must include {line}")
            return versions
    return [line]


repo, tag, dist = sys.argv[1], sys.argv[2], Path(sys.argv[3])
pattern = re.compile(r"^phase-companion-(fabric|forge|neoforge)-(.+)-(\d+\.\d+\.\d+)\.jar$")
mods = []
for jar in sorted(dist.glob("*.jar")):
    match = pattern.match(jar.name)
    if not match:
        sys.exit(f"unexpected file name: {jar.name}")
    loader, line, version = match.groups()
    data = jar.read_bytes()
    mods.append({
        "loader": loader,
        "mc": game_versions(loader, line),
        "version": version,
        "file": jar.name,
        "url": f"https://github.com/{repo}/releases/download/{tag}/{jar.name}",
        "size": len(data),
        "blake3": blake3.blake3(data).hexdigest(),
    })
(dist / "companion.json").write_text(
    json.dumps({"schema": 1, "mods": mods}, indent=2, ensure_ascii=False) + "\n",
    encoding="utf-8",
)
print(f"companion.json: {len(mods)} jar(s)")
