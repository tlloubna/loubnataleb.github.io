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

package forceps.extension.intervener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import capsis.defaulttype.Tree;
import capsis.defaulttype.TreeList;
import capsis.kernel.GModel;
import capsis.kernel.GScene;
import capsis.kernel.Step;
import capsis.kernel.extensiontype.AbstractGroupableIntervener;
import capsis.kernel.extensiontype.Intervener;
import capsis.util.group.GroupableIntervener;
import capsis.util.group.GroupableType;
import forceps.model.CepsInitialParameters;
import forceps.model.CepsModel;
import forceps.model.CepsScene;
import forceps.model.CepsTree;
import jeeb.lib.util.Alert;
import jeeb.lib.util.Log;
import jeeb.lib.util.Translator;

/**
 * CepsGHAThinner3: a tool that cuts trees according to a thinning param_type
 * (from top, from below), a target G intensity.
 * 
 * @author X. Morin, Lisa Grell, F. de Coligny - March 2021,
 */
public class CepsGHAThinner3 extends AbstractGroupableIntervener implements Intervener, GroupableIntervener {

	// fc+xm-31.3.2021 Copied from CepsGHAThinner2

	static {
		Translator.addBundle("forceps.extension.intervener.CepsGHAThinner3");
	}

	// nb-30.08.2018
	// static final public String SUBTYPE = "SelectiveThinner"; // fc-4.12.2014
	// added final
	// (reviewing
	// static
	// variables
	// after a
	// Simmem bug)

	private boolean constructionCompleted = false; // if cancel in interactive
													// mode, false

	// Parameters

	protected double param_minCutG; // m2/ha
	protected double param_type; // [0, 1] from below, from above
	protected double param_finalG; // m2/ha G to be reached after thinning

	// Optional, if set, in [0, 100] if >= 0, param_finalG is calculated in init ()
	protected double param_finalGpercentage = -1;

//	// Expected species mix after intervention, in percentage for each species,
//	// sum percentages <= 100%
//	// e.g. "AAlb-60%, FSyl-30%" (and 10% of other species)
//	protected String param_expectedMixComposition;

	// Parameters

//	private Set<String> expectedMix; // e.g. AAlb, FSyl
//	private Map<String, Double> mixMap; // e.g. AAlb->60 Fsyl->30

	// sNames of the trees in the scene
//	private Set<String> presentSNames;

	protected CepsScene scene; // Reference stand: will be altered by apply ()
	private CepsInitialParameters ip;

	private List<CepsTree> concernedTrees;
	private double initG; // m2/ha

	/**
	 * Default constructor.
	 */
	public CepsGHAThinner3() {
	}

	/**
	 * Script constructor.
	 * 
	 * @param targetGHA  is a double and corresponds to the target in m2/ha (i.e.
	 *                   final, remaining after intervention) GHA - must be within
	 *                   possible limits
	 * @param param_type [0,1] : 0 for a from below thinning - 1 for a from above
	 *                   thinning
	 * @throws Exception
	 */
	public CepsGHAThinner3(double minCutG, double param_type,
			String finalGtext /* , String param_expectedMixComposition */) throws Exception {

		setMinCutG(minCutG);

		setType(param_type);

		// FinalG is tricky, can be absolute: 15.7 m2 or relative: 32%
		setFinalG(finalGtext);

//		setExpectedMixComposition(param_expectedMixComposition);
	}

	protected void setMinCutG(double minCutG) throws Exception {
		this.param_minCutG = minCutG;
		if (minCutG < 0)
			throw new Exception("param_minCutG must be positive or null");
	}

	protected void setType(double param_type) throws Exception {
		this.param_type = param_type;
		if (param_type < 0 || param_type > 1)
			throw new Exception("param_type must be in [0, 1]");

	}

