import subprocess
import time
import pandas as pd
import matplotlib.pyplot as plt
import os
import glob
from scipy.stats import ttest_ind

#il faut vous changer les chemins pour que vous puissiez lancer les simulations pour faire la comparaison entre les 10 patchs et les 30 patchs

# Fonction pour modifier un fichier de configuration en remplaçant la ligne contenant "inventory_file" 
# par une nouvelle valeur. Cette fonction permet de spécifier un nouveau fichier d'inventaire pour la simulation.
def modify_config_file(inventory_file, output_file):
    with open(output_file, 'r') as file:
        lines = file.readlines()
    
    with open(output_file, 'w') as file:
        for line in lines:
            if 'inventory_file' in line:
                file.write(f'inventory_file = {inventory_file}\n')
            else:
                file.write(line)

# Fonction pour exécuter une simulation en utilisant un script shell. 
# Elle capture et affiche la sortie standard en temps réel et retourne le temps d'exécution total.
def run_simulation(config_file):
    start_time = time.time()
    #il faut bien s'assurer de la ligne de commande qui est dans le fichier du commande.
    command = f'sh /home/loubna/bibliographie/canew/capsis.sh -p script forceps.myscripts.LM_paralle.SimulationLoubnaManagementPa {config_file}'
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
    return elapsed_time

# Fonction pour analyser un fichier de données de fitness. 
# Elle lit le fichier en sautant les 18 premières lignes et retourne un DataFrame des données.
def parse_fitness_file(file_path):
    try:
        fitness_data = pd.read_csv(file_path, delim_whitespace=True, skiprows=18, on_bad_lines='skip')
        return fitness_data
    except Exception as e:
        print(f"Error parsing fitness file: {e}")
        return None

# Fonction pour analyser les données de fitness en identifiant les générations et le maximum de fitness par génération.
# Elle retourne une liste de générations et un DataFrame du maximum de fitness pour chaque génération.
def analyze_fitness(fitness_data):
    if fitness_data is not None and '#Generation' in fitness_data.columns:
        generations = fitness_data['#Generation'].unique()
        max_fitness_per_gen = fitness_data.groupby('#Generation')['Fitness'].max().reset_index()
        return generations, max_fitness_per_gen
    else:
        print("Fitness data is invalid or missing 'Generation' column")
        return [], []

# Fonction pour tracer l'évolution de la fitness au fil des générations pour un nombre donné de patches.
# Elle génère un graphique montrant le maximum de fitness par génération.
def plot_fitness_evolution(generations, max_fitness_per_gen, num_patches):
    plt.plot(max_fitness_per_gen['#Generation'], max_fitness_per_gen['Fitness'], label=f'num_patch {num_patches}')
    plt.xlabel('Generations')
    plt.ylabel('Fitness')
    plt.title('Evolution of Fitness over Generations')
    plt.legend()
    plt.show()

# Fonction pour lister tous les fichiers d'un répertoire se terminant par un suffixe donné.
# Elle retourne une liste des chemins complets des fichiers correspondants.
def list_files_ending_with(directory_path, suffix):
    try:
        # Obtenir la liste de tous les fichiers dans le répertoire
        files = []
        for file in os.listdir(directory_path):
            if file.endswith(suffix):
                files.append(os.path.join(directory_path, file))
        return files
    except Exception as e:
        print(f"Error reading directory {directory_path}: {e}")
        return []

# Fonction pour extraire et calculer les paramètres d'intérêt à partir d'un fichier de données.
# Elle retourne un dictionnaire contenant les moyennes de plusieurs paramètres calculés à partir du fichier.
def extract_parameters(file_path, skip_last_n=0):
    try:
        data = pd.read_csv(file_path, delim_whitespace=True, skiprows=1)
        # Ignorer les dernières n lignes si nécessaire
        data = data.iloc[:len(data)-1]
        
        params = {
            'surface_terriere': data['basalAreaTot_ha'].mean(),
            'gini_index': data['coefGini'].mean(),
            'mortality_rate': data['mortalityRate'].mean(),
            'num_species': data['nbOfSpecies'].mean(),
            'wood_harvested': data['vol_prod_ha'].mean(),
            'num_trees': pd.to_numeric(data["nbTree_patch"]).mean()
        }
        
        return params
    except Exception as e:
        print(f"Error reading parameters from file {file_path}: {e}")
        return None

