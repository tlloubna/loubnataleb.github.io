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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

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
import jeeb.lib.util.AdditionMap;
import jeeb.lib.util.Alert;
import jeeb.lib.util.AmapTools;
import jeeb.lib.util.Log;
import jeeb.lib.util.Translator;

/**
 * CepsGHAThinner2bis: a tool that cuts trees according to a thinning param_type
 * (from top, from below), a target G intensity.
 *
 * @author X. Morin, B. Cornet, F. de Coligny - January 2018,
 *         with some components from GymnoGHAThinner2 by G. Ligot (gymnos module)
 */
public class CepsGHAThinner2bis extends AbstractGroupableIntervener implements Intervener, GroupableIntervener {
//public class CepsGHAThinner2bis implements Intervener, GroupableIntervener {

	// fc-8.6.2020 Added AbstractGroupableIntervener for getSceneArea (scene)

	static {
		Translator.addBundle("forceps.extension.intervener.CepsGHAThinner2bis");
	}

	// nb-13.08.2018
	//public static final String NAME = "CepsGHAThinner2bis";
	//public static final String VERSION = "1.0";
	//public static final String AUTHOR = "X. Morin, B. Cornet, F. de Coligny";
	//public static final String DESCRIPTION = "CepsGHAThinner2bis.description";

	// nb-30.08.2018
	//static final public String SUBTYPE = "SelectiveThinner"; // fc-4.12.2014
																// added final
																// (reviewing
																// static
																// variables
																// after a
																// Simmem bug)

	private boolean constructionCompleted = false; // if cancel in interactive
													// mode, false

	// Parameters

	protected double param_type; // [0, 1] from below, from above
	protected double param_finalG; // m2/ha G to be reached after thinning
	protected double param_finalGpercentage = -1; // optional, if set, in [0,
													// 100] if
	// >= 0, param_finalG is calculated in
	// init ()

	// Expected species mix after intervention, in percentage for each species,
	// sum percentages <= 100%
	// e.g. "AAlb-60%, FSyl-30%" (and 10% of other species)

	// Parameters

	private Set<String> expectedMix; // e.g. AAlb, FSyl
	private Map<String, Double> mixMap; // e.g. AAlb->60 Fsyl->30

	// sNames of the trees in the scene
	private Set<String> presentSNames;

	protected CepsScene scene; // Reference stand: will be altered by apply ()
	private CepsInitialParameters ip;

	private List<CepsTree> concernedTrees;
	private double initG; // m2/ha

	/**
	 * Default constructor.
	 */
	public CepsGHAThinner2bis() {
	}

	/**
	 * Script constructor.
	 *
	 * @param targetGHA
	 * 
	 *            is a double and corresponds to the target in m2/ha (i.e.
	 *            final, remaining after intervention) GHA - must be within
	 *            possible limits
	 * @param param_type
	 *            [0,1] : 0 for a from below thinning - 1 for a from above
	 *            thinning
	 * @throws Exception
	 */
	public CepsGHAThinner2bis(double param_type, double finalG)
			throws Exception {

		setType(param_type);

		// FinalG is tricky, can be absolute: 15.7 m2 or relative: 32%
		setFinalG(finalG);

		setExpectedMixComposition();//la composition attendu après la coupe 
	}

	//setter  le type de la coupe avec celle de la classe .
	protected void setType(double param_type) throws Exception {
		this.param_type = param_type;
		if (param_type < 0 || param_type > 1)
			throw new Exception("param_type must be in [0, 1]");//ce n'est pas dans l'intervalle [0,1]

	}
	//voir la surface térrière objective à la fin restante soit en m2/ha ou bien en %
	protected void setFinalG(double finalG) throws Exception {
		if (finalG > 1){
			this.param_finalG = finalG;
		}
		else if (finalG >= 0){
			param_finalGpercentage = finalG;
		}
		else{
			throw new Exception(
					"could not evaluate param_finalG, should be a number (absolute value in m2/ha) or a proportion (between 0 and 1)");
		}
	}
	//Quel composition attendu après la coupe 
	protected void setExpectedMixComposition(){

		expectedMix = new HashSet<>();//liste  des composés attendus
		mixMap = new HashMap<>();// dictionnaire des liste attendu avec l'espece et sa proportion en % par rapport au surface térrière 
		// add by michelle 19/07/23
		mixMap.put("otherSp", 100d);

	}

