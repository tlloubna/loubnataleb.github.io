fichier<-file.choose()
data<-read.csv(fichier,header=TRUE)
library(ggplot2)
library(dplyr)
 
Calcul_distance <- function(taxon1, plot1, taxon2, plot2) {
  T1 <- data %>%
    filter(Taxon == taxon1 & plot == plot1)
  T2 <- data %>%
    filter(Taxon == taxon2 & plot == plot2)
  
  model1 <- loess(as.formula(paste("rel_cover", "~ year_trt")), data = T1)
  model2 <- loess(as.formula(paste("rel_cover", "~ year_trt")), data = T2)
  
  f1 <- predict(model1, newdata = data.frame(year_trt = T1$year_trt))
  f2 <- predict(model2, newdata = data.frame(year_trt = T2$year_trt))
  
  # Calcul de la différence
  max1 <- max(T1$year_trt)
  max2 <- max(T2$year_trt)
  borne <- min(max1, max2)
  somme <- 0
  diff1 <- numeric(length = borne)
  for (i in 1:borne) {
    diff1[i] <- abs(f2[i] - f1[i])
  }
  for (i in 1:(borne - 1)) {
    somme <- somme + diff1[i]
    #méthode de trapèze: somme <- somme + (T1$year_trt[i + 1] - T1$year_trt[i]) * (diff1[i + 1] + diff1[i]) / 2
  }
  return(somme)
}




#### OBJ 1 ####

### MATRICE 1
# Fonction pour calculer la matrice de distances entre toutes les paires de trajectoires d'un site et d'un traitement spécifiques
CalculerMatriceDistances <- function(data, site, traitement) {
  # Filtrer les données pour le site et le traitement spécifiques
  data_site_traitement <- data %>%
    filter(site_code == site & trt == traitement)
  
  # Sélectionne toutes les combinaisons uniques de (Taxon, plot) pour le site et le traitement spécifiques
  combinaisons <- unique(data_site_traitement[, c("Taxon", "plot")])
  
  # Initialise une matrice vide
  matrice_distances <- matrix(NA, nrow = nrow(combinaisons), ncol = nrow(combinaisons), 
                              dimnames = list(combinaisons$Taxon, combinaisons$Taxon))
  
  # Parcourir toutes les combinaisons de paires de taxons et de plots
  for (i in 1:nrow(combinaisons)) {
    for (j in 1:nrow(combinaisons)) {
      taxon1 <- combinaisons$Taxon[i]
      plot1 <- combinaisons$plot[i]
      taxon2 <- combinaisons$Taxon[j]
      plot2 <- combinaisons$plot[j]
      
      # Filtrer les données pour chaque paire de taxons et de plots
      data_taxon1_plot1 <- data_site_traitement %>%
        filter(Taxon == taxon1 & plot == plot1)
      data_taxon2_plot2 <- data_site_traitement %>%
        filter(Taxon == taxon2 & plot == plot2)
      
      # S'il y a des données pour les deux paires de taxons et de plots, calculer la distance
      if (nrow(data_taxon1_plot1) > 0 & nrow(data_taxon2_plot2) > 0) {
        # Calculer la distance entre les paires de trajectoires
        distance <- Calcul_distance(taxon1, plot1, taxon2, plot2)
        # Enregistrer la distance dans la matrice
        matrice_distances[i, j] <- distance
      }
    }
  }
  
  return(matrice_distances)
}

