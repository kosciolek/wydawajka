# Working hours per month (business days × 8h, Mon–Fri, excluding Polish public holidays).
# Holidays: New Year, Epiphany, Easter Sunday & Monday, 1 May, 3 May,
#           Pentecost, Corpus Christi, 15 Aug, 1 Nov, 11 Nov, 25–26 Dec.
# Key: (year, month), Value: hours
WORKING_HOURS: dict[tuple[int, int], int] = {
    (2026,  1): 160,
    (2026,  2): 160,
    (2026,  3): 176,
    (2026,  4): 168,  # Easter Monday 6 Apr
    (2026,  5): 160,  # 1 May
    (2026,  6): 168,  # Corpus Christi 4 Jun
    (2026,  7): 184,
    (2026,  8): 168,
    (2026,  9): 176,
    (2026, 10): 176,
    (2026, 11): 160,  # 11 Nov
    (2026, 12): 176,  # 25 Dec
    (2027,  1): 152,  # New Year + Epiphany
    (2027,  2): 160,
    (2027,  3): 176,  # Easter Monday 29 Mar
    (2027,  4): 176,
    (2027,  5): 152,  # 3 May + Corpus Christi 27 May
    (2027,  6): 176,
    (2027,  7): 176,
    (2027,  8): 176,
    (2027,  9): 176,
    (2027, 10): 168,
    (2027, 11): 160,  # 1 Nov + 11 Nov
    (2027, 12): 184,
    (2028,  1): 160,  # Epiphany 6 Jan
    (2028,  2): 168,
    (2028,  3): 184,
    (2028,  4): 152,  # Easter Monday 17 Apr
    (2028,  5): 168,  # 1 May + 3 May
    (2028,  6): 168,  # Corpus Christi 15 Jun
    (2028,  7): 168,
    (2028,  8): 176,  # 15 Aug
    (2028,  9): 168,
    (2028, 10): 176,
    (2028, 11): 168,  # 1 Nov
    (2028, 12): 152,  # 25–26 Dec
    (2029,  1): 176,  # 1 Jan
    (2029,  2): 160,
    (2029,  3): 176,
    (2029,  4): 160,  # Easter Monday 2 Apr
    (2029,  5): 160,  # 1 May + 3 May + Corpus Christi 31 May
    (2029,  6): 168,
    (2029,  7): 176,
    (2029,  8): 176,  # 15 Aug
    (2029,  9): 160,
    (2029, 10): 184,
    (2029, 11): 168,  # 1 Nov + 11 Nov (Sun, no effect)
    (2029, 12): 152,  # 25–26 Dec
}
