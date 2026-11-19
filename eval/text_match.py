"""Prototype of label-text keyword selection and matching (mirrors core/recognition/text in the app).

Uses eval/ocr_cache.py output. Reports, per rule setting:
- Objectron (whole-turn enrollment from 8 frames): sequences dismissed within 3 s, false dismissals
- GroceryStoreDataset: per-photo accepts (true / same brand, other variant / other brands)

Usage: python -I eval/text_match.py <eval_dir>
"""
import collections
import itertools
import json
import re
import sys
from pathlib import Path

STOP = {"THE", "AND", "FOR", "WITH", "NET", "WT", "USE", "ONLY", "TABLET", "TABLETS", "CAPSULE",
        "CAPSULES", "MG", "ML", "OZ", "FL", "PER", "EACH", "NEW", "NOT", "ARE", "THIS", "YOUR", "ALL"}
UNIT = re.compile(r"^(\d+)(MG|ML|G|OZ|HR|HOUR|CT|MCG|IU|L|KG|%)$")


def tokens(lines, min_conf=0.5):
    out = set()
    for text, conf in lines:
        if conf < min_conf:
            continue
        words = re.findall(r"[A-Za-z0-9ÅÄÖåäöÉéÜü%]+", text.upper())
        # Join a number with the unit that follows it: "180 MG" -> "180MG".
        joined, i = [], 0
        while i < len(words):
            if words[i].isdigit() and i + 1 < len(words) and UNIT.match(words[i] + words[i + 1]):
                joined.append(words[i] + words[i + 1]); i += 2
            else:
                joined.append(words[i]); i += 1
        for w in joined:
            if len(w) >= 3 and w not in STOP and (any(c.isalpha() for c in w)):
                out.add(w)
    return out


def edit_distance(a, b, limit):
    if abs(len(a) - len(b)) > limit:
        return limit + 1
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        cur = [i] + [0] * len(b)
        for j, cb in enumerate(b, 1):
            cur[j] = min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (ca != cb))
        prev = cur
    return prev[-1]


def allowed(k):
    return 0 if len(k) <= 4 else 1 if len(k) <= 8 else 2


def matches(k, toks):
    if k in toks:
        return True
    lim = allowed(k)
    return lim > 0 and any(edit_distance(k, t, lim) <= lim for t in toks)


def keywords(frames, min_share, max_kw=12):
    count = collections.Counter(t for f in frames for t in f)
    need = max(2, -(-len(frames) * min_share // 1))
    kw = [t for t, c in count.items() if c >= need]
    kw.sort(key=lambda t: (-count[t], -len(t)))
    return kw[:max_kw]


def verified(kw, toks, min_matched, min_share, min_long):
    m = [k for k in kw if matches(k, toks)]
    return len(m) >= min(min_matched, len(kw)) and len(m) >= min_share * len(kw) and any(len(k) >= min_long for k in m)


def confirms(flags, req=3, win=5, frame_ms=333):
    recent = collections.deque()
    for i, ok in enumerate(flags):
        recent.append(i if ok else None)
        while len(recent) > win: recent.popleft()
        while recent and recent[0] is None: recent.popleft()
        if ok and sum(x is not None for x in recent) >= req:
            return i * frame_ms
    return None


def main():
    root = Path(sys.argv[1])
    cache = {k: tokens(v) for k, v in json.loads((root / "ocr-cache.json").read_text()).items()}
    # Objectron: frames cached every 2nd -> 3 fps.
    obj = collections.defaultdict(list)
    for p in sorted(cache):
        if "/objectron/" in p:
            obj[Path(p).parent].append(p)
    groc = root / "GroceryStoreDataset/dataset"
    classes = sorted((groc / "train/Packages").glob("*/*"))
    print("enroll_share minMatched matchShare minLong | objectron enrollable, TD<=3s, FD | grocery pos/same/other")
    for es, mm, ms, ml in itertools.product([0.25, 0.4], [2, 3, 4], [0.25, 0.4, 0.5], [6, 8]):
        # Objectron
        enrolled, td, fd, nseq = 0, 0, 0, 0
        inst = {d: fr for d, fr in obj.items() if len(fr) >= 10}
        kws = {}
        for d, fr in inst.items():
            idx = [round(i * (len(fr) - 1) / 7) for i in range(8)]
            kw = keywords([cache[fr[i]] for i in idx], es)
            if len(kw) >= 3 and any(len(k) >= ml for k in kw):
                near = {j for i in idx for j in (i - 1, i, i + 1)}
                kws[d] = (kw, [cache[fr[i]] for i in range(len(fr)) if i not in near])
        for d, (kw, test) in kws.items():
            enrolled += 1
            t = confirms([verified(kw, x, mm, ms, ml) for x in test])
            td += t is not None and t <= 3000
            for d2, fr2 in inst.items():
                if d2 != d:
                    nseq += 1
                    fd += confirms([verified(kw, cache[f], mm, ms, ml) for f in fr2]) is not None
        # Grocery
        acc = collections.Counter(); tot = collections.Counter(); genrolled = 0
        for c in classes:
            kw = keywords([cache[str(f)] for f in sorted(c.glob("*.jpg"))[:5] if str(f) in cache], es)
            if len(kw) < 3 or not any(len(k) >= ml for k in kw):
                continue
            genrolled += 1
            for c2 in classes:
                lab = "pos" if c2 == c else "same" if c2.parent.name == c.parent.name else "other"
                for f in (groc / "test/Packages" / c2.parent.name / c2.name).glob("*.jpg"):
                    if str(f) in cache:
                        tot[lab] += 1; acc[lab] += verified(kw, cache[str(f)], mm, ms, ml)
        r = lambda l: acc[l] / max(1, tot[l])
        print(es, mm, ms, ml, "|", f"{enrolled}/{len(inst)}", f"{td}/{enrolled}", f"FD {fd}/{nseq}", "|",
              f"enrolled {genrolled}/{len(classes)}", f"{r('pos'):.3f}/{r('same'):.4f}/{r('other'):.4f}")


if __name__ == "__main__":
    main()
