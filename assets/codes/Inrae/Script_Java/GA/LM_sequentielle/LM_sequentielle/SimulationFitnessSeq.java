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
 import java.lang.management.ManagementFactory;
 import java.lang.management.MemoryMXBean;
 import java.lang.management.MemoryUsage;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.ObjectOutputStream;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.StringTokenizer;
 import java.util.stream.Collectors;


import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
import java.util.HashSet;
import java.lang.Math;
 import jeeb.lib.util.AdditionMap;
 import jeeb.lib.util.Check;
 import capsis.app.C4Script;
 import capsis.kernel.GModel;
 import capsis.kernel.GScene;
 import capsis.kernel.Step;
 import forceps.extension.intervener.CepsGHAThinner2;
 import forceps.extension.intervener.CepsGHAThinner2bis;
 import forceps.extension.intervener.CepsGHAthinnerCompostion;
 import forceps.extension.ioformat.CepsExport;
 import forceps.extension.ioformat.CepsMeanStandExport;
 import forceps.extension.ioformat.CepsProductivityScene;
 import forceps.model.CepsEvolutionParameters;
 import forceps.model.CepsInitialParameters;
 import forceps.model.CepsPatch;
 import forceps.model.CepsScene;
 import forceps.model.CepsTree;
 import forceps.model.CepsDeadWood;
 import org.apache.commons.math3.stat.StatUtils;
 import java.text.DecimalFormat;
 
 /**
  * Forceps simulation to calculate the objective function of the genetic algorithm.
  *
  * @author M. Malara - July 2023 & M. Jourdan - March 2024 & TALEB 2024
  */
 public class SimulationFitnessSeq {
 
    public String working_dir; 	// All files are read in the same directory than the commanFile
    private String output_dir;  // name of the directory where the results will be saved
    private String setup_file;
    private String site_file;
    private String climate_file;
    private String inventory_file;
    private String potential_species;
    private String manage_species;//les 5 espèces à ajouter dans le génome de la simulation add by loubna 
    public String Combinaisons_species; //le fichier des combinaisons pour les espèces.
    private boolean risk_aversion;
    private double basal_area_min;
    private double basal_area_max;
    private double gini_min;
    private double gini_max;
    private double m_rate_max;
    private int nb_spe_min;
    private int nb_spe_max;
    private double v_prod_min;
    private double v_prod_max;
    //Enrigsitrer la seed 
    private int RandomSeed;
   //Le switch pour la simulation 
	 public int Simulation_Non_viable_fin;

    //ça sera mieux de creer un ditionnaire ou t'a tous les espèces avec son identifiant :
    public Map<Integer,String> speciesMapId;
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
 
 
   /**
    * Constructor:
    */
   public SimulationFitnessSeq(String working_dir, String output_dir, String setup_file,
                            String site_file, String climate_file, String inventory_file,
                            String potential_species,String manage_species,boolean risk_aversion,
                            double basal_area_min, double basal_area_max,
                            double gini_min, double gini_max, double m_rate_max,
                            int nb_spe_min, int nb_spe_max, double v_prod_min, double v_prod_max,String Combinaisons_species,int RandomSeed,int Simulation_Non_viable_fin) {
     // directory use
     this.working_dir = working_dir;
     this.output_dir = output_dir;
     // file for forceeps simulation
     this.setup_file = setup_file;
     this.site_file = site_file;
     this.climate_file = climate_file;
     this.inventory_file = inventory_file;
     this.setup_file = setup_file;
     this.Combinaisons_species=Combinaisons_species;
     //paramètres pour les espèces
     this.potential_species=potential_species;
     this.manage_species= manage_species;//change

     
    
     // risk aversion (false )
     this.risk_aversion = risk_aversion;
     // constraints
     this.basal_area_min = basal_area_min;
     this.basal_area_max = basal_area_max;
     this.gini_min = gini_min;
     this.gini_max = gini_max;
     this.m_rate_max = m_rate_max;
     this.nb_spe_min = nb_spe_min;
     this.nb_spe_max = nb_spe_max;
     this.v_prod_min = v_prod_min;
     this.v_prod_max = v_prod_max;
     // random seed
     this.RandomSeed = RandomSeed;
    
     //Le switch pour la simulation 
	  this.Simulation_Non_viable_fin=Simulation_Non_viable_fin;
     setSpeciesMapId();
   }

  
 
 
   /* Printer */
   @Override
   public String toString(){
     String res = "# basa area constraints = ["+this.basal_area_min+", "+this.basal_area_max+"] m2/ha\n"
                + "# gini constraints = ["+this.gini_min+", "+this.gini_max+"]\n"
                + "# number of species constraints = ["+this.nb_spe_min+", "+this.nb_spe_max+"]\n"
                + "# production constraints = ["+this.v_prod_min+", "+this.v_prod_max+"]m3/ha/year\n"
                + "# mortality rate constraints < "+this.m_rate_max+"\n"+
                "#Managed species :"+manage_species
                +"#Simulation_Non_viable_fin:"+Simulation_Non_viable_fin;
     return(res);
   }
 
 
   public boolean getRiskAversion(){
     return this.risk_aversion;
   }
 
 
   private String genomeString(List<Double> genome){
     /** convert genome (lis<Double>) on String
   return interest score String
   */
       String res = "";
       for(double g: genome){
         res += Double.toString(g) + ", ";
       }
       return res;
   }
 
 
   public double calcFitness(List<Double> genome, int id, int inter_max, boolean all_exit) throws Exception {
     /** do simulation to calculate interest score either with
         the risk aversion version or with the average version
     return interest score
     */
     if(this.risk_aversion){
      //avec la méthode : on sort de l'espace  d'exploration si au moins une contrainte sur un seul patch
      //n'est pas respecté
       return calcFitnessRiskAversion(genome, id, inter_max, all_exit);}
     else{
      //On sort de l'espace d'exploration si la contrainte n'est pas respecté en moyenne sur tous les patchs.
       return calcFitnessAverage(genome, id, inter_max, all_exit);}
   }

  ///Pour calculer la memoire de la simulation
  private static long getUsedMemory() {
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
    return heapMemoryUsage.getUsed();
  }

  //Retourner la seed 
  public int getSeed(){
    return this.RandomSeed;
    }
   
  //*******************************Commencer la simulation pour la version risk*************************************************** */
   private double calcFitnessRiskAversion(List<Double> genome, int id, int inter_max, boolean all_exit) throws Exception{
     /** do simulation to calculate interest score with risk aversion version
     return interest score
     */
     // Calculate constraints
     
     int nb_tree ;
     double constraint;
       // basal area
     double total_g;
     double basal_area = 0.0d;
       // gini
     double total_d;
     double diff_d;
     double year_gini;
     double periode_gini;
     List<Double> list_gini = new ArrayList<Double>();
       // mortality rate
     int nb_dead;
     double dead_g;
     double year_m_rate;
     double periode_m_rate;
     List<Double> list_m_rate = new ArrayList<Double>();
       // nb species
     List<Integer> species_list = new ArrayList<Integer>();
     int species_list_size;
     int id_spe;
     int year_nb_spe;
     double periode_nb_spe;
     List<Double> list_nb_spe = new ArrayList<Double>();
 
       // production
     double v_prod = 0.0d;
     List<Double> list_v_prod = new ArrayList<Double>();
     // Do intervention
     double theta;
     double tp; // [0, 1]
     double g_obj;
     //pour adapter aux paramétres d'inventaire
     String G_obj;
     Map<String,Double> C_esp=new HashMap<String,Double>();
     Map<String,Double> C_esp_init=new HashMap<String,Double>();
     Map<String,Double> C_esp_f=new HashMap<String,Double>();
     String param_expectedMixComposition;
     //calculer la surface initiale :
     
     
     //exemple dinventaire  avec la composition :5-0.5-0.9-PSYL_20-FSyl_30-CBet_20-QPet_20-TCor_0    // Calculate fitness
     double res = 0.0d;
 
     try{
       //// Open file for results
       String fileName = this.output_dir+"/individu"+id+"_evolConstraints.txt";
       File file = new File(fileName);
       if (!file.getParentFile().exists())
         file.getParentFile().mkdirs();
       if (!file.exists())
         file.createNewFile();
       FileWriter fw = new FileWriter(file);
       System.out.println("# indidual "+id);
       fw.write("# indidual " + id + " genome : " + this.genomeString(genome) +
                "\n#year	idPatch	nbTree	basalAreaTot	nbDeadTree	basalAreaDaed	" +
                "mortalityRate	coefGini	nbOfSpecies	production\n");
 
       
       //// Start forceeps in capsis
       C4Script s = new C4Script("forceps");
       CepsInitialParameters i = new CepsInitialParameters(this.working_dir + "/"
                                                         + this.setup_file);
       i.loadSetupFile();
       i.siteFileName = i.addSetupDirectory(this.site_file);
       i.defaultClimateFileName = i.addSetupDirectory(this.climate_file);
       i.inventoryFileName = i.addSetupDirectory(this.inventory_file);
       //add  :lire les espèces en formats string species 
      
       
       List<Integer> Id_species_Cf=i.convertSpeciesShortNamesToIds(this.manage_species);//enregesitrer les k premiers especes 
       this.potential_species=Id_species_Cf.stream()
                      .map(String::valueOf)  // Converts each integer to a string
                      .collect(Collectors.joining(" "));  // Joins them with space as a delimiter
       i.setPotentialSpeciesList(this.potential_species);
       
       s.init(i);
 
       // set variables
       Step step = s.getRoot();
       CepsScene scene = (CepsScene)step.getScene();
       int nb_patch = scene.getPatches().size(); // i.inventoryPatchN;
       double patch_area = scene.getPatches().get(1).getArea();
      for(int k=0; k<nb_patch; ++k){
         list_v_prod.add(0.0d);
         list_gini.add(0.0d);
         list_m_rate.add(0.0d);
         list_nb_spe.add(0.0d);
       }
 
       // calc Cst
       double basal_area_expect = this.basal_area_min * patch_area / 10000.0d;
       double basal_area_var = (this.basal_area_max-this.basal_area_min) * patch_area / 10000.0d;
       double v_prod_expect = this.v_prod_min * (patch_area/10000.0d) * inter_max*5.0d;
       double v_prod_var = (this.v_prod_max - this.v_prod_min) * (patch_area/10000.0d) * inter_max*5.0d;
       
       // Evolution  Change by loubna - - - - -  - - - - - - - - - - - - - - - - - - - - - - - -
       for (int j = 0; j < inter_max; ++j) {
         double iniArea=0.0d;
         theta = genome.get(j*8); // years changer 3 en 8 pour le genome de 128 variables
         // set constraints to zero
         periode_m_rate = 0.0d;
         periode_gini = 0.0d;
         periode_nb_spe = 0.0d;
         tp = genome.get(j*8+1); // [0, 1]
         g_obj = genome.get(j*8+2); // m2/ha OR %
         List<Double> percentage_Cf=new ArrayList<>();
         //add by loubna 
         //il faut transformer G_obj en % 
         Double G=g_obj*100;
         G_obj=String.valueOf(G);
         //ajouter à G_obj %
         G_obj+="%";
         for (int k=3;k<8;k++){
           percentage_Cf.add(genome.get(j*8+k));
         }
         //Recuprer les noms et placer dans parmetre_expectedmix;
         String[] splitShortNames = manage_species.split(" ");
         StringBuilder sb = new StringBuilder();
         for (int k = 0; k < splitShortNames.length; k++) {
             if (k > 0) {
                 sb.append(", ");
             }
             sb.append(splitShortNames[k]);
             sb.append("-");
             sb.append(Math.round(percentage_Cf.get(k)));  
             //C_esp_f.put(splitShortNames[k],Math.round(percentage_Cf.get(k)) );
         }
         param_expectedMixComposition=sb.toString();
         //
         for(int t = 0; t < 5; ++t){ // - - - - -  - - - - - - - - - - - -
           // run simulation during the period of 5 year, year by year
           step = s.evolve(new CepsEvolutionParameters(1));
           // calcul constraints each years
           scene = (CepsScene)step.getScene();
           year_gini = 0.0d;
           year_m_rate = 0.0d;
           year_nb_spe = 0;
           basal_area = 0.0d;
 
           for(CepsPatch patch : scene.getPatches()) { // - - - - -  - - - - -
             // calculate for 30 patch
             nb_tree = 0;
             nb_dead = 0;
             total_g = 0.0d;
             total_d = 0.0d;
             diff_d = 0.0d;
             dead_g = 0.0d;
             species_list.clear();
             species_list_size = 0;
             List<CepsTree> deadTrees = scene.getDeadTrees();
             List<Integer> idTreesDead = new ArrayList<>();
             for (CepsTree deadTree : deadTrees) {
                 idTreesDead.add(deadTree.getId());
             }
             for(CepsTree tree : patch.getTrees()) { // - - - - - - - - - - - -
               // eatch tree
               nb_tree += 1;
                 // basal area
               total_g += tree.getBasalArea();
                 // gini
              int val=tree.getTreeIdInPatch();
               total_d += tree.getDbh();
               dead_g+=1;
               nb_dead+=1;
               for(CepsTree othertree : patch.getTrees()){
                 diff_d += Math.abs(tree.getDbh() - othertree.getDbh());
               }
                 // nb species
               id_spe = tree.getSpecies().getValue();
               if(!species_list.contains(id_spe)){
                 species_list.add(id_spe);
                 species_list_size += 1;
               }
               
             } // - - - - - - - - - - - - - - - - - - - - - - - - - - - end trees
               // mortality rate
             if (scene.getDeadTrees() != null) {
               for (CepsTree deadtree : scene.getDeadTrees()) {
                 if (deadtree.getPatchId() == patch.getId()){
                   dead_g += deadtree.getBasalArea();
                   nb_dead += 1;
                 }
               }
             }
             // calculate deadTree
             
             // save results to a file
             fw.write(scene.getDate() + "	" + patch.getId() + "	" + nb_tree + "	" +
                      total_g + "	" + nb_dead + "	" + dead_g + "	" + nb_dead*1.0d/(nb_tree+nb_dead)
                     + "	" + diff_d/(2*nb_tree*total_d) + "	" +
                     species_list_size + "	" + v_prod + "\n");
 
             // test basal area constraint in any year and any patch
             if(total_g < basal_area_expect){
               String message = "# individual " + id +" for rotation " + j +
                               " in a patch " + patch.getId() + ": this basal "
                               + "area is out of constraints " + total_g;
               System.out.println(message);
               fw.write(message);
               fw.close();
               s.closeProject();
               return 0.0d;
             }
 
             // save calcul values
             basal_area += total_g;
             year_gini += diff_d/(2*nb_tree*total_d);
             constraint = diff_d/(2*nb_tree*total_d) + list_gini.get(patch.getId());
             list_gini.set(patch.getId(), constraint);
             year_m_rate += nb_dead*1.0d/(nb_tree+nb_dead);
             constraint = nb_dead*1.0d/(nb_tree+nb_dead) + list_m_rate.get(patch.getId());
             list_m_rate.set(patch.getId(), constraint);
             year_nb_spe += species_list_size;
             constraint = species_list_size + list_nb_spe.get(patch.getId());
             list_nb_spe.set(patch.getId(), constraint);
           } // - - - - - - - - - - - - - - - - - - - - - - - - - - -end 30 patch
 
           // average on patches
           basal_area /= nb_patch;
           periode_gini += year_gini / nb_patch ;
           periode_m_rate += year_m_rate / nb_patch;
           periode_nb_spe += year_nb_spe / nb_patch;
           // add to result the distance to contraint for basal area
                                                         // periode * (max - min)
           res += Math.min(1.0d,
                  (basal_area - basal_area_expect) / (5.0d * basal_area_var));
         } // - - - - - - - - - - - - - - - - - - - - - - - - - - - - end periode
 
         // average on periode
         periode_m_rate /= 5.0d;
         periode_gini /= 5.0d;
         periode_nb_spe /= 5.0d;
 
         // test other constraints over the period
         if(Collections.min(list_m_rate)/(nb_patch*5.0d) > this.m_rate_max){
           String message = "# individual " + id +" for rotation " + j +
            ": this mortality rate is out of constraints for at least one patch";
           System.out.println(message);
           fw.write(message);
           fw.close();
           s.closeProject();
           return 0.0d;
         }
         else if(Collections.min(list_gini)/(nb_patch*5.0d) < this.gini_min ||
                 Collections.max(list_gini)/(nb_patch*5.0d) > this.gini_max){
           String message = "# individual " + id +" for rotation " + j +
            ": this gini coefficient is out of constraints for at least one patch";
           System.out.println(message);
           fw.write(message);
           fw.close();
           s.closeProject();
           return 0.0d;
         }
         else if(Collections.min(list_nb_spe)/(nb_patch*5.0d) < this.nb_spe_min){
           String message = "# individual " + id +" for rotation " + j +
            ": numbur of species is out of constraints for at least one patch";
           System.out.println(message);
           fw.write(message);
           fw.close();
           s.closeProject();
           return 0.0d;
         }
 
         // add to result the distance to contraints
         res += Math.min(1.0d,
                (this.m_rate_max - periode_m_rate ) / this.m_rate_max);
         res += Math.min(1.0d,
                Math.abs(periode_gini - ((this.gini_max + this.gini_min)/2.0d)) / (this.gini_max - this.gini_min));
         res += Math.min(1.0d,
                (periode_nb_spe - this.nb_spe_min) / (this.nb_spe_max - this.nb_spe_min));
 
         if(theta > 0){
           // do cutting intervention
           //CepsGHAThinner2bis thinner = new CepsGHAThinner2bis(tp, g_obj);
           CepsGHAthinnerCompostion thinner_Compostion=new CepsGHAthinnerCompostion(tp,G_obj,param_expectedMixComposition);
           //CepsGHAThinner2 thinner_2=new CepsGHAThinner2(3,tp,G_obj,param_expectedMixComposition);
           step = s.runIntervener(thinner_Compostion, step);
           // save the production
           scene = (CepsScene)step.getScene();
           if (scene.getCutTrees() != null) {
             for (CepsTree cuttree : scene.getCutTrees()) {
               constraint = cuttree.getVolume() + list_v_prod.get(cuttree.getPatchId());
               list_v_prod.set(cuttree.getPatchId(), constraint);
               v_prod += cuttree.getVolume();
             }
             
           }
         }
       } //// - - - - - - - - - - - - - - - - - - - - - - - - - - - End evolution
 
       // test production constraint
       if(Collections.min(list_v_prod)< v_prod_expect){
         String message = "# individual " + id + ": this production is out of "
                           + "constraints " + v_prod_expect
                           + " for patch for at least one patch";
         System.out.println(message);
         fw.write(message);
         fw.close();
         s.closeProject();
         return 0.0d;
       }
       // add to result the distance to contraints for production
       res /= inter_max; // in [0,4]
       res += Math.min(1.0d,
              (v_prod - v_prod_expect) / ( v_prod_var * 5.0d * nb_patch));
 
       // save data in files
       String message = "# indidual "+id+", fitness "+res;
       System.out.println(message);
       fw.write(message);
       fw.close();
       if(all_exit){
         // Mean stand export -> mean.txt
         CepsMeanStandExport e1 = new CepsMeanStandExport();
         e1.initExport(s.getModel(), step);
         e1.save(this.output_dir+"/individu"+id+"_mean.txt");
         // Complete export -> complete.txt
         CepsExport e2 = new CepsExport();
         e2.initExport(s.getModel(), step, 1, 1);
         e2.save(this.output_dir+"/individu"+id+"_complete.txt");
         // ProductivityScene export -> productivityScene.txt
         CepsProductivityScene e = new CepsProductivityScene();
         e.initExport(s.getModel(), step, 1, 1);
         e.save(this.output_dir+"/individu"+id+"_productivityScene.txt");
       }
       s.closeProject();
     }
     catch (Exception e) {
       throw new Exception ("Wrong format in simulation "+id);
       }
     return res;
   }
 
   /********************************La version moyenne*********************************/
   private double calcFitnessAverage(List<Double> genome, int id, int inter_max, boolean all_exit) throws Exception{
     /** do simulation to calculate interest score either with average version
     return interest fitness
     */
     
     //calculer le temps d'excution :
     long startTime, endTime;
     startTime=System.nanoTime();
     //nombre des arbres
     int nb_tree ;

    // basal area
     double total_g;
     double basal_area = 0.0d;
     double periode_Basal_area;

    // gini
     double total_d;
     double diff_d;
     double year_gini;
     double periode_gini;
      // mortality rate
     int nb_dead;
     double dead_g;
     double year_m_rate;
     double periode_m_rate;

      // nb species
     List<Integer> species_list = new ArrayList<Integer>();
     int species_list_size;
     int id_spe;
     int year_nb_spe;
     double periode_nb_spe;
     String speciesName;

    // production
    double v_prod = 0.0d;
    
    //  intervention
     double theta;//[5,10,15,20]
     double tp; // [0, ,0.5,1]
     double g_obj; //[0.5,0.6,0.7,0.8,0.9]
     String G_obj; //transformer g_obj en % : 50%,60%,70%,80%,90%

     //Pour recuperer les valeurs dans le genome.
     Map<String,Double> C_esp=new HashMap<String,Double>(); //pour enregistrer le pourcentage pour chaque espèce :{PSYL:60%,CBET:40%}
     String param_expectedMixComposition; //pour ajouter la composition des espèces sous forme :PSyl-60%;CBet-40%

      // Calculate fitness la fonction objective que tu cherche à à respecter dans GA.
      double res = 0.0d;
      //Tester sur 5 ans Si on sort de l'espace des contraintes
      boolean Viable_BA=true;
      Boolean Viable_m_rate=true;
      Boolean Viable_gini=true;
      Boolean Viable_nb_spe=true;
      Boolean Viable_V_prod=true;
    //Enregistrer les non viables dans un texte à la fin.
    String Message_Non_viable="#Individual :"+id+ " is out of Constraints because of \n";
    //le nombre des espèces dans chaque séquence du génome.
     int Nb_esp = manage_species.split(" ").length;
    try{
       //***************************************Initialiser le fichier des Contraintes****************************************** */
       String fileName = this.output_dir+"/individu"+id+"_evolConstraints.txt";
      //Création des fichiers : chaque individus viable et non viable .
       File file = new File(fileName);
       if (!file.getParentFile().exists())
         file.getParentFile().mkdirs();
       if (!file.exists())
         file.createNewFile();
       FileWriter fw = new FileWriter(file);
       System.out.println("# indidual "+id);
        String[] split = manage_species.split(" ");
       // Writing header information to file
       fw.write("# individual " + id + " genome : " + this.genomeString(genome) + "\n#year\tidPatch\tnbTree_ha\tbasalAreaTot_ha\t");
       
      fw.write("basalAreaDead_ha\tmortalityRate\tcoefGini\tnbOfSpecies\tvol_prod_ha\n");
      
       //// ***********************************************************Start forceeps in capsis
       C4Script s = new C4Script("forceps");
       CepsInitialParameters i = new CepsInitialParameters(this.working_dir + "/"
                                                         + this.setup_file);
       i.loadSetupFile();
       i.siteFileName = i.addSetupDirectory(this.site_file);
       i.defaultClimateFileName = i.addSetupDirectory(this.climate_file);
       i.inventoryFileName = i.addSetupDirectory(this.inventory_file);
       
       //Récuperer les Ids des premiers espèces 
       List<Integer> Id_species_Cf=i.convertSpeciesShortNamesToIds(this.manage_species);
      i.setPotentialSpeciesList(this.potential_species);
      s.init(i);
      
       /*********************************************************Evol Etat ************************************************************** */
       String fileEtat = this.output_dir+"/individu"+id+"_evolEtat.txt";
       File fileEtat1 = new File(fileEtat);
       if (!fileEtat1.getParentFile().exists())
       fileEtat1.getParentFile().mkdirs();
       if (!fileEtat1.exists())
       fileEtat1.createNewFile();
       FileWriter fwEtat = new FileWriter(fileEtat);
       fwEtat.write("#Individual "+id+" genome :"+this.genomeString(genome)+"\n");
       //Ecrire les caracteristique pour les espèces manged_species et les especes qui peut integrer la simulation
        fwEtat.write("#Managed species :");
        for (int j=0;j<split.length;j++) {
            fwEtat.write("("+split[j]+","+Id_species_Cf.get(j)+')'+'\t');
        }
       fwEtat.write("\n#Non-managed species:");
       //Il faut recuperer l'id et son nom mais les espèces qui n'existe pas dans Id_species_Cf
       String[] tous_species=potential_species.split(" ");
       for (String k :tous_species){
        //transformer k en entier
        int k1=Integer.parseInt(k);
        if (!Id_species_Cf.contains(k1)){
          //Il faut qu'on recupère le shortname de l'Id k
          String shortname = this.speciesMapId.get(k1);
          fwEtat.write("("+shortname+","+k1+')'+'\t');
          
       }
      }
      //Ajouter année ,patchId,species,Nb_tree_h,Basal_area,Dimaètre_moyen,Diametre_std,Hauteur_moy,Hauteur_std
      //Indice de sechresse :droutght_Index_moy , lightavaibility_moy
      fwEtat.write("\n#year"+"\t"+"patchId"+"\t"+"specie"+"\t"+"Nb_tree_ha"+"\t"+"Basal_area_ha"+"\t"+"Diameter_mean_ha"+"\t"+"Diameter_std_ha"+"\t"+"Height_max_ha"+"\t"+"Height_mean_ha"+"\t"+"Height_std_ha"+"\t"+"droughtIndex_mean"+"\t"+"light_Avaibility_mean"+"\t"+"Gini"+"\n");
      
      //************************************************************Initialiser la scene ***************/
       Step step = s.getRoot();
       CepsScene scene = (CepsScene)step.getScene();
       int nb_patch = scene.getPatches().size(); // i.inventoryPatchN;
       double patch_area = scene.getPatches().get(1).getArea();
      //Creer un dictionnaire ou chaque cle :pacth attribuer les arbres qui constite
      //dictionnaire pour les arbres qui sont coupée dans le patch i 
       Map<Integer,Double> dic_Vol_cut_Patch=new HashMap<>();
       //Inialiser les volumes dans les patchs à 0 :
       for (CepsPatch patch :scene.getPatches()){

           dic_Vol_cut_Patch.put(patch.getId(),0.0d);

        }
       
       // calc Constante pour la surface térrière minimale, bois récolté minimale,
       double basal_area_expect = this.basal_area_min*(patch_area/10000) ;
       double basal_area_var = (this.basal_area_max-this.basal_area_min)*(patch_area/10000) ;
       double v_prod_expect = this.v_prod_min  * inter_max*5.0d *(patch_area/10000);
       double v_prod_var = (this.v_prod_max - this.v_prod_min)  * inter_max*5.0d*(patch_area/10000);
      
        //// Evolution ------------------------------------- - - - - -  - - - - - - - - - - - - - - - - - - - - - - - -
       for (int j = 0; j < inter_max; ++j) {
         
         scene = (CepsScene)step.getScene();

        // set constraints to zero
        periode_m_rate = 0.0d;
        periode_gini = 0.0d;
        periode_nb_spe = 0.0d;
        periode_Basal_area=0.0d;
        //Recuperer les valeurs pour une séquence du génome 
        // periode [5,10,15,20]
        theta = genome.get(j*(3+Nb_esp)); 
        //le type d'éclairci  // [0, 1]
         tp = genome.get(j*(3+Nb_esp)+1); 
         //Surface terrière  // en proportion 
         g_obj = genome.get(j*(3+Nb_esp)+2);
         //transformer en %
          Double G=g_obj*100;
         //transformer  g_OBJ en String
         G_obj=String.valueOf(G); 
         //ajouter à G_obj %
         G_obj+="%";
         //******COmposition des espèces 

         List<Double> percentage_Cf=new ArrayList<>();
         //Enregistrer les pourcentages 
         for (int k=3;k<(3+Nb_esp);k++){
           percentage_Cf.add(genome.get(j*(3+Nb_esp)+k));
         }
         //Recuprer les noms et placer dans param_expectedMixComposition
         String[] splitShortNames = manage_species.split(" ");
         StringBuilder sb = new StringBuilder();
         //Remplir la composition pour couper la forets 
         for (int k = 0; k < splitShortNames.length; k++) {
             if (k > 0) {
                 sb.append(", ");
             }
             sb.append(splitShortNames[k]);
             sb.append("-");
             sb.append(Math.round(percentage_Cf.get(k)));  
            
         }
         param_expectedMixComposition=sb.toString();
        //***********fin composition espèces */
         
         for(int t = 0; t < 5; ++t){ // - - - - -  - - - - - - - - - - - -Ecolution sur 5 ans *************************************
          

           // run simulation during the period of 5 year, year by year
           step = s.evolve(new CepsEvolutionParameters(1));
           // calcul constraints each years
           scene = (CepsScene)step.getScene();
           year_gini = 0.0d;
           year_m_rate = 0.0d;
           year_nb_spe = 0;
           basal_area = 0.0d;
          
          for(CepsPatch patch : scene.getPatches()) { // - - - - -  - - - - -start for 10 patch
            //************************************************Evol Etat**************************************************************** */
            //Remplir par rapport au totale des indivdius 
            //recuperer les arbres de la foret
            List<CepsTree> Tree_patch=patch.getTrees();
            //Ecrire la date, le patch , le size, la surface terriere par ha
            fwEtat.write(scene.getDate() + "\t" + patch.getId() + "\t" +"total"+"\t");
            fwEtat.write(Tree_patch.size()*(10000/patch_area)+"\t"+getGha(Tree_patch)+"\t");
            //Enregistrer le diametre , la hauteur et la lumière disponible et l'indice de sechresse .
           
            Double Light_mean_total=0.0d;
            Double DroughtIndex_mean_total=0.0d;
            //ecart type pour le diametre et hauteur
            double[] Diamter_total = new double[Tree_patch.size()];
            double[] Height_total = new double[Tree_patch.size()];
            //Indice de Gini annuelle:
            Double deno_Gini=0.0d;
            Double Nomi_Gini=0.0d;
            for(CepsTree tree : Tree_patch) {
              
              Light_mean_total+=tree.getLightAvailability();
              DroughtIndex_mean_total+=tree.getTreeDroughtIndex();
              Diamter_total[Tree_patch.indexOf(tree)]=tree.getDbh()*(10000/patch_area);
              Height_total[Tree_patch.indexOf(tree)]=tree.getHeight()*(10000/patch_area);
              //Gini
              deno_Gini+=tree.getDbh();
              for (CepsTree OtherTree :patch.getTrees()){
                Nomi_Gini+=Math.abs(tree.getDbh()-OtherTree.getDbh());
              }
            }
            //Ecrire dans le fichier 
            fwEtat.write(StatUtils.mean(Diamter_total)+"\t"+Math.sqrt(StatUtils.variance(Diamter_total))+"\t"+StatUtils.max(Height_total)+"\t");
            fwEtat.write(StatUtils.mean(Height_total)+"\t"+Math.sqrt(StatUtils.variance(Height_total))+"\t"+DroughtIndex_mean_total/Tree_patch.size()+"\t");
            fwEtat.write(Light_mean_total/Tree_patch.size()+"\t");
            fwEtat.write(Nomi_Gini/(2*Tree_patch.size()*deno_Gini)+"\n");
            for (String id_sp:tous_species){
                //**********************************************enregistrer year ,patch and species
                //recuperer l'espèce
                int k1=Integer.parseInt(id_sp);
                String name_sp=this.speciesMapId.get(k1);
                fwEtat.write(scene.getDate() + "\t" + patch.getId() + "\t" +name_sp+"\t");
                //******************************************************************recuperer l'etat de l'espèce
                //Calculer le nombre  des arbres, surface terrière,diametrer_moyen _ecart_type, hauteur_moyenn indice de sechresse, light pour chaque espèce
                List<CepsTree> Name_trees=getTrees(patch.getTrees(), name_sp);

                //enregistrer le nombre d'arbres getGha c'est deja par ha
                fwEtat.write(Name_trees.size()*(10000/patch_area)+"\t"+getGha(Name_trees)+"\t");
                //Enrigistrer la valeur moyen pour le diamètre et son ecart type , et aussi 
               
                double[] Diamter_ = new double[Name_trees.size()];
                //HAuteur de l'arbre 
                
                double[] Hauteur_ = new double[Name_trees.size()];
                //Indice de sechresse 
                Double drouht_Index=0.0d;
                //lumiere disponible 
                Double Light_avaibility=0.0d;
                 //Indice de Gini annuelle:
                Double deno_Gini_=0.0d;
                Double Nomi_Gini_=0.0d;
                //Si t'a pas de valeurs
                if (Name_trees.size()==0){
                  fwEtat.write(0+"\t"+0+"\t"+0+"\t"+0+"\t"+0+"\t"+0+"\t"+0+"\t"+0+"\n");
                }
                else {
                for (CepsTree tree: Name_trees){
                  
                  Diamter_[Name_trees.indexOf(tree)]=tree.getDbh()*(10000/patch_area);
                  
                  Hauteur_[Name_trees.indexOf(tree)]=tree.getHeight()*(10000/patch_area);
                  drouht_Index+=tree.getTreeDroughtIndex();
                  Light_avaibility+=tree.getLightAvailability();
                 //Gini
                  deno_Gini_+=tree.getDbh();
                  for (CepsTree OtherTree :Name_trees){
                    Nomi_Gini_+=Math.abs(tree.getDbh()-OtherTree.getDbh());
                  }
                }
                //ecrire dans le fichier d'evolution d'état
                fwEtat.write(StatUtils.mean(Diamter_)+"\t"+Math.sqrt(StatUtils.variance(Diamter_))+"\t"+StatUtils.max(Hauteur_)+"\t");
                fwEtat.write(StatUtils.mean(Hauteur_)+"\t"+Math.sqrt(StatUtils.variance(Hauteur_))+"\t");
                fwEtat.write(drouht_Index/Name_trees.size()+"\t");
                fwEtat.write(Light_avaibility/Name_trees.size()+"\t");
                fwEtat.write(Nomi_Gini_/(deno_Gini_*Name_trees.size()*2)+"\n");
                
              }
            }
            //******************************************************************Fin varaition Etat******************************* */

            /**************************************************Variation constraintes ***************************************** */
             // Initialiser les paramètres 
             nb_tree = 0;
             nb_dead = 0;
             double nb_dead_tree=0;
             total_g = 0.0d;
             total_d = 0.0d;
             diff_d = 0.0d;
             dead_g = 0.0d;
             species_list.clear();
             species_list_size = 0;
             for(CepsTree tree : patch.getTrees()) { // - - - - - - - - - - - -tree-------------------------------------------------
                 // each tree
               nb_tree += 1;
               // basal area
               total_g += tree.getBasalArea();
                // gini :le denominateur 
               total_d += tree.getDbh();
               //calcul du nominateur 
               for(CepsTree othertree : patch.getTrees()){
                 diff_d += Math.abs(tree.getDbh() - othertree.getDbh());
               }
               // nb species
              id_spe = tree.getSpecies().getValue();
               if(!species_list.contains(id_spe)){
                 species_list.add(id_spe);
                 species_list_size += 1;
               }
               //ajouter les arbres mortes

                          
            } // - - - - - - - - - - - - - - - - - - - - - - - - - - - end trees
             //********************************Mortality**************************************************** */
             if (scene.getDeadSlowGrowthTrees() != null ) {
              for (CepsTree deadtree : scene.getDeadSlowGrowthTrees()) {
                if (deadtree.getPatchId() == patch.getId()){
                        dead_g += deadtree.getBasalArea();
                        nb_dead+= 1;
                      }
              
            }
            }
           //Bois couper sur le patch :
             //Enregistrer les valeurs pour chaque l'ensemble des patchs 
             basal_area += total_g;
             year_gini += diff_d/(2*nb_tree*total_d);
             year_m_rate += nb_dead*1.0d/(nb_tree+nb_dead);
             year_nb_spe += species_list_size;
              // save results to a file
             fw.write(scene.getDate() + "\t" + patch.getId() + "\t" + nb_tree*(10000/patch_area) + "\t" +total_g*(10000/patch_area) +"\t");
            fw.write( dead_g*(10000/patch_area)+ "\t" + nb_dead*1.0d/(nb_tree+nb_dead)
             + "\t" + diff_d/(2*nb_tree*total_d) + "\t" +
             species_list_size + "\t" + dic_Vol_cut_Patch.get(patch.getId())*(10000d/patch_area) + "\n");

        } // - - - - - - - - - - - - - - - - - - - - - - - - - - -end 10 patch
           // Calculer la moyenne pour l'ensemble des patchs
           periode_Basal_area +=basal_area / nb_patch;
           periode_gini += year_gini / nb_patch ;
           periode_m_rate += year_m_rate / nb_patch;
           periode_nb_spe += year_nb_spe / nb_patch;
 
        } // - - - - - - - - - - - - - - - - - - - - - - - - - - - -------------------------------------------- end periode
         // La moyenne sur 5ans  pour teta , Gini et nb_spe
         periode_Basal_area/=5.0d;
         periode_m_rate /= 5.0d;
         periode_gini /= 5.0d;
         periode_nb_spe /= 5.0d;
         //Verifier pour la surface terrière
         
         if(periode_Basal_area < basal_area_expect)
         {
            Viable_BA=false;
            String message = "# individual " + id +" for rotation " + j +
             ": this basal area is out of constraints " + periode_Basal_area*(10000/patch_area)  +"minBasal:"+basal_area_expect *(10000/patch_area) ;
            System.out.println(message);
            if(Simulation_Non_viable_fin==1){
             res=0.0d;
             File nonViableFileEtat=new File(this.output_dir + "/individu" + id + "_evolEtat_nonViable.txt");
            File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
            file.renameTo(nonViableFile);
            fileEtat1.renameTo(nonViableFileEtat);
            Message_Non_viable+="At year "+scene.getDate()+" BA : "+periode_Basal_area *(10000/patch_area)+" < "+basal_area_expect *(10000/patch_area)+"\n";


            }else {
            
            fwEtat.write(message);
            fwEtat.close();
            fw.write(message);
            fw.close();
            s.closeProject();
            //Renomer en Non_viables:
            File nonViableFileEtat=new File(this.output_dir + "/individu" + id + "_evolEtat_nonViable.txt");
            File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
            file.renameTo(nonViableFile);
            fileEtat1.renameTo(nonViableFileEtat);
            return 0.0d;
             }
          }
          //Taux de mortalité
          else if (periode_m_rate > this.m_rate_max){
          //Ne pas fermer si Simulation_Non_viable_fin==1
          String message = "# individual " + id +" for rotation " + j +
            ": this mortality rate" + periode_m_rate + "is out of constraints " + m_rate_max;
           System.out.println(message);
          Viable_m_rate=false;
          if(Simulation_Non_viable_fin==1){
            res=0.0d;
            File nonViableFileEtat=new File(this.output_dir + "/individu" + id + "_evolEtat_nonViable.txt");
            File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
            file.renameTo(nonViableFile);
            fileEtat1.renameTo(nonViableFileEtat);
            Message_Non_viable+="At year "+scene.getDate()+" Tm : "+periode_m_rate+" > "+ this.m_rate_max+"\n";
          }
          else 
          {
           fwEtat.write(message);
           fwEtat.close();
           fw.write(message);
           fw.close();
           s.closeProject();
           //Renomer en Non_viables:
            File nonViableFileEtat=new File(this.output_dir + "/individu" + id + "_evolEtat_nonViable.txt");
            File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
            file.renameTo(nonViableFile);
            fileEtat1.renameTo(nonViableFileEtat);
            return 0.0d;
            }
         }
         //Gini sur 5 ans 
         else if(periode_gini < this.gini_min || periode_gini > this.gini_max){
          Viable_gini=false;
          String message = "# individual " + id +" for rotation " + j +
            ": this gini coefficient " + periode_gini + " is out of constraints [" + this.gini_min + ";" + this.gini_max + "]";
           System.out.println(message);
          if(Simulation_Non_viable_fin==1){
            res=0.0d;
            File nonViableFileEtat=new File(this.output_dir + "/individu" + id + "_evolEtat_nonViable.txt");
             File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
             file.renameTo(nonViableFile);
             fileEtat1.renameTo(nonViableFileEtat);
             Message_Non_viable+="At year "+scene.getDate()+" Gini : "+periode_gini+" not in "+ "[" + this.gini_min + ";" + this.gini_max + "]\n";
            }
            
          else {
           
           fwEtat.write(message);
            fwEtat.close();
           fw.write(message);
           fw.close();
           s.closeProject();
            //Renomer en Non_viables:
            File nonViableFileEtat=new File(this.output_dir + "/individu" + id + "_evolEtat_nonViable.txt");
            File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
            file.renameTo(nonViableFile);
            fileEtat1.renameTo(nonViableFileEtat);
           return 0.0d;
            }
         }
         //Nb des espèces sur 5 ans 
         else if(periode_nb_spe < this.nb_spe_min)
         {
          Viable_nb_spe=false;
          String message = "# individual " + id +" for rotation " + j +
            ": numbur of species "+ periode_nb_spe +" is out of constraints " + this.nb_spe_min;
           System.out.println(message);
          if(Simulation_Non_viable_fin==1)
          {
              res=0.0d;
              File nonViableFileEtat=new File(this.output_dir + "/individu" + id + "_evolEtat_nonViable.txt");
              File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
              file.renameTo(nonViableFile);
              fileEtat1.renameTo(nonViableFileEtat);
              Message_Non_viable+="At year "+scene.getDate()+" RS: "+periode_nb_spe+" < "+this.nb_spe_min+"\n";
            }else {
           
                fwEtat.write(message);
                fwEtat.close();
                fw.write(message);
                fw.close();
                s.closeProject();
                //Renomer en Non_viables:
                File nonViableFileEtat=new File(this.output_dir + "/individu" + id + "_evolEtat_nonViable.txt");
                File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
                file.renameTo(nonViableFile);
                fileEtat1.renameTo(nonViableFileEtat);
                return 0.0d;
            }
         }
         
         if (Viable_gini && Viable_m_rate && Viable_nb_spe && Viable_BA) {
          res += Math.min(1.0d,
                 (basal_area - basal_area_expect) / ( basal_area_var));
         res += Math.min(1.0d,
                (this.m_rate_max - periode_m_rate ) / this.m_rate_max);
         res += Math.min(1.0d,
                Math.abs(periode_gini - ((this.gini_max + this.gini_min)/2.0d)) / (this.gini_max - this.gini_min));
         res += Math.min(1.0d,
                (periode_nb_spe - this.nb_spe_min) / (this.nb_spe_max - this.nb_spe_min));
         }
         //Couper si seulement si nous sommes dans une année d'intervention 
         if(theta > 0){
           // do cutting intervention
           CepsGHAthinnerCompostion thinnner_comp=new CepsGHAthinnerCompostion(tp,G_obj,param_expectedMixComposition);
           step = s.runIntervener(thinnner_comp, step);
           // save the production
           
           scene = (CepsScene)step.getScene();
           long beforeUsedMem = getUsedMemory();
          //Revoire les arbres couper de la scene scene.getCutree dans chaque patch et calculer le bois coupée
            if (scene.getCutTrees() != null) {
             for (CepsTree Tree:scene.getCutTrees())
             {
              int patchId = Tree.getPatchId();
              double volumeCut = Tree.getVolume(); // Supposons que vous avez une méthode pour obtenir le volume coupé de l'arbre
              double currentVolume = dic_Vol_cut_Patch.getOrDefault(patchId, 0.0d);
              dic_Vol_cut_Patch.put(patchId, currentVolume + volumeCut);
              v_prod += Tree.getVolume();
              scene.removeTree(Tree);

            }
          }
          System.gc();
          
         long afterUsedMem = getUsedMemory();
 
         // Calcul de la mémoire utilisée
         long usedMem = beforeUsedMem - afterUsedMem;
         }
        } //// - - - - - - - - - - - - - - - - - - - - - - - - - - - End evolution

       //diviser v_prod par le nombre de patch : ceci n'est pas ramener à l'ha 
       v_prod/=nb_patch;
 
       // test production constraint , la valeur minimale est 3.1 m3/ha/an il ny pas de probleme sur la terminsaion de la lecture car si deja la fin
       if(v_prod < v_prod_expect){
        Viable_V_prod=false;
        
        String message = "# individual " + id + ": this production " +
                           v_prod *(10000/patch_area)+ " is out of constraints " + v_prod_expect*(10000/patch_area);
         System.out.println(message);
        if (Simulation_Non_viable_fin==1) 
        {
          res=0.0d;
          //Renomer en Non_viables:
         File nonViableFileEtat=new File(this.output_dir + "/individu" + id + "_evolEtat_nonViable.txt");
         File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
         file.renameTo(nonViableFile);
         fileEtat1.renameTo(nonViableFileEtat);
         Message_Non_viable+="At year "+scene.getDate()+" v_prod: "+v_prod *(10000/patch_area)+" < "+v_prod_expect*(10000/patch_area)+"\n";

        }else {
         fwEtat.write(message);
         fwEtat.close();
         fw.write(message);
         fw.close();
         s.closeProject();
        //Renomer en Non_viables:
          File nonViableFileEtat=new File(this.output_dir + "/individu" + id + "_evolEtat_nonViable.txt");
          File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
          file.renameTo(nonViableFile);
          fileEtat1.renameTo(nonViableFileEtat);
         return 0.0d;
       }
      }
       //FIn d'intervention pour individus i , afficher les resultats pour tous .
      if (Viable_gini && Viable_m_rate && Viable_nb_spe && Viable_BA && Viable_V_prod ) {
       System.out.println("----------------------------------------------------------------------- prod " + (v_prod - v_prod_expect) / ( v_prod_var * 5.0d * nb_patch));
       System.out.println("------------------------------------------------------------ prod sur 80 ans  " + v_prod *(10000/patch_area)+" m3/ha ");
       System.out.println("---------------------------------------------------------- expect sur 80 ans " + v_prod_expect *(10000/patch_area)+" m3/ha ");
       System.out.println("------------------------------------------------------------- var " + v_prod_var );
      res /= inter_max; // in [0,4]
      res += Math.min(1.0d,
              (v_prod - v_prod_expect) / ( v_prod_var * 5.0d * nb_patch));
       
       // save data in files
       String message = "# indidual "+id+", fitness "+res;
       System.out.println(message);
       fwEtat.write(message);
       fwEtat.close();
       fw.write(message);
       fw.close();
       }else  {
        System.out.println(Message_Non_viable);
        fwEtat.write(Message_Non_viable);
        fwEtat.close();
        fw.write(Message_Non_viable);
        fw.close();
       }
       if(all_exit){
         // Mean stand export -> mean.txt
         CepsMeanStandExport e1 = new CepsMeanStandExport();
         e1.initExport(s.getModel(), step);
         e1.save(this.output_dir+"/individu"+id+"_mean.txt");
         // Complete export -> complete.txt
         CepsExport e2 = new CepsExport();
         e2.initExport(s.getModel(), step, 1, 1);
         e2.save(this.output_dir+"/individu"+id+"_complete.txt");
         // ProductivityScene export -> productivityScene.txt
         CepsProductivityScene e = new CepsProductivityScene();
         e.initExport(s.getModel(), step, 1, 1);
         e.save(this.output_dir+"/individu"+id+"_productivityScene.txt");
       }
       s.closeProject();
       endTime = System.nanoTime();
       FileWriter fz= new FileWriter(output_dir + "/execution_times.txt",true);
       fz.write("Id: "+id+ "\t" +(endTime - startTime)/Math.pow(10,9)+"s\t"+res +".\n");
       fz.close();
       }
     catch (Exception e) {
       System.out.println("Hello Marion\n\n");
       throw e;
       }
      
     return res;
   }

//Ajouter pour Caluler la surface terrière totale pour tous les espèces 
    protected double getGha(List<CepsTree> trees) {
  
      double gha = 0;
      for (CepsTree t : trees) {
          gha += getGHA(t);
      }
  
      return gha;
  }
  
  //Savoir des arbres de l'espece name dans la scene 
  protected List<CepsTree> getTrees(List<CepsTree> candidateTrees,  String sName) {

        

    List<CepsTree> trees = new ArrayList<>();

    for (CepsTree t : candidateTrees) 
    {
      if (t.getSpecies().sname.equals(sName)) {
      trees.add(t);
      }
        
    }

    

    return trees;
}
private double getGHA(CepsTree t) {
        double g = Math.pow(t.getDbh() / 100d, 2d) * Math.PI / 4d;
        double gHa = g * 10000d / 1000d;
        return gHa;
    }
  
    private String traceSceneComposition(List<CepsTree> trees) {
      StringBuffer b = new StringBuffer("");
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
    
          b.append(" " + sName + ": " +  " " + percentage + "%");
      }
    
      return b.toString();
    }
    
 }