	@Override
	public void init(GModel m, Step s, GScene gscene, Collection c) {

		// This is referentStand.getInterventionBase ();
		scene = (CepsScene) gscene; //la scene ou on va placer les objets 
		ip = (CepsInitialParameters) m.getSettings(); //des configurations spècifiques à la scène 

		// The trees that can be cut
		if (c == null) { //si la collection d'arbres n'est pas fournie 
			concernedTrees = scene.getTrees(); //tous les arbres dans la scene sont concerné par la simulation 
		} else {
			concernedTrees = new ArrayList<CepsTree>(c); //sinon  on prend juste ces arbres qui ont déjà définie dans la collection 
		}

		initG = getGha(concernedTrees); //calculer la surface térriere des arbres concernée 

		presentSNames = new HashSet<>(); //liste des éspèces  présents dans la scene 
		for (CepsTree t : concernedTrees) {
			presentSNames.add(t.getSpecies().sname); //ajouter le nom  de l'espèce au tableau des espèces présentes 
		}

		constructionCompleted = true;//construction de la scène est términé 

	}

	@Override
	public boolean initGUI() throws Exception {
		// Interactive start
		//CepsGHAThinnerDialog2bis dlg = new CepsGHAThinnerDialog2bis(this);

		//constructionCompleted = false;
		//if (dlg.isValidDialog()) {
	//		constructionCompleted = true;
//		}
	//	dlg.dispose();

		return true;  //initialiser l'interface graphique  si nécessaire

	}

	/**
	 * Extension dynamic compatibility mechanism. This matchWith method checks
	 * if the extension can deal (i.e. is compatible) with the referent.
	 */
	static public boolean matchWith(Object referent) {
		try {
			return referent instanceof CepsModel; //pour vérifier l'extension peut traiter l'objet référent:travailler qu'aven des objets comptaibles  

		} catch (Exception e) {
			Log.println(Log.ERROR, "CepsGHAThinner2bis.matchWith ()", "Error in matchWith () (returned false)", e); //on n'arrive pas 
			return false;
		}

	}


	@Override
	public String getName() {
		return Translator.swap("CepsGHAThinner2bis.name");//le nom de fichier 
	}

	@Override
	public String getAuthor() {
		return "X. Morin, B. Cornet, F. de Coligny";//l'auteur de fichier 
	}

