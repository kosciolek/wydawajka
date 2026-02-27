# JDG Earnings & Tax Calculator

## What This Script Does

`main.py` calculates and prints a monthly financial breakdown for a Polish JDG (sole proprietorship) on **ryczałt ewidencjonowany at 12%**, covering **February 2026 through December 2029**.

For each month it displays:
- **Hours** — actual business days × 8h (Mon–Fri, using Python's `calendar` module)
- **Revenue** — hours × hourly rate (100 PLN + 22 USD × exchange rate)
- **ZUS Social** — social insurance contributions (emerytalne, rentowe, wypadkowe, and Fundusz Pracy when applicable)
- **Health Insurance** — składka zdrowotna based on annual revenue bracket (9% of a base determined by GUS average wage)
- **Ryczałt Tax** — 12% of (revenue − ZUS social − 50% of health insurance)
- **Net** — revenue minus all deductions

A totals row is printed at the bottom.

## Key Assumptions & Parameters

| Parameter | Value | Notes |
|-----------|-------|-------|
| USD/PLN exchange rate | 3.5 | Hardcoded constant `USD_PLN` at top of file |
| Hourly rate | 100 PLN + 22 USD | = 177 PLN/hour at 3.5 rate |
| Tax regime | Ryczałt 12% | Flat rate on revenue |
| JDG established | October 2024 | |
| Ulga na start | Used | Oct 2024 – Mar 2025 (outside script range) |
| Preferential ZUS | Apr 2025 – Mar 2027 | 30% of minimum wage as base |
| Full ZUS | Apr 2027 onwards | 60% of forecast average wage as base |
| Chorobowe (sickness) | Not paid | Voluntary, opted out |
| Polish holidays | Not accounted for | Only weekends excluded |

## ZUS Rate Projections

- **2025–2026**: Based on actual announced rates (minimum wage 4,666/4,806 PLN, average wage forecast 8,673/9,420 PLN)
- **2027–2029**: Projected at ~3% annual growth from 2026 values
- All yearly parameters are in the `ZUS_PARAMS` dictionary — easy to update when official rates are announced

## Health Insurance Brackets (Ryczałt)

Annual revenue determines the bracket:
- ≤ 60,000 PLN → base = 60% of GUS Q4 average wage
- 60,001 – 300,000 PLN → base = 100% of GUS Q4 average wage
- > 300,000 PLN → base = 180% of GUS Q4 average wage

Rate is always 9%. At the current hourly rate, annual revenue is ~360k PLN (highest bracket).

## Tax Calculation

```
taxable_base = revenue - zus_social - (50% × health_insurance)
ryczałt_tax  = 12% × taxable_base
```

50% of health insurance is deductible from the tax base (not from tax itself).

## Project Setup

- **Python 3.12**, managed with `uv`
- **Type checker**: `ty` (dev dependency)
- **No runtime dependencies** — only stdlib (`calendar`, `typing`)

```bash
uv run python main.py        # Run the script
uv run ty check main.py      # Type check
```

## Future Improvements

- Fetch live USD/PLN exchange rate from an API (e.g., NBP API) instead of hardcoded constant
- Account for Polish public holidays (currently only weekends are excluded)
- Accept parameters via CLI arguments (hours override, custom date range)
- Add yearly subtotal rows in the output
- Update ZUS_PARAMS with official rates as they are announced for 2027+
