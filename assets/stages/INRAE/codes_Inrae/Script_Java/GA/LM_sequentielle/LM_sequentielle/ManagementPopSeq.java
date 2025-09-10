
package forceps.myscripts.LM_sequentielle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.Math;


import forceps.myscripts.LM_sequentielle.ManagementIndvSeq;
import forceps.myscripts.michelle.ManagementIndv;

/**
 *La classe est crée pour gérer la population dans GA . population :est l'ensemble des itinéraires sylvicoles(individus)
 *
 * @author M. Malara - July 2023 & M. Jourdan - March 2024 & loubna 2024
 */

public class ManagementPopSeq{  
  /** class grouping a set of individuals in population for genetic algorithm
   use ManagementIndv for individuals
  */

  private List<ManagementIndvSeq> individuals; // list of individuals in the population
  private List<Integer> list_id;            // list of individual identifiers
  private int size;                         // population size, number of individuals in the population
  private SimulationFitnessSeq simu_fitness;
  private ManagementIndvSeq best_indv;         // best individual in population
  private double best_fitness;               // interest score of best individual

   
  /* Costructor */
    public ManagementPopSeq(int nb_indv, int inter_max, SimulationFitnessSeq simu_fitness) throws Exception {
     
    List<ManagementIndvSeq> individuals = new ArrayList<ManagementIndvSeq>();
      List<Integer> list_id = new ArrayList<Integer>();
      ManagementIndvSeq indv;
      double indv_fitness;
      ManagementIndvSeq best_indv =  new ManagementIndvSeq();
      double best_fitness = 0.0d;
      for (int i = 0; i < nb_indv; ++i){
        // create random individuals
        indv = new ManagementIndvSeq(inter_max, simu_fitness);
        individuals.add(indv);
        list_id.add(indv.getId());
        // cheak fitness
        indv_fitness = indv.getFitness();
        if(indv_fitness > best_fitness){
          best_fitness = indv_fitness;
          best_indv = indv;
        }
      }
      this.simu_fitness = simu_fitness;
      this.individuals = individuals;
      this.list_id = list_id;
      this.size = nb_indv;
      this.best_fitness = best_fitness;
      if(nb_indv > 0){ // else there no best_indv
      this.best_indv = best_indv;
      }
    
    
    }


    public ManagementPopSeq (ManagementIndvSeq indv){
      /** create a population composed of a single individual
      ManagementIndv indv : the only individual in the population
      */
      //Individus vide
      List<ManagementIndvSeq > individuals = new ArrayList<ManagementIndvSeq >();
      //Id vides
      List<Integer> list_id = new ArrayList<Integer>();
     //Ajouter l'individus à la population 
       individuals.add(indv);
      list_id.add(indv.getId());
      //Modifier les nouveaux valeurs pour chaque Seq ramètres d'individus.
      this.simu_fitness = indv.getSimuFitness();
      this.individuals = individuals;
      this.list_id = list_id;
      this.size = 1;
      this.best_fitness = indv.getFitness();
      this.best_indv = indv;
    }


    public ManagementPopSeq (List<ManagementIndvSeq > individuals){
      /** create a population composed of a set of individuals
      List<ManagementIndv> individuals : set of individuals
      */
      //J'ai Seq s utilisé le Seq rallélisme ici car il faut juste enregistrer les données et ne Seq s calculer.
      List<Integer> list_id = new ArrayList<Integer>();
      double indv_fitness;
      ManagementIndvSeq best_indv =  new ManagementIndvSeq ();
      double best_fitness = 0.0d;
      for (ManagementIndvSeq indv : individuals){
        // create random individuals
        list_id.add(indv.getId());
        // cheak fitness
        indv_fitness = indv.getFitness();
        if(indv_fitness > best_fitness){
          best_fitness = indv_fitness;
          best_indv = indv;
        }
      }
      this.simu_fitness = simu_fitness;
      this.individuals = individuals;
      this.list_id = list_id;
      this.size = individuals.size();
      this.best_fitness = best_fitness;
      this.best_indv = best_indv;
    }


  /* Printer */
  @Override
  public String toString(){
    return("Population of " + this.size + " individuals. Best fitness = " +
     this.best_fitness + ".");
  }


  /* Getter */
  public int getSize(){
    return this.size;
  }


  public List<ManagementIndvSeq > getIndividuals(){
    return this.individuals;
  }


  public ManagementIndvSeq getIndividu(int i){
    return this.individuals.get(i);
  }


  public double getBestFitness(){
    return this.best_fitness;
  }


  public ManagementIndvSeq getBestIndv(){
    return this.best_indv;
  }


