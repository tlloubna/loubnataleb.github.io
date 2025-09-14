#netoyyer data 
library(dplyr)
library(ggplot2) 
# Utiliser la fonction aggregate pour calculer le nombre d'apparition de chaque traitement par site_code, par Taxon et par traitement
resultats <- aggregate(year_trt ~ site_code + Taxon + trt, data, FUN = length)
resultats <- resultats[order(resultats$site_code), ]
# Renommer les colonnes résultantes
colnames(resultats) <- c("Pays", "Taxon", "Traitement", "Nombre_Apparition")
site_a_supprimer <- subset(resultats, Nombre_Apparition < 7)$Pays

# Supprimer les lignes correspondantes dans la base de données 'data'
data <- data[!(data$site_code %in% site_a_supprimer), ]
