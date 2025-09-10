
package forceps.myscripts.michelle;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import forceps.myscripts.michelle.ManagementPop;

/**
 * Class to apply a genetic algorithm to management.
 *
 * @author M. Malara - July 2023 & M. Jourdan - March 2024 & change by loubna April 2024
 */

public class GeneticAlgo{
  /** class to apply a genetic algorithm to management
   use ManagementPop for generation and ManagementIndv for individuals
  */
   
   private String setup_file;
   private String site_file;
   private String climate_file;
   private String inventory_file;
   private String potential_species;
   private String trait_potentiel_species;
   private String output_dir;  // name of the directory where the results will be saved
   private SimulationFitness simu_fitness;
   private int best_to_parent;       // number of the best individuals who automatically survive
 	 private double proba_mutation;    // probability of a gene having a mutation
   private int rand_indv;            // number of random individuals added at each generation
   private int stop_crit;            // stopping criteria
   private int inter_max;            // maximum number of interventions
   private int generation_cnt;       // number of current generation
   private ManagementPop pop;        // current generation
   private ManagementIndv best_indv; // best individual of the current generation
   private ManagementPop viab_indv;  // all viable individuals calculated during the simulation
   private static final long SEED = 12345L;


   /* Costructor */
   public GeneticAlgo(int indv_first_pop, int best_to_parent, double proba_mutation,
                      int rand_indv, int stop_crit, int inter_max,
                      String output_dir, SimulationFitness simu_fitness)
                      throws Exception {
     /** Costructor
      int indv_first_pop : number of individuals generated at the start of the simulation
      int best_to_parent : number of the best individuals who automatically survive
      int rand_indv : number of random individuals added at each generation
      int stop_crit : stopping criteria, number of years of stagnation of best fitness
      int inter_max : maximum number of interventions (ManagementIndv parameter)
      int simu_fitness : number of simulations to fitness calculation (ManagementIndv parameter)
      String output_dir : name of the directory where the results will be saved
     */
     // algorithm parameters
     this.best_to_parent = best_to_parent;
     this.proba_mutation = proba_mutation;
     this.rand_indv = rand_indv;
     this.stop_crit = stop_crit;
     // indv parameters
     this.inter_max = inter_max;
     this.simu_fitness = simu_fitness;
     // directory use
     this.output_dir = output_dir;
     // algorithm variables
     this.generation_cnt = 0;
     //evaluatePopulationParallel();
     this.pop = new ManagementPop(indv_first_pop, inter_max, simu_fitness);
     //evaluatePopulationParallel();
     System.out.println("on a creer une generation de "+pop.toString());
     // creation file for result
     try{
        //add file for execution Time :
        
    	 String fileName = this.output_dir + "/evolFitness.txt";
       File file = new File(fileName);
       if (!file.getParentFile().exists())
         file.getParentFile().mkdirs();
       if (!file.exists())
         file.createNewFile();
       FileWriter fw = new FileWriter(file);
       String heading = "# setup file = "+ setup_file
                +"\n# site file = "+site_file
                +"\n# climate file = "+climate_file
                +"\n# inventory file = "+inventory_file
                +"\n# species list = "+potential_species
                +"\n# stopping criteria = "+this.stop_crit
                +"\n# best survivors = "+this.best_to_parent
                +"\n# mutation probability = "+this.proba_mutation
                +"\n# random individuals = "+this.rand_indv
                +"\n# number of interventions = "+this.inter_max;
       if(this.simu_fitness.getRiskAversion()){
         heading += "\n# risk aversion \n";
       }
       else{
         heading += "\n# average \n";
       }
       fw.write(simu_fitness.toString());
       fw.write(heading + "\n\n#Generation	Individual	Fitness\n");
       fw.close();
       fileName = this.output_dir + "/allViabIndv.txt";
       file = new File(fileName);
       if (!file.getParentFile().exists())
         file.getParentFile().mkdirs();
       if (!file.exists())
         file.createNewFile();
       fw = new FileWriter(file);
       fw.write(heading);
       fw.close();
     }
     catch (IOException e) { e.printStackTrace(); }
     // clean pop : this.viab and this.best_indv
     this.cleanPop();

   }

