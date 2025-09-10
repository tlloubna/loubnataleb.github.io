#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Jul 25 11:21:15 2024

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
# Définir le répertoire des fichiers
from sklearn.metrics import silhouette_score
import os
from glob import glob
#*********************************Shannon*************************************
def shannon(df_year):
    species = df_year['specie'].unique()
    N = df_year[df_year["specie"] != "total"]["Nb_tree_ha"].astype(float).sum()
    pi = df_year[df_year["specie"] != "total"]["Nb_tree_ha"].astype(float) / N
    
    # Remplacer les valeurs de pi égales à 0 par une petite valeur positive
    pi = pi.replace(0, 1e-9)
    
    L = pi * np.log(pi)
    return -np.sum(L)
#********************Shanon Basal AREA**************************************
def shannon_Basal_ARea(df_year):
    species = df_year['specie'].unique()
    N = df_year[df_year["specie"] != "total"]["Basal_area_ha"].astype(float).sum()
    pi = df_year[df_year["specie"] != "total"]["Basal_area_ha"].astype(float) / N
    
    # Remplacer les valeurs de pi égales à 0 par une petite valeur positive
    pi = pi.replace(0, 1e-9)
    
    L = pi * np.log(pi)
    return -np.sum(L)

#*************************************************************************
output_500="/home/loubna/bibliographie/canew/data_test_/output-Cmd_test_1.txt"
#******************************Read file Viable et Non viable  ************************************
file_Non_Viable=glob(os.path.join(output_500, 'individu*_evolEtat_nonViable.txt'))
#*******************Control*****************************
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
    V_teta = [s[0] for s in serie_divisee]
    V_tp = [s[1] for s in serie_divisee]
    V_Gobj = [s[2] for s in serie_divisee]
    
    # Récupérer un tuple des trois dernières valeurs de chaque série non nulle
    V_Cf = [(s[3], s[4], s[5]) for s in serie_divisee]
    
    return V_teta, V_tp, V_Gobj, V_Cf

#******************************Nb_species**********************************
def calcul_Nb_species(df_year, attribute='Nb_tree_ha'):
    N = 0
    species = df_year['specie'].unique()
    for sp in species:
        if sp != 'total':
            values = df_year[df_year["specie"] == sp][attribute].astype(float).values
            if len(values) > 0 and values[0] != 0:
                N += 1
    return N
#****************************Moy patch *******************************************
def moy_patch(file_path):
   data = pd.read_csv(file_Non_Viable[1], delim_whitespace=True, skiprows=3)
   data = data.iloc[:8800]  # Supprimer la dernière ligne

   # Calculer la moyenne des colonnes numériques groupées par 'year' et 'specie'
   numeric_cols =[ 'patchId',  'Nb_tree_ha', 'Basal_area_ha',
          'Diameter_mean_ha', 'Diameter_std_ha', 'Height_max_ha',
          'Height_mean_ha', 'Height_std_ha', 'droughtIndex_mean',
          'light_Avaibility_mean', 'Gini']

   data[numeric_cols] = data[numeric_cols].astype(float)
   grouped = data.groupby(['#year', 'specie'])[numeric_cols].mean().reset_index()

   grouped=grouped.drop("patchId",axis=1)
   
   return grouped


def viable_non_viable(BA, Gini, RS):
    if BA < 10 or Gini < 0.25 or Gini > 0.75 or RS < 2:
        return 0
    return 1

#******************************Moy patch and Total ***********************************
def moy_patch_total(file_path):
    L = []
    data = pd.read_csv(file_path, delim_whitespace=True, skiprows=3).iloc[:8800]
    data_sh = moy_patch(file_path)
    for year in data["#year"].unique():
        data_year = data[data["#year"] == year]
        data_total = data_year[data_year["specie"] == "total"]
        df = data_total.drop(["specie", "patchId", "#year"], axis=1).astype(float).mean().to_frame().T
        df["#year"] = int(year)
        df["n_specie"] = calcul_Nb_species(data_sh[data_sh["#year"] == year])
        BA, Gini, RS = df["Basal_area_ha"].values[0], df["Gini"].values[0], df['n_specie'].values[0]
        df["Viability"] = viable_non_viable(BA, Gini, RS)
        df['shannon']=shannon_Basal_ARea(data_sh[data_sh["#year"] == year])
        L.append(df)
    final_df = pd.concat(L, ignore_index=True)
    Hmax=final_df["shannon"].max()
    final_df["Pielou"]=final_df["shannon"]/Hmax
    final_df=final_df[["#year","Basal_area_ha","Viability","Gini","n_specie","Pielou","Nb_tree_ha","droughtIndex_mean","light_Avaibility_mean"]]
    return final_df

final_df=moy_patch_total(file_Non_Viable[1])
#******************************************Trouver l'année non viable sinon c'est t-1******************************
def trouver_Non_viabilty(data_moy_patch, year_first, year_end):
    for year in range(year_first, year_end + 1):
        data_year = data_moy_patch[data_moy_patch["#year"] == year]
        if not data_year.empty and data_year["Viability"].values[0] == 0:
            return year, 0
    return year_end, 1
#j c'est l'année de la coupe ou V_teta[j] est non nulle
def trouver_teta_suivant(V_teta, j):
    i=j+1 #le cas de 5 est inclus
    if j==15 or i==15:
        return 15
    else :
        while V_teta[i]=='0.0':
            i+=1
            if i==15: #S'il existe pas un suivant eton arrive à la fin 
                return 15
        return i
    return i
V_teta, V_tp, V_Gobj, V_Cf=V(file_Non_Viable[1]) 
print(V_teta)

k=trouver_teta_suivant(V_teta,13)
print(V_teta[k],k)


#*************************************************************************************************************************

#*********************************Calcul non viability de [t-1;t+4] à [t+1:t+4]***************************************************************

