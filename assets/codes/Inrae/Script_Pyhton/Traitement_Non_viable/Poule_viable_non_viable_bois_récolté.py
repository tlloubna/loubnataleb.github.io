#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Aug  2 11:33:12 2024

@author: loubna
"""

import pandas as pd
import numpy as np
from sklearn.preprocessing import StandardScaler
from sklearn.decomposition import PCA
from sklearn.cluster import AgglomerativeClustering
from scipy.cluster.hierarchy import dendrogram, linkage
import matplotlib.pyplot as plt
import seaborn as sns
import os
import glob
from sklearn.cluster import KMeans
from sklearn.datasets import make_blobs
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import silhouette_score
from glob import glob

# Définir le répertoire contenant les fichiers de sortie : Regarder ou t'a les données 
output_500 = "/home/loubna/bibliographie/canew/data_test_/output-Cmd_test_1.txt"

# Obtenir les fichiers qui ne satisfont pas les contraintes (fichiers non viables)
file_Non_constraints = glob(os.path.join(output_500, 'individu*_evolConstraints_nonViable.txt'))

# Obtenir les fichiers qui satisfont les contraintes (fichiers viables)
file_viable = glob(os.path.join(output_500, 'individu*_evolConstraints.txt'))

# Filtrer les fichiers non viables selon une condition spécifique
valid_files = []
for file in file_Non_constraints:
    # Lire le fichier en ignorant les commentaires et les espaces
    data = pd.read_csv(file, delim_whitespace=True, comment="#", header=None)
    histoire = data[800:]
    # Obtenir la dernière ligne du fichier
    last_line = data.iloc[-1]
    v_prod = histoire[3].values
    # Si la condition est remplie, ajouter le fichier à la liste des fichiers valides
    if (len(v_prod) == 1) & (v_prod[0] == "v_prod:"):
        valid_files.append(file)

# Traiter les fichiers non viables sélectionnés
final_df = pd.DataFrame()
all_data_file = []
j = 0
for files in valid_files[:50]:
    print(j)
    if os.path.getsize(files) > 0:
        data = pd.read_csv(files, delim_whitespace=True, comment="#", header=None)
        data = data[:800]
        data.columns = ['year', 'idPatch', 'nbTree_ha', 'basalAreaTot_ha',
                        'basalAreaDead_ha', 'mortalityRate', 'coefGini', 'nbOfSpecies', 'vol_prod_ha']
        L = []
        for year in data["year"].unique():
            data_year = data[data["year"] == year]
            df = data_year.astype(float).mean().to_frame().T
            df = df.drop(["idPatch"], axis=1)
            L.append(df)
        j += 1
    final_df = pd.concat(L, ignore_index=True)[["year", "basalAreaTot_ha", "coefGini", "nbOfSpecies"]]
    all_data_file.append(final_df)

# Concaténer tous les fichiers non viables traités
final_df_nviable = pd.concat(all_data_file, ignore_index=True)

# Traiter les fichiers viables
final_df = pd.DataFrame()
all_data_file = []
j = 0
for files in file_viable[:50]:
    print(j)
    if os.path.getsize(files) > 0:
        data = pd.read_csv(files, delim_whitespace=True, comment="#", header=None)
        data = data[:800]
        data.columns = ['year', 'idPatch', 'nbTree_ha', 'basalAreaTot_ha',
                        'basalAreaDead_ha', 'mortalityRate', 'coefGini', 'nbOfSpecies', 'vol_prod_ha']
        L = []
        for year in data["year"].unique():
            data_year = data[data["year"] == year]
            df = data_year.astype(float).mean().to_frame().T
            df = df.drop(["idPatch"], axis=1)
            L.append(df)
        j += 1
    final_df = pd.concat(L, ignore_index=True)[["year", "basalAreaTot_ha", "coefGini", "nbOfSpecies"]]
    all_data_file.append(final_df)

# Concaténer tous les fichiers viables traités
final_df_viable = pd.concat(all_data_file, ignore_index=True)

# Calculer les statistiques pour les fichiers viables (moyenne, 1er quartile, 3e quartile)
moy_v = {}
q1_v = {}
q3_v = {}
years = data["year"].unique()
for year in years:
    data_year = final_df_viable[final_df_viable["year"] == year]
    moy_v[year] = {
        "BA": data_year["basalAreaTot_ha"].mean(),
        "Gini": data_year["coefGini"].mean(),
        "RS": data_year["nbOfSpecies"].mean()
    }
    q1_v[year] = {
        "BA": np.percentile(data_year["basalAreaTot_ha"], 25),
        "Gini": np.percentile(data_year["coefGini"], 25),
        "RS": np.percentile(data_year["nbOfSpecies"], 25)
    }
    q3_v[year] = {
        "BA": np.percentile(data_year["basalAreaTot_ha"], 75),
        "Gini": np.percentile(data_year["coefGini"], 75),
        "RS": np.percentile(data_year["nbOfSpecies"], 75)
    }

# Calculer les statistiques pour les fichiers non viables
moy_nv = {}
q1_nv = {}
q3_nv = {}
years = data["year"].unique()
for year in years:
    data_year = final_df_nviable[final_df_nviable["year"] == year]
    moy_nv[year] = {
        "BA": data_year["basalAreaTot_ha"].mean(),
        "Gini": data_year["coefGini"].mean(),
        "RS": data_year["nbOfSpecies"].mean()
    }
    q1_nv[year] = {
        "BA": np.percentile(data_year["basalAreaTot_ha"], 25),
        "Gini": np.percentile(data_year["coefGini"], 25),
        "RS": np.percentile(data_year["nbOfSpecies"], 25)
    }
    q3_nv[year] = {
        "BA": np.percentile(data_year["basalAreaTot_ha"], 75),
        "Gini": np.percentile(data_year["coefGini"], 75),
        "RS": np.percentile(data_year["nbOfSpecies"], 75)
    }

# Tracer l'évolution des paramètres BA, Gini et RS pour les viables et non viables
params = ["BA", "Gini", "RS"]
fig, axs = plt.subplots(3, 1, figsize=(12, 12))

for i, param in enumerate(params):
    years = moy_nv.keys()
    means_v = [moy_v[year][param] for year in years]
    q1_vv = [q1_v[year][param] for year in years]
    q3_vv = [q3_v[year][param] for year in years]

    means_nv = [moy_nv[year][param] for year in years]
    q1_nvv = [q1_nv[year][param] for year in years]
    q3_nvv = [q3_nv[year][param] for year in years]

    # Tracer la moyenne pour chaque paramètre avec les intervalles interquartiles
    axs[i].plot(years, means_v, label=f'Vi', color="b")
    axs[i].plot(years, means_nv, label=f'NV (Bois recolté)', color="r")
    axs[i].fill_between(years, q1_vv, q3_vv, alpha=0.2, color="b")
    axs[i].fill_between(years, q1_nvv, q3_nvv, alpha=0.2, color="r")

    axs[i].set_title(f"Evolution de {param} avec plage interquartile")
    axs[i].set_xlabel('Année')
    axs[i].set_ylabel(f'{param}')
    axs[i].legend()
    axs[i].grid(True)

fig.suptitle("Evolution du BA , Gini et RS pour les itinéraires viables et non viables à cause du bois récolté")
plt.tight_layout()
plt.show()

# Fonction pour extraire les valeurs d'un fichier de contrôle
def V(file_path):
    with open(file_path, 'r') as file:
        line = file.readline().strip()

    genome_values = line.split(':')[1].strip().split(', ')
    n = 6
    serie_divisee = [genome_values[i:i + n] for i in range(0, len(genome_values), n)]
    series_non_nulles = [s for s in serie_divisee if any(val != '0.0' for val in s)]
    if any(val == '0.0' for val in series_non_nulles[-1]):
        series_non_nulles = series_non_nulles[:-1]

    V_teta = [s[0] for s in series_non_nulles]
    V_tp = [s[1] for s in series_non_nulles]
    V_Gobj = [s[2] for s in series_non_nulles]
    V_Cf = [(s[3], s[4], s[5]) for s in series_non_nulles]

    return V_teta, V_tp, V_Gobj, V_Cf

# Extraire les valeurs des fichiers viables et non viables
V_teta_v, V_tp_v, V_Gobj_v, V_Cf_v = zip(*[V(file) for file in file_viable[:30]])
V_teta_nv, V_tp_nv, V_Gobj_nv, V_Cf_nv = zip(*[V(file) for file in valid_files[:30]])

# Regrouper toutes les valeurs dans des listes pour l'analyse
Teta_v, tp_v, Gobj_v = [], [], []
for x in V_teta_v:
    Teta_v.extend(x)
for x in V_tp_v:
    tp_v.extend(x)
for x in V_Gobj_v:
    Gobj_v.extend(x)

Teta_v = [float(item) for item in Teta_v]
tp_v = [float(item) for item in tp_v]
Gobj_v = [float(item) for item in Gobj_v]

Teta_nv, tp_nv, Gobj_nv = [], [], []
for x in V_teta_nv:
    Teta_nv.extend(x)
for x in V_tp_nv:
    tp_nv.extend(x)
for x in V_Gobj_nv:
    Gobj_nv.extend(x)

Teta_nv = [float(item) for item in Teta_nv]
tp_nv = [float(item) for item in tp_nv]
Gobj_nv = [float(item) for item in Gobj_nv]

# Regrouper les statistiques des viables et non viables pour les différents paramètres
poule_stats = {
    "V": {
        "Teta": Teta_v,
        "Tp": tp_v,
        "Gobj": Gobj_v
    },
    "NV": {
        "Teta": Teta_nv,
        "Tp": tp_nv,
        "Gobj": Gobj_nv
    }
}

# Organiser les données pour les graphiques de violon
data_violin = []
titles = ["Periode", "Type d'eclairci", "Surface terrière objective"]
params = ["Teta", "Tp", "Gobj"]

for Viability in poule_stats.keys():
    for param in params:
        for value in poule_stats[Viability][param]:
            data_violin.append([Viability, param, value])

# Convertir les données en DataFrame pour faciliter l'utilisation de seaborn
df_violin = pd.DataFrame(data_violin, columns=['Viability', 'Parameter', 'Value'])

# Créer les graphiques de violon
fig, axs = plt.subplots(1, 3, figsize=(20, 10))
for i, param in enumerate(params):
    sns.violinplot(x='Viability', y='Value', data=df_violin[df_violin['Parameter'] == param], palette=["b", "r"], ax=axs[i])
    axs[i].set_xlabel('Classe')
    axs[i].set_ylabel('Distribution')
    axs[i].set_title(titles[i])

# Ajuster l'agencement des sous-graphiques pour éviter les chevauchements
plt.tight_layout()
plt.show()
