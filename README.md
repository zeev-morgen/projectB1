# Concurrent Web Crawler & Content Analyzer

A multi-threaded web crawler written in Java (JDK 25) that discovers pages
progressively, respects a user-defined depth limit, and runs pluggable
content analyses over the result set.

## Build

```bash
mvn -q clean package
```

This produces `target/concurrent-web-crawler-1.0.0.jar`. Jsoup and Jackson
are the only third-party dependencies.

## Run

Copy the runtime dependencies once, then launch:

```bash
mvn -q dependency:copy-dependencies -DoutputDirectory=target/libs
java -cp "target/concurrent-web-crawler-1.0.0.jar:target/libs/*" Main \
  --analysis WORD_COUNT,BROKEN_LINKS,MOST_LINKED_DOMAIN,KEYWORD_FREQUENCY \
  --poolsize 8 \
  --depth 2 \
  --input seeds.txt \
  --output report.json
```

### CLI arguments

| Flag         | Meaning |
|--------------|---------|
| `--analysis` | Comma-separated list. One or more of `WORD_COUNT`, `MOST_LINKED_DOMAIN`, `BROKEN_LINKS`, `KEYWORD_FREQUENCY`. |
| `--poolsize` | Integer > 0. Size of the fixed worker pool. |
| `--depth`    | Integer ≥ 0. `0` = seeds only, `1` = seeds + direct links, etc. |
| `--input`    | Text file, one seed URL per line. |
| `--output`   | Report path. |
| `--format`   | **Extension.** `json` (default) or `csv`. |
| `--domains`  | **Extension.** Comma-separated host whitelist (`example.com,other.org`). |

### Error messages (stderr)

* `<url> malformed` – non-`http`/`https` URL.
* `<url> failed` – network / timeout error (crawl continues).
* `error saving report` – output write failure.
* `invalid input file` – missing / unreadable seeds file (exits).
* `invalid pool size` – pool size ≤ 0 (exits).
* `invalid depth` – negative depth (exits).
* `<name> is unknown` – unknown analysis name (ignored).
* `no valid analysis` – no known analysis requested (exits).

## Test suite

No JUnit is used (the spec disallows extra libraries). The suite uses
a zero-dependency harness and spins up the JDK-provided
`com.sun.net.httpserver.HttpServer` for controlled fixtures:

```bash
mvn -q test-compile
mvn -q dependency:copy-dependencies -DoutputDirectory=target/libs
java -cp "target/classes:target/test-classes:target/libs/*" RunAllTests
```

Coverage: `poolSize=1`, `poolSize>1`, `depth=0`, `depth>1`,
duplicate URLs, circular links, a 100+ page fan-out graph,
first-discovery ordering, broken-link handling, and the
`--domains` whitelist.

## Design patterns used

1. **Strategy** — `AnalysisStrategy` + four concrete implementations
   (`WordCountStrategy`, `MostLinkedDomainStrategy`,
   `BrokenLinksStrategy`, `KeywordFrequencyStrategy`). New analyses
   plug in without changing the crawler (OCP).
2. **Factory** — `AnalysisStrategyFactory` (name → strategy) and
   `OutputFormatterFactory` (format → formatter) hide instantiation
   details from callers.
3. **Builder** — `PageResult.Builder` constructs the immutable page
   record so each field (including optional body text) can be set
   independently.
4. **Observer** — `CrawlObserver` + `CrawlEventPublisher` expose a
   thread-safe listener channel for discovery, completion, failure and
   finish events. `NoOpObserver` is the default subscriber; additional
   observers (logging, metrics) can be registered without changing
   the crawler.

## Extensions implemented

Exactly two of the optional extensions are wired up:

1. **`--format <type>`** — emits `csv` output (pages table followed by
   an analysis table) in addition to the default `json`.
2. **`--domains <d1,d2>`** — hostname whitelist filter. A host matches
   either an exact entry or a subdomain suffix (`foo.example.com`
   matches `example.com`).
