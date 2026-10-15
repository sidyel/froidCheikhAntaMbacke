package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.JetonTelechargement;
import com.froidcheikh.ecommerce.repository.JetonTelechargementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JetonTelechargementService {

    private final JetonTelechargementRepository jetonRepo;

    /**
     * Retourne le PDF si le jeton est valide, lève une exception sinon.
     */
    @Transactional(readOnly = true)
    public byte[] telechargerParJeton(String jeton) {
        JetonTelechargement j = jetonRepo.findByJeton(jeton)
                .orElseThrow(() -> new IllegalArgumentException("Jeton invalide"));

        if (!j.isValide()) {
            throw new IllegalStateException("Jeton expiré ou révoqué");
        }

        byte[] pdf = j.getDocument().getPdfBytes();
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Aucun PDF disponible pour ce document");
        }
        return pdf;
    }

    @Transactional
    public void revoquer(String jeton, String login) {
        JetonTelechargement j = jetonRepo.findByJeton(jeton)
                .orElseThrow(() -> new IllegalArgumentException("Jeton introuvable"));
        j.setRevoque(true);
        jetonRepo.save(j);
    }
}
