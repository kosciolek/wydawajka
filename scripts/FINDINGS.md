# Health Insurance (Skladka Zdrowotna) for Ryczalt — Findings

## How the bracket is determined

For ryczalt, health insurance uses the **standard method** (metoda standardowa): cumulative current-year revenue determines the bracket.

- Each year starts at the **low** bracket
- Steps up to **mid** when cumulative revenue crosses 60,000 PLN
- Steps up to **high** when cumulative revenue crosses 300,000 PLN

The thresholds (60k / 300k) are **fixed and absolute** — they are NOT pro-rated for partial years of operation.

There is also a **simplified method** (metoda uproszczona) that locks the bracket for the whole year based on prior-year revenue, but it requires full prior year of operation and is only worth using if it results in a lower bracket.

## 2026 health insurance bases

Derived from GUS Q4 2025 average enterprise wage: **9,228.64 PLN**.

| Bracket       | Revenue threshold | % of avg wage | Base (podstawa) | Monthly (x 9%) |
|---------------|-------------------|---------------|-----------------|-----------------|
| Low           | <= 60,000         | 60%           | 5,537.18        | 498.35          |
| Mid           | 60,001 - 300,000  | 100%          | 9,228.64        | 830.58          |
| High          | > 300,000         | 180%          | 16,611.55       | 1,495.04        |

## 2025 health insurance bases

Derived from GUS Q4 2024 average enterprise wage: **8,549.18 PLN**.

| Bracket       | Revenue threshold | % of avg wage | Base (podstawa) | Monthly (x 9%) |
|---------------|-------------------|---------------|-----------------|-----------------|
| Low           | <= 60,000         | 60%           | 5,129.51        | 461.66          |
| Mid           | 60,001 - 300,000  | 100%          | 8,549.18        | 769.43          |
| High          | > 300,000         | 180%          | 15,388.52       | 1,384.97        |

## Example: 2026 bracket progression

With ~177 PLN/hour and ~168 hours/month (~28,320 PLN/month):

| Months  | Cumulative revenue | Bracket | Health Ins |
|---------|--------------------|---------|------------|
| Jan-Feb | <= 56,640          | Low     | 498.35     |
| Mar-Sep | 60k - 283k        | Mid     | 830.58     |
| Oct-Dec | > 300k             | High    | 1,495.04   |

## Annual settlement

The annual settlement (rozliczenie roczne) reconciles monthly payments against the bracket determined by total annual revenue. If total annual revenue puts you in a higher bracket than some months' payments reflected, you owe the difference. The annual contribution is proportional to months of insurance coverage.

## Sources

- Art. 81 ust. 2e-2f — Ustawa o swiadczeniach opieki zdrowotnej finansowanych ze srodkow publicznych
- https://poradnikprzedsiebiorcy.pl/-wyliczenie-skladki-zdrowotnej-u-ryczaltowca
- https://symfonia.pl/blog/rozwoj-firmy/jdg/skladka-zdrowotna-2025-ryczalt/