  /* Public Functions */
  public void selection(int best_to_parent){
    /** select best_to_parent individuals to be parents
    modify individuals and size
    * les parents sont sélectionné :
    *1 : On choisit les parents n_save :avec la meilleur fitness trièe dans l'ordre croissante et on récupère 
    *   les n_save meilleurs individus dernier 
    *2: on choisit les individis avec le critère qu'il soit probablement parent : sa probabilité > w in [0,1]
    */
    double fitness_to_parent = this.calcRankFitness(best_to_parent);
    int size_survivor = 0;

    List<ManagementIndvSeq> survivor = new ArrayList<ManagementIndvSeq>();

    List<ManagementIndvSeq> ns_vivor=new ArrayList<ManagementIndvSeq>();

    List<Integer> id_n=new ArrayList<Integer>();

    List<Integer> id_survivor = new ArrayList<Integer>();

    double indv_fitness;

    double p_survivor;
    //Fixer la graine de l'aléatoire pour la reproductibilité des résultats.
    Random rn = new Random(simu_fitness.getSeed());
    double rv;
    for(ManagementIndvSeq indv : this.individuals) {
      indv_fitness  = indv.getFitness();
      p_survivor = indv_fitness / this.best_fitness; // calc survival proba
      rv = rn.nextDouble(); // random variable
      if(rv <= p_survivor || indv_fitness >= fitness_to_parent){
        // the individual survive || is one of the best so is automaticaly save
        survivor.add(indv);
        id_survivor.add(indv.getId());
        ++size_survivor;
      }
      else {
        indv.cleanup();//we clean the generation of not used individuals.

      }
    }
    // update individuals and size
    this.individuals = survivor;
    this.size = size_survivor;
    this.list_id = id_survivor;
  }

  public void crossover(ManagementIndvSeq parent1, ManagementIndvSeq parent2,
                        double proba_mutation) throws Exception {
    /** un crossover un par un pour les genes non nulles et par paquts du mutilple de 8 
     * pour les zeros 
    */
    List<Double> genome1 = new ArrayList<Double>();//un enfant pour le 1 ier  couple de parents
    List<Double> genome2 = new ArrayList<Double>();//le deuxiéme enfant pour le 2 éme  couple de parents
    
    int Nb_esp=parent1.getNb_esp(); //les espèces 
    //initialiser la longueur du parents  :avec 3 espèces :il sera 16*6=96
    int cdt = parent1.getInterMax()*(3+Nb_esp); //pour 3 espèces c'est 96 pour 5 c'est 128 ((3+x)*16 où x :est le nombre des espèces )
    
    for (int i=0;i<cdt;i+=3+Nb_esp) {
      //Vérifier si l'un des sèquence pour les deux parents est nulle .
      boolean parent_1_is_null=isNullserie(parent1.getGenome(),i,3+Nb_esp);
      boolean parent_2_is_null=isNullserie(parent2.getGenome(), i, 3+Nb_esp);

      // crossover nulle 
      if (parent_1_is_null && parent_2_is_null) 
      { 
        //On fait rien les deux sont nulles 
        for (int j=0;j<(3+Nb_esp);j++){
          genome1.add(parent1.getGene(i+j));
          genome2.add(parent2.getGene(i+j));
        }
       
      }
      else if (parent_1_is_null){
        //parents 1 est nulles
        //On fait rien :le premier parent prend la première séquence et ainsi de suite pour le deuième 
       
        for (int j=0;j<(3+Nb_esp);j++){
          genome1.add(parent1.getGene(i+j));
          genome2.add(parent2.getGene(i+j));
          
        }
     
       
      }
      else if (parent_2_is_null){
        //parent 2 est nulles 
        
        for (int j=0;j<(3+Nb_esp);j++){
          genome1.add(parent1.getGene(i+j));
          genome2.add(parent2.getGene(i+j));
         
        }
        //On pourrai faire cette étape avec Or : || , c'est au moins l'un des deux prends , une séquence nulle 
        //on n'alterne pas point à point pour ne pas se tromper dans les intervalles.
        //Je l'ai fait au début comme ça pour des tests plus spècifiques.
      }else {
        //cross over normal un par un et par paquet de n espèces pour la composition 
        
        boolean pair = false;
        //Impair 
        for (int j=0;j<3;j++){
          if (j%2==0) {
            genome1.add(parent1.getGene(i+j));
            genome2.add(parent2.getGene(i+j));
          }else{
            genome1.add(parent2.getGene(i+j));
            genome2.add(parent1.getGene(i+j));
          }
        }
        
        //il faut le faire par triplet.
        if(pair){
          for (int j=3;j<(3+Nb_esp);j++){
            genome1.add(parent1.getGene(i+j));
            genome2.add(parent2.getGene(i+j));
          }
        }else{
          for (int j=3;j<(3+Nb_esp);j++){
          genome1.add(parent2.getGene(i+j));
          genome2.add(parent1.getGene(i+j));
        }
        
      }
        pair=!pair;
      }
      

      }
    
    // Une mutation sur l'un des gènes.
    genome1 = this.mutation(genome1, parent1.getAllTP(), parent1.getAllGobj(), proba_mutation,parent1.getAllCf_k());
    genome2 = this.mutation(genome2, parent1.getAllTP(), parent1.getAllGobj(), proba_mutation,parent1.getAllCf_k());
    // add in pop
    if(!parent1.sameGenome(genome1)){ // add child 1 in pop if it is different
      ManagementIndvSeq child1 = new ManagementIndvSeq(genome1, this.simu_fitness, false);
      this.individuals.add(child1);
      this.list_id.add(child1.getId());
      this.size += 1;
    }
    else{ // add parent in pop if not
      //Ajouter le parents si l'enfant 1 est le méme que parent 1 
      this.individuals.add(parent1);
      this.list_id.add(parent1.getId());
      this.size += 1;
    }
    if(!parent2.sameGenome(genome2)){ // add child 2 in pop if it is different
      //meme pour l'enfants 2
      ManagementIndvSeq child2 = new ManagementIndvSeq(genome2, this.simu_fitness, false);
      this.individuals.add(child2);
      this.list_id.add(child2.getId());
      this.size += 1;
    }
    else{ // add parent in pop if not
      this.individuals.add(parent2);
      this.list_id.add(parent2.getId());
      this.size += 1;
    }
  }
  //verifier que le genome est nulle sur une sequence 
private boolean isNullserie(List<Double> parent,int start,int Nb_esp){
  //Voir si la séquence de 3 + Nb_espèces (qu'on veut gerer dans trait_potentiel) est nulle
  for (int i=start;i<start+Nb_esp;i++) {
    if (parent.get(i)!=0) return false;

  }
  return true ;
}



  

/*
 * La mutation est faite pour bien explorer en "locale"  l'espace et creer de la divérsité dans la population.
 * On ne mute pas teta car ça va changer la structure de genome et des zeros 
 * On mute soit tp ,Gobj soit les Cfi de façon aléatoire 
 * pour les Cfi : on choisit un u_plet aléatoire dans le fichier que j'ai crée 
 */