def Calcul_Etat_non_viable(file_Non_Viable):
    all_data_file = []
    z = 0
    for files in file_Non_Viable:
        z += 1
        print(z)
        if os.path.getsize(files) > 0:
            data = pd.read_csv(files, delim_whitespace=True, skiprows=3).iloc[:8800]
            years = data['#year'].unique()
            year_5 = [int(years[i]) for i in range(5, len(years), 5)]
            data_moy_patch = moy_patch_total(files)
            V_teta, V_tp, V_Gobj, V_Cf = V(files)
            
            L = []
            for year in year_5:
                j = year_5.index(year)
                teta = int(float(V_teta[j]))
                G_obj=float(V_Gobj[j])
                data_year_5_modified = pd.DataFrame()
                
                
                year_before = trouver_Non_viabilty(data_moy_patch, year - 3, year - 1)
                data_year=data_moy_patch[data_moy_patch["#year"]==year]
               
                if teta == 0:
                    data_year_5_modified.loc[0, "year_Before"] = year_before[0]
                    data_year_5_modified.loc[0, "Ba_before"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0, "Pielou"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Pielou"].values[0]
                    data_year_5_modified.loc[0, "Ntree"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Nb_tree_ha"].values[0]
                    data_year_5_modified.loc[0, "Drought"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["droughtIndex_mean"].values[0]
                    data_year_5_modified.loc[0, "light"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["light_Avaibility_mean"].values[0]
                    data_year_5_modified.loc[0, "Gini"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Gini"].values[0]
                    data_year_5_modified.loc[0,"V/Nv_before"]=year_before[1]
                    data_year_5_modified.loc[0, "Coupe"] = 0
                    data_year_5_modified.loc[0,"year_c"]=year
                    data_year_5_modified.loc[0, "teta"] = teta
                    data_year_5_modified.loc[0, "G_obj"] = G_obj
                    data_year_5_modified.loc[0, "year_after"] = year
                    data_year_5_modified.loc[0,"V/Nv_after"]=data_year["Viability"].values[0]
                    data_year_5_modified.loc[0, "Ba_after"] = data_moy_patch[data_moy_patch["#year"]==year]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0,"Gini_after"]=data_moy_patch[data_moy_patch["#year"]==year]["Gini"].values[0]
                    data_year_5_modified.loc[0,"RS_after"]=data_moy_patch[data_moy_patch["#year"]==year]["n_specie"].values[0]
                    data_year_5_modified.loc[0,"teta_s"]=0
                else:
                    #print("year",year)
                    indice_suivant = trouver_teta_suivant(V_teta, j)
                    teta_s = int(float(V_teta[indice_suivant])) 
                    year_after = trouver_Non_viabilty(data_moy_patch, year, year + teta_s - 1)
                    #print(year_after[0],teta_s)
                    data_year_5_modified.loc[0, "year_Before"] = year_before[0]
                    data_year_5_modified.loc[0, "Ba_before"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0, "Pielou"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Pielou"].values[0]
                    data_year_5_modified.loc[0, "Ntree"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Nb_tree_ha"].values[0]
                    data_year_5_modified.loc[0, "Drought"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["droughtIndex_mean"].values[0]
                    data_year_5_modified.loc[0, "light"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["light_Avaibility_mean"].values[0]
                    data_year_5_modified.loc[0, "Gini"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Gini"].values[0]
                    data_year_5_modified.loc[0,"V/Nv_before"]=year_before[1]
                    data_year_5_modified.loc[0, "Coupe"] = 1
                    data_year_5_modified.loc[0,"year_c"]=year
                    data_year_5_modified.loc[0, "teta"] = teta
                    data_year_5_modified.loc[0, "G_obj"] = G_obj
                    data_year_5_modified.loc[0, "year_after"] = year_after[0]
                    data_year_5_modified.loc[0,"V/Nv_after"]=year_after[1]
                    data_year_5_modified.loc[0, "Ba_after"] = data_moy_patch[data_moy_patch["#year"]==year_after[0]]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0,"Gini_after"]=data_moy_patch[data_moy_patch["#year"]==year_after[0]]["Gini"].values[0]
                    data_year_5_modified.loc[0,"RS_after"]=data_moy_patch[data_moy_patch["#year"]==year_after[0]]["n_specie"].values[0]
                    data_year_5_modified.loc[0,"teta_s"]=teta_s

                    
                
                
                L.append(data_year_5_modified)
                
            final_df = pd.concat(L, ignore_index=True)
            all_data_file.append(final_df)
    final_data_file = pd.concat(all_data_file, ignore_index=True)
    return final_data_file

final_data_non_viable = Calcul_Etat_non_viable(file_Non_Viable)
print(final_data_non_viable)

#************************************************************Regader de t-1 à t 
def Calcul_Etat_non_viable_1_année(file_Non_Viable):
    all_data_file = []
    z = 0
    for files in file_Non_Viable:
        z += 1
        print(z)
        if os.path.getsize(files) > 0:
            data = pd.read_csv(files, delim_whitespace=True, skiprows=3).iloc[:8800]
            years = data['#year'].unique()
            year_5 = [int(years[i]) for i in range(5, len(years), 5)]
            data_moy_patch = moy_patch_total(files)
            V_teta, V_tp, V_Gobj, V_Cf = V(files)
            
            L = []
            for year in year_5:
                j = year_5.index(year)
                teta = int(float(V_teta[j]))
                G_obj=float(V_Gobj[j])
                data_year_5_modified = pd.DataFrame()
                data_year=data_moy_patch[data_moy_patch["#year"]==year]
               
                if teta == 0:
                    data_year_5_modified.loc[0, "year_Before"] = year-1
                    data_year_5_modified.loc[0, "Ba_before"] = data_moy_patch[data_moy_patch["#year"]==year-1]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0, "Gini"] = data_moy_patch[data_moy_patch["#year"]==year-1]["Gini"].values[0]
                    data_year_5_modified.loc[0, "Ntree"] = data_moy_patch[data_moy_patch["#year"]==year-1]["Nb_tree_ha"].values[0]
                    data_year_5_modified.loc[0, "light"] = data_moy_patch[data_moy_patch["#year"]==year-1]["light_Avaibility_mean"].values[0]
                    data_year_5_modified.loc[0, "Drought"] = data_moy_patch[data_moy_patch["#year"]==year-1]["droughtIndex_mean"].values[0]
                    data_year_5_modified.loc[0, "Pielou"] = data_moy_patch[data_moy_patch["#year"]==year-1]["Pielou"].values[0]
                    data_year_5_modified.loc[0,"V/Nv_before"]=data_moy_patch[data_moy_patch["#year"]==year-1]["Viability"].values[0]
                    data_year_5_modified.loc[0, "Coupe"] = 0
                    data_year_5_modified.loc[0,"year_c"]=year
                    data_year_5_modified.loc[0, "teta"] = teta
                    data_year_5_modified.loc[0, "G_obj"] = G_obj
                    data_year_5_modified.loc[0, "year_after"] = year
                    data_year_5_modified.loc[0,"V/Nv_after"]=data_year["Viability"].values[0]
                    data_year_5_modified.loc[0, "Ba_after"] = data_moy_patch[data_moy_patch["#year"]==year]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0,"Gini_after"]=data_moy_patch[data_moy_patch["#year"]==year]["Gini"].values[0]
                    data_year_5_modified.loc[0,"RS_after"]=data_moy_patch[data_moy_patch["#year"]==year]["n_specie"].values[0]
                    data_year_5_modified.loc[0,"teta_s"]=0
                else:
                    
                    indice_suivant = trouver_teta_suivant(V_teta, j)
                    teta_s = int(float(V_teta[indice_suivant])) 
                    data_year_5_modified.loc[0, "year_Before"] = year-1
                    data_year_5_modified.loc[0, "Ba_before"] = data_moy_patch[data_moy_patch["#year"]==year-1]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0, "Gini"] = data_moy_patch[data_moy_patch["#year"]==year-1]["Gini"].values[0]
                    data_year_5_modified.loc[0, "Ntree"] = data_moy_patch[data_moy_patch["#year"]==year-1]["Nb_tree_ha"].values[0]
                    data_year_5_modified.loc[0, "light"] = data_moy_patch[data_moy_patch["#year"]==year-1]["light_Avaibility_mean"].values[0]
                    data_year_5_modified.loc[0, "Drought"] = data_moy_patch[data_moy_patch["#year"]==year-1]["droughtIndex_mean"].values[0]
                    data_year_5_modified.loc[0, "Pielou"] = data_moy_patch[data_moy_patch["#year"]==year-1]["Pielou"].values[0]
                    data_year_5_modified.loc[0,"V/Nv_before"]=data_moy_patch[data_moy_patch["#year"]==year-1]["Viability"].values[0]
                    data_year_5_modified.loc[0, "Coupe"] = 1
                    data_year_5_modified.loc[0,"year_c"]=year
                    data_year_5_modified.loc[0, "teta"] = teta
                    data_year_5_modified.loc[0, "G_obj"] = G_obj
                    data_year_5_modified.loc[0, "year_after"] = year
                    data_year_5_modified.loc[0,"V/Nv_after"]=data_year["Viability"].values[0]
                    data_year_5_modified.loc[0, "Ba_after"] = data_year["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0,"Gini_after"]=data_year["Gini"].values[0]
                    data_year_5_modified.loc[0,"RS_after"]=data_year["n_specie"].values[0]
                    data_year_5_modified.loc[0,"teta_s"]=teta_s

                    
                
                
                L.append(data_year_5_modified)
                
            final_df = pd.concat(L, ignore_index=True)
            all_data_file.append(final_df)
    final_data_file = pd.concat(all_data_file, ignore_index=True)
    return final_data_file

final_data_non_viable_1_year = Calcul_Etat_non_viable_1_année(file_Non_Viable)
print(final_data_non_viable_1_year)

#*****************************Etat [t-3;t-1] et t+4*****************************

def Calcul_Etat_non_viable_4_years(file_Non_Viable):
    all_data_file = []
    z = 0
    for files in file_Non_Viable:
        z += 1
        print(z)
        if os.path.getsize(files) > 0:
            data = pd.read_csv(files, delim_whitespace=True, skiprows=3).iloc[:8800]
            years = data['#year'].unique()
            year_5 = [int(years[i]) for i in range(5, len(years), 5)]
            data_moy_patch = moy_patch_total(files)
            V_teta, V_tp, V_Gobj, V_Cf = V(files)
            
            L = []
            for year in year_5:
                j = year_5.index(year)
                teta = int(float(V_teta[j]))
                G_obj=float(V_Gobj[j])
                data_year_5_modified = pd.DataFrame()
                
                
                year_before = trouver_Non_viabilty(data_moy_patch, year -3, year - 1)
                data_year=data_moy_patch[data_moy_patch["#year"]==year]
               
                if teta == 0:
                    data_year_5_modified.loc[0, "year_Before"] = year_before[0]
                    data_year_5_modified.loc[0, "Ba_before"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0, "Gini"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Gini"].values[0]
                    data_year_5_modified.loc[0, "Ntree"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Nb_tree_ha"].values[0]
                    data_year_5_modified.loc[0, "light"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["light_Avaibility_mean"].values[0]
                    data_year_5_modified.loc[0, "Drought"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["droughtIndex_mean"].values[0]
                    data_year_5_modified.loc[0, "Pielou"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Pielou"].values[0]
                    data_year_5_modified.loc[0,"V/Nv_before"]=year_before[1]
                    data_year_5_modified.loc[0, "Coupe"] = 0
                    data_year_5_modified.loc[0,"year_c"]=year
                    data_year_5_modified.loc[0, "teta"] = teta
                    data_year_5_modified.loc[0, "G_obj"] = G_obj
                    data_year_5_modified.loc[0, "year_after"] = year
                    data_year_5_modified.loc[0,"V/Nv_after"]=data_year["Viability"].values[0]
                    data_year_5_modified.loc[0, "Ba_after"] = data_moy_patch[data_moy_patch["#year"]==year]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0,"Gini_after"]=data_moy_patch[data_moy_patch["#year"]==year]["Gini"].values[0]
                    data_year_5_modified.loc[0,"RS_after"]=data_moy_patch[data_moy_patch["#year"]==year]["n_specie"].values[0]
                    data_year_5_modified.loc[0,"teta_s"]=0
                else:
                    #print("year",year)
                    indice_suivant = trouver_teta_suivant(V_teta, j)
                    teta_s = int(float(V_teta[indice_suivant])) 
                    year_after = trouver_Non_viabilty(data_moy_patch, year, year + 4)
                    #print(year_after[0],teta_s)
                    data_year_5_modified.loc[0, "year_Before"] = year_before[0]
                    data_year_5_modified.loc[0, "Ba_before"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0, "Gini"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Gini"].values[0]
                    data_year_5_modified.loc[0, "Ntree"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Nb_tree_ha"].values[0]
                    data_year_5_modified.loc[0, "light"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["light_Avaibility_mean"].values[0]
                    data_year_5_modified.loc[0, "Drought"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["droughtIndex_mean"].values[0]
                    data_year_5_modified.loc[0, "Pielou"] = data_moy_patch[data_moy_patch["#year"]==year_before[0]]["Pielou"].values[0]
                    data_year_5_modified.loc[0,"V/Nv_before"]=year_before[1]
                    data_year_5_modified.loc[0, "Coupe"] = 1
                    data_year_5_modified.loc[0,"year_c"]=year
                    data_year_5_modified.loc[0, "teta"] = teta
                    data_year_5_modified.loc[0, "G_obj"] = G_obj
                    data_year_5_modified.loc[0, "year_after"] = year_after[0]
                    data_year_5_modified.loc[0,"V/Nv_after"]=year_after[1]
                    data_year_5_modified.loc[0, "Ba_after"] = data_moy_patch[data_moy_patch["#year"]==year_after[0]]["Basal_area_ha"].values[0]
                    data_year_5_modified.loc[0,"Gini_after"]=data_moy_patch[data_moy_patch["#year"]==year_after[0]]["Gini"].values[0]
                    data_year_5_modified.loc[0,"RS_after"]=data_moy_patch[data_moy_patch["#year"]==year_after[0]]["n_specie"].values[0]
                    data_year_5_modified.loc[0,"teta_s"]=teta_s

                    
                
                
                L.append(data_year_5_modified)
                
            final_df = pd.concat(L, ignore_index=True)
            all_data_file.append(final_df)
    final_data_file = pd.concat(all_data_file, ignore_index=True)
    return final_data_file

final_data_non_viable_4_years = Calcul_Etat_non_viable_4_years(file_Non_Viable)
print(final_data_non_viable)

#************claasfication et ACP dans un premier temps 
data_Etat=final_data_non_viable_4_years[["Ba_before","Pielou","Gini","Ntree","light",'Drought']]

# Définir la palette de couleurs
def get_palette(data, palette_name='viridis'):
    unique_clusters = data['clusters'].unique()
    return sns.color_palette(palette_name, len(unique_clusters))
data_Etat.rename(columns={
    'Ba_before': 'G_tot',
    'Pielou': 'E',
    'Gini': 'Gini',
    'Ntree': 'Ntree',
    'light': 'Light',
    'Drought': 'Drought'
}, inplace=True)

# Afficher les nouvelles colonnes pour vérifier
print(data_Etat.columns)
standardized_data = (data_Etat - data_Etat.mean()) / data_Etat.std()

# Application de l'ACP
pca = PCA()  # Réduire à deux composantes principales
principal_components = pca.fit_transform(standardized_data)
explained_variance_percentage = pca.explained_variance_ratio_ * 100
wcss = []
for i in range(1, 20):
    kmeans = KMeans(n_clusters=i, init='k-means++', max_iter=400, n_init=50, random_state=0)
    kmeans.fit(principal_components)
    wcss.append(kmeans.inertia_)
    
kmeans = KMeans(n_clusters=3, init='k-means++', max_iter=400, n_init=50, random_state=0)
clusters = kmeans.fit_predict(principal_components)
data_Etat["clusters"]=clusters
# Affichage de la variance expliquée par chaque composante
plt.figure(figsize=(8, 4))
plt.bar(range(1, pca.n_components_ + 1), pca.explained_variance_ratio_*100)
plt.ylabel('Variance expliquée')
plt.xlabel('Composantes principales')
plt.title('Variance expliquée par chaque composante')
plt.show()

palette = get_palette(data_Etat, 'deep')
plt.figure(figsize=(10, 8))
circle_radii = np.sqrt(pca.explained_variance_)
scaler = MinMaxScaler(feature_range=(-1, 1))
principal_components=scaler.fit_transform(principal_components)
sns.scatterplot(x=principal_components[:, 0], y=principal_components[:, 1], hue=clusters, palette=palette, s=100)
for i in range(len(pca.components_)):
     plt.arrow(0, 0, pca.components_[0, i] * circle_radii[0], pca.components_[1, i] * circle_radii[1], color='red', alpha=0.5, linewidth=5)
     plt.text(pca.components_[0, i] * circle_radii[0] * 1.15, pca.components_[1, i] * circle_radii[1] * 1.15, data_Etat.columns[i], color='black', ha='center', va='center', fontsize=16)
     
circle = plt.Circle((0, 0), 1, edgecolor='b', facecolor='none')
plt.gca().add_artist(circle)
plt.title('Cercle de Corrélation et kmeans')
plt.xlabel(f'PC2 ({explained_variance_percentage[1]:.2f}% )')
plt.ylabel(f'PC1 ({explained_variance_percentage[0]:.2f}% )')
plt.grid(True)
plt.axis('equal')
plt.show()
final_data_non_viable_4_years["clusters"]=clusters
#*****************************Calcul de la matrice de transition *****************************************************
def calculer_matrice_transition(data,key, col_intervalle='V/Nv_before', col_intervalle_suivant='V/Nv_after', col_coupe='Coupe'):
    # Obtenir les intervalles uniques
    intervalles_uniques = sorted(data[col_intervalle].unique())
    data=data[data['clusters']==key]
    # Créer des matrices de transition initialisées à zéro
    transition_matrix_sans_coupe = pd.DataFrame(0, index=intervalles_uniques, columns=intervalles_uniques)
    transition_matrix_avec_coupe = pd.DataFrame(0, index=intervalles_uniques, columns=intervalles_uniques)
    
    # Boucler sur les données pour mettre à jour les matrices de transition
    for _, row in data.iterrows():
        i = row[col_intervalle]
        j = row[col_intervalle_suivant]
        coupe = row[col_coupe]
        
        if coupe == 0:  # Sans coupe
            if i in transition_matrix_sans_coupe.index and j in transition_matrix_sans_coupe.columns:
                transition_matrix_sans_coupe.loc[i, j] += 1
        elif coupe == 1:  # Avec coupe
            if i in transition_matrix_avec_coupe.index and j in transition_matrix_avec_coupe.columns:
                transition_matrix_avec_coupe.loc[i, j] += 1
    
    # Réindexer pour s'assurer que toutes les combinaisons possibles sont présentes (non nécessaire ici car nous avons déjà initialisé toutes les combinaisons)
    transition_matrix_sans_coupe = transition_matrix_sans_coupe.reindex(index=intervalles_uniques, columns=intervalles_uniques, fill_value=0)
    transition_matrix_avec_coupe = transition_matrix_avec_coupe.reindex(index=intervalles_uniques, columns=intervalles_uniques, fill_value=0)
    
    # Calculer les probabilités
    def calculer_probabilites(matrice):
       return matrice.div(matrice.sum(axis=1), axis=0)
    transition_matrix_sans_coupe = calculer_probabilites(transition_matrix_sans_coupe)
    transition_matrix_avec_coupe = calculer_probabilites(transition_matrix_avec_coupe)
    return transition_matrix_sans_coupe, transition_matrix_avec_coupe

# Utiliser la fonction sur votre DataFrame
key=1
matrice_sans_coupe, matrice_avec_coupe = calculer_matrice_transition(final_data_non_viable_1_year,key)

print(f"Matrice de Transition Sans Coupe: (classe :{key})")
print(matrice_sans_coupe)

print("\nMatrice de Transition Avec Coupe: (classe: {key})")
print(matrice_avec_coupe)  

#****************************************Afficher pour tous ************************
matrice_sans_coupe = matrice_sans_coupe.rename(index={0: 'NV', 1: 'V'},
                                                       columns={0: 'NV', 1: 'V'})
matrice_avec_coupe = matrice_avec_coupe.rename(index={0: 'NV', 1: 'V'},
                                                       columns={0: 'NV', 1: 'V'})
# Créer les sous-graphiques pour les cartes de chaleur
fig, axes = plt.subplots(1, 2, figsize=(18, 8))

# Matrice sans coupe
ax = axes[0]
sns.heatmap(matrice_sans_coupe, xticklabels=matrice_sans_coupe.columns, yticklabels=matrice_sans_coupe.columns, cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax, fmt=".2f")
for i in range(matrice_sans_coupe.shape[0]):
    for j in range(matrice_sans_coupe.shape[1]):
        text = ax.text(j + 0.5, i + 0.5, f"{matrice_sans_coupe.iloc[i, j]:.2f}", ha="center", va="center", color="black", size=16)
ax.set_title('Transitions Sans Coupe')
ax.set_xlabel('Etat(t)', labelpad=20)
ax.set_ylabel('Etat(t-1)')
ax.xaxis.set_ticks_position('top')
ax.xaxis.set_label_position('top')

# Matrice avec coupe
ax = axes[1]
sns.heatmap(matrice_avec_coupe, xticklabels=matrice_avec_coupe.columns, yticklabels=matrice_avec_coupe.columns, cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax, fmt=".2f")
for i in range(matrice_avec_coupe.shape[0]):
    for j in range(matrice_avec_coupe.shape[1]):
        text = ax.text(j + 0.5, i + 0.5, f"{matrice_avec_coupe.iloc[i, j]:.2f}", ha="center", va="center", color="black", size=16)
ax.set_title('Transitions Avec Coupe')
ax.set_xlabel('Etat(tc)', labelpad=20)
ax.set_ylabel(' Etat(tc-1)')
ax.xaxis.set_ticks_position('top')
ax.xaxis.set_label_position('top')

# Ajouter un titre global
fig.suptitle(f'Matrices de Transition classe:{key}', fontsize=16)
plt.tight_layout(rect=[0, 0, 1, 0.95])

# Afficher les graphiques
plt.show()

#*********************************Afficher pour chaque teta**************************************************
#****************************Combinaisoons entre G_obj et teta*****************************
# Fonction pour calculer la matrice de transition pour différentes valeurs de theta et Gobj
def calculer_matrice_transition_pour_combinaisons(data,key, thetas, g_objs, col_teta='teta_s', col_gobj='G_obj', col_intervalle='V/Nv_before', col_intervalle_suivant='V/Nv_after', col_coupe='Coupe'):
    resultats = {}
    data = data.astype(float)
    data=data[data["clusters"]==key]
    intervalles_uniques = sorted(data[col_intervalle].unique())
    for teta in thetas:
        for gobj in g_objs:
            data_filtre = data[(data[col_teta] == teta) & (data[col_gobj] == gobj)]
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
            transition_matrix_avec_coupe= transition_matrix_avec_coupe.rename(index={0: 'NV', 1: 'V'},
                                                                   columns={0: 'NV', 1: 'V'})
            
            def calculer_probabilites(matrice):
               return matrice.div(matrice.sum(axis=1), axis=0)
            
            transition_matrix_avec_coupe = calculer_probabilites(transition_matrix_avec_coupe)
            resultats[(teta, gobj)] = transition_matrix_avec_coupe
    return resultats

thetas = [5,10,15,20]
tps=[0,0.5,1]
g_objs = [0.5, 0.6, 0.7, 0.8, 0.9]
key=1
resultats_comb = calculer_matrice_transition_pour_combinaisons(final_data_non_viable_1_year, key,thetas, g_objs)

# Affichage des matrices de transition pour chaque combinaison de theta et Gobj
for (teta, gobj), matrices in resultats_comb.items():
    print(f"\nMatrice de Transition pour (θs={teta}, Gobj={gobj}) (classe ={key}):")
    print(matrices)
    
#*************************************Afficher Gobj et teta****************************************
# Initialiser les sous-graphes (nombre de lignes et colonnes à ajuster selon le nombre de combinaisons)
fig, axes = plt.subplots(len(thetas),len(g_objs), figsize=(18, 16))

# Itérer sur les résultats pour chaque combinaison de theta et Gobj
for i, teta in enumerate(thetas):
    for j, gobj in enumerate(g_objs):
        matrice_avec_coupe = resultats_comb[(teta, gobj)]
        ax = axes[i, j]  # Déterminer la position du sous-graphe
        
        # Créer la heatmap pour la matrice avec coupe
        sns.heatmap(matrice_avec_coupe, xticklabels=matrice_avec_coupe.columns, yticklabels=matrice_avec_coupe.index, 
                    cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax,  fmt=".2f")
        for ii in range(matrice_avec_coupe.shape[0]):
            for jj in range(matrice_avec_coupe.shape[1]):
                text = ax.text(jj + 0.5, ii + 0.5, f"{matrice_avec_coupe.iloc[ii, jj]:.2f}", ha="center", va="center", color="black", size=16)
        # Configurer les titres et labels
        ax.set_title(f'Transitions (θs={teta}, Gobj={gobj})')
        ax.set_xlabel('Etat([t,t+θs-1])', labelpad=20)
        ax.set_ylabel('Etat([t-3,t-1])')
        ax.xaxis.set_ticks_position('top')
        ax.xaxis.set_label_position('top')

# Ajuster la mise en page pour éviter les chevauchements
plt.tight_layout()
plt.show()
#******************************thetas**********************************************
def calculer_matrice_transition_pour_thetas(data, thetas, col_teta='teta', col_intervalle='V/Nv_before', col_intervalle_suivant='V/Nv_after', col_coupe='Coupe'):
    resultats = {}
    intervalles_uniques = sorted(data[col_intervalle].unique())

    for teta in thetas:
        # Initialiser une nouvelle matrice de transition pour chaque theta
        transition_matrix_avec_coupe = pd.DataFrame(0, index=intervalles_uniques, columns=intervalles_uniques)

        # Filtrer les données pour le theta courant
        data_teta = data[data[col_teta] == teta]

        # Boucler sur les données pour mettre à jour les matrices de transition
        for _, row in data_teta.iterrows():
            i = row[col_intervalle]
            j = row[col_intervalle_suivant]
            coupe = row[col_coupe]

            if coupe == 1:  # Avec coupe
                if i in transition_matrix_avec_coupe.index and j in transition_matrix_avec_coupe.columns:
                    transition_matrix_avec_coupe.loc[i, j] += 1

        # Réindexer pour s'assurer que toutes les combinaisons possibles sont présentes (non nécessaire ici car nous avons déjà initialisé toutes les combinaisons)
        transition_matrix_avec_coupe = transition_matrix_avec_coupe.reindex(index=intervalles_uniques, columns=intervalles_uniques, fill_value=0)
        transition_matrix_avec_coupe= transition_matrix_avec_coupe.rename(index={0: 'NV', 1: 'V'},
                                                               columns={0: 'NV', 1: 'V'})
        # Ajouter la matrice de transition pour le theta courant aux résultats
        resultats[teta] = transition_matrix_avec_coupe

    return resultats

resultats_thetas = calculer_matrice_transition_pour_thetas(final_data_non_viable, thetas=[5, 10, 15, 20])

for teta, matrices in resultats_thetas.items():
    print(f"\nMatrice de Transition pour theta = {teta} (avec Coupe):")
    print(matrices)

#***************************************G_obj*******************************************
def calculer_matrice_transition_pour_Gobj(data, Gobjs, col_Gobj='Gobj'):
    resultats = {}
    for Gobj in Gobjs :
        data_Gobj = data[data[col_Gobj] == Gobj]
        matrice_sans_coupe, matrice_avec_coupe = calculer_matrice_transition(data_Gobj)
        matrice_avec_coupe = matrice_avec_coupe.rename(index={0: 'NV', 1: 'V'},
                                                               columns={0: 'NV', 1: 'V'})
        resultats[Gobj] =  matrice_avec_coupe
    return resultats

resultats_Gobj = calculer_matrice_transition_pour_Gobj(final_data_non_viable,Gobjs= [0.5, 0.6, 0.7, 0.8,0.9])

for teta, matrices in resultats_Gobj.items():
    print(f"\nMatrice de Transition pour Gobj = {teta} (avec Coupe):")
    print(matrices)
    
#*********************Matrice de transition avec tp {0,0.5,1}****************************
def calculer_matrice_transition_pour_tp(data, tps, col_tp='tp'):
    resultats = {}
    for tp in tps :
        data_tp = data[data[col_tp] == tp]
        matrice_sans_coupe, matrice_avec_coupe = calculer_matrice_transition(data_tp)
        matrice_avec_coupe = matrice_avec_coupe.rename(index={0: 'NV', 1: 'V'},
                                                               columns={0: 'NV', 1: 'V'})
        resultats[tp] =  matrice_avec_coupe
    return resultats

resultats_tp = calculer_matrice_transition_pour_tp(final_data_non_viable,tps= [0, 0.5, 1])

for tp, matrices in resultats_tp.items():
    print(f"\nMatrice de Transition pour tp= {tp} (avec Coupe):")
    print(matrices)


#**************************µpour les cfs***********************************************************
Cf1=[0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0]
Cf2=[0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0]
Cf3=[0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0]
def calculer_matrice_transition_pour_combinaisons(data, Cf1,Cf2,Cf3, col_1='Cf1', col_2='Cf2', col_3='Cf3'):
    resultats = {}
    data = data.astype(float)
    for c1 in Cf1:
        for c2 in Cf2:
            for c3 in Cf3:
                if c1+c2 + c3 == 100:  # Filtrer les combinaisons valides
                    data_filtre = data[(data[col_1] == c1) & (data[col_2] == c2) ]
                    matrice_sans_coupe, matrice_avec_coupe = calculer_matrice_transition(data_filtre)
                    resultats[(c1, c2)] = matrice_avec_coupe
    return resultats

resultats_comb = calculer_matrice_transition_pour_combinaisons(final_data_non_viable, Cf1, Cf2, Cf3)
for (c1, c2), matrices in resultats_comb.items():
    print(f"\nMatrice de Transition pour (Cf1={c1}, Cf2={c2}) (avec Coupe):")
    print(matrices)
#****************************Affciher les Cfi*
# Nombre de sous-graphes par figure
nrows, ncols = 5, 5
fig_num = 1

for fig_start in range(0, len(Cf1), nrows):
    for fig_end in range(0, len(Cf2), ncols):
        fig, axes = plt.subplots(nrows, ncols, figsize=(18, 16))
        
        for i in range(nrows):
            for j in range(ncols):
                Cf1_idx = fig_start + i
                Cf2_idx = fig_end + j
                
                if Cf1_idx < len(Cf1) and Cf2_idx < len(Cf2):
                    teta = Cf1[Cf1_idx]
                    gobj = Cf2[Cf2_idx]
                    matrice_avec_coupe = resultats_comb.get((teta, gobj), pd.DataFrame(np.zeros((4, 4))))
                    ax = axes[i, j]
                    
                    # Créer la heatmap pour la matrice avec coupe
                    sns.heatmap(matrice_avec_coupe, xticklabels=matrice_avec_coupe.columns, yticklabels=matrice_avec_coupe.index, 
                                cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax, annot=True, fmt=".2f")
                    
                    # Configurer les titres et labels
                    ax.set_title(f'Transitions (Cf1={teta}, Cf2={gobj})')
                    ax.set_xlabel('Etat(t+4)', labelpad=20)
                    ax.set_ylabel('Etat(t-1)')
                    ax.xaxis.set_ticks_position('top')
                    ax.xaxis.set_label_position('top')
                else:
                    axes[i, j].axis('off')
        
        # Ajouter les intervalles sous forme de texte en bas de la figure
        
        
        # Ajuster la mise en page pour éviter les chevauchements
        plt.tight_layout()
        plt.suptitle(f'Figure {fig_num}', y=1.02)
        plt.subplots_adjust(top=0.95)
        plt.show()
        
        fig_num += 1


# Initialiser les sous-graphes
fig, axes = plt.subplots(2, 2, figsize=(18, 16))

# Itérer sur les résultats pour chaque theta
for i, (teta, matrice_avec_coupe) in enumerate(resultats_thetas.items()):
    ax = axes[i // 2, i % 2]  # Déterminer la position du sous-graphe
    
    # Créer la heatmap pour la matrice avec coupe
    sns.heatmap(matrice_avec_coupe, xticklabels=matrice_avec_coupe.columns, yticklabels=matrice_avec_coupe.index, 
                    cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax,  fmt=".2f")
    for i in range(matrice_avec_coupe.shape[0]):
        for j in range(matrice_avec_coupe.shape[1]):
            text = ax.text(j + 0.5, i + 0.5, f"{matrice_avec_coupe.iloc[i, j]:.2f}", ha="center", va="center", color="black", size=8)
    
    # Configurer les titres et labels
    ax.set_title(f'Transitions Avec Coupe (theta={teta})')
    ax.set_xlabel('Intervalle de Etat(t+4)', labelpad=20)
    ax.set_ylabel('Intervalle de Etat(t-1)')
    ax.xaxis.set_ticks_position('top')
    ax.xaxis.set_label_position('top')

# Ajuster la mise en page pour éviter les chevauchements
plt.tight_layout()
plt.show()
#*************************Afficher pour chaque Gobj***********************************
# Initialiser les sous-graphes
fig, axes = plt.subplots(2, 2, figsize=(18, 16))

# Itérer sur les résultats pour chaque theta
for i, (Gobj, matrice_avec_coupe) in enumerate(resultats_Gobj.items()):
    ax = axes[i // 2, i % 2]  # Déterminer la position du sous-graphe
    
    sns.heatmap(matrice_avec_coupe, xticklabels=matrice_avec_coupe.columns, yticklabels=matrice_avec_coupe.index, 
                    cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax,  fmt=".2f")
    for i in range(matrice_avec_coupe.shape[0]):
        for j in range(matrice_avec_coupe.shape[1]):
            text = ax.text(j + 0.5, i + 0.5, f"{matrice_avec_coupe.iloc[i, j]:.2f}", ha="center", va="center", color="black", size=8)
    
    # Configurer les titres et labels
    ax.set_title(f'Transitions Avec Coupe (Gobj={Gobj})')
    ax.set_xlabel('Intervalle de Etat(t+4)', labelpad=20)
    ax.set_ylabel('Intervalle de Etat(t-1)')
    ax.xaxis.set_ticks_position('top')
    ax.xaxis.set_label_position('top')

# Ajuster la mise en page pour éviter les chevauchements
plt.tight_layout()
plt.show()
#*************************************tps***************************
# Initialiser les sous-graphes
fig, axes = plt.subplots(1, 3, figsize=(24, 8))

# Itérer sur les résultats pour chaque tp
for i, (tp, matrice_avec_coupe) in enumerate(resultats_tp.items()):
    ax = axes[i]  # Déterminer la position du sous-graphe
    
    # Créer la heatmap pour la matrice avec coupe
    sns.heatmap(matrice_avec_coupe, xticklabels=matrice_avec_coupe.columns, yticklabels=matrice_avec_coupe.index, 
                    cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax,  fmt=".2f")
    for i in range(matrice_avec_coupe.shape[0]):
        for j in range(matrice_avec_coupe.shape[1]):
            text = ax.text(j + 0.5, i + 0.5, f"{matrice_avec_coupe.iloc[i, j]:.2f}", ha="center", va="center", color="black", size=8)
    # Configurer les titres et labels
    ax.set_title(f'Transitions Avec Coupe (tp={tp})')
    ax.set_xlabel('Intervalle de Etat(t+4)', labelpad=20)
    ax.set_ylabel('Intervalle de Etat(t-1)')
    ax.xaxis.set_ticks_position('top')
    ax.xaxis.set_label_position('top')

# Ajuster la mise en page pour éviter les chevauchements
plt.tight_layout()
plt.show()


#*******************************Classifier les états avec Kmeans*************************************
data_Etat=final_data_non_viable[['Basal_area_ha',"droughtIndex_mean","Pielou"]]
def Kmens_data(data_Etat):#H et G
    standardized_data = (data_Etat - data_Etat.mean()) / data_Etat.std()
   
    wcss = []
    for i in range(1, 20):
        kmeans = KMeans(n_clusters=i, init='k-means++', max_iter=400, n_init=50, random_state=0)
        kmeans.fit(standardized_data)
        wcss.append(kmeans.inertia_)
    
    plt.figure(figsize=(8, 6))
    plt.plot(range(1, 20), wcss)
    plt.title('Méthode du coude')
    plt.xlabel('Nombre de clusters')
    plt.ylabel('WCSS')
    plt.show()
    
    # Appliquer l'algorithme K-means avec K=3 (par exemple)
    kmeans = KMeans(n_clusters=3, init='k-means++', max_iter=400, n_init=50, random_state=0)
    clusters = kmeans.fit_predict(standardized_data)
    print(clusters)
    
    # Afficher l'effectif de chaque classe sous forme de diagramme circulaire
    unique_clusters, counts = np.unique(clusters, return_counts=True)
    
    palette = ["b", "r", "y"]  # Palette de couleurs
    
    plt.figure(figsize=(8, 6))
    plt.pie(counts, labels=[f"Classe {c}" for c in unique_clusters], colors=palette[:len(unique_clusters)], autopct='%1.1f%%')
    plt.title("Effectif de chaque classe")
    plt.axis('equal')
    plt.show()
    data_Etat["clusters"]=clusters
    return data_Etat
data_Etat_P_G_D=Kmens_data(data_Etat)
#**************************Palette de coleur **************************
# Fonction pour créer la palette de couleurs sous forme de dictionnaire
def get_palette(data, palette_name='viridis'):
    unique_clusters = data['clusters'].unique()
    palette = sns.color_palette(palette_name, len(unique_clusters))
    palette_dict = {cluster: color for cluster, color in zip(unique_clusters, palette)}
    return palette_dict
palette_dict = get_palette(data_Etat_P_G_D, 'deep')
#********************Visualisation des clusters *****************************************
# Fonction pour créer des boxplots et des diagrammes en violon
def boxplot_cluster(data_Analyse, palette_dict):
    n_clusters = data_Analyse['clusters'].nunique()
    parameters = ['droughtIndex_mean', 'Basal_area_ha', 'Pielou']
    titles = parameters
    fig, axes = plt.subplots(1, len(parameters), figsize=(20, 6))
    
    for i, (param, title) in enumerate(zip(parameters, titles)):
        palette = [palette_dict[cluster] for cluster in sorted(palette_dict.keys())]
        sns.violinplot(x='clusters', y=param, data=data_Analyse, palette=palette, ax=axes[i], whis_width=1, linewidth=2)
        axes[i].set_title(title)
        axes[i].set_ylabel(param)
    
    plt.tight_layout()
    plt.show()

boxplot_cluster(data_Etat_P_G_D, palette_dict)
#**********************************Visualisation avec des points *********************************************
from matplotlib.colors import ListedColormap
# Fonction pour tracer le scatter plot dans un sous-graphique
def plot_cluster_scatter(ax, data, palette_dict, axe):
    cluster_colors = data['clusters'].map(palette_dict)
    scatter = ax.scatter(
        data['Basal_area_ha'],
        data[axe],
        c=cluster_colors,
        s=50
    )
    ax.set_xlabel('Basal Area (ha)')
    ax.set_ylabel(f'{axe}')
    ax.set_title(f'Scatter Plot for {axe}')
    return scatter

# Fonction principale pour créer les sous-graphes et tracer les nuages de points
def plot_cluster_scatter_drought(data, palette_dict):
    fig, axes = plt.subplots(3, 2, figsize=(20, 15), constrained_layout=True)
    axes = axes.flatten()
    
    intervals = [
        (min(data["droughtIndex_mean"]), 0.05),
        (0.05, 0.1),
        (0.1, 0.15),
        (0.15, 0.2),
        (0.2, max(data["droughtIndex_mean"]))
    ]
    
    labels = [
        f'[{intervals[0][0]}-{intervals[0][1]}]',
        f'[{intervals[1][0]}-{intervals[1][1]}]',
        f'[{intervals[2][0]}-{intervals[2][1]}]',
        f'[{intervals[3][0]}-{intervals[3][1]}]',
        f'[{intervals[4][0]}-{intervals[4][1]:.2f}]'
    ]
    # Création d'une color map basée sur le dictionnaire de palettes
    all_colors = [palette_dict[cluster] for cluster in sorted(palette_dict.keys())]
    cmap = ListedColormap(all_colors)
    
    for i, (low, high) in enumerate(intervals):
        subset = data[(data["droughtIndex_mean"] >= low) & (data["droughtIndex_mean"] < high)]
        scatter = plot_cluster_scatter(axes[i], subset, palette_dict, 'Pielou')
        axes[i].set_title(f'Drought: {labels[i]}')
    sm = plt.cm.ScalarMappable(cmap=cmap, norm=plt.Normalize(vmin=min(data['clusters']), vmax=max(data['clusters'])))
    sm.set_array([])
    cbar = fig.colorbar(sm, ax=axes, orientation='horizontal', fraction=0.02, pad=0.05)
    cbar.set_ticks(np.arange(len(palette_dict)))
    cbar.set_ticklabels([f'Cluster {i}' for i in sorted(palette_dict.keys())])
    cbar.set_label('Clusters')
    plt.show()

plot_cluster_scatter_drought(data_Etat_P_G_D, palette_dict)


#****************************************************Visualisation avec les Controles**********************************
#Ajout des colonnes 
data_Control_Etat=data_Etat_P_G_D.copy()
data_Control_Etat["theta"]=final_data_non_viable["theta"]
data_Control_Etat["tp"]=final_data_non_viable["tp"]
data_Control_Etat["Gobj"]=final_data_non_viable["Gobj"]
data_Control_Etat["Cf1"]=final_data_non_viable["Cf1"]
data_Control_Etat["Cf2"]=final_data_non_viable["Cf2"]
data_Control_Etat["Cf3"]=final_data_non_viable["CF3"]
data_Control_Etat["Coupe"]=final_data_non_viable["Coupe"]

def plot_control(x_col, y_col, data):
    data_1=data[data["clusters"]==0]
    data_2=data[data["clusters"]==1]
    data_3=data[data["clusters"]==2]
    plt.scatter(data_1[y_col], data_1[x_col], marker='o', linestyle='-',color=palette_dict[0])
    plt.scatter(data_2[y_col], data_2[x_col], marker='o', linestyle='-',color=palette_dict[1])
    plt.scatter(data_3[y_col], data_3[x_col], marker='o', linestyle='-',color=palette_dict[2])
    plt.xlabel(y_col)
    plt.ylabel(x_col)
    plt.xlim(data[y_col].min()-0.1,data[y_col].max()+0.1)
    
    plt.title(f"{x_col} en fonction du {y_col}")
    plt.show()

plot_control("Basal_area_ha", "theta", data_Control_Etat[data_Control_Etat["Coupe"]==1])
#*******************************Matrice de dispersion *************************
Control_Etat=data_Control_Etat.drop(['clusters',"Cf1","Cf2","Cf3"],axis=1)
pd.plotting.scatter_matrix(Control_Etat,figsize=(10, 10))
plt.suptitle('Matrice de dispersion des variables')
plt.show()
#**********************************************Matrice de transition pour chaque classe*************************
data_Control_Etat_Cluster=data_Control_Etat.copy()
data_Control_Etat_Cluster["Viability_t-1"]=final_data_non_viable["Viability_t-1"]
data_Control_Etat_Cluster["Viability_T+1"]=final_data_non_viable["Viability_T+1"]
data_Control_Etat_Cluster["Coupe"]=final_data_non_viable["Coupe"]
final_data_non_viable["clusters"]=data_Etat_P_G_D["clusters"]
# Fonction pour calculer la matrice de transition pour différentes valeurs de theta et Gobj
def calculer_matrice_transition_pour_combinaisons_cluster(data, thetas, g_objs,cluster, col_teta='theta', col_gobj='Gobj'):
    resultats = {}
    data = data.astype(float)
    data=data[data["clusters"]==cluster]
    for teta in thetas:
        for gobj in g_objs:
            data_filtre = data[(data[col_teta] == teta) & (data[col_gobj] == gobj)]
            if not data_filtre.empty:
                matrice_sans_coupe, matrice_avec_coupe = calculer_matrice_transition(data_filtre)
                matrice_avec_coupe = matrice_avec_coupe.rename(index={0: 'NV', 1: 'V'},
                                                               columns={0: 'NV', 1: 'V'})
                resultats[(teta, gobj)] = matrice_avec_coupe
            else:
                # Créer une matrice vide si les données filtrées sont vides
                matrice_vide = pd.DataFrame(0, index=['NV', 'V'], columns=['NV', 'V'])
                resultats[(teta, gobj)] = matrice_vide
    return resultats

thetas = [5, 10, ]
g_objs = [0.5, 0.6, 0.7, 0.8,0.9]
cluster=0
resultats_comb = calculer_matrice_transition_pour_combinaisons_cluster(data_Control_Etat_Cluster, thetas, g_objs,cluster)

# Affichage des matrices de transition pour chaque combinaison de theta et Gobj
for (teta, gobj), matrices in resultats_comb.items():
    print(f"\nMatrice de Transition pour le cluster {cluster} et pour  (θ={teta}, Gobj={gobj}) (avec Coupe):")
    print(matrices)
    

# Définir les valeurs de theta et Gobj
thetas = [5,10]
g_objs = [0.5, 0.6, 0.7, 0.8, 0.9]
cluster = 2



# Création des sous-graphes pour les matrices de transition
def plot_transition_matrices(final_data_non_viable, thetas, g_objs,cluster):
    # Calculer les matrices de transition pour chaque combinaison
    resultats_comb = calculer_matrice_transition_pour_combinaisons_cluster(final_data_non_viable, thetas, g_objs, cluster)
    # Initialiser les sous-graphes (nombre de lignes et colonnes à ajuster selon le nombre de combinaisons)
    fig, axes = plt.subplots(len(thetas), len(g_objs), figsize=(18, 16))

    # Itérer sur les résultats pour chaque combinaison de theta et Gobj
    for i, teta in enumerate(thetas):
        for j, gobj in enumerate(g_objs):
            matrice_avec_coupe = resultats_comb[(teta, gobj)]
            ax = axes[i, j]  # Déterminer la position du sous-graphe
            
            # Créer la heatmap pour la matrice avec coupe
            sns.heatmap(matrice_avec_coupe, xticklabels=matrice_avec_coupe.columns, yticklabels=matrice_avec_coupe.index, 
                        cmap='YlGnBu', vmin=0, vmax=1, cbar=True, ax=ax,  fmt=".2f")
            for ii in range(matrice_avec_coupe.shape[0]):
                for jj in range(matrice_avec_coupe.shape[1]):
                    text = ax.text(jj + 0.5, ii + 0.5, f"{matrice_avec_coupe.iloc[ii, jj]:.2f}", ha="center", va="center", color="black", size=16)
            # Configurer les titres et labels
            ax.set_title(f'Transitions (θ={teta}, Gobj={gobj})')
            ax.set_xlabel('Etat(t+4)', labelpad=20)
            ax.set_ylabel(' Etat(t-1)')
            ax.xaxis.set_ticks_position('top')
            ax.xaxis.set_label_position('top')

    # Ajuster la mise en page pour éviter les chevauchements
    plt.tight_layout()
    fig.suptitle(f"Matrices de transition pour différentes combinaisons de {thetas} et {g_objs} pour la classe {cluster}", fontsize=16, y=1.02)
    plt.show()

# Tracer les matrices de transition
plot_transition_matrices(data_Control_Etat_Cluster, thetas, g_objs,cluster)