   private void writeExecutionTimes() {
    try {
        FileWriter execTimeFw = new FileWriter(this.output_dir + "/execution_times.txt", true);
        for (ManagementIndv indv : this.pop.getIndividuals()) {
            execTimeFw.write("Id: " + indv.getId() + "\t" + indv.getExecution() + " ns\t" + (indv.getExecution() / Math.pow(10, 9)) + " s\t" + indv.getFitness() + "\n");
        }
        execTimeFw.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
   /* Printer */
   @Override
   public String toString(){
     return("Generation " + this.generation_cnt + ". " + this.pop.toString());
   }


   /* Getter */
   public ManagementIndv getBestIndv(){
     /** return the best individual calculated since the beginning
     */
     return this.viab_indv.getBestIndv();
   }


   public ManagementIndv getCurrentBestIndv(){
     /** return the best individual of the current generation
     */
     return this.best_indv;
   }


   /* Public Functions */
   public ManagementPop runAlgo(int max_cnt) throws Exception {
     /** run the entire genetic algorithm
     return all viable individuals calculated during simulation
     */
    System.out.println("this pop is :"+pop);
    System.out.println("runner GA avec :"+max_cnt+"d'iteration");
     int security_cnt = 0;    // if stop criteria never reached
     int stagnation_cnt = 0;
     double save_fitness;
     System.out.println(this.toString());
     while(this.pop.getSize() > 2 && stagnation_cnt < this.stop_crit && security_cnt < max_cnt){
       ++security_cnt;
       //evaluatePopulationParallel();
       save_fitness = this.pop.getBestFitness();
       this.newGeneration();
       if(this.pop.getBestFitness() == save_fitness){
         ++stagnation_cnt;
       }
       else{
         stagnation_cnt = 0;
       }
     }
      try{
        // write end
       FileWriter fw = new FileWriter(this.output_dir + "/allViabIndv.txt");
       if(security_cnt == max_cnt){
         System.out.println("WARNING : safety criteria " + max_cnt + " reached.");
         fw.write("# WARNING : safety criteria " + max_cnt + " reached.\n");
       }
       if(this.pop.getSize() == 2){
         System.out.println("WARNING : the population has gone extinct.");
         fw.write("# WARNING : the population has gone extinct.\n");
       }
       else{
         System.out.println("Stop criteria reached.");
         fw.write("# The population has reached the stop criterion in "+
                  this.generation_cnt+" generation.\n");
       }
       System.out.println(this.toString());
       System.out.println("All viable individuals.");
       System.out.println(this.viab_indv.toString());
       fw.write("# " + this.viab_indv.toString() + " Best indvidiual = " +
                this.best_indv.getId() + ".\n# Genome :" + this.best_indv.genomeString() +
                "\n\n# Id Fitness Genome \n");
       for(ManagementIndv indv : this.viab_indv.getIndividuals()){
         fw.write(indv.getId() + "	" + indv.getFitness() + "	" + indv.genomeString() + "\n");


       
       }
       fw.close();
       // save best_individu_evolConstraints.txt
       File src = new File(this.output_dir + "/individu"+this.best_indv.getId()+"_evolConstraints.txt");
       File dest = new File(this.output_dir + "/best_individu_evolConstraints.txt");
       Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
     }
     catch (IOException e) { e.printStackTrace(); }
     return this.viab_indv;
   }


/* Private Functions */
   private void newGeneration() throws Exception {
     /** calculate a new generation for an iteration of the algorithm
     modify pop, generation_cnt and viab_indv
     */
    //writeExecutionTimes();
    System.out.println("on a cree une nouvelle generation la voici :");
    //System.out.println("Voici la pop  d:"+this.pop);
    ++this.generation_cnt;
    ManagementIndv save_best = this.best_indv;
    this.selection();           // select parents
    this.crossover();           // calculate children and do mutation
    this.explorBest(save_best); // add best offspring of best parent
    this.addRandomIndv();       // add random individuals in pop
    this.cleanPop(); 
    
           // avoid non viable indv, update viab_indv, update best and writes fitness to the out file
    if(this.generation_cnt%1000==0){
      System.out.println(this.toString());
    }
    System.out.println("Voici la pop  apres GA  :"+this.pop+"de "+this.generation_cnt);
   }


   private void selection(){
     /** select individuals to be parents
     modify pop
     */
     this.pop.selection(this.best_to_parent);
   }


   private void crossover() throws Exception {
     /** mixe genes to calculate children = new population and applie a mutation
         to genes with a probability of proba_mutation
     modify pop
     */
     ManagementPop child_pop = new ManagementPop(0, this.inter_max, this.simu_fitness);
     // randomly select parent pairs
     this.pop.shuffle();
     for(int i = 0; i < this.pop.getSize()-1; i += 2){
       child_pop.crossover(this.pop.getIndividu(i), this.pop.getIndividu(i+1), this.proba_mutation);
     }  
     List<ManagementIndv> child_tmp=new ArrayList<>();
     for (int i = 0; i < child_pop.getSize(); i += 1){
      
      child_tmp.add(child_pop.getIndividu(i));
     }
     System.out.print("Voic la pop tmp :"+child_tmp);
     ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());//les coeurs 
     List<Future<Double>> futures = new ArrayList<>();
     for (ManagementIndv ind :child_tmp ) {
      System.out.println("Parallelisation  crossover :"+ind.getId());
        futures.add(executor.submit(() -> ind.calcFitness(false)));
      }
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.HOURS);
    System.out.println("Finir crossover :");
    for (Future<Double> future : futures) {

     try {
      double fitness = future.get();
      ManagementIndv indv = child_pop.getIndividu(futures.indexOf(future));
      indv.setFitness(fitness);
     //ajouter cette individu à this.pop
     
    }catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
    }
    ManagementPop new_pop=new ManagementPop(child_tmp);
     this.pop = new_pop;
     System.out.println("Voic la pop after Crossover:"+this.pop);;
   }


