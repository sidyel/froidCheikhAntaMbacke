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
}
