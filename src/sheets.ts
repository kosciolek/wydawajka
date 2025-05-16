import { GoogleAuth } from "google-auth-library";
import { google } from "googleapis";
import { SERVICE_ACCOUNT_CREDENTIALS, SPREADSHEET_ID } from "./env";

const auth = new GoogleAuth({
    credentials: JSON.parse(SERVICE_ACCOUNT_CREDENTIALS!),
    scopes: ["https://www.googleapis.com/auth/spreadsheets"],
});

const sheets = google.sheets({ version: "v4", auth });

export const addTransactionRow = async ({
    date = "",
    amount = "",
    memo = "",
    tags = "",
}: {
    date?: string;
    amount?: string;
    memo?: string;
    tags?: string;
}) =>
    await sheets.spreadsheets.values.append({
        spreadsheetId: SPREADSHEET_ID,
        range: "Main!A:D",
        valueInputOption: "USER_ENTERED",
        requestBody: {
            values: [[date, amount, memo, tags]],
        },
    });
