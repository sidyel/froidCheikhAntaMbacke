package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.CompteurDocument;
import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.repository.CompteurDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class NumeroDocumentService {

    private final CompteurDocumentRepository compteurRepository;

    /**
     * Attribue le prochain numéro séquentiel sans trou pour le type et l'année donnés.
     * Utilise SELECT FOR UPDATE pour garantir l'unicité sous concurrence.
     * Doit être appelé dans une transaction dédiée (REQUIRES_NEW) pour que le verrou
     * soit libéré le plus tôt possible.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String attribuerNumero(TypeDocument type, String prefixe) {
        int annee = LocalDate.now().getYear();

        CompteurDocument compteur = compteurRepository
                .findByTypeAndAnneeForUpdate(type, annee)
                .orElseGet(() -> {
                    CompteurDocument c = new CompteurDocument();
                    c.setTypeDocument(type);
                    c.setAnnee(annee);
                    c.setDernierNumero(0);
                    return compteurRepository.save(c);
                });

        int nouveau = compteur.getDernierNumero() + 1;
        compteur.setDernierNumero(nouveau);
        compteurRepository.save(compteur);

        return String.format("%s-%d-%04d", prefixe, annee, nouveau);
    }
}
