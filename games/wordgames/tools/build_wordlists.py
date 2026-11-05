#!/usr/bin/env python3
"""Builds the word lists in src/main/assets from the `wordlist-english` npm package (MIT),
which is derived from SCOWL (see THIRD_PARTY_NOTICES-SCOWL.txt).

Usage:
    curl -sSLo pkg.tgz https://registry.npmjs.org/wordlist-english/-/wordlist-english-1.2.1.tgz
    tar xzf pkg.tgz
    python3 tools/build_wordlists.py package/ src/main/assets/

Outputs:
    words.txt   every word a player may enter (SCOWL size 10-35, 3-8 letters, a-z only)
    common.txt  words the games pick as answers (SCOWL size 10-20, 3-8 letters), minus a blocklist
"""
import json
import os
import sys

src, out = sys.argv[1], sys.argv[2]

# Never chosen as an answer or seed. Not exhaustive: SCOWL's common tiers are mostly clean,
# this catches the obvious cases. Players can still type any word in words.txt.
BLOCK = set("""
anal anus arse ass bastard bitch bitches bloody bollocks boob boobs breast breasts butt
crap cunt damn dick dildo drug drugs drunk fag fart fuck fucked fucker fucking gay
hell homo horny kill killed killer killing kills nazi nigger nipple nude penis piss porn
pussy rape raped rapist sex sexy shit slut sperm suicide tit tits vagina wank whore
""".split())

def load(tier):
    with open(os.path.join(src, f"english-words-{tier}.json")) as f:
        return json.load(f)

def keep(w):
    return w.isascii() and w.isalpha() and w.islower() and 3 <= len(w) <= 8

valid = sorted({w for t in (10, 20, 35) for w in load(t) if keep(w)} - BLOCK)
common = sorted({w for t in (10, 20) for w in load(t) if keep(w)} - BLOCK)

os.makedirs(out, exist_ok=True)
with open(os.path.join(out, "words.txt"), "w") as f:
    f.write("\n".join(valid) + "\n")
with open(os.path.join(out, "common.txt"), "w") as f:
    f.write("\n".join(common) + "\n")
print(f"words.txt: {len(valid)} words, common.txt: {len(common)} words")
