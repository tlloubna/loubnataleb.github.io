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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import capsis.defaulttype.Tree;
import capsis.defaulttype.TreeCollection;
import capsis.defaulttype.TreeList;
import capsis.kernel.GModel;
import capsis.kernel.GScene;
import capsis.kernel.MethodProvider;
import capsis.kernel.Step;
import capsis.kernel.extensiontype.AbstractGroupableIntervener;
import capsis.kernel.extensiontype.Intervener;
import capsis.util.group.GroupableIntervener;
import capsis.util.group.GroupableType;
import capsis.util.methodprovider.GProvider;
import forceps.model.CepsInitialParameters;
import forceps.model.CepsModel;
import forceps.model.CepsScene;
import forceps.model.CepsTree;
import jeeb.lib.util.Log;
import jeeb.lib.util.Translator;

/**
 * CepsGHAThinner: a generic tool that cuts trees according to the thinning type, intensity (basal area or proportion) of trees to thin
 * (and a randomness coefficient, deduced from the type).
 * @author Ph. Dreyfus - December 2017
 *   it is a mainly a copy of GymnoGHAThinner2 with adaptation according to HistoThinner (CepsScene is not an instance of TreeList)
 */
public class CepsGHAThinner extends AbstractGroupableIntervener implements Intervener, GroupableIntervener {
//public class CepsGHAThinner implements Intervener, GroupableIntervener {
	
	// fc-8.6.2020 Added AbstractGroupableIntervener for getSceneArea (scene)

	static {
		Translator.addBundle ("forceps.extension.intervener.CepsGHAThinner");
	}

	// nb-13.08.2018
	//public static final String NAME = "CepsGHAThinner";
	//public static final String VERSION = "1.0";
	//public static final String AUTHOR = "Ph. Dreyfus";
	//public static final String DESCRIPTION = "CepsGHAThinner.description";

	// nb-30.08.2018
	//static final public String SUBTYPE = "SelectiveThinner"; // fc-4.12.2014 added final (reviewing static variables after a Simmem bug)	

	private boolean constructionCompleted = false; // if cancel in interactive mode, false
	private int mode; // CUT or MARK
	
	protected CepsScene stand1; // Reference stand: will be altered by apply ()
	private TreeCollection stand1TC; // Reference stand: will be altered by apply
	
	private Collection<? extends Tree> concernedTrees;
	
	private Collection<Integer> treeIds;
	private double type;
	private double cMin;
	private double cMax;
	private double targetGHA;
	private double initGHA;
	
	private String speciesShortName;
	private CepsInitialParameters ip;
	
	transient private List<ScoredTree> arraytree = new ArrayList<ScoredTree>();
	private MethodProvider methodProvider;
	
	private ArrayList vctOtherSpeciesTrees;

	/**
	 * Default constructor.
	 */
	public CepsGHAThinner () {	}


	/** Script constructor.
	 * The first parameter is a double and corresponds to the proportion of trees to cut [0,1].
	 * @param targetGHA is a double and corresponds to the target GHA - must be within possible limit
	 * @param type [0,1] : 0 for a from below thinning - 1 for a from above thinning
	 * @throws Exception
	 */
	public CepsGHAThinner (double targetGHA, double type) {
		this.targetGHA = targetGHA;
		this.type = type;
		this.speciesShortName = "";
	}

	public CepsGHAThinner (double targetGHA, double type, CepsInitialParameters ip, String speciesShortName) {
		this.targetGHA = targetGHA;
		this.type = type;
		this.speciesShortName = speciesShortName;
		this.ip = ip;
	}


