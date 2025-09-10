#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri Jul 12 09:39:03 2024

@author: loubna
"""

from itertools import product
import csv

# Définition de la liste des nombres possibles
Ensemble_10= [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100]
Ensemble_20= [0,  20,  40, 60, 80,  100]
#Le nombre des espèces à choisir 
nombre_espèce=3
# Génération de toutes les combinaisons possibles de 3 nombres avec répétition
combinaisons = list(product(Ensemble_10, repeat=nombre_espèce))

# Filtre pour trouver les combinaisons valides où la somme est égale à 1
combinaisons_valides = [comb for comb in combinaisons if sum(comb) == 100]

# Enregistrement des combinaisons valides dans un fichier CSV
combinaisons_possibles = 'combinaisons_valides.csv'
with open(combinaisons_possibles, mode='w', newline='') as fichier_csv:
    writer = csv.writer(fichier_csv)

    writer.writerows(combinaisons_valides)

print(f"{len(combinaisons_valides)} combinaisons valides ont été enregistrées dans '{combinaisons_possibles}'.")
c=[]
for x in combinaisons_valides:
    c=x
    if 100 in c:
        print(c)
len(combinaisons_valides)