### MATRICE 2
# Fonction pour calculer la matrice de différences d'abondance initiale entre les paires de trajectoires d'un site et d'un traitement spécifiques
CalculerMatriceDifferencesAbondanceInitiale <- function(data, site, traitement) {
  data_site_traitement <- data %>%
    filter(site_code == site & trt == traitement)
  
  combinaisons <- unique(data_site_traitement[, c("Taxon", "plot")])
  
  matrice_differences <- matrix(NA, nrow = nrow(combinaisons), ncol = nrow(combinaisons), 
                                dimnames = list(combinaisons$Taxon, combinaisons$Taxon))
  
  for (i in 1:nrow(combinaisons)) {
    for (j in 1:nrow(combinaisons)) {
      taxon1 <- combinaisons$Taxon[i]
      plot1 <- combinaisons$plot[i]
      taxon2 <- combinaisons$Taxon[j]
      plot2 <- combinaisons$plot[j]
      
      # Filtrer les données pour chaque paire de taxons et de plots
      data_taxon1_plot1 <- data_site_traitement %>%
        filter(Taxon == taxon1 & plot == plot1)
      data_taxon2_plot2 <- data_site_traitement %>%
        filter(Taxon == taxon2 & plot == plot2)
      
      if (nrow(data_taxon1_plot1) > 0 & nrow(data_taxon2_plot2) > 0) {
        # Calculer la différence d'abondance initiale entre les paires de trajectoires
        #difference <- abs(data_taxon1_plot1$initial_rel_cover - data_taxon2_plot2$initial_rel_cover)
        difference <- abs(as.numeric(data_taxon1_plot1$initial_rel_cover[1]) - as.numeric(data_taxon2_plot2$initial_rel_cover[1]))
        matrice_differences[i, j] <- difference
      }
    }
  }
  
  return(matrice_differences)
}

### CORRELATION 
# Fonction pour calculer la corrélation
CalculerCorrelation <- function(matrice_distances, matrice_differences) {
  # Convertir les matrices en vecteurs
  vecteur_distances <- as.vector(matrice_distances)
  vecteur_differences <- as.vector(matrice_differences)
  
  # Calculer la corrélation entre les deux vecteurs
  correlation <- cor(vecteur_distances, vecteur_differences)
  
  return(correlation)
}


# Application:
filtre0<-subset(data, site_code == "arch.us" & trt == "NPK")

matrice1 <- CalculerMatriceDistances(data, "arch.us", "NPK")

matrice2 <- CalculerMatriceDifferencesAbondanceInitiale(data, "arch.us", "NPK")

correlation <- CalculerCorrelation(matrice1, matrice2)

print(matrice1)
print(matrice2)
print(correlation)
Tab_correlation1 <- function(data) {
  # Vecteurs pour stocker les résultats
  sites <- unique(data$site_code)
  traitements <- c("Control", "NPK", "Fence", "NPK+Fence")
  correlations <- c()  # Vecteur pour stocker les corrélations
  
  # Pour chaque site
  for (site in sites) {
    # Pour chaque traitement
    for (traitement in traitements) {
      # Calculer la corrélation
      matrice1 <- CalculerMatriceDistances(data, site, traitement)
      matrice2 <- CalculerMatriceDifferencesAbondanceInitiale(data, site, traitement)
      correlation <- CalculerCorrelation(matrice1, matrice2)
      
      # Stocker la corrélation dans le vecteur
      correlations <- c(correlations, correlation)
    }
  }
  
  # Créer un tableau de données avec les résultats
  Tab_correlation <- data.frame(
    Site = rep(sites, each = length(traitements)),
    Traitement = rep(traitements, times = length(sites)),
    Correlation = correlations
  )
  
  return(Tab_correlation)
}


#### OBJ 2 ####

CalculerMoyenneMedian <- function(matrice_distances) {
  diag(matrice_distances) <- NA
  vecteur_distances <- as.vector(matrice_distances)
  nl <- nrow(matrice_distances)
  
  moyenne <- mean(vecteur_distances, na.rm = TRUE)
  #moyenne <- moyenne*nl^2/(nl^2-nl)
  mediane <- median(vecteur_distances, na.rm = TRUE)
  
  return(list(moyenne = moyenne, mediane = mediane))
}

# Reprendre les matrices de distance construites au point 3
matrice_distances_control <- CalculerMatriceDistances(data, "arch.us", "Control")
matrice_distances_npk <- CalculerMatriceDistances(data, "arch.us", "NPK")
matrice_distances_fence <- CalculerMatriceDistances(data, "arch.us", "Fence")

matrice_distances_npk_fence <- CalculerMatriceDistances(data, "arch.us", "NPK+Fence")


