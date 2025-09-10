#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Jun  7 06:54:04 2024

@author: loubna
"""

import subprocess
import time
import pandas as pd
import matplotlib.pyplot as plt
import os
import itertools
import glob
import gc

# Fonction pour analyser un fichier de données de fitness.
# Elle lit les données en ignorant les 18 premières lignes et retourne un DataFrame contenant les données.
def parse_fitness_file(file_path):
    try:
        fitness_data = pd.read_csv(file_path, delim_whitespace=True, skiprows=18)
        return fitness_data
    except Exception as e:
        print(f"Error parsing fitness file: {e}")
        return None

# Fonction pour analyser l'évolution de la fitness sur plusieurs générations.
# Elle retourne une liste de générations uniques et un DataFrame contenant le maximum de fitness par génération.
def analyze_fitness(fitness_data):
    if fitness_data is not None and '#Generation' in fitness_data.columns:
        generations = fitness_data['#Generation'].unique()
        max_fitness_per_gen = fitness_data.groupby('#Generation')['Fitness'].max().reset_index()
        return generations, max_fitness_per_gen
    else:
        print("Fitness data is invalid or missing 'Generation' column")
        return [], []

# Définition des chemins d'accès des différents fichiers de fitness à analyser
fitness_file_path_1= '/home/loubna/loubna_data_forceeps/data_climat_bern/data_test_time1000_3fois_500/output-FBrin3_rcp_10_10_5_0.05_500_0.txt/evolFitness.txt'
fitness_file_path_2= '/home/loubna/loubna_data_forceeps/data_climat_bern/data_test_time1000_3fois_500/output-FBrin3_rcp_10_10_5_0.05_1000_0.txt/evolFitness.txt'

# Analyse des fichiers de fitness
fitness_data_1 = parse_fitness_file(fitness_file_path_1)
fitness_data_2 = parse_fitness_file(fitness_file_path_2)


# Analyse des générations et calcul du maximum de fitness par génération pour chaque fichier
generations1, max_fitness_per_gen1 = analyze_fitness(fitness_data_1)
generations2, max_fitness_per_gen2 = analyze_fitness(fitness_data_2)


# Tracé des courbes de fitness maximale en fonction des générations pour chaque fichier
plt.plot(max_fitness_per_gen1["#Generation"], max_fitness_per_gen1["Fitness"], label="init_G=500 & iter=0")
plt.plot(max_fitness_per_gen2["#Generation"], max_fitness_per_gen2["Fitness"], label="init_G=1000 & iter=0")

# Configuration des étiquettes des axes et du titre du graphique
plt.xlabel("Generations")
plt.ylabel("Best Fitness")
plt.title("Best Fitness vs Generations")
plt.legend()  # Affichage de la légende
plt.grid()    # Affichage de la grille
plt.show()    # Affichage du graphique
