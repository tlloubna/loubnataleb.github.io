Distance_Taxon <- function(Espece, traitement, type_abundance, plotC, plotT) {
  
  # Filtrer les données en fonction de l'espèce
  DataE <- data%>%
    filter(Taxon == Espece)
  
  # Filtrer les données en fonction du traitement choisi et des plots spécifiés
  dfST <- DataE %>%
    filter((trt == "Control" & plot == plotC) | (trt == traitement & plot == plotT))
  
  # Filtrer les données spécifiques au traitement Control
  dfSST <- dfST %>%
    filter(trt == "Control")
  
  # Afficher le dataframe dfST (commenté pour une utilisation plus générale)
  # print(dfST)
  
  # Créer le graphique sans l'afficher
  plot <- ggplot(dfST, aes_string(x = "year_trt", y = type_abundance, color = "trt")) +
    geom_point() +
    geom_smooth(method = "loess", se = FALSE, linetype = "dashed", aes(color=trt)) +
    labs(title = paste("Évolution de ",Espece, "pour le traitement", traitement),
         x = "Année de traitement",
         y = type_abundance,
         color = "Traitement") +
    theme_minimal()
  
  # Afficher le graphique
  print(plot)
  
  # Calculer la corrélation entre les courbes temporelles
  #correlation <- cor(dfSST[[type_abundance]], dfST[[type_abundance]])
  
  # Afficher la corrélation
  #cat("Corrélation entre les courbes temporelles :", correlation, "\n")
  
  #return(correlation)
}

# Appeler la fonction avec des valeurs aléatoires
random_taxon <- sample(unique(data$Taxon), 1)
Plot <- data %>%
  filter(Taxon == "COUSINIA THOMSONII") %>%
  select(plot, trt, rel_cover)

# Sélectionner aléatoirement une valeur de trt (autre que "Control")
trait <- sample(unique(Plot$trt[Plot$trt != "Control"]), 1)
# Sélectionner aléatoirement un plot pour le trt spécifié
PlotT <- sample((Plot$plot[Plot$trt == trait]), 1)



PlotC <- sample((Plot$plot[Plot$trt == "Control"]), 1)

# Appeler la fonction avec des valeurs aléatoires
Distance_Taxon(random_taxon, trait, "rel_cover", PlotC, PlotT)

