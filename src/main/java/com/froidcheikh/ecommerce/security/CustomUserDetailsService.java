package com.froidcheikh.ecommerce.security;

import com.froidcheikh.ecommerce.entity.Administrateur;
import com.froidcheikh.ecommerce.entity.Client;
import com.froidcheikh.ecommerce.repository.AdministrateurRepository;
import com.froidcheikh.ecommerce.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final ClientRepository clientRepository;
    private final AdministrateurRepository administrateurRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Vérifier d'abord dans les clients
        Client client = clientRepository.findByEmail(email).orElse(null);
        if (client != null) {
            return client;
        }

        // Vérifier ensuite dans les administrateurs
        Administrateur admin = administrateurRepository.findByEmail(email).orElse(null);
        if (admin != null) {
            return admin;
        }

        throw new UsernameNotFoundException("Utilisateur non trouvé avec l'email : " + email);
    }
}