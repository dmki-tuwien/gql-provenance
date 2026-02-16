import os
import json
import pandas as pd
import sys

# Path to your JSON configuration
CONFIG_FILE = "headers.json"

if len(sys.argv) != 2:
    print("Usage: python update_headers.py <scale-factor>")
    sys.exit(1)

scale_factor = f"sf{sys.argv[1]}"  # e.g., "0.1" or "1"
print(f"For scale factor: {scale_factor}")

# Root folder containing nodes/edges folders
DATA_ROOT = f"/home/user/data/{scale_factor}/"
subfolders=["snapshot/"]

# Load the JSON config
with open(CONFIG_FILE, "r") as f:
    config = json.load(f)

def process_csv(file_path, folder_config, keep_header=True):
    print(f"Processing {file_path}")

    df = pd.read_csv(file_path, sep="|")
    # Update headers according to the config
    for update in folder_config.get("header-update", []):
        current_col = update["current"]
        new_col = update["new"]
        if current_col in df.columns:
            df = df.rename(columns={current_col: new_col})

            if ":boolean" in new_col:
                df[new_col] = df[new_col].astype(str).str.strip().str.lower()

            if ":long" in new_col and "lastLoginTime" not in new_col:
                df[new_col] = pd.to_datetime(df[new_col], format='mixed')

                # 2. Convert datetime to milliseconds since epoch
                df[new_col] = (df[new_col].astype('int64') // 10**6)

    # Add :LABEL or :TYPE column if needed
    if folder_config["type"] == "node":
        df[":LABEL"] = folder_config["label"]
    elif folder_config["type"] == "relationship":
        df[":TYPE"] = folder_config["label"]

    if keep_header:
        df.to_csv(file_path, sep="|", index=False)
    else:
        ## If there are multiple files for the same entity, only the first file should have the header
        df.to_csv(file_path, sep="|", index=False, header=False)

    print(f"Processed {file_path}")

def deprocess_csv(file_path, folder_config, keep_header=True):

    print(f"Processing {file_path}")

    df = pd.read_csv(file_path, sep="|")
    # Update headers according to the config
    for update in folder_config.get("header-update", []):
        current_col = update["current"]
        new_col = update["new"]
        if current_col in df.columns:
            df = df.rename(columns={current_col: new_col})

            if ":boolean" in new_col:
                df[new_col] = df[new_col].astype(str).str.strip().str.lower()

    # Add :LABEL or :TYPE column if needed
    if folder_config["type"] == "node":
        df[":LABEL"] = folder_config["label"]
    elif folder_config["type"] == "relationship":
        df[":TYPE"] = folder_config["label"]

    if keep_header:
        df.to_csv(file_path, sep="|", index=False)
    else:
        ## If there are multiple files for the same entity, only the first file should have the header
        df.to_csv(file_path, sep="|", index=False, header=False)

    print(f"Processed {file_path}")

# Loop through each folder in the JSON
for folder_name, folder_config in config.items():
    for subfolder in subfolders:

        # folder_path = os.path.join(DATA_ROOT, subfolder)
        file_name = os.path.join(DATA_ROOT, subfolder, folder_name+".csv")

        # if os.path.exists(folder_path) and os.path.isdir(folder_path):
        #     # Iterate through files in the folder
        #     for filename in os.listdir(folder_path):
        #         if folder_name in filename:
        #             matching_file = os.path.join(folder_path, filename)
        #             process_csv(matching_file, folder_config, keep_header=(0 == 0))
        #             break  # stop at the first match

        if not os.path.exists(file_name):
            print(f"File not found: {file_name}")
            continue

        # folder_path = os.path.join(DATA_ROOT, subfolder, folder_name)
        #
        # if not os.path.exists(folder_path):
        #     print(f"Folder not found: {folder_path}")
        #     continue

        # csv_files = [f for f in os.listdir(folder_path) if f.endswith(".csv")]
        # csv_files.sort()  # Ensure consistent order

        # Keep header for first file, remove for others
        # for i, file_name in enumerate(csv_files):
        # file_path = os.path.join(folder_path, file_name)
        process_csv(file_name, folder_config, keep_header=(0 == 0))

