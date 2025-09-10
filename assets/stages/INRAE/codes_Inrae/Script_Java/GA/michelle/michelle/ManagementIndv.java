
package forceps.myscripts.michelle;

import java.util.Random;
import java.util.List;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import forceps.myscripts.michelle.SimulationFitness;

/**
 * Class representing management as a gene for the genetic algorithm.
 *
 * @author M. Malara - July 2023 & M. Jourdan - March 2024
 */

public class ManagementIndv {
  /** class representing management as a gene for the genetic algorithm
   use CalcFitness for fitness calculation
  */
  //lire le fichier des combinaisons possibles:
  //calculer le temps d'exection pour chaque individu
  private long executionTime ;
  private static int cnt = 0; // counter for id
  // authorised values for genes
  private static List<Double> theta = Arrays.asList(5.0d,10.0d,15.0d,20.0d); // period in year, factor of 5
  private static List<Double> tp = Arrays.asList(0.0d,0.5d,1.0d);            // type of thinning, between 0 and 1
  private static List<Double> g_obj = Arrays.asList(0.5d,0.6d,0.7d,0.8d);// basal area objective in %
  //on va choisir la combinaisons parmi le fichier qui nous donne combinaions valides de 1001 combinaisons possibles.
  private  List<List<Double>> Cf_k = new ArrayList<>();
  private static int nb_theta = theta.size();
  private static int nb_tp = tp.size();
  private static int nb_g_obj = g_obj.size();
  //add seed
  // Add this line at the class level to define a fixed seed
private static final long SEED = 12345L;
private static final Random r = new Random(SEED);

  private int length=0;
  // individual parameters
  private int id;              // individual identifier
  private int inter_max;       // maximum number of interventions
  private SimulationFitness simu_fitness;
  private List<Double> genome;  // list of genes (size inter_max*3)
  private double fitness;       // resistance/interest score