	protected void setFinalG(String finalGtext) throws Exception {

		try {
			// if number, absolute value, take as is
			this.param_finalG = Double.parseDouble(finalGtext);
		} catch (Exception e) {
			try {
				// if not a number, we expect a percentage like: 30%
				String token = finalGtext.trim();
				if (!token.contains("%"))
					throw new Exception();
				token = token.replace("%", "").trim();
				param_finalGpercentage = Double.parseDouble(token);
				if (param_finalGpercentage < 0 || param_finalGpercentage > 100)
					throw new Exception();

			} catch (Exception e2) {
				throw new Exception(
						"could not evaluate param_finalG, should be a number (absolute value in m2/ha) or a percentage with a '%'");
			}

		}

	}

//	protected void setExpectedMixComposition(String param_expectedMixComposition) throws Exception {
//
//		if (param_expectedMixComposition == null || param_expectedMixComposition.length() == 0)
//			throw new Exception("missing expected mix composition");
//
//		this.param_expectedMixComposition = param_expectedMixComposition;
//		expectedMix = new HashSet<>();
//		mixMap = new HashMap<>();
//
//		double mixPercentageControl = 0;
//
//		StringTokenizer st = new StringTokenizer(param_expectedMixComposition, ", ");
//		while (st.hasMoreTokens()) {
//
//			try {
//				String token = st.nextToken().trim();
//
//				int separatorIndex = token.indexOf("-");
//				String sName = token.substring(0, separatorIndex).trim(); // e.g.
//																			// AAlb
//				String p = token.substring(separatorIndex + 1);
//				double percentage = Double.parseDouble(p);
//
//				if (percentage < 0 || percentage > 100)
//					throw new Exception("wrong mix composition value for: " + sName);
//
//				expectedMix.add(sName);
//				mixMap.put(sName, percentage);
//
//				mixPercentageControl += percentage;
//
//			} catch (Exception e) {
//				throw new Exception("could not evaluate an expected mix composition", e);
//			}
//
//		}
//
//		if (mixPercentageControl > 100)
//			throw new Exception("error in param_expectedMixComposition, sum of percentages must be lower than 100");
//
//	}

	@Override
	public void init(GModel m, Step s, GScene gscene, Collection c) {

		// This is referentStand.getInterventionBase ();
		scene = (CepsScene) gscene;
		ip = (CepsInitialParameters) m.getSettings();

		// The trees that can be cut
		if (c == null) {
			concernedTrees = scene.getTrees();
		} else {
			concernedTrees = new ArrayList<CepsTree>(c);
		}

		initG = getGha(concernedTrees);

//		presentSNames = new HashSet<>();
//		for (CepsTree t : concernedTrees) {
//			presentSNames.add(t.getSpecies().sname);
//		}

		constructionCompleted = true;

	}

	@Override
	public boolean initGUI() throws Exception {
		// Interactive start
		CepsGHAThinnerDialog3 dlg = new CepsGHAThinnerDialog3(this);

		constructionCompleted = false;
		if (dlg.isValidDialog()) {
			constructionCompleted = true;
		}
		dlg.dispose();

		return constructionCompleted;

	}

	/**
	 * Extension dynamic compatibility mechanism. This matchWith method checks if
	 * the extension can deal (i.e. is compatible) with the referent.
	 */
	static public boolean matchWith(Object referent) {
		try {
			return referent instanceof CepsModel;

		} catch (Exception e) {
			Log.println(Log.ERROR, "CepsGHAThinner3.matchWith ()", "Error in matchWith () (returned false)", e);
			return false;
		}

	}

	@Override
	public String getName() {
		return Translator.swap("CepsGHAThinner3.name");
	}

	@Override
	public String getAuthor() {
		return "X. Morin, Lisa Grell, F. de Coligny";
	}

