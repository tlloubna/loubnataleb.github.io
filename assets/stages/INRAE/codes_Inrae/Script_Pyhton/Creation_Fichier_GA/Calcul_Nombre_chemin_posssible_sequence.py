#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Aug 14 15:21:45 2024

@author: loubna
"""
from collections import deque
import numpy as np

def generate_paths(matrix, start_state, path_length, state_names):
    num_states = matrix.shape[0]
    
    # Trouver l'indice correspondant à l'état de départ dans la liste des noms d'états
    start_index = state_names.index(start_state) if start_state in state_names else -1
    
    # Initialiser la queue avec le chemin de départ, si l'indice est valide
    if start_index == -1:
        return []  # Retourner une liste vide si l'état de départ n'est pas trouvé
    queue = deque([([start_state], 0)])
    
    paths = []
    
    # Processus BFS pour trouver les chemins
    while queue:
        current_path, current_length = queue.popleft()
        current_state = current_path[-1]
        current_index = state_names.index(current_state)  # Obtenir l'indice actuel du dernier état dans le chemin
        
        if current_length == path_length:
            paths.append(current_path)
            continue
        
        for next_state in range(num_states):
            if matrix[current_index, next_state] > 0:
                queue.append((current_path + [state_names[next_state]], current_length + 1))
    
    return paths

def calcul_nb(chemin):
    n = 0
    Tab = [5, 10, 15, 20]
    for x in chemin:
        if x in Tab:
            n += 1
    return n

# États renommés, remplir avec des indices si non spécifiés
state_names = [i for i in range(10)]  # Indices par défaut de 0 à 9
state_names[9] = 5  # Renommer l'état 9 en 5
state_names[3] = 20
state_names[5] = 10
state_names[8] = 15

# Matrice de transition A
A = np.zeros((10, 10))
A[0, 1] = 1
A[1, 2] = 1
A[2, 3] = 1
A[3, 0] = 1
A[3, 4] = 1
A[3, 6] = 1
A[3, 9] = 1
A[4, 5] = 1
A[5, 4] = 1
A[5, 0] = 1
A[5, 6] = 1
A[5, 9] = 1
A[6, 7] = 1
A[7, 8] = 1
A[8, 0] = 1
A[8, 4] = 1
A[8, 6] = 1
A[8, 9] = 1
A[9, 0] = 1
A[9, 4] = 1
A[9, 6] = 1
A[9, 9] = 1

start_state = 5  # Début à l'état renommé '5', qui est maintenant l'indice 9
path_length = 16  # Longueur de chemin demandée

# Générer les chemins à partir des paramètres donnés
path_5 = generate_paths(A, 5, path_length, state_names)
path_0_10= generate_paths(A, 4, path_length, state_names)
path_0_15 = generate_paths(A, 6, path_length, state_names)
path_0_20 = generate_paths(A, 0, path_length, state_names)
# Calculer et afficher les statistiques des chemins
# Calcul des combinaisons totales pour chaque path
C1 = sum((3*5*21)**calcul_nb(chemin[:16]) for chemin in path_5)
C2 = sum((3*5*21)**calcul_nb(chemin[:16]) for chemin in path_0_10)
C3 = sum((3*5*21)**calcul_nb(chemin[:16]) for chemin in path_0_15)
C4 = sum((3*5*21)**calcul_nb(chemin[:16]) for chemin in path_0_20)
total = C1 + C2 + C3 + C4

# Affichage du résultat total en notation scientifique
print('Le nombre de combinaison pour theta est', format(total, '.2e'))
import numpy as np
# Calcul et affichage du log2 du résultat total
# Conversion de total en float pour éviter les erreurs de type lors du calcul du logarithme
total_float = float(total)
log2_total = np.log2(total_float)
print("Le log2 du total des combinaisons est", log2_total)
print('le nombre des chemins possibles pour 5 est :',len(path_5))
print('le nombre des chemins possibles pour 0_10 est ',len(path_0_10))
print('le nombre des chemins possibles pour 0_15 est :' ,len(path_0_15))
print('le nombre des chemins possibles pour 0_20 est :',len(path_0_20))
for chemin in path_5:
    print(calcul_nb(chemin[:16]))
    print(chemin[:16])