	@Override
	public String getDescription() {
		return Translator.swap("CepsGHAThinner2bis.description");
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getSubType() {
		return Translator.swap("CepsGHAThinner2bis.subType");
	}

	/**
	 * GroupableIntervener interface. This intervener acts on trees, tree groups
	 * can be processed.
	 */
	public GroupableType getGrouperType() {
		return TreeList.GROUP_ALIVE_TREE;//definir des actions ou des interventions qui peuvent etrre appliquée 
	}

	/**
	 * Returns the cumulated basal area of the given trees (m2/ha)
	 */
	protected double getGha(List<CepsTree> trees) {

		double gha = 0;
		for (CepsTree t : trees) {
			gha += getGHA(t);
		}

		return gha; //la surface téériere cummulé dans la liste des arbres
	}

	private double getGHA(Tree t) {
		double g = Math.pow(t.getDbh() / 100d, 2d) * Math.PI / 4d;

		// fc-8.6.2020 considers group area is any
		double gHa = g * 10000d / getSceneArea (scene);
//		double gHa = g * 10000d / scene.getArea();

		return gHa; //la surface térriere de l'arbre t en m2/ha 
	}

	/**
	 * Returns the trees with the given sName
	 */
	protected List<CepsTree> getTrees(List<CepsTree> candidateTrees, Set<String> expectedMix, String sName) {

		// StringBuffer b = new StringBuffer ("getTrees ("+sName+")... ");

		List<CepsTree> trees = new ArrayList<>();//la liste des arbres à remplir 

		for (CepsTree t : candidateTrees) {
			//les arbres candidate  sont ceux que l'on peut ajouter à	 la listes des arbres à remplir
			boolean treeIsSName = expectedMix.contains(sName) && t.getSpecies().sname.equals(sName);//est cette liste contient cette esp et est ce que le nom de l'arbre est le meme de Sname 
			boolean treeIsOtherSp = sName.equals("otherSp") && !expectedMix.contains(t.getSpecies().sname); //est ce Sname est parmi les autres esp et est ce que cette espece n'est p	s dans la liste des esp attendus 

			if (treeIsSName || treeIsOtherSp) {
				// b.append(t.getSpecies().sname+", ");
				trees.add(t); //on l'ajoute  si le nom est correct ou si on ne vérifie pas le nom dans la liste des arbres 
			}
		}

		// System.out.println(b.toString ());

		return trees;
	}

	/**
	 * These assertions are checked at the beginning of apply () in script AND
	 * interactive mode.
	 *
	 */
	private boolean assertionsAreOk() {

		if (scene == null) { //si on a une scene pour faire la simulation 
			Log.println(Log.ERROR, "CepsGHAThinner2bis.assertionsAreOk ()",
					"scene is null. CepsGHAThinner2bis is not appliable.");
			return false;
		}

		return true;
	}

	/**
	 * Intervener interface. Controls input parameters.//si la construction est appliqué et si on a une scene pour faire la simulation 
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
			this.param_finalG = initG * param_finalGpercentage;//si la surface térrière est exprimé en % on  l'a calculée à cette étape en mutlipiant son % par la surface initiale 



		if (param_finalG < 0)
			throw new Exception("error, param_finalG: " + param_finalG + " must be positive"); //on ne peut pas avoir une surface térriere negative 

		// Check mixMap keys and percentages on calcule le pourcentage finale de la surface térriere  de chaque espece 
		double sum = 0;
		for (String sName : mixMap.keySet()) {

			// fc+xm+bc-allow a command for a species not on the scene
			// (the cut command will be dispatched on the other species)
			// if (!presentSNames.contains(sName))
			// throw new
			// Exception("found a sName in the expected mix composition which is not present in the scene: "
			// + sName);

			double v = mixMap.get(sName);
			sum += v;
		}

		if (sum > 100)
			throw new Exception("error in expected mix map, sum of percentages must be lower than 100: " + sum); //oui on ne peut pas avoir une surface térrière qui est >à 100%

		// Check if apply is possible
		if (!isReadyToApply()) {
			throw new Exception("CepsGHAThinner2bis.apply () - Wrong input parameters, see Log"); //si tous est regle en terme de scene (on a une scene pour faire l'etude et que la construction de la classe est faite avec succès 
		}

		// Start of intervention
 
		scene.setInterventionResult(true);

		if (param_finalG > initG) {
			Alert.print("CepsGHAThinner2bis.apply (): param_finalG: " + param_finalG + " is lower than initG: " + initG
					+ ", cut nothing"); //c'est normal on fixe pas un objectif qui est supèrieur à ce qu'on a 
			return scene;
		}

		// test basal area cut if intervention // add by michelle 13/06/23
		double cutG = (initG - this.param_finalG); //m2/ha //G(t)-Gobj(t) 
		if ( cutG < 0) {
			cutG = initG; //si  G(t)<Gobj(t), on coupe tous les arbres  de la scence, donc G=Gobj
		}

		// Stage 1. Determining accurately what must be cut (how much G must be
		// left for each species in the command + other species)
		// i.e. check if the command can be processed or change it accordingly
		// to reality to make it possible
		// Reality means how much G is actually present for each species in the
		// scene

		System.out.println("CepsGHAThinner2bis entering stage1...");//on commence par couper des arbres 

		// MIX (uppercase) contains all entries in param_expectedMixComposition
		// + "otherSp"
		// Values are G values expected to be found at the end
		Map<String, Double> expectedMIXpercentages = new HashMap<>(); //une liste pour chaque espèce avec le pourcentage qu'on a visioned time t
		double otherSpPercentage = 100d; //les autres espces ont un pourcentage 100% d'apparition
		for (String sName : mixMap.keySet()) {
			double percentage = mixMap.get(sName);
			otherSpPercentage -= percentage; //on soustraint les autres pourcentages de la liste pour déternimer le porcentage des autres esp

			expectedMIXpercentages.put(sName, percentage);//on ajoute l'espèce couper avec son poucentage 

		}
		expectedMIXpercentages.put("otherSp", otherSpPercentage);//et on ajoute les autres espèces 

		Map<String, Double> expectedMIXvalues = new HashMap<>(); //on convertir les pourcentage en surface térrière m2/ha G*pert/100
		resetExpectedMIXvalues(param_finalG, expectedMIXpercentages, expectedMIXvalues);

		System.out.println("initial expectedMIXpercentages: " + traceMap(expectedMIXpercentages));//les pourcentages initiales des espèces 
		System.out.println("initial expectedMIXvalues: " + traceMap(expectedMIXvalues));//la surface térrire en m2/ha 
		System.out.println("initial param_finalG: " + param_finalG); //la surface terrire finale 

		double tmp_finalG = param_finalG; //une valeur temporaire  pour le calcul de finalG
		int security = 0;
		while (!cutIsPossible(tmp_finalG, expectedMIXvalues)) { //tous les especes ont une surface térrière inférieur à la surface térrière prevue =>les espèces ont suffisement de surface pour etre coupées 

			tmp_finalG = changeExpectedMIXvalues(tmp_finalG, expectedMIXpercentages, expectedMIXvalues);//calculer la surface térrière en respectant la contrainte que tous les espèces ont une surface térrière suffisante et on supprime les espè qui n'ont pas une surface térrire suffisante

			System.out.println("changed expectedMIXpercentages: " + traceMap(expectedMIXpercentages));
			System.out.println("changed expectedMIXvalues: " + traceMap(expectedMIXvalues));
			System.out.println("changed tmp_finalG: " + tmp_finalG);

			if (++security > 50)
				throw new Exception("CepsGHAThinner2bis error, could not adjust the expected mix composition in "
						+ security + " iterations, aborted");
		}

		// Stage 2. cut the trees accordingly

		System.out.println("CepsGHAThinner2bis entering stage2, expectedMIXvalues: " + traceMap(expectedMIXvalues));

		double remainingTotalG = initG; // la surface térrière intiale qu'on a  encore à couper

		for (String sName : expectedMIXvalues.keySet()) {

			// Extract the trees of the given sName
			List<CepsTree> sNameTrees = getTrees(concernedTrees, expectedMix, sName);

			double expectedG = expectedMIXvalues.get(sName);
			remainingTotalG = processCut(expectedG, sNameTrees, remainingTotalG); //couper de façon soit croissante ou décroissante en tenant compte des espèces que j'ai et que la surface térriere obj et la surface térrière attendu

			if (remainingTotalG <= param_finalG)
				break;//si la surface terrière parce qu'on veut est supèrieure au total des surfaces des arbres on sort de la boucle

		}

		System.out.println("CepsGHAThinner2bis: before cut: " + traceSceneComposition(concernedTrees));
		System.out.println("CepsGHAThinner2bis: after cut: " + traceSceneComposition(scene.getTrees()));
		System.out.println("CepsGHAThinner2bis: param_finalG: " + param_finalG + " remainingTotalG: " + remainingTotalG);

		return scene;//retourner ce scénario

	}

	private String traceSceneComposition(List<CepsTree> trees) {
		StringBuffer b = new StringBuffer("Scene composition:");

		// fc-1.12.2021 AdditionMap key type now generic, added <String>
		AdditionMap<String> map = new AdditionMap<>();
		double totalGHA = 0d;
		for (CepsTree t : trees) {
			String sName = t.getSpecies().sname;
			double gha = getGHA(t);
			map.addValue(sName, gha);
			totalGHA += gha;
		}

		for (String sName : map.getKeys()) {
			double gha = map.getValue(sName);
			double percentage = gha / totalGHA * 100d;

			b.append(" " + sName + ": " + gha + " (" + percentage + "%)");
		}

		return b.toString();
	}
	//retourner l'ensemble des espève avec la surface térriére que j'ai est sont pourcentage 

	//convertir le dictionnaire en string  et renvoyer
	private String traceMap(Map<String, Double> map) {
		String s = AmapTools.toString(map).replace('\n', ' ');
		s = s.replace("  ", " ");
		return s;
	}

	/**
	 * Detect which entries can not be processed in expectedMIXvalues (because
	 * sp_realG is lower than sp_expectedG). Remove these entries: all sp_realG
	 * will be kept. Change the other entries (decrease their sp_expectedG) to
	 * cut them more, by removing the missing sp_realG from their sp_expectedG.
	 */
	private double changeExpectedMIXvalues(double tmp_finalG, Map<String, Double> expectedMIXpercentages,
			Map<String, Double> expectedMIXvalues) {

		// Contains sNames and 'otherSp'
		Set<String> problematicSpecies = new HashSet<>();
		double sp_correctPercentageSum = 0d;

		for (String sName : expectedMIXvalues.keySet()) {
			//identifier les esp qui ont une surface térrière insuffisante 
			double sp_expectedG = expectedMIXvalues.get(sName);

			// Extract the trees of the given sName
			List<CepsTree> sNameTrees = getTrees(concernedTrees, expectedMix, sName);
			double sp_realG = getGha(sNameTrees);


			if (sp_expectedG > sp_realG) { // problematic

				tmp_finalG -= sp_realG; //on soustrait la surface téérière de ce qu'on a  déjà trouvé pour cette espèce
				problematicSpecies.add(sName); //cette espèce a une G insuffisante 

			} else {
				sp_correctPercentageSum += expectedMIXpercentages.get(sName);//sinon on l'ajoute dabs la tables des pourcentages vrais  des especes prevus 
			}

		}

		for (String sName : new ArrayList<>(expectedMIXpercentages.keySet())) {

			double sp_expectedPercentage = expectedMIXpercentages.get(sName);

			if (problematicSpecies.contains(sName)) {
				expectedMIXpercentages.remove(sName); //supprimer l'espcece qui un a un pourcentage eleève 

			} else {

				if (sp_correctPercentageSum != 0){
					// change percentage for this remaining species
					double newPercentage = (sp_expectedPercentage / sp_correctPercentageSum) * 100d;
					expectedMIXpercentages.put(sName, newPercentage);
				}
				else { // case where it just remains otherSp=0.0 // add by michelle 13/06/23
					double newPercentage = (tmp_finalG /this.param_finalG)*100d;
					expectedMIXpercentages.put(sName, newPercentage); //on recalcule le pourcentage des esp qui ont une surface térrière suffisante
					resetExpectedMIXvalues(this.param_finalG, expectedMIXpercentages, expectedMIXvalues);
					return tmp_finalG;
				}
			}
		}

		resetExpectedMIXvalues(tmp_finalG, expectedMIXpercentages, expectedMIXvalues);
		return tmp_finalG;
	}

	/**
	 * Returns true if all the species have enough G to allow the cut planned in
	 * expectedMIXvalues (contains remaining G expected after the cut).
	 * 
	 */
	private boolean cutIsPossible(double tmp_finalG, Map<String, Double> expectedMIXvalues) {

		for (String sName : expectedMIXvalues.keySet()) {

			double sp_expectedG = expectedMIXvalues.get(sName); //les  G attendus pour cette espèce

			// Extract the trees of the given sName
			List<CepsTree> sNameTrees = getTrees(concernedTrees, expectedMix, sName);
			double sp_realG = getGha(sNameTrees); //on calcule la surface térrière de l'espece I 

			if (sp_expectedG > sp_realG) {
				System.out.println("cutIsPossible false for sName: " + sName + ", sp_expectedG: " + sp_expectedG
						+ ", sp_realG: " + sp_realG);
				return false; //si la surface  attandue est plus grande que la superficie real =>on ne peut pas couper
			} 
		}
		return true;//si tous les espèces ont suffisement de surface pour  le coup prevue dans espectedMixvalues donc on coupe 
	}

	/**
	 * Clears and restores expectedMIXvalues according to expectedMIXpercentages
	 * (there may be less keys in the result).
	 */
	private void resetExpectedMIXvalues(double G, Map<String, Double> expectedMIXpercentages,
			Map<String, Double> expectedMIXvalues) {

		expectedMIXvalues.clear();

		for (String sName : expectedMIXpercentages.keySet()) {
			double percentage = expectedMIXpercentages.get(sName);
			double value = G * percentage / 100d;

			expectedMIXvalues.put(sName, value);
		}

	}

	/**
	 * Process the cut for the trees of a given species, cut to reach the given
	 * final basal area sp_finalG in the resulting scene. sp_finalG was
	 * calculated before in order to be reachable. Special case: this method can
	 * be called for the list of "other species trees". Returns the
	 * remainingTotalG after this cut processCut.
	 */
	private double processCut(double sp_finalG, List<CepsTree> sNameTrees, double remainingTotalG) {

		double remainingG = getGha(sNameTrees);//retourner la surface térriere des esp que j'ai 

		// Score the trees
		double type_;
		double randomness = 0; //comment comparer ce score CROIt ou bien décroit 
		if (param_type >= 0 && param_type <= 0.5) {
			randomness = (2d * param_type);
			type_ = 0d; // from below
		} else if (param_type <= 1) {
			randomness = 2d - 2d * param_type;
			type_ = 1d; // from above
		}

		double ci;
		Random random = new Random();//une varibale aléatoire 
		double uniformProba;//une probabilité unifomre d'etre coupée 
		double probaOfBeingCut; //la probabilité d'etre coupée 
		double score;//le score de coupe 
		double rangeMax;//la  limite sup pour le score de coupe 
		double circOfMaxProba;//la  circonférence maximale de  de probabilité de coupe 

		// Compute cmin and cmax needed to interpret the param_type (cm)
		double cMin = 1000; 
		double cMax = 0;
		for (Iterator i = concernedTrees.iterator(); i.hasNext();) {
			Tree t = (Tree) i.next();
			ci = t.getDbh() * Math.PI;
			if (ci < cMin)
				cMin = ci;//la circonférence  de base minimale est plus petite
			if (ci > cMax)
				cMax = ci;//le circonférence  de base maximale  est plus grande
		}

		circOfMaxProba = param_type * (cMax - cMin) + cMin;//
		rangeMax = circOfMaxProba - cMin + 1;//la limite 
		if ((-circOfMaxProba + cMax + 1) > rangeMax) {
			rangeMax = (-circOfMaxProba + cMax + 1);
		}

		List<ScoredTree> arrayTree = new ArrayList<ScoredTree>();
		for (Iterator i = sNameTrees.iterator(); i.hasNext();) {
			Tree t = (Tree) i.next();

			ci = t.getDbh() * Math.PI;
			uniformProba = random.nextDouble();
			probaOfBeingCut = 1 - Math.abs(ci - circOfMaxProba) / rangeMax;
			//k_alfa = randomness * uniformProba + (1 - randomness) ???
			score = randomness * uniformProba + (1 - randomness) * probaOfBeingCut;

			ScoredTree scoredTree = new ScoredTree(t, score);
			arrayTree.add(scoredTree);
		}//correspond bien aux formules définis dans le rapport 
		Collections.sort(arrayTree); //couper par ordre croisssant des scores     

		// Cut the trees
		int i = 0;
		// double cutGsum = 0;
		while (remainingG > sp_finalG && i < arrayTree.size()) {

			Tree t = arrayTree.get(i).t;
			double gHa = getGHA(t);

			// cut this tree
			scene.removeTree(t);

			// cutGsum += gHa;
			remainingG -= gHa;
			remainingTotalG -= gHa;//c'est avec la surface térrière avec les autres espèces 

			if (remainingTotalG <= param_finalG)
				break;//on coupe jusqu'à que la surface térrière restante 

			i++;
		}

		return remainingTotalG;
	}

	/**
	 * toString () method.
	 */
	public String toString() {
		// nb-13.08.2018
		//return "class=" + getClass().getName() + " name=" + NAME + " constructionCompleted=" + constructionCompleted
		return "class=" + getClass().getName() + " name=" + getName() + " constructionCompleted=" + constructionCompleted
				+ " stand=" + scene + " param_type =" + param_type
				+ " param_finalG =" + param_finalG ;
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
