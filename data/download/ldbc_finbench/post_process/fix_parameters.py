import pandas as pd
from glob import glob
import os

base_path = "/home/user/params"

# find all files starting with 'params'
files = glob(os.path.join(base_path, "params*"))

# ensure output folder exists
fixed_path = os.path.join(base_path, "fixed")
os.makedirs(fixed_path, exist_ok=True)

for filename in files:
    file = pd.read_csv(filename, delimiter="|")

    print("Processing:", filename)
    print(file.columns)

    if "startTime" in file.columns:
        file["startTime"] = 0
    else:
        print("startTime column missing")

    if "endTime" in file.columns:
        file["endTime"] = 99999999999999
    else:
        print("endTime column missing")

    output_file = os.path.join(fixed_path, os.path.basename(filename))
    file.to_csv(output_file, sep="|", index=False)