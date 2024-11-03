#!/bin/bash

process_files() {
    while IFS= read -r filename; do
        # Skip empty lines
        [ -z "$filename" ] && continue

        # Find the full path of the file
        filepath=$(find . -name "$filename" -type f)

        if [ -n "$filepath" ]; then
            echo "Creating gist for $filename from $filepath"
            gh gist create "$filepath" --filename "$filename"
        else
            echo "Warning: Could not find $filename"
        fi
    done
}

# Check if input is being piped in
if [ -p /dev/stdin ]; then
    # Process piped input
    process_files
elif [ "$1" ]; then
    # Process input file from argument
    if [ -f "$1" ]; then
        process_files < "$1"
    else
        echo "Error: File $1 not found"
        exit 1
    fi
else
    echo "Usage: $0 <filename> or pipe input to script"
    echo "Example: $0 file_list"
    echo "Example: cat file_list | $0"
    exit 1
fi