	@Override
	public void init (GModel m, Step s, GScene scene, Collection c) {

		// This is referentStand.getInterventionBase ();
		stand1 = (CepsScene) scene;
		stand1TC = (TreeCollection) scene;

		// fc+xm+bc-19.1.2018 ip was sometimes missing
		ip = (CepsInitialParameters) m.getSettings();
		
		// Keep only trees with the selected species    // PhD 2017-12-26
		Collection cloneTrees = stand1TC.getTrees ();
		ArrayList vct = new ArrayList (cloneTrees.size ());
		vct.addAll(cloneTrees);
		for (int i = 0; i < vct.size(); i++) {
			CepsTree t = (CepsTree) vct.get(i);
			int speciesCode = t.getSpecies ().getValue ();
			//System.out.println("CepGHAThinner,  speciesShortName : " + speciesShortName);
			 if (!ip.speciesManager.getOriginalSpecies(speciesCode).sname.equals(speciesShortName)) {
				 CepsTree t2 = (CepsTree)  ((TreeCollection) stand1TC).getTree (t.getId());
				stand1TC.removeTree(t2);
			 }
		}
		
		// vctOtherSpeciesTrees will contain only trees with OTHER species    // PhD 2017-12-26
		vctOtherSpeciesTrees = new ArrayList ();		// temporary
		for (int i = 0; i < vct.size(); i++) {
			CepsTree t = (CepsTree) vct.get(i);
			int speciesCode = t.getSpecies ().getValue ();
			//System.out.println("CepGHAThinner,  speciesShortName : " + speciesShortName);
			 if (!ip.speciesManager.getOriginalSpecies(speciesCode).sname.equals(speciesShortName)) {
				vctOtherSpeciesTrees.add(t);
			 }
		}
					
		// The trees that can be cut
		if (c == null) {
			concernedTrees = stand1TC.getTrees();
		} else {
			concernedTrees = c;
		}

		
		// Save ids for future use
		treeIds = new HashSet<Integer> ();
		for (Object o : concernedTrees) {
			Tree t = (Tree) o;
			treeIds.add (t.getId ());
		}
		// Define cutting mode: ask model
		mode = (m.isMarkModel ()) ? MARK : CUT;

		// Retrieve method provider
		methodProvider = m.getMethodProvider ();

		// per Ha computation
		
		double coefHa =  10000 / getSceneArea (scene); // fc-8.6.2020
//		double coefHa =  10000 / scene.getArea ();

		initGHA =  ((GProvider) methodProvider).getG (stand1, stand1.getTrees()) * coefHa;

		constructionCompleted = true;

	}

	@Override
	public boolean initGUI () throws Exception {
		// Interactive start
		CepsGHAThinnerDialog dlg = new CepsGHAThinnerDialog (this);

		constructionCompleted = false;
		if (dlg.isValidDialog ()) {
			// valid -> ok was hit and all checks were ok
			try {
				targetGHA = dlg.getTargetBA ();
				type = dlg.getTypeValue ();
				constructionCompleted = true;
			} catch (Exception e) {
				constructionCompleted = false;
				throw new Exception ("CepsGHAThinner (): Could not get parameters in CepsGHAThinner", e);
			}
		}
		dlg.dispose ();

		return constructionCompleted;

	}

	/**
	 * Extension dynamic compatibility mechanism. This matchWith method checks if the extension can
	 * deal (i.e. is compatible) with the referent.
	 */
	static public boolean matchWith (Object referent) {
		try {
			return referent instanceof CepsModel;
			
		} catch (Exception e) {
			Log.println(Log.ERROR, "CepsGHAThinner.matchWith ()",
					"Error in matchWith () (returned false)", e);
			return false;
		}

	}

	@Override
	public String getName() {
		return Translator.swap("CepsGHAThinner.name");
	}

	@Override
	public String getAuthor() {
		return "Ph. Dreyfus";
	}

