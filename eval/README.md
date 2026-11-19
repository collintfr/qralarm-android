# Custom-item recognition: evaluation

The "Show my own item" dismissal must not stop an alarm for the wrong item. Items are recognised
by their label text; these scripts prototype the keyword rules on real, labelled images with an
open OCR engine (ML Kit only runs on Android). Results and caveats: `RESULTS.md`.

| Set | What it is | Hard negatives |
| --- | --- | --- |
| GroceryStoreDataset (MIT) | 31 packaged products (juice, milk, yoghurt) photographed in shops on a phone | Same brand, other variant (Arla milk vs. Arla lactose-free milk) |
| Google Objectron (CC-BY 4.0) | Walk-around videos of single bottles, cereal boxes, books, cups | Other instances of the same category |
| Your own items | Your medication bottles, face wash, etc. (see below) | Other brands, variants, cap/back only, empty rooms |

## Running

```bash
python3 -m venv ~/eval-venv && ~/eval-venv/bin/pip install opencv-python-headless rapidocr_onnxruntime
git clone --depth 1 https://github.com/marcusklasson/GroceryStoreDataset <eval_dir>/GroceryStoreDataset
~/eval-venv/bin/python -I eval/fetch_objectron.py <eval_dir> 12
~/eval-venv/bin/python -I eval/ocr_cache.py <eval_dir>
~/eval-venv/bin/python -I eval/text_match.py <eval_dir>
```

## Your own items (the test that matters)

Record close-ups with your phone, the way you will show items when the alarm rings, and arrange
them as described in `extract_frames.py` (enrollment photos, positive videos, negatives such as
another brand or another variant of the same brand). Then run `extract_frames.py` followed by
`ocr_cache.py`/`text_match.py` on that folder, or simply try the items in the app.
