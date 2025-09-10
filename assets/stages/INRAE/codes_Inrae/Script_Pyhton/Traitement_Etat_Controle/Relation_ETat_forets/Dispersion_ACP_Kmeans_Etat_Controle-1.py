#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Jul 18 11:05:32 2024

@author: loubna
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.preprocessing import StandardScaler, MinMaxScaler
from sklearn.decomposition import PCA
from sklearn.cluster import KMeans
from glob import glob
import os

# Définir le répertoire des fichiers
output_dir = "/home/loubna/bibliographie/canew/data_test_/output-Cmd_test_1.txt"

def shannon(df_year):
    """
    Calcul de l'indice de Shannon pour la diversité des espèces.
    """
    species = df_year['specie'].unique()
    N = df_year[df_year["specie"] != "total"]["Nb_tree_ha"].astype(float).sum()
    pi = df_year[df_year["specie"] != "total"]["Nb_tree_ha"].astype(float) / N
    pi = pi.replace(0, 1e-9)  # Remplacer les valeurs nulles par une petite valeur positive
    L = pi * np.log(pi)
    return -np.sum(L)

def read_viable_files(directory):
    """
    Lire tous les fichiers viables dans le répertoire spécifié.
    """
    return glob(os.path.join(directory, 'individu*_evolEtat.txt'))

def process_file(file_path):
    """
    Traiter un fichier spécifique pour extraire les valeurs et les séries.
    """
    with open(file_path, 'r') as file:
        line = file.readline().strip()
    
    genome_values = line.split(':')[1].strip().split(', ')
    n = 6
    serie_divisee = [genome_values[i:i + n] for i in range(0, len(genome_values), n)]
    
    V_teta = [s[0] for s in serie_divisee]
    V_tp = [s[1] for s in serie_divisee]
    V_Gobj = [s[2] for s in serie_divisee]
    V_Cf = [(s[3], s[4], s[5]) for s in serie_divisee]
    
    return V_teta, V_tp, V_Gobj, V_Cf

def moy_patch(file_path):
    """
    Calculer la moyenne des colonnes numériques groupées par année et espèce.
    """
    data = pd.read_csv(file_path, delim_whitespace=True, skiprows=3).iloc[:8800]
    numeric_cols = ['patchId', 'Nb_tree_ha', 'Basal_area_ha', 'Diameter_mean_ha',
                    'Diameter_std_ha', 'Height_max_ha', 'Height_mean_ha', 'Height_std_ha',
                    'droughtIndex_mean', 'light_Avaibility_mean', 'Gini']
    data[numeric_cols] = data[numeric_cols].astype(float)
    grouped = data.groupby(['#year', 'specie'])[numeric_cols].mean().reset_index()
    return grouped.drop("patchId", axis=1)

def moy_patch_total(file_path):
    """
    Calculer les statistiques moyennes et la diversité de Shannon pour chaque année.
    """
    data = pd.read_csv(file_path, delim_whitespace=True, skiprows=3).iloc[:-1]
    data_sh = moy_patch(file_path)
    
    result = []
    for year in data["#year"].unique():
        data_year = data[data["#year"] == year]
        data_total = data_year[data_year["specie"] == "total"]
        data_W_sp = data_total.drop(["specie", "patchId", "#year"], axis=1)
        df = data_W_sp.astype(float).mean().to_frame().T
        df["#year"] = year
        df['shanon'] = shannon(data_sh[data_sh["#year"] == year])
        result.append(df)
    
    return pd.concat(result, ignore_index=True)