  //lire le fichier qui contient les combinaisons possible:
  private void loadAdditionalGenes(String filePath) throws Exception {
    Cf_k.clear();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
        String line;
        while ((line = br.readLine()) != null) {
            List<Double> geneList = new ArrayList<>();
            String[] genes = line.split(","); // Supposons que les valeurs sont séparées par des virgules
            
            for (String gene : genes) {
                //System.out.println("on a lu le fichier et voici les genes:"+gene);
                geneList.add(Double.parseDouble(gene.trim())); // Convertir en Double et enlever les espaces
            }
            Cf_k.add(geneList);
            this.length=geneList.size();
            //System.out.println("voici la longueur de la liste :"+this.length);
            //System.out.println(geneList);
            if(Cf_k.isEmpty()){
              System.out.println("la liste C_fk est vide ");
            }
        }
    }
}
  /* Costructor */
  public ManagementIndv(){
    /** empty Constructor
    */
    try {
      loadAdditionalGenes("../combinaisons_valides.csv");
      System.out.println("on a lu le fichier  par defaut");
  } catch (Exception e) {
      System.err.println("Failed to load additional genes: " + e.getMessage());
      
  }
    
  }


  public ManagementIndv(int inter_max, SimulationFitness simu_fitness) throws Exception {
    /** create a random individual of size 8*inter_max
    int inter_max : maximum number of interventions
    SimulationFitness simu_fitness : number of simulations to fitness calculation
    */
    ++cnt;
    this.id = cnt;
    this.inter_max = inter_max;
    this.simu_fitness = simu_fitness;
    try {
      loadAdditionalGenes("../combinaisons_valides.csv");
      System.out.println("on a lu le fichier a partir du inter+simu  ");
  } catch (Exception e) {
      System.err.println("Failed to load additional genes: " + e.getMessage());
     
  }
    this.genome = this.randomGenome(inter_max); // create a random genome
    System.out.println("Voici l'individu  cree dans la pop:"+genome);
    
    //this.fitness = this.calcFitness(false );     // calculate is fitness
    
  }


  public ManagementIndv(List<Double> genome, SimulationFitness simu_fitness, boolean all_exit) throws Exception {
    /** create a individual with genome
    List<Double> genome : list of genes (length divisible by 3)
    SimulationFitness simu_fitness : number of simulations to fitness calculation
    */
    // TO DO Cheker si c'est divisible par 3
    try {
      loadAdditionalGenes("../combinaisons_valides.csv");
      System.out.println("on a lu le fichier par genome ");
  } catch (Exception e) {
      System.err.println("Failed to load additional genes: " + e.getMessage());
      
  }
    ++cnt;
    this.id = cnt;
    this.inter_max = genome.size()/(3+length);  // 3 genes for each intervention
    this.simu_fitness = simu_fitness;
    this.genome = genome;
    //this.fitness = this.calcFitness(all_exit); // calculate is fitness
    
    
  }
  

  /* Printer */
  @Override
  public String toString(){
    return("Individual number " + this.id + " with a fitness = " +
     this.fitness + ". Execution time = " + this.executionTime + " ns"+" = "+this.executionTime/Math.pow(10, 9)+"s." );
  }

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
    public int getLength(){
      return this.length;
    }
  /* Getter */
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


  public SimulationFitness getSimuFitness(){
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


  // public ManagementIndv explorBest() throws Exception {
  //   /** explore mutation of the individual (+/-1 at each genes)
  //   return the best of them
  //   */
  //   ManagementIndv mut;
  //   double mut_fitness;
  //   double g;
  //   List<Double> genome = this.genome;
  //   double gene;
  //   ManagementIndv best_mut = this;
  //   double best_fitness = this.fitness;
  //   int i = 0;
  //   while(i < 3*this.inter_max){
  //     // mutation of the period
  //     // TO DO
  //     gene = genome.get(i);
  //     if(gene > 0){
  //       ++i;
  //       // mutation of type of thinning
  //       gene = genome.get(i);
  //       if(gene < 1.0d){
  //         g = genome.set(i, gene+0.5d);
  //         mut = new ManagementIndv(genome, this.simu_fitness);
  //         genome = this.genome;
  //         mut_fitness = mut.getFitness();
  //         if(mut_fitness>best_fitness){
  //           best_mut = mut;
  //           best_fitness = mut_fitness;
  //         }
  //       }
  //       if(gene > 0.0d){
  //         g = genome.set(i, gene-0.5d);
  //         mut = new ManagementIndv(genome, this.simu_fitness);
  //         genome = this.genome;
  //         mut_fitness = mut.getFitness();
  //         if(mut_fitness>best_fitness){
  //           best_mut = mut;
  //           best_fitness = mut_fitness;
  //         }
  //       }
  //       ++i;
  //       // mutation of basal area objective
  //       gene = genome.get(i);
  //       if(gene > 1.0d){ // in m2/ha
  //         if(gene < 30.0d){
  //           g = genome.set(i, gene+5.0d);
  //           mut = new ManagementIndv(genome, this.simu_fitness);
  //           genome = this.genome;
  //           mut_fitness = mut.getFitness();
  //           if(mut_fitness>best_fitness){
  //             best_mut = mut;
  //             best_fitness = mut_fitness;
  //           }
  //         }
  //         if(gene > 10.0d){
  //           g = genome.set(i, gene-5.0d);
  //           mut = new ManagementIndv(genome, this.simu_fitness);
  //           genome = this.genome;
  //           mut_fitness = mut.getFitness();
  //           if(mut_fitness>best_fitness){
  //             best_mut = mut;
  //             best_fitness = mut_fitness;
  //           }
  //         }
  //       }
  //       else{ // in %
  //         if(gene < 0.9d){
  //           g = genome.set(i, gene+0.1d);
  //           mut = new ManagementIndv(genome, this.simu_fitness);
  //           genome = this.genome;
  //           mut_fitness = mut.getFitness();
  //           if(mut_fitness>best_fitness){
  //             best_mut = mut;
  //             best_fitness = mut_fitness;
  //           }
  //         }
  //         if(gene > 0.7d){
  //           g = genome.set(i, gene-0.1d);
  //           mut = new ManagementIndv(genome, this.simu_fitness);
  //           genome = this.genome;
  //           mut_fitness = mut.getFitness();
  //           if(mut_fitness>best_fitness){
  //             best_mut = mut;
  //             best_fitness = mut_fitness;
  //           }
  //         }
  //       }
  //       i++;
  //     }
  //     else{
  //       i += 3;
  //     }
  //   }
  //   return best_mut;
  // }


  /* Private Functions */
  private List<Double> randomGenome(int inter_max){
    System.out.println("Choisir un génome aléatoire ");;
    //Integer id = new Integer(this.id);
    //long seed = id.longValue(); // Utiliser l'ID comme graine pour la reproductibilité, si nécessaire
    //Random r = new Random(seed);
    List<Double> random_genome = new ArrayList<>();
    int totalSize = 0;
    for (List<Double> list : Cf_k) {
            totalSize += 1;
    }
    List <Double> seqNull=new ArrayList<>();
    for (int i=0 ;i< 3+this.length;i++){
        seqNull.add(0.0d);
    }
    System.out.println("totalSize is :"+totalSize);

    for (int i = 0; i < inter_max; ++i) {
        int j = r.nextInt(nb_theta); // Sélection aléatoire de theta
         // nb_theta=4 docn j=0,1,2,3
      if (j>0) {
        for (int t = 0; t < j && i < inter_max; ++t) {
                // Ajouter des zéros pour les périodes sans intervention :
                //changer la liste pour ajouter la longueur pour 3+ length 

                random_genome.addAll(seqNull);
                //System.out.println("la période est :"+theta.get(j)+"donc on a "+j*8 +"zero");
                ++i;
        }
      }
        if (i < inter_max) {
            // Ajouter les trois gènes standards
            random_genome.add(theta.get(j)); // Période
            random_genome.add(tp.get(r.nextInt(nb_tp))); // Type de coupe
            random_genome.add(g_obj.get(r.nextInt(nb_g_obj))); // Objectif de surface basale
            //System.out.println("on tire aléatoirement theta:"+theta.get(j)+"tp:"+tp.get(r.nextInt(nb_tp)+);
            // Sélectionner aléatoirement un quintuplet de Cf_k et l'ajouter
            int k=r.nextInt(totalSize);
            List<Double> selectedQuintuplet = Cf_k.get(k);
            System.out.println("on tire aleatoirement theta: "+theta.get(j)+" tp: "+tp.get(r.nextInt(nb_tp))+" g_obj: "+g_obj.get(r.nextInt(nb_g_obj))+" Cf: "+Cf_k.get(k));
            random_genome.addAll(selectedQuintuplet);
        }
    }
    return random_genome;
}

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


}