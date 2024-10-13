import streamlit as st
import folium
from streamlit_folium import st_folium
from geopy.geocoders import Nominatim
import sqlite3
from database import create_connection, add_feedback, get_danger_zones
from map_utils import get_coordinates

# Title of the app
st.title("SafeWay - Navigation, Safety Feedback, and Itinerary")

# Input for start and destination addresses
start_address = st.text_input("Enter the starting address:")
destination_address = st.text_input("Enter the destination address:")

# Initialize map centered on Clermont-Ferrand
map_center = [45.7772, 3.0870]
folium_map = folium.Map(location=map_center, zoom_start=13)

# Get coordinates for both addresses
start_coords = get_coordinates(start_address) if start_address else None
destination_coords = get_coordinates(destination_address) if destination_address else None

# Show map with start and destination addresses
if start_coords:
    folium.Marker(location=start_coords, popup="Start Location", icon=folium.Icon(color='blue')).add_to(folium_map)
if destination_coords:
    folium.Marker(location=destination_coords, popup="Destination Location", icon=folium.Icon(color='blue')).add_to(folium_map)

# Draw a polyline between start and destination if both are available
if start_coords and destination_coords:
    folium.PolyLine(locations=[start_coords, destination_coords], color='blue', weight=5).add_to(folium_map)

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
        
        # Add markers for danger zones with color based on average rating
        folium.Marker(
            location=[zone['lat'], zone['lon']],
            popup=f"Avg Rating: {zone['avg_rating']:.2f}",
            icon=folium.Icon(color=color)
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
