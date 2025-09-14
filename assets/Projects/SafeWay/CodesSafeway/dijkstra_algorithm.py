import json
import heapq

def load_graph(file_path):
    with open(file_path, 'r') as f:
        return json.load(f)

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
    graph = load_graph('graph_with_coordinates.json')
    start = 'A'  # Replace with your start point
    end = 'G'    # Replace with your end point
    path, distance = dijkstra(graph, start, end)
    print("Shortest path from", start, "to", end, "is:", path)
    print("Total distance:", distance)
