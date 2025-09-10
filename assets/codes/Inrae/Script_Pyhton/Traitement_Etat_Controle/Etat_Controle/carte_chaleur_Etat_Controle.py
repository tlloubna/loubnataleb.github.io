#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Aug  9 11:36:13 2024

@author: loubna
"""

import numpy as np
import pandas as pd
import glob
import os
import matplotlib.pyplot as plt
import seaborn as sns

from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import silhouette_score

# Définir le répertoire des fichiers
output_dir = "/home/loubna/bibliographie/canew/data_test_/output-Cmd_test_1.txt"

# Trouver les fichiers viables
viables_files = glob.glob(os.path.join(output_dir, 'individu*_evolConstraints.txt'))

def parse_file(file_path):
    with open(file_path, 'r') as file:
        line = file.readline().strip()
    
    genome_values = line.split(':')[1].strip().split(', ')
    
    # Diviser la série en 6 morceaux
    n = 6
    serie_divisee = [genome_values[i:i + n] for i in range(0, len(genome_values), n)]
    
    # Récupérer les valeurs nécessaires
    V_teta = [s[0] for s in serie_divisee]
    V_tp = [s[1] for s in serie_divisee]
    V_Gobj = [s[2] for s in serie_divisee]
    V_Cf = [(s[3], s[4], s[5]) for s in serie_divisee]
    
    return V_teta, V_tp, V_Gobj, V_Cf

def control_etat(viables_files):
    control_etat = {}
    
    for file_path in viables_files:
        data = pd.read_csv(file_path, delim_whitespace=True, skiprows=1)
        data = data.iloc[:-1]  # Supprimer la dernière ligne
        
        # Récupérer les colonnes 'year', 'vol_prod_ha' et 'basalAreaTot_ha'
        years = data['#year'].unique()
        year_5 = [years[i-1] for i in range(5, len(years), 5)]
        year_a5 = [years[i] for i in range(5, len(years), 5)]
        data_moy_patch = {}
        
        for year in years:
            gen0_data = data[data.iloc[:,0] == year]
            basal_area_gen0 = gen0_data["basalAreaTot_ha"].astype(float).mean()
            gini_gen0 = gen0_data["coefGini"].astype(float).mean()
    
            data_moy_patch[year] = {
                "G": [basal_area_gen0],
                "Gini": [gini_gen0]
            }
        
        V_teta, V_tp, V_Gobj, V_Cf = parse_file(file_path)
        
        for i in range(len(V_teta)):
            if V_teta[i] != '0.0' and i != 15:
                year = year_5[i]
                year_a = year_a5[i]
                
                if year not in control_etat:
                    control_etat[year] = {
                        "G": data_moy_patch[year]["G"],
                        "Gini": data_moy_patch[year]["Gini"],
                        "G_a": data_moy_patch[year_a]["G"],
                        "Gini_a": data_moy_patch[year_a]["Gini"],
                        "teta": [float(V_teta[i])],
                        "tp": [float(V_tp[i])],
                        "G_obj": [float(V_Gobj[i])]
                    }
                else:
                    control_etat[year]["G"].extend(data_moy_patch[year]["G"])
                    control_etat[year]["Gini"].extend(data_moy_patch[year]["Gini"])
                    control_etat[year]["G_a"].extend(data_moy_patch[year_a]["G"])
                    control_etat[year]["Gini_a"].extend(data_moy_patch[year_a]["Gini"])
                    control_etat[year]["teta"].append(float(V_teta[i]))
                    control_etat[year]["tp"].append(float(V_tp[i]))
                    control_etat[year]["G_obj"].append(float(V_Gobj[i]))
    
    return control_etat

control_etat = control_etat(viables_files)
print(control_etat)

# Créer une liste vide pour stocker les DataFrames
all_dfs = []

# Parcourir les années et récupérer les DataFrames
for year in control_etat.keys():
    df = pd.DataFrame(control_etat[year])
    df['year'] = year
    all_dfs.append(df)

# Fusionner tous les DataFrames
merged_df = pd.concat(all_dfs, ignore_index=True)
print(merged_df)

# Définir les plages de valeurs pour les graphiques
G = list(range(10, int(merged_df['G'].max()), 10))
Gini = [merged_df["Gini"].min(), 0.3, 0.4, 0.5, 0.6, merged_df["Gini"].max()]
tp = [0, 0.5, 1]
G_obj = [0.5, 0.6, 0.7, 0.8, 0.9]

# Définir les intervalles
L_G = [[G[i], G[i+1]] for i in range(len(G) - 1)]
L_Gini = [[Gini[j], Gini[j+1]] for j in range(len(Gini) - 1)]

# Initialiser les matrices avec des NaN
matrices = {teta: np.full((len(L_G), len(L_Gini)), np.nan) for teta in [5, 10, 15, 20]}

for teta in [5, 10, 15, 20]:
    for i in range(len(L_G)):
        for j in range(len(L_Gini)):
            Int_G = merged_df[(merged_df["G"] >= L_G[i][0]) & (merged_df["G"] < L_G[i][1])]
            Int_Gini = Int_G[(Int_G["Gini"] >= L_Gini[j][0]) & (Int_G["Gini"] < L_Gini[j][1])]
            df_teta = Int_Gini[Int_Gini["teta"] == teta]
            
            if len(df_teta) != 0:
                matrices[teta][i, j] = df_teta["G_obj"].mean()

# Définir les étiquettes pour les axes
G_labels = [f'{L_G[i][0]}-{L_G[i][1]}' for i in range(len(L_G))]
Gini_labels = [f'{L_Gini[i][0]:.2f}-{L_Gini[i][1]:.2f}' for i in range(len(L_Gini))]

# Calculer vmin et vmax à partir de toutes les matrices
vmin = 0
vmax = 1

fig, axes = plt.subplots(2, 2, figsize=(20, 16))

for idx, teta in enumerate([5, 10, 15, 20]):
    ax = axes[idx // 2, idx % 2]
    sns.heatmap(matrices[teta], xticklabels=Gini_labels, yticklabels=G_labels, cmap='coolwarm', cbar=True, ax=ax, vmin=vmin, vmax=vmax)
    ax.set_title(f'teta={teta}')
    ax.set_xlabel('Gini')
    ax.set_ylabel('G')
    for i in range(len(L_G)):
        for j in range(len(L_Gini)):
            text = ax.text(j + 0.5, i + 0.5, f"{matrices[teta][i, j]:.2f}" if not np.isnan(matrices[teta][i, j]) else "nan", ha="center", va="center", color="black", size=20)

fig.suptitle('Carte de chaleur des matrices de G_obj pour différents teta', fontsize=20)
plt.tight_layout()
plt.show()

#**************************************Effectifs******************************************************

moyenne_matrices = {teta: np.zeros((len(L_G), len(L_Gini), len(G_obj)), dtype=object) for teta in [5, 10, 15, 20]}

for teta in [5, 10, 15, 20]:
    for i in range(len(L_G)):
        for j in range(len(L_Gini)):
            Int_G = merged_df[(merged_df["G"] >= L_G[i][0]) & (merged_df["G"] < L_G[i][1])]
            Int_Gini = Int_G[(Int_G["Gini"] >= L_Gini[j][0]) & (Int_G["Gini"] < L_Gini[j][1])]
            df_teta = Int_Gini[Int_Gini["teta"] == teta]
            for k in range(len(G_obj)):
                if len(df_teta) != 0:
                    Int_tp = df_teta[df_teta["G_obj"] == G_obj[k]]
                    moyenne_matrices[teta][i, j, k] = list(Int_tp["G_obj"]) if len(Int_tp) != 0 else []

# Tracer les effectifs
fig, axes = plt.subplots(len(L_Gini), len(L_G), figsize=(19, 15))

for i in range(len(L_G)):
    for j in range(len(L_Gini)):
        G_density = np.concatenate([moyenne_matrices[5][i, j, k] for k in range(len(G_obj))])
        axes[i, j].hist(G_density, color="red", edgecolor='red', hatch='/', bins=10, alpha=0.5)
        axes[i, j].set_xticks(tp)
        if j == 0:
            axes[i, j].set_ylabel(f"{L_G[i][0]} - {L_G[i][1]}")
        if i == len(L_G) - 1:
            axes[i, j].set_xlabel(f"{L_Gini[j][0]} - {L_Gini[j][1]}")

plt.tight_layout()
fig.supylabel("Surface terrière")
fig.supxlabel("Gini")
fig.suptitle("Effectif de tp pour chaque combinaison de Surface terrière et GINI et teta = 5", fontsize=16)
plt.subplots_adjust(top=0.92)
plt.show()
