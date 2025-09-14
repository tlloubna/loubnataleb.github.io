import folium
from geopy.geocoders import Nominatim

def get_coordinates(address):
    geolocator = Nominatim(user_agent="safeway_app")
    location = geolocator.geocode(address)
    if location:
        return (location.latitude, location.longitude)
    return None

def show_map(coords):
    folium_map = folium.Map(location=coords, zoom_start=14)
    folium.Marker(location=coords, popup="Selected Location").add_to(folium_map)
    return folium_map
