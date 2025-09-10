#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Aug 13 09:25:05 2024

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
from sklearn.linear_model import Ridge, Lasso
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import cross_val_score
import numpy as np
from sklearn.metrics import r2_score, mean_squared_error
#lire  le fichiers csv
Fichier = "/home/loubna/loubna/bibliographie/canew/data_test_/E_U.csv"
E_U=pd.read_csv(Fichier)
E=E_U[['Basal_area_ha','Gini','Pielou','Nb_tree_ha','droughtIndex_mean','light_Avaibility_mean']]
U=E_U[['teta','tp','Gobj','Cf1','Cf2','Cf3']]


# Standardiser les données
scaler = StandardScaler()
E_scaled = scaler.fit_transform(E)

# Appliquer la régression Ridge
ridge = Ridge(alpha=1.0)  # Le paramètre alpha contrôle la régularisation
ridge.fit(E_scaled, U)

# Obtenir les scores de validation croisée pour comparer les modèles
ridge_scores = cross_val_score(ridge, E_scaled, U, cv=5, scoring='neg_mean_squared_error')

ridge_coefs = ridge.coef_


import matplotlib.pyplot as plt

# Prédire les valeurs de U à partir de E pour les deux modèles
U_pred_ridge = ridge.predict(E_scaled)
#U_pred_lasso = lasso.predict(E_scaled)

# Nom des colonnes de U et E pour les titres des graphiques
U_columns = U.index
E_columns = E.index

# Tracer les graphes
fig, axes = plt.subplots(len(U_columns), len(E_columns), figsize=(20, 20))
fig.subplots_adjust(hspace=0.4, wspace=0.4)

for i, u_col in enumerate(U_columns):
    for j, e_col in enumerate(E_columns):
        ax = axes[i, j]

        # Valeurs réelles
        ax.scatter(E.loc[e_col], U.loc[u_col], color='blue', label='Réel', s=10)

        # Valeurs prédites par Ridge
        ax.scatter(E.loc[e_col], U_pred_ridge[i,:], color='red', label='Prédit (Ridge)', s=10, alpha=0.6)
        ax.set_xlabel(e_col)
        ax.set_ylabel(u_col)
        ax.set_title(f'{u_col} vs {e_col}')

        # Affichage des légendes pour le premier graphique seulement
        if i == 0 and j == 0:
            ax.legend()


# Calcul du R^2 pour le modèle Ridge
r2_ridge = r2_score(U, U_pred_ridge, multioutput='uniform_average')
print("R^2 pour Ridge:", r2_ridge)


def predict_u(E_new, model, scaler):
  """
  Prédit les valeurs de U à partir de nouvelles valeurs de E en utilisant le modèle donné.

  Parameters:
  - E_new: array-like, shape (n_features,)
    Nouvelles valeurs des variables explicatives.
  - model: modèle de régression (par exemple, modèle Ridge ou Lasso).
  - scaler: objet StandardScaler utilisé pour la normalisation des données.

  Returns:
  - U_pred: array-like, shape (n_targets,)
    Valeurs prédites de U.
  """
  # Standardiser les nouvelles valeurs de E
  E_new_scaled = scaler.transform([E_new])

  # Prédire les valeurs de U
  U_pred = model.predict(E_new_scaled)

  return U_pred[0]

# Exemple d'une nouvelle observation E
E_new = [30.0, 0.5, 7, 0.9, 5000, 0.05, 0.1]

# Prédire U en utilisant le modèle Ridge
U_pred = predict_u(E_new, ridge, scaler)
print("Valeurs prédites de U:", U_pred)