library(dplyr)
library(ggplot2)  

min_Distance <- function(taxon1, plot1, taxon2, plot2) {
 
  
  # Calcul des distances entre les courbes
  T1 <- data %>%
    filter(Taxon == taxon1 & plot == plot1)
  T2 <- data %>%
    filter(Taxon == taxon2 & plot == plot2)
  
  model1 <- loess(as.formula(paste("rel_cover", "~ year_trt")), data = T1)
  model2 <- loess(as.formula(paste("rel_cover", "~ year_trt")), data = T2)
  
  f1 <- predict(model1, newdata = data.frame(year_trt = T1$year_trt))
  f2 <- predict(model2, newdata = data.frame(year_trt = T2$year_trt))
  
  Tab_pente1 =numeric(length=length(f1)-1)
  Tab_pente2=numeric(length = length(f2)-1)
  for (i in 1:length(Tab_pente1)) {
    Tab_pente1[i]=f1[i+1]-f1[i]
  }
  for(i in 1:length(Tab_pente2)) {
    Tab_pente2=f2[i+1]-f2[i]
  }

  min_pente1=min(Tab_pente1)
  min_pente2=min(Tab_pente2)
  error=abs((min_pente2-min_pente1))/abs(min_pente1)*100
  #print(which.min(Tab_pente1))
  print(which.min(Tab_pente2))
  cat("le pourcentage de différence de la pente entre ", taxon1, "et", taxon2, "est:", error, "%\n")
  # Tracé des courbes
  ggplot() +
    geom_line(data = T1, aes(x = year_trt, y = rel_cover), color = "blue", linetype = "solid") +
    geom_line(data = T2, aes(x = year_trt, y = rel_cover), color = "red", linetype = "dashed") +
    labs(title = "Comparaison des courbes de décroissance",
         x = "Année de traitement",
         y = "Rel_cover") +
    theme_minimal()
  
}
#regarder la distribution des plantes pour un site code et un traitement 
# Remplacez "site_code", "treatment", "Taxon" et "rel_cover" par les noms de colonnes réels dans votre jeu de données
# Assurez-vous que les noms de colonnes sont corrects et correspondent à votre structure de données


min_Distance("AXONOPUS FURCATUS", 1, "PANICUM SP.", 11)
