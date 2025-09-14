library(ggplot2)
library(dplyr)
Application_anova <- function(donne1, taxon1, plot1, taxon2, plot2) {
  data_taxon1_plot1 <- donne1 %>%
    filter(Taxon == taxon1 & plot == plot1)
  data_taxon2_plot2 <- donne1 %>%
    filter(Taxon == taxon2 & plot == plot2)
  if (plot1!=plot2 & taxon1!=taxon2){
    resultat_anova <- aov(rel_cover ~ type_Abondance, data = rbind(data_taxon1_plot1, data_taxon2_plot2))
    anova <- summary(resultat_anova)
    p_value <- anova[[1]]$'Pr(>F)'[1]
    return(p_value)
  }else {
    return (0)
  }
  
}


Anova <- function(data, site, traitement) {
  data1 <- data %>%
    filter(site_code == site , trt == traitement) %>%
    select(plot, Taxon, initial_rel_cover, year_trt, rel_cover)
  
  combinaisons <- unique(data1[, c("Taxon", "plot")])
  matrice_distances <- matrix(NA, nrow = nrow(combinaisons), ncol = nrow(combinaisons), 
                              dimnames = list(combinaisons$Taxon, combinaisons$Taxon))
  
  for (i in 1:nrow(combinaisons)) {
    for (j in 1:nrow(combinaisons)) {
      
      taxon1 <- combinaisons$Taxon[i]
      plot1 <- combinaisons$plot[i]
      taxon2 <- combinaisons$Taxon[j]
      plot2 <- combinaisons$plot[j]
      
      if (taxon1 != taxon2) {
        donne1 <- data1 %>%
          filter((Taxon == taxon1 | Taxon == taxon2) & (plot == plot1 | plot == plot2)) %>%
          select(plot, Taxon, initial_rel_cover, year_trt, rel_cover)
        donne1 <- donne1 %>%
          mutate(type_Abondance = ntile(initial_rel_cover, 4)) %>%
          mutate(type_Abondance = ifelse(type_Abondance > 2, 1, 0))
        
        matrice_distances[i, j] <- Application_anova(donne1, taxon1, plot1, taxon2, plot2)
        
        
      } else {
        matrice_distances[i, j] <- 0
      }
      
    }
  }
  return(matrice_distances)
}


Tab_ANOVA1 <- function(data) {
  sites <- unique(data$site_code)
  traitements <- c("Control", "NPK", "Fence", "NPK+Fence")
  p_values <- c()
  
  for (site in sites) {
    tester <- data %>%
      filter(site_code == site) %>%
      pull(trt)  # Récupérer les traitements du site
    
    for (traitement in traitements) {
      if (traitement %in% tester) {  # Vérifier si le traitement est présent parmi les traitements du site
        matrice_p_valeurs <- Anova(data, site, traitement)
        mean_p_value <- mean(matrice_p_valeurs, na.rm = TRUE)
        p_values <- c(p_values, mean_p_value)
      } else {
        p_values <- c(p_values, NA)
      }
    }
  }
  
  Tab_ANOVA <- data.frame(
    Site = rep(sites, each = length(traitements)),
    Traitement = rep(traitements, times = length(sites)),
    mean_p_value = p_values
  )
  
  return(Tab_ANOVA)
}



