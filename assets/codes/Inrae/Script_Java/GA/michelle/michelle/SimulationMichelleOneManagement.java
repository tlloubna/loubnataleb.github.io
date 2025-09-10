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

package forceps.myscripts.michelle;

import java.io.File;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

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
 * sh capsis.sh -p script forceps.myscripts.Michelle.SimulationMichelleOneManagement data/forceps/Michelle/cmd_bauges_sap_epi.txt
 * // Windows
 * capsis -p script forceps.myscripts.Michelle.SimulationMichelleOneManagement data/forceps/Michelle/cmd_bauges_sap_epi.txt
 * </pre>
 *
 * @author M. Malara - July 2023 & M. Jourdan - March 2024 & change  by loubna 2024
 */
public class SimulationMichelleOneManagement {
	//la classe pour executer la simulation en utilisant Ga avec Forceps 
	private String output_dir; // All exports go to the output dir
	private int inter_max; //nombre d'itération maximale 
	private List<Double> genome; //initialiser le génome sous forme d'un vecteur 
	private SimulationFitness simu_fitness;//l'objet qui calcule la fitness 

	public static void main(String[] args) throws Exception {
		// init simu : début de la simulation 
		SimulationMichelleOneManagement simu = new SimulationMichelleOneManagement(args);

		// init indv: définir l'iténéraire sylvicole.
		ManagementIndv indv = new ManagementIndv(simu.genome, simu.simu_fitness, false);
	}

	/**
	 * Constructor
	 */
	private SimulationMichelleOneManagement(String[] args) throws Exception {
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
		// Read and interpret the command file et charger la commande par the data 
		SimulationMichelleSimulationReader data = new SimulationMichelleSimulationReader();
		data.load(commandFileName);
		// Set the working_dir trouver le chemin du travail 
		String working_dir = new File(commandFileName).getParentFile().getAbsolutePath();
		// Set the output_dir et creer le dossier de travail out_put*****
		this.output_dir = working_dir + "/output-" + new File(commandFileName).getName();
		new File(output_dir).mkdir();
		//convertir la chaine de caractére en double car data.genome est un string 
		this.genome = this.strToListOnDouble(data.genome);
		//add trait_potetiel_species 
		this.simu_fitness = new SimulationFitness( working_dir, this.output_dir,
															 data.setup_file, data.site_file, data.climate_file,
															 data.inventory_file, data.potential_species,data.trait_potentiel_species,
															 data.risk_aversion, data.basal_area_min, data.basal_area_max,
		                           data.gini_min, data.gini_max, data.m_rate_max,
		                           data.nb_spe_min, data.nb_spe_max, data.v_prod_min, data.v_prod_max);
	}


	private List<Double> strToListOnDouble(String genomeStr){
			List<Double> genome = new ArrayList<Double>();
			StringTokenizer st = new StringTokenizer(genomeStr, ", ");
			for (; st.hasMoreTokens();) {
				String token = st.nextToken();
				Double gene = Double.parseDouble(token);
				genome.add(gene);
			}
			return genome;
	}



}