   private void explorBest(ManagementIndv best_indv) throws Exception {
    System.out.println("Commencer l'exploration du best ");
    ManagementIndv mut;
    List<Double> genome = best_indv.getGenome();
    double best_fitness = best_indv.getFitness();
    List<ManagementIndv> pop_exploreBest = new ArrayList<>();
    double gene;
    int length = best_indv.getLength();
    Random rn = new Random(SEED);
    ManagementIndv best_mut = best_indv;
    for (int i = 0; i < genome.size(); i += 3 + length) {
        // Vérification des séquences nulles
        boolean genome_seq_null = isNullserie(genome, i, 3 + length);
        if (genome_seq_null) {
            continue;
        }

        // Mutation du type d'éclaircie (tp)
        gene = genome.get(i + 1);
        if (gene < 1.0d) {
            List<Double> new_genome = new ArrayList<>(genome);
            new_genome.set(i + 1, Math.min(1.0d, gene + 0.5d));
            mut = new ManagementIndv(new_genome, this.simu_fitness, false);
            System.out.println("Genome créé avec mutation tp: " + new_genome);
            pop_exploreBest.add(mut);
        }
        if (gene > 0.01d) {
            List<Double> new_genome = new ArrayList<>(genome);
            new_genome.set(i + 1, Math.max(0.0d, gene - 0.5d));
            mut = new ManagementIndv(new_genome, this.simu_fitness, false);
            System.out.println("Genome créé avec mutation tp: " + new_genome);
            pop_exploreBest.add(mut);
        }

        // Mutation de l'objectif de surface basale (G_obj)
        gene = genome.get(i + 2);
        if (gene < 0.8d) {
            List<Double> new_genome = new ArrayList<>(genome);
            double input = Math.min(0.9d, gene + 0.1d);
            new_genome.set(i + 2, Math.round(input * 10) / 10.0);
            mut = new ManagementIndv(new_genome, this.simu_fitness, false);
            System.out.println("Genome créé avec mutation G_obj: " + new_genome);
            pop_exploreBest.add(mut);
        } else if (gene > 0.7d) {
            List<Double> new_genome = new ArrayList<>(genome);
            double input = Math.max(0.7d, gene - 0.1d);
            new_genome.set(i + 2, Math.round(input * 10) / 10.0);
            mut = new ManagementIndv(new_genome, this.simu_fitness, false);
            System.out.println("Genome créé avec mutation G_obj: " + new_genome);
            pop_exploreBest.add(mut);
        }

        // Mutation de la composition (CF)
        int j1 = rn.nextInt(length);
        int j2 = rn.nextInt(length);
        Double x1 = genome.get(i + 3 + j1);
        Double x2 = genome.get(i + 3 + j2);

        if (j1 != j2) {
            System.out.println("Indice de la période est: " + i + " et de J1: " + j1);
            System.out.println("Indice de la période est: " + i + " et de J2: " + j2);
            System.out.println("On fait la mutation d'explore best sur la composition C_fk pour les indices :" + (i + 3 + j1) + " de gene " + genome.get(i + 3 + j1) + " et d'indice " + (i + 3 + j2) + " et de gene " + genome.get(i + 3 + j2) + " de genome " + genome);
            List<Double> new_genome = new ArrayList<>(genome);
            if (x1 > 10 && x2 < 100) {
                new_genome.set(i + 3 + j1, x1 - 10);
                new_genome.set(i + 3 + j2, x2 + 10);
            } else if (x1 < 100 && x2 > 10) {
                new_genome.set(i + 3 + j1, x1 + 10);
                new_genome.set(i + 3 + j2, x2 - 10);
            }
            System.out.println("La mutation est en composition de genome est: " + new_genome);
            mut = new ManagementIndv(new_genome, this.simu_fitness, false);
            System.out.println("Voici le genome cree CF: " + mut.getGenome());
            pop_exploreBest.add(mut);
        } else {
            System.out.println("On ne peut pas muter C_fk car: " + j1 + " = " + j2);
        }
    }

    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    List<Future<Double>> futures = new ArrayList<>();

    for (ManagementIndv ind : pop_exploreBest) {
        futures.add(executor.submit(() -> ind.calcFitness(false)));
    }
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.HOURS);

