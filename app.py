from flask import Flask, jsonify, render_template, request
from database import init_db, seed_db, get_db
from recommender import recommend

app = Flask(__name__)
app.config["JSON_SORT_KEYS"] = False

with app.app_context():
    init_db()
    seed_db()

@app.get("/")
def index():
    return render_template("index.html")

@app.get("/api/careers")
def careers():
    db = get_db()
    rows = db.execute("SELECT id, name, description FROM careers ORDER BY name").fetchall()
    return jsonify([dict(row) for row in rows])

@app.get("/api/skills")
def skills():
    db = get_db()
    rows = db.execute("SELECT id, name, category FROM skills ORDER BY category, name").fetchall()
    return jsonify([dict(row) for row in rows])

@app.post("/api/recommend")
def recommendations():
    payload = request.get_json(silent=True) or {}
    errors = []
    name = str(payload.get("name", "")).strip()
    department = str(payload.get("department", "")).strip()
    interests = [str(x).strip().lower() for x in payload.get("interests", []) if str(x).strip()]
    skill_names = [str(x).strip().lower() for x in payload.get("skills", []) if str(x).strip()]
    try:
        cgpa = float(payload.get("cgpa", 0))
    except (ValueError, TypeError):
        cgpa = -1
    if not name: errors.append("Name is required")
    if not department: errors.append("Department is required")
    if not 0 <= cgpa <= 10: errors.append("CGPA must be between 0 and 10")
    if not skill_names: errors.append("Select at least one skill")
    if errors: return jsonify({"errors": errors}), 400
    result = recommend(name, department, cgpa, skill_names, interests)
    return jsonify(result)

if __name__ == "__main__":
    app.run(debug=True)
