
package forceps.myscripts.LM_sequentielle;

import java.util.Random;
import java.util.List;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import forceps.myscripts.LM_sequentielle.SimulationFitnessSeq;

/**
 * Class representing management as a gene for the genetic algorithm.
 *
 * @author M. Malara - July 2023 & M. Jourdan - March 2024 & loubna 2024 March-August
 */

public class ManagementIndvSeq{
  /** class representing management as a gene for the genetic algorithm
   use CalcFitness for fitness calculation
  */
 
  //calculer le temps d'exection pour chaque individu
  private long executionTime ;
  private static int cnt = 0; // counter for id
  // authorised values for genes
  private static List<Double> theta = Arrays.asList(5.0d,10.0d,15.0d,20.0d); // period in year, factor of 5
  private static List<Double> tp = Arrays.asList(0.0d,0.5d,1.0d);            // type of thinning, between 0 and 1
  private static List<Double> g_obj = Arrays.asList(0.5d,0.6d,0.7d,0.8d,0.9d);// basal area objective in %
  //on va choisir la combinaisons parmi le fichier qui nous donne combinaions valides de 1001 combinaisons possibles, PS : le nombre des combinaisons peut changer
  private  List<List<Double>> Cf_k = new ArrayList<>();
 //nb _list
  private static int nb_theta = theta.size();
  private static int nb_tp = tp.size();
  private static int nb_g_obj = g_obj.size();
  private int Nb_esp=0;

  //Paramètre d'individu.
    private int id;              // individual identifier
    private int inter_max;       // maximum number of interventions
    private SimulationFitnessSeq simu_fitness;
    private List<Double> genome;  // list of genes (size inter_max*3)
    private double fitness;       // resistance/interest score

 
  /* Costructor */
  public ManagementIndvSeq(){
    /** empty Constructor
    */
    try {
      //Il faut que tu places combinaisons_valides.csv dans le repertoire de canew ou bien de ton capsis
      loadAdditionalGenes(simu_fitness.working_dir+"/"+simu_fitness.Combinaisons_species);
      
  } catch (Exception e) {
      System.err.println("Failed to load additional genes: " + e.getMessage());
      
  }
    
  }
  public ManagementIndvSeq(int inter_max, SimulationFitnessSeq simu_fitness) throws Exception {
    /** create a random individual of size 8*inter_max
    int inter_max : maximum number of interventions
    SimulationFitness simu_fitness : number of simulations to fitness calculation
    */
    ++cnt;
    this.id = cnt;
    this.inter_max = inter_max;
    this.simu_fitness = simu_fitness;
    try {
      loadAdditionalGenes(simu_fitness.working_dir+"/"+simu_fitness.Combinaisons_species);
     
  } catch (Exception e) {
      System.err.println("Failed to load additional genes: " + e.getMessage());
     
  }
    this.genome = this.randomGenome(inter_max); // create a random genome
    this.fitness = this.calcFitness(false );     // calculate is fitness
   
    
  }


  public ManagementIndvSeq(List<Double> genome, SimulationFitnessSeq simu_fitness, boolean all_exit) throws Exception {
    /** create a individual with genome
    List<Double> genome : list of genes (Nb_esp divisible by 3)
    SimulationFitness simu_fitness : number of simulations to fitness calculation
    */
    // TO DO Cheker si c'est divisible par 3
    try {
      loadAdditionalGenes(simu_fitness.working_dir+"/"+simu_fitness.Combinaisons_species);
      
  } catch (Exception e) {
      System.err.println("Failed to load additional genes: " + e.getMessage());
      
  }
    ++cnt;
    this.id = cnt;
    this.inter_max = genome.size()/(3+Nb_esp);  // 3 +Nb_esp :genes for each intervention
    this.simu_fitness = simu_fitness;
    this.genome = genome;
    this.fitness = this.calcFitness(all_exit); // calculate is fitness
    
    
  }
  

  /* Printer */

  @Override
  public String toString(){
    //Tu peux ajouter le temps de simulations
    return("Individual number " + this.id + " with a fitness = " +
     this.fitness  );
    }
    //les getteurs 

    public List<List<Double>> getAllCf_k() {
      return this.Cf_k;
    }

    public String genomeString(){
        /** convert genome (lis<Double>) on String
      return interest score String
      */
          String res = "";
          for(double g: this.genome){
            res += Double.toString(g) + ", ";
          }
          return res;
    }
    //retourner le temps d'execution 
      public long getExecution() {
        return this.executionTime;
      }
      //la longueur de la liste des espèces :
      
