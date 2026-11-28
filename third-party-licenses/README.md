# Preserved third-party materials

`inventory.json` is the authoritative index of original artifact materials, artifact/version URLs and SHA-256 values. All legal text in the artifact directories is unmodified upstream content. Reproduce it with [the notice collector](../tools/release/collect_notices.py) and [assembly instructions](../docs/deployment/third-party-notices.md).

The coursework candidate redistributes source/documentation and the Maven Wrapper. Wrapper license and NOTICE bytes are mandatory and validated by the export helper. The 71 application runtime-library entries are reference inventory; their binaries are not included in the candidate. Entries lacking standalone embedded text remain explicit, and no complete binary/image license compliance or legal approval is claimed.
