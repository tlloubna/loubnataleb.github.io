import streamlit as st
import folium
from streamlit_folium import st_folium
from geopy.geocoders import Nominatim
import sqlite3
from database import create_connection, add_feedback, get_danger_zones, add_user
import re
from map_utils import get_coordinates
from dijkstra_with_danger import dijkstra, adjust_graph_weights
from create_graph import load_graph

# Initialize session state for user registration
if 'registration_successful' not in st.session_state:
    st.session_state.registration_successful = False

# Title of the app (centered)
st.markdown("<h1 style='text-align: center;'>SafeWay</h1>", unsafe_allow_html=True)

# User registration section
if not st.session_state.registration_successful:
    st.subheader("User Registration")

    with st.form("registration_form"):
        first_name = st.text_input("First Name")
        last_name = st.text_input("Last Name")
        gender = st.selectbox("Gender", ["Male", "Female", "Other"])
        disability_status = st.selectbox("Disability Status", ["None", "Physical", "Mental", "Other"])
        address = st.text_input("Home Address")
        age = st.number_input("Age", min_value=1, max_value=120, step=1)
        email = st.text_input("Email")
        password = st.text_input("Password", type="password")

        # Validate Email Format
        if email and not re.match(r"[^@]+@[^@]+\.[^@]+", email):
            st.error("Please enter a valid email address.")

        # Submit button for registration
        submitted = st.form_submit_button("Register")
        if submitted:
            if email and re.match(r"[^@]+@[^@]+\.[^@]+", email):  # Additional check
                conn = create_connection()
                add_user(conn, first_name, last_name, gender, disability_status, address, age, email, password)
                st.session_state.registration_successful = True  # Mark registration as successful
                st.success("Registration successful! You can now proceed to the map.")
            else:
                st.error("Registration failed. Please check the inputs.")
else:
    # Load graph data
    graph_data = load_graph('graph_with_coordinates.json')

    # Coordinates for A and G to bypass Nominatim
    A_coords = (45.77682785036359, 3.078695625467816)
    G_coords = (45.773750711693566, 3.084978767303113)

    adjusted_graph = adjust_graph_weights(graph_data)

    

    # Input for start and destination addresses
    start_address = st.text_input("Enter the starting address (or type 'A'):")
    destination_address = st.text_input("Enter the destination address (or type 'G'):")

    # Initialize map centered on Clermont-Ferrand
    map_center = [45.7772, 3.0870]
    folium_map = folium.Map(location=map_center, zoom_start=13)

    # Bypass Nominatim for specific A and G coordinates
    if start_address == 'A':
        start_coords = A_coords
    else:
        start_coords = get_coordinates(start_address) if start_address else None

    if destination_address == 'G':
        destination_coords = G_coords
    else:
        destination_coords = get_coordinates(destination_address) if destination_address else None

    # Show map with start and destination addresses
    if start_coords:
        folium.Marker(location=start_coords, popup="Start Location", icon=folium.Icon(color='blue')).add_to(folium_map)
    if destination_coords:
        folium.Marker(location=destination_coords, popup="Destination Location", icon=folium.Icon(color='blue')).add_to(folium_map)

    # If both start and destination coordinates are available, run Dijkstra algorithm
    if start_coords and destination_coords:
        # Find the corresponding points in the graph
        start_point = 'A' if start_coords == A_coords else None
        destination_point = 'G' if destination_coords == G_coords else None

        if start_point and destination_point:
            path, total_distance = dijkstra(adjusted_graph, start_point, destination_point)
            if path:
                st.write(f"Shortest path: {path}")
                st.write(f"Total adjusted distance: {total_distance:.2f} meters")

                # Draw the path on the map using PolyLine
                folium.PolyLine(locations=[adjusted_graph[pt]["coordinates"][::-1] for pt in path], color='blue', weight=5).add_to(folium_map)
            else:
                st.error("No path found.")
        else:
            st.error("Invalid start or destination for Dijkstra.")

    # Display danger zones (locations with avg rating between 0 and 3)
    danger_zones = get_danger_zones(create_connection())
    if danger_zones:
        for zone in danger_zones:
            color = 'green'  # Default color
            if zone['avg_rating'] < 1:
                color = 'green'
            elif 1 <= zone['avg_rating'] < 2:
                color = 'orange'
            else:  # 2 or more
                color = 'red'
            folium.Circle(
                           location=[zone['lat'], zone['lon']],
                           radius=100,  # Rayon en mètres (ajuste selon l'échelle de ta carte)
                           color=color,
                           fill=True,
                           fill_color=color,
                           fill_opacity=0.4,  # Niveau de transparence (0 = complètement transparent, 1 = complètement opaque)
                           ).add_to(folium_map)

    # Show the map with start, destination, and danger zones
    st_folium(folium_map, width=700, height=500)

    # Feedback Section
    st.subheader("Rate the safety of the start or destination location:")
    rating = st.slider("Danger Rating (0 - No Danger, 3 - Very Dangerous)", 0, 3, 1)

    # Dropdown to select whether to rate the start or destination address
    location_choice = st.selectbox("Which location are you rating?", ["Start", "Destination"])
    if location_choice == "Start":
        selected_coords = start_coords
    elif location_choice == "Destination":
        selected_coords = destination_coords

    # Submit feedback for the selected location
    if st.button("Submit Feedback"):
        if selected_coords:
            address = start_address if location_choice == "Start" else destination_address
            conn = create_connection()
            add_feedback(conn, address, rating, selected_coords)
            st.success(f"Feedback for {location_choice} location submitted successfully!")
        else:
            st.error(f"Please enter a valid {location_choice.lower()} address.")
