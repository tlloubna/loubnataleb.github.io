
package forceps.myscripts.LM_sequentielle;



import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import forceps.myscripts.LM_sequentielle.ManagementPopSeq;
import forceps.myscripts.michelle.ManagementIndv;
import forceps.myscripts.michelle.ManagementPop;
import forceps.myscripts.LM_sequentielle.ManagementIndvSeq;

/**
 * Class to apply a genetic algorithm to management.
 *
 * @author Malara (July 2023) _Jourdan(March 2024) _TALEB (March-April 2024)
 */

public class GeneticAlgoSeq{
  /** class to apply a genetic algorithm to management
   use ManagementPop for generation and ManagementIndv for individuals
  */
   
   private String setup_file;
   private String site_file;
   private String climate_file;
   private String inventory_file;
   private String potential_species;
   private String manage_specie;
   private String output_dir;  // name of the directory where the results will be saved
   private SimulationFitnessSeq simu_fitness;
   private int best_to_parent;       // number of the best individuals who automatically survive
 	 private double proba_mutation;    // probability of a gene having a mutation
   private int rand_indv;            // number of random individuals added at each generation
   private int stop_crit;            // stopping criteria
   private int inter_max;            // maximum number of interventions
   private int generation_cnt;       // number of current generation
   private ManagementPopSeq pop;        // current generation
   private ManagementIndvSeq best_indv; // best individual of the current generation
   private ManagementPopSeq viab_indv;  // all viable individuals calculated during the simulation
   

   /* Costructor */
   //Creer tous les fichiers + Initialiser la premiere population 
   public GeneticAlgoSeq(int indv_first_pop, int best_to_parent, double proba_mutation,
                      int rand_indv, int stop_crit, int inter_max,
                      String output_dir, SimulationFitnessSeq simu_fitness)
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
     //Pour calculer le temps de chaque pop.
     long startTime,endTime;
		 startTime = System.nanoTime();
     String execTimeFileName=this.output_dir+"/Execution_timePop.txt";
		 File execTimeFile=new File(execTimeFileName);
		 if (!execTimeFile.getParentFile().exists()) 
		  execTimeFile.getParentFile().mkdirs();
		if(!execTimeFile.exists()) 
		execTimeFile.createNewFile();
		FileWriter execTimeFw=new FileWriter(execTimeFile);
		execTimeFw.write("#Pop\t Nb_Ind\t  Execution (s)\t Best_fitness \n");
		
    //the first pop :
     this.pop = new ManagementPopSeq(indv_first_pop, inter_max, simu_fitness);
     endTime =System.nanoTime();
     execTimeFw.write(this.generation_cnt+"\t"+this.pop.getSize()+"\t"+(endTime-startTime)/Math.pow(10,9)+"\t"+this.pop.getBestFitness()+"\n");
     
     execTimeFw.close();
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

   
   /* Printer */
   @Override
   public String toString(){
     return("Generation " + this.generation_cnt + ". " + this.pop.toString());
   }


   /* Getter */
   public ManagementIndvSeq getBestIndv(){
     /** return the best individual calculated since the beginning
     */
     return this.viab_indv.getBestIndv();
   }


   public ManagementIndvSeq getCurrentBestIndv(){
     /** return the best individual of the current generation
     */
     return this.best_indv;
   }


