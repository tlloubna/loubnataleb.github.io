#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu May 30 14:38:19 2024

@author: loubna
"""
import subprocess
import time
import pandas as pd
import matplotlib.pyplot as plt
import os
from scipy.stats import ttest_ind
import numpy as np

def list_files_ending_with(directory_path, suffix):
    try:
        # Obtenir la liste de tous les fichiers dans le répertoire
        files = []
        for file in os.listdir(directory_path):
            if file.endswith(suffix):
                files.append(os.path.join(directory_path, file))
        return files
    except Exception as e:
        print(f"Error reading directory {directory_path}: {e}")
        return []

def extract_parameters(file_path, skip_last_n=0):
    try:
        data = pd.read_csv(file_path, delim_whitespace=True, skiprows=1)
        # Ignorer les dernières n lignes si nécessaire
        data=data.iloc[:len(data)-1]
        
        params = {
            'Basal_Area': data['basalAreaTot_ha'].mean(),
            'gini_index': data['coefGini'].mean(),
             #'mortality_rate': data['mortalityRate'].mean(),
            'num_species': data['nbOfSpecies'].mean(),
            'wood_harvested': data.iloc[-1, 13],
            'num_trees': pd.to_numeric(data["nbTree_patch"]).mean()
        }
        
        return params
    except Exception as e:
        print(f"Error reading parameters from file {file_path}: {e}")
        return None

dir_10_patches = '/home/loubna/loubna_data_forceeps/data_climat_bern/dataBasicBern/output-FBrin3_rcp_10.txt'
dir_30_patches = '/home/loubna/loubna_data_forceeps/data_climat_bern/dataBasicBern/output-FBrin3_rcp_30.txt'

files_10_patches = list_files_ending_with(dir_10_patches, 'evolConstraints.txt')
files_30_patches = list_files_ending_with(dir_30_patches, 'evolConstraints.txt')

all_params_10 = []
all_params_30 = []

# Nombre de lignes à ignorer à la fin des fichiers
skip_last_n = 1

for file in files_10_patches:
    params = extract_parameters(file, skip_last_n)
    params["wood_harvested"]= params["wood_harvested"]/10
    if params:
        all_params_10.append(params)

for file in files_30_patches:
    params = extract_parameters(file, skip_last_n)
    params["wood_harvested"]= params["wood_harvested"]/30
    if params:
        all_params_30.append(params)

df_10_patches = pd.DataFrame(all_params_10)
df_30_patches = pd.DataFrame(all_params_30)

means_10 = df_10_patches.mean()
stds_10 = df_10_patches.std()
means_30 = df_30_patches.mean()
stds_30 = df_30_patches.std()
print("m10:",means_10,'ecart:',stds_10)
print("m30:",means_30,"ecart :",stds_30)

# Création de graphiques séparés
fig, axes = plt.subplots(2, len(means_10), figsize=(10, 20))


#calculer l'erreur stadard 
n_samples_10 = len(df_10_patches)
std_error_10 = stds_10 / np.sqrt(n_samples_10)

# Calcul de l'erreur standard pour les données des 30 patches
n_samples_30 = len(df_30_patches)
std_error_30 = stds_30 / np.sqrt(n_samples_30)

# Liste des paramètres à comparer
parametres = df_10_patches.columns

# Diviser les paramètres en trois groupes pour les trois lignes
parametres_ligne1 = parametres[:2]
parametres_ligne2 = parametres[2:4]
parametres_ligne3 = parametres[4:]

# Créer trois lignes de graphiques à barres
fig, axs = plt.subplots(3, 2, figsize=(15, 15))

for i, parametres_ligne in enumerate([parametres_ligne1, parametres_ligne2, parametres_ligne3]):
    for j, parametre in enumerate(parametres_ligne):
        ax = axs[i, j]
        ax.bar([2, 4], [means_10[parametre], means_30[parametre]], yerr=[std_error_10[parametre], std_error_30[parametre]], tick_label=['10 patches', '30 patches'],color=["#87CEEB",'#FFA500'])
        ax.set_title(f'Comparaison de la moyenne de {parametre} avec erreur standard')
        ax.set_xlabel('Nombre de patches')
        ax.set_ylabel('Valeur moyenne')
        if parametre=="wood_harvested":
            ax.set_title(f'Comparaison du bois cumulé avec erreur standard')
            ax.set_xlabel('Nombre de patches')
            ax.set_ylabel('bois cumulée  ')
            

# Masquer les axes non utilisés
for i in range(3):
    for j in range(2):
        if (i == 2) and (j == 1):
            axs[i, j].axis('off')

plt.tight_layout()
plt.show()

def plot_fitness_evolution(generations, max_fitness_per_gen, num_patches):
    plt.plot(max_fitness_per_gen['#Generation'], max_fitness_per_gen['Fitness'], label=f'{num_patches} patches')
    plt.xlabel('Generations')
    plt.ylabel('Fitness')
    plt.title('Evolution of Fitness over Generations')
    plt.legend()

def parse_fitness_file(file_path):
    try:
        fitness_data = pd.read_csv(file_path, delim_whitespace=True, skiprows=18, on_bad_lines='skip')
        return fitness_data
    except Exception as e:
        print(f"Error parsing fitness file: {e}")
        return None  

def analyze_fitness(fitness_data):
    if fitness_data is not None and '#Generation' in fitness_data.columns:
        generations = fitness_data['#Generation'].unique()
        max_fitness_per_gen = fitness_data.groupby('#Generation')['Fitness'].max().reset_index()
        return generations, max_fitness_per_gen
    else:
        print("Fitness data is invalid or missing 'Generation' column")
        return [], []

# Paths to fitness files
fitness_file_paths = {
    10: '/home/loubna/loubna_data_forceeps/data_climat_bern/dataBasicBern/output-FBrin3_rcp_10.txt/evolFitness.txt',
    30: '/home/loubna/loubna_data_forceeps/data_climat_bern/dataBasicBern/output-FBrin3_rcp_30.txt/evolFitness.txt'
}

plt.figure(figsize=(10, 6))

for num_patches, fitness_file_path in fitness_file_paths.items():
    if os.path.exists(fitness_file_path):
        fitness_data = parse_fitness_file(fitness_file_path)
        generations, max_fitness_per_gen = analyze_fitness(fitness_data)
        if len(generations) > 0 and not max_fitness_per_gen.empty:
            plot_fitness_evolution(generations, max_fitness_per_gen, num_patches)
plt.grid()
plt.show()


# Charger les données à partir du fichier CSV
data = pd.read_csv('/home/loubna/loubna_data_forceeps/data_climat_bern/dataBasicBern/simulation_results.csv')

# Définir les noms des paramètres à comparer
params = ['elapsed_time', 'num_viable_individuals', 'num_generations']

# Créer une figure et des sous-graphiques
fig, axes = plt.subplots(1, len(params), figsize=(15, 5))

# Parcourir les paramètres et créer un graphique à barres pour chaque paramètre
for i, param in enumerate(params):
    axes[i].bar(data['num_patches'], data[param], color=['blue', 'orange'])
    axes[i].set_title(f'Comparison of {param}')
    axes[i].set_xlabel('Number of Patches')
    axes[i].set_ylabel(param.replace('_', ' ').capitalize())
    axes[i].set_xticks(data['num_patches'])
    axes[i].set_xticklabels([f'{int(patch)} patches' for patch in data['num_patches']])

# Ajuster l'agencement des sous-graphiques
plt.tight_layout()
plt.show()
