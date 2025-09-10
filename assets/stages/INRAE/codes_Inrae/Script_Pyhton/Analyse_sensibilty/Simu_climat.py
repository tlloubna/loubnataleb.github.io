#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Jun 11 10:37:32 2024

@author: loubna
"""

import subprocess
import time
import pandas as pd
import matplotlib.pyplot as plt
import os
import itertools
import glob
import gc

# Fonction pour modifier un fichier de configuration.
# Elle remplace le chemin du fichier climat par un nouveau fichier spécifique.
def modify_config_file(climate_file, input_file, output_file):
    with open(input_file, 'r') as file:
        lines = file.readlines()

    with open(output_file, 'w') as file:
        for line in lines:
            if 'climate_file' in line:
                file.write(f'climate_file = {climate_file}\n')
            else:
                file.write(line)

# Fonction pour exécuter une simulation en utilisant un fichier de configuration spécifique.
# Elle capture le temps d'exécution et vide les caches du système après l'exécution pour libérer de la mémoire.
def run_simulation(config_file):
    start_time = time.time()
    command = f'sh capsis.sh -p script forceps.myscripts.LM_paralle.SimulationLoubnaManagementPa  {config_file}'
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, env=os.environ, cwd='/home/loubna/bibliographie/canew')

    # Affichage de la sortie et des erreurs au fur et à mesure
    while True:
        output = process.stdout.readline()
        if output == '' and process.poll() is not None:
            break
        if output:
            print(output.strip())
    
    # Affichage des erreurs s'il y en a
    stderr = process.stderr.read()
    if stderr:
        print("Standard Error:")
        print(stderr.strip())

    end_time = time.time()
    elapsed_time = end_time - start_time
    
    # Synchroniser et vider les caches
    subprocess.run(['sudo', 'sync'])
    subprocess.run(['sudo', 'sh', '-c', 'echo 3 > /proc/sys/vm/drop_caches'])
    
    return elapsed_time

# Fonction pour analyser un fichier de données de fitness.
# Elle lit les données en ignorant les 18 premières lignes et retourne un DataFrame contenant les données.
def parse_fitness_file(file_path):
    try:
        fitness_data = pd.read_csv(file_path, delim_whitespace=True, skiprows=18)
        return fitness_data
    except Exception as e:
        print(f"Error parsing fitness file: {e}")
        return None

# Fonction pour analyser l'évolution de la fitness sur plusieurs générations.
# Elle retourne une liste de générations uniques et un DataFrame contenant le maximum de fitness par génération.
def analyze_fitness(fitness_data):
    if fitness_data is not None and '#Generation' in fitness_data.columns:
        generations = fitness_data['#Generation'].unique()
        max_fitness_per_gen = fitness_data.groupby('#Generation')['Fitness'].max().reset_index()
        return generations, max_fitness_per_gen
    else:
        print("Fitness data is invalid or missing 'Generation' column")
        return [], []

# Fonction pour tracer l'évolution de la fitness au fil des générations.
# Elle génère un graphique montrant le maximum de fitness pour chaque génération avec un label spécifique.
def plot_fitness_evolution(generations, max_fitness_per_gen, label):
    plt.plot(max_fitness_per_gen['#Generation'], max_fitness_per_gen['Fitness'], label=label)
    plt.xlabel('Generations')
    plt.ylabel('Fitness')
    plt.title('Evolution of Fitness over Generations')
    plt.legend()

# Fonction pour compter le nombre d'individus viables dans un fichier spécifique.
# Elle retourne le nombre total d'individus viables trouvés dans le fichier.
def count_viable_individuals(file_path):
    try:
        viable_individuals_data = pd.read_csv(file_path, delim_whitespace=True, skiprows=4, on_bad_lines='skip')
        num_viable_individuals = len(viable_individuals_data)
        return num_viable_individuals
    except Exception as e:
        print(f"Error reading viable individuals file: {e}")
        return 0

# Fonction pour compter le nombre total d'individus non viables dans un répertoire donné.
# Elle retourne le nombre total de fichiers correspondant aux individus non viables.
def count_NonViable_individuals(output_directory):
    try:
        total_individuals = len(glob.glob(os.path.join(output_directory, 'individu*_evolConstraints_nonViable.txt')))
        return total_individuals
    except Exception as e:
        print(f"Error counting total individuals: {e}")
        return 0

# Fonction principale qui orchestre les simulations, analyse les données de fitness,
# compte les individus viables et non viables, et enregistre les résultats pour différentes combinaisons de fichiers climatiques.
def main():
    n_rand_values = [10]
    best_to_parent_values = [5]
    Pmut_values = [0.05]
    n_init_values = [1000, 500]
    climate_files = ["climat_maille_1382_rcp4_CNRM.bern", "climat_maille_1382_rcp4_MPI.bern",
                     "climat_maille_1382_rcp8_CNRM.bern", "climat_maille_1382_rcp8_MPI.bern"]

    for climate_file in climate_files:
        input_config_file = '/home/loubna/bibliographie/canew/data_test_Climat/Fbrin_10.txt'
        output_config_file_template = '/home/loubna/bibliographie/canew/data_test_Climat/FBrin3_rcp_{climate_file}.txt'
        
        results = []
    
        output_config_file = output_config_file_template.format(climate_file=climate_file)
        modify_config_file(climate_file, input_config_file, output_config_file)
        
        # Assurez-vous que le fichier capsis.sh est exécutable
        capsis_script_path = '/home/loubna/bibliographie/canew/capsis.sh'
        os.chmod(capsis_script_path, 0o755)
        
        # Définir les variables d'environnement nécessaires pour Java
        os.environ["JAVA_HOME"] = "/usr/lib/jvm/java-8-openjdk-amd64"
        os.environ["CLASSPATH"] = "/usr/lib/jvm/java-8-openjdk-amd64/lib"
        
        elapsed_time = run_simulation(output_config_file)
        
        viable_individu_file_path = f'/home/loubna/bibliographie/canew/data_test_Climat/Foutput-FBrin3_rcp_{climate_file}.txt/allViabIndv.txt'
        output_directory_file_path = f'/home/loubna/bibliographie/canew/data_test_Climat/output-FBrin3_rcp_{climate_file}.txt'
        
        fitness_file_path = f'/home/loubna/bibliographie/canew/data_test_Climat/output-FBrin3_rcp_{climate_file}.txt/evolFitness.txt'
        
        if os.path.exists(fitness_file_path):
            fitness_data = parse_fitness_file(fitness_file_path)
            generations, max_fitness_per_gen = analyze_fitness(fitness_data)
            
            # Sauvegarder les données de fitness dans un fichier CSV
            fitness_data_c = {
                'generation': generations,
                'fitness_max': max_fitness_per_gen["Fitness"]
            }
            fitness_save = pd.DataFrame(fitness_data_c)
            fitness_save.to_csv(f'/home/loubna/bibliographie/canew/data_test_Climat/simuFitness_{climate_file}.csv')
            
            num_viable_individuals = count_viable_individuals(viable_individu_file_path)
            num_total_individuals = count_NonViable_individuals(output_directory_file_path)
            proportion_viable = num_viable_individuals / (num_total_individuals + num_viable_individuals)
            num_generations = len(generations)
           
            results.append({
                'elapsed_time': elapsed_time,
                'num_viable_individuals': num_viable_individuals,
                'num_total_individuals': num_total_individuals,
                'proportion_viable': proportion_viable,
                'num_generations': num_generations
            })
    
        else:
            print(f"File {fitness_file_path} does not exist. Skipping analysis for the combination: climate={climate_file}.")
        print("Le climat est :", climate_file)
            
    # Sauvegarder les résultats dans un fichier CSV
    results_df = pd.DataFrame(results)
    results_df.to_csv(f'/home/loubna/bibliographie/canew/data_test_Climat/simulation_results_{climate_file}.csv', index=False)
    
if __name__ == "__main__":
    main() 
