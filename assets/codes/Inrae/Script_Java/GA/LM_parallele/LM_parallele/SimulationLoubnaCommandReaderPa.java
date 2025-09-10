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

 import java.util.HashMap;
 import java.util.Map;
 
 import jeeb.lib.util.Record;
 import jeeb.lib.util.fileloader.FileLoader;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
 
 /**
  * Another simulation command file reader, for Michelle Cornet.
  *
  *  @author M. Malara - July 2023 & M. Jourdan - March 2024 & L.TALEB 2024
  */
 public class SimulationLoubnaCommandReaderPa extends FileLoader {
 
	 public String setup_file;
	 public String site_file;
	 public String climate_file;
	 public String inventory_file;
	 public String Combinaisons_species;
	 //les espèces 
	 public String potential_species;
	 //ajouter les espèces qu'on veut traiter 
	 public String manage_specie;
	 //GA
	 public boolean risk_aversion;
	 public int indv_first_pop;
	 public int best_to_parent;
	 public double proba_mutation;
	 public int rand_indv;
	 public int stop_crit;
	 public int inter_max;
	 public int max_cnt;
	//Constraints
	 public double basal_area_min;
	 public double basal_area_max;
	 public double gini_min;
	 public double gini_max;
	 public double m_rate_max;
	 public int nb_spe_min;
	 public int nb_spe_max;
	 public double v_prod_min;
	 public double v_prod_max;
	 //La seed
	 public int RandomSeed;
	 //Le nombre de division du processuer 
	 public int nb_process;
	 //Le switch pour la simulation 
	 public int Simulation_Non_viable_fin;
 
 
	 //ajouter la liste des especes avec leurs Id :
	 
	 /**
	  * Constructor.
	  */
	 public SimulationLoubnaCommandReaderPa() throws Exception {
		 super ();
		 //setSpeciesMapId();
	 }
	 //Remplir la liste des especes :
	 
	 protected void checks() throws Exception {
 
		int coreCount = Runtime.getRuntime().availableProcessors();
		 // checkAssertion throws an exception if trouble
		 checkAssertion(indv_first_pop >= 2, "indv_first_pop must be >= 2");
		 checkAssertion(best_to_parent >= 0, "best_to_parent must be >= 0");
		 checkAssertion(rand_indv  >= 0, "rand_indv must be >= 0");
		 checkAssertion(stop_crit  >= 1, "stop_crit must be >= 1");
		 checkAssertion(inter_max  >= 1, "inter_max must be >= 1");
		 checkAssertion(max_cnt  >= 1, "max_cnt must be >= 1");
		 checkAssertion(proba_mutation >= 0.0d, "proba_mutation must be => 0");
		 checkAssertion(proba_mutation <= 1.0d, "proba_mutation must be <= 1");
		 checkAssertion(basal_area_min >= 0.0d, "basal_area_min must be => 0");
		 checkAssertion(basal_area_min <= basal_area_max, "basal_area_min must be <= basal_area_max");
		 checkAssertion(gini_min >= 0.0d, "gini_min must be => 0");
		 checkAssertion(gini_max <= 1.0d, "gini_max must be <= 1");
		 checkAssertion(gini_min <= gini_max, "gini_min must be <= gini_max");
		 checkAssertion(m_rate_max >= 0.0d, "m_rate_max must be => 0");
		 checkAssertion(m_rate_max <= 1.0d, "m_rate_max must be <= 1");
		 checkAssertion(nb_spe_min >= 1, "nb_spe_min must be => 1");
		 checkAssertion(nb_spe_min <= nb_spe_max, "nb_spe_min must be <= nb_spe_max");
		 checkAssertion(v_prod_min >= 0.0d, "v_prod_min must be => 0");
		 checkAssertion(v_prod_min <= v_prod_max, "v_prod_min must be <= v_prod_max");
		 checkAssertion(nb_process > 0, "nb_process must be > 0");
		 checkAssertion(nb_process < coreCount, "nb_process must be < coreCount");
		 
		 
		 
 
	 }
 
 
 }