  private List<Double> mutation(List<Double> genome, List<Double> all_tp,
      List<Double> all_Gobj, double proba_mutation,List<List<Double>> Cf_k){
      
      System.out.println("On commence la mutation:");
      //Fixer la graine pour la reproductibilité des résultats : tu peux l'enlever quand tu teste tous 
      //par exemple pour l'analyse de sentsibilité il faut mieux la fixer .mais pour comparer la fitness entre deux lancement du GA , il faut mieux ne pas la fixer
      Random rn = new Random(simu_fitness.getSeed());
      double rv;
      double gene;
      double g;
      int j;
      int j1;
      int j2;
      int i = 0;
      int nb_tp = all_tp.size();
      int nb_g_obj = all_Gobj.size(); 
      //ça depend des combinaisons , il y a un fichier en python Combinaisons.py qui cree ces combinaisons
      int Nb_esp=Cf_k.get(0).size();//equivalent au nombre des espèces.
      int totalSize= Cf_k.size();//c'est le fichier totale : ça dépend des combinaisons , par exemple 1001 pour 5 espèces et 121 pour 3 espèces dont {0,20,....100}ou bien {0,10,...100}
      while(i < genome.size()){
        //voir si la premiere sequence est nulle si oui pas de mutation

        boolean genome_seq_null=isNullserie(genome, i, 3+Nb_esp);
        if (genome_seq_null)
        {
          i+=3+Nb_esp;
          
        }
        else 
        {
          //On mute pas teta , on commence par tp
          i++;//--------------mutation tp 
          rv=rn.nextDouble();
          gene = genome.get(i);
          
          if(rv<proba_mutation)
          {
            //mutattion of type of thinning 
            j=rn.nextInt(nb_tp);
            g=genome.set(i, all_tp.get(j));
          }
          i++;//----------mutation G_obj
          rv=rn.nextDouble();
          gene = genome.get(i);
          if(rv<proba_mutation)
          {
              j=rn.nextInt(nb_g_obj);
              g=genome.set(i,all_Gobj.get(j));
          }
          i++;//---------------mutation composition 
          rv=rn.nextDouble();
          if(rv<proba_mutation)
          {
              j=rn.nextInt(totalSize);
              List<Double> list =Cf_k.get(j);//un upelet aléatoire 
              int k=i;
             
              //La mutation sur tous les u_plets
              for ( Double x :list )
              {
                g=genome.set(k,x);
                k++;
              
              }
          }
          i+=Nb_esp;//---------------Fin mutation composition
        }
        System.out.println("On a finit  la :"+i+"periode ");
      }
      return genome;
    }


   
    