# Fonction principale qui orchestre l'exécution de simulations, l'analyse des données de fitness,
# et la comparaison des paramètres entre différentes configurations de simulation (10 et 30 patches).
def main():
    configs = [
        ('/path/to/inventaire_3sp_10patches.inv', '/home/loubna/bibliographie/canew/dataBasicBern/FBrin3_rcp_10.txt', 10),
        ('/path/to/inventaire_3sp_30patches.inv', '/home/loubna/bibliographie/canew/dataBasicBern/FBrin3_rcp_30.txt', 30)
    ]
    
    results = []

    for inventory_file, config_file, num_patches in configs:
        modify_config_file(inventory_file, config_file)
        
        # Assurez-vous que le fichier capsis.sh est exécutable
        capsis_script_path = '/home/loubna/bibliographie/canew/capsis.sh'
        os.chmod(capsis_script_path, 0o755)
        
        # Définir les variables d'environnement nécessaires pour Java
        os.environ["JAVA_HOME"] = "/usr/lib/jvm/java-8-openjdk-amd64"
        os.environ["CLASSPATH"] = "/usr/lib/jvm/java-8-openjdk-amd64/lib"
        
        elapsed_time = run_simulation(config_file)
        
        fitness_file_path = f'/home/loubna/bibliographie/canew/dataBasicBern/output-FBrin3_rcp_{num_patches}.txt/evolFitness.txt'
        if os.path.exists(fitness_file_path):
            fitness_data = parse_fitness_file(fitness_file_path)
            generations, max_fitness_per_gen = analyze_fitness(fitness_data)
            if len(generations) > 0 and not max_fitness_per_gen.empty:
                plot_fitness_evolution(generations, max_fitness_per_gen, num_patches)
            
            num_viable_individuals = fitness_data['Individual'].nunique() if fitness_data is not None else 0
            num_generations = len(generations)
            
            results.append({
                'num_patches': num_patches,
                'elapsed_time': elapsed_time,
                'num_viable_individuals': num_viable_individuals,
                'num_generations': num_generations
            })
        else:
            print(f"File {fitness_file_path} does not exist. Skipping analysis for {num_patches} patches.")

    results_df = pd.DataFrame(results)
    results_df.to_csv('/home/loubna/bibliographie/canew/dataBasicBern/simulation_results.csv', index=False)
    
    
    dir_10_patches = '/home/loubna/bibliographie/canew/dataBasicBern/output-FBrin3_rcp_10.txt'
    dir_30_patches = '/home/loubna/bibliographie/canew/dataBasicBern/output-FBrin3_rcp_30.txt'

    files_10_patches = list_files_ending_with(dir_10_patches, 'evolConstraints.txt')
    files_30_patches = list_files_ending_with(dir_30_patches, 'evolConstraints.txt')

    all_params_10 = []
    all_params_30 = []
    
    # Nombre de lignes à ignorer à la fin des fichiers
    skip_last_n = 1
    
    for file in files_10_patches:
        params = extract_parameters(file, skip_last_n)
        if params:
            all_params_10.append(params)
    
    for file in files_30_patches:
        params = extract_parameters(file, skip_last_n)
        if params:
            all_params_30.append(params)
    
    df_10_patches = pd.DataFrame(all_params_10)
    df_30_patches = pd.DataFrame(all_params_30)
    
    means_10 = df_10_patches.mean()
    stds_10 = df_10_patches.std()
    means_30 = df_30_patches.mean()
    stds_30 = df_30_patches.std()
    
    for param in means_10.index:
        t_stat, p_val = ttest_ind(df_10_patches[param], df_30_patches[param])
        print(f"{param}: t-statistic = {t_stat}, p-value = {p_val}")
    
    fig, ax = plt.subplots(figsize=(10, 6))
    x = range(len(means_10))
    ax.bar(x, means_10, yerr=stds_10, capsize=5, label='10 patches', alpha=0.5)
    ax.bar(x, means_30, yerr=stds_30, capsize=5, label='30 patches', alpha=0.5)
    ax.set_xticks(x)
    ax.set_xticklabels(means_10.index, rotation=45)
    ax.set_title('Comparison of Parameters Between 10 and 30 Patches')
    ax.set_ylabel('Mean Value')
    ax.legend()
    plt.tight_layout()
    plt.show()
        
if __name__ == "__main__":
    main()