   // Lancer Ga Tant que le critètre de stagnation n'est pas atteint et qu'on fait 1000 itérations 
   public ManagementPopSeq runAlgo(int max_cnt) throws Exception {
     /** run the entire genetic algorithm
     return all viable individuals calculated during simulation
     */
 
     int security_cnt = 0;    // if stop criteria never reached
     int stagnation_cnt = 0;
     double save_fitness;
   
     while(this.pop.getSize() > 2 && stagnation_cnt < this.stop_crit && security_cnt < max_cnt){
       ++security_cnt;
       
       save_fitness = this.pop.getBestFitness();
       this.newGeneration();//Lancer Crossover -Mutation -exploitation du meilleur -AddrandIndividus =>generations cnt +1 
       if(this.pop.getBestFitness() == save_fitness){//Après 5 genérations où save_fitness ne varie pas , on s'arrète
         ++stagnation_cnt;
       }
       else{
         stagnation_cnt = 0;
       }
     }
      try{
        // On récupère tous les individus avec leurs fitness à la fin de de GA 
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
       for(ManagementIndvSeq indv : this.viab_indv.getIndividuals()){
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
    
    FileWriter fw = new FileWriter(this.output_dir + "/Execution_timePop.txt",true);
    long startTime,endTime;
		startTime = System.nanoTime();
    ++this.generation_cnt;
    ManagementIndvSeq save_best = this.best_indv;
    this.selection();           // select parents
    this.crossover();           // calculate children and do mutation
    this.explorBest(save_best); // add best offspring of best parent
    this.addRandomIndv();       // add random individuals in pop
    endTime =System.nanoTime();
    fw.write(this.generation_cnt+"\t"+this.pop.getSize()+"\t"+(endTime-startTime)/Math.pow(10,9)+"\t"+this.pop.getBestFitness()+"\n");
     
    fw.close();
    this.cleanPop(); 
    System.gc();
    if(this.generation_cnt%1000==0){
      System.out.println(this.toString());
    }
    
    
   }

  private void selection(){
        /** select individuals to be parents
         modify pop
        */
        this.pop.selection(this.best_to_parent);
  }


private void crossover() throws Exception {
    ManagementPopSeq child_pop = new ManagementPopSeq(0, this.inter_max, this.simu_fitness);
     // randomly select parent pairs
     this.pop.shuffle();
     for(int i = 0; i < this.pop.getSize()-1; i += 2){
       child_pop.crossover(this.pop.getIndividu(i), this.pop.getIndividu(i+1), this.proba_mutation);
     } // add le dernier ?
     // update
     this.pop = child_pop;
    
    
}

//exploitation du meilleur  : On cree plusiers genomes à partir du premier :
//On le fait sur le meilleur individus 
   private void explorBest(ManagementIndvSeq best_indv) throws Exception {
    System.out.println("Commencer l'exploration du best ");
    ManagementIndvSeq mut;
    double mut_fitness;
    List<Double> genome = best_indv.getGenome();
    double best_fitness = best_indv.getFitness();
    List<Double> genome_save = best_indv.getGenome();
    double gene;
    int Nb_esp = best_indv.getNb_esp();
    Random rn = new Random(simu_fitness.getSeed());
    ManagementIndvSeq best_mut = best_indv;
    for (int i = 0; i < genome.size(); i += 3 + Nb_esp) {
        // Vérification des séquences nulles
        boolean genome_seq_null = isNullserie(genome, i, 3 + Nb_esp);
        if (genome_seq_null) {
            continue;
        }

        // Mutation du type d'éclaircie (tp)
        gene = genome.get(i + 1);
        if (gene < 1.0d) {

            List<Double> new_genome = new ArrayList<>(genome);
            new_genome.set(i + 1, Math.min(1.0d, gene + 0.5d));
            mut = new ManagementIndvSeq(new_genome, this.simu_fitness, false);
            genome = genome_save;
           mut_fitness = mut.getFitness();
           if(mut_fitness > 0.0d){
            
             this.viab_indv.addToPop(mut);
           }
           if(mut_fitness>best_fitness){
             best_mut = mut;
             best_fitness = mut_fitness;
           }

        }
        if (gene > 0.01d) {

            List<Double> new_genome = new ArrayList<>(genome);
            new_genome.set(i + 1, Math.max(0.0d, gene - 0.5d));
            mut = new ManagementIndvSeq(new_genome, this.simu_fitness, false);
            
            genome = genome_save;
           mut_fitness = mut.getFitness();
           if(mut_fitness > 0.0d){
            
             this.viab_indv.addToPop(mut);
           }
           if(mut_fitness>best_fitness){
            
            best_mut = mut;
            best_fitness = mut_fitness;
          }

        }

        // Mutation de l'objectif de surface basale (G_obj)
        gene = genome.get(i + 2);
        if (gene < 0.8d) {

            List<Double> new_genome = new ArrayList<>(genome);
            double input = Math.min(0.9d, gene + 0.1d);
            new_genome.set(i + 2, Math.round(input * 10) / 10.0);
            mut = new ManagementIndvSeq(new_genome, this.simu_fitness, false);
            
            genome = genome_save;
             mut_fitness = mut.getFitness();
             if(mut_fitness > 0.0d){
               this.viab_indv.addToPop(mut);
             }
             if(mut_fitness>best_fitness){
               best_mut = mut;
               best_fitness = mut_fitness;
              }

        } else if (gene > 0.7d) {

            List<Double> new_genome = new ArrayList<>(genome);
            double input = Math.max(0.7d, gene - 0.1d);
            new_genome.set(i + 2, Math.round(input * 10) / 10.0);
            mut = new ManagementIndvSeq(new_genome, this.simu_fitness, false);
            genome = genome_save;
              mut_fitness = mut.getFitness();
              if(mut_fitness > 0.0d){
                this.viab_indv.addToPop(mut);
              }
              if(mut_fitness>best_fitness){
                best_mut = mut;
                best_fitness = mut_fitness;
              }
        }

        // Mutation de la composition (CF)
        int j1 = rn.nextInt(Nb_esp);
        int j2 = rn.nextInt(Nb_esp);
        Double x1 = genome.get(i + 3 + j1);
        Double x2 = genome.get(i + 3 + j2);

        if (j1 != j2) {
          List<Double> new_genome = new ArrayList<>(genome);
          if (x1 > 0 && x2 < 100) {
            new_genome.set(i + 3 + j1, x1 - 10);
            new_genome.set(i + 3 + j2, x2 + 10);
        } else if (x1 < 100 && x2 > 0) {
            new_genome.set(i + 3 + j1, x1 + 10);
            new_genome.set(i + 3 + j2, x2 - 10);
        }
            mut = new ManagementIndvSeq(new_genome, this.simu_fitness, false);
            genome = genome_save;
            mut_fitness = mut.getFitness();
            if(mut_fitness > 0.0d){
              this.viab_indv.addToPop(mut);
            }
            if(mut_fitness>best_fitness){
              best_mut = mut;
              best_fitness = mut_fitness;
            }
        }
    }
    this.pop.addToPop(best_mut);
    
  }

  

  private boolean isNullserie(List<Double> parent,int start,int Nb_esp){
      for (int i=start;i<start+Nb_esp;i++) {
        if (parent.get(i)!=0) return false;
    
      }
      return true ;
    }
    
   private void addRandomIndv() throws Exception {
     /** add new random individuals in population
        modify pop
     */
    for(int i = 0; i < this.rand_indv; ++i){
      ManagementIndvSeq indv = new ManagementIndvSeq(this.inter_max, this.simu_fitness);
      System.out.println("Creer des indivdus aleatoire de nb:"+rand_indv);;
      this.pop.addToPop(indv);
 }
        

   }


   private void cleanPop() {
     /** update the list of viable individuals and removes non-viable
         individuals from the population, update best individu and its fitness,
         writes fitness to the out file
     modify pop, viab_indv, best_indv and best_fitness
     */
     // avoid non viable indv, update best and writes fitness to the out file
    
     this.pop.updatePop(this.generation_cnt, this.output_dir);
     
     // and update viab_indv
     if (this.generation_cnt == 0){
       // first call, creation of viab_indv
       this.viab_indv = new ManagementPopSeq(this.pop.getIndividuals());
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
    
   }


}