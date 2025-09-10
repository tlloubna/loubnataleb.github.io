#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Jul 19 11:10:51 2024

@author: loubna
"""

# Importer les bibliothèques nécessaires
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
import os
from glob import glob

#*********************************Shannon*************************************
def shannon(df_year):
    """
    Calcule l'indice de diversité de Shannon pour les données d'une année donnée.
    
    Arguments:
    df_year -- DataFrame contenant les données pour une année spécifique.
    
    Retourne:
    Indice de Shannon pour les données fournies.
    """
    # Obtenir les espèces uniques
    species = df_year['specie'].unique()
    # Calculer le total des arbres pour les espèces, en excluant 'total'
    N = df_year[df_year["specie"] != "total"]["Nb_tree_ha"].astype(float).sum()
    # Calculer la proportion de chaque espèce
    pi = df_year[df_year["specie"] != "total"]["Nb_tree_ha"].astype(float) / N
    
    # Remplacer les valeurs de pi égales à 0 par une petite valeur positive pour éviter les erreurs de calcul
    pi = pi.replace(0, 1e-9)
    
    # Calculer l'indice de Shannon
    L = pi * np.log(pi)
    return -np.sum(L)

#********************Shannon Basal Area**************************************
def shannon_Basal_ARea(df_year):
    """
    Calcule l'indice de diversité de Shannon basé sur la surface basale pour les données d'une année donnée.
    
    Arguments:
    df_year -- DataFrame contenant les données pour une année spécifique.
    
    Retourne:
    Indice de Shannon pour la surface basale.
    """
    # Obtenir les espèces uniques
    species = df_year['specie'].unique()
    # Calculer le total de la surface basale pour les espèces, en excluant 'total'
    N = df_year[df_year["specie"] != "total"]["Basal_area_ha"].astype(float).sum()
    # Calculer la proportion de chaque espèce
    pi = df_year[df_year["specie"] != "total"]["Basal_area_ha"].astype(float) / N
    
    # Remplacer les valeurs de pi égales à 0 par une petite valeur positive pour éviter les erreurs de calcul
    pi = pi.replace(0, 1e-9)
    
    # Calculer l'indice de Shannon
    L = pi * np.log(pi)
    return -np.sum(L)

#***********************Pielou Evenness****************************************************

def pielou_evenness(df_year, Hmax, attribute='Basal_area_ha'):
    """
    Calcule l'équité de Pielou pour les données d'une année donnée.
    
    Arguments:
    df_year -- DataFrame contenant les données pour une année spécifique.
    Hmax -- Valeur maximale de l'indice de Shannon.
    attribute -- Attribut pour lequel calculer l'équité de Pielou (par défaut 'Basal_area_ha').
    
    Retourne:
    Équité de Pielou pour les données fournies.
    """
    # Calculer le nombre d'espèces
    S = df_year[df_year["specie"] != "total"].shape[0]
    # Calculer l'indice de Shannon
    H = shannon(df_year)
    # Calculer l'équité de Pielou
    return H / Hmax

#***********************Output************************************************

# Définir le répertoire de sortie où se trouvent les fichiers de données
output_500 = "/home/loubna/bibliographie/canew/data_test_/output-Cmd_test_1.txt"

#******************************Read file ************************************
# Trouver tous les fichiers correspondant au modèle spécifié dans le répertoire de sortie
file_viables = glob(os.path.join(output_500, 'individu*_evolEtat.txt'))
#*****************************Control*****************************
def V(file_path):
    """
    Lit un fichier et extrait des valeurs spécifiques en les divisant en morceaux.
    
    Arguments:
    file_path -- Chemin du fichier à lire.
    
    Retourne:
    Quatre listes extraites du fichier :
    - V_teta: Le premier élément de chaque morceau.
    - V_tp: Le deuxième élément de chaque morceau.
    - V_Gobj: Le troisième élément de chaque morceau.
    - V_Cf: Un tuple des trois dernières valeurs de chaque morceau.
    """
    with open(file_path, 'r') as file:
        # Lire la première ligne du fichier
        line = file.readline().strip()
    
    # Extraire les valeurs après le premier deux-points et les diviser en une liste
    genome_values = line.split(':')[1].strip().split(', ')
    
    # Diviser la liste en morceaux de 6 éléments chacun
    n = 6
    serie_divisee = [genome_values[i:i + n] for i in range(0, len(genome_values), n)]
    
    # Filtrer les morceaux non nuls (c'est-à-dire contenant au moins une valeur différente de '0.0')
    series_non_nulles = [s for s in serie_divisee if any(val != '0.0' for val in s)]
    
    # Si le dernier morceau contient uniquement des '0.0', l'exclure
    if any(val == '0.0' for val in series_non_nulles[-1]):
        series_non_nulles = series_non_nulles[:-1]
    
    # Extraire les valeurs spécifiques des morceaux
    V_teta = [s[0] for s in serie_divisee]
    V_tp = [s[1] for s in serie_divisee]
    V_Gobj = [s[2] for s in serie_divisee]
    
    # Extraire un tuple des trois dernières valeurs de chaque morceau
    V_Cf = [(s[3], s[4], s[5]) for s in serie_divisee]
    
    return V_teta, V_tp, V_Gobj, V_Cf

#****************************Moy patch *******************************************
def moy_patch(file_path):
    """
    Calcule la moyenne des colonnes numériques groupées par année et espèce à partir d'un fichier.
    
    Arguments:
    file_path -- Chemin du fichier CSV à lire.
    
    Retourne:
    DataFrame avec la moyenne des colonnes numériques, groupées par année et espèce.
    """
    # Lire les données du fichier CSV en sautant les 3 premières lignes
    data = pd.read_csv(file_path, delim_whitespace=True, skiprows=3)
    # Supprimer la dernière ligne (potentiellement vide ou non nécessaire)
    data = data.iloc[:8800]
    
    # Définir les colonnes numériques pour le calcul de la moyenne
    numeric_cols = [ 'patchId',  'Nb_tree_ha', 'Basal_area_ha',
                     'Diameter_mean_ha', 'Diameter_std_ha', 'Height_max_ha',
                     'Height_mean_ha', 'Height_std_ha', 'droughtIndex_mean',
                     'light_Avaibility_mean', 'Gini']
    
    # Convertir les colonnes numériques en float
    data[numeric_cols] = data[numeric_cols].astype(float)
    # Calculer la moyenne des colonnes numériques, groupées par année et espèce
    grouped = data.groupby(['#year', 'specie'])[numeric_cols].mean().reset_index()
    
    # Supprimer la colonne 'patchId' du DataFrame résultant
    grouped = grouped.drop("patchId", axis=1)
    
    return grouped

#******************************Calcul du nombre des espèces************************
def calcul_Nb_species(df_year, attribute='Nb_tree_ha'):
    """
    Calcule le nombre d'espèces uniques dans les données d'une année donnée, en excluant 'total'.
    
    Arguments:
    df_year -- DataFrame contenant les données pour une année spécifique.
    attribute -- Attribut utilisé pour vérifier les valeurs non nulles (par défaut 'Nb_tree_ha').
    
    Retourne:
    Nombre d'espèces uniques présentes dans les données.
    """
    N = 0
    species = df_year['specie'].unique()
    for sp in species:
        if sp != 'total':
            values = df_year[df_year["specie"] == sp][attribute].astype(float).values
            if len(values) > 0 and values[0] != 0:
                N += 1
    return N

#******************************Moy patch and Total ***********************************
def moy_patch_total(file_path):
    """
    Calcule les statistiques moyennes pour chaque année en utilisant les données du fichier.
    
    Arguments:
    file_path -- Chemin du fichier CSV à lire.
    
    Retourne:
    DataFrame avec les statistiques calculées pour chaque année, incluant la moyenne des attributs et les indices de biodiversité.
    """
    L = []
    # Lire les données du fichier CSV
    data = pd.read_csv(file_path, delim_whitespace=True, skiprows=3)
    # Supprimer la dernière ligne (potentiellement vide ou non nécessaire)
    data = data.iloc[:-1]
    # Calculer les moyennes par année et espèce
    data_sh = moy_patch(file_path)
    
    # Calculer les statistiques pour chaque année
    for year in data["#year"].unique():
        data_year = data[data["#year"] == year]
        data_total = data_year[data_year["specie"] == "total"]
        data_W_sp = data_total.drop(["specie", "patchId", "#year"], axis=1)
        df = data_W_sp.astype(float).mean().to_frame().T
        df["#year"] = year
        # Ajouter l'indice de Shannon basé sur la surface basale
        df['Shannon_basal'] = shannon_Basal_ARea(data_sh[data_sh["#year"] == year])
        # Ajouter le nombre d'espèces
        df["n_specie"] = calcul_Nb_species(data_sh[data_sh["#year"] == year])
        L.append(df)
    
    # Combiner les résultats de toutes les années en un seul DataFrame
    final_df = pd.concat(L, ignore_index=True)
    
    # Calculer la valeur maximale de l'indice de Shannon pour normaliser l'équité de Pielou
    Hmax_global = final_df["Shannon_basal"].max()
    final_df["Pielou"] = final_df["Shannon_basal"] / Hmax_global
    
    # Sélectionner les colonnes pertinentes pour le résultat final
    final_df = final_df[["#year", "Basal_area_ha", "Gini", "n_specie", "Pielou", "Nb_tree_ha", "droughtIndex_mean", "light_Avaibility_mean"]]
    return final_df
#*************************************Etat controle Coupe et pas Coupe ******************************
def Etat_Control_Effet(viables_files):
    """
    Analyse les fichiers de données pour évaluer les effets de la coupe sur divers attributs écologiques.
    
    Arguments:
    viables_files -- Liste des chemins vers les fichiers contenant les données à analyser.
    
    Retourne:
    DataFrame combiné avec les résultats des effets de coupe pour chaque fichier.
    """
    Control_etat = {}
    final_df = pd.DataFrame()
    all_data_file = []
    j = 0
    
    for files in viables_files:
        print(j)
        # Lire les données à partir du fichier
        data = pd.read_csv(files, delim_whitespace=True, skiprows=3)
        data = data.iloc[:-1]  # Supprimer la dernière ligne
        years = data['#year'].unique()
        
        # Déterminer les années multiples de 5
        year_5 = [years[i-1] for i in range(5, len(years), 5)]
        # Calculer les moyennes pour chaque année
        data_moy_patch = moy_patch_total(files)
        
        L = []
        # Extraire les valeurs spécifiques du fichier
        V_teta, V_tp, V_Gobj, V_Cf = V(files)
        
        for i in range(len(V_teta)-1):
            year = year_5[i]
            year_suivant = year_5[i+1] if i+1 < len(year_5) else years[len(years)-1]
            indice_suivant = trouver_teta_suivant(V_teta, i)
            teta_s = int(float(V_teta[indice_suivant])) 
            
            # Filtrer les données pour l'année courante
            data_year_5 = data_moy_patch[data_moy_patch["#year"] == year]
            
            # Copier les données pour modification
            data_year_5_modified = data_year_5.copy()
            data_year_5_modified['year'] = year
            # Ajouter des colonnes pour les effets de coupe et les valeurs suivantes
            data_year_5_modified["Coupe"] = 1 if V_teta[i] != '0.0' else 0
            data_year_5_modified["Peilou_suivant"] = data_moy_patch[data_moy_patch["#year"] == year_suivant]['Pielou'].values
            data_year_5_modified['n_specie_suivant'] = data_moy_patch[data_moy_patch["#year"] == year_suivant]['n_specie'].values
            data_year_5_modified["Basal_area_suivant"] = data_moy_patch[data_moy_patch["#year"] == year_suivant]['Basal_area_ha'].values
            data_year_5_modified["droughtIndex_mean_suivant"] = data_moy_patch[data_moy_patch["#year"] == year_suivant]['droughtIndex_mean'].values
            data_year_5_modified["light_Avaibility_mean_suivant"] = data_moy_patch[data_moy_patch["#year"] == year_suivant]['light_Avaibility_mean'].values
            data_year_5_modified["Gini_suivant"] = data_moy_patch[data_moy_patch["#year"] == year_suivant]['Gini'].values
            data_year_5_modified["Nb_tree_ha_suivant"] = data_moy_patch[data_moy_patch["#year"] == year_suivant]['Nb_tree_ha'].values
            data_year_5_modified["teta"] = float(V_teta[i])
            data_year_5_modified["tp"] = float(V_tp[i])
            data_year_5_modified["Gobj"] = float(V_Gobj[i])
            data_year_5_modified["Cf1"] = float(float(V_Cf[i][0]))
            data_year_5_modified["Cf2"] = float(float(V_Cf[i][1]))
            data_year_5_modified["Cf3"] = float(float(V_Cf[i][2]))
            data_year_5_modified["teta_s"] = teta_s
            data_year_5_modified["file"] = j
            L.append(data_year_5_modified)
        
        # Combiner les données de l'année courante
        final_df = pd.concat(L, ignore_index=True)
        all_data_file.append(final_df)
        j += 1
    
    # Combiner les résultats de tous les fichiers
    final_data_file = pd.concat(all_data_file)
    return final_data_file

# Exécuter la fonction et filtrer les résultats pour les lignes où la coupe a eu lieu
Etat_Effet = Etat_Control_Effet(file_viables)
Etat_coupe = Etat_Effet[Etat_Effet["Coupe"] == 1]
print(Etat_Effet)
Etat_coupe.to_csv("/home/loubna/bibliographie/canew/data_test_/E_U.csv", index=False)

# Lire un fichier de données de contrôle
file_data = pd.read_csv("/home/loubna/bibliographie/canew/data_test_/Control_etat.csv")
print(file_data)
Etat_Effet_test = Etat_Effet.copy()

#******************************Matrice de dispersion************************************

#*************************************************Ajout d'intervalle *******************************************************

def ajouter_intervalles_pielou(data, colonne_pielou='Pielou', col_pielou_suivant="Peilou_suivant",
                                col_Basal="Basal_area_ha", col_basal_suivant="Basal_area_suivant",
                                col_Gini="Gini", col_Gini_suivante="Gini_suivant"):
    """
    Ajoute des intervalles de valeurs pour plusieurs colonnes dans un DataFrame.
    
    Arguments:
    data -- DataFrame contenant les données à traiter.
    colonne_pielou -- Nom de la colonne contenant les valeurs de Pielou (par défaut 'Pielou').
    col_pielou_suivant -- Nom de la colonne contenant les valeurs de Pielou suivantes (par défaut 'Peilou_suivant').
    col_Basal -- Nom de la colonne contenant les valeurs de la surface basale (par défaut 'Basal_area_ha').
    col_basal_suivant -- Nom de la colonne contenant les valeurs de la surface basale suivante (par défaut 'Basal_area_suivant').
    col_Gini -- Nom de la colonne contenant les valeurs de Gini (par défaut 'Gini').
    col_Gini_suivante -- Nom de la colonne contenant les valeurs de Gini suivantes (par défaut 'Gini_suivant').
    
    Retourne:
    DataFrame avec les intervalles ajoutés.
    """
    # Définir les intervalles pour chaque colonne
    intervalles = [[0, 0.6], [0.6, 0.7], [0.7, 0.8], [0.8, 0.9], [0.9, 1]]
    intervalles_Basal = [[data["Basal_area_ha"].min(), 15], [15, 20], [20, 25], [25, 30],
                         [30, 35], [30, data["Basal_area_ha"].max()]]
    intervalles_Gini = [[data["Gini"].min(), 0.35], [0.35, 0.4], [0.4, 0.45], [0.45, 0.5],
                        [0.5, data["Gini"].max()]]
    intervalles_Nbtree = [[data["Nb_tree_ha"].min(), 2000], [2000, 4000], [4000, 6000],
                          [6000, 8000], [8000, data["Nb_tree_ha"].max()]]
    intervalles_Drought = [[data["droughtIndex_mean"].min(), 0.05], [0.05, 0.1], [0.1, 0.15],
                           [0.15, 0.2], [0.2, data["droughtIndex_mean"].max()]]
    intervalles_Light = [[data["light_Avaibility_mean"].min(), 0.1], [0.1, 0.15], [0.15, 0.2],
                         [0.25, data["light_Avaibility_mean"].max()]]
    
    # Fonction pour déterminer l'intervalle de Pielou
    def trouver_intervalle(valeur):
        for i, (lower, upper) in enumerate(intervalles):
            if lower <= valeur <= upper:
                return i
        return np.nan
    
    # Fonction pour déterminer l'intervalle de la surface basale
    def trouver_intervalle_G(valeur):
        for i, (lower, upper) in enumerate(intervalles_Basal):
            if lower <= valeur <= upper:
                return i
        return np.nan
    
    # Fonction pour déterminer l'intervalle de Gini
    def trouver_intervalle_Gini(valeur):
        for i, (lower, upper) in enumerate(intervalles_Gini):
            if lower <= valeur <= upper:
                return i
        return np.nan
    
    # Fonction pour déterminer l'intervalle du nombre d'arbres
    def trouver_intervalle_Nbtree(valeur):
        for i, (lower, upper) in enumerate(intervalles_Nbtree):
            if lower <= valeur <= upper:
                return i
        return np.nan
    
    # Fonction pour déterminer l'intervalle du stress hydrique
    def trouver_intervalle_drought(valeur):
        for i, (lower, upper) in enumerate(intervalles_Drought):
            if lower <= valeur <= upper:
                return i
        return np.nan
    
    # Fonction pour déterminer l'intervalle de la disponibilité de lumière
    def trouver_intervalle_light(valeur):
        for i, (lower, upper) in enumerate(intervalles_Light):
            if lower <= valeur <= upper:
                return i
        return np.nan
    
    # Appliquer les fonctions d'intervalle aux colonnes correspondantes
    data['Intervalle_Pielou'] = data[colonne_pielou].apply(trouver_intervalle)
    data['Intervalle_Pielou_suivante'] = data[col_pielou_suivant].apply(trouver_intervalle)
    data["Intervalle_Basal"] = data[col_Basal].apply(trouver_intervalle_G)
    data["Intervalle_Basal_suivante"] = data[col_basal_suivant].apply(trouver_intervalle_G)
    data["Intervalle_Gini"] = data[col_Gini].apply(trouver_intervalle_Gini)
    data["Intervalle_Gini_suivante"] = data[col_Gini_suivante].apply(trouver_intervalle_Gini)
    data["Intervalle_Drought"] = data["droughtIndex_mean"].apply(trouver_intervalle_drought)
    data["Intervalle_Drought_suivante"] = data["droughtIndex_mean_suivant"].apply(trouver_intervalle_drought)
    data["Intervalle_Light"] = data["light_Avaibility_mean"].apply(trouver_intervalle_light)
    data["Intervalle_Light_suivante"] = data["light_Avaibility_mean_suivant"].apply(trouver_intervalle_light)
    data["Intervalle_Nbtree"] = data["Nb_tree_ha"].apply(trouver_intervalle_Nbtree)
    data["Intervalle_Nbtree_suivante"] = data["Nb_tree_ha_suivant"].apply(trouver_intervalle_Nbtree)
    
    return data

# Ajouter des intervalles aux données de l'effet de coupe
data_Etat_effet = ajouter_intervalles_pielou(Etat_Effet)
print(data_Etat_effet)
#***********************************classification et ACP pour chaque classe*************************
# Sélectionner les colonnes d'intérêt pour l'analyse
data_Etat = data_Etat_effet[["Basal_area_ha", "Pielou", "droughtIndex_mean", "Nb_tree_ha", "light_Avaibility_mean", "Gini"]]

# Définir la palette de couleurs pour les clusters
def get_palette(data, palette_name='viridis'):
    """
    Génère une palette de couleurs en fonction du nombre de clusters uniques dans les données.
    
    Arguments:
    data -- DataFrame contenant une colonne 'clusters'.
    palette_name -- Nom de la palette de couleurs à utiliser (par défaut 'viridis').
    
    Retourne:
    Palette de couleurs pour les clusters.
    """
    unique_clusters = data['clusters'].unique()
    return sns.color_palette(palette_name, len(unique_clusters))

# Renommer les colonnes pour faciliter l'analyse
data_Etat.rename(columns={
    'Basal_area_ha': 'G_tot',
    'Pielou': 'E',
    'Gini': 'Gini',
    'Nb_tree_ha': 'Ntree',
    'light_Avaibility_mean': 'Light',
    'droughtIndex_mean': 'Drought'
}, inplace=True)

# Afficher les nouvelles colonnes pour vérifier le renommage
print(data_Etat.columns)

# Standardiser les données
standardized_data = (data_Etat - data_Etat.mean()) / data_Etat.std()

# Application de l'Analyse en Composantes Principales (ACP)
pca = PCA()  # Création de l'objet PCA
principal_components = pca.fit_transform(standardized_data)  # Appliquer PCA et obtenir les composantes principales
explained_variance_percentage = pca.explained_variance_ratio_ * 100  # Pourcentage de variance expliquée par chaque composante

# Trouver le nombre optimal de clusters avec K-means
wcss = []
for i in range(1, 20):
    kmeans = KMeans(n_clusters=i, init='k-means++', max_iter=400, n_init=50, random_state=0)
    kmeans.fit(principal_components)
    wcss.append(kmeans.inertia_)  # Calculer la somme des carrés intra-cluster

# Appliquer K-means avec 3 clusters (nombre choisi après analyse du WCSS)
kmeans = KMeans(n_clusters=3, init='k-means++', max_iter=400, n_init=50, random_state=0)
clusters = kmeans.fit_predict(principal_components)  # Prévoir les clusters pour chaque observation
data_Etat["clusters"] = clusters  # Ajouter les clusters au DataFrame

# Affichage de la variance expliquée par chaque composante principale
plt.figure(figsize=(8, 4))
plt.bar(range(1, pca.n_components_ + 1), pca.explained_variance_ratio_ * 100)
plt.ylabel('Variance expliquée')
plt.xlabel('Composantes principales')
plt.title('Variance expliquée par chaque composante')
plt.show()

# Obtenir la palette de couleurs pour les clusters
palette = get_palette(data_Etat, 'deep')

# Visualisation des résultats de l'ACP et des clusters avec un cercle de corrélation
plt.figure(figsize=(10, 8))
circle_radii = np.sqrt(pca.explained_variance_)  # Calculer les rayons des cercles en fonction de la variance expliquée
scaler = MinMaxScaler(feature_range=(-1, 1))  # Normaliser les composantes principales pour la visualisation
principal_components = scaler.fit_transform(principal_components)
sns.scatterplot(x=principal_components[:, 0], y=principal_components[:, 1], hue=clusters, palette=palette, s=100)

# Ajouter les vecteurs de composantes principales sur le graphique
for i in range(len(pca.components_)):
    plt.arrow(0, 0, pca.components_[0, i] * circle_radii[0], pca.components_[1, i] * circle_radii[1], color='red', alpha=0.5, linewidth=5)
    plt.text(pca.components_[0, i] * circle_radii[0] * 1.15, pca.components_[1, i] * circle_radii[1] * 1.15, data_Etat.columns[i], color='black', ha='center', va='center', fontsize=16)

# Ajouter un cercle de corrélation au graphique
circle = plt.Circle((0, 0), 1, edgecolor='b', facecolor='none')
plt.gca().add_artist(circle)
plt.title('Cercle de Corrélation et kmeans')
plt.xlabel(f'PC2 ({explained_variance_percentage[1]:.2f}% )')
plt.ylabel(f'PC1 ({explained_variance_percentage[0]:.2f}% )')
plt.grid(True)
plt.axis('equal')
plt.show()

data_Etat_effet["clusters"]=clusters

#*********************************************Matrice de transition*******************************************
def calculer_matrice_transition(data,key, col_intervalle='Intervalle_Light', col_intervalle_suivant='Intervalle_Light_suivante', col_coupe='Coupe'):
    # Obtenir les intervalles uniques
    intervalles_uniques = sorted(data[col_intervalle].unique())
    n_intervalles = len(intervalles_uniques)
    data=data[data["clusters"]==key]
    # Créer les matrices de transition
    transition_matrix_sans_coupe = pd.DataFrame(0, index=intervalles_uniques, columns=intervalles_uniques)
    transition_matrix_avec_coupe = pd.DataFrame(0, index=intervalles_uniques, columns=intervalles_uniques)
    
    # Calculer les effectifs pour les transitions
    transitions_sans_coupe = data[data[col_coupe] == 0]
    transitions_avec_coupe = data[data[col_coupe] == 1]
    
    def mettre_a_jour_matrice(df, matrice):
        for _, ligne in df.iterrows():
            i = ligne[col_intervalle]
            j = ligne[col_intervalle_suivant]
            if i in matrice.index and j in matrice.columns:
                matrice.loc[i, j] += 1
    
    mettre_a_jour_matrice(transitions_sans_coupe, transition_matrix_sans_coupe)
    mettre_a_jour_matrice(transitions_avec_coupe, transition_matrix_avec_coupe)
    
    # Calculer les probabilités
    def calculer_probabilites(matrice):
        return matrice.div(matrice.sum(axis=1), axis=0)
    
    transition_matrix_sans_coupe_prob = calculer_probabilites(transition_matrix_sans_coupe)
    transition_matrix_avec_coupe_prob = calculer_probabilites(transition_matrix_avec_coupe)
    
    return transition_matrix_sans_coupe_prob, transition_matrix_avec_coupe_prob

# Utiliser la fonction sur votre DataFrame
key=2
matrice_sans_coupe, matrice_avec_coupe = calculer_matrice_transition(data_Etat_effet,key)

print("Matrice de Transition Sans Coupe:")
print(matrice_sans_coupe)

print("\nMatrice de Transition Avec Coupe:")
print(matrice_avec_coupe)

#***************************Afficher**********************************************************
intervalles = [[0, 0.6],
               [0.6, 0.7],
               [0.7, 0.8],
               [0.8, 0.9],
               [0.9, 1]]
intervalles_Drought=[[data_Etat_effet["droughtIndex_mean"].min(),0.05],
                 [0.05,0.1],
                 [0.1,0.15],
                 [0.15,0.2],
                 [0.2,data_Etat_effet["droughtIndex_mean"].max()],
                 ]
intervalles_Basal = [[data_Etat_effet["Basal_area_ha"].min(),15],
                   [15,20],
                   [20,25],
                   [25,30],
                   
                   [30,35],
                   [30,data_Etat_effet["Basal_area_ha"].max()]
                  ]

intervalles_Light=[[data_Etat_effet["light_Avaibility_mean"].min(),0.1],
                  [0.1,0.15],
                  [0.15,0.2],
                  [0.25,data_Etat_effet["light_Avaibility_mean"].max()],
                  
                  ]
intervalles_Gini=[[data_Etat_effet["Gini"].min(),0.35],
                 [0.35,0.4],
                 [0.4,0.45],
                 [0.45,0.5],
                 [0.5,data_Etat_effet["Gini"].max()]]

intervalles_Nbtree=[[data_Etat_effet["Nb_tree_ha"].min(),2000],
                 [2000,4000],
                 [4000,6000],
                 [6000,8000],
                 [8000,data_Etat_effet["Nb_tree_ha"].max()],
                 ]

labels_intervalles = [f"{lower:.2f} - {upper:.2f}" for lower, upper in intervalles_Drought]
labels_intervalles_BA = [f"{int(lower)} - {int(upper)}" for lower, upper in intervalles_Basal]
#********************************afficher entre coupe et pas coupe******************************************************
# Créer les sous-graphiques pour les cartes de chaleur
fig, axes = plt.subplots(1, 2, figsize=(18, 8))

# Matrice sans coupe
ax = axes[0]
sns.heatmap(matrice_sans_coupe, xticklabels=labels_intervalles, yticklabels=labels_intervalles, cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax, fmt=".2f")
for i in range(matrice_sans_coupe.shape[0]):
    for j in range(matrice_sans_coupe.shape[1]):
        text = ax.text(j + 0.5, i + 0.5, f"{matrice_sans_coupe.iloc[i, j]:.2f}", ha="center", va="center", color="black", size=8)
ax.set_title('Transitions Sans Coupe')
ax.set_xlabel('Intervalle de Light(t+1)', labelpad=20)
ax.set_ylabel('Intervalle de Light(t-1)')
ax.xaxis.set_ticks_position('top')
ax.xaxis.set_label_position('top')

# Matrice avec coupe
ax = axes[1]
sns.heatmap(matrice_avec_coupe, xticklabels=labels_intervalles, yticklabels=labels_intervalles, cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax, fmt=".2f")
for i in range(matrice_avec_coupe.shape[0]):
    for j in range(matrice_avec_coupe.shape[1]):
        text = ax.text(j + 0.5, i + 0.5, f"{matrice_avec_coupe.iloc[i, j]:.2f}", ha="center", va="center", color="black", size=8)
ax.set_title('Transitions Avec Coupe')
ax.set_xlabel('Intervalle de Light(t+1)', labelpad=20)
ax.set_ylabel('Intervalle de Light(t-1)')
ax.xaxis.set_ticks_position('top')
ax.xaxis.set_label_position('top')

# Ajouter un titre global
fig.suptitle(f'Matrices de Transition des Intervalles de Light (classe={key}) ', fontsize=16)
plt.tight_layout(rect=[0, 0, 1, 0.95])

# Afficher les graphiques
plt.show()
#**************************Combinaisons******************************************************
def calculer_matrice_transition_pour_combinaisons(data,key, thetas, g_objs, col_teta='teta',col_Gobj="Gobj", col_intervalle='Intervalle_Pielou', col_intervalle_suivant='Intervalle_Pielou_suivante', col_coupe='Coupe'):
    resultats = {}
    data = data.astype(float)
    data=data[data["clusters"]==key]
    intervalles_uniques = sorted(data[col_intervalle].unique())
    for teta in thetas:
        for gobj in g_objs:
            data_filtre = data[(data[col_teta] == teta) & (data[col_Gobj] == gobj)]
            # Initialiser une nouvelle matrice de transition pour chaque theta
            transition_matrix_avec_coupe = pd.DataFrame(0, index=intervalles_uniques, columns=intervalles_uniques)

            

            # Boucler sur les données pour mettre à jour les matrices de transition
            for _, row in data_filtre.iterrows():
                i = row[col_intervalle]
                j = row[col_intervalle_suivant]
                coupe = row[col_coupe]

                if coupe == 1:  # Avec coupe
                    if i in transition_matrix_avec_coupe.index and j in transition_matrix_avec_coupe.columns:
                        transition_matrix_avec_coupe.loc[i, j] += 1

            # Réindexer pour s'assurer que toutes les combinaisons possibles sont présentes (non nécessaire ici car nous avons déjà initialisé toutes les combinaisons)
            transition_matrix_avec_coupe = transition_matrix_avec_coupe.reindex(index=intervalles_uniques, columns=intervalles_uniques, fill_value=0)
            
            
            def calculer_probabilites(matrice):
               return matrice.div(matrice.sum(axis=1), axis=0)
            
            transition_matrix_avec_coupe = calculer_probabilites(transition_matrix_avec_coupe)
            resultats[(teta, gobj)] = transition_matrix_avec_coupe
    return resultats
thetas = [5,10,15,20]
g_objs = [0.5, 0.6,0.7,0.8,0.9]
tps=[0,0.5,1]
Cf1=[0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0]
Cf2=[0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0]
Cf3=[0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0]
key=1
resultats_comb =calculer_matrice_transition_pour_combinaisons(data_Etat_effet.astype(float),key, thetas, g_objs)

# Affichage des matrices de transition pour chaque combinaison de theta et Gobj
for (teta, gobj), matrices in resultats_comb.items():
    print(f"\nMatrice de Transition pour (θ={teta}, Gobj={gobj}) (avec Coupe):")
    print(matrices)

#*********************µµµµAfficher*********************************

# Initialiser les sous-graphes (nombre de lignes et colonnes à ajuster selon le nombre de combinaisons)
fig, axes = plt.subplots(len(thetas), len(g_objs), figsize=(18, 16))
# Itérer sur les résultats pour chaque combinaison de theta et Gobj
for i, teta in enumerate(thetas):
    for j, gobj in enumerate(g_objs):
        matrice_avec_coupe = resultats_comb[(teta, gobj)]
        ax = axes[i, j]  # Déterminer la position du sous-graphe
        
        # Créer la heatmap pour la matrice avec coupe
        sns.heatmap(matrice_avec_coupe,  xticklabels=matrice_avec_coupe.columns, yticklabels=matrice_avec_coupe.index, 
                    cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax,  fmt=".2f")
        for ii in range(matrice_avec_coupe.shape[0]):
            for jj in range(matrice_avec_coupe.shape[1]):
                text = ax.text(jj + 0.5, ii + 0.5, f"{matrice_avec_coupe.iloc[ii, jj]:.2f}", ha="center", va="center", color="black", size=8)
        # Configurer les titres et labels
        ax.set_title(f'Transitions (θ={teta}, Gobj={gobj})')
        ax.set_xlabel('Pielou(t+4)', labelpad=20)
        ax.set_ylabel('Pielou(t-1)')
        ax.xaxis.set_ticks_position('top')
        ax.xaxis.set_label_position('top')
        # Ajouter les intervalles sous forme de texte
        

legend_text = "\n".join([f"Indice {k}: {label}" for k, label in enumerate(labels_intervalles)])
fig.text(0.5, -0.05, legend_text, ha='center', va='center', fontsize=12)
# Ajuster la mise en page pour éviter les chevauchements
plt.tight_layout()
plt.show()
