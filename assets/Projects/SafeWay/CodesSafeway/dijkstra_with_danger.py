import json
import sqlite3
import heapq

# Function to load the graph from a JSON file
def load_graph(file_path):
    with open(file_path, 'r') as f:
        return json.load(f)

# Function to get the danger value (avg_rating) from the database based on latitude and longitude
def get_danger_value(lat, lon):
    conn = sqlite3.connect('data/feedback.db')  # Adjust path if necessary
    cursor = conn.cursor()
    cursor.execute("SELECT avg_rating FROM danger_zones WHERE lat = ? AND lon = ?", (lat, lon))
    result = cursor.fetchone()
    conn.close()
    return result[0] if result else 0  # Return 0 if no rating is found

# Function to calculate the alpha coefficient between two points based on danger values
def calculate_alpha(lat1, lon1, lat2, lon2):
    # Get danger values for both points
    danger1 = get_danger_value(lat1, lon1)
    danger2 = get_danger_value(lat2, lon2)
    
    # Alpha is the maximum danger value between the two points
    alpha = max(danger1, danger2)
    return alpha

# Function to adjust the graph's weights based on danger ratings
def adjust_graph_weights(graph):
    # Create a new adjusted graph
    adjusted_graph = {}

    # Loop through all vertices in the graph
    for vertex, data in graph.items():
        adjusted_graph[vertex] = {
            "coordinates": data["coordinates"],
            "neighbors": {}
        }

        # Loop through each neighbor and adjust the edge weight
        for neighbor, weight in data["neighbors"].items():
            lat1, lon1 = data["coordinates"]
            lat2, lon2 = graph[neighbor]["coordinates"]
            alpha = calculate_alpha(lat1, lon1, lat2, lon2)

            # Adjust weight by multiplying with (alpha + 1)
            adjusted_weight = weight * (alpha + 1)
            adjusted_graph[vertex]["neighbors"][neighbor] = adjusted_weight

    return adjusted_graph

# Dijkstra's algorithm implementation
def dijkstra(graph, start, end):
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

        # Loop through neighbors and compare distances
        for neighbor, weight in graph[current_vertex]["neighbors"].items():
            distance = current_distance + weight

            # Only consider this new path if it's better
            if distance < distances[neighbor]:
                distances[neighbor] = distance
                previous_vertices[neighbor] = current_vertex
                heapq.heappush(queue, (distance, neighbor))

    return None, float('infinity')  # Return None if there is no path

# Example usage
if __name__ == "__main__":
    # Load the unadjusted graph
    graph = load_graph('graph_with_coordinates.json')
    
    # Adjust graph with real-time danger values
    adjusted_graph = adjust_graph_weights(graph)

    # Define start and end points
    start = 'A'  # Replace with your start point
    end = 'G'    # Replace with your end point

    # Run Dijkstra on the adjusted graph
    path, distance = dijkstra(adjusted_graph, start, end)

    # Output the result
    print("Shortest path from", start, "to", end, "is:", path)
    print("Total adjusted distance:", distance)
