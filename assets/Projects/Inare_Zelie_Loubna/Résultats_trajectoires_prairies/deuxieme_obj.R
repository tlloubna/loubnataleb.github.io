Application_anova2 <- function(donne1, taxon1) {
  data_taxon1_plot1 <- donne1 %>%
    filter(Taxon == taxon1, NPK == 0,trt=="Control")
  data_taxon2_plot2 <- donne1 %>%
    filter(Taxon == taxon1, NPK == 1)
  if (nrow(data_taxon1_plot1 )>0 & nrow(data_taxon2_plot2)>0) {
  resultat_anova <- aov(rel_cover ~ NPK, data = rbind(data_taxon1_plot1, data_taxon2_plot2))
  anova <- summary(resultat_anova)
  p_value <- anova[[1]]$'Pr(>F)'[1]
  return(p_value)
  }else {
    return (NA)
    
  }
}

Anova_2 <- function(data, site) {
  data1 <- data %>%
    filter(site_code == site) %>%
    select(plot, Taxon, trt, initial_rel_cover, year_trt, rel_cover, NPK)
  taxons <- unique(data1$Taxon)
  p_value <- c()
  
  for (taxon in taxons) {
    p <- Application_anova2(data1, taxon)
    p_value <- c(p_value, p)
  }
  Npk_p_value <- data.frame(
    Taxons = rep(taxons, each = 1),
    p_value = p_value
  )
  return(Npk_p_value)
}
voir <- data %>%
  filter(site_code == "bogong.au")

matrice <- Anova_2(data, "bogong.au")

Tab_ANOVA2 <- function(data) {
  sites <- unique(data$site_code)
  p_values <- c()
  
  for (site in sites) {
    tab <- Anova_2(data, site)
    mean_p_value <- mean(tab$p_value, na.rm = TRUE)
    p_values <- c(p_values, mean_p_value)
  }
  Tab_ANOVA <- data.frame(
    Site = rep(sites, each = 1),
    mean_p_value = p_values
  )
  return(Tab_ANOVA)
}



