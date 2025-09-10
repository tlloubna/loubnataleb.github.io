import sqlite3

# Create connection and ensure both feedback and danger_zones tables exist
def create_connection():
    conn = sqlite3.connect('data/feedback.db')
    cursor = conn.cursor()
    
    # Create feedback table if it doesn't exist
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS feedback (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            address TEXT,
            rating INTEGER,
            lat REAL,
            lon REAL
        )
    """)
    
    # Create danger_zones table if it doesn't exist
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS danger_zones (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            lat REAL,
            lon REAL,
            avg_rating REAL
        )
    """)
    conn.commit()
    return conn

# Function to add new feedback
def add_feedback(conn, address, rating, coords):
    cursor = conn.cursor()
    cursor.execute("INSERT INTO feedback (address, rating, lat, lon) VALUES (?, ?, ?, ?)", 
                   (address, rating, coords[0], coords[1]))
    conn.commit()
    
    # After inserting feedback, update the average rating for the zone
    update_zone_average(conn, coords[0], coords[1])

# Function to calculate and update the average rating of a location
def update_zone_average(conn, lat, lon):
    cursor = conn.cursor()
    
    # Calculate the average rating for the given location
    cursor.execute("""
        SELECT AVG(rating) FROM feedback WHERE lat = ? AND lon = ?
    """, (lat, lon))
    avg_rating = cursor.fetchone()[0]
    
    # Check if the location already exists in danger_zones
    cursor.execute("""
        SELECT id FROM danger_zones WHERE lat = ? AND lon = ?
    """, (lat, lon))
    zone = cursor.fetchone()
    
    if zone:
        # Update the existing record
        cursor.execute("""
            UPDATE danger_zones SET avg_rating = ? WHERE lat = ? AND lon = ?
        """, (avg_rating, lat, lon))
    else:
        # Insert a new record
        cursor.execute("""
            INSERT INTO danger_zones (lat, lon, avg_rating) VALUES (?, ?, ?)
        """, (lat, lon, avg_rating))
    
    conn.commit()

# Function to retrieve dangerous zones (e.g., average rating between 0 and 3)
def get_danger_zones(conn):
    cursor = conn.cursor()
    cursor.execute("SELECT lat, lon, avg_rating FROM danger_zones WHERE avg_rating <= 3")
    rows = cursor.fetchall()
    return [{'lat': row[0], 'lon': row[1], 'avg_rating': row[2]} for row in rows]

def add_user(conn, first_name, last_name, gender, disability_status, address, age, email, password):
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            first_name TEXT,
            last_name TEXT,
            gender TEXT,
            disability_status TEXT,
            address TEXT,
            age INTEGER,
            email TEXT,
            password TEXT
        )
    """)
    cursor.execute("INSERT INTO users (first_name, last_name, gender, disability_status, address, age, email, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", 
                   (first_name, last_name, gender, disability_status, address, age, email, password))
    conn.commit()
