"""Build the fixed campaign from authored graphs. No runtime random map generation."""
import json
import math
from collections import deque
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# Each graph has an opening, a choice of fronts, and a finish. Coordinates use board widths.
LAYOUTS = [
    ("First steps", [(0.5,.22),(.32,.50),(.65,.77),(.40,1.03),(.5,1.37)], [(0,1),(1,2),(2,3),(3,4)]),
    ("Fork", [(.5,.20),(.23,.47),(.77,.47),(.50,.76),(.23,1.05),(.77,1.05),(.5,1.37)], [(0,1),(0,2),(1,3),(2,3),(3,4),(3,5),(4,6),(5,6)]),
    ("Two banks", [(.25,.20),(.22,.52),(.73,.42),(.30,.83),(.74,.77),(.22,1.12),(.76,1.13),(.5,1.40)], [(0,1),(0,2),(1,3),(2,4),(3,4),(3,5),(4,6),(5,7),(6,7)]),
    ("Bridge", [(.5,.18),(.20,.43),(.80,.43),(.50,.64),(.24,.86),(.76,.86),(.50,1.08),(.20,1.29),(.80,1.29),(.5,1.46)], [(0,1),(0,2),(1,3),(2,3),(3,4),(3,5),(4,6),(5,6),(6,7),(6,8),(7,9),(8,9)]),
    ("Loop", [(.5,.17),(.20,.36),(.80,.36),(.16,.69),(.84,.69),(.22,1.03),(.78,1.03),(.5,1.45),(.5,.64),(.5,1.12)], [(0,1),(0,2),(1,3),(2,4),(3,5),(4,6),(5,7),(6,7),(3,8),(4,8),(5,9),(6,9),(8,9)]),
    ("Pincer", [(.5,.18),(.20,.40),(.80,.40),(.25,.68),(.75,.68),(.50,.84),(.20,1.00),(.80,1.00),(.25,1.27),(.75,1.27),(.5,1.46)], [(0,1),(0,2),(1,3),(2,4),(3,5),(4,5),(3,6),(4,7),(5,6),(5,7),(6,8),(7,9),(8,10),(9,10)]),
    ("Windward", [(.5,.17),(.20,.37),(.80,.37),(.20,.65),(.80,.65),(.50,.78),(.20,.94),(.80,.94),(.20,1.23),(.80,1.23),(.50,1.45)], [(0,1),(0,2),(1,3),(2,4),(3,5),(4,5),(3,6),(4,7),(5,6),(5,7),(6,8),(7,9),(8,10),(9,10)]),
    ("Switchback", [(.5,.17),(.19,.37),(.81,.37),(.19,.66),(.50,.61),(.81,.66),(.19,.96),(.50,.88),(.81,.96),(.19,1.25),(.50,1.16),(.81,1.25),(.5,1.45)], [(0,1),(0,2),(1,3),(2,5),(3,4),(4,5),(3,6),(5,8),(6,7),(7,8),(6,9),(8,11),(9,10),(10,11),(9,12),(11,12)]),
    ("Shortcut", [(.22,.20),(.53,.21),(.80,.43),(.51,.58),(.20,.56),(.20,.89),(.50,.91),(.80,.81),(.80,1.14),(.50,1.23),(.21,1.24),(.5,1.46)], [(0,1),(1,2),(2,3),(3,4),(4,5),(5,6),(6,7),(7,8),(8,9),(9,10),(9,11),(10,11)]),
    ("Tripwire", [(.5,.17),(.21,.39),(.79,.39),(.21,.70),(.79,.70),(.50,.85),(.21,1.02),(.79,1.02),(.21,1.27),(.79,1.27),(.5,1.46)], [(0,1),(0,2),(1,3),(2,4),(3,5),(4,5),(5,6),(5,7),(6,8),(7,9),(8,10),(9,10)]),
    ("High ground", [(.20,.19),(.52,.20),(.81,.36),(.22,.53),(.55,.55),(.82,.72),(.20,.87),(.51,.86),(.23,1.20),(.80,1.08),(.52,1.17),(.81,1.40),(.42,1.45)], [(0,1),(0,3),(1,2),(1,4),(2,5),(3,4),(3,6),(4,7),(5,7),(5,9),(6,8),(7,10),(8,10),(9,11),(10,11),(10,12)]),
    ("Crossroads", [(.5,.16),(.18,.33),(.82,.33),(.18,.60),(.50,.50),(.82,.60),(.18,.87),(.50,.78),(.82,.87),(.18,1.14),(.50,1.05),(.82,1.14),(.18,1.41),(.50,1.45),(.82,1.41)], [(0,1),(0,2),(0,4),(1,3),(2,5),(3,4),(4,5),(3,6),(4,7),(5,8),(6,7),(7,8),(6,9),(7,10),(8,11),(9,10),(10,11),(9,12),(10,13),(11,14),(12,13),(13,14)]),
]
MODES = ["conquest", "capture_flag", "home_flags", "alliances"]
CHAPTERS = ["Take ground", "Race for the flag", "Protect your flag", "Choose your allies"]
CONCEPTS = [
    (0, "Capture", "Tap your green node, then a linked node to send troops. Match the troops and shields to capture it. Take the red nodes to win."),
    (1, "Keep a reserve", "Select a node and tap Send half. Keep a reserve while the rest takes the next node. Pause and the speed slider give you time to plan."),
    (3, "Grow", "Grow costs 10 troops and doubles production, but removes shields. Invest on a safe node, then use its troops to push forward."),
    (7, "Defend", "Defend costs 10 and replaces Grow. Its shields absorb attacks and recover faster. Hold the bridge while troops grow behind it."),
    (7, "Read the opponent", "A triangular flag marks Simple AI. Watch where it sends troops, then take a node it has left weak."),
    (7, "Two kinds of trouble", "A crown marks Savvy AI: it keeps reserves and combines attacks. A circle marks Silly AI: some orders are erratic. Protect both approaches."),
    (15, "Follow the arrows", "Arrows mark a directional route: twice the normal speed forward, 0.3 times backward. Send along the arrows and return by another route."),
    (15, "The long way round", "A slow reverse route can expose your home. Use the side path to reinforce the front, and keep a reserve on the return side."),
    (31, "Dig a shortcut", "Actions > Route blueprint, then tap a second node. Send troops from the first node to dig it. A normal length consumes about 80; the number is the work left."),
    (63, "Set a bomb", "Buy a bomb for 20 at a Grow node. Actions > Place bomb, then tap your moving fleet. Two troops set it; the first hostile fleet crossing takes 12 damage."),
    (127, "Artillery", "Actions > Artillery costs 20 and replaces the node's upgrade. Fire within three usual route lengths: a shot costs 20, deals 10 damage and reloads in 5 game seconds. Guns never capture."),
    (127, "Use the whole board", "Choose a front, keep a reserve and look for an unguarded flank. Tools open options, but ordinary troop movement still wins ground."),
]
TACTICS = ["Secure the first foothold before splitting your army.", "Choose which arm to grow and which to defend.", "Keep a rear supply lane on one bank.", "Hold the bridge; attack from both sides together.", "Use the loop to avoid the strongest garrison.", "Arrange two arrivals instead of feeding one losing attack.", "Exploit the fast direction before the defender can react.", "Treat the slow reverse link as a commitment.", "Compare a short dig with the cost of fighting round the bend.", "Keep a moving escort where the next enemy wave will arrive.", "Place a gun where it can reach more than one useful target.", "Take one useful junction without abandoning the other front."]


