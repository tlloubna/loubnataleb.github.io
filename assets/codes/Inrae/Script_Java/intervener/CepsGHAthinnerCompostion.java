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

 import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
 import java.io.IOException;
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

import javax.rmi.ssl.SslRMIClientSocketFactory;

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
  
  *This class applies thinning methods in ForCEEPS, with a method to 
  *respect the constraint that the Gobj is less than the Greal, 
  *and with a logic where we choose the tree that minimizes the difference between the composition 
  *of species in the actual and the target.
  * @author loubna TALEB APRIL 2024
  *         
  */
 public class CepsGHAthinnerCompostion extends AbstractGroupableIntervener implements Intervener, GroupableIntervener {
    
 
	 static {
        Translator.addBundle("forceps.extension.intervener.CepsGHAThinner2");
    }

   

    private boolean constructionCompleted = false; // if cancel in interactive
                                                                // mode, false
            
    // Parameters
            
    protected double param_minCutG; // m2/ha
    protected double param_type; // [0, 1] from below, from above
    protected double param_finalG; // m2/ha G to be reached after thinning
    protected double param_finalGpercentage = -1; // optional, if set, in [0,
                                                                // 100] if
               
	 public String param_expectedMixComposition;
 
	 // Parameters
 
	 public  Set<String> expectedMix; // e.g. AAlb, FSyl
	 public  Map<String, Double> mixMap; // e.g. AAlb->60 Fsyl->30
    
	 // sNames of the trees in the scene
	 private Set<String> presentSNames;
     protected CepsScene scene; // Reference stand: will be altered by apply ()
	 private CepsInitialParameters ip;
 
	 public List<CepsTree> concernedTrees;
	 public double initG; // m2/ha
	 
    private BufferedWriter logger; //To save the différence between the diff between the G and Cf

    private long seed = 12345;//To fix the seed when calculating the alpha score.

    // methods to enregister the data
    private void initLogger(String outputDir) throws IOException {
        File file = new File(outputDir, "diff_cut");
        logger = new BufferedWriter(new FileWriter(file, true));  // false to overwrite
       
    }
    //used to save the data related of the volume of cutting
    private void logDetails(String species, double volumeCut) throws IOException {
        logger.write(species + "\t" + volumeCut + "\n");
    }

    private void closeLogger() throws IOException {
        if (logger != null) {
            logger.close();
        }
    }
    /**
	  * Default constructor.
	  */
    public CepsGHAthinnerCompostion() {
    }
    /**
	  * Script constructor.
	  * 
	  * @param targetGHA
	  *            is a double and corresponds to the target in m2/ha (i.e.
	  *            final, remaining after intervention) GHA - must be within
	  *            possible limits
	  * @param param_type
	  *            [0,1] : 0 for a from below thinning - 1 for a from above
	  *            thinning
	  * @throws Exception
	  *
    */
	 public CepsGHAthinnerCompostion( double param_type, String finalGtext, String param_expectedMixComposition)
     throws Exception 
     {
            //initLogger();
           // setMinCutG(minCutG);
            
            setType(param_type);

            // FinalG is tricky, can be absolute: 15.7 m2 or relative: 32%
            setFinalG(finalGtext);
            
            setExpectedMixComposition(param_expectedMixComposition);
    }

    //the minimun of the basal Area
    protected void setMinCutG(double minCutG) throws Exception {
        this.param_minCutG = minCutG;
        if (minCutG < 0)
            throw new Exception("param_minCutG must be positive or null");
    }

    //the type of thining : 0,1,0.5
    protected void setType(double param_type) throws Exception {
        this.param_type = param_type;
        if (param_type < 0 || param_type > 1)
            throw new Exception("param_type must be in [0, 1]");

    }

    //the final GHA:{50,60,70,80,90}
    protected void setFinalG(String finalGtext) throws Exception {

    try 
    {
            // if number, absolute value, take as is
            this.param_finalG = Double.parseDouble(finalGtext);
    } 
    catch (Exception e)
     {
        try
         {
                // if not a number, we expect a percentage like: 30%
                String token = finalGtext.trim();
                if (!token.contains("%"))
                    throw new Exception();
                token = token.replace("%", "").trim();
                param_finalGpercentage = Double.parseDouble(token);
                if (param_finalGpercentage < 0 || param_finalGpercentage > 100)
                    throw new Exception();

        } 
        catch (Exception e2) 
        {
            throw new Exception(
                        "could not evaluate param_finalG, should be a number (absolute value in m2/ha) or a percentage with a '%'");
        }

        }

    }
    //the expected mix composition
    protected void setExpectedMixComposition(String param_expectedMixComposition) throws Exception {

        if (param_expectedMixComposition == null || param_expectedMixComposition.length() == 0)
            throw new Exception("missing expected mix composition");

        this.param_expectedMixComposition = param_expectedMixComposition;
        expectedMix = new HashSet<>();
        mixMap = new HashMap<>();
        

        double mixPercentageControl = 0;
        double percentage_max=100;
        StringTokenizer st = new StringTokenizer(param_expectedMixComposition, ", ");
        while (st.hasMoreTokens()) {

            try 
            {
                String token = st.nextToken().trim();

                int separatorIndex = token.indexOf("-");
                String sName = token.substring(0, separatorIndex).trim(); // e.g.
                                                                            // AAlb
                String p = token.substring(separatorIndex + 1);
                double percentage = Double.parseDouble(p);
                

                if (percentage < 0 || percentage > 100)
                    throw new Exception("wrong mix composition value for: " + sName);

                expectedMix.add(sName);
                mixMap.put(sName, percentage);

                mixPercentageControl += percentage;
                percentage_max-=percentage;

            } 
            catch (Exception e) 
            {
                throw new Exception("could not evaluate an expected mix composition", e);
            }

        }
        if (percentage_max==0)
        {
            // The objective is to adjust 'otherSp' to achieve the target 'sible' that we want.
            mixMap.put("otherSp",0.0d);
            expectedMix.add("otherSp");
           
        }

        if (mixPercentageControl > 100)
            throw new Exception("error in param_expectedMixComposition, sum of percentages must be lower than 100");

    }
    //initialiser la scene 
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

        presentSNames = new HashSet<>();
        for (CepsTree t : concernedTrees) {
            presentSNames.add(t.getSpecies().sname);
        }

        constructionCompleted = true;

    }

    @Override
    public boolean initGUI() throws Exception {
       

        return true;

    }

    /**
     * Extension dynamic compatibility mechanism. This matchWith method checks
     * if the extension can deal (i.e. is compatible) with the referent.
     */
    static public boolean matchWith(Object referent) {
        try {
            return referent instanceof CepsModel;

        } catch (Exception e) {
            Log.println(Log.ERROR, "CepsGHAThinner2.matchWith ()", "Error in matchWith () (returned false)", e);
            return false;
        }

    }

    @Override
    public String getName() {
        return Translator.swap("CepsGHAThinner2.name");
    }

    @Override
    public String getAuthor() {
        return "X. Morin, B. Cornet, F. de Coligny,L.TALEB";
    }

    @Override
    public String getDescription() {
        return Translator.swap("CepsGHAThinner2.description");
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getSubType() {
        return Translator.swap("CepsGHAThinner2.subType");
    }

    /**
     * GroupableIntervener interface. This intervener acts on trees, tree groups
     * can be processed.
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
    //the BasalARea of the tree t.
    private double getGHA(Tree t) {
        double g = Math.pow(t.getDbh() / 100d, 2d) * Math.PI / 4d;
        
        
        double gHa = g * 10000d / getSceneArea (scene);

        
        return gHa;
    }

    /**
     * Returns the trees with the given sName
     */
    protected List<CepsTree> getTrees(List<CepsTree> candidateTrees, Set<String> expectedMix, String sName) {

        

        List<CepsTree> trees = new ArrayList<>();

        for (CepsTree t : candidateTrees) 
        {

            boolean treeIsSName = expectedMix.contains(sName) && t.getSpecies().sname.equals(sName); // ajouter pour l'espèce
            boolean treeIsOtherSp = sName.equals("otherSp") && !expectedMix.contains(t.getSpecies().sname); //ajouter si c'etait othersp 

            if (treeIsSName || treeIsOtherSp) {
                
                trees.add(t);
            }
        }

        

        return trees;
    }

    /**
     * These assertions are checked at the beginning of apply () in script AND
     * interactive mode.
     *
     */
    private boolean assertionsAreOk() {

        if (scene == null) {
            Log.println(Log.ERROR, "CepsGHAThinner2.assertionsAreOk ()",
                    "scene is null. CepsGHAThinner2 is not appliable.");
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
    /* To sort the trees for each species so that we have, for example:
        * PSYl: (tree1, alpha1),(tree2,alpha2),(tree3,alpha3)....
        * CBET: (tree1, alpha1),(tree2,alpha2),
        *alpha le score de l'arbre tree selon tp 
    */
    private Map<String, List<ScoredTree>> sortTrees(Set<String> expectedMix) 
    {

    // Dictionary where the key is the species and the value is a list of trees for that species
    HashMap<String, List<ScoredTree>> sortedTree = new HashMap<String, List<ScoredTree>>();

    double type;
    double randomness = 0;

    if (param_type >= 0 && param_type <= 0.5)
    {
        randomness = (2d * param_type);
        type = 0d; // from below
    } else if (param_type <= 1) {
        randomness = 2d - 2d * param_type;
        type = 1d; // from above
    }
    // Set the alpha score parameters
    double ci;
    Random random = new Random(seed);
    double uniformProba; // when tp=0.5
    double probaOfBeingCut; //
    double score; // alpha
    double rangeMax; // the denominator
    double circOfMaxProba; // to adjust the denominator based on tp=0.5 and tp=1
    // Calculate Cmax and Cmix for all species in the intervention so it
    // does not change after each thinning

    double cMin = 1000d;
    double cMax = 0d;
    
    for (Iterator i = concernedTrees.iterator(); i.hasNext();) 
    {
        Tree t = (Tree) i.next();
        ci = t.getDbh() * Math.PI;
        if (ci < cMin) {
            cMin = ci; //
        }
        if (ci > cMax) {
            cMax = ci;
        }
    }

    // Calculate the denominator for tp=0 and 1
    circOfMaxProba = param_type * (cMax - cMin) + cMin;
    rangeMax = circOfMaxProba - cMin + 1; // for tp=1 => Cmax-Cmin+1

    // case tp=0 => circofMaxProba=Cmin so rangeMax=1

    if ((-circOfMaxProba + cMax + 1) > rangeMax) 
    {
        rangeMax = (-circOfMaxProba + cMax + 1);
    }

    // Sort for each species in expectedMix and add them to sortedTree
    // for each species in expectedMix

    for (String name : expectedMix) 
    {
        List<ScoredTree> arrayTree = new ArrayList<ScoredTree>();
        // Retrieve all trees for the species 'name'
        List<CepsTree> sNameTrees = getTrees(concernedTrees, expectedMix, name);

        for (Iterator i = sNameTrees.iterator(); i.hasNext();) 
        {
            Tree t = (Tree) i.next();
            ci = t.getDbh() * Math.PI;
            uniformProba = random.nextDouble();
            probaOfBeingCut = 1 - Math.abs(ci - circOfMaxProba) / rangeMax;
            score = randomness * uniformProba + (1 - randomness) * probaOfBeingCut;
            ScoredTree scoredTree = new ScoredTree(t, score);
            arrayTree.add(scoredTree);
        }
        // Sort the trees of the species 'name'
        Collections.sort(arrayTree);
        // Add arrayTree to sortedTree
        sortedTree.put(name, arrayTree);
    }

    /*System.out.println("Display scores to verify they are sorted in ascending order of score:");
    for (String species : expectedMix) {
        List<ScoredTree> trees = sortedTree.get(species);
        System.out.println("Sorted trees for species " + species + ":");
        for (ScoredTree st : trees) {
            System.out.println("Tree ID: " + st.getTree().getId() + ", Score: " + st.getScore());
        }
    }*/

    return sortedTree;
    }

    /* Select k: from the sorted dictionary sortedTree, we select the first k
    * for each species we choose: k1, k2, k3, k4, k5, ...
    * Parameters: sortedTree: sorted dictionary of trees
    * Return: List<Cepstree>: list of the top trees sorted by score and
    * thinning type tp = {0, 0.5, 1},
    * the algorithm is available for any number of species, not just five.
    */
    private List<Tree> selectkTree(Map<String, List<ScoredTree>> sortedTree) {
    List<Tree> kTrees = new ArrayList<>();
    for (String name : sortedTree.keySet()) {
        List<ScoredTree> arrayTree = sortedTree.get(name);
        if (!arrayTree.isEmpty()) { // Ensure there is at least one tree in the list
            kTrees.add(arrayTree.get(0).getTree()); // Add the first tree without the score
        }
    }
    
    /*for (int i = 0; i < kTrees.size(); i++) {
        System.out.println("The top k trees: " + kTrees.get(i).getId());
    }*/
    return kTrees;

    }

    /* Composition_new: We need to calculate the current composition after each cut during
        * the intervention to compare it with the target composition.
        * Parameters: Tree: the tree chosen to be cut
        *             G_esp: current area of the species 'esp' in m²/ha
        *             G_init: initial area of the species 'esp' in m²/ha
        * Return: double (G_esp - G_tree) / G_init
    */

    private Double Composition_new(Tree t, Double G_esp, Double G_init_esp)
    {
        Double G_tree = getGHA(t);
        return (G_esp - G_tree) / G_init_esp;
    }


     /* Composition_act: I retrieve the dictionary of the current composition after cutting tree t
        * among the different top k trees.
        * Parameters: expectedMix: the different species that exist
        *             G_int: the initial surface area of each species
        *             Tree t: the tree of species 'esp'
        *             G_esp_act: current surface area in m²/ha
        * Return: return the composition of all species after cutting tree t in %
        */
    private Map<String, Double> Composition_act(Set<String> expectedMix, Double G_int, Tree t, Map<String, Double> G_esp_act) 
    {
        Map<String, Double> C_act = new HashMap<>();
        Double C_a = 0.0d;
        
        // System.out.println("Here is the composition before cutting:" + tmp_C_act + " Here is the surface of t:" + getGHA(t));
        String speciesOfTree = (t instanceof CepsTree) ? ((CepsTree) t).getSpecies().sname : "othersp";

        for (String name : expectedMix) {
            // assign to each species its initial surface
            Double G_esp = G_esp_act.get(name);
            //Voir  pourquoi j'ai pas initialiser G_int 
            if (G_int != 0.0d) 
            {
                if (name.equals(speciesOfTree)) 
                {
                    Double G_tree = getGHA(t); 
                    C_a = (G_esp - G_tree) * 100d / G_int; // Recalculate after assumed cutting % Gint=surface terrire totale  (60%)

                } 
                else 
                {
                    C_a = G_esp * 100d / G_int;
                }
                C_act.put(name, C_a);
            }
        }
         // System.out.println("Here is the composition after cutting the tree:" + C_act);
        return C_act;
        }   

   /* 
    *norm_k: calculate the difference between the composition after cutting tree k
    * Parameters: currentComposition: the current composition after cutting tree k
    * mixMap: the target composition but in proportions {0, 0.1, 0.2, ..., 0.9, 1}
    * Return: the norm ||Ck_act - mixMap|| mixMpa : la composition cible 
    */
    public double calculateNorm(Map<String, Double> currentComposition) 
    {
        double sumOfSquares = 0.0;

        // Iterate over the keys (species) of the target composition
        for (Map.Entry<String, Double> entry : mixMap.entrySet()) 
        {
            String species = entry.getKey();
            double targetPercentage = entry.getValue();
            double currentPercentage = currentComposition.getOrDefault(species, 0.0);

        // Calculate the squared difference for each species
        sumOfSquares += Math.abs(currentPercentage - targetPercentage);
        }

    // Square root of the sum of squares to obtain the Euclidean norm
    // System.out.println("The norm of the difference: " + Math.sqrt(sumOfSquares));
    return Math.sqrt(sumOfSquares);
    }

  

     /**
         * Applies the thinning operation to the scene based on specified parameters.
         * Initializes logging, performs initial checks, calculates compositions,
         * selects trees for cutting based on scores, and updates various metrics.
         * 
         * @return The updated scene object after thinning operation.
         * @throws Exception If there are errors during the thinning operation.
    */
    public Object apply() throws Exception 
    {
            // Initialize logging at the beginning of the apply method: you should to change for your output 
            //
            /*String outputDir = "/home/loubna/bibliographie/canew/data_test/data_cut";
            File file = new File(outputDir);
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            if (!file.exists())
                file.createNewFile();
            FileWriter fw = new FileWriter(file, true);*/

            System.out.println("le type d'eclairci est : " + param_type + " et  G_obj:" + param_finalGpercentage);

            // Initial checks and calculations
            if (param_finalGpercentage >= 0)
                this.param_finalG = initG * param_finalGpercentage / 100d;
            if (param_finalG < 0)
                throw new Exception("error, param_finalG: " + param_finalG + " must be positive");
            //la somme doit etre egale à 100
            double sum = 0;
            for (String sName : mixMap.keySet()) 
            {
                double v = mixMap.get(sName);
                sum += v;
            }

            if (sum > 100)
                throw new Exception("error in expected mix map, sum of percentages must be lower than 100: " + sum);

            if (!isReadyToApply())
                throw new Exception("CepsGHAThinner2.apply() - Wrong input parameters, see Log");

            scene.setInterventionResult(true);

            if (param_finalG > initG) 
            {
                System.out.println("CepsGHAThinner2.apply(): param_finalG: " + param_finalG + " is lower than initG: " + initG + ", cut nothing");
                return scene;
            }

            // Dictionary to fill with cut species
            Map<String, Double> mixVolume_cut = new HashMap<>();
            for (String name : expectedMix) 
            {
                mixVolume_cut.put(name, 0.0d);
            }

            Double G_cut = 0.0d;
            Double B_m_ha = 0.0d;
            Set<String> NamesSp = expectedMix;
            NamesSp.add("otherSp");

            // Sort trees for each species including "otherSp"
            Map<String, List<ScoredTree>> sortedTree = sortTrees(NamesSp);

            // Calculate initial surface areas for each species
            Map<String, Double> G_esp = new HashMap<>();
            for (String name : NamesSp) {
                
                List<CepsTree> NamesTree = getTrees(concernedTrees, expectedMix, name);
                G_esp.put(name, getGha(NamesTree));
            }

            // Iteration counter
            int i = 0;
            Double G_int = initG;
            Double pourcentage=0.0d;
            // Main loop to cut trees until param_finalG is achieved
            while (param_finalG < initG - G_cut) 
            {
                i += 1;
                // Calculate current composition
                Map<String, Double> tmp_Ckact = new HashMap<>();
                for (String name : expectedMix) 
                {
                    tmp_Ckact.put(name, (G_esp.get(name) * 100) / G_int);
                }

                if (param_finalGpercentage == 100) 
                {
                    System.out.println("on ne  coupe pas car  Gobj=:" + param_finalG+"et initG :"+initG );
                    break;
                    
                } 

                // Select the top k trees
                List<Tree> K_trees = selectkTree(sortedTree);

                // Select the tree with minimal norm
                if (K_trees.isEmpty()) {
                    System.out.println("la liste K_tree est vide : on sort ");
                    break;
                }

                Tree minTree = null;
                Double min = Double.MAX_VALUE;
                Double norm = 0d;

                for (Tree tree : K_trees) {
                    Map<String, Double> Compk_act = Composition_act(NamesSp, G_int, tree, G_esp);//pour le choix de l'arbre coupée
                    norm = calculateNorm(Compk_act);

                    

                    if (norm < min) 
                    {
                        min = norm;
                        minTree = tree;
                    }
                }

                // Cut minTree from the scene
                if (minTree != null) 
                {
                    String speciesOfTree = ((CepsTree) minTree).getSpecies().sname;

                    // Update G_esp based on the tree species
                    if (expectedMix.contains(speciesOfTree))
                     {
                        G_esp.put(speciesOfTree, G_esp.get(speciesOfTree) - getGHA(minTree));
                    } else {
                        G_esp.put("otherSp", G_esp.get("otherSp") - getGHA(minTree));
                    }

                    // Remove the tree from the scene
                    scene.removeTree(minTree);
                    removeTreeFromSortedTree(minTree, sortedTree);

                    G_cut += getGHA(minTree);
                    G_int -= getGHA(minTree);
                    B_m_ha += ((CepsTree) minTree).getVolume();
                }
            }

           
            // Print scene compositions before and after thinning
            System.out.println("CepsGHAThinner2: avant coupe : " + traceSceneComposition(concernedTrees));
            System.out.println("CepsGHAThinner2: La composition cible est : " + param_expectedMixComposition);
            System.out.println("CepsGHAThinner2: apres coupe: " + traceSceneComposition(scene.getTrees()));
            System.out.println("CepsGHAThinner2: param_finalG: " + param_finalG + " et la surface restante apres coupe : " + G_int + " et la surface initiale : " + initG);
            //System.out.println("Voici G_esp act: " + traceMap(G_esp));

            // Calculate B_m_ha with adjustment factor
            double haFactor = 10000d / getSceneArea(scene);
            B_m_ha *= haFactor;
            System.out.println("le facteur est : " + haFactor + " et B_m_ha : " + B_m_ha);

            return scene;
    }

 

    //  method to remove a tree from the sorted trees structure
    private void removeTreeFromSortedTree(Tree tree, Map<String, List<ScoredTree>> sortedTree) 
    {
            for (List<ScoredTree> list : sortedTree.values())
            {
                list.removeIf(scoredTree -> scoredTree.getTree().equals(tree));
                //System.out.println("l'arbre :"+tree.getId()+"est coupee ");
            }
    }
    /*From the trees , we calculate the percentage of each basal_area and 
    *attribue to evry tree.
    */

    private String traceSceneComposition(List<CepsTree> trees) 
    {
        StringBuffer b = new StringBuffer("");

        // fc-1.12.2021 AdditionMap key type now generic, added <String>
        //AdditionMap<String> map = new AdditionMap<>();
        Set<String> NamesSp = expectedMix;
        NamesSp.add("otherSp");
        Map<String, Double> map=new HashMap<>();
        double totalGHA = getGha(trees);
        //Trouver les arbres de ces espcèes
        for (String Sname :NamesSp) {
            List<CepsTree> tree_sp=getTrees(trees, expectedMix, Sname);
            double gha=getGha(tree_sp);
            double percentage = gha / totalGHA * 100d;
            b.append(" " + Sname + ": " +  " " + percentage + "%");

        }
        
        /*for (CepsTree t : trees) 
        {
            String sName = t.getSpecies().sname;
            double gha = getGHA(t);
            map.addValue(sName, gha);
            totalGHA += gha;
        }

        for (String sName : map.getKeys())
         {
            double gha = map.getValue(sName);
            double percentage = gha / totalGHA * 100d;

            b.append(" " + sName + ": " +  " " + percentage + "%");
        }*/
        

        return b.toString();
    }
    
    //Transform the map until a string pour l'afficher
    private String traceMap(Map<String, Double> map)
     {
        String s = AmapTools.toString(map).replace('\n', ' ');
        s = s.replace("  ", " ");
        return s;
    }

    
    



    /**
     * toString () method.
     */
    public String toString() {
        // nb-13.08.2018
        //return "class=" + getClass().getName() + " name=" + NAME + " constructionCompleted=" + constructionCompleted
        return "class=" + getClass().getName() + " name=" + getName() + " constructionCompleted=" + constructionCompleted
                + " stand=" + scene + " param_minCutG =" + param_minCutG + " param_type =" + param_type
                + " param_finalG =" + param_finalG + " param_expectedMixComposition: " + param_expectedMixComposition;
    }

    

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
        public Tree getTree() {
            return t;
        }
        @Override
       public int compareTo(Object scoredTree) {
           double score1 = this.getScore();
           double score2 = ((ScoredTree) scoredTree).getScore();
           return Double.compare(score1, score2);
   }

    }

}
