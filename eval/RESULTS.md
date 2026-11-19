# Custom-item recognition: evaluation notes

## Current method: label text (ML Kit)
Items are recognised by the words on their label (`core/recognition/text/LabelText.kt`).
Enrollment keeps words read in at least 25% of the photos (and in two); at alarm time a frame
counts when ≥ 3 keywords, ≥ 40% of all keywords and one keyword of ≥ 6 letters are read
(fuzzy: 1 slip for 5–8 letters, 2 for longer), and 3 of the last 5 frames confirm.

`eval/ocr_cache.py` + `eval/text_match.py` prototype these rules with an open OCR engine
(RapidOCR, PaddleOCR models), since ML Kit runs only on Android.

| Set | Result (default rules) |
| --- | --- |
| Objectron, other objects' clips (1,128) | **0 false dismissals** (every setting tried) |
| Objectron, enrollable instances | 24/48 (cups and many bottles show no readable text) |
| Objectron, true dismissal within 3 s | 9/24 |
| GroceryStoreDataset, other brands per photo | 0.04% |
| GroceryStoreDataset, same brand / other variant per photo | 6.4% |
| GroceryStoreDataset, true accepts per photo | 34% |

**These public sets are a poor test of this method.** Objectron filmed objects from about a
metre at 640 px and the grocery photos are 348 px, so labels are a few pixels tall: the OCR reads
fragments ("urtel", "FLJT", "MELANLK"). The app reads a label held close at 1280×720, so recall
there should be far higher; it has to be measured on real close-up recordings
(`userCapturedItems` / `extract_frames.py`, see README). Same-brand variants that share most of
their words remain the main risk; on the enrollment screen the user sees the keywords and can
remove generic ones (e.g. the brand) so the distinguishing words decide.

## Earlier method: keypoints (ORB, then SIFT + photometric check)
Replaced because it depended on the background (the enrollment square included the table, whose
texture became part of the reference) and was unreliable in practice. On Objectron with
whole-turn enrollment SIFT reached 90.7% true dismissal with 0 false dismissals and 1.4% same-brand
accepts per photo; the scripts are in git history (eval/variants.py, eval/analyze_variants.py).