def make_level(chapter, number):
    # Keep geometric complexity increasing inside every mode. Objective, fronts,
    # mirrored banks and directed edges change how the same tactical motif plays.
    layout_index = number
    name, coords, edges = LAYOUTS[layout_index]
    coords, edges = list(coords), list(edges)
    if chapter % 2:
        coords = [(round(1-x, 3), y) for x, y in coords]
    features, concept, brief = CONCEPTS[number] if chapter == 0 else (127, "", "")
    mode = MODES[chapter]
    start, finish = 0, max(range(len(coords)), key=lambda i: coords[i][1])
    powers = [28,30,34,40,42,48,48,54,170,65,110,75]
    home_power = powers[number] if chapter == 0 else 38 + number * 3
    enemy_power = (8 + number * 2) if chapter == 0 else 22 + number * 3
    if number >= 10:
        home_power += 22
    owners = {start: "green", finish: "red"}
    def hops(source):
        distances = {source: 0}
        queue = deque([source])
        while queue:
            at = queue.popleft()
            for a,b in edges:
                neighbor = b if a == at else a if b == at else None
                if neighbor is not None and neighbor not in distances:
                    distances[neighbor] = distances[at] + 1
                    queue.append(neighbor)
        return distances
    # Later chapters have a third faction. Alliance puzzles always need a real common enemy.
    if chapter == 3 or (chapter > 0 and number >= 6) or (chapter == 0 and number >= 5):
        distances = [hops(i) for i in owners]
        candidate = max((i for i in range(len(coords)) if i not in owners),
                        key=lambda i: (min(d[i] for d in distances), coords[i][0]))
        owners[candidate] = "yellow"
    if chapter == 0 and number >= 5 and number != 8:
        home_power += 12
    if chapter == 1:
        home_power = 30 + min(number,6)*2
    # An exposed second front needs a visible rear supply source, rather than faster input.
    supply_levels = {5,8,11,15,18,19,20,21,22,23,26,29,30,40,41,43,44,45,46,47}
    index = chapter * 12 + number
    supply = None
    if index in supply_levels:
        neighbors = [b if a == start else a for a,b in edges if a == start or b == start]
        supply = min((i for i in neighbors if i not in owners), key=lambda i: coords[i][1], default=None)
        if supply is not None: owners[supply] = "green"
    if chapter == 3:
        enemy_power += 22  # stronger rival makes cooperation useful; it is shown, never a hidden bonus.
    flag = None
    if mode == "capture_flag":
        distances = [hops(i) for i in owners if i != supply]
        flag = min((i for i in range(len(coords)) if i not in owners),
                   key=lambda i: (max(0, 2-min(d[i] for d in distances)),
                                  max(d[i] for d in distances)-min(d[i] for d in distances),
                                  sum(d[i] for d in distances), abs(coords[i][1]-.8)))
    nodes = []
    for i, (x,y) in enumerate(coords):
        owner = owners.get(i, "neutral")
        supply_power = 40 if index in (11,23) else 20
        troops = (supply_power if i == supply else home_power) if owner == "green" else enemy_power if owner == "red" else 24 + number if owner == "yellow" else 2 + number // 4
        kind = "empty"
        shield = 0
        if i == supply: kind = "economic"
        if chapter == 2 and i in owners and i != supply: kind, shield = "defensive", 14
        if i == flag: troops = 50
        if chapter == 0 and number == 9 and i == start: kind = "economic"
        if number >= 8 and i == finish and (chapter != 0 or number >= 10): kind = "defensive"
        nodes.append(dict(owner=owner, type=kind, troops=troops, shield=shield, x=x, y=y))
    directed = []
    if features & 8:
        for k, (a,b) in enumerate(edges):
            if (k + number + chapter) % 4 == 0:
                if flag is not None:
                    to_flag = hops(flag)
                    if to_flag[a] < to_flag[b]: a,b = b,a
                elif (number + k) % 3 == 0: a,b = b,a
                directed.append((a,b))
    rows = []
    for a,b in edges:
        direction = next(((x,y) for x,y in directed if {x,y} == {a,b}), None)
        if direction: a,b = direction
        rows.append(dict(a=a,b=b,directional=bool(direction)))
    if chapter == 0 and number == 10:
        # A funded forward position can disable the exposed economy across a gap.
        # The lesson shows a useful gun target before asking players to design a siege.
        nodes[6].update(owner="green",type="empty",troops=60,shield=0)
        yellow = next(n for n in nodes if n["owner"] == "yellow")
        yellow.update(type="economic",troops=8,shield=0)
    if chapter == 1 and number == 8:
        lengths = sorted(math.dist(coords[a], coords[b]) for a,b in edges)
        cost = max(80, math.ceil(80 * math.dist(coords[start],coords[flag]) / lengths[len(lengths)//2]))
        rows.append(dict(a=start,b=flag,directional=True,builder="green",cost=cost,spent=cost-80))
        nodes[start]["troops"] = 125
        nodes[start]["type"] = "economic"
    difficulty = "easy" if (chapter == 0 and number < 5) or (chapter > 0 and number < 3) else "hard" if chapter >= 2 and number >= 10 else "normal"
    ai = {"red": "simple" if number < 5 else "savvy", "yellow": "silly" if number < 8 else "simple"}
    if chapter == 3: ai = {"red":"savvy", "yellow":"simple"}
    index = chapter * 12 + number
    if chapter > 0 and number == 0:
        concept = ["", "Neutral flag", "Starting flags", "Alliances"][chapter]
        brief = ["", "The larger neutral node starts with 50 troops. The first player to capture it wins immediately. Build enough force without leaving a route to your home open.",
                 "Each starting node has a flag. Capture any enemy flag to win immediately. You do not need to bring it home. Keep your own starting node defended.",
                 "Use Pacts to offer or accept an alliance. Allied arrivals add 70% of their troops without capturing. Either side can break a pact forever. Mutually allied survivors win together."][chapter]
    title = name if chapter == 0 else ["", "Beacon", "Hold", "Pact"][chapter] + " " + name
    hint = TACTICS[layout_index]
    if chapter == 0 and number == 10:
        hint = "Convert the forward 60-troop node to artillery. Fire at the weak yellow Grow node across the gap to strip its upgrade. Send troops around the links to capture it."
    if chapter == 3 and number == 11:
        hint = "Offer yellow a pact early, while red is still the shared threat. Supply the common front. Breaking a pact makes that pair hostile for the rest of the match."
    if chapter == 1 and number == 8:
        title = "Unfinished crossing"
        hint = "The route needs 80 more diggers. Combine your two armies to finish it and beat the flag's 50 troops. Half your starting army will not finish the route."
    map_code = "opacha_level_code#map_name:" + title + "#game_rules:" + difficulty + " false " + mode + " " + str(bool(directed)).lower() + " " + str(features) + "#ai_types:"
    map_code += ",".join(owner + " " + ai.get(owner,"simple") for owner in owners.values() if owner != "green")
    map_code += "#general:tiny#planets:" + ",".join(f"{n['owner']} {n['type']} {n['troops']} {n['x']} {n['y']} 0 {n['shield']}" for n in nodes)
    map_code += "#links:" + ",".join(f"neutral {coords[r['a']][0]} {coords[r['a']][1]} {coords[r['b']][0]} {coords[r['b']][1]} {str(r['directional']).lower()} {r.get('builder','none')} {r.get('cost',0)} {r.get('spent',0)}" for r in rows)
    map_code += "#tactics:" + (f"flag {flag} none;" if flag is not None else "") + "#"
    return dict(index=index, chapter=chapter, title=title, mode=mode, layout=name, concept=concept, brief=brief,
                hint=hint, features=features, difficulty=difficulty, nodes=nodes, routes=rows, flag=flag, code=map_code)


def main():
    levels = [make_level(chapter, number) for chapter in range(4) for number in range(12)]
    destination = ROOT / "assets/campaign/levels.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(dict(version=1, chapters=CHAPTERS, levels=levels), indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(levels)} authored maps to {destination}")


if __name__ == "__main__":
    main()
