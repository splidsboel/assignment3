from __future__ import annotations

import argparse
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

import matplotlib.pyplot as plt
import pandas as pd


def load_results(path: Path, label: str) -> pd.DataFrame:
    if not path.exists():
        raise FileNotFoundError(f"Results file not found: {path}")
    df = pd.read_csv(path)
    expected_cols = {"source", "target", "distance", "time_ns", "relaxed"}
    if set(df.columns) != expected_cols:
        raise ValueError(f"Unexpected columns in {path}: {df.columns.tolist()}")
    df = df.copy()
    df["algorithm"] = label
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

def make_scatterplot(
    first_df: pd.DataFrame,
    second_df: pd.DataFrame,
    first_label: str,
    second_label: str,
    output_path: Path,
) -> None:
    fig, ax = plt.subplots(figsize=(6, 4))
    ax.scatter(
        first_df["distance"],
        first_df["relaxed"],
        color="tab:blue",
        label=first_label,
        alpha=0.6,
        s=5,
    )
    ax.scatter(
        second_df["distance"],
        second_df["relaxed"],
        color="tab:orange",
        label=second_label,
        alpha=0.6,
        s=5,
    )
    ax.set_xlabel("Distance")
    ax.set_ylabel("# of relaxed edges")
    ax.set_title("Distance vs relaxed edges")
    ax.legend(title="Algorithm")
    ax.set_yscale("log")
    ax.grid(True, linestyle="--", linewidth=0.5, alpha=0.7)
    fig.tight_layout()

    fig.savefig(output_path, dpi=600)
    plt.close(fig)

def compute_speedup(first_df: pd.DataFrame, second_df: pd.DataFrame) -> float:
    merged = first_df.merge(
        second_df,
        on=["source", "target"],
        suffixes=("_first", "_second"),
    )
    ratio = merged["time_ms_first"] / merged["time_ms_second"]
    return ratio.mean()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Compare Dijkstra variants and generate summary artefacts."
    )
    parser.add_argument(
        "--first",
        type=Path,
        default=Path("results/dijkstra_results.csv"),
        help="Path to CSV for the first algorithm (e.g. regular Dijkstra).",
    )
    parser.add_argument(
        "--first-label",
        default="Algorithm A",
        help="Label for the first dataset in plots/tables.",
    )
    parser.add_argument(
        "--second",
        type=Path,
        default=Path("results/bidirectional_results.csv"),
        help="Path to CSV for the second algorithm (e.g. bidirectional).",
    )
    parser.add_argument(
        "--second-label",
        default="Algorithm B",
        help="Label for the second dataset in plots/tables.",
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
    parser.add_argument(
        "--scatter",
        type=Path,
        default = Path("results/scatter_plot.png")
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    first_df = load_results(args.first, args.first_label)
    second_df = load_results(args.second, args.second_label)

    combined = pd.concat([first_df, second_df], ignore_index=True)

    summaries = [
        (args.first_label, summarise(first_df)),
        (args.second_label, summarise(second_df)),
    ]

    args.table.parent.mkdir(parents=True, exist_ok=True)
    args.plot.parent.mkdir(parents=True, exist_ok=True)

    args.table.write_text(build_latex_table(summaries))
    make_plot(combined, args.plot)
    make_scatterplot(
        first_df,
        second_df,
        args.first_label,
        args.second_label,
        args.scatter,
    )

    speedup = compute_speedup(first_df, second_df)
    print(
        "Average speed-up ({first} / {second}): {ratio:.2f}x".format(
            first=args.first_label,
            second=args.second_label,
            ratio=speedup,
        )
    )
    print(f"LaTeX table written to: {args.table}")
    print(f"Runtime plot written to: {args.plot}")


if __name__ == "__main__":
    main()