    for (Future<Double> future : futures) {
        try {
            double fitness = future.get();
            ManagementIndv indv = pop_exploreBest.get(futures.indexOf(future));
            indv.setFitness(fitness);
            if (fitness > 0.0d) {
                this.viab_indv.addToPop(indv);
                System.out.println("Genome ajouté à viable: " + indv.getGenome());
            }
            if (fitness > best_fitness) {
                best_mut = indv;
                best_fitness = fitness;
                System.out.println("Best mutation mise à jour: " + best_mut.getGenome());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    this.pop.addToPop(best_mut);
}



  private boolean isNullserie(List<Double> parent,int start,int length){
      for (int i=start;i<start+length;i++) {
        if (parent.get(i)!=0) return false;
    
      }
      return true ;
    }
    
   private void addRandomIndv() throws Exception {
     /** add new random individuals in population
        modify pop
     */
    List <ManagementIndv> ind_rand_tmp=new ArrayList<>();
     for(int i = 0; i < this.rand_indv; ++i){
          System.out.println("Creer des indivdus aleatoire de nb:"+rand_indv);;
          ManagementIndv indv = new ManagementIndv(this.inter_max, this.simu_fitness);
          ind_rand_tmp.add(indv);
          //indv.setFitness(indv.calcFitness(false));
          
          //this.pop.addToPop(indv);
     }
     //calculer la fitness des individus sur plusieurs coeurs du processeur :
     ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());//les coeurs 
     List<Future<Double>> futures = new ArrayList<>();
     for (ManagementIndv ind : ind_rand_tmp) {
      System.out.println("On commence le calcule de la fitness sur plusieurs coeurs de la pop aléatoire ");
      futures.add(executor.submit(() -> ind.calcFitness(false)));
     }
     System.out.println("On attend la fin du calcul de la fitness sur plusieurs coeurs de la pop aléatoire ");
     executor.shutdown();
    executor.awaitTermination(1, TimeUnit.HOURS);
    System.out.println("On a fini le calcul de la fitness sur plusieurs coeurs de la pop aléatoire ");
    //ajouter les individus  cette population :
    for (Future<Double> future : futures) {
      try {
      ManagementIndv indv = ind_rand_tmp.get(futures.indexOf(future));
      indv.setFitness(future.get());
      this.pop.addToPop(indv);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      }
    }
    System.out.println("On a fini d'ajouter les individus  cette population :");

   }


   private void cleanPop() {
     /** update the list of viable individuals and removes non-viable
         individuals from the population, update best individu and its fitness,
         writes fitness to the out file
     modify pop, viab_indv, best_indv and best_fitness
     */
     // avoid non viable indv, update best and writes fitness to the out file
     System.out.println("Voici la pop  avant clean :"+this.pop);
     this.pop.updatePop(this.generation_cnt, this.output_dir);
     System.out.println("Voici la pop  apres update :"+this.pop);
     System.out.println("Voici la pop  apres update et  viable  :"+this.viab_indv);
     // and update viab_indv
     if (this.generation_cnt == 0){
       // first call, creation of viab_indv
       this.viab_indv = new ManagementPop(this.pop.getIndividuals());
       if (this.pop.getSize() == 0){
         // test if ther is viab individuals
         System.err.println("No viable individuals in initial generation");
         System.exit(1);
       }
     }
     else{
      this.viab_indv.addToPop(this.pop);
       // test if ther is viab individuals
       if (this.pop.getSize() == 0){
         System.err.println("No viable individuals in generation "+this.generation_cnt);
         System.exit(1);
       }
     }
     this.best_indv = this.pop.getBestIndv();
     System.out.println("Individu viable de GA  :"+this.viab_indv);
   }


}
