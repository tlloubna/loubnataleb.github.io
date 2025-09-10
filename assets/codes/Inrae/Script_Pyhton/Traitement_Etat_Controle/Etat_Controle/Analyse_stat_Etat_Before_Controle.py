#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Aug  9 11:19:27 2024

@author: loubna
"""

# Importation des bibliothèques nécessaires
import numpy as np
import pandas as pd
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


output2 = "/home/loubna/loubna_data_forceeps/data_climat_bern/dataRCP_GA/Output_RCP_8_MPI/output-FBrin3_rcp_10_10_5_0.05_500_0.txt"

# Trouver les fichiers viables à partir du répertoire défini
viables_files = glob.glob(os.path.join(output2, 'individu*_evolConstraints.txt'))

def V(file_path):
    """
    Fonction pour extraire les valeurs des paramètres à partir d'un fichier de chemin donné.
    
    :param file_path: Chemin vers le fichier à lire.
    :return: Quatre listes contenant les valeurs extraites des paramètres.
    """
    with open(file_path, 'r') as file:
        # Lire la première ligne du fichier et la nettoyer
        line = file.readline().strip()
    
    # Extraire les valeurs du génome à partir de la ligne lue
    genome_values = line.split(':')[1].strip().split(', ')
    
    # Diviser les valeurs en séries de 6 éléments
    n = 6
    serie_divisee = [genome_values[i:i + n] for i in range(0, len(genome_values), n)]
    
    # Extraire les valeurs des différentes parties des séries divisées
    V_teta = [s[0] for s in serie_divisee]
    V_tp = [s[1] for s in serie_divisee]
    V_Gobj = [s[2] for s in serie_divisee]
    
    # Extraire les tuples des trois dernières valeurs de chaque série
    V_Cf = [(s[3], s[4], s[5]) for s in serie_divisee]
    
    return V_teta, V_tp, V_Gobj, V_Cf

def Control_Etat(viables_files):
    """
    Fonction pour contrôler l'état des fichiers viables et extraire les données pertinentes.
    
    :param viables_files: Liste des chemins vers les fichiers viables.
    :return: Dictionnaire contenant les états contrôlés par année.
    """
    Control_etat = {}
    j=0
    for files in viables_files:
        j+=1
        # Charger les données du fichier en utilisant pandas
        data = pd.read_csv(files, delim_whitespace=True, skiprows=1)
        data = data.iloc[:-1]  # Supprimer la dernière ligne
        print(j)
        # Récupérer les années uniques présentes dans les données
        years = data['#year'].unique()
        # Sélectionner les années toutes les 5 années pour les années avant et après
        year_5 = [years[i-1] for i in range(5, len(years), 5)]
        year_a5 = [years[i] for i in range(5, len(years), 5)]
        
        # Dictionnaire pour stocker les moyennes des données par année
        data_moy_patch = {}
        for year in years:
            # Filtrer les données pour l'année courante
            gen0_data = data[data.iloc[:,0] == year]
            basal_area_gen0 = gen0_data["basalAreaTot_ha"].mean()
            gini_gen0 = gen0_data["coefGini"].mean()
    
            # Stocker les résultats dans le dictionnaire
            data_moy_patch[year] = {
                "G": [basal_area_gen0],
                "Gini": [gini_gen0]
            }
        
        # Extraire les paramètres génétiques du fichier
        V_teta, V_tp, V_Gobj, V_Cf = V(files)
        
        for i in range(len(V_teta)):
            
            if V_teta[i] != '0.0' and i != 15:
                # Trouver les années correspondant aux indices
                year = year_5[i]
                year_a = year_a5[i]
                
                if year not in Control_etat:
                    # Initialiser les entrées du dictionnaire pour une nouvelle année
                    Control_etat[year] = {
                        "G": data_moy_patch[year]["G"],
                        "Gini": data_moy_patch[year]["Gini"],
                        "G_a": data_moy_patch[year_a]["G"],
                        "Gini_a": data_moy_patch[year_a]["Gini"],
                        "teta": [float(V_teta[i])],
                        "tp": [float(V_tp[i])],
                        "G_obj": [float(V_Gobj[i])]
                    }
                else:
                    # Mettre à jour les données existantes pour l'année en cours
                    Control_etat[year]["G"].extend(data_moy_patch[year]["G"])
                    Control_etat[year]["Gini"].extend(data_moy_patch[year]["Gini"])
                    Control_etat[year]["G_a"].extend(data_moy_patch[year_a]["G"])
                    Control_etat[year]["Gini_a"].extend(data_moy_patch[year_a]["Gini"])
                    Control_etat[year]["teta"].append(float(V_teta[i]))
                    Control_etat[year]["tp"].append(float(V_tp[i]))
                    Control_etat[year]["G_obj"].append(float(V_Gobj[i]))
    
    return Control_etat

# Exécuter la fonction pour obtenir l'état contrôlé
Control_etat = Control_Etat(viables_files)
print(Control_etat)

# Créer une liste vide pour stocker les DataFrames
all_dfs = []

# Parcourir les années et récupérer les DataFrames
for year in Control_etat.keys():
    # Créer un DataFrame pour chaque année à partir des données de Control_etat
    df = pd.DataFrame(Control_etat[year])
    # Ajouter une colonne 'year' au DataFrame avec l'année correspondante
    df['year'] = year
    # Ajouter le DataFrame à la liste all_dfs
    all_dfs.append(df)

# Fusionner tous les DataFrames en un seul DataFrame
merged_df = pd.concat(all_dfs, ignore_index=True)

# **********************ANOVA******************
import statsmodels.api as sm
from statsmodels.formula.api import ols

# Modèle pour l'analyse de variance (ANOVA) de la surface terrière avant coupe
model_st = ols('G ~ C(tp) + C(G_obj) + C(tp):C(G_obj)', data=merged_df).fit()
anova_table_st = sm.stats.anova_lm(model_st, typ=2)
# Afficher les résultats de l'ANOVA pour la surface terrière avant coupe
print(anova_table_st)

# Modèle pour l'analyse de variance (ANOVA) de l'indice de Gini avant coupe
model_gini = ols('Gini ~ C(tp) + C(G_obj) + C(tp):C(G_obj)', data=merged_df).fit()
anova_table_gini = sm.stats.anova_lm(model_gini, typ=2)
# Afficher les résultats de l'ANOVA pour l'indice de Gini avant coupe
print(anova_table_gini)

# Créer un graphique pour visualiser l'interaction entre le type de coupe et le pourcentage de surface terrière restante
plt.figure(figsize=(10, 6))
sns.set_style("whitegrid")  # Définir le style du graphique
sns.pointplot(x="G_obj", y="G", hue="teta", data=merged_df)  # Création du graphique avec les points
plt.title("Interaction entre le type de coupe et le pourcentage de surface terrière restante sur la surface terrière avant coupe")
plt.xlabel("Pourcentage de surface terrière restante")  # Étiquette de l'axe x
plt.ylabel("Surface terrière avant coupe")  # Étiquette de l'axe y
plt.legend(title="teta")  # Légende du graphique
plt.show()  # Afficher le graphique

# Créer un graphique pour visualiser l'interaction entre le type de coupe et le pourcentage de surface terrière restante sur l'indice de Gini
plt.figure(figsize=(10, 6))
sns.set_style("whitegrid")  # Définir le style du graphique
sns.pointplot(x="G_obj", y="Gini", hue="teta", data=merged_df)  # Création du graphique avec les points
plt.title("Interaction entre le type de coupe et le pourcentage de surface terrière restante sur GINI")
plt.xlabel("Pourcentage de surface terrière restante")  # Étiquette de l'axe x
plt.ylabel("GINI")  # Étiquette de l'axe y
plt.legend(title="teta")  # Légende du graphique
plt.show()  # Afficher le graphique

# Calculer la matrice de corrélation entre les variables
corr_matrix = merged_df.corr()

from sklearn.linear_model import LinearRegression

# Afficher la matrice de corrélation
fig, ax = plt.subplots(figsize=(10, 8))  # Créer une figure avec des dimensions spécifiées
sns.heatmap(corr_matrix, cmap='YlOrRd', fmt='.2f')  # Tracer la matrice de corrélation sous forme de carte thermique
plt.title('Matrice de corrélation')  # Ajouter un titre au graphique

# Ajouter les valeurs de la matrice de corrélation sur la carte thermique
for i in range(corr_matrix.shape[0]):
    for j in range(corr_matrix.shape[1]):
        text = ax.text(j + 0.5, i + 0.5, f"{corr_matrix.iloc[i, j]:.2f}", ha="center", va="center", color="black", size=8)


# Tracer le graphique de dispersion pour 'G' avant et après coupe
plt.figure(figsize=(8, 6))  # Créer une figure avec des dimensions spécifiées
plt.scatter(merged_df["G"], merged_df["G_a"], color="red", alpha=0.5)  # Tracer les points de dispersion avec une transparence de 0.5
plt.xlabel("G before cutting")  # Étiquette de l'axe x
plt.ylabel("G after cutting")  # Étiquette de l'axe y
plt.title("Graphique de dispersion de G_before et G_after")  # Ajouter un titre au graphique

# Réaliser la régression linéaire pour 'G' avant et après coupe
model = LinearRegression()  # Créer un modèle de régression linéaire
model.fit(merged_df[["G"]], merged_df["G_a"])  # Ajuster le modèle sur les données

# Afficher les résultats de la régression
print("Coefficient de régression (pente) :", model.coef_[0])  # Afficher le coefficient de régression (pente)
print("Ordonnée à l'origine :", model.intercept_)  # Afficher l'ordonnée à l'origine
print("Coefficient de détermination (R-carré) :", model.score(merged_df[["G"]], merged_df["G_a"]))  # Afficher le coefficient de détermination (R-carré)

# Tracer la droite de régression sur le graphique de dispersion
plt.plot(merged_df["G"], model.predict(merged_df[["G"]]), color="blue", linewidth=2)  # Tracer la droite de régression en bleu avec une épaisseur de 2
plt.show()  # Afficher le graphique

# Tracer le graphique de dispersion pour l'indice de Gini avant et après coupe
plt.figure(figsize=(8, 6))  # Créer une figure avec des dimensions spécifiées
plt.scatter(merged_df["Gini"], merged_df["Gini_a"], color="red", alpha=0.5)  # Tracer les points de dispersion avec une transparence de 0.5
plt.xlabel("Gini before cutting")  # Étiquette de l'axe x
plt.ylabel("Gini after cutting")  # Étiquette de l'axe y
plt.title("Graphique de dispersion de Gini_before et Gini_after")  # Ajouter un titre au graphique

# Réaliser la régression linéaire pour l'indice de Gini avant et après coupe
model = LinearRegression()  # Créer un modèle de régression linéaire
model.fit(merged_df[["Gini"]], merged_df["Gini_a"])  # Ajuster le modèle sur les données

# Afficher les résultats de la régression
print("Coefficient de régression (pente) :", model.coef_[0])  # Afficher le coefficient de régression (pente)
print("Ordonnée à l'origine :", model.intercept_)  # Afficher l'ordonnée à l'origine
print("Coefficient de détermination (R-carré) :", model.score(merged_df[["Gini"]], merged_df["Gini_a"]))  # Afficher le coefficient de détermination (R-carré)

# Tracer la droite de régression sur le graphique de dispersion
plt.plot(merged_df["Gini"], model.predict(merged_df[["Gini"]]), color="blue", linewidth=2)  # Tracer la droite de régression en bleu avec une épaisseur de 2
plt.show()  # Afficher le graphique