def ETAT_Control(viable_files):
    """
    Traiter tous les fichiers viables pour extraire et combiner les données.
    """
    final_df = pd.DataFrame()
    for file_path in viable_files:
        if os.path.getsize(file_path) > 0:
            data = pd.read_csv(file_path, delim_whitespace=True, skiprows=3).iloc[:-1]
            years = data['#year'].unique()
            year_5 = [years[i-1] for i in range(5, len(years), 5)]
            data_moy_patch = moy_patch_total(file_path)
            V_teta, V_tp, V_Gobj, V_Cf = process_file(file_path)
            
            for i in range(len(V_teta)):
                if V_teta[i] != '0.0' and i != 15:
                    year = year_5[i]
                    data_year_5 = data_moy_patch[data_moy_patch["#year"] == year].astype(float)
                    data_year_5["teta"] = float(V_teta[i])
                    data_year_5["tp"] = float(V_tp[i])
                    data_year_5["Gobj"] = float(V_Gobj[i])
                    data_year_5["Cf1"], data_year_5["Cf2"], data_year_5["Cf3"] = map(float, V_Cf[i])
                    final_df = pd.concat([final_df, data_year_5], ignore_index=True)
    
    return final_df

# Lire et traiter les données
file_data = ETAT_Control(read_viable_files(output_dir))
file_data.to_csv("/home/loubna/bibliographie/canew/data_test_/Control_etat.csv", index=False)

file_data=pd.read_csv("/home/loubna/bibliographie/canew/data_test_/Control_etat.csv")
def ACP(file_data):
    """
    Effectuer l'Analyse en Composantes Principales (ACP) sur les données.
    """
    control = ["teta", "tp", "Cf1", "Gobj", "Cf2", "Cf3", "#year", "Height_std_ha", "Diameter_std_ha", "Height_mean_ha", "Diameter_mean_ha"]
    data_Etat = file_data.drop(control, axis=1)
    
    standardized_data = (data_Etat - data_Etat.mean()) / data_Etat.std()
    pca = PCA()
    principal_components = pca.fit_transform(standardized_data)
    
    explained_variance_ratio = pca.explained_variance_ratio_
    cumulative_explained_variance = np.cumsum(explained_variance_ratio)
    n_components = np.argmax(cumulative_explained_variance >= 0.7) + 1
    
    explained_variance_percentage = explained_variance_ratio * 100
    print(f"Nombre de composantes principales : {n_components}")
    
    plt.figure(figsize=(10, 6))
    plt.bar(range(1, len(explained_variance_percentage) + 1), explained_variance_percentage, alpha=0.7, align='center')
    plt.title("Pourcentage de variance expliquée par chaque composante principale")
    plt.xlabel("Composante principale")
    plt.ylabel("Pourcentage de variance expliquée")
    plt.show()
    
    print(f"Pourcentage total pour les 3 premiers axes : {np.sum(explained_variance_percentage[:3]):.2f}%")
    
    eigenvectors = pca.components_
    fig, axes = plt.subplots(1, n_components, figsize=(12, 12))
    
    for i in range(n_components):
        contributions = eigenvectors[i] ** 2 / np.sum(eigenvectors[i] ** 2) * 100
        axes[i].bar(data_Etat.columns, contributions)
        axes[i].set_title(f"Contribution à l'axe {i+1}")
        axes[i].set_xlabel("Variable")
        axes[i].set_ylabel("Contribution (%)")
        axes[i].tick_params(axis='x', rotation=90)
    
    plt.tight_layout()
    plt.show()
    
    return principal_components

principal_components = ACP(file_data)

def Kmeans(reduced_components):
    """
    Appliquer l'algorithme K-means pour la classification des données.
    """
    wcss = []
    for i in range(1, 20):
        kmeans = KMeans(n_clusters=i, init='k-means++', max_iter=400, n_init=50, random_state=0)
        kmeans.fit(reduced_components)
        wcss.append(kmeans.inertia_)
    
    plt.figure(figsize=(8, 6))
    plt.plot(range(1, 20), wcss)
    plt.title("Méthode du coude")
    plt.xlabel("Nombre de clusters")
    plt.ylabel("WCSS")
    plt.show()
    
    kmeans = KMeans(n_clusters=3, init='k-means++', max_iter=400, n_init=50, random_state=0)
    clusters = kmeans.fit_predict(reduced_components)
    print(clusters)
    
    unique_clusters, counts = np.unique(clusters, return_counts=True)
    palette = ["b", "r", "y"]
    
    plt.figure(figsize=(8, 6))
    plt.pie(counts, labels=[f"Classe {c}" for c in unique_clusters], colors=palette[:len(unique_clusters)], autopct='%1.1f%%')
    plt.title("Effectif de chaque classe")
    plt.axis('equal')
    plt.show()
    
    return clusters

