"""Small, dependency-free report for one frozen playtest run. Never edits earlier reports."""
import argparse
import csv
import html
import json
from pathlib import Path
from statistics import median


def read(path):
    with Path(path).open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    keys = [(r["mode"], r["level"], r["seed"], r["seat"], r["style"]) for r in rows]
    if not rows or len(set(keys)) != len(keys):
        raise ValueError("Empty run or duplicate match keys")
    return rows


def won(row):
    return row["won"] == "true"


def timeout(row):
    return row.get("status", "") == "timeout" or (not row.get("status") and float(row["seconds"]) >= 299.9)


def table(headers, rows):
    return "<div class='table'><table><thead><tr>" + "".join(f"<th>{html.escape(str(v))}</th>" for v in headers) + "</tr></thead><tbody>" + "".join(
        "<tr>" + "".join(f"<td>{html.escape(str(v))}</td>" for v in row) + "</tr>" for row in rows) + "</tbody></table></div>"


def summarize(rows, field):
    result = []
    for key in sorted({r[field] for r in rows}, key=lambda s: int(s) if s.isdigit() else s):
        group = [r for r in rows if r[field] == key]
        wins = [r for r in group if won(r)]
        result.append([key, f"{len(wins)}/{len(group)}", f"{median(float(r['seconds']) for r in wins):.1f}" if wins else "—",
                       sum(timeout(r) for r in group), f"{median(float(r['longest_drought']) for r in group):.1f}"])
    return result


