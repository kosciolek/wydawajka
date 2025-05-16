import {
  createFileRoute,
  useLayoutEffect,
  useRouter,
} from "@tanstack/react-router";
import { createServerFn, useServerFn } from "@tanstack/react-start";
import {
  Autocomplete,
  Button,
  Checkbox,
  FormControlLabel,
} from "@mui/material";
import { Stack, TextField } from "@mui/material";
import { useState } from "react";
import { z } from "zod";
import { addTransactionRow } from "../sheets";
import TagsMultiSelect from "../tags-select";
import { TOKEN } from "../env";
import { useLocalStorage } from "usehooks-ts";

const addTransaction = createServerFn({ method: "POST" })
  .validator(
    (data: {
      token: string;
      amount: string;
      memo: string;
      tags: string[];
      date: string;
    }) =>
      z
        .object({
          token: z.string(),
          date: z.string(),
          amount: z.string(),
          memo: z.string(),
          tags: z.array(z.string()),
        })
        .parse(data)
  )
  .handler(async ({ data }) => {
    if (data.token !== TOKEN) {
      throw new Error("Invalid token");
    }
    await addTransactionRow({
      date: data.date,
      amount: data.amount,
      memo: data.memo,
      tags: data.tags.join(", "),
    });
    console.log("Transaction added");
  });

export const Route = createFileRoute("/")({
  component: Home,
  // loader: async () => await getCount(),
});

function Home() {
  const [amount, setAmount] = useState("");
  const [memo, setMemo] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [token, setToken] = useLocalStorage("token", "");
  const [shouldShowAlert, setShouldShowAlert] = useLocalStorage(
    "shouldShowAlert",
    true
  );
  const addTransactionFn = useServerFn(addTransaction);

  const [isLoading, setIsLoading] = useState(false);

  const [availableTags, setAvailableTags] = useLocalStorage<string[]>(
    "availableTags",
    []
  );

  return (
    <Stack gap={2} p={2}>
      <form
        style={{ display: "contents" }}
        onSubmit={(e) => {
          e.preventDefault();
          setIsLoading(true);
          const date = new Date().toISOString().split("T")[0];
          addTransactionFn({ data: { token, amount, memo, tags, date } })
            .then(() => {
              if (shouldShowAlert) {
                alert("Transaction added");
              }
              setAvailableTags([...new Set([...availableTags, ...tags])]);
            })
            .catch((e) => alert(e))
            .finally(() => {
              setIsLoading(false);
            });
        }}
      >
        <TextField
          label="Token"
          value={token}
          onChange={(e) => setToken(e.target.value)}
          required
          type="password"
        />
        <TextField
          label="Amount"
          value={amount}
          onChange={(e) => setAmount(e.target.value.replace(/\,/g, "."))}
          type="number"
          required
        />
        <TextField
          label="Memo"
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
        />
        <Autocomplete
          multiple
          freeSolo
          options={availableTags}
          value={tags}
          onChange={(_, newValue: string[]) => {
            setTags(newValue);
          }}
          renderInput={(params) => (
            <TextField
              {...params}
              autocapitalize="off"
              label="Tags"
              placeholder="Add or select tags"
            />
          )}
        />

        <Button type="submit" loading={isLoading}>
          Add
        </Button>
        <FormControlLabel
          control={<Checkbox checked={shouldShowAlert} />}
          label="Show alert"
          onChange={(e) => setShouldShowAlert(e.target.checked)}
        />
        <Button type="button" onClick={() => setAvailableTags([])} size="small">
          Clear tags
        </Button>
      </form>
    </Stack>
  );
}
