from __future__ import annotations

import argparse
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

import matplotlib.pyplot as plt
import pandas as pd


def load_results(path: Path, algorithm: str) -> pd.DataFrame:
    if not path.exists():
        raise FileNotFoundError(f"Results file not found: {path}")
    df = pd.read_csv(path)
    expected_cols = {"source", "target", "distance", "time_ns", "relaxed"}
    if set(df.columns) != expected_cols:
        raise ValueError(f"Unexpected columns in {path}: {df.columns.tolist()}")
    df = df.copy()
    df["algorithm"] = algorithm
    df["time_ms"] = df["time_ns"] / 1e6
    return df


def summarise(df: pd.DataFrame) -> Dict[str, float]:
    return {
        "Mean time (ms)": df["time_ms"].mean(),
        "Median time (ms)": df["time_ms"].median(),
        "Mean relaxed": df["relaxed"].mean(),
        "Median relaxed": df["relaxed"].median(),
    }


def build_latex_table(rows: Iterable[Tuple[str, Dict[str, float]]]) -> str:
    lines: List[str] = [
        "\\begin{tabular}{lrrrr}",
        "\\toprule",
        "Algorithm & Mean time (ms) & Median time (ms) & Mean relaxed & Median relaxed \\\\",
        "\\midrule",
    ]
    for algorithm, metrics in rows:
        lines.append(
            f"{algorithm} & "
            f"{metrics['Mean time (ms)']:.2f} & "
            f"{metrics['Median time (ms)']:.2f} & "
            f"{metrics['Mean relaxed']:.0f} & "
            f"{metrics['Median relaxed']:.0f} \\\\"
        )
    lines.extend(["\\bottomrule", "\\end{tabular}"])
    return "\n".join(lines)


def make_plot(df: pd.DataFrame, output_path: Path) -> None:
    fig, ax = plt.subplots(figsize=(6, 4))
    data = [df[df["algorithm"] == algo]["time_ms"] for algo in df["algorithm"].unique()]
    ax.boxplot(data, labels=df["algorithm"].unique(), showfliers=False)
    ax.set_ylabel("Query time (ms)")
    ax.set_title("Query runtime comparison")
    ax.set_yscale("log")
    ax.grid(axis="y", linestyle="--", linewidth=0.5, alpha=0.7)
    fig.tight_layout()
    fig.savefig(output_path, dpi=200)
    plt.close(fig)


def compute_speedup(dijkstra_df: pd.DataFrame, bidirectional_df: pd.DataFrame) -> float:
    merged = dijkstra_df.merge(
        bidirectional_df,
        on=["source", "target"],
        suffixes=("_dijkstra", "_bidirectional"),
    )
    ratio = merged["time_ms_dijkstra"] / merged["time_ms_bidirectional"]
    return ratio.mean()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Compare Dijkstra variants and generate summary artefacts."
    )
    parser.add_argument(
        "--dijkstra",
        type=Path,
        default=Path("results/dijkstra_results.csv"),
        help="Path to CSV with standard Dijkstra results.",
    )
    parser.add_argument(
        "--bidirectional",
        type=Path,
        default=Path("results/bidirectional_results.csv"),
        help="Path to CSV with bidirectional Dijkstra results.",
    )
    parser.add_argument(
        "--table",
        type=Path,
        default=Path("results/comparison_table.tex"),
        help="Where to write the LaTeX table.",
    )
    parser.add_argument(
        "--plot",
        type=Path,
        default=Path("results/runtime_comparison.png"),
        help="Where to write the PNG plot.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    dijkstra_df = load_results(args.dijkstra, "Dijkstra")
    bidirectional_df = load_results(args.bidirectional, "Bidirectional")

    combined = pd.concat([dijkstra_df, bidirectional_df], ignore_index=True)

    summaries = [
        ("Dijkstra", summarise(dijkstra_df)),
        ("Bidirectional", summarise(bidirectional_df)),
    ]

    args.table.parent.mkdir(parents=True, exist_ok=True)
    args.plot.parent.mkdir(parents=True, exist_ok=True)

    args.table.write_text(build_latex_table(summaries))
    make_plot(combined, args.plot)

    speedup = compute_speedup(dijkstra_df, bidirectional_df)
    print(f"Average speed-up (Dijkstra / Bidirectional): {speedup:.2f}x")
    print(f"LaTeX table written to: {args.table}")
    print(f"Runtime plot written to: {args.plot}")


if __name__ == "__main__":
    main()
