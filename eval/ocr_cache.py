"""Run an open OCR engine (RapidOCR = PaddleOCR models) over the ground-truth images and cache
the recognised text lines as JSON. ML Kit, which the app uses, only runs on Android; this is a
stand-in for prototyping keyword selection and matching rules (eval/text_match.py).

Usage: python -I eval/ocr_cache.py <eval_dir>
"""
import json
import sys
from multiprocessing import Pool
from pathlib import Path


def ocr(path):
    global engine
    if "engine" not in globals():
        from rapidocr_onnxruntime import RapidOCR
        engine = RapidOCR()
    result, _ = engine(str(path))
    return str(path), [(r[1], float(r[2])) for r in (result or [])]


def main():
    root = Path(sys.argv[1])
    out = root / "ocr-cache.json"
    cache = json.loads(out.read_text()) if out.exists() else {}
    grocery = root / "GroceryStoreDataset/dataset"
    paths = []
    for c in sorted((grocery / "train/Packages").glob("*/*")):
        paths += sorted(c.glob("*.jpg"))[:5]
    paths += sorted((grocery / "test/Packages").glob("*/*/*.jpg"))
    for d in sorted((root / "objectron").glob("*/*")):
        paths += sorted(d.glob("*.jpg"))[::2]
    todo = [p for p in paths if str(p) not in cache]
    with Pool(4) as pool:
        for i, (p, lines) in enumerate(pool.imap_unordered(ocr, todo, chunksize=8)):
            cache[p] = lines
            if i % 200 == 0:
                out.write_text(json.dumps(cache))
                print(f"{i}/{len(todo)}", flush=True)
    out.write_text(json.dumps(cache))
    print("done", len(cache))


if __name__ == "__main__":
    main()
