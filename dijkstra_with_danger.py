import json
import heapq
import sqlite3

def load_graph(file_path):
    with open(file_path, 'r') as f:
        return json.load(f)

def get_danger_value(lat, lon):
    conn = sqlite3.connect('data/feedback.db')  # Adjust path if necessary
    cursor = conn.cursor()
    cursor.execute("SELECT avg_rating FROM danger_zones WHERE lat = ? AND lon = ?", (lat, lon))
    result = cursor.fetchone()
    conn.close()
    return result[0] if result else 0  # Return 0 if not found

def dijkstra(graph, start, end):
    # Fetch danger values and calculate alpha based on coordinates
    start_lat, start_lon = graph[start]["coordinates"]
    end_lat, end_lon = graph[end]["coordinates"]
    
    start_danger = get_danger_value(start_lat, start_lon)
    end_danger = get_danger_value(end_lat, end_lon)
    alpha = max(start_danger, end_danger)

    # Priority queue to hold (distance, vertex)
    queue = [(0, start)]
    distances = {vertex: float('infinity') for vertex in graph}
    distances[start] = 0
    previous_vertices = {vertex: None for vertex in graph}
    
    while queue:
        current_distance, current_vertex = heapq.heappop(queue)

        # If we reach the end vertex, we can build the path
        if current_vertex == end:
            path = []
            while current_vertex is not None:
                path.append(current_vertex)
                current_vertex = previous_vertices[current_vertex]
            return path[::-1], distances[end]  # Return reversed path and distance

        if current_distance > distances[current_vertex]:
            continue

        for neighbor, weight in graph[current_vertex]["neighbors"].items():
            # Adjust distance with the coefficient (alpha + 1)
            adjusted_distance = (weight * (alpha + 1))
            distance = current_distance + adjusted_distance

            # Only consider this new path if it's better
            if distance < distances[neighbor]:
                distances[neighbor] = distance
                previous_vertices[neighbor] = current_vertex
                heapq.heappush(queue, (distance, neighbor))

    return None, float('infinity')  # Return None if there is no path

# Example usage
if __name__ == "__main__":
    graph = load_graph('graph_with_coordinates.json')
    start = 'A'  # Replace with your start point
    end = 'G'    # Replace with your end point
    path, distance = dijkstra(graph, start, end)
    print("Shortest path from", start, "to", end, "is:", path)
    print("Total adjusted distance:", distance)
