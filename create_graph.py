import json
from math import radians, sin, cos, sqrt, atan2

def haversine(coord1, coord2):
    R = 6371.0  # Radius of the Earth in kilometers
    lat1, lon1 = radians(coord1[1]), radians(coord1[0])
    lat2, lon2 = radians(coord2[1]), radians(coord2[0])
    dlon = lon2 - lon1
    dlat = lat2 - lat1
    a = sin(dlat / 2)**2 + cos(lat1) * cos(lat2) * sin(dlon / 2)**2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))
    distance = R * c
    return distance

# Replace with your actual connections
connections = {
    "A": ["B", "D"],
    "B": ["A", "C"],
    "C": ["D", "B", "F"],
    "D": ["A", "C", "E"],
    "E": ["D", "F"],
    "F": ["E", "C", "G"],
    "G": ["F", "H"],
    "H": ["G", "B", "C"]
}

# Coordinates from your GeoJSON
coordinates = {
    "A": (3.078695625467816, 45.77682785036359),
    "B": (3.0816698937919114, 45.77719951573192),
    "C": (3.082066462902219, 45.775514036646854),
    "D": (3.079525942041812, 45.77486576189483),
    "E": (3.0800340462136546, 45.77369020445562),
    "F": (3.0824382464427345, 45.77372478002826),
    "G": (3.084978767303113, 45.773750711693566),
    "H": (3.084582198192834, 45.77588571077186)
}

graph = {}

# Create the graph with distances and coordinates
for point, neighbors in connections.items():
    graph[point] = {
        "coordinates": coordinates[point],
        "neighbors": {}
    }
    for neighbor in neighbors:
        distance = haversine(coordinates[point], coordinates[neighbor])
        graph[point]["neighbors"][neighbor] = distance

# Save the graph to a JSON file
with open('graph_with_coordinates.json', 'w') as f:
    json.dump(graph, f, indent=4)

# Add the function to load the graph
def load_graph(file_path):
    with open(file_path, 'r') as f:
        return json.load(f)
