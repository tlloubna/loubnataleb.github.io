#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Jun 12 09:41:24 2024

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
from sklearn.preprocessing import MinMaxScaler
import scipy.cluster.hierarchy as sch # For HAC Dendrogram
import statistics

# Définir le répertoire des fichiers

output_500_Climat="/home/loubna/bibliographie/canew/data_test_/output-Cmd_test_1.txt"

# Trouver les fichiers viables et non viables
viables_files = glob.glob(os.path.join(output_500_Climat, 'individu*_evolConstraints.txt'))

def V(file_path):
    with open(file_path, 'r') as file:
        line = file.readline().strip()
    
    genome_values = line.split(':')[1].strip().split(', ')
    
    # Diviser la série en 6 morceaux
    n = 6
    serie_divisee = [genome_values[i:i + n] for i in range(0, len(genome_values), n)]
    
    # Filtrer les séries non nulles
    series_non_nulles = [s for s in serie_divisee if any(val != '0.0' for val in s)]
    
    if any(val == '0.0' for val in series_non_nulles[-1]):
        series_non_nulles = series_non_nulles[:-1]
    
    # Récupérer le deuxième élément et le troisième élément de chaque série non nulle
    V_teta = [s[0] for s in series_non_nulles]
    V_tp = [s[1] for s in series_non_nulles]
    V_Gobj = [s[2] for s in series_non_nulles]
    
    # Récupérer un tuple des trois dernières valeurs de chaque série non nulle
    V_Cf = [(s[3], s[4], s[5]) for s in series_non_nulles]
    
    return V_teta, V_tp, V_Gobj, V_Cf

# Chemin vers le fichier
file_path = viables_files[1]

V_teta, V_tp, V_Gobj, V_Cf = zip(*[V(file) for file in viables_files])


#*********************Moyenner les données**********************************************
def extract_parameters(file_path, viable):
    try:
        if viable:
            data = pd.read_csv(file_path, delim_whitespace=True, skiprows=1)
            data = data.iloc[:-1]  # Supprimer la dernière ligne
        else:
            data = pd.read_csv(file_path, delim_whitespace=True, comment="#", header=None)
            data.columns = ['year', 'idPatch', 'nbTree_ha', 'nbTree_patch', 'basalAreaTot_ha', 
                            'basalAreaPSyl', 'basalAreaQPet', 'basalAreaTCor', 'nbDeadTree_ha', 
                            'basalAreaDead_ha', 'mortalityRate', 'coefGini', 'nbOfSpecies', 'vol_prod_ha']
        
        params = {
            'Basal_Area': data["basalAreaTot_ha" ].astype(float).mean(),
            'gini': data["coefGini"].astype(float).mean(),
            'wood_harvested': data["vol_prod_ha"].astype(float).mean(),
            'nbOfspeceies': data["nbOfSpecies"].astype(float).mean(),
            "mortalityRate":data["mortalityRate"].astype(float).mean(),
            
        }
        
        return params
    except Exception as e:
        print(f"Error reading parameters from file {file_path}: {e}")
        return None
#************************Lire Tous les fichiers
def load_and_process_files(file_paths, viable=True):
    all_params = []
    for file_path in file_paths:
        params = extract_parameters(file_path, viable)
        if params:
            all_params.append(params)
    return pd.DataFrame(all_params)
#*********************Application********************


data_viable = load_and_process_files(viables_files, True)

standardized_data = (data_viable - data_viable.mean()) / data_viable.std()
#**********************Matrice de Correlation********************
correlation_matrix = data_viable.corr(method="pearson")
plt.figure(figsize=(8, 6))
sns.heatmap(correlation_matrix,cmap='coolwarm',annot=True)
for i in range(correlation_matrix.shape[0]):
    for j in range(correlation_matrix.shape[1]):
        text = plt.text(j+0.5, i+0.5, f"{correlation_matrix.iloc[i, j]:.2f}", ha="center", va="center", color="black", size=8)
