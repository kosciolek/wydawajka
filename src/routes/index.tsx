import {
  createFileRoute,
  useLayoutEffect,
  useRouter,
} from "@tanstack/react-router";
import { createServerFn, useServerFn } from "@tanstack/react-start";
import { Button } from "@mui/material";
import { Stack, TextField } from "@mui/material";
import { useState } from "react";
import { z } from "zod";
import { addTransactionRow } from "../sheets";

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
    await addTransactionRow({
      date: data.date,
      amount: data.amount,
      memo: data.memo,
      tags: data.tags,
    });
  });

export const Route = createFileRoute("/")({
  component: Home,
  // loader: async () => await getCount(),
});

function Home() {
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

  const addTransactionFn = useServerFn(addTransaction);

  return (
    <Stack gap={2} p={2}>
      <form
        style={{ display: "contents" }}
        onSubmit={(e) => {
          e.preventDefault();
          const date = new Date().toISOString().split("T")[0];
          addTransactionFn({ data: { token, amount, memo, tags, date } }).catch(
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
