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

import java.util.List;

import jeeb.lib.util.Record;
import jeeb.lib.util.fileloader.FileLoader;

/**
 * Another simulation command file reader, for Brieuc Cornet.
 * 
 * @author Brieuc Cornet, X. Morin - January 2018
 */
public class SimulationBrieucCommandReader extends FileLoader{
	 
	public String setupFileName;
	public int numberOfYearsToBeJumped;
	public int exportTimeStep;
	
	public List<SimulationLine> simulationLines;
 
	/**
	 * Constructor.
	 */
	public SimulationBrieucCommandReader() throws Exception {
		super ();
	}
	
	protected void checks() throws Exception {
 
		// checkAssertion throws an exception if trouble
		
		checkAssertion(numberOfYearsToBeJumped >= 0, "numberOfYearsToBeJumped must be >= 0");
		checkAssertion(exportTimeStep >= 1, "exportTimeStep must be >= 1");
		checkAssertion(simulationLines != null && !simulationLines.isEmpty (), "At least one simulation line must be provided");
		
//			checkAssertion(plantAge > 0, "plantAge must be > 0");
//			checkAssertion(plantSeed >= 0, "plantSeed must be >= 0");
// 
//			if (simRecords.isEmpty ()) {
//				report ("The simRecords list should not be empty");
//				throw new Exception (report.toString ());
//			}
 
	}
	
	
	
	
	// A line in the command file
	static public class SimulationLine extends Record {
 
		public String siteFileName;
		public String climateFileName;
		public String inventoryFileName; 
		public String potentialSpeciesList;
		public String scenario;

		public SimulationLine(String line) throws Exception {
			super(line);
		}
	}
	 

}
