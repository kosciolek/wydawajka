import { Autocomplete, TextField } from "@mui/material";
import { useLayoutEffect } from "@tanstack/react-router";
import { useState, useEffect } from "react";

const LOCAL_STORAGE_KEY = "my_tags";

export default function TagsMultiSelect({
  value,
  onChange,
}: {
  value: string[];
  onChange: (value: string[]) => void;
  label?: string;
}) {
  const [options, setOptions] = useState<string[]>(() => {
    if (typeof window === "undefined") {
      return [];
    }
    const stored = localStorage.getItem(LOCAL_STORAGE_KEY);
    return stored ? JSON.parse(stored) : ["example", "react", "mui"];
  });

  useLayoutEffect(() => {
    const stored = localStorage.getItem(LOCAL_STORAGE_KEY);
    if (stored) {
      setOptions(JSON.parse(stored));
    }
  }, []);

  useEffect(() => {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(options));
  }, [options]);

  return (
    <Autocomplete
      multiple
      freeSolo
      options={options}
      value={value}
      onChange={(_, newValue) => {
        // Add any new tags to options
        const newTags = newValue.filter((tag) => !options.includes(tag));
        if (newTags.length > 0) {
          setOptions((prev) => [...prev, ...newTags]);
        }
        onChange(newValue);
      }}
      renderInput={(params) => (
        <TextField {...params} label="Tags" placeholder="Add or select tags" />
      )}
    />
  );
}
