"""
Benchmark runner for original Dijkstra and CH-enabled bidirectional Dijkstra.
Supports both the plain graph and the augmented graph produced during CH preprocessing.
Example run:

python dijkstra_analysis.py \
    --classpath app/build/libs/app.jar \
    --original-graph denmark.graph \
    --augmented-graph denmark-augmented.graph \
    --pairs 1000 --output results
"""

from __future__ import annotations

import argparse
import csv
import re
import subprocess
import sys
from enum import Enum
from pathlib import Path
from typing import Iterable, List, Sequence, Tuple

from input import input_generator

TIMEOUT = 600


class GraphFormat(Enum):
    ORIGINAL = "original"
    AUGMENTED = "augmented"


def load_vertex_ids(graph_path: Path) -> Tuple[List[int], GraphFormat]:
    with graph_path.open() as fh:
        header = fh.readline().split()
        if len(header) < 2:
            raise ValueError("Graph file missing vertex/edge counts header.")
        vertex_count = int(header[0])

        ids: List[int] = []
        detected_format: GraphFormat | None = None
        for i in range(vertex_count):
            line = fh.readline()
            if not line:
                raise ValueError("Graph file ended before reading all vertex ids.")
            parts = line.split()
            if not parts:
                raise ValueError(f"Empty vertex line encountered at index {i}.")

            token_count = len(parts)
            if detected_format is None:
                if token_count == 3:
                    detected_format = GraphFormat.ORIGINAL
                elif token_count == 4:
                    detected_format = GraphFormat.AUGMENTED
                else:
                    raise ValueError(
                        "Vertex rows must have 3 (original) or 4 (augmented) columns; "
                        f"got {token_count} in line: {line!r}"
                    )
            else:
                expected = 3 if detected_format is GraphFormat.ORIGINAL else 4
                if token_count != expected:
                    raise ValueError("Mixed vertex formats detected within graph file.")

            ids.append(int(parts[0]))

    if not ids:
        raise ValueError("No vertex IDs found in graph file.")

    return ids, detected_format or GraphFormat.ORIGINAL


def remap_pairs_to_vertices(pairs: Sequence[Tuple[int, int]], vertices: Sequence[int]) -> List[Tuple[int, int]]:
    size = len(vertices)
    return [
        (vertices[abs(s) % size], vertices[abs(t) % size])
        for s, t in pairs
    ]


QUERY_RE = re.compile(r"distance=(-?\d+)\s+relaxed=(\d+)\s+time\(ns\)=(\d+)")


def run_java_query(
    classpath: str,
    mode: str,
    graph_path: Path,
    source: int,
    target: int,
) -> Tuple[int, int, int]:
    proc = subprocess.run(
        [
            "java",
            "-cp",
            classpath,
            "ch.Main",
            mode,
            str(graph_path),
            str(source),
            str(target),
        ],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        timeout=TIMEOUT,
    )
    if proc.returncode != 0:
        raise RuntimeError(
            f"Java exited {proc.returncode} while running {mode}.\n"
            f"STDERR (first 500 chars): {proc.stderr[:500]}"
        )

    matches = list(QUERY_RE.finditer(proc.stdout))
    if not matches:
        sample = proc.stdout.strip() or "<no stdout>"
        raise ValueError(
            "Failed to parse query output. Expected 'distance=.. relaxed=.. time(ns)=..'.\n"
            f"STDOUT was: {sample[:200]!r}"
        )

    match = matches[-1]
    distance = int(match.group(1))
    relaxed = int(match.group(2))
    time_ns = int(match.group(3))
    return distance, time_ns, relaxed


def write_csv(path: Path, rows: Iterable[Tuple[int, int, int, int, int]]) -> None:
    with path.open("w", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(["source", "target", "distance", "time_ns", "relaxed"])
        for row in rows:
            writer.writerow(row)


def run_analysis(
    classpath: str,
    original_graph: Path,
    augmented_graph: Path,
    pair_count: int,
    output_dir: Path,
) -> None:
    vertex_ids, original_format = load_vertex_ids(original_graph)
    if original_format is GraphFormat.AUGMENTED:
        raise ValueError(
            "--original-graph appears to be an augmented file; please supply the plain graph."
        )
    _, augmented_format = load_vertex_ids(augmented_graph)
    if augmented_format is not GraphFormat.AUGMENTED:
        raise ValueError(
            "--augmented-graph must include vertex ranks (4 columns per vertex)."
        )

    raw_pairs = input_generator(n=pair_count)
    pairs = remap_pairs_to_vertices(raw_pairs, vertex_ids)

    output_dir.mkdir(parents=True, exist_ok=True)

    algorithms = (
        (
            "regular-dijkstra",
            "query-dijkstra",
            original_graph,
            "regular",
            "dijkstra_results.csv",
        ),
        (
            "regular-bidirectional",
            "query-raw",
            original_graph,
            "regular",
            "bidirectional_results.csv",
        ),
        (
            "augmented-bidirectional",
            "query",
            augmented_graph,
            "augmented",
            "bidirectional_results.csv",
        ),
    )

    for algorithm, mode, graph_file, subdir, filename in algorithms:
        target_dir = output_dir / subdir
        target_dir.mkdir(parents=True, exist_ok=True)
        rows: List[Tuple[int, int, int, int, int]] = []
        for source, target in pairs:
            distance, time_ns, relaxed = run_java_query(
                classpath, mode, graph_file, source, target
            )
            rows.append((source, target, distance, time_ns, relaxed))
        write_csv(target_dir / filename, rows)


def parse_args(argv: List[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run Dijkstra analysis experiments.")
    parser.add_argument(
        "--classpath",
        required=True,
        help="Classpath containing compiled ch.Main or .jar file (e.g. app/build/libs/app.jar)",
    )
    parser.add_argument(
        "--original-graph",
        type=Path,
        default=None,
        help="Path to the original .graph input (default: denmark.graph).",
    )
    parser.add_argument(
        "--augmented-graph",
        type=Path,
        default=Path("denmark-augmented.graph"),
        help="Path to the augmented .graph file with ranks (default: denmark-augmented.graph).",
    )
    parser.add_argument(
        "--graph",
        type=Path,
        help=argparse.SUPPRESS,
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
    args = parser.parse_args(argv)
    if args.original_graph is None:
        args.original_graph = args.graph or Path("denmark.graph")
    return args


def main(argv: List[str]) -> None:
    args = parse_args(argv)
    run_analysis(args.classpath, args.original_graph, args.augmented_graph, args.pairs, args.output)


if __name__ == "__main__":
    main(sys.argv[1:])
