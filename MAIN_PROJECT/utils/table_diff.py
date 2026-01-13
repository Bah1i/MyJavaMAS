def diff_tables(df1, df2):
    diffs = []
    for col in df1.columns:
        if col not in df2.columns:
            diffs.append(f"Column removed: {col}")

    for col in df2.columns:
        if col not in df1.columns:
            diffs.append(f"Column added: {col}")

    return diffs