plt.title('Matrice de corrélation entre les variables')
plt.show()

#******************************************ACP

pca = PCA()
principal_components = pca.fit_transform(standardized_data)
#***********************************HAC*****************************
model = AgglomerativeClustering(n_clusters=3, affinity='euclidean', linkage='ward')
clusters = model.fit_predict(standardized_data)

#*************tracer HAC***********************************************************************
palette = ["b","y","r"]
cluster_colors = [palette[label] for label in clusters]
# Visualisation du dendrogramme
plt.figure(figsize=(10, 7))
linkage_matrix = linkage(standardized_data[:100], method='ward')

from scipy.cluster import hierarchy
plt.hist(clusters,range = (0, 5),)
distance_threshold = np.sort(linkage_matrix[:, 2])[-(3-1)]  # Trouver la distance à laquelle se fait la dernière division avant les 3 clusters
hierarchy.set_link_color_palette(palette)
dend = dendrogram(
    linkage_matrix,
    color_threshold=distance_threshold,
    above_threshold_color='grey'  # Couleur au-dessus du seuil
)
plt.axhline(y=distance_threshold, color='r', linestyle='--')


plt.title('Dendrogramme de la CHA')
plt.xlabel('Index des échantillons')
plt.ylabel('Distance de Ward')
plt.show()



# ********************Tracer ACP et Données classé par HAC
explained_variance_percentage = pca.explained_variance_ratio_ * 100
plt.figure(figsize=(8, 8))
circle_radii = np.sqrt(pca.explained_variance_)
scaler = MinMaxScaler(feature_range=(-1, 1))
principal_components=scaler.fit_transform(principal_components)

sns.scatterplot(x=principal_components[:, 0], y=principal_components[:, 1], hue=clusters, palette=palette, s=100)
for i in range(len(pca.components_)):
    plt.arrow(0, 0, pca.components_[0, i] * circle_radii[0], pca.components_[1, i] * circle_radii[1], color='red', alpha=0.5, linewidth=5)
    plt.text(pca.components_[0, i] * circle_radii[0] * 1.15, pca.components_[1, i] * circle_radii[1] * 1.15, data_viable.columns[i], color='red', ha='center', va='center', fontsize=14)
circle = plt.Circle((0, 0), 1, edgecolor='b', facecolor='none')
plt.gca().add_artist(circle)
plt.title('Cercle de Corrélation et ACP')
plt.xlabel(f'PC2 ({explained_variance_percentage[1]:.2f}% )')
plt.ylabel(f'PC1 ({explained_variance_percentage[0]:.2f}% )')
plt.grid(True)
plt.axis('equal')
plt.show()


#****************************Boxplot_ACp et classification******************
data_viable['cluster'] = clusters
# Récupérer le nombre de clusters
n_clusters = data_viable['cluster'].nunique()
# Définir les paramètres à tracer
parameters = ['Basal_Area', 'gini', 'wood_harvested', 'nbOfspeceies']
titles = ['Surface terrière', 'Coefficient de Gini', 'Bois récolté', 'Nombre d\'espèces']
# Créer une figure avec plusieurs sous-graphes
fig, axes = plt.subplots(1, len(parameters), figsize=(20, 6))
# Tracer les boxplots pour chaque paramètre
for ax, param, title in zip(axes, parameters, titles):
    box = ax.boxplot(
        [data_viable[data_viable['cluster'] == i][param] for i in range(n_clusters)],
        patch_artist=True
    )
    for patch, color in zip(box['boxes'], palette):
        patch.set_facecolor(color)
    ax.set_title(title)
    ax.set_xlabel('Cluster')
    ax.set_ylabel(param)

plt.tight_layout()
plt.show()

#**************************************Evolution _tem*******************************

