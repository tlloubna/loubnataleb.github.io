/*
 * The Forceps model.
 *
 * Copyright (C) April 2013: X. Morin (CNRS CEFE).
 *
 * This file is part of the Forceps model and is NOT free software. It is the property of its
 * authors and must not be copied without their permission. It can be shared by the modellers of the
 * Capsis co-development community in agreement with the Capsis charter
 * (http://capsis.cirad.fr/capsis/charter). See the license.txt file in the Capsis installation
 * directory for further information about licenses in Capsis.
 */

 package forceps.myscripts.LM_paralle;

 import java.io.File;
 import java.io.FileWriter;
 import java.util.StringTokenizer;
 
 import jeeb.lib.util.Check;
 import capsis.app.C4Script;
 import capsis.kernel.GModel;
 import capsis.kernel.Step;
 import forceps.extension.intervener.CepsGHAThinner2bis;
 import forceps.extension.ioformat.CepsExport;
 import forceps.extension.ioformat.CepsMeanStandExport;
 import forceps.extension.ioformat.CepsProductivityScene;
 import forceps.model.CepsEvolutionParameters;
 import forceps.model.CepsInitialParameters;
 
 /**
  * A simulation script for use a genetic algorithm with Forceps.
  *
  * <pre>
  * // Linux / Mac
  * sh capsis.sh -p script forceps.myscripts.LM_paralle.SimulationLoubnaManagementPa "Le chemin du data:loubna/data_Forceps/cmd2.txt"
  * // Windows
  * capsis -p script forceps.myscripts.LM_paralle.SimulationLoubnaManagementPa "Le chemin du data:loubna/data_Forceps/cmd2.txt"
  * </pre>
  *
  * @author M. Malara - July 2023 & M. Jourdan - March 2024 & change by L.TALEB
  */
 public class SimulationLoubnaManagementPa {

	//lire les données du fichier du commande
	 private String output_dir; // All exports go to the output dir
	 private SimulationFitnessPa simu_fitness;
	 private int indv_first_pop;
	 private int best_to_parent;
	 private int rand_indv;
	 private int stop_crit;
	 private int inter_max;
	 private int max_cnt;
	 private double proba_mutation;
	 private int RandomSeed;
	 //Le nombre de division du processuer 
	 private int nb_process;
	 //Le switch pour la simulation 
	 private int Simulation_Non_viable_fin;
	
	 //le main pour lancer la simulation avec GA .
	 public static void main(String[] args) throws Exception {

		 //pour calculer le temps du simulation de lecture des données à partir du cmd2.txt
		 long startTime,endTime;
		 

		 SimulationLoubnaManagementPa simu = new SimulationLoubnaManagementPa(args);

		

		//Le fichier est crée pour calculer le temps du simulation de chaque individus dans GA.
		 String execTimeFileName=simu.output_dir+"/execution_times.txt";
		 File execTimeFile=new File(execTimeFileName);
		 if (!execTimeFile.getParentFile().exists()) 
		  execTimeFile.getParentFile().mkdirs();
		if(!execTimeFile.exists()) 
		execTimeFile.createNewFile();

		FileWriter execTimeFw=new FileWriter(execTimeFile);
		execTimeFw.write("#Id\t  Execution (s)\t fitness \n");
		execTimeFw.close();

		 //Calculer le temps du simulation pour GA et lancer GA  pour traiter la population initiale.
		 startTime = System.nanoTime();
		 GeneticAlgoPa algo = new GeneticAlgoPa(simu.indv_first_pop, simu.best_to_parent,
																			  simu.proba_mutation, simu.rand_indv,
																			  simu.stop_crit, simu.inter_max,
																			  simu.output_dir, simu.simu_fitness);

		 //calculer le temps du simulation pour la population initiale.
		 endTime = System.nanoTime();
		 //System.out.println("Time for the pop 0 " + (endTime - startTime) + " ns");

		 //run GA
		 startTime =System.nanoTime();
		 ManagementPopPa res = algo.runAlgo(simu.max_cnt);
		 endTime = System.nanoTime();
		 //System.out.println("Time to run GA : " + (endTime - startTime) + " ns");
 
		 // Afficher le meilleur individus dans GA.
		 ManagementIndvPa best = res.getBestIndv();
		 System.out.println(best.toString());
		 System.out.println("Genome : " + best.genomeString());
	 }
 
	 /**
	  * Constructor
	  */
	 private SimulationLoubnaManagementPa(String[] args) throws Exception {
		 // Check the parameters
		 // args[0] is the name of this script (useless)
		 // args[1] is the name of command file to be processed

		 
		 if (args == null || args.length != 2) {
			 throw new Exception("One parameter needed: missing command file name");
		 }

		 // Check the command file
		 String commandFileName = args[1];
		 if (!Check.isFile(commandFileName)) {
			 throw new Exception("Wrong command file name: " + commandFileName);
		 }

		 // Read and interpret the command file
		 SimulationLoubnaCommandReaderPa data = new SimulationLoubnaCommandReaderPa();
		 data.load(commandFileName);

		 // Set the working_dir
		 String working_dir = new File(commandFileName).getParentFile().getAbsolutePath();

		 // Set the output_dir
		 this.output_dir = working_dir + "/output-" + new File(commandFileName).getName();
		 new File(output_dir).mkdir();
		 //add manage_specie
		 this.simu_fitness = new SimulationFitnessPa( working_dir, this.output_dir,
															  data.setup_file, data.site_file, data.climate_file,
															  data.inventory_file, data.potential_species,data.manage_specie,
															  data.risk_aversion, data.basal_area_min, data.basal_area_max,
									data.gini_min, data.gini_max, data.m_rate_max,
									data.nb_spe_min, data.nb_spe_max, data.v_prod_min, data.v_prod_max,data.Combinaisons_species,data.RandomSeed,data.nb_process,data.Simulation_Non_viable_fin);
 
		 this.indv_first_pop = data.indv_first_pop;
		 this.best_to_parent = data.best_to_parent;
		 this.rand_indv = data.rand_indv;
		 this.stop_crit = data.stop_crit;
		 this.inter_max = data.inter_max;
		 this.max_cnt = data.max_cnt;
		 this.proba_mutation = data.proba_mutation;
		 this.RandomSeed=data.RandomSeed;
		 this.nb_process=data.nb_process;
		 this.Simulation_Non_viable_fin=data.Simulation_Non_viable_fin;

		
		 
	 }
 
 
 
 }