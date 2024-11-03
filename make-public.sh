#!/bin/bash

# Function to create a temporary directory and clean it up on exit
setup_temp_dir() {
    temp_dir=$(mktemp -d)
    trap 'rm -rf "$temp_dir"' EXIT
}

# Function to download a gist's content
download_gist_content() {
    local gist_id="$1"
    local filename="$2"
    local output_file="$temp_dir/$filename"
    
    gh gist view "$gist_id" --filename "$filename" > "$output_file"
    echo "$output_file"
}

# Function to create public gist and return success/failure
create_public_gist() {
    local content_file="$1"
    local filename="$2"
    local desc="$3"
    
    if gh gist create "$content_file" --public --filename "$filename" > /dev/null; then
        return 0
    else
        return 1
    fi
}

# Set up temporary directory
setup_temp_dir

# Get all private gists and process them
echo "Fetching private gists..."
gh gist list --limit 100 --secret | while read -r gist_id filename visibility desc; do
    echo "Processing $filename..."
    
    # Download content
    content_file=$(download_gist_content "$gist_id" "$filename")
    
    if [ -f "$content_file" ]; then
        echo "Creating new public gist for $filename..."
        
        if create_public_gist "$content_file" "$filename" "$desc"; then
            echo "Successfully created public gist for $filename"
            echo "Deleting original private gist $gist_id..."
            
            if gh gist delete "$gist_id"; then
                echo "Successfully deleted original private gist"
            else
                echo "Failed to delete original gist $gist_id"
            fi
        else
            echo "Failed to create public gist for $filename, keeping original"
        fi
    else
        echo "Failed to download content for $filename"
    fi
done

echo "Done processing gists!"