Fic_tmp={}
i=0
data = pd.read_csv(viables_files[1], delim_whitespace=True, skiprows=1)
data = data.iloc[:-1]
years = sorted(data['#year'].unique())
#avant tous il faut regrouper les individus par classe 
data_year = {}  # Initialiser le dictionnaire en dehors de la boucle
clusters_frame=pd.DataFrame(viables_files)
clusters_frame['cluster'] = clusters
# Regrouper les fichiers par classe au début
clusters_files = {}
for j, cluster in enumerate(clusters_frame['cluster'].unique()):
    cluster_files = clusters_frame[clusters_frame['cluster'] == cluster][0].tolist()
    clusters_files[cluster] = cluster_files
 # Regrouper les fichiers pour chaque année dans chaque classe
for cluster, cluster_files in clusters_files.items():
    data_year[cluster] = {}  # Initialiser un dictionnaire vide pour chaque classe
    for year in years:
        data_year[cluster][year] = {}  # Initialiser un dictionnaire vide pour chaque année
        for j, file in enumerate(cluster_files):
            print(file)
            data_ = pd.read_csv(file, delim_whitespace=True, skiprows=1)
            data_ = data_.iloc[:-1]  # Supprimer la dernière ligne
            year_data = data_[data_['#year'] == year]
            
            # Ajouter les données pour chaque année dans chaque classe
            data_year[cluster][year][j] = {
                "G": year_data["basalAreaTot_ha"].tolist(),
                "Gini": year_data["coefGini"].tolist(),
                "Btot": year_data["vol_prod_ha"].tolist(),
                "sp": year_data["nbOfSpecies"].tolist()
            }
            print(cluster)
#****************************Evolution _moyennne****************
# Afficher le dictionnaire final

moy = {}  # Initialiser le dictionnaire en dehors de la boucle
std={}
for key in data_year.keys():
    moy[key] = {}
    std[key]={}
    for year in years:
        L_G = []
        L_Gini = []
        L_Btot = []
        L_sp = []
        for j, file in enumerate(cluster_files):
            L_G.extend([float(x) for x in data_year[key][year][j]["G"]])
            L_Gini.extend([float(x) for x in data_year[key][year][j]["Gini"]])
            L_Btot.extend([float(x) for x in data_year[key][year][j]["Btot"]])
            L_sp.extend([float(x) for x in data_year[key][year][j]["sp"]])
         # Calculer la moyenne pour tous les individus
        moy[key][year] = {
            "G": np.mean(L_G),
            "Gini": np.mean(L_Gini),
            "Btot": np.mean(L_Btot),
            "sp": np.mean(L_sp)
        }
        std[key][year] = {
            "G": np.std(L_G),
            "Gini": np.std(L_Gini),
            "Btot": np.std(L_Btot),
            "sp": np.std(L_sp)
        }



colors = ["b", "y", "r"]  # Liste de couleurs pour chaque classe
params = ['G', 'Gini', 'Btot', 'sp']

fig, axs = plt.subplots(2, 2, figsize=(10, 10))

for i, param in enumerate(params):
    for j, key in enumerate(moy.keys()):
        years_moy = [float(x) for x in moy[key]]
        means = [moy[key][x][param] for x in moy[key]]
        stds = [std[key][x][param] for x in std[key]]
        years_nb = list(range(len(years_moy)))

        # Calcul de l'indice de ligne et de colonne pour placer le graphique
        row = i // 2
        col = i % 2

        # Tracer la moyenne avec la couleur correspondante
        axs[row, col].plot(years_nb, means, label=f'Classe {key}', color=colors[j])

        # Ajouter la plage de l'écart type
        axs[row, col].fill_between(years_nb, [mean - std for mean, std in zip(means, stds)], [mean + std for mean, std in zip(means, stds)], alpha=0.2, color=colors[j])

        axs[row, col].set_title(f"Evolution de {param} avec plage d'écart type")
        axs[row, col].set_xlabel('Année')
        axs[row, col].set_ylabel(f'{param}')
        axs[row, col].legend()
        axs[row, col].grid(True)

