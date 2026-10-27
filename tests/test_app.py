import os, tempfile, pytest
os.chdir(os.path.dirname(os.path.dirname(__file__)))
from app import app

@pytest.fixture()
def client(tmp_path):
    import database
    database.DATABASE = str(tmp_path / "test.db")
    with app.app_context():
        database.init_db(); database.seed_db()
    return app.test_client()

def test_catalogs(client):
    assert len(client.get('/api/skills').json) >= 10
    assert len(client.get('/api/careers').json) >= 5

def test_recommendation_ranks_and_finds_gaps(client):
    response = client.post('/api/recommend', json={'name':'Test Student','department':'CSE','cgpa':8.5,'skills':['java','spring boot','sql'],'interests':['backend']})
    assert response.status_code == 200
    data=response.json
    assert data['recommendations'][0]['career'] == 'Java Backend Developer'
    assert data['recommendations'][0]['score'] > 50
    assert 'data structures' in data['recommendations'][0]['skill_gaps']
    assert data['recommendations'][0]['roadmap']

def test_validation(client):
    response=client.post('/api/recommend', json={'name':'','department':'','cgpa':11,'skills':[]})
    assert response.status_code == 400
    assert len(response.json['errors']) == 4