# Sélection des premiers composants principaux réduits (ici les 3 premiers)
reduced_compenents = principal_components[:, :3]

# Application du clustering K-means sur les composants principaux réduits
clusters = Kmeans(reduced_compenents)

# ************************ACP & Kmeans*********************************
def ACP_Kmeans(data_Etat, principal_components, clusters, PC0, PC1):
    palette = ["b", "r", "y"]  # Palette de couleurs pour les clusters

    # Normalisation des données
    standardized_data = (data_Etat - data_Etat.mean()) / data_Etat.std()
    
    # Création d'un objet PCA et transformation des données
    pca = PCA()
    principal_components = pca.fit_transform(standardized_data)
    
    # Calcul des rayons du cercle de corrélation
    circle_radii = np.sqrt(pca.explained_variance_)
    
    # Mise à l'échelle des composants principaux pour qu'ils soient entre -1 et 1
    scaler = MinMaxScaler(feature_range=(-1, 1))
    principal_components = scaler.fit_transform(principal_components)

    # Visualisation des données projetées avec les clusters
    sns.scatterplot(x=principal_components[:, PC1], y=principal_components[:, PC0], hue=clusters, palette=palette, s=100)
    
    # Ajout des flèches du cercle de corrélation
    for i in range(len(pca.components_)):
        plt.arrow(0, 0, pca.components_[PC1, i] * circle_radii[0], pca.components_[PC0, i] * circle_radii[1], color='black', alpha=0.5, linewidth=5)
        plt.text(pca.components_[PC1, i] * circle_radii[0] * 1.15, pca.components_[PC0, i] * circle_radii[1] * 1.15, data_Etat.columns[i], color='black', ha='center', va='center', fontsize=14)
    
    # Ajout du cercle de corrélation
    circle = plt.Circle((0, 0), 1, edgecolor='b', facecolor='none')
    plt.gca().add_artist(circle)
    
    # Calcul et affichage du pourcentage de variance expliquée
    explained_variance_ratio = pca.explained_variance_ratio_
    explained_variance_percentage = pca.explained_variance_ratio_ * 100
    plt.title(f'Cercle de Corrélation et kmeans\nPC{PC0}: {explained_variance_percentage[PC0]:.2f}% de la variance expliquée, PC{PC1}: {explained_variance_percentage[PC1]:.2f}% de la variance expliquée')
    plt.xlabel(f'PC{PC1} ({explained_variance_percentage[PC1]:.2f}% de la variance expliquée)')
    plt.ylabel(f'PC{PC0} ({explained_variance_percentage[PC0]:.2f}% de la variance expliquée)')
    plt.grid(True)
    plt.axis('equal')
    plt.show()

# Suppression des colonnes non pertinentes et appel de la fonction ACP_Kmeans
Colonnes_non = ["teta", "tp", "Cf1", "Gobj", "Cf2", "Cf3", "#year", "Height_std_ha", "Diameter_std_ha", "Height_mean_ha", "Diameter_mean_ha"]
data_Etat = file_data.drop(Colonnes_non, axis=1)
ACP_Kmeans(data_Etat, reduced_compenents, clusters, 1, 2)

# *************************************************Contribution de chaque variable à la construction de l'axe 
data_Etat_year = file_data.copy()
data_Etat_year["year"] = file_data['#year']
data_Etat_year = data_Etat_year.astype(float)
data_Etat_year["cluster"]=clusters
n_clusters = data_Etat_year["cluster"].nunique()

