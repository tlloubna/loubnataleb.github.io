#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri May 31 09:38:25 2024

@author: loubna
"""

import pandas as pd
import glob
import os
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D  # Fonction pour afficher des graphiques en 3D
import numpy as np

# Fonction pour extraire les paramètres d'un fichier spécifique
def extract_parameters(file_path, viable):
    try:
        if viable:
            # Charger les données pour les fichiers viables
            data = pd.read_csv(file_path, delim_whitespace=True, skiprows=1)
            data = data.iloc[:-1]  # Supprimer la dernière ligne
        else:
            # Charger les données pour les fichiers non viables
            data = pd.read_csv(file_path, delim_whitespace=True, comment="#", header=None)
            data.columns = ['year', 'idPatch', 'nbTree_ha', 'nbTree_patch', 'basalAreaTot_ha', 
                            'basalAreaPSyl', 'basalAreaQPet', 'basalAreaTCor', 'nbDeadTree_ha', 
                            'basalAreaDead_ha', 'mortalityRate', 'coefGini', 'nbOfSpecies', 'vol_prod_ha']
        
        # Récupérer les années uniques et trier
        years = sorted(data.iloc[:, 0].unique())
        # Filtrer les données pour la dernière année
        data_end = data[data.iloc[:, 0] == years[-1]]
        # Calculer la surface terrière pour la dernière année
        G = [gi / 10 for gi in data_end.iloc[:, 4]]
        # Filtrer les données pour les cinq dernières années
        last_5_years = years[-5:]
        data_last_5_years = data[data.iloc[:, 0].isin(last_5_years)]
        # Calculer l'indice de Gini moyen sur les cinq dernières années
        gini = [gi for gi in data_last_5_years.iloc[:, 11]]
        
        # Stocker les paramètres extraits dans un dictionnaire
        params = {
            'surface_terriere': sum(G),
            'gini_index': np.mean(gini),
            'mortality_rate': data_end.iloc[:, 10].astype(float).mean(),
            'num_species': data_end.iloc[:, 12].astype(float).mean(),
            'wood_harvested': data_end.iloc[:, 13].astype(float).mean(),
            'num_trees': data_end.iloc[:, 2].astype(float).mean(),
            'year': data_end.iloc[:, 0].nunique(),
            'file_path': file_path
        }
        
        return params
    except Exception as e:
        print(f"Error reading parameters from file {file_path}: {e}")
        return None

# Fonction pour charger et traiter une liste de fichiers
def load_and_process_files(file_paths, viable=True):
    all_params = []
    for file_path in file_paths:
        params = extract_parameters(file_path, viable)
        if params:
            all_params.append(params)
    return pd.DataFrame(all_params)

# Chemin vers le dossier contenant les fichiers de sortie :Regarder le chemin ou t'a les données 
output_1000 = "/home/loubna/loubna_data_forceeps/data_climat_bern/dataGAparameter500_1000/output-FBrin3_rcp_10_10_5_0.05_1000.txt"
# Liste des fichiers viables et non viables
viables_files = glob.glob(os.path.join(output_1000, 'individu*_evolConstraints.txt'))
non_viable_files = glob.glob(os.path.join(output_1000, 'individu*_evolConstraints_nonViable.txt'))

# Charger et traiter les fichiers viables et non viables
df_viable = load_and_process_files(viables_files, viable=True)
df_non_viable = load_and_process_files(non_viable_files, viable=False)

# Suivre les fichiers colorés en bleu
blue_files = []

# Initialiser la figure pour le graphique 3D
fig = plt.figure(figsize=(11, 10))
ax = fig.add_subplot(111, projection='3d')  # Création d'un sous-graphique en 3D

# Extraire les données pour le graphique
z_viable = df_viable["wood_harvested"]
y_viable = df_viable['surface_terriere']
x_viable = df_viable['gini_index']
z_Nviable = df_non_viable["wood_harvested"]
y_Nviable = df_non_viable['surface_terriere']
x_Nviable = df_non_viable['gini_index']

# Définir les couleurs pour les points non viables
colors_non_viable = []
x_Nviable_gini = []
y_Nviable_gini = []
z_Nviable_gini = []

x_Nviable_basal = []
y_Nviable_basal = []
z_Nviable_basal = []

x_Nviable_other = []
y_Nviable_other = []
z_Nviable_other = []
count_gini_non_viable = 0
count_basal_non_viable = 0
aure = 0

# Assigner des couleurs spécifiques en fonction des critères
for index, (gini, basal, wood_harvested) in enumerate(zip(x_Nviable, y_Nviable, z_Nviable)):
    if gini < 0.25 or gini > 0.75:
        colors_non_viable.append('purple')
        x_Nviable_gini.append(gini)
        y_Nviable_gini.append(basal)
        z_Nviable_gini.append(wood_harvested)
        count_gini_non_viable += 1
    elif basal < 10:
        colors_non_viable.append('orange')
        x_Nviable_basal.append(gini)
        y_Nviable_basal.append(basal)
        z_Nviable_basal.append(wood_harvested)
        count_basal_non_viable += 1
    else:
        colors_non_viable.append('blue')
        x_Nviable_other.append(gini)
        y_Nviable_other.append(basal)
        z_Nviable_other.append(wood_harvested)
        blue_files.append(df_non_viable.iloc[index]['file_path'])
        aure += 1

# Tracer les points pour les individus viables et non viables
ax.scatter(x_viable, y_viable, z_viable, label='Individus Viables', c="red")
ax.scatter(x_Nviable_gini, y_Nviable_gini, z_Nviable_gini, label='Non Viables (Gini)', c='purple')
ax.scatter(x_Nviable_basal, y_Nviable_basal, z_Nviable_basal, label='Non Viables (Surface terrière)', c='orange')
ax.scatter(x_Nviable_other, y_Nviable_other, z_Nviable_other, label='Non Viables (Autres)', c='blue')

# Configurer les axes et le titre du graphique
plt.title("Individus Viables et Non Viables n_init=1000")
ax.set_xlabel('GINI')
ax.set_ylabel('Surface terrière (m2/ha)')
ax.set_zlabel('Bois récolté (m3/ha)', rotation=140)

# Définir l'angle de vue pour le graphique 3D
ax.view_init(elev=20, azim=30)  # Élévation de 20 degrés, azimut de 30 degrés
plt.legend(bbox_to_anchor=(1.05, 1), loc='upper right')
plt.tight_layout()
plt.show()

# Afficher les fichiers colorés en bleu
print("Fichiers colorés en bleu :")
for file in blue_files:
    print(file)
