import { Autocomplete, TextField } from "@mui/material";
import { useLayoutEffect } from "@tanstack/react-router";
import { useState, useEffect } from "react";

const LOCAL_STORAGE_KEY = "my_tags";

export default function TagsMultiSelect({
  value,
  onChange,
  availableTags,
}: {
  value: string[];
  onChange: (value: string[]) => void;
  availableTags?: string[];
}) {
  return (
    <Autocomplete
      multiple
      freeSolo
      options={availableTags}
      value={value}
      onChange={(_, newValue) => {
        onChange(newValue);
      }}
      renderInput={(params) => (
        <TextField {...params} label="Tags" placeholder="Add or select tags" />
      )}
    />
  );
}