def compare(rows, other):
    key = lambda r: tuple(r[k] for k in ("mode", "level", "seed", "seat", "style"))
    previous = {key(r): r for r in other}
    pairs = [(previous[key(r)], r) for r in rows if key(r) in previous]
    if not pairs:
        raise ValueError("No matching map, seed, seat and strategy cases to compare")
    gained = sum(not won(a) and won(b) for a, b in pairs)
    lost = sum(won(a) and not won(b) for a, b in pairs)
    return {"paired_matches": len(pairs), "gained_wins": gained, "lost_wins": lost,
            "unpaired_current": len(rows)-len(pairs), "unpaired_previous": len(other)-len(pairs)}, pairs


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("run", type=Path)
    parser.add_argument("--compare", type=Path)
    args = parser.parse_args()
    root = args.run
    rows = read(root / "matches.csv")
    timing = json.loads((root / "timing.json").read_text())
    if timing["matches"] != len(rows):
        raise ValueError("CSV match count differs from the completed run")
    manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8-sig"))
    modes = summarize(rows, "mode")
    styles = summarize(rows, "style")
    levels = summarize(rows, "level")
    summary = {"matches": len(rows), "wins": sum(map(won, rows)), "timeouts": sum(map(timeout, rows)),
               "maps_without_win": [int(r[0]) for r in levels if r[1].split('/')[0] == '0' and r[0] != '0'],
               "wall_seconds": timing["wall_seconds"], "source_sha256": manifest["source_sha256"]}
    comparison = ""
    if args.compare:
        old_path = args.compare / "matches.csv" if args.compare.is_dir() else args.compare
        stats, pairs = compare(rows, read(old_path))
        old_manifest = old_path.parent / "manifest.json"
        old = json.loads(old_manifest.read_text(encoding="utf-8-sig")) if old_manifest.exists() else {}
        reasons = []
        for name in ("policy_sha256", "difficulty"):
            if name == "difficulty" and old.get("mode") in ("Quick", "Full") and manifest.get("mode") in ("Quick", "Full"):
                continue  # Campaigns use their authored difficulty, including older manifests.
            if name not in old or old[name] != manifest[name]:
                reasons.append(name)
        stats["same_policy_and_difficulty"] = not reasons
        stats["same_campaign_data"] = old.get("campaign_sha256") == manifest["campaign_sha256"]
        summary["comparison"] = stats
        wording = "Same policies and difficulty setting." if not reasons else "Descriptive comparison; missing or different provenance: " + ", ".join(reasons) + "."
        if not stats["same_campaign_data"]: wording += " Campaign data changed."
        comparison = "<h2>Comparison</h2><p>" + html.escape(wording) + "</p>" + table(
            ["Paired games", "Gained wins", "Lost wins", "Unpaired now / before"],
            [[stats["paired_matches"],stats["gained_wins"],stats["lost_wins"],f"{stats['unpaired_current']} / {stats['unpaired_previous']}"]])
    (root / "summary.json").write_text(json.dumps(summary, indent=2)+"\n", encoding="utf-8")
    flags = []
    for row in levels:
        level, score, seconds, unfinished, drought = row
        if level == "0":
            continue
        wins, count = map(int, score.split("/"))
        if wins == 0: flags.append(f"Map {level}: no tested winning line.")
        elif wins / count < .25: flags.append(f"Map {level}: only {score} scripted wins.")
        if unfinished: flags.append(f"Map {level}: {unfinished} unfinished games.")
    notes = table(["Finding"], [[s] for s in flags]) if flags else "<p>No narrow or unfinished maps in this sample.</p>"
    headers = ["Map", "Wins / games", "Win time, s", "Unfinished", "Capture gap, s"]
    source = f"""<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Opacha playtest</title><style>
*{{box-sizing:border-box}}body{{margin:0;background:#fcfbfa;color:#302a28;font:16px system-ui,sans-serif;line-height:1.5}}main{{max-width:940px;margin:auto;padding:28px 20px 30px}}h1{{font-size:30px;margin:0 0 8px}}h2{{font-size:20px;margin:16px 0 10px}}p{{margin:8px 0}}a{{color:#175d53}}.table{{overflow:auto;max-height:60vh}}table{{border-collapse:collapse;width:100%;font-variant-numeric:tabular-nums}}th,td{{padding:10px 12px;text-align:right;border-bottom:1px solid #ddd4d0;white-space:nowrap}}th:first-child,td:first-child{{text-align:left}}th{{font-size:13px;color:#645953;position:sticky;top:0;background:#fcfbfa}}small{{color:#645953}}select{{font:inherit;min-height:48px;padding:8px 12px;border:1px solid #716762;border-radius:8px;background:#fff;color:inherit;max-width:100%;margin:8px 8px 8px 0}}[hidden]{{display:none!important}}#inputs td,#checks td,#rules td{{white-space:normal;text-align:left;overflow-wrap:anywhere}}@media(max-width:500px){{main{{padding:16px 12px}}h1{{font-size:26px}}th,td{{padding:10px 7px;font-size:14px}}}}
</style><main><h1>Opacha playtest</h1><p>{len(rows)} scripted matches · {timing['wall_seconds']:.1f}s · {summary['wins']} wins · {summary['timeouts']} unfinished</p>
<p><small>Bot results show available strategies and pacing. Human playtests must establish enjoyment and learning.</small></p>
<select aria-label="View" onchange="document.querySelectorAll('section').forEach(s=>s.hidden=s.id!==this.value)"><option value="results">Results</option><option value="inputs">Inputs</option><option value="checks">Data checks</option><option value="rules">Rules</option></select>
<section id="results"><select aria-label="Group results by" onchange="document.getElementById('maps').hidden=this.value!=='maps';document.getElementById('styles').hidden=this.value!=='styles'"><option value="maps">{'Maps' if any(r['level']!='0' for r in rows) else 'Modes'}</option><option value="styles">Strategies</option></select>
<div id="maps">{table(headers,levels) if any(r['level']!='0' for r in rows) else table(['Mode']+headers[1:],modes)}</div><div id="styles" hidden>{table(['Strategy']+headers[1:],styles)}</div>{comparison}</section>
<section id="inputs" hidden>{table(['Setting','Value'],[['Run',root.name],['Difficulty',manifest['difficulty']],['Seeds',str(manifest['seed_start'])+' through '+str(manifest['seed_start']+manifest['seeds']-1)],['Source SHA-256',manifest['source_sha256']]])}<p><a href="matches.csv">Raw matches</a> · <a href="manifest.json">Full settings</a> · <a href="timing.json">Timing</a></p></section>
<section id="checks" hidden>{notes}</section>
<section id="rules" hidden>{table(['Measure','Meaning'],[['Win time','Median game seconds among winning matches.'],['Unfinished','Reached the time limit; included in all game counts.'],['Capture gap','Median of each match’s longest period without a player capture.'],['Capture sampling','Every 0.5 game seconds; fast recaptures may be missed.'],['Narrow map','Fewer than 25% of the scripted attempts won. This is a review prompt, not a human difficulty rating.'],['Policies','Seven fixed strategies with delayed decisions and seeded mistakes. They do not learn or represent measured human behaviour.']])}</section></main></html>"""
    (root / "report.html").write_text(source, encoding="utf-8")
    print(f"{summary['wins']}/{len(rows)} scripted wins; {summary['timeouts']} unfinished; maps without a win: {summary['maps_without_win']}")
    print(root / "report.html")


if __name__ == "__main__":
    main()
