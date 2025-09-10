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
 
 /**
  * Another simulation command file reader, for Michelle Cornet.
  *
  *  @author M. Malara - July 2023 & M. Jourdan - March 2024
  */
 public class SimulationLoubnaSimulationReaderPa extends FileLoader {
 
	 public String setup_file;
	 public String site_file;
	 public String climate_file;
	 public String inventory_file;

	 public String Combinaisons_species;
	 public String potential_species;
	 //ajouter les especes traiter dans le genome
	 public String manage_specie;
	 
	 public boolean risk_aversion;
	 public double basal_area_min;
	 public double basal_area_max;
	 public double gini_min;
	 public double gini_max;
	 public double m_rate_max;
	 public int nb_spe_min;
	 public int nb_spe_max;
	 public double v_prod_min;
	 public double v_prod_max;
	 public String genome;

	 //lire la seed
	 public int RandomSeed;
	 //Le nombre de division du processuer 
	 public int nb_process;
	 //Le switch pour la simulation 
	 public int Simulation_Non_viable_fin;
	 
	 //cette classe pour gérer les fichiers de configuration et les paramétres de simulation 
	 public Map<Integer,String> speciesMapId;
	 /**
	  * Constructor.
	  */
	 public SimulationLoubnaSimulationReaderPa() throws Exception {
		 super (); // définir le constructeur qui hérite de FileFolder 
		 setSpeciesMapId();
	 }
	 //pour s'assurer que les espèces ecrit correspond bien au fichier de sortie.
	 //l'entier correspond à l'identifiant de l'espèce
	 //Strinf correspond au shortname de l'espece i.
	 protected void setSpeciesMapId() {
		 speciesMapId = new HashMap<Integer, String>();
		 speciesMapId.put(0, "AAlb");
		 speciesMapId.put(1, "LDec");
		 speciesMapId.put(2, "PAbi");
		 speciesMapId.put(3, "PCem");
		 speciesMapId.put(4, "PMon");
		 speciesMapId.put(5, "PSyl");
		 speciesMapId.put(6, "TBac");
		 speciesMapId.put(7, "ACam");
		 speciesMapId.put(8, "APla");
		 speciesMapId.put(9, "APse");
		 speciesMapId.put(10, "AGlu");
		 speciesMapId.put(11, "AInc");
		 speciesMapId.put(12, "AVir");
		 speciesMapId.put(13, "BPen");
		 speciesMapId.put(14, "CBet");
		 speciesMapId.put(15, "CSat");
		 speciesMapId.put(16, "Cave");
		 speciesMapId.put(17, "FSyl");
		 speciesMapId.put(18, "FExc");
		 speciesMapId.put(19, "PNig");
		 speciesMapId.put(20, "PTre");
		 speciesMapId.put(21, "QPet");
		 speciesMapId.put(22, "QPub");
		 speciesMapId.put(23, "QRob");
		 speciesMapId.put(24, "SAlb");
		 speciesMapId.put(25, "SAri");
		 speciesMapId.put(26, "SAuc");
		 speciesMapId.put(27, "TCor");
		 speciesMapId.put(28, "TPla");
		 speciesMapId.put(29, "UGla");
		 speciesMapId.put(30, "Pmar");
		 speciesMapId.put(31, "Pgra");
		 speciesMapId.put(32, "Phal");
		 speciesMapId.put(33, "Ptsu");
		 speciesMapId.put(34, "Qile");
		 speciesMapId.put(35, "Adec");
		 speciesMapId.put(36, "Aeve");
		 speciesMapId.put(37, "Geve");
	 }
	 //Donc il faut verfier que manage_specie
	 //Verifier bien que les espèces sont ecrit correctement
	 public boolean isInpotentielSpecies(){
		 String[] splitShortNames = manage_specie.split(" ");
		 for (String sp : splitShortNames){
			 if (!speciesMapId.containsValue(sp)){
				 return false;
			 }
		 }
		 return true;
	 }
	 
 
	 protected void checks() throws Exception {
		int coreCount = Runtime.getRuntime().availableProcessors();
		 // checkAssertion throws an exception if trouble
		 checkAssertion(basal_area_min >= 0.0d, "basal_area_min must be => 0");//la surface térrière n'est pas négative 
		 checkAssertion(basal_area_min <= basal_area_max, "basal_area_min must be <= basal_area_max"); //Gmin <=Gmax 
		 checkAssertion(gini_min >= 0.0d, "gini_min must be => 0");//Gini dans [0.25,0.75]
		 checkAssertion(gini_max <= 1.0d, "gini_max must be <= 1");
		 checkAssertion(gini_min <= gini_max, "gini_min must be <= gini_max");
		 checkAssertion(m_rate_max >= 0.0d, "m_rate_max must be => 0");//taux de moratlité n'est pas négative 
		 checkAssertion(m_rate_max <= 1.0d, "m_rate_max must be <= 1"); //c'est pas tous les arbres sont nulles 
		 checkAssertion(nb_spe_min >= 1, "nb_spe_min must be => 1");// il faut au min avoir une espèce dans la foret pas une surface nu 
		 checkAssertion(nb_spe_min <= nb_spe_max, "nb_spe_min must be <= nb_spe_max");//toujours nb_spe_min> nb_spe_max 
		 checkAssertion(v_prod_min >= 0.0d, "v_prod_min must be => 0");//la biomasse ne doit pas étre nulle ce qu'on extrait à la fin de la simulation 
		 checkAssertion(v_prod_min <= v_prod_max, "v_prod_min must be <= v_prod_max");//la min toujours inférieur à la max ( min est 30 m3/ha )
		 checkAssertion(nb_process > 0, "nb_process must be => 0");
		 checkAssertion(nb_process < coreCount, "nb_process must be <  coreCount");
		 checkAssertion(isInpotentielSpecies()==true,"verifier que les especes de manage_specie sont ecrites correctement  ");
		 
	 }
 
 
 }