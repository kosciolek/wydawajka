export const SPREADSHEET_ID = process.env.SPREADSHEET_ID!;
if (!SPREADSHEET_ID) {
    throw new Error("SPREADSHEET_ID is not set");
}

export const SERVICE_ACCOUNT_CREDENTIALS = process.env
    .SERVICE_ACCOUNT_CREDENTIALS!;
if (!SERVICE_ACCOUNT_CREDENTIALS) {
    throw new Error("SERVICE_ACCOUNT_CREDENTIALS is not set");
}

export const TOKEN = process.env.TOKEN!;
if (!TOKEN) {
    throw new Error("TOKEN is not set");
}
