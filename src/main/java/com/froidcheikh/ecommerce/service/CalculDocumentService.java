package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.LigneDocument;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculs HT / TVA / TTC pour les documents de facturation.
 *
 * Règles :
 * - Si prixAfficheTTC = true : prixUnitaireHT fourni est en réalité un prix TTC ;
 *   on en déduit le HT = TTC / (1 + tauxTva/100).
 * - Si prixAfficheTTC = false : prixUnitaireHT est le vrai HT.
 * - Arrondi à l'unité (FCFA) via HALF_UP sur les montants finaux.
 * - Remise par ligne : appliquée sur le montant HT avant TVA.
 * - Remise totale : appliquée sur le total HT après somme des lignes.
 */
@Service
public class CalculDocumentService {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final int SCALE_CALC = 6; // précision interne

    /**
     * Calcule et renseigne montantHT, montantTva, montantTTC sur chaque ligne.
     * Le tauxTva de la ligne doit déjà être renseigné avant l'appel.
     */
    public void calculerLigne(LigneDocument ligne, boolean prixAfficheTTC) {
        BigDecimal taux = ligne.getTauxTva() != null ? ligne.getTauxTva() : BigDecimal.ZERO;
        BigDecimal remise = ligne.getRemisePourcent() != null ? ligne.getRemisePourcent() : BigDecimal.ZERO;
        BigDecimal qte = ligne.getQuantite();
        BigDecimal prixUnitaire = ligne.getPrixUnitaireHT();

        BigDecimal prixHtUnitaire;
        if (prixAfficheTTC && taux.compareTo(BigDecimal.ZERO) > 0) {
            // Convertir TTC → HT : HT = TTC / (1 + taux/100)
            BigDecimal diviseur = BigDecimal.ONE.add(taux.divide(CENT, SCALE_CALC, RoundingMode.HALF_UP));
            prixHtUnitaire = prixUnitaire.divide(diviseur, SCALE_CALC, RoundingMode.HALF_UP);
        } else {
            prixHtUnitaire = prixUnitaire;
        }

        // Montant HT avant remise
        BigDecimal htBrut = prixHtUnitaire.multiply(qte).setScale(SCALE_CALC, RoundingMode.HALF_UP);

        // Application remise
        BigDecimal facteurRemise = BigDecimal.ONE.subtract(remise.divide(CENT, SCALE_CALC, RoundingMode.HALF_UP));
        BigDecimal htNet = htBrut.multiply(facteurRemise);

        // TVA
        BigDecimal tva = htNet.multiply(taux.divide(CENT, SCALE_CALC, RoundingMode.HALF_UP));
        BigDecimal ttc = htNet.add(tva);

        // Arrondi FCFA (unité entière)
        ligne.setMontantHT(arrondir(htNet));
        ligne.setMontantTva(arrondir(tva));
        ligne.setMontantTTC(arrondir(ttc));
    }

    /**
     * Calcule les totaux d'un document à partir de ses lignes.
     * Applique la remise totale sur le HT global.
     *
     * @return tableau [montantHT, montantTva, montantTTC] arrondis à l'unité
     */
    public BigDecimal[] calculerTotaux(List<LigneDocument> lignes,
                                       BigDecimal remiseTotalePourcent,
                                       boolean prixAfficheTTC) {
        BigDecimal totalHT = BigDecimal.ZERO;
        BigDecimal totalTva = BigDecimal.ZERO;

        for (LigneDocument l : lignes) {
            calculerLigne(l, prixAfficheTTC);
            totalHT = totalHT.add(l.getMontantHT());
            totalTva = totalTva.add(l.getMontantTva());
        }

        // Remise totale sur HT
        BigDecimal remise = remiseTotalePourcent != null ? remiseTotalePourcent : BigDecimal.ZERO;
        if (remise.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal facteur = BigDecimal.ONE.subtract(remise.divide(CENT, SCALE_CALC, RoundingMode.HALF_UP));
            BigDecimal ratioTva = totalHT.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : totalTva.divide(totalHT, SCALE_CALC, RoundingMode.HALF_UP);
            totalHT = totalHT.multiply(facteur);
            totalTva = totalHT.multiply(ratioTva);
        }

        BigDecimal totalTTC = totalHT.add(totalTva);
        return new BigDecimal[]{arrondir(totalHT), arrondir(totalTva), arrondir(totalTTC)};
    }

    private BigDecimal arrondir(BigDecimal val) {
        return val.setScale(0, RoundingMode.HALF_UP);
    }
}
