#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed May 29 13:25:43 2024

@author: loubna
"""

import random

def generate_inventory(num_patches, num_entries_per_patch, filename):
    with open(filename, 'w', encoding='utf-8') as file:
        file.write("#Forceps inventory\n\n")
        file.write(f"inventoryPatchN={num_patches}\n")
        file.write(f"inventoryPatchArea=1000\n\n")
        file.write("#patchId\ttreeId\tspeciesId\tage\tdbh\tcrownA1\tdbhInc\tslowGrowthIndex\n")
        
        treeId = 1
        species_ids = [27, 21, 5]

        for patchId in range(1, num_patches + 1):
            for _ in range(num_entries_per_patch):
                speciesId = random.choice(species_ids)
                age = random.randint(1, 100)
                dbh = round(random.uniform(5.0, 50.0), 2)
                crownA1 = 0.1
                dbhInc = round(random.uniform(0.01, 0.1), 2)
                slowGrowthIndex = random.randint(0, 1)
                
                file.write(f"{patchId}\t{treeId}\t{speciesId}\t{age}\t{dbh}\t{crownA1}\t{dbhInc}\t{slowGrowthIndex}\n")
                treeId += 1

# Exemple d'utilisation pour générer un fichier avec 30 patchs et 100 entrées par patch
generate_inventory(10, 30, '/home/loubna/bibliographie/canew/data_climat_RCP_MPI/inventaire_3sp_10patches.inv')