      //Netoyage de la population courante :Supprimer les non viables 
      //et trouver l'individus avec la meilleur fitnss 
      public void updatePop(int generation_cnt, String output_dir){
        /** removes non-viable individuals from the population,
            update best individu and its fitness,
            writes fitness to the out file
        modify individuals, list_id, size, best_indv and best_fitness
        */
        try{
          FileWriter fw = new FileWriter(output_dir + "/evolFitness.txt", true);
          
          int size_viable = 0;
          List<Integer> list_id = new ArrayList<Integer>();
          List<ManagementIndvSeq> viable = new ArrayList<ManagementIndvSeq>();
          double indv_fitness;
          double current_best_fitness = 0.0d;
          ManagementIndvSeq current_best_indv = this.individuals.get(0);
          for(ManagementIndvSeq indv : this.individuals) {
            indv_fitness = indv.getFitness();
            if(indv_fitness > 0.0d){
              // viable
              viable.add(indv);
              list_id.add(indv.getId());
              ++size_viable;
              // writes fitness to the out file
              fw.write(generation_cnt + "	" + indv.getId() + "	" + indv_fitness + "\n");
              if(indv_fitness > current_best_fitness){
              // best
                current_best_fitness = indv_fitness;
                current_best_indv = indv;
              }
            }else {
              indv.cleanup();//Le supprimer pour vider la mémoire , aider le processeur a bien travailler 
            }
          }
          // update
          this.individuals = viable;
          this.list_id = list_id;
          this.size = size_viable;
          this.best_fitness = current_best_fitness;
          this.best_indv = current_best_indv;
          fw.close();
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }


  public void addToPop(ManagementPopSeq pop){
    /** add to the population any individuals from pop that are not yet present
    modify individuals, list_id, size, best_indv and best_fitness
    */
    int size_viable = 0;
    List<ManagementIndvSeq> viable;
    int indv_id;
    double indv_fitness;
    for(ManagementIndvSeq indv : pop.getIndividuals()) {
      indv_id = indv.getId();
      if(!this.list_id.contains(indv_id)){
        // add in pop if is it not
        this.individuals.add(indv);
        this.list_id.add(indv_id);
        ++this.size;
        indv_fitness = indv.getFitness();
        if(indv_fitness > this.best_fitness){
          // replace best
          this.best_fitness = indv_fitness;
          this.best_indv = indv;
        }
      }
    }
  }
  


  public void addToPop(ManagementIndvSeq indv){
    /** add to the population the individual if it is not yet present
    modify individuals, list_id, size, best_indv and best_fitness
    */
    //System.out.println("Ajouter indv a la pop");
    int size_viable = 0;
    List<ManagementIndvSeq> viable;
    int indv_id = indv.getId();
      if(!this.list_id.contains(indv_id)){
        // add in pop if is it not
        this.individuals.add(indv);
        this.list_id.add(indv_id);
        ++this.size;
        double indv_fitness = indv.getFitness();
        if(indv_fitness > this.best_fitness){
          // replace best
          this.best_fitness = indv_fitness;
          this.best_indv = indv;
        }
      }
      
  }


  public void shuffle(){
    /** shuffle individuals list
    */
    
    Collections.shuffle(this.individuals, new Random(simu_fitness.getSeed()));
  }


  /* Private Functions */
  private double calcRankFitness(int rank){ //Calculer la fitness de la N_save individus 
    /** find the "rank" best individual
    return the fitness of the "rank" th individual
    */
    List<Double> fitness_best =  new ArrayList<Double>();
    double indv_fitness;
    int i = 0;
    while (i < Integer.min(rank, this.size)){
      // recover of the first "rank" individuals and their fitness
      fitness_best.add(this.individuals.get(i).getFitness());
      ++i;
    }
    Collections.sort(fitness_best);
    while(i < this.size){
      // rest of individuals
      indv_fitness = this.individuals.get(i).getFitness();
      if(indv_fitness > fitness_best.get(0)){
        fitness_best.remove(0);
        fitness_best.add(indv_fitness);
        Collections.sort(fitness_best);
      }
      ++i;
    }
    return fitness_best.get(0);
  }


  private void updateFitness(){
    /** found the best fitness af all individuals
    modify best_indv and best_fitness
    */
    double current_best_fitness = 0;
    ManagementIndvSeq current_best_indv = this.individuals.get(0);
    double indv_fitness ;
    for(ManagementIndvSeq indv : individuals){
      indv_fitness = indv.getFitness();
      if(indv_fitness > current_best_fitness){
        current_best_fitness = indv_fitness;
        current_best_indv = indv;
      }
    }
    this.best_fitness = current_best_fitness;
    this.best_indv = current_best_indv;
  }

}