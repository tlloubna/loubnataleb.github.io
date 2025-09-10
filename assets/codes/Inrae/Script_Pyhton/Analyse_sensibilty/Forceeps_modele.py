#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Fri May 31 16:12:22 2024

@author: loubna
"""

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches

# Création du schéma de la dynamique de ForCEEPS
fig, ax = plt.subplots(figsize=(14, 10))

# Ajout des boîtes représentant les composants principaux
env_box = mpatches.FancyBboxPatch((0.1, 0.7), 0.3, 0.2, boxstyle="round,pad=0.1", edgecolor='black', facecolor='lightblue', linewidth=1.5)
plant_box = mpatches.FancyBboxPatch((0.6, 0.7), 0.3, 0.2, boxstyle="round,pad=0.1", edgecolor='black', facecolor='lightgreen', linewidth=1.5)
growth_box = mpatches.FancyBboxPatch((0.1, 0.4), 0.3, 0.2, boxstyle="round,pad=0.1", edgecolor='black', facecolor='lightyellow', linewidth=1.5)
mortality_box = mpatches.FancyBboxPatch((0.6, 0.4), 0.3, 0.2, boxstyle="round,pad=0.1", edgecolor='black', facecolor='lightcoral', linewidth=1.5)
establishment_box = mpatches.FancyBboxPatch((0.1, 0.1), 0.3, 0.2, boxstyle="round,pad=0.1", edgecolor='black', facecolor='lightgrey', linewidth=1.5)
output_box = mpatches.FancyBboxPatch((0.6, 0.1), 0.3, 0.2, boxstyle="round,pad=0.1", edgecolor='black', facecolor='lightpink', linewidth=1.5)

# Ajout des boîtes au graphique
ax.add_patch(env_box)
ax.add_patch(plant_box)
ax.add_patch(growth_box)
ax.add_patch(mortality_box)
ax.add_patch(establishment_box)
ax.add_patch(output_box)

# Ajout des flèches pour représenter les interactions
ax.arrow(0.4, 0.8, 0.2, 0, head_width=0.02, head_length=0.02, fc='black', ec='black')
ax.arrow(0.4, 0.5, 0.2, 0, head_width=0.02, head_length=0.02, fc='black', ec='black')
ax.arrow(0.4, 0.2, 0.2, 0, head_width=0.02, head_length=0.02, fc='black', ec='black')

ax.arrow(0.25, 0.7, 0, -0.1, head_width=0.02, head_length=0.02, fc='black', ec='black')
ax.arrow(0.75, 0.7, 0, -0.1, head_width=0.02, head_length=0.02, fc='black', ec='black')
ax.arrow(0.25, 0.3, 0, -0.1, head_width=0.02, head_length=0.02, fc='black', ec='black')
ax.arrow(0.75, 0.3, 0, -0.1, head_width=0.02, head_length=0.02, fc='black', ec='black')

# Ajout des étiquettes pour les boîtes
ax.text(0.25, 0.8, "Modèle Environnemental", horizontalalignment='center', verticalalignment='center', fontsize=12, fontweight='bold')
ax.text(0.75, 0.8, "Modèle des Plantes", horizontalalignment='center', verticalalignment='center', fontsize=12, fontweight='bold')
ax.text(0.25, 0.5, "Croissance", horizontalalignment='center', verticalalignment='center', fontsize=12, fontweight='bold')
ax.text(0.75, 0.5, "Mortalité", horizontalalignment='center', verticalalignment='center', fontsize=12, fontweight='bold')
ax.text(0.25, 0.2, "Établissement", horizontalalignment='center', verticalalignment='center', fontsize=12, fontweight='bold')
ax.text(0.75, 0.2, "Sortie du Modèle", horizontalalignment='center', verticalalignment='center', fontsize=12, fontweight='bold')

# Ajout des descriptions
ax.text(0.25, 0.75, "Contraintes extérieures\n(climat, sol, latitude)", horizontalalignment='center', verticalalignment='center', fontsize=10)
ax.text(0.75, 0.75, "Processus individuels\n(croissance, mortalité)", horizontalalignment='center', verticalalignment='center', fontsize=10)
ax.text(0.25, 0.45, "Incrément de diamètre\net croissance", horizontalalignment='center', verticalalignment='center', fontsize=10)
ax.text(0.75, 0.45, "Probabilité de mortalité\nbasée sur les stress", horizontalalignment='center', verticalalignment='center', fontsize=10)
ax.text(0.25, 0.15, "Succès de l'établissement\ndes semis", horizontalalignment='center', verticalalignment='center', fontsize=10)
ax.text(0.75, 0.15, "Biomasse, structure\net composition de la forêt", horizontalalignment='center', verticalalignment='center', fontsize=10)

# Réglages finaux
ax.set_xlim(0, 1)
ax.set_ylim(0, 1)
ax.axis('off')

# Affichage du schéma
plt.title("Dynamique du Modèle ForCEEPS", fontsize=14, fontweight='bold')
plt.show()

