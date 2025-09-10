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

 package forceps.myscripts.LM_sequentielle;

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
 public class SimulationLoubnaManagementSeq {

	//lire les données du fichier du commande
	 private String output_dir; // All exports go to the output dir
	 private SimulationFitnessSeq simu_fitness;
	 private int indv_first_pop;
	 private int best_to_parent;
	 private int rand_indv;
	 private int stop_crit;
	 private int inter_max;
	 private int max_cnt;
	 private double proba_mutation;
	 private int RandomSeed;
	
	 //Le switch pour la simulation 
	 private int Simulation_Non_viable_fin;
	
	 //le main pour lancer la simulation avec GA .
	 public static void main(String[] args) throws Exception {

		 //pour calculer le temps du simulation de lecture des données à partir du cmd2.txt
		 long startTime,endTime;
		 

		 SimulationLoubnaManagementSeq simu = new SimulationLoubnaManagementSeq(args);

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

		 
		 GeneticAlgoSeq algo = new GeneticAlgoSeq(simu.indv_first_pop, simu.best_to_parent,
																			  simu.proba_mutation, simu.rand_indv,
																			  simu.stop_crit, simu.inter_max,
																			  simu.output_dir, simu.simu_fitness);

		

		 //run GA
		 startTime =System.nanoTime();
		 ManagementPopSeq res = algo.runAlgo(simu.max_cnt);
		 endTime = System.nanoTime();
		 //System.out.println("Time to run GA : " + (endTime - startTime) + " ns");
 
		 // Afficher le meilleur individus dans GA.
		 ManagementIndvSeq best = res.getBestIndv();
		 System.out.println(best.toString());
		 System.out.println("Genome : " + best.genomeString());
	 }
 
	 /**
	  * Constructor
	  */
	 private SimulationLoubnaManagementSeq(String[] args) throws Exception {
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
		 SimulationLoubnaCommandReaderSeq data = new SimulationLoubnaCommandReaderSeq();
		 data.load(commandFileName);

		 // Set the working_dir
		 String working_dir = new File(commandFileName).getParentFile().getAbsolutePath();

		 // Set the output_dir
		 this.output_dir = working_dir + "/output-" + new File(commandFileName).getName();
		 new File(output_dir).mkdir();
		 //add manage_specie
		 this.simu_fitness = new SimulationFitnessSeq( working_dir, this.output_dir,
															  data.setup_file, data.site_file, data.climate_file,
															  data.inventory_file, data.potential_species,data.manage_specie,
															  data.risk_aversion, data.basal_area_min, data.basal_area_max,
									data.gini_min, data.gini_max, data.m_rate_max,
									data.nb_spe_min, data.nb_spe_max, data.v_prod_min, data.v_prod_max,data.Combinaisons_species,data.RandomSeed,data.Simulation_Non_viable_fin);
 
		 this.indv_first_pop = data.indv_first_pop;
		 this.best_to_parent = data.best_to_parent;
		 this.rand_indv = data.rand_indv;
		 this.stop_crit = data.stop_crit;
		 this.inter_max = data.inter_max;
		 this.max_cnt = data.max_cnt;
		 this.proba_mutation = data.proba_mutation;
		 this.RandomSeed=data.RandomSeed;
		 
		 this.Simulation_Non_viable_fin=data.Simulation_Non_viable_fin;

		
		 
	 }
 
 
 
 }