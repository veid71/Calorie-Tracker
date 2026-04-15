#!/usr/bin/env python3
"""
Build a pre-built Open Food Facts SQLite database for CalorieTracker app.

Downloads the full OFFs JSONL export, filters to products with valid
barcodes and nutrition data, and outputs a compressed SQLite file that
the app can download and import in seconds.

Usage:
    pip install -r requirements.txt
    python build_database.py

Output:
    output/openfoodfacts.db.gz    - Compressed SQLite database (~30-80MB)
    output/database_version.json  - Version metadata for the app to check
"""

import argparse
import gzip
import json
import os
import sqlite3
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

import requests

# ── Configuration ──────────────────────────────────────────────────────────────
OFFS_EXPORT_URL = "https://static.openfoodfacts.org/data/openfoodfacts-products.jsonl.gz"
OUTPUT_DIR = Path("output")
DB_FILE = OUTPUT_DIR / "openfoodfacts.db"
DB_GZ_FILE = OUTPUT_DIR / "openfoodfacts.db.gz"
VERSION_FILE = OUTPUT_DIR / "database_version.json"

BATCH_SIZE = 1000       # Rows inserted per SQLite transaction
LOG_INTERVAL = 50_000   # Print progress every N products scanned
COMPRESS_LEVEL = 6      # gzip compression level (1=fast, 9=smallest)

# Minimum barcode length (UPC-A=12, EAN-13=13, allow shorter for some markets)
MIN_BARCODE_LEN = 8


# ── Schema ─────────────────────────────────────────────────────────────────────
CREATE_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS openfoodfacts_items (
    id              TEXT PRIMARY KEY,
    barcode         TEXT UNIQUE NOT NULL,
    productName     TEXT NOT NULL,
    brands          TEXT,
    categories      TEXT,
    labels          TEXT,
    countries       TEXT,
    ingredients     TEXT,
    allergens       TEXT,
    servingSize     TEXT,
    servingQuantity REAL,
    energyKj        REAL,
    energyKcal      REAL,
    fat             REAL,
    saturatedFat    REAL,
    carbohydrates   REAL,
    sugars          REAL,
    fiber           REAL,
    proteins        REAL,
    salt            REAL,
    sodium          REAL,
    imageUrl        TEXT,
    productUrl      TEXT,
    nutritionGrade  TEXT,
    novaGroup       INTEGER,
    completeness    REAL,
    lastUpdated     INTEGER
)
"""

CREATE_INDEXES_SQL = [
    "CREATE INDEX IF NOT EXISTS idx_productName ON openfoodfacts_items(productName)",
    "CREATE UNIQUE INDEX IF NOT EXISTS idx_barcode ON openfoodfacts_items(barcode)",
]

INSERT_SQL = """
INSERT OR REPLACE INTO openfoodfacts_items
(id, barcode, productName, brands, categories, labels, countries,
 ingredients, allergens, servingSize, servingQuantity,
 energyKj, energyKcal, fat, saturatedFat, carbohydrates, sugars,
 fiber, proteins, salt, sodium, imageUrl, productUrl,
 nutritionGrade, novaGroup, completeness, lastUpdated)
VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
"""


# ── Helpers ────────────────────────────────────────────────────────────────────
def _float(val):
    try:
        return float(val) if val is not None and val != "" else None
    except (ValueError, TypeError):
        return None


def _int(val):
    try:
        return int(val) if val is not None and val != "" else None
    except (ValueError, TypeError):
        return None


def _str(val):
    """Return stripped string or None."""
    s = str(val).strip() if val is not None else ""
    return s if s else None


def extract_product(product: dict):
    """
    Map an OFFs product dict to a row tuple.
    Returns None if the product should be skipped.
    """
    barcode = _str(product.get("code"))
    name = _str(product.get("product_name"))

    # Must have both barcode and name
    if not barcode or not name:
        return None
    if len(barcode) < MIN_BARCODE_LEN:
        return None

    nutriments = product.get("nutriments") or {}
    serving_qty = _float(product.get("serving_quantity"))

    # ── Calories per 100g ────────────────────────────────────────────────────────
    # Prefer explicit _100g fields.  The bare "energy-kcal" key is ambiguous in OFFs
    # exports — contributors sometimes store a per-serving value there — so we avoid
    # it as a direct fallback.  Instead, if only a per-serving value exists and we
    # know the serving size in grams, we back-calculate the per-100g figure.
    energy_kcal = (
        _float(nutriments.get("energy-kcal_100g"))
        or _float(nutriments.get("energy_100g"))
    )
    if energy_kcal is None:
        kcal_serving = _float(nutriments.get("energy-kcal_serving"))
        if kcal_serving is not None and serving_qty and serving_qty > 0:
            energy_kcal = kcal_serving / (serving_qty / 100.0)

    if energy_kcal is None:
        return None

    # ── Energy in kJ per 100g (same logic) ──────────────────────────────────────
    energy_kj = _float(nutriments.get("energy-kj_100g"))
    if energy_kj is None:
        kj_serving = _float(nutriments.get("energy-kj_serving"))
        if kj_serving is not None and serving_qty and serving_qty > 0:
            energy_kj = kj_serving / (serving_qty / 100.0)

    now_ms = int(time.time() * 1000)

    return (
        barcode,                                            # id
        barcode,                                            # barcode
        name,                                               # productName
        _str(product.get("brands")),
        _str(product.get("categories")),
        _str(product.get("labels")),
        _str(product.get("countries")),
        _str(product.get("ingredients_text")),
        _str(product.get("allergens")),
        _str(product.get("serving_size")),
        serving_qty,
        energy_kj,
        energy_kcal,
        _float(nutriments.get("fat_100g")),
        _float(nutriments.get("saturated-fat_100g")),
        _float(nutriments.get("carbohydrates_100g")),
        _float(nutriments.get("sugars_100g")),
        _float(nutriments.get("fiber_100g")),
        _float(nutriments.get("proteins_100g")),
        _float(nutriments.get("salt_100g")),
        _float(nutriments.get("sodium_100g")),
        _str(product.get("image_url")),
        _str(product.get("url")),
        _str(product.get("nutrition_grades")),
        _int(product.get("nova_group")),
        _float(product.get("completeness")),
        now_ms,                                             # lastUpdated
    )


# ── Database setup ─────────────────────────────────────────────────────────────
def init_db(path: Path) -> sqlite3.Connection:
    conn = sqlite3.connect(str(path))
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=NORMAL")
    conn.execute("PRAGMA cache_size=-64000")   # 64 MB page cache
    conn.execute(CREATE_TABLE_SQL)
    conn.commit()
    return conn


def create_indexes(conn: sqlite3.Connection):
    print("Building indexes...")
    for sql in CREATE_INDEXES_SQL:
        conn.execute(sql)
    conn.commit()


# ── Main ───────────────────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="Build OFFs pre-built database")
    parser.add_argument(
        "--input", metavar="FILE",
        help="Path to a pre-downloaded en.openfoodfacts.org.products.jsonl.gz file. "
             "If omitted the script streams the file directly from Open Food Facts.",
    )
    args = parser.parse_args()

    OUTPUT_DIR.mkdir(exist_ok=True)

    source = args.input if args.input else OFFS_EXPORT_URL
    print(f"[{datetime.now():%Y-%m-%d %H:%M:%S}] Building OFFs database")
    print(f"Source : {source}")
    print(f"Output : {DB_GZ_FILE}\n")

    conn = init_db(DB_FILE)
    batch: list = []
    total_inserted = 0
    total_scanned = 0
    t0 = time.time()

    def flush():
        nonlocal total_inserted
        if batch:
            conn.executemany(INSERT_SQL, batch)
            conn.commit()
            total_inserted += len(batch)
            batch.clear()

    def process_stream(f):
        nonlocal total_scanned
        for raw_line in f:
            raw_line = raw_line.strip()
            if not raw_line:
                continue
            total_scanned += 1
            try:
                row = extract_product(json.loads(raw_line))
                if row:
                    batch.append(row)
                    if len(batch) >= BATCH_SIZE:
                        flush()
            except (json.JSONDecodeError, Exception):
                pass

            if total_scanned % LOG_INTERVAL == 0:
                elapsed = time.time() - t0
                rate = total_scanned / elapsed
                print(
                    f"  Scanned {total_scanned:>8,}  |  "
                    f"Inserted {total_inserted:>7,}  |  "
                    f"{rate:,.0f} lines/s"
                )

    if args.input:
        # Process a pre-downloaded local .gz file
        input_path = Path(args.input)
        if not input_path.exists():
            print(f"ERROR: --input file not found: {input_path}", file=sys.stderr)
            sys.exit(1)
        print(f"Processing local file: {input_path} ({input_path.stat().st_size / 1024 / 1024:.1f} MB)")
        with gzip.open(input_path, mode="rt", encoding="utf-8", errors="replace") as f:
            process_stream(f)
    else:
        # Stream-download → decompress → parse → insert (never writes full uncompressed file)
        print("Streaming download from Open Food Facts...")
        # timeout=(connect_s, read_s): no read timeout — the OFFs export is several GB and can
        # take hours; a fixed read timeout would kill the stream mid-download.
        # decode_content=False: tell urllib3 NOT to decompress Content-Encoding automatically
        # so that gzip.open receives the raw compressed bytes it expects.
        with requests.get(OFFS_EXPORT_URL, stream=True, timeout=(60, None)) as resp:
            resp.raise_for_status()
            resp.raw.decode_content = False   # prevent double-decompression
            with gzip.open(resp.raw, mode="rt", encoding="utf-8", errors="replace") as f:
                process_stream(f)

    flush()
    create_indexes(conn)
    conn.close()

    elapsed = time.time() - t0
    db_size_mb = DB_FILE.stat().st_size / 1024 / 1024
    print(f"\n✅ Database built in {elapsed:.0f}s")
    print(f"   Products : {total_inserted:,}")
    print(f"   SQLite   : {db_size_mb:.1f} MB")

    # Compress
    print("\nCompressing database...")
    with open(DB_FILE, "rb") as f_in, gzip.open(DB_GZ_FILE, "wb", compresslevel=COMPRESS_LEVEL) as f_out:
        while chunk := f_in.read(131072):
            f_out.write(chunk)

    gz_size_mb = DB_GZ_FILE.stat().st_size / 1024 / 1024
    print(f"✅ Compressed: {db_size_mb:.1f} MB → {gz_size_mb:.1f} MB")

    # Write version JSON (download_url filled in by CI after release is created)
    version = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    version_data = {
        "version": version,
        "build_date": datetime.now(timezone.utc).strftime("%Y-%m-%d"),
        "download_url": "__PLACEHOLDER__",
        "file_size_bytes": DB_GZ_FILE.stat().st_size,
        "product_count": total_inserted,
    }
    with open(VERSION_FILE, "w") as f:
        json.dump(version_data, f, indent=2)

    print(f"\n📦 Output files:")
    print(f"   {DB_GZ_FILE}   ({gz_size_mb:.1f} MB)")
    print(f"   {VERSION_FILE}")
    print("\nNext step: upload openfoodfacts.db.gz to a GitHub Release,")
    print("then update database_version.json with the download URL.")


if __name__ == "__main__":
    main()