	@Override
	public String getDescription() {
		return Translator.swap("CepsGHAThinner3.description");
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getSubType() {
		return Translator.swap("CepsGHAThinner3.subType");
	}

	/**
	 * GroupableIntervener interface. This intervener acts on trees, tree groups can
	 * be processed.
	 */
	public GroupableType getGrouperType() {
		return TreeList.GROUP_ALIVE_TREE;
	}

	/**
	 * Returns the cumulated basal area of the given trees (m2/ha)
	 */
	protected double getGha(List<CepsTree> trees) {

		double gha = 0;
		for (CepsTree t : trees) {
			gha += getGHA(t);
		}

		return gha;
	}

	private double getGHA(Tree t) {
		double g = Math.pow(t.getDbh() / 100d, 2d) * Math.PI / 4d;

		// fc-8.6.2020 considers group area is any
		double gHa = g * 10000d / getSceneArea(scene);
//		double gHa = g * 10000d / scene.getArea();

		return gHa;
	}

	/**
	 * Returns the trees with the given sName
	 */
//	protected List<CepsTree> getTrees(List<CepsTree> candidateTrees, Set<String> expectedMix, String sName) {
//
//		// StringBuffer b = new StringBuffer ("getTrees ("+sName+")... ");
//
//		List<CepsTree> trees = new ArrayList<>();
//
//		for (CepsTree t : candidateTrees) {
//
//			boolean treeIsSName = expectedMix.contains(sName) && t.getSpecies().sname.equals(sName);
//			boolean treeIsOtherSp = sName.equals("otherSp") && !expectedMix.contains(t.getSpecies().sname);
//
//			if (treeIsSName || treeIsOtherSp) {
//				// b.append(t.getSpecies().sname+", ");
//				trees.add(t);
//			}
//		}
//
//		// System.out.println(b.toString ());
//
//		return trees;
//	}

	/**
	 * These assertions are checked at the beginning of apply () in script AND
	 * interactive mode.
	 *
	 */
	private boolean assertionsAreOk() {

		if (scene == null) {
			Log.println(Log.ERROR, "CepsGHAThinner3.assertionsAreOk ()",
					"scene is null. CepsGHAThinner3 is not appliable.");
			return false;
		}

		return true;
	}

	/**
	 * Intervener interface. Controls input parameters.
	 */
	public boolean isReadyToApply() {
		// Cancel on dialog in interactive mode -> constructionCompleted = false
		if (constructionCompleted && assertionsAreOk()) {
			return true;
		}
		return false;
	}

	/**
	 * Intervener interface. Makes the action: cuts trees.
	 */
	public Object apply() throws Exception {

		// If param_finalG was expressed as a percentage at construction time,
		// calculate it now
		if (param_finalGpercentage >= 0)
			this.param_finalG = initG * param_finalGpercentage / 100d;

		if (param_finalG < 0)
			throw new Exception("error, param_finalG: " + param_finalG + " must be positive");

//		// Check mixMap keys and percentages
//		double sum = 0;
//		for (String sName : mixMap.keySet()) {
//
//			// fc+xm+bc-allow a command for a species not on the scene
//			// (the cut command will be dispatched on the other species)
//			// if (!presentSNames.contains(sName))
//			// throw new
//			// Exception("found a sName in the expected mix composition which is not present in the scene: "
//			// + sName);
//
//			double v = mixMap.get(sName);
//			sum += v;
//		}
//
//		if (sum > 100)
//			throw new Exception("error in expected mix map, sum of percentages must be lower than 100: " + sum);

		// Check if apply is possible
		if (!isReadyToApply()) {
			throw new Exception("CepsGHAThinner3.apply () - Wrong input parameters, see Log");
		}

		// Start of intervention

		scene.setInterventionResult(true);

		if (param_finalG > initG) {
			Alert.print("CepsGHAThinner3.apply (): param_finalG: " + param_finalG + " is lower than initG: " + initG
					+ ", cut nothing");
			return scene;
		}

		// Stage 1. Determining accurately what must be cut (how much G must be
		// left for each species in the command + other species)
		// i.e. check if the command can be processed or change it accordingly
		// to reality to make it possible
		// Reality means how much G is actually present for each species in the
		// scene

//		System.out.println("CepsGHAThinner3 entering stage1...");
//
//		// MIX (uppercase) contains all entries in param_expectedMixComposition
//		// + "otherSp"
//		// Values are G values expected to be found at the end
//		Map<String, Double> expectedMIXpercentages = new HashMap<>();
//		double otherSpPercentage = 100d;
//		for (String sName : mixMap.keySet()) {
//			double percentage = mixMap.get(sName);
//			otherSpPercentage -= percentage;
//
//			expectedMIXpercentages.put(sName, percentage);
//
//		}
//		expectedMIXpercentages.put("otherSp", otherSpPercentage);
//
//		Map<String, Double> expectedMIXvalues = new HashMap<>();
//		resetExpectedMIXvalues(param_finalG, expectedMIXpercentages, expectedMIXvalues);
//
//		System.out.println("initial expectedMIXpercentages: " + traceMap(expectedMIXpercentages));
//		System.out.println("initial expectedMIXvalues: " + traceMap(expectedMIXvalues));
//		System.out.println("initial param_finalG: " + param_finalG);
//
//		double tmp_finalG = param_finalG;
//		int security = 0;
//		while (!cutIsPossible(tmp_finalG, expectedMIXvalues)) {
//
//			tmp_finalG = changeExpectedMIXvalues(tmp_finalG, expectedMIXpercentages, expectedMIXvalues);
//
//			System.out.println("changed expectedMIXpercentages: " + traceMap(expectedMIXpercentages));
//			System.out.println("changed expectedMIXvalues: " + traceMap(expectedMIXvalues));
//			System.out.println("changed tmp_finalG: " + tmp_finalG);
//
//			if (++security > 50)
//				throw new Exception("CepsGHAThinner3 error, could not adjust the expected mix composition in "
//						+ security + " iterations, aborted");
//		}

		// Stage 2. cut the trees accordingly

//		System.out.println("CepsGHAThinner3 entering stage2, expectedMIXvalues: " + traceMap(expectedMIXvalues));

		double remainingTotalG = initG;

		remainingTotalG = processCut(param_finalG, concernedTrees, remainingTotalG);

//		for (String sName : expectedMIXvalues.keySet()) {
//
//			// Extract the trees of the given sName
//			List<CepsTree> sNameTrees = getTrees(concernedTrees, expectedMix, sName);
//
//			double expectedG = expectedMIXvalues.get(sName);
//			remainingTotalG = processCut(expectedG, sNameTrees, remainingTotalG);
//
//			if (remainingTotalG <= param_finalG)
//				break;
//
//		}

//		System.out.println("CepsGHAThinner3: before cut: " + traceSceneComposition(concernedTrees));
//		System.out.println("CepsGHAThinner3: after cut: " + traceSceneComposition(scene.getTrees()));
		System.out.println("CepsGHAThinner3: param_finalG: " + param_finalG + " remainingTotalG: " + remainingTotalG);

		return scene;

	}

//	private String traceSceneComposition(List<CepsTree> trees) {
//		StringBuffer b = new StringBuffer("Scene composition:");
//
//		AdditionMap map = new AdditionMap();
//		double totalGHA = 0d;
//		for (CepsTree t : trees) {
//			String sName = t.getSpecies().sname;
//			double gha = getGHA(t);
//			map.addValue(sName, gha);
//			totalGHA += gha;
//		}
//
//		for (String sName : map.getKeys()) {
//			double gha = map.getValue(sName);
//			double percentage = gha / totalGHA * 100d;
//
//			b.append(" " + sName + ": " + gha + " (" + percentage + "%)");
//		}
//
//		return b.toString();
//	}

//	private String traceMap(Map<String, Double> map) {
//		String s = AmapTools.toString(map).replace('\n', ' ');
//		s = s.replace("  ", " ");
//		return s;
//	}

	/**
	 * Detect which entries can not be processed in expectedMIXvalues (because
	 * sp_realG is lower than sp_expectedG). Remove these entries: all sp_realG will
	 * be kept. Change the other entries (decrease their sp_expectedG) to cut them
	 * more, by removing the missing sp_realG from their sp_expectedG.
	 */
//	private double changeExpectedMIXvalues(double tmp_finalG, Map<String, Double> expectedMIXpercentages,
//			Map<String, Double> expectedMIXvalues) {
//
//		// Contains sNames and 'otherSp'
//		Set<String> problematicSpecies = new HashSet<>();
//		double sp_correctPercentageSum = 0d;
//
//		for (String sName : expectedMIXvalues.keySet()) {
//
//			double sp_expectedG = expectedMIXvalues.get(sName);
//
//			// Extract the trees of the given sName
//			List<CepsTree> sNameTrees = getTrees(concernedTrees, expectedMix, sName);
//			double sp_realG = getGha(sNameTrees);
//
//			if (sp_expectedG > sp_realG) { // problematic
//
//				tmp_finalG -= sp_realG;
//				problematicSpecies.add(sName);
//
//			} else {
//				sp_correctPercentageSum += expectedMIXpercentages.get(sName);
//			}
//
//		}
//
//		for (String sName : new ArrayList<>(expectedMIXpercentages.keySet())) {
//
//			double sp_expectedPercentage = expectedMIXpercentages.get(sName);
//
//			if (problematicSpecies.contains(sName)) {
//				expectedMIXpercentages.remove(sName);
//
//			} else {
//
//				// change percentage for this remaining species
//				double newPercentage = (sp_expectedPercentage / sp_correctPercentageSum) * 100d;
//				expectedMIXpercentages.put(sName, newPercentage);
//
//			}
//
//		}
//
//		resetExpectedMIXvalues(tmp_finalG, expectedMIXpercentages, expectedMIXvalues);
//
//		return tmp_finalG;
//	}

	/**
	 * Returns true if all the species have enough G to allow the cut planned in
	 * expectedMIXvalues (contains remaining G expected after the cut).
	 */
//	private boolean cutIsPossible(double tmp_finalG, Map<String, Double> expectedMIXvalues) {
//
//		for (String sName : expectedMIXvalues.keySet()) {
//
//			double sp_expectedG = expectedMIXvalues.get(sName);
//
//			// Extract the trees of the given sName
//			List<CepsTree> sNameTrees = getTrees(concernedTrees, expectedMix, sName);
//			double sp_realG = getGha(sNameTrees);
//
//			if (sp_expectedG > sp_realG) {
//				System.out.println("cutIsPossible false for sName: " + sName + ", sp_expectedG: " + sp_expectedG
//						+ ", sp_realG: " + sp_realG);
//				return false;
//			}
//		}
//		return true;
//	}

	/**
	 * Clears and restores expectedMIXvalues according to expectedMIXpercentages
	 * (there may be less keys in the result).
	 */
//	private void resetExpectedMIXvalues(double G, Map<String, Double> expectedMIXpercentages,
//			Map<String, Double> expectedMIXvalues) {
//
//		expectedMIXvalues.clear();
//
//		for (String sName : expectedMIXpercentages.keySet()) {
//			double percentage = expectedMIXpercentages.get(sName);
//			double value = G * percentage / 100d;
//
//			expectedMIXvalues.put(sName, value);
//		}
//
//	}

	/**
	 * Process the cut for the trees of a given species, cut to reach the given
	 * final basal area sp_finalG in the resulting scene. sp_finalG was calculated
	 * before in order to be reachable. Special case: this method can be called for
	 * the list of "other species trees". Returns the remainingTotalG after this cut
	 * processCut.
	 */
	private double processCut(double finalG, List<CepsTree> candidateTrees, double remainingTotalG) {

		double remainingG = getGha(candidateTrees);

		// Score the trees
		double type_;
		double randomness = 0;
		if (param_type >= 0 && param_type <= 0.5) {
			randomness = (2d * param_type);
			type_ = 0d; // from below
		} else if (param_type <= 1) {
			randomness = 2d - 2d * param_type;
			type_ = 1d; // from above
		}

		double ci;
		Random random = new Random();
		double uniformProba;
		double probaOfBeingCut;
		double score;
		double rangeMax;
		double circOfMaxProba;

		// Compute cmin and cmax needed to interpret the param_type (cm)
		double cMin = 1000;
		double cMax = 0;
		for (Iterator i = concernedTrees.iterator(); i.hasNext();) {
			Tree t = (Tree) i.next();
			ci = t.getDbh() * Math.PI;
			if (ci < cMin)
				cMin = ci;
			if (ci > cMax)
				cMax = ci;
		}

		circOfMaxProba = param_type * (cMax - cMin) + cMin;
		rangeMax = circOfMaxProba - cMin + 1;
		if ((-circOfMaxProba + cMax + 1) > rangeMax) {
			rangeMax = (-circOfMaxProba + cMax + 1);
		}

		List<ScoredTree> arrayTree = new ArrayList<ScoredTree>();
		for (Iterator i = candidateTrees.iterator(); i.hasNext();) {
			Tree t = (Tree) i.next();

			ci = t.getDbh() * Math.PI;
			uniformProba = random.nextDouble();
			probaOfBeingCut = 1 - Math.abs(ci - circOfMaxProba) / rangeMax;
			score = randomness * uniformProba + (1 - randomness) * probaOfBeingCut;

			ScoredTree scoredTree = new ScoredTree(t, score);
			arrayTree.add(scoredTree);
		}
		Collections.sort(arrayTree);

		// Cut the trees
		int i = 0;
		// double cutGsum = 0;
		while (remainingG > finalG && i < arrayTree.size()) {

			Tree t = arrayTree.get(i).t;
			double gHa = getGHA(t);

			// cut this tree
			scene.removeTree(t);

			// cutGsum += gHa;
			remainingG -= gHa;
			remainingTotalG -= gHa;

			if (remainingTotalG <= param_finalG)
				break;

			i++;
		}

		return remainingTotalG;
	}

	/**
	 * toString () method.
	 */
	public String toString() {
		// nb-13.08.2018
		// return "class=" + getClass().getName() + " name=" + NAME + "
		// constructionCompleted=" + constructionCompleted
		return "class=" + getClass().getName() + " name=" + getName() + " constructionCompleted="
				+ constructionCompleted + " stand=" + scene + " param_minCutG =" + param_minCutG + " param_type ="
				+ param_type + " param_finalG ="
				+ param_finalG /* + " param_expectedMixComposition: " + param_expectedMixComposition */;
	}

	// fc-14.12.2020 Unused, deprecated
//	public void activate() {
//	}

	/**
	 * This class handles the trees with a score of being cut
	 */
	protected class ScoredTree implements java.lang.Comparable {

		public Tree t;
		private double score;

		ScoredTree(Tree t, double score) {
			this.t = t;
			this.score = score;
		}

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}

		@Override
		public int compareTo(Object scoredtree) {
			double score1 = ((ScoredTree) scoredtree).getScore();
			double score2 = this.getScore();
			if (score1 < score2)
				return -1;
			else if (score1 == score2)
				return 0;
			else
				return 1;
		}
	}

}
