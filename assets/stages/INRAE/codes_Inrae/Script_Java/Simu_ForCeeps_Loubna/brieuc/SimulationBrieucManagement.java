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

package forceps.myscripts.brieuc;

import java.io.File;
import java.util.StringTokenizer;

import jeeb.lib.util.Check;
import capsis.app.C4Script;
import capsis.kernel.GModel;
import capsis.kernel.Step;
import forceps.extension.intervener.CepsGHAThinner2;
import forceps.extension.ioformat.CepsExport;
import forceps.extension.ioformat.CepsMeanStandExport;
import forceps.extension.ioformat.CepsProductivityScene;
import forceps.model.CepsEvolutionParameters;
import forceps.model.CepsInitialParameters;

/**
 * A simulation script for Forceps.
 * 
 * <pre>
 * // Linux / Mac
 * sh capsis.sh -p script fbrieuc.SimulationBrieucManagement data/forceps/brieuc/cmd_bauges_sap_epi.txt
 * // Windows
 * capsis -p script forceps.myscripts.brieuc.SimulationBrieucManagement data/forceps/brieuc/cmd_bauges_sap_epi.txt
 * </pre>
 * 
 * @author Brieuc Cornet, X. Morin - January 2018
 */
public class SimulationBrieucManagement {

	// All files are read in the same directory than the commanFile
	private String workingDir;

	// All exports go to the output dir
	private String outputDir;

	public static void main(String[] args) throws Exception {
		new SimulationBrieucManagement(args);
	}

	/**
	 * Constructor
	 */
	private SimulationBrieucManagement(String[] args) throws Exception {

		// Print the script parameters
		System.out.println("SimulationBrieucManagement, #args: " + args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.println("  " + args[i]);
		}
		System.out.println();

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
		System.out.println("commandFileName: " + commandFileName);

		// Read and interpret the command file
		SimulationBrieucCommandReader commandReader = new SimulationBrieucCommandReader();
		commandReader.load(commandFileName);

		// Set the workingDir
		workingDir = new File(commandFileName).getParentFile().getAbsolutePath();
		System.out.println("workingDir: " + workingDir);

		// outputDir
		outputDir = workingDir + "/output-" + new File(commandFileName).getName();
		new File(outputDir).mkdir();
		System.out.println("outputDir: " + outputDir);

		// Run a simulation per line in commandFileReader
		int k = 0;
		for (SimulationBrieucCommandReader.SimulationLine line : commandReader.simulationLines) {
			k++;
			runOneSimulation(k, commandReader.setupFileName, commandReader.numberOfYearsToBeJumped,
					commandReader.exportTimeStep, line);

		}

		System.out.println("Script ended correctly, performed " + k + " simulations, see results in \n" + outputDir);

	}

	/**
	 * Runs a simulation for one line in the command file. Throws an exception
	 * (stops the script) if an error arises.
	 */
	private void runOneSimulation(int k, String setupFileName, int numberOfYearsToBeJumped, int exportTimeStep,
			SimulationBrieucCommandReader.SimulationLine line) throws Exception {

		// int numberOfYears = line.numberOfYears;

		// fc-24.1.2018 fileName was too long, removed scenario, replaced by simulation id
		String filePrefix = line.climateFileName + "_" + line.inventoryFileName + "_"
				+ "simulation_"+k;
//		String filePrefix = line.climateFileName + "_" + line.inventoryFileName + "_"
//				+ line.scenario.replace(' ', '_').replace(';', '-');

		// Writing traces
		System.out.println("\nForceps: simulation #" + k + "...");
		System.out.println("filePrefix: " + filePrefix);

		// Model loading and initialisation
		System.out.println("Forceps Initialisation...");

		C4Script s = new C4Script("forceps");
		CepsInitialParameters i = new CepsInitialParameters(workingDir + "/" + setupFileName);

		i.loadSetupFile(); // fc-18.4.2014 before changing the parameters below

		// Overwrite parameters from the simulation line
		// addSetupDirectory () adds the directory containing the setup file
		// before the given file
		// name (all these files are in the setup file directory) fc-18.4.2014
		i.siteFileName = i.addSetupDirectory(line.siteFileName);
		i.defaultClimateFileName = i.addSetupDirectory(line.climateFileName);
		i.inventoryFileName = i.addSetupDirectory(line.inventoryFileName);
		i.setPotentialSpeciesList(line.potentialSpeciesList);

		s.init(i);

		Step step = s.getRoot();

		// Evolution
		StringTokenizer st = new StringTokenizer(line.scenario, ";");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();

			StringTokenizer rotation = new StringTokenizer(token, "_");

			try {
				// rotation format:
				// nbOfYear_minCutG_type_finalG_expectedMixComposition
				// e.g. 125_1_1_15_AAlb-40, FSyl-50
				int nbOfYear = Integer.parseInt(rotation.nextToken()); // years
				double minCutG = Double.parseDouble(rotation.nextToken()); // m2/ha
				double type = Double.parseDouble(rotation.nextToken()); // [0, 1]
				String finalG = rotation.nextToken(); // m2/ha OR %
				String expectedMixComposition = rotation.nextToken();
	
				System.out.println("Forceps Evolution: " + nbOfYear);
				step = s.evolve(new CepsEvolutionParameters(nbOfYear));
	
				CepsGHAThinner2 thinner = new CepsGHAThinner2(minCutG, type, finalG, expectedMixComposition);
	
				step = s.runIntervener(thinner, step);
				
			} catch (Exception e) {
				throw new Exception ("Wrong format in simulation #"+k+" in scenario for rotation: "+token);
			}
		}

		// Export section
		GModel model = s.getModel();

		// Mean stand export -> mean.txt
		CepsMeanStandExport e = new CepsMeanStandExport();
		String fileName = outputDir + "/" + filePrefix + "mean.txt";
		System.out.println("Writing mean stand export: " + fileName + "...");
		e.initExport(model, step);
		e.save(fileName);

		// Complete export -> complete.txt
		CepsExport e2 = new CepsExport();
		fileName = outputDir + "/" + filePrefix + "complete.txt";
		System.out.println("Writing complete export: " + fileName + "...");
		e2.initExport(model, step, numberOfYearsToBeJumped, exportTimeStep);
		e2.save(fileName);

		// Productivity export -> productivity.txt
		// CepsProductivity e3 = new CepsProductivity ();
		// fileName = outputDir + "/" + filePrefix + "productivity.txt";
		// System.out.println ("Writing productivity export: " + fileName +
		// "...");
		// e3.initExport (model, step, numberOfYearsToBeJumped,
		// exportTimeStep);
		// e3.save (fileName);

		// Species export -> species.txt
		// CepsSpeciesExport e4 = new CepsSpeciesExport ();
		// fileName = outputDir + "/" + filePrefix + "species.txt";
		// System.out.println ("Writing species export: " + fileName + "...");
		// e4.initExport (model, step, numberOfYearsToBeJumped,
		// exportTimeStep);
		// e4.save (fileName);

		// ProductivityScene export -> productivityScene.txt
		CepsProductivityScene e5 = new CepsProductivityScene();
		fileName = outputDir + "/" + filePrefix + "productivityScene.txt";
		System.out.println("Writing productivityScene export: " + fileName + "...");
		e5.initExport(model, step, numberOfYearsToBeJumped, exportTimeStep);
		e5.save(fileName);

		// fc-17.4.2014 was missing
		s.closeProject();
		System.out.println("end-of-simulation #" + k);

	}

}