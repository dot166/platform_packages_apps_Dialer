#!/bin/bash

# Configuration
PROJECT_DIR="./"
MAPPING_FILE="./androidx_material_mapping.csv"

# File types to process (Java, Kotlin, XML)
FILE_PATTERNS=(-name "*.java" -o -name "*.kt" -o -name "*.xml")

# Check for gsed on macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    SED_CMD="gsed"
    if ! command -v gsed &> /dev/null; then
        echo "Warning: 'gsed' (GNU sed) not found. Install with 'brew install gnu-sed'."
        echo "Attempting with 'sed', but replacements may fail."
        SED_CMD="sed"
    fi
else
    SED_CMD="sed"
fi

echo "Starting AndroidX/Material migration for files in $PROJECT_DIR..."
echo "Using mapping file: $MAPPING_FILE"
echo "---"

# Build replacement rules into an array
REPLACE_COMMAND=()
while IFS=, read -r old_class new_class; do
    [[ "$old_class" =~ ^#.* ]] || [[ -z "$old_class" ]] && continue
    ESCAPED_OLD=$(echo "$old_class" | $SED_CMD -e 's/[\/.]/\\&/g')
    ESCAPED_NEW=$(echo "$new_class" | $SED_CMD -e 's/[\/.]/\\&/g')
    REPLACE_COMMAND+=("-e" "s/${ESCAPED_OLD}/${ESCAPED_NEW}/g")
done < "$MAPPING_FILE"

# Track changed files
changed_files=()

# Find and process files one by one
while IFS= read -r -d '' file; do
    before_hash=$(md5sum "$file" | awk '{print $1}')
    $SED_CMD -i "${REPLACE_COMMAND[@]}" "$file"
    after_hash=$(md5sum "$file" | awk '{print $1}')

    if [[ "$before_hash" != "$after_hash" ]]; then
        changed_files+=("$file")
        echo "✔ Updated: $file"
    fi
done < <(find "$PROJECT_DIR" \( "${FILE_PATTERNS[@]}" \) -type f \
    -not -path '*/.git*' \
    -not -path '*/.repo*' \
    -not -path '*/out/*' \
    -not -path '*/build/*' \
    -print0)

echo "---"
if [ ${#changed_files[@]} -eq 0 ]; then
    echo "No files were modified."
else
    echo "Migration finished. ${#changed_files[@]} file(s) updated."
fi
# Post-check: look for leftover android.support.* that wasn’t converted
echo "---"
echo "Scanning for unconverted android.support.* references..."
LEFTOVERS=$(grep -R --include="*.java" --include="*.kt" --include="*.xml" "android.support." "$PROJECT_DIR" \
    | grep -v '\.bak')

if [ -n "$LEFTOVERS" ]; then
    echo "Found possible unconverted references:"
    echo "$LEFTOVERS"
    echo "Check your mapping file and add missing entries."
else
    echo "No unconverted android.support.* references found."
fi
