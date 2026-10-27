from collections import defaultdict, deque
from heapq import heappush, heappop
from database import get_db

def recommend(name, department, cgpa, skill_names, interests):
    db = get_db()
    profile = db.execute("INSERT INTO profiles(name, department, cgpa, interests, skills) VALUES(?,?,?,?,?)",
        (name, department, cgpa, ",".join(interests), ",".join(skill_names)))
    db.commit()
    skill_map = {row["name"].lower(): row["id"] for row in db.execute("SELECT id,name FROM skills")}
    selected = {skill_map[s] for s in skill_names if s in skill_map}
    graph = defaultdict(list)
    rows = db.execute("SELECT career_id, skill_id, importance FROM career_skills").fetchall()
    for row in rows:
        graph[row["career_id"]].append((row["skill_id"], row["importance"]))
    heap = []
    for career in db.execute("SELECT id,name,description FROM careers"):
        required = graph[career["id"]]
        total = sum(weight for _, weight in required) or 1
        matched = [skill_id for skill_id, _ in required if skill_id in selected]
        missing_ids = [skill_id for skill_id, _ in required if skill_id not in selected]
        score = round(sum(weight for skill_id, weight in required if skill_id in selected) / total * 100, 1)
        interest_bonus = 3 if any(i in career["name"].lower() for i in interests) else 0
        heappush(heap, (-(score + interest_bonus), career["name"], career, matched, missing_ids))
    ranked = []
    while heap:
        neg_score, _, career, matched, missing_ids = heappop(heap)
        names = lambda ids: [db.execute("SELECT name FROM skills WHERE id=?", (sid,)).fetchone()[0] for sid in ids]
        ranked.append({"career": career["name"], "description": career["description"], "score": min(round(-neg_score, 1), 100),
                       "matched_skills": names(matched), "skill_gaps": names(missing_ids),
                       "roadmap": [{"step": i + 1, "skill": s, "action": f"Study {s} fundamentals and complete a small project"} for i, s in enumerate(names(missing_ids))]})
    return {"profile_id": profile.lastrowid, "student": {"name": name, "department": department, "cgpa": cgpa}, "recommendations": ranked}
