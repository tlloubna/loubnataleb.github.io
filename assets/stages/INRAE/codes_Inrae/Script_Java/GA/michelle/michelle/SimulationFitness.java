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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

/**
 * Forceps simulation to calculate the objective function of the genetic algorithm.
 *
 * @author M. Malara - July 2023 & M. Jourdan - March 2024 & change by loubna 2024
 */
public class SimulationFitness {

   private String working_dir; 	// All files are read in the same directory than the commanFile
	 private String output_dir;  // name of the directory where the results will be saved
	 private String setup_file;
	 private String site_file;
	 private String climate_file;
	 private String inventory_file;
	 private String potential_species;
   private String trait_potentiel_species;//les 5 espèces à ajouter dans le génome de la simulation add by loubna 
   
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


	/**
	 * Constructor
	 */
	public SimulationFitness(String working_dir, String output_dir, String setup_file,
                           String site_file, String climate_file, String inventory_file,
                           String potential_species,String trait_potentiel_species,boolean risk_aversion,
                           double basal_area_min, double basal_area_max,
                           double gini_min, double gini_max, double m_rate_max,
                           int nb_spe_min, int nb_spe_max, double v_prod_min, double v_prod_max) {
		// directory use
		this.working_dir = working_dir;
		this.output_dir = output_dir;
		// file for forceeps simulation
		this.setup_file = setup_file;
		this.site_file = site_file;
		this.climate_file = climate_file;
		this.inventory_file = inventory_file;
		this.setup_file = setup_file;
    this.potential_species=potential_species;
		this.trait_potentiel_species= trait_potentiel_species;//change
   
    // risk aversion
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
	}


  /* Printer */
  @Override
  public String toString(){
    String res = "# basa area constraints = ["+this.basal_area_min+", "+this.basal_area_max+"] m2/ha\n"
               + "# gini constraints = ["+this.gini_min+", "+this.gini_max+"]\n"
               + "# number of species constraints = ["+this.nb_spe_min+", "+this.nb_spe_max+"]\n"
               + "# production constraints = ["+this.v_prod_min+", "+this.v_prod_max+"]m3/ha/year\n"
               + "# mortality rate constraints < "+this.m_rate_max+"\n";
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
      return calcFitnessRiskAversion(genome, id, inter_max, all_exit);}
    else{
      return calcFitnessAverage(genome, id, inter_max, all_exit);}
  }

