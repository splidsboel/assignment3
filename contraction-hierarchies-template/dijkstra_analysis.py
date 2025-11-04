from __future__ import annotations

import argparse
import csv
import subprocess
import sys
from pathlib import Path
from typing import Iterable, List, Sequence, Tuple

from input import input_generator

TIMEOUT = 600


def build_java_input(graph_path: Path, pairs: Iterable[Tuple[int, int]]) -> str:
    graph_data = graph_path.read_text().rstrip("\n")
    pair_list = list(pairs)
    lines = [graph_data, str(len(pair_list))]
    lines.extend(f"{s} {t}" for s, t in pair_list)
    return "\n".join(lines) + "\n"


def run_java(classpath: str, algorithm: str, stdin_data: str) -> str:
    proc = subprocess.run(
        ["java", "-cp", classpath, "ch.Main", algorithm],
        input=stdin_data,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        timeout=TIMEOUT,
    )
    if proc.returncode != 0:
        raise RuntimeError(
            f"Java exited {proc.returncode} while running {algorithm}.\n"
            f"STDERR (first 500 chars): {proc.stderr[:500]}"
        )
    return proc.stdout


def parse_results(output: str) -> List[Tuple[int, int, int, int, int]]:
    rows: List[Tuple[int, int, int, int, int]] = []
    for line in output.splitlines():
        if not line.strip():
            continue
        parts = line.strip().split(",")
        if len(parts) != 5:
            raise ValueError(f"Unexpected output line: {line!r}")
        s, t, distance, time_ns, relaxed = (int(part) for part in parts)
        rows.append((s, t, distance, time_ns, relaxed))
    return rows


def write_csv(path: Path, rows: Iterable[Tuple[int, int, int, int, int]]) -> None:
    with path.open("w", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(["source", "target", "distance", "time_ns", "relaxed"])
        for row in rows:
            writer.writerow(row)


def load_vertex_ids(graph_path: Path) -> List[int]:
    with graph_path.open() as fh:
        header = fh.readline().split()
        if len(header) < 2:
            raise ValueError("Graph file missing vertex/edge counts header.")
        vertex_count = int(header[0])

        ids: List[int] = []
        for _ in range(vertex_count):
            parts = fh.readline().split()
            if len(parts) < 1:
                raise ValueError("Graph file ended before reading all vertex ids.")
            ids.append(int(parts[0]))

    if not ids:
        raise ValueError("No vertex IDs found in graph file.")

    return ids


def remap_pairs_to_vertices(pairs: Sequence[Tuple[int, int]], vertices: Sequence[int]) -> List[Tuple[int, int]]:
    size = len(vertices)
    return [
        (vertices[abs(s) % size], vertices[abs(t) % size])
        for s, t in pairs
    ]


def run_analysis(
    classpath: str,
    graph_path: Path,
    pair_count: int,
    output_dir: Path,
) -> None:
    vertex_ids = load_vertex_ids(graph_path)
    raw_pairs = input_generator(n=pair_count)
    pairs = remap_pairs_to_vertices(raw_pairs, vertex_ids)
    stdin_data = build_java_input(graph_path, pairs)

    output_dir.mkdir(parents=True, exist_ok=True)

    for algorithm, filename in (
        ("dijkstra", "dijkstra_results.csv"),
        ("bidirectional", "bidirectional_results.csv"),
    ):
        stdout = run_java(classpath, algorithm, stdin_data)
        rows = parse_results(stdout)
        write_csv(output_dir / filename, rows)


def parse_args(argv: List[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run Dijkstra analysis experiments.")
    parser.add_argument(
        "--classpath",
        required=True,
        help="Classpath containing compiled ch.Main (e.g. app/build/classes/java/main)",
    )
    parser.add_argument(
        "--graph",
        type=Path,
        default=Path("denmark.graph"),
        help="Path to .graph input file (defaults to denmark.graph).",
    )
    parser.add_argument(
        "--pairs",
        type=int,
        default=1000,
        help="Number of random (s,t) pairs to generate (default: 1000).",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("."),
        help="Directory where CSV results will be written (default: current directory).",
    )
    return parser.parse_args(argv)


def main(argv: List[str]) -> None:
    args = parse_args(argv)
    run_analysis(args.classpath, args.graph, args.pairs, args.output)


if __name__ == "__main__":
    main(sys.argv[1:])
