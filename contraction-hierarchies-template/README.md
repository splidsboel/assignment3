This document contains instructions on how to generate the augmented graph and how to run the benchmarks from our report
## Generating and querying the augmented graph

```
# Preprocess original graph -> augmented graph (reads from stdin)
java -cp app/build/libs/app.jar ch.Main preprocess denmark-augmented.graph < denmark.graph

# Bidirectional CH query on augmented graph
java -cp app/build/libs/app.jar ch.Main query denmark-augmented.graph 123 456

# Bidirectional query on original graph (no ranks)
java -cp app/build/libs/app.jar ch.Main query-raw denmark.graph 123 456

# Plain Dijkstra query on original graph
java -cp app/build/libs/app.jar ch.Main query-dijkstra denmark.graph 123 456
```

## Running benchmarks

`dijkstra_analysis.py` generates random `(s,t)` pairs (via `input.py`) and invokes
the Java CLI for each algorithm. Example:

```
python dijkstra_analysis.py \
  --classpath app/build/libs/app.jar \
  --original-graph denmark.graph \
  --augmented-graph denmark-augmented.graph \
  --pairs 1000 \
  --output results
```

Output layout:

```
results/
  regular/
    dijkstra_results.csv          # plain Dijkstra on original graph
    bidirectional_results.csv     # bidirectional Dijkstra on original graph
  augmented/
    bidirectional_results.csv     # bidirectional Dijkstra on CH graph
```

Each CSV row: `source,target,distance,time_ns,relaxed`. Use a small `--pairs`
value first to verify the pipeline. Large runs can take >60 minutes

## Creating plots and LaTeX tables

`compare_algorithms.py` loads two CSVs and produces a LaTeX table plus PNG plots.
Example comparing the two regular-graph variants:

```
python compare_algorithms.py \
  --first results/regular/dijkstra_results.csv \
  --first-label "Regular Dijkstra" \
  --second results/regular/bidirectional_results.csv \
  --second-label "Regular Bidirectional" \
  --table results/regular/comparison_table.tex \
  --plot results/regular/runtime_comparison.png \
  --scatter results/regular/scatter_plot.png
```

To contrast regular vs. CH bidirectional searches, point `--second` at
`results/augmented/bidirectional_results.csv` and update the label accordingly.

The script prints the mean speed-up and generates:

- `comparison_table.tex` – mean/median times and relaxed edges.
- `runtime_comparison.png` – log-scale runtime boxplot.
- `scatter_plot.png` – distance vs. relaxed edges.
