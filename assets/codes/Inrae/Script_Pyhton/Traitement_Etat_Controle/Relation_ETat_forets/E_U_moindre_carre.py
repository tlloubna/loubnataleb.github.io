#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Aug  8 17:29:18 2024

@author: loubna
"""

import pandas as pd
import numpy as np
from scipy.linalg import lstsq
import matplotlib.pyplot as plt
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
# Charger les données
file_data = pd.read_csv("/home/loubna/loubna/bibliographie/canew/data_test_/E_U.csv")

# Définir les colonnes pour E et U
U_columns = ['teta', 'Gobj', 'tp', 'Cf1', 'Cf2', 'Cf3']
E_columns = ['Basal_area_ha', 'Gini', 'droughtIndex_mean', 'light_Avaibility_mean', 'Pielou', 'Nb_tree_ha']

# Sélectionner les colonnes pour E et U
E = file_data[E_columns].values.T
U = file_data[U_columns].values.T
num_rows_E, num_cols_E = 6,9305

num_rows_U, num_cols_U = 6,9305

# Nombre de blocs 6x6
num_blocks = num_cols_E // 6

alpha_teta_G = []
df_dic = {}
M_blocks = []
def remove_outliers(data, threshold=3.0):
    z_scores = np.abs((data - np.mean(data, axis=0)) / np.std(data, axis=0))
    mask = z_scores < threshold  # Crée un masque booléen
    return np.where(mask, data, np.nan)  # Remplace les valeurs aberrantes par NaN ou une autre valeur choisie

for i in range(num_blocks):
    # Extraire les blocs 6x6
    E_block = E[:, i*6:(i+1)*6]
    U_block = U[:, i*6:(i+1)*6]
    
    # Supprimer les valeurs aberrantes
    E_block = remove_outliers(E_block)
    U_block = remove_outliers(U_block)
    
    # Supprimer les colonnes contenant des NaN (si nécessaire)
    nan_mask = ~np.isnan(E_block).any(axis=0) & ~np.isnan(U_block).any(axis=0)
    E_block = E_block[:, nan_mask]
    U_block = U_block[:, nan_mask]
    
    # Résoudre le problème de moindres carrés
    M, residuals, rank, s = lstsq(E_block, U_block)
    
    # Calculer M_prime
    E_pseudo_inv = np.linalg.pinv(E_block)
    M_prime = np.dot(np.dot(E_block, M), E_pseudo_inv)
    
    # Calculer U_predit
    U_predit = M_prime @ E_block
   
    M_blocks.append(M_prime)


# Récupérer les valeurs de U en utilisant les coefficients
U_predicted = np.hstack([M_blocks[i]@E[:, i*6:(i+1)*6]for i in range(num_blocks)])
dic_T={}
teta=[]
G_obj=[]
tp=[]
cf1=[]
cf2=[]
cf3=[]
# Extraire les coefficients de chaque bloc
for i in range(len(M_blocks)):
    teta.append(M_blocks[i][0, :])
    G_obj.append(M_blocks[i][1, :])
    tp.append(M_blocks[i][2, :])
    cf1.append(M_blocks[i][3, :])
    cf2.append(M_blocks[i][4, :])
    cf3.append(M_blocks[i][5, :])

# Créer un dictionnaire avec les listes
dic_T = {
    "teta": teta,
    "Gobj": G_obj,
    "tp": tp,
    "cf1": cf1,
    "cf2": cf2,
    "cf3": cf3
}



data = pd.DataFrame(dic_T).T

# Initialisation des listes pour stocker les valeurs extraites
e1_t = []
e2_t = []
e3_t = []
e4_t = []
e5_t = []
e6_t = []

# Remplissage des listes avec les valeurs correspondantes
for j in range(data.shape[1]):
    row = data.loc["cf3", j]
    e1_t.append(row[0])
    e2_t.append(row[1])
    e3_t.append(row[2])
    e4_t.append(row[3])
    e5_t.append(row[4])
    e6_t.append(row[5])

# Calcul des moyennes
dic_teta_e = {
    "e1": [np.mean(e1_t)],
    "e2": [np.mean(e2_t)],
    "e3": [np.mean(e3_t)],
    "e4": [np.mean(e4_t)],
    "e5": [np.mean(e5_t)],
    "e6": [np.mean(e6_t)]
}

data_e_Cf3 = pd.DataFrame(dic_teta_e, index=["Cf3"])


data_e_tp= pd.DataFrame(dic_teta_e, index=["tp"])
data_e_teta = pd.DataFrame(dic_teta_e, index=["teta"])
data_e_Gobj = pd.DataFrame(dic_teta_e, index=["Gobj"])
data_e_Cf1 = pd.DataFrame(dic_teta_e, index=["Cf1"])
data_e_Cf2 = pd.DataFrame(dic_teta_e, index=["Cf2"])

data_tous= pd.concat([data_e_teta, data_e_Gobj,data_e_tp,data_e_Cf1,data_e_Cf2,data_e_Cf3])

print(data_tous)




U_predicted_df = pd.DataFrame(U_predicted)

# Convertir U en DataFrame pour faciliter la comparaison
U_df = pd.DataFrame(U)

# Supprimer les trois dernières colonnes de U_df
U_df_reduced = U_df.iloc[:, :-5]

# Calculer les erreurs
# Calculer les erreurs
U_predicted_df_reduced = U_predicted_df  # Assurez-vous que U_predicted_df a la même réduction
mse = mean_squared_error(U_df_reduced, U_predicted_df_reduced)
mae = mean_absolute_error(U_df_reduced, U_predicted_df_reduced)
r2 = r2_score(U_df_reduced, U_predicted_df_reduced)

print(f"Mean Squared Error (MSE): {mse}")
print(f"Mean Absolute Error (MAE): {mae}")
print(f"R^2 Score: {r2}")

E=E[:,:-5]
U=U[:,:-5]

fig, axes = plt.subplots(len(U_columns),len(E_columns), figsize=(20, 20))
fig.subplots_adjust(hspace=0.4, wspace=0.4)

for i, u_col in enumerate(U_columns):
    for j, e_col in enumerate(E_columns):
        ax = axes[i, j]
        
        # Valeurs réelles
        ax.scatter(E[j,:], U[i,:], color='blue', label='Valeurs Réelles', s=10)
        
        # Valeurs prédites
        ax.scatter(E[j,:], U_predicted[i,:], color='red', label='Valeurs Prédites', s=10, alpha=0.6)
        
        ax.set_xlabel(e_col)
        ax.set_ylabel(u_col)
        ax.set_title(f'{u_col} vs {e_col}')
        
        # Afficher la légende uniquement pour le premier graphique
        if i == 0 and j == 0:
            ax.legend()

plt.show()