      public int getNb_esp(){
        return this.Nb_esp;
      }
 
    public List<Double> getAllTheta(){
      return theta;
    }


    public List<Double> getAllTP(){
      return tp;
    }


    public List<Double> getAllGobj(){
      return g_obj;
    }


    public int getId(){
      return this.id;
    }


    public SimulationFitnessSeq getSimuFitness(){
      return this.simu_fitness;
    }


    public int getInterMax(){
      return this.inter_max;
    }


    public double getFitness(){
      return this.fitness;
    }
    public void setFitness(double fit){
      this.fitness = fit;
    }

    public List<Double> getGenome(){
      return this.genome;
    }


    public double getGene(int i){
      return this.genome.get(i);
    }


      /* Public Functions */
    public boolean sameGenome(List<Double> genome){
        /** compares its genome with that given as input
        return true if they are identical, false if not
        */
        int i = 0;
        for(double g : this.genome){
          if(g != genome.get(i)){
            return false;
          }
          ++i;
        }
        return true;
      }
       //lire le fichier qui contient les combinaisons possible:
      private void loadAdditionalGenes(String filePath) throws Exception {
        Cf_k.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<Double> geneList = new ArrayList<>();
                String[] genes = line.split(","); // les valeurs sont séparer par des virgules
                
                for (String gene : genes) {
                  
                    geneList.add(Double.parseDouble(gene.trim())); // Convertir en Double et enlever les espaces
                }
                Cf_k.add(geneList);
                this.Nb_esp=geneList.size();
                
                if(Cf_k.isEmpty()){
                  System.out.println("la liste C_fk est vide ");
                }
            }
        }
    }

  


  /* Private Functions */
  private List<Double> randomGenome(int inter_max){
    //System.out.println("Choisir un génome aléatoire ");;
    Integer id = new Integer(this.id);
    long seed = id.longValue(); // Utiliser l'ID comme graine pour la reproductibilité, si nécessaire
    //Random r = new Random(seed);
    List<Double> random_genome = new ArrayList<>();
    Random r = new Random(seed+simu_fitness.getSeed());
   
    int totalSize = 0;
    for (List<Double> list : Cf_k) {
            totalSize += 1;
    }
    List <Double> seqNull=new ArrayList<>();
    for (int i=0 ;i< 3+this.Nb_esp;i++){
        seqNull.add(0.0d);
    }

    for (int i = 0; i < inter_max; ++i) 
    {
        int j = r.nextInt(nb_theta); // Sélection aléatoire de theta
      if (j>0) 
      {
        for (int t = 0; t < j && i < inter_max; ++t) 
        {
                // Ajouter des zéros pour les périodes sans intervention :
                random_genome.addAll(seqNull);
                ++i;
        }
      }
      if (i < inter_max) 
        {
            // Ajouter les trois gènes standards
            random_genome.add(theta.get(j)); // Période
            random_genome.add(tp.get(r.nextInt(nb_tp))); // Type de coupe
            random_genome.add(g_obj.get(r.nextInt(nb_g_obj))); // Objectif de surface 
            // Sélectionner aléatoirement un n_uplet de Cf_k et l'ajouter
            int k=r.nextInt(totalSize);
            List<Double> selectedQuintuplet = Cf_k.get(k); //ça dépend , ce n'est pas exactement un Quintuplet , peut etre un 6_uplet , un Triplet .....
            random_genome.addAll(selectedQuintuplet);
        }
    }
    return random_genome;
  }
  
  //C'est pour calculer sa fitness : lancer le scprit de SimulationFitnessPa.java et aussi calculer son temps de simulation 
  public double calcFitness(boolean all_exit) throws Exception {
    /** use this.simu_fitness to do simulation and calculate interest score
    return interest score
    */
    long starTime=System.nanoTime();
    double fitness=this.simu_fitness.calcFitness(this.genome, this.id, this.inter_max, all_exit);
    long endTime=System.nanoTime();
    this.executionTime=endTime-starTime;
    return fitness;
  }
  //supprimer les individus non viables : pour vider le processeur quand on ne vas pas utiliser cette Ind dans la prochaine generation ou bien la generation courante.
    public void cleanup() {
        // Libérer les ressources lourdes si nécessaire
        this.simu_fitness = null;
        this.Cf_k.clear();
    }

}