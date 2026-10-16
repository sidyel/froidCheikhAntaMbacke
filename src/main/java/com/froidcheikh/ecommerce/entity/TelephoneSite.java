package com.froidcheikh.ecommerce.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelephoneSite {

    private String numero;

    /** Label affiché (ex: "Service client", "WhatsApp") */
    private String libelle;

    private Boolean whatsapp = false;

    /** Si false, ce numéro est exclu du pied de page des documents PDF. */
    @jakarta.persistence.Column(name = "afficher_en_documents")
    private Boolean afficherEnDocuments = true;
}
