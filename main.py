from typing import Literal

from working_hours import WORKING_HOURS

# ── Configuration ────────────────────────────────────────────────────────────
USD_PLN = 3.5
HOURLY_RATE_PLN = 100.0
HOURLY_RATE_USD = 22.0
HOURLY_RATE_TOTAL = HOURLY_RATE_PLN + HOURLY_RATE_USD * USD_PLN  # 177.0 PLN

RYCZALT_RATE = 0.12
HEALTH_INSURANCE_RATE = 0.09
HEALTH_INSURANCE_DEDUCTION_RATE = 0.50  # 50% of health insurance deductible from revenue

# ZUS contribution rates (without chorobowe)
PREF_RATE = 0.1952 + 0.08 + 0.0167  # emerytalne + rentowe + wypadkowe = 29.19%
FULL_RATE = 0.1952 + 0.08 + 0.0167 + 0.0245  # + Fundusz Pracy = 31.64%

# ── Yearly parameters ────────────────────────────────────────────────────────
# Each year: (preferential_base, full_base, health_bracket_thresholds, health_bases)
# Health bases: (base_low, base_mid, base_high) corresponding to revenue brackets
# Revenue brackets: <=60k, 60k-300k, >300k

ZUS_PARAMS: dict[int, dict[str, float]] = {
    2025: {
        "pref_base": 1399.80,
        "full_base": 5203.80,
        "health_base_low": 5129.51,    # 60% of 8549.18
        "health_base_mid": 8549.18,    # 100% of 8549.18
        "health_base_high": 15388.52,  # 180% of 8549.18
    },
    2026: {
        "pref_base": 1441.80,   # 30% of 4806 (min wage 2026)
        "full_base": 5652.00,   # 60% of 9420 (est avg wage 2026)
        "health_base_low": 5284.20,    # ~3% growth
        "health_base_mid": 8807.06,
        "health_base_high": 15852.70,
    },
    2027: {
        "pref_base": 1485.05,   # ~3% growth
        "full_base": 5821.56,
        "health_base_low": 5442.73,
        "health_base_mid": 9071.27,
        "health_base_high": 16328.28,
    },
    2028: {
        "pref_base": 1529.60,   # ~3% growth
        "full_base": 5996.21,
        "health_base_low": 5606.01,
        "health_base_mid": 9343.41,
        "health_base_high": 16818.13,
    },
    2029: {
        "pref_base": 1575.49,   # ~3% growth
        "full_base": 6176.10,
        "health_base_low": 5774.19,
        "health_base_mid": 9623.71,
        "health_base_high": 17322.67,
    },
}

# Revenue thresholds for health insurance brackets
HEALTH_BRACKET_LOW = 60_000.0
HEALTH_BRACKET_HIGH = 300_000.0

# ── ZUS timeline ─────────────────────────────────────────────────────────────
# JDG established October 2024, ulga na start used:
#   Oct 2024 – Mar 2025: ulga na start (health only, no social)
#   Apr 2025 – Mar 2027: preferential ZUS
#   Apr 2027 onwards:    full ZUS

ZusPhase = Literal["preferential", "full"]


def get_zus_phase(year: int, month: int) -> ZusPhase:
    if (year, month) <= (2027, 3):
        return "preferential"
    return "full"


def get_working_hours(year: int, month: int) -> int:
    return WORKING_HOURS[(year, month)]


def get_zus_social(year: int, phase: ZusPhase) -> float:
    params = ZUS_PARAMS[year]
    if phase == "preferential":
        return params["pref_base"] * PREF_RATE
    return params["full_base"] * FULL_RATE


def get_health_insurance(year: int, annual_revenue: float) -> float:
    params = ZUS_PARAMS[year]
    if annual_revenue <= HEALTH_BRACKET_LOW:
        base = params["health_base_low"]
    elif annual_revenue <= HEALTH_BRACKET_HIGH:
        base = params["health_base_mid"]
    else:
        base = params["health_base_high"]
    return base * HEALTH_INSURANCE_RATE


def calculate_ryczalt(revenue: float, zus_social: float, health_insurance: float) -> float:
    deductible_health = health_insurance * HEALTH_INSURANCE_DEDUCTION_RATE
    taxable = revenue - zus_social - deductible_health
    if taxable < 0:
        taxable = 0.0
    return taxable * RYCZALT_RATE


def main() -> None:
    start_year, start_month = 2026, 2
    end_year, end_month = 2029, 12

    # Pass 1: compute full-year revenues to determine health insurance brackets
    # (always use all 12 months, regardless of display range)
    annual_revenues: dict[int, float] = {}
    for year in range(start_year, end_year + 1):
        total = 0.0
        for month in range(1, 13):
            hours = get_working_hours(year, month)
            total += hours * HOURLY_RATE_TOTAL
        annual_revenues[year] = total

    # Pass 2: print monthly breakdown
    header = (
        f"{'Month':<10} {'Hours':>5} {'Revenue':>12} {'ZUS Social':>12} "
        f"{'Health Ins':>12} {'Ryczałt':>12} {'Net':>12}"
    )
    separator = "-" * len(header)

    print(header)
    print(separator)

    totals = {
        "hours": 0,
        "revenue": 0.0,
        "zus_social": 0.0,
        "health": 0.0,
        "tax": 0.0,
        "net": 0.0,
    }

    current_year = start_year
    for year in range(start_year, end_year + 1):
        m_start = start_month if year == start_year else 1
        m_end = end_month if year == end_year else 12

        if year != current_year:
            # Print yearly subtotal separator
            current_year = year

        for month in range(m_start, m_end + 1):
            hours = get_working_hours(year, month)
            revenue = hours * HOURLY_RATE_TOTAL
            phase = get_zus_phase(year, month)
            zus_social = get_zus_social(year, phase)
            health = get_health_insurance(year, annual_revenues[year])
            tax = calculate_ryczalt(revenue, zus_social, health)
            net = revenue - zus_social - health - tax

            label = f"{year}-{month:02d}"
            print(
                f"{label:<10} {hours:>5} {revenue:>12,.2f} {zus_social:>12,.2f} "
                f"{health:>12,.2f} {tax:>12,.2f} {net:>12,.2f}"
            )

            totals["hours"] += hours
            totals["revenue"] += revenue
            totals["zus_social"] += zus_social
            totals["health"] += health
            totals["tax"] += tax
            totals["net"] += net

        # Year subtotal
        print(separator)

    # Grand totals
    print(
        f"{'TOTAL':<10} {totals['hours']:>5} {totals['revenue']:>12,.2f} "
        f"{totals['zus_social']:>12,.2f} {totals['health']:>12,.2f} "
        f"{totals['tax']:>12,.2f} {totals['net']:>12,.2f}"
    )


if __name__ == "__main__":
    main()