# ****************************Boxplot***********************************************
def boxplot_cluster(data_Analyse, clusters):
    palette = ["b", "r", "y"]
    data_Analyse['cluster'] = clusters
    n_clusters = data_Analyse['cluster'].nunique()

    # Définir les paramètres à tracer
    parameters = data_Analyse.columns[:3]
    titles = data_Analyse.columns[:3]

    # Créer une figure avec plusieurs sous-graphes
    fig, axes = plt.subplots(1, len(parameters), figsize=(20, 6))

    # Tracer les boxplots pour chaque paramètre
    for ax, param, title in zip(axes, parameters, titles):
        box = ax.boxplot(
            [data_Analyse[data_Analyse['cluster'] == i][param] for i in range(n_clusters)],
            patch_artist=True
        )
        for patch, color in zip(box['boxes'], palette):
            patch.set_facecolor(color)
        ax.set_title(title)
        ax.set_xlabel('Cluster')
        ax.set_ylabel(param)

    plt.tight_layout()
    plt.show()

boxplot_cluster(data_Etat, clusters)


#*********************Control******************
data_Control=file_data
data_Control["cluster"]=clusters
parameters = ["teta","tp","Gobj"]
titles =  ["teta","tp","Gobj"]
palette = ["b", "r", "y"]
n_clusters=data_Control["cluster"].nunique()
fig, axes = plt.subplots(1, len(parameters), figsize=(20, 6))
for ax, param, title in zip(axes, parameters, titles):
    for i in range(n_clusters):
        cluster_data = data_Control[data_Control['cluster'] == i][param]
        ax.hist(cluster_data, bins=20, alpha=0.5, label=f'Classe {i}', color=palette[i])
    ax.set_title(title)
    ax.set_xlabel(param)
    ax.set_ylabel('Densité')
    ax.legend()

plt.tight_layout()
plt.show()



fig, axs =plt.subplots(len(parameters), 1, figsize=(12, 12))

params = ["teta", "tp", "Gobj"]
for i, (param, title) in enumerate(zip(parameters, titles)):
    

    data = [data_Control[data_Control['cluster'] == j][param] for j in range(3)]

    axs[i].hist(data, color=palette,density=True,  label=['classe 0', 'classe 1', 'classe 2'], histtype='bar',alpha=0.5)
    axs[i].set_xlabel('valeurs')
    axs[i].set_ylabel('density')
    axs[i].set_title(title)
plt.legend(loc='lower center',bbox_to_anchor=(0.5, -0.45), ncol=len(parameters)) 

plt.tight_layout()
plt.show()


# *********************************Distribution Dispersion*********************************************
def tracer_dispersion(file_viables):
    # Lire les données du premier fichier pour obtenir les noms des colonnes
    data = pd.read_csv(file_viables[1], delim_whitespace=True, skiprows=3)
    data = data.iloc[:-1]  # Supprimer la dernière ligne
    names = data.columns
    df = pd.DataFrame(columns=names)
    
    # Parcourir chaque fichier de données spécifié
    for file in file_viables:
        if os.path.getsize(file) > 0:  # Vérifier que le fichier n'est pas vide
            data = pd.read_csv(file, delim_whitespace=True, skiprows=3)
            data = data.iloc[:-1]  # Supprimer la dernière ligne
            
            # Filtrer les données pour conserver uniquement les lignes où l'espèce est "total"
            data = data[data["specie"] == "total"]
            selected_data = data[names]  # Sélectionner uniquement les colonnes d'intérêt
            
            # Supprimer la colonne 'specie' car elle n'est pas nécessaire pour la matrice de dispersion
            selected_data_Wtotal = selected_data.drop("specie", axis=1)
            
            # Convertir les données en float pour les colonnes numériques
            for name in names:
                if name != "specie":
                    selected_data_Wtotal[name] = selected_data_Wtotal[name].astype(float)
            
            # Concaténer les données du fichier actuel avec le DataFrame principal
            df = pd.concat([df, selected_data_Wtotal], ignore_index=True)
    
    # Sélectionner les colonnes d'intérêt pour la matrice de dispersion
    standardized_data = df[["Height_std_ha", "Diameter_std_ha", "Gini"]]
    
    # Tracer la matrice de dispersion
    pd.plotting.scatter_matrix(standardized_data, figsize=(10, 10))
    plt.suptitle('Matrice de dispersion des variables')
    plt.show()

tracer_dispersion(read_viable_files(output_dir))
