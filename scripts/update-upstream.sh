#!/usr/bin/env bash
# Replaces the decompiled source with a new upstream ATT2_Project release.
#
#   scripts/update-upstream.sh path/to/ATT2_Project-X.Y.Z.jar
#
# Commit the result on its own, then rebase or reapply the rebuild's changes on top.
# Needs bash, curl, python and a Java 21+ `java` on PATH (or JAVA_HOME set).
set -euo pipefail

if [ $# -ne 1 ] || [ ! -f "$1" ]; then
    echo "usage: $0 path/to/ATT2_Project-X.Y.Z.jar" >&2
    exit 1
fi

cd "$(dirname "$0")/.."
VINEFLOWER_VERSION=1.12.0
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

name="$(basename "$1")"
mkdir -p original tools
if [ "$(realpath "$1")" != "$(realpath -m "original/$name")" ]; then
    cp "$1" "original/$name"
fi

# On Git Bash, Java needs Windows paths
winpath() { if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else echo "$1"; fi; }

if [ ! -f tools/vineflower.jar ]; then
    curl -fsSL -o tools/vineflower.jar \
        "https://repo1.maven.org/maven2/org/vineflower/vineflower/$VINEFLOWER_VERSION/vineflower-$VINEFLOWER_VERSION.jar"
fi

# Let Loom remap the release jar (intermediary -> Mojang names)
./gradlew compileJava -PupstreamJar="original/$name" --console=plain -q
remapped="$(find .gradle/loom-cache/remapped_mods -name "${name%.jar}-*.jar" | head -1)"
[ -n "$remapped" ] || { echo "Loom did not produce a remapped jar" >&2; exit 1; }

# Minecraft, Fabric API, Fabric Loader and Mixin as decompiler libraries so generics resolve
libs=()
while IFS= read -r jar; do libs+=("-e=$(winpath "$jar")"); done < <(
    find .gradle/loom-cache -name "minecraft-merged*.jar" ! -name "*sources*"
    find "$HOME/.gradle/caches/fabric-loom" .gradle/loom-cache -path "*remapped*fabric*" -name "*.jar" 2>/dev/null | grep -v ATT2_Project
    find "$HOME/.gradle/caches/modules-2/files-2.1/net.fabricmc/fabric-loader" \
         "$HOME/.gradle/caches/modules-2/files-2.1/net.fabricmc/sponge-mixin" \
         "$HOME/.gradle/caches/modules-2/files-2.1/io.github.llamalad7" -name "*.jar" ! -name "*sources*" 2>/dev/null
)

rm -rf decomp && mkdir decomp
"$JAVA" -Xmx4G -jar "$(winpath tools/vineflower.jar)" -dgs=1 -rsy=1 -rbr=1 -ind="    " "${libs[@]}" \
    "$(winpath "$remapped")" "$(winpath decomp)" > decomp.log 2>&1

if grep -rl '\$VF' decomp --include=*.java; then
    echo "warning: the files above contain methods Vineflower could not decompile" >&2
fi

# Swap in the new source and resources
rm -rf src/main/java src/main/resources
mkdir -p src/main/java src/main/resources
cp -r decomp/fr decomp/com src/main/java/
(cd decomp && cp -r assets resourcepacks META-INF LICENSE_* att2.mixins.json lootbeams.mixins.json fabric.mod.json ../src/main/resources/)
rm -f src/main/resources/META-INF/MANIFEST.MF

python scripts/fix_mixin_casts.py

version="$(python -c "import json; print(json.load(open('src/main/resources/fabric.mod.json', encoding='utf-8'))['version'])")"
sed -i "s/^mod_version=.*/mod_version=$version/" gradle.properties

./gradlew build --console=plain -q
echo "Decompiled $name ($version); build/libs has the unmodified rebuild"