	@Override
	public String getDescription() {
		return Translator.swap("CepsGHAThinner.description");
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getSubType() {
		return Translator.swap("CepsGHAThinner.subType");
	}

	/**
	 * GroupableIntervener interface. This intervener acts on trees, tree groups can be processed.
	 */
	public GroupableType getGrouperType () {
		return TreeList.GROUP_ALIVE_TREE;
	}

	/**
	 * These assertions are checked at the beginning of apply () in
	 * script AND interactive mode.
	 *
	 */
	private boolean assertionsAreOk () {
		if (mode != CUT && mode != MARK) {
			Log.println (Log.ERROR, "CepsGHAThinner.assertionsAreOk ()", "Wrong mode=" + mode + ", should be " + CUT + " (CUT) or " + MARK
					+ " (MARK). CepsGHAThinner is not appliable.");
			return false;
		}
		if (stand1 == null) {
			Log.println (Log.ERROR, "CepsGHAThinner.assertionsAreOk ()", "stand is null. CepsGHAThinner is not appliable.");
			return false;
		}

		if (type < 0 || type > 1){
			Log.println (Log.ERROR, "CepsGHAThinner.assertionsAreOk ()","CepsGHAThinner() - The thinning type must be a double between 0 and 1");
			return false;
		}

		if (targetGHA < 0){
			Log.println (Log.ERROR, "CepsGHAThinner.assertionsAreOk ()","CepsGHAThinner() - The target GHA must be higher than 0");
			return false;
		}

		return true;
	}

	/**
	 * Intervener interface. Controls input parameters.
	 */
	public boolean isReadyToApply () {
		// Cancel on dialog in interactive mode -> constructionCompleted = false
		if (constructionCompleted && assertionsAreOk ()) { return true; }
		return false;
	}

	/**
	 * Intervener interface. Makes the action: cuts trees.
	 */
	public Object apply () throws Exception {
		// Check if apply is possible
		if (!isReadyToApply ()) {
			throw new Exception ("CepsGHAThinner.apply () - Wrong input parameters, see Log");
		}
		
		double type_;
		double randomness;
		if(type>=0 & type<=0.5){
			randomness = (2d * type);
			type_ = 0d; //from below
		}else if(type<=1){
			randomness= 2d - 2d * type;
			type_ = 1d; //from above
		}else{
			randomness=0;
			type_ = 0;
			Log.println (Log.ERROR, "CepsGHAThinner.calcRandomness ()","CepsGHAThinner() - The thinning type must be a double between 0 and 1");
		}
		
		
		//set the number of trees needed to interpret the intensity
		
		stand1.setInterventionResult (true);

		double ci;
		Random random = new Random();
		double UniformProba;
		double probaOfBeingCut;
		double score;
		double rangeMax;
		double circOfMaxProba;

		//compute cmin and cmax needed to interpret the type (cm)
		cMin = 1000;
		cMax = 0;
		for (Iterator i = concernedTrees.iterator (); i.hasNext ();) {
			Tree t = (Tree) i.next ();
			ci = t.getDbh()*Math.PI;
			if (ci<cMin) cMin = ci;
			if (ci>cMax) cMax = ci;
		}

		circOfMaxProba = type * ( cMax- cMin) + cMin;
		rangeMax = circOfMaxProba - cMin + 1;
		if ( ( - circOfMaxProba +cMax + 1) > rangeMax ) {rangeMax = ( - circOfMaxProba + cMax + 1);}

		for (Iterator i = concernedTrees.iterator (); i.hasNext ();) {
			Tree t = (Tree) i.next ();
			ci = t.getDbh()*Math.PI;
			UniformProba = random.nextDouble();
			probaOfBeingCut = 1 - Math.abs(ci - circOfMaxProba)/rangeMax;
			score = randomness * UniformProba + 	(1-randomness) * probaOfBeingCut;
			ScoredTree scoredTree = new ScoredTree (t, score);
			arraytree.add(scoredTree);
		}
		Collections.sort(arraytree);

		//cutting trees
		//initialize
		int i = 0;
		double tmpGHA = initGHA;
		double g = 0;
		double gHa = 0;
		double nRemainingConcernedTrees = concernedTrees.size();
		while(tmpGHA > targetGHA & nRemainingConcernedTrees > 0){
			Tree t = arraytree.get(i).t;
			g = Math.pow(t.getDbh()/100,2) * Math.PI / 4;
			
			// fc-8.6.2020 considers group area is any
			gHa = g * 10000 / getSceneArea (stand1);
//			gHa = g * 10000 / stand1.getArea();

			// cut this tree
			stand1TC.removeTree(t);

			nRemainingConcernedTrees -= 1;
			i++;
			tmpGHA -= gHa;
		}
		
		if (nRemainingConcernedTrees <= 0 & tmpGHA > targetGHA){
			System.out.println("CepsGHAThinner.apply () - tmpGHA : " + tmpGHA + "targetGHA : " + targetGHA + "nRemainingConcernedTrees : " + nRemainingConcernedTrees);
			Log.println ("ForCEEPS", "CepsGHAThinner.apply () - the target basal area could not be reached.");
			System.out.println("CepsGHAThinner.apply () - the target basal area could not be reached.");
		}

		// OTHER species trees are now put back into stand1TC :			// PhD 2017-12-26
		for (int j = 0; j < vctOtherSpeciesTrees.size(); j++) {
			CepsTree t = (CepsTree) vctOtherSpeciesTrees.get(j);
			if(t != null) stand1TC.addTree(t);
		}
		
		return stand1;
	}


	/**
	 * toString () method.
	 */
	public String toString () {
		// nb-13.08.2018
		//return "class=" + getClass ().getName () + " name=" + NAME + " constructionCompleted=" + constructionCompleted + " mode=" + mode + " stand=" + stand1 +
		return "class=" + getClass ().getName () + " name=" + getName() + " constructionCompleted=" + constructionCompleted + " mode=" + mode + " stand=" + stand1 +
				" targetGHA =" + targetGHA + " Type =" + type;
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

			ScoredTree (Tree t, double score) {
				this.t = t;
				this.score=score;
			}

			public double getScore(){return score;}
			public void setScore(double score){this.score=score;}

			@Override
			public int compareTo(Object scoredtree) {
				double score1 = ((ScoredTree) scoredtree).getScore();
				double score2 = this.getScore();
				if (score1 < score2) return -1;
				else if(score1 == score2)return 0;
				else return 1;
			}
		}

	public double getInitGHA() {return initGHA;}

}