//Add by loubna 
private static long getUsedMemory() {
  MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
  MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
  return heapMemoryUsage.getUsed();
}


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
      //add by loubna :lire les espèces en formats string species 
      System.out.println("les especes sont :"+this.trait_potentiel_species);
      List<Integer> Id_species_Cf=i.convertSpeciesShortNamesToIds(this.trait_potentiel_species);//enregesitrer les 5 premiers especes 
      /*this.potential_species=Id_species_Cf.stream()
                     .map(String::valueOf)  // Converts each integer to a string
                     .collect(Collectors.joining(" "));  // Joins them with space as a delimiter*/
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

      //// Evolution  Change by loubna - - - - -  - - - - - - - - - - - - - - - - - - - - - - - -
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
        String[] splitShortNames = trait_potentiel_species.split(" ");
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

            for(CepsTree tree : patch.getTrees()) { // - - - - - - - - - - - -
              // eatch tree
              nb_tree += 1;
                // basal area
              total_g += tree.getBasalArea();
                // gini
              total_d += tree.getDbh();
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


  private double calcFitnessAverage(List<Double> genome, int id, int inter_max, boolean all_exit) throws Exception{
    /** do simulation to calculate interest score either with average version
    return interest score
    */
    // Calculate constraints
    //calculer le temps d'excution :
    long startTime, endTime;
    startTime=System.nanoTime();
    int nb_tree ;
      // basal area
    double total_g;
    double basal_area = 0.0d;
    double basal1;
    double basal2;
    double basal3;
    double basal4;
    double basal5;
    //List<Double> BasalArea_sp=new ArrayList<Double>();
    List<Double> BasalArea_sp = new ArrayList<>(Collections.nCopies(trait_potentiel_species.split(" ").length, 0.0d));
    String speciesName;
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
      // production
    double v_prod = 0.0d;
    //basal area initial:
    
    // Do intervention
    double theta;
    double tp; // [0, 1]
    double g_obj;
    // Calculate fitness
    double res = 0.0d;
    String G_obj;
    Map<String,Double> C_esp=new HashMap<String,Double>();
    String param_expectedMixComposition;
    //la longeur de la liste :
    int length = trait_potentiel_species.split(" ").length;
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

      String[] split = trait_potentiel_species.split(" ");
      // Writing header information to file
      fw.write("# individual " + id + " genome : " + this.genomeString(genome) + "\n#year\tidPatch\tnbTree_ha nbTree_patch\tbasalAreaTot_ha");
      for (String species : split) {
          fw.write(" basalArea" + species);
      }

      fw.write(" nbDeadTree_ha\tbasalAreaDead_ha\tmortalityRate\tcoefGini\tnbOfSpecies\tvol_prod_ha\n");
      //ecrire dans le fichier les caracteriqtiques de chaque intervention 
      String filename = this.output_dir+"/individu"+id+"intervention.txt";
      File fil = new File(filename);

      if (!fil.getParentFile().exists())
        fil.getParentFile().mkdirs();
      if (!fil.exists())
        fil.createNewFile();

      FileWriter fv = new FileWriter(fil);
      fv.write("# individual " + id + " genome : " + this.genomeString(genome) + "\n#year_Inv\ttp\tG_init\tG_obj\tG_rest\tC_init\tC_cible\tC_rest\n");

      
      //// Start forceeps in capsis
  		C4Script s = new C4Script("forceps");
  		CepsInitialParameters i = new CepsInitialParameters(this.working_dir + "/"
                                                        + this.setup_file);
  		i.loadSetupFile();
  		i.siteFileName = i.addSetupDirectory(this.site_file);
  		i.defaultClimateFileName = i.addSetupDirectory(this.climate_file);
  		i.inventoryFileName = i.addSetupDirectory(this.inventory_file);
  		System.out.println("les especes sont :"+this.trait_potentiel_species);

      List<Integer> Id_species_Cf=i.convertSpeciesShortNamesToIds(this.trait_potentiel_species);//enregesitrer les 5 premiers especes 

      /*this.potential_species=Id_species_Cf.stream()
                     .map(String::valueOf)  // Converts each integer to a string
                     .collect(Collectors.joining(" "));  // Joins them with space as a delimiter*/

  		i.setPotentialSpeciesList(this.potential_species);
      
  		s.init(i);

      // set variables
  		Step step = s.getRoot();
      CepsScene scene = (CepsScene)step.getScene();
      int nb_patch = scene.getPatches().size(); // i.inventoryPatchN;
      double patch_area = scene.getPatches().get(1).getArea();
      
      // calc Cst
      double basal_area_expect = this.basal_area_min*(patch_area/10000) ;
      double basal_area_var = (this.basal_area_max-this.basal_area_min)*(patch_area/10000) ;
      double v_prod_expect = this.v_prod_min  * inter_max*5.0d *(patch_area/10000);
      double v_prod_var = (this.v_prod_max - this.v_prod_min)  * inter_max*5.0d*(patch_area/10000);
      //caracteristique de l'intervention
      double initG; //c'est déja calculer 
      String C_end;
      String C_init;

      double G_end=0.0d;
      //startTime = System.nanoTime();//time pour l'evolution 
  		//// Evolution  - - - - -  - - - - - - - - - - - - - - - - - - - - - - - -
  		for (int j = 0; j < inter_max; ++j) {
        //calculer initG
        scene = (CepsScene)step.getScene();
        initG=0.0d;
        for (CepsTree tree :scene.getTrees()){
          initG+=getGHA(tree);
        }
        G_end=initG;
        //calculer la composition initiale :
        List<CepsTree> trees=scene.getTrees();
        C_init=traceSceneComposition(trees);
        C_end=traceSceneComposition(trees);
        //changer la longueur de génome 
  			theta = genome.get(j*(3+length)); // years
        // set constraints to zero
        periode_m_rate = 0.0d;
        periode_gini = 0.0d;
        periode_nb_spe = 0.0d;
        //le type d'éclairci 
				tp = genome.get(j*(3+length)+1); // [0, 1]
				g_obj = genome.get(j*(3+length)+2); // m2/ha OR %
        List<Double> percentage_Cf=new ArrayList<>();
        //add by loubna 
        Double G=g_obj*100;
        G_obj=String.valueOf(G);
        //ajouter à G_obj %
        G_obj+="%";
        for (int k=3;k<(3+length);k++){
          percentage_Cf.add(genome.get(j*(3+length)+k));
        }
        //Recuprer les noms et placer dans parmetre_expectedmix;
        String[] splitShortNames = trait_potentiel_species.split(" ");
        StringBuilder sb = new StringBuilder();
        //Remplir la composition pour couper la forets 
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
        //startTime = System.nanoTime();//time pour 
        for(int t = 0; t < 5; ++t){ // - - - - -  - - - - - - - - - - - -
          // run simulation during the period of 5 year, year by year
					step = s.evolve(new CepsEvolutionParameters(1));
          // calcul constraints each years
          scene = (CepsScene)step.getScene();
          year_gini = 0.0d;
          year_m_rate = 0.0d;
          year_nb_spe = 0;
          basal_area = 0.0d;
          //startTime = System.nanoTime();
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
            BasalArea_sp = new ArrayList<>(Collections.nCopies(trait_potentiel_species.split(" ").length, 0.0d));
            //startTime = System.nanoTime();//time pour les tree 
            for(CepsTree tree : patch.getTrees()) { // - - - - - - - - - - - -
                // each tree
              nb_tree += 1;
                // basal area
              total_g += tree.getBasalArea();
                // gini
              total_d += tree.getDbh();
              for(CepsTree othertree : patch.getTrees()){
                diff_d += Math.abs(tree.getDbh() - othertree.getDbh());
              }
              //les surfaces par especes :
              String[] split2 = trait_potentiel_species.split(" ");
              speciesName = tree.getSpecies().sname;
              for (int z = 0; z < splitShortNames.length; z++) {
                    if (speciesName.equals(splitShortNames[z])) {
                          BasalArea_sp.set(z, BasalArea_sp.get(z) + tree.getBasalArea());
                      }
              }
              
                // nb species
              id_spe = tree.getSpecies().getValue();
              if(!species_list.contains(id_spe)){
                species_list.add(id_spe);
                species_list_size += 1;
              }
            } // - - - - - - - - - - - - - - - - - - - - - - - - - - - end trees
            //endTime = System.nanoTime();
            //System.out.println("Time to calculate the Tree : " + (endTime - startTime) + " ns");
              // mortality rate
            if (scene.getDeadTrees() != null) {
              for (CepsTree deadtree : scene.getDeadTrees()) {
                if (deadtree.getPatchId() == patch.getId()){
                  dead_g += deadtree.getBasalArea();
                  nb_dead += 1;
                }
              }
            }

            // save results to a file
            fw.write(scene.getDate() + "\t" + patch.getId() + "\t" + nb_tree*(10000/patch_area) + "\t" + nb_tree +"\t"+
                     total_g*(10000/patch_area) +"\t");

            for (int z = 0; z < split.length; z++)  {
                      fw.write(BasalArea_sp.get(z)*(10000/patch_area)+"\t");
            }

            fw.write(nb_dead*(10000/patch_area) + "	" + dead_g*(10000/patch_area) + "	" + nb_dead*1.0d/(nb_tree+nb_dead)
            + "\t" + diff_d/(2*nb_tree*total_d) + "\t" +
            species_list_size + "\t" + v_prod*(10000d/patch_area) + "\n");

            basal_area += total_g;
            year_gini += diff_d/(2*nb_tree*total_d);
            year_m_rate += nb_dead*1.0d/(nb_tree+nb_dead);
            year_nb_spe += species_list_size;
          } // - - - - - - - - - - - - - - - - - - - - - - - - - - -end 30 patch
          //endTime = System.nanoTime();
        //System.out.println("Time to calculate in the patch: " + (endTime - startTime) + " ns");
          // average on patches
          basal_area /= nb_patch;
          periode_gini += year_gini / nb_patch ;
          periode_m_rate += year_m_rate / nb_patch;
          periode_nb_spe += year_nb_spe / nb_patch;

          // test basal area constraint in any year
          if(basal_area < basal_area_expect){
            String message = "# individual " + id +" for rotation " + j +
             ": this basal area is out of constraints " + basal_area +"minBasal:"+basal_area_expect;
            System.out.println(message);
            fw.write(message);
            fw.close();
        		s.closeProject();
            File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
            file.renameTo(nonViableFile);
            return 0.0d;
          }

          // add to result the distance to contraint for basal area
          // (basal_area - 1.0d) / (theta * 7.0d));
          res += Math.min(1.0d,
                 (basal_area - basal_area_expect) / (5.0d * basal_area_var));
        } // - - - - - - - - - - - - - - - - - - - - - - - - - - - - end periode
        //endTime = System.nanoTime();
        //System.out.println("Time to calculate in the period : " + (endTime - startTime) + " ns");
        // average on periode
        periode_m_rate /= 5.0d;
        periode_gini /= 5.0d;
        periode_nb_spe /= 5.0d;

        // test other constraints over the period
        if(periode_m_rate > this.m_rate_max){
          String message = "# individual " + id +" for rotation " + j +
           ": this mortality rate" + periode_m_rate + "is out of constraints " + m_rate_max;
          System.out.println(message);
          fw.write(message);
          fw.close();
      		s.closeProject();
          File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
          file.renameTo(nonViableFile);
          return 0.0d;
        }
        else if(periode_gini < this.gini_min || periode_gini > this.gini_max){
          String message = "# individual " + id +" for rotation " + j +
           ": this gini coefficient " + periode_gini + " is out of constraints [" + this.gini_min + ";" + this.gini_max + "]";
          System.out.println(message);
          fw.write(message);
          fw.close();
      		s.closeProject();
          File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
          file.renameTo(nonViableFile);
          return 0.0d;
        }
        else if(periode_nb_spe < this.nb_spe_min){
          String message = "# individual " + id +" for rotation " + j +
           ": numbur of species "+ periode_nb_spe +" is out of constraints " + this.nb_spe_min;
          System.out.println(message);
          fw.write(message);
          fw.close();
      		s.closeProject();
          File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
          file.renameTo(nonViableFile);
          return 0.0d;
        }

        // add to result the distance to contraints
        res += Math.min(1.0d,
               (this.m_rate_max - periode_m_rate ) / this.m_rate_max);
        res += Math.min(1.0d,
               Math.abs(periode_gini - ((this.gini_max + this.gini_min)/2.0d)) / (this.gini_max - this.gini_min));
        res += Math.min(1.0d,
               (periode_nb_spe - this.nb_spe_min) / (this.nb_spe_max - this.nb_spe_min));
        //startTime = System.nanoTime();
  			if(theta > 0){
					// do cutting intervention
					//CepsGHAThinner2bis thinner = new CepsGHAThinner2bis(tp, g_obj);
          //CepsGHAThinner2 thinner_2=new CepsGHAThinner2(3,tp,G_obj,param_expectedMixComposition);
          CepsGHAthinnerCompostion thinnner_comp=new CepsGHAthinnerCompostion(tp,G_obj,param_expectedMixComposition);
					step = s.runIntervener(thinnner_comp, step);
          // save the production
          
          scene = (CepsScene)step.getScene();
          long beforeUsedMem = getUsedMemory();
          if (scene.getCutTrees() != null) {
            for (CepsTree cuttree : scene.getCutTrees()) {
              v_prod += cuttree.getVolume();
              G_end-=getGHA(cuttree);
              scene.removeTree(cuttree);

            }
            
          }
          System.gc();
          /*try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        long afterUsedMem = getUsedMemory();

        // Calcul de la mémoire utilisée
        long usedMem = beforeUsedMem - afterUsedMem;

        System.out.println("Mémoire liberee  par la suppression des arbres : " + usedMem/ (1024.0 * 1024.0) + "MB  et memoire avant:"+beforeUsedMem / (1024.0 * 1024.0)+ "MB");
        System.out.println("memoire apres supression est : "+afterUsedMem/ (1024.0 * 1024.0)+"MB"  );
          C_end=traceSceneComposition(scene.getTrees());
          //inclure les données dans le fichier :
          fv.write(scene.getDate()+"\t"+tp+"\t"+initG+"\t"+G_obj+"\t"+(G_end/initG)*100+"\t"+C_init+"\t"+param_expectedMixComposition+"\t"+C_end+"\n");
          
  			}
        
        //endTime = System.nanoTime();
        //System.out.println("Time to Thinning: " + (endTime - startTime) + " ns");
  		} //// - - - - - - - - - - - - - - - - - - - - - - - - - - - End evolution
      //endTime = System.nanoTime();
      //System.out.println("Time for evolution 80 year : " + (endTime - startTime) + " ns");

      // test production constraint
      if(v_prod < v_prod_expect){
        String message = "# individual " + id + ": this production " +
                          v_prod + " is out of constraints " + v_prod_expect;
        System.out.println(message);
        fw.write(message);
        fw.close();
        s.closeProject();
        File nonViableFile = new File(this.output_dir + "/individu" + id + "_evolConstraints_nonViable.txt");
        file.renameTo(nonViableFile);
        return 0.0d;
      }
      // add to result the distance to contraints for production
      res /= inter_max; // in [0,4]
      System.out.println("----------------------------------------------------------------------- prod " + (v_prod - v_prod_expect) / ( v_prod_var * 5.0d * nb_patch));
      System.out.println("------------------------------------------------------------ prod " + v_prod );
      System.out.println("---------------------------------------------------------- expect " + v_prod_expect );
      System.out.println("------------------------------------------------------------- var " + v_prod_var );
      res += Math.min(1.0d,
             (v_prod - v_prod_expect) / ( v_prod_var * 5.0d * nb_patch));

      // save data in files
      String message = "# indidual "+id+", fitness "+res;
      System.out.println(message);
      fw.write(message);
      fw.close();
      fv.close();
      //startTime = System.nanoTime();
      
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
      }//endTime = System.nanoTime();
      //System.out.println("Time for filling files mean complete productivity : " + (endTime - startTime) + " ns");
  		s.closeProject();
      endTime = System.nanoTime();
      System.out.println("Time for calcFitness : " + (endTime - startTime) + " ns");
     FileWriter fz= new FileWriter(output_dir + "/execution_times.txt",true);
      
      fz.write("Id: "+id+"\t" +(endTime - startTime) + "ns\t" +(endTime - startTime)/Math.pow(10,9)+"s\t"+res +".\n");
      fz.close();
      

    /*endTime = System.nanoTime();
    System.out.println("Time for calcFitness : " + (endTime - startTime) + " ns");

    //ExecutionTimeData data = new ExecutionTimeData(id, endTime - startTime, (endTime - startTime) / Math.pow(10, 9), res);

   try (FileOutputStream fos = new FileOutputStream(output_dir + "/execution_times.dat", true);
          ObjectOutputStream oos = new ObjectOutputStream(fos)) {
          //oos.writeObject(data);
          oos.writeInt(id);
          oos.writeDouble((endTime - startTime) / Math.pow(10, 9));
          oos.writeDouble(res);
      }*/

    }
    catch (Exception e) {
      System.out.println("Hello Marion\n\n");
      // better log error, return also interior of Exception e to know what is happening
      throw e;
      //throw new Exception ("Wrong format in simulation "+id);
      }
     
    return res;
	}


  protected double getGha(List<CepsTree> trees) {

    double gha = 0;
    for (CepsTree t : trees) {
        gha += getGHA(t);
    }

    return gha;
}


private double getGHA(CepsTree t) {
    double g = Math.pow(t.getDbh() / 100d, 2d) * Math.PI / 4d;
    
    // fc-8.6.2020 considers group area is any
    double gHa = g * 10000d / 1000d;
//		double gHa = g * 10000d / scene.getArea();
    
    return gHa;
}

private String traceSceneComposition(List<CepsTree> trees) {
  StringBuffer b = new StringBuffer("");

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

      b.append(" " + sName + ": " +  " " + percentage + "%");
  }

  return b.toString();
}

}