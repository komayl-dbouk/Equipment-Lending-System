# Third-party materials for the backend source candidate

The selected candidate is a **Git source export**, with project documentation/slides and the Maven Wrapper. It does not redistribute the application JAR, Java runtime, container image, PostgreSQL image or downloaded Maven dependencies. Project licensing is unchanged. This scope is chosen for an inspectable coursework handoff; the instructor's official channel/format remains to be confirmed by the submission owner.

## Exact inventory and preserved bytes

[Inventory](../../third-party-licenses/inventory.json) identifies 71 actual `BOOT-INF/lib` artifacts from the verified application JAR and the one redistributed Maven Wrapper artifact. Each entry records exact artifact coordinates, SHA-256, Maven Central URL, resolved POM license declarations where available, original embedded legal-material paths and material checksums. The inventory also records the application build revision/JAR hash and canonical Git-blob hashes of all `src/` and POM build inputs.

Original license/NOTICE/copyright text is copied byte-for-byte under [third-party-licenses](../../third-party-licenses/README.md). Runtime entries are reference material: some upstream JARs do not contain a standalone license/NOTICE file, and some declarations are inherited or absent from locally available POMs. These facts are explicit per entry. No missing license is invented, no upstream notice is rewritten and no binary-distribution or legal approval is claimed. Original legal years are part of the upstream text and are preserved.

The source candidate does redistribute `.mvn/wrapper/maven-wrapper.jar` version 3.3.4. Its actual checksum is matched to the exact Maven artifact, and its original `META-INF/LICENSE` and `META-INF/NOTICE` are included and checked before export. The candidate builder rejects a changed wrapper, missing/changed notice material or stale application build-input inventory.

## Reproduce the inventory

From the selected application revision with Java 17 and the existing Maven cache:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
python tools/release/collect_notices.py --jar target/equipment-lending-system-0.0.1-SNAPSHOT.jar --revision <explicit-application-commit> --output target/release/rebuilt-notices
```

The collector is offline: it reads the build JAR and local resolved Maven artifacts/POMs. It refuses an existing destination and verifies selected build inputs using Git's normal line-ending filters. It does not edit the Maven cache. When Maven bootstrap/plugin-injected artifacts are absent from that cache, download their exact versioned Maven Central JAR/POM into a task-owned Maven-layout directory and pass `--supplemental-repository <directory>`. Match the actual JAR checksum; never substitute a version guess. The original execution used that option for Boot jarmode tools 3.5.16 and Maven Wrapper 3.3.4.

Compare the rebuilt inventory and material hashes with the committed inventory. A freshly built application JAR may have different archive timestamps; retain its own new hash/source rather than relabeling the old run. Regenerate and review the inventory whenever source/POM dependencies change. Build tooling/materials from unrelated projects must not be substituted.

## Binary or image distribution is separate

If a later handoff distributes a built JAR or image, the release owner must finish the component-specific review, including missing standalone upstream text, applicable source-availability requirements, any modifications, and JRE/OS/database image materials. The present source candidate does not contain those binaries and is not evidence that those additional duties have been completed. This preserves ELS-46's original engineering-review limits instead of converting a reference inventory into legal clearance.

Use the [submission checklist](../../reports/release/submission-checklist.md) for the explicit revision, inspected source archive, manifest and checksum workflow. None of this publishes a release or submits to the instructor.
