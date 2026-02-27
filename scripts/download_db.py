import csv
import json
import subprocess

result = subprocess.run(
    ["npx", "wrangler", "d1", "execute", "finances", "--command", "SELECT * FROM spendings", "--json", "--remote"],
    capture_output=True, text=True, check=True,
    cwd="../worker",
)
rows = json.loads(result.stdout)[0]["results"]

with open("database.csv", "w", newline="") as f:
    if rows:
        w = csv.DictWriter(f, fieldnames=rows[0].keys())
        w.writeheader()
        w.writerows(rows)

print(f"Downloaded {len(rows)} rows to database.csv")
