import sqlite3
from flask import g

DATABASE = "skillsync.db"

def get_db():
    if "db" not in g:
        g.db = sqlite3.connect(DATABASE)
        g.db.row_factory = sqlite3.Row
    return g.db

def init_db():
    db = get_db()
    db.executescript("""
    CREATE TABLE IF NOT EXISTS skills(id INTEGER PRIMARY KEY, name TEXT UNIQUE NOT NULL, category TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS careers(id INTEGER PRIMARY KEY, name TEXT UNIQUE NOT NULL, description TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS career_skills(career_id INTEGER, skill_id INTEGER, importance INTEGER NOT NULL,
      PRIMARY KEY(career_id, skill_id), FOREIGN KEY(career_id) REFERENCES careers(id), FOREIGN KEY(skill_id) REFERENCES skills(id));
    CREATE TABLE IF NOT EXISTS profiles(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, department TEXT, cgpa REAL, interests TEXT, skills TEXT);
    """)
    db.commit()

def seed_db():
    db = get_db()
    skills = [("java", "Programming"), ("python", "Programming"), ("javascript", "Programming"),
              ("sql", "Database"), ("html", "Web"), ("css", "Web"), ("spring boot", "Backend"),
              ("data structures", "Computer Science"), ("algorithms", "Computer Science"),
              ("machine learning", "AI"), ("statistics", "AI"), ("git", "Tools"), ("communication", "Professional")]
    db.executemany("INSERT OR IGNORE INTO skills(name, category) VALUES (?, ?)", skills)
    careers = [("Java Backend Developer", "Builds reliable server-side applications and APIs."),
               ("Full Stack Developer", "Develops both browser interfaces and backend services."),
               ("Data Analyst", "Turns structured data into useful business insights."),
               ("Machine Learning Engineer", "Builds and deploys predictive machine learning systems."),
               ("Software Engineer", "Designs, implements, tests, and maintains software systems.")]
    db.executemany("INSERT OR IGNORE INTO careers(name, description) VALUES (?, ?)", careers)
    mapping = {"Java Backend Developer": [("java", 5), ("spring boot", 5), ("sql", 4), ("data structures", 4), ("algorithms", 3), ("git", 2)],
      "Full Stack Developer": [("javascript", 5), ("html", 4), ("css", 4), ("sql", 3), ("spring boot", 3), ("git", 2)],
      "Data Analyst": [("python", 5), ("sql", 5), ("statistics", 5), ("communication", 3), ("git", 1)],
      "Machine Learning Engineer": [("python", 5), ("machine learning", 5), ("statistics", 5), ("algorithms", 4), ("sql", 2)],
      "Software Engineer": [("data structures", 5), ("algorithms", 5), ("java", 3), ("python", 3), ("git", 2), ("communication", 2)]}
    for career, required in mapping.items():
        career_id = db.execute("SELECT id FROM careers WHERE name=?", (career,)).fetchone()[0]
        for skill, importance in required:
            skill_id = db.execute("SELECT id FROM skills WHERE name=?", (skill,)).fetchone()[0]
            db.execute("INSERT OR IGNORE INTO career_skills VALUES (?, ?, ?)", (career_id, skill_id, importance))
    db.commit()