plt.tight_layout()
plt.show()

#************************Tracer controle********************************
# Récupérer les trajectoires de 'tp' et les organiser par cluster
params_trajectories = {
    'V_teta': {i: [] for i in range(n_clusters)},
    'V_tp': {i: [] for i in range(n_clusters)},
    'V_Gobj': {i: [] for i in range(n_clusters)},
    'V_Cf': {i: [] for i in range(n_clusters)}
}

# Itérer sur chaque individu et son cluster associé
i=0
for idx, cluster in enumerate(clusters):
    params_trajectories['V_teta'][cluster].append(V_teta[idx])
    params_trajectories['V_tp'][cluster].append(V_tp[idx])
    params_trajectories['V_Gobj'][cluster].append(V_Gobj[idx])
    params_trajectories['V_Cf'][cluster].append(V_Cf[idx])
        
# Calculer la moyenne et l'écart-type pour chaque paramètre de contrôle et chaque classe
cluster_stats = {}
for cluster in range(n_clusters):
    L_tp=[]
    L_Gobj=[]
    L_teta=[]
    L_Cf=[]
    cluster_stats[cluster] = {
        'V_teta': [],
        'V_tp': [],
        'V_Gobj': [],
        'V_Cf':[],
    }
    for cle in params_trajectories:
        if cle =="V_tp":
            for s in params_trajectories[cle][cluster]:
                for x in s:
                    L_tp.append(float(x))
        if cle =="V_Gobj":
            for s in params_trajectories[cle][cluster]:
                for x in s:
                    L_Gobj.append(float(x))
        if cle =="V_teta":
            for s in params_trajectories[cle][cluster]:
                for x in s:
                    L_teta.append(float(x))
        
        if cle=="V_Cf":
            for s in params_trajectories[cle][cluster]:
                for x in s:
                    nombres = [float(nombre.replace(",", "")) for nombre in x]
                    L_Cf.append(np.std(nombres))
                
                
            
                        
                    
    cluster_stats[cluster]['V_tp']= L_tp
    cluster_stats[cluster]['V_teta']= L_teta
    cluster_stats[cluster]['V_Gobj']= L_Gobj
    cluster_stats[cluster]['V_Cf']= L_Cf
    
#******************Trcer Controle ************************************
fig, axs = plt.subplots(2, 2, figsize=(10, 10))

params = ["V_tp", "V_Gobj", "V_teta", "V_Cf"]
for i, param in enumerate(params):
    row = i // 2
    col = i % 2

    data = [cluster_stats[j][param] for j in range(3)]

    axs[row, col].hist(data, color=palette, edgecolor='red', hatch='/', label=['classe 0', 'classe 1', 'classe 2'], histtype='bar')
    axs[row, col].set_xlabel('valeurs')
    axs[row, col].set_ylabel('effectif')
    axs[row, col].set_title(f'Distribution de {param}')
    axs[row, col].legend()

plt.tight_layout()
plt.show()

fig, axs = plt.subplots(2, 2, figsize=(10, 10))

params = ["V_tp", "V_Gobj", "V_teta", "V_Cf"]
for i, param in enumerate(params):
    row = i // 2
    col = i % 2

    data = [cluster_stats[j][param] for j in range(3)]

    axs[row, col].set_xlabel('valeurs')
    axs[row, col].set_ylabel('densité')
    axs[row, col].set_title(f'Distribution de {param}')

    sns.kdeplot(data[0], shade=True, color=palette[0], ax=axs[row, col], label='classe 0')
    sns.kdeplot(data[1], shade=True, color=palette[1], ax=axs[row, col], label='classe 1')
    sns.kdeplot(data[2], shade=True, color=palette[2], ax=axs[row, col], label='classe 2')

    axs[row, col].legend()

plt.tight_layout()
plt.show()
