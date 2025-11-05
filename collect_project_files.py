import os
from pathlib import Path
# Import pyperclip if you have it, to keep clipboard functionality
try:
    import pyperclip
except ImportError:
    pyperclip = None

# --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
# CONFIGURATION: Edit these variables
# --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

# 1. Add the relative paths for all files you want to include.
#    Paths must be relative to this script's location and use
#    forward slashes (e.g., 'app/src/main/MyFile.kt').
#
#    I have pre-filled this list based on your old configuration.
#    YOU MUST UPDATE THESE PATHS to match your project structure.
FILES_TO_INCLUDE = [
    # --- Project Root Files ---
    'build.gradle.kts',
    'gradle.properties',
    'settings.gradle.kts',

    'app/.gitignore',
    'app/build.gradle.kts',

    'app/src/main/AndroidManifest.xml',
    
    # --- Source Code (UPDATE THESE PATHS) ---

    # app/src/main/java/com/example/bugsgame/
    'app/src/main/java/com/example/bugsgame/AuthorsFragment.kt',
    'app/src/main/java/com/example/bugsgame/GameFragment.kt',
    'app/src/main/java/com/example/bugsgame/MainActivity.kt',
    'app/src/main/java/com/example/bugsgame/RegistrationFragment.kt',
    'app/src/main/java/com/example/bugsgame/RulesFragment.kt',
    'app/src/main/java/com/example/bugsgame/ScoresFragment.kt',
    'app/src/main/java/com/example/bugsgame/SettingsFragment.kt',
    'app/src/main/java/com/example/bugsgame/ViewPagerAdapter.kt',
    
    # app/src/main/java/com/example/bugsgame/data/
    'app/src/main/java/com/example/bugsgame/data/AppDao.kt',
    'app/src/main/java/com/example/bugsgame/data/AppDatabase.kt',
    'app/src/main/java/com/example/bugsgame/data/DatabaseProvider.kt',
    'app/src/main/java/com/example/bugsgame/data/Player.kt',

    # app/src/main/java/com/example/bugsgame/widget/
    'app/src/main/java/com/example/bugsgame/widget/CbrApiService.kt',
    'app/src/main/java/com/example/bugsgame/widget/GoldRateWidgetProvider.kt',

    # app/src/main/res/drawable
    'app/src/main/res/drawable/widget_background_ball.xml',

    # app/src/main/res/layout/
    'app/src/main/res/layout/activity_main.xml',
    'app/src/main/res/layout/fragment_authors.xml',
    'app/src/main/res/layout/fragment_game.xml',
    'app/src/main/res/layout/fragment_registration.xml',
    'app/src/main/res/layout/fragment_rules.xml',
    'app/src/main/res/layout/fragment_scores.xml',
    'app/src/main/res/layout/fragment_settings.xml',
    'app/src/main/res/layout/item_author.xml',
    'app/src/main/res/layout/item_score.xml',
    'app/src/main/res/layout/widget_gold_rate.xml',

    # app/src/main/res/raw/
    'app/src/main/res/raw/rules.html',

    # app/src/main/res/values/
    'app/src/main/res/values/colors.xml',
    'app/src/main/res/values/strings.xml',
    'app/src/main/res/values/themes.xml',

    # app/src/main/res/values-night/
    'app/src/main/res/values-night/themes.xml',

    # app/src/main/res/xml/
    'app/src/main/res/xml/backup_rules.xml',
    'app/src/main/res/xml/data_extraction_rules.xml',
    'app/src/main/res/xml/gold_rate_widget_info.xml',
    'app/src/main/res/xml/network_security_config.xml',
]

# 2. This will be the name of the combined output file.
OUTPUT_FILENAME = "project_contents.txt"

# --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

def collect_files():
    """
    Reads a specific list of files and concatenates their contents.
    """
    try:
        # Get the directory where the script is located
        script_dir = Path(__file__).parent.resolve()
    except NameError:
        # Fallback for interactive environments (like notebooks)
        script_dir = Path.cwd().resolve()
        
    output_filepath = script_dir / OUTPUT_FILENAME
    all_contents = []
    
    # --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    # NEW: Read 'prompt-introduction.txt' first
    # --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    prompt_intro_file = script_dir / "prompt-introduction.txt"
    print(f"Checking for introduction file: {prompt_intro_file.name}...")
    
    if prompt_intro_file.exists() and prompt_intro_file.is_file():
        try:
            with open(prompt_intro_file, 'r', encoding='utf-8', errors='ignore') as f:
                intro_content = f.read()
            all_contents.append(intro_content)
            # Add the same separator that's used between project files
            all_contents.append("\n" + "="*80 + "\n")
            print(f" + Added introduction from {prompt_intro_file.name}")
        except Exception as e:
            print(f" - ERROR: Could not read {prompt_intro_file.name}: {e}")
    else:
        print(f" - Info: 'prompt-introduction.txt' not found, skipping.")
    # --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---
    # End of new section
    # --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

    print(f"\nScript starting in: {script_dir}") # Added newline for spacing
    print(f"Output file will be: {OUTPUT_FILENAME}")
    print("Processing project files...")

    if not FILES_TO_INCLUDE:
        print("Warning: The 'FILES_TO_INCLUDE' list is empty. No project files to process.")
        # We might still have the intro content, so we continue
    
    # --- Loop through the explicit file list ---
    for relative_path_str in FILES_TO_INCLUDE:
        # Standardize path separators
        relative_path = Path(relative_path_str.replace("\\", "/"))
        full_file_path = script_dir / relative_path

        # Skip the output file itself
        if full_file_path == output_filepath:
            print(f"  - Skipping output file: {relative_path_str}")
            continue
            
        # Skip the prompt intro file since we already processed it
        if full_file_path == prompt_intro_file:
            print(f"  - Skipping intro file (already processed): {relative_path_str}")
            continue

        # Check if file exists before trying to read it
        if not full_file_path.exists():
            print(f"  - WARNING: File not found, skipping: {relative_path_str}")
            continue
            
        if not full_file_path.is_file():
            print(f"  - WARNING: Is a directory, not a file, skipping: {relative_path_str}")
            continue

        # --- Read and append file content ---
        try:
            # Open file, ignoring non-text files
            with open(full_file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            
            # Add to our collection, using the original string for the header
            all_contents.append(f"{relative_path_str}:")
            all_contents.append(content)
            all_contents.append("\n" + "="*80 + "\n")
            
            print(f"  + Added: {relative_path_str}")

        except Exception as e:
            print(f"  - ERROR: Could not read {relative_path_str}: {e}")
            
    # --- Write the collected contents to the output file ---
    if not all_contents:
        print("\nNo content (intro or project files) was processed. Output file not created.")
        return

    try:
        with open(output_filepath, 'w', encoding='utf-8') as f:
            f.write("\n".join(all_contents))
            
        print(f"\n{'-'*30}\n")
        print(f"✅ Success! File created at:")
        print(f"{output_filepath}")

        # Try to copy the path to the clipboard
        if pyperclip:
            pyperclip.copy(str(output_filepath))
            print("\n(The file's full path has been copied to your clipboard!)")
        else:
            print("\n(Install 'pyperclip' to copy the path automatically: pip install pyperclip)")

    except Exception as e:
        print(f"\n❌ Error writing output file: {e}")


if __name__ == "__main__":
    collect_files()