# App overview 

This is an app to track personal finances.
* An android app voice records a spending in the form of (date, spending_message), where spending_message begins with an amount like "23 zł fryzjer"
* The app uploads it to a cloudflare worker
* It's then possible to download the data locally to CSV (`./download.sh`), analyze it and draw graphs with tools in the "scripts" directory

# Other

Continuously maintain a specification of the program in SPECS.md. It must contain everything needed for another agent to recreate the program. If needed, also edit `CLAUDE.md`.

Use uv to run scripts.

For D1 and sqlite, use STRICT tables.

