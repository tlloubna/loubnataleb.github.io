#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Jun  7 13:16:38 2024

@author: loubna
"""

import random
import matplotlib.pyplot as plt
import pandas as pd 
import numpy as np
# Charger les données  !regarder bien ou se trouve les fichiers 
data_time_Pa = pd.read_csv("/home/loubna/loubna_data_forceeps/data_climat_bern/dataGa_compar_timePa/simulation_results.csv")
data_time_Seq = pd.read_csv("/home/loubna/loubna_data_forceeps/data_climat_bern/dataGa_compar_timeSeq/simulation_results.csv")

# Créer la figure avec 3 sous-figures
fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(12, 12))

# Sous-figure 1 : Elapsed_time
ax1.plot(data_time_Pa["n_init"], data_time_Pa["elapsed_time"], color="red", label="Parallèle", marker="o")
ax1.plot(data_time_Seq["n_init"], data_time_Seq["elapsed_time"], color="blue", label="Séquentiel", marker="o")
ax1.set_xlabel("n_init")
ax1.set_ylabel("elapsed_time")
ax1.set_title("Parallèle vs Séquentiel : Elapsed_time")
ax1.grid()
ax1.legend()

# Sous-figure 2 : Num_Viable
ax2.plot(data_time_Pa["n_init"], data_time_Pa["num_viable_individuals"], color="red", label="Parallèle", marker="o")
ax2.plot(data_time_Seq["n_init"], data_time_Seq["num_viable_individuals"], color="blue", label="Séquentiel", marker="o")
ax2.set_xlabel("n_init")
ax2.set_ylabel("num_Viable_individuals")
ax2.set_title("Parallèle vs Séquentiel : Num_Viable")
ax2.grid()
ax2.legend()

# Sous-figure 3 : Num_Generation
ax3.plot(data_time_Pa["n_init"], data_time_Pa["num_generations"], color="red", label="Parallèle", marker="o")
ax3.plot(data_time_Seq["n_init"], data_time_Seq["num_generations"], color="blue", label="Séquentiel", marker="o")
ax3.set_xlabel("n_init")
ax3.set_ylabel("num_generations")
ax3.set_title("Parallèle vs Séquentiel : Num_Generation")
ax3.grid()
ax3.legend()

# Ajuster l'espacement entre les sous-figures
plt.subplots_adjust(hspace=0.5)

# Afficher la figure
plt.show()
