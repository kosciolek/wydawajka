import * as fs from "node:fs";
import {
  createFileRoute,
  useLayoutEffect,
  useRouter,
} from "@tanstack/react-router";
import { createServerFn } from "@tanstack/react-start";
import { Button } from "@mui/material";
import { Stack, TextField } from "@mui/material";
import { useEffect, useState } from "react";
import { z } from "zod";
import { JWT } from "google-auth-library";
import { sheets_v4 } from "@googleapis/sheets";
import {
  GOOGLE_PRIVATE_KEY,
  GOOGLE_SERVICE_ACCOUNT_EMAIL,
  SPREADSHEET_ID,
} from "../env";

const addTransaction = createServerFn({ method: "POST" })
  .validator(
    (data: {
      token: string;
      amount: string;
      memo: string;
      tags: string;
      date: string;
    }) =>
      z
        .object({
          token: z.string(),
          date: z.string(),
          amount: z.string(),
          memo: z.string(),
          tags: z.string(),
        })
        .parse(data)
  )
  .handler(async ({ data }) => {
    if (data.token !== process.env.TOKEN) {
      throw new Error("Invalid token");
    }
    const auth = new JWT({
      email: GOOGLE_SERVICE_ACCOUNT_EMAIL,
      key: GOOGLE_PRIVATE_KEY,
      scopes: ["https://www.googleapis.com/auth/spreadsheets"],
    });

    const sheets = new sheets_v4.Sheets({
      auth,
    });

    const response = await sheets.spreadsheets.values.append({
      spreadsheetId: SPREADSHEET_ID,
      range: "Main!A:D",
      valueInputOption: "USER_ENTERED",
      requestBody: {
        values: [[data.date, data.amount, data.memo, data.tags]],
      },
    });
  });

export const Route = createFileRoute("/")({
  component: Home,
  // loader: async () => await getCount(),
});

function Home() {
  const router = useRouter();
  // const state = Route.useLoaderData();

  const [amount, setAmount] = useState("");
  const [memo, setMemo] = useState("");
  const [tags, setTags] = useState("");
  const [token, setToken] = useState("");

  useLayoutEffect(() => {
    setToken(localStorage.getItem("token") || "");
  }, []);

  const onTokenChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setToken(e.target.value);
    localStorage.setItem("token", e.target.value);
  };

  return (
    <Stack gap={2} p={2}>
      <form
        style={{ display: "contents" }}
        onSubmit={(e) => {
          e.preventDefault();
          const date = new Date().toISOString().split("T")[0];
          addTransaction({ data: { token, amount, memo, tags, date } }).catch(
            (e) => alert(e)
          );
        }}
      >
        <TextField
          label="Token"
          value={token}
          onChange={onTokenChange}
          required
          type="password"
        />
        <TextField
          label="Amount"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          type="number"
          required
        />
        <TextField
          label="Memo"
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
        />
        <TextField
          label="Tags"
          value={tags}
          onChange={(e) => setTags(e.target.value)}
        />
        <Button type="submit">Add</Button>
      </form>
    </Stack>
  );
}