# Calculer la moyenne et la médiane pour chaque traitement
resultats_control <- CalculerMoyenneMedian(matrice_distances_control)
resultats_npk <- CalculerMoyenneMedian(matrice_distances_npk)
resultats_fence <- CalculerMoyenneMedian(matrice_distances_fence)
resultats_npk_fence <- CalculerMoyenneMedian(matrice_distances_npk_fence)

# Comparer les valeurs de médiane et moyenne obtenues pour différents traitements d'un même site
comparaison_resultats <- data.frame(
  Traitement = c("Control", "NPK"),
  Moyenne = c(resultats_control$moyenne, resultats_npk$moyenne),
  Median = c(resultats_control$mediane, resultats_npk$mediane))

# Afficher les résultats
print(comparaison_resultats)


###

# Fonction pour calculer la moyenne et la médiane des valeurs de distance pour un site_code et un traitement spécifiques
CalculerMoyenneMedianSiteTraitement <- function(data, site_code, traitement) {
  # Reprendre les matrices de distance construites pour le site_code et le traitement spécifiques
  matrice_distances <- CalculerMatriceDistances(data, site_code, traitement)
  
  # Exclure la diagonale en la remplaçant par NA
  diag(matrice_distances) <- NA
  
  # Convertir la matrice en vecteur
  vecteur_distances <- as.vector(matrice_distances)#, na.rm = TRUE)
  
  # Calculer la moyenne et la médiane
  moyenne <- mean(vecteur_distances, na.rm = TRUE)
  mediane <- median(vecteur_distances, na.rm = TRUE)
  
  return(list(moyenne = moyenne, mediane = mediane))
}

# Fonction pour calculer la moyenne et la médiane des valeurs de distance pour tous les site_code et traitements
CalculerMoyenneMedianTousSitesTraitements <- function(data) {
  # Vecteurs pour stocker les moyennes et médianes pour chaque traitement
  moyennes <- c()
  medianes <- c()
  
  # Traitements à considérer
  traitements <- c("Control", "NPK")
  
  # Pour chaque site_code
  sites <- unique(data$site_code)
  for (site in sites) {
    # Pour chaque traitement
    for (traitement in traitements) {
      # Calculer la moyenne et la médiane pour le site_code et le traitement actuels
      resultats <- CalculerMoyenneMedianSiteTraitement(data, site, traitement)
      
      # Ajouter les résultats aux vecteurs
      moyennes <- c(moyennes, resultats$moyenne)
      medianes <- c(medianes, resultats$mediane)
    }
  }
  
  # Créer un tableau de données avec les résultats
  comparaison_resultats <- data.frame(
    Site = rep(sites, each = length(traitements)),
    Traitement = rep(traitements, times = length(sites)),
    Moyenne = moyennes,
    Median = medianes
  )
  
  return(comparaison_resultats)
}

# Utiliser la fonction pour calculer les moyennes et médianes pour tous les site_code et traitements
resultats_tous_sites_traitements <- CalculerMoyenneMedianTousSitesTraitements(data)
comparer_Moy <- function(data) {
  res <- CalculerMoyenneMedianTousSitesTraitements(data)
  sites <- unique(res$Site)
  differences <- c()
  
  for(site in sites) {
    moy_control <- res$Moyenne[res$Site == site & res$Traitement == "Control"]
    moy_npk <- res$Moyenne[res$Site == site & res$Traitement == "NPK"]
    
    if(length(moy_control) > 0 && length(moy_npk) > 0) {
      difference <-( abs(moy_control - moy_npk))
      differences <- c(differences, difference)
    }
  }
  
  comparaison_resultats <- data.frame(
    Site = rep(sites, each = 1),  # Répéter chaque site une fois
    Difference_Moyenne = differences
  )
  
  return(comparaison_resultats)
}

Tab_moy<-comparer_Moy(data)
write.table(Tab_moy, file = "Tab_moyenne_Npk.csv", sep = ",", row.names = TRUE)
# Calculer les pourcentages de différence relative entre la médiane et la moyenne
resultats_tous_sites_traitements$Diff_Relative <- paste0(
  abs(((resultats_tous_sites_traitements$Median - resultats_tous_sites_traitements$Moyenne)) / resultats_tous_sites_traitements$Moyenne) * 100,
  "%"
)

