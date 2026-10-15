package com.froidcheikh.ecommerce.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre de tentatives de suivi de commande par adresse IP.
 * 10 tentatives max sur une fenêtre glissante de 15 minutes.
 */
@Service
public class SuiviRateLimiterService {

    private static final int MAX_TENTATIVES = 10;
    private static final long FENETRE_MS = 15 * 60 * 1000L; // 15 min

    private final Map<String, Deque<Long>> tentatives = new ConcurrentHashMap<>();

    /**
     * @return true si la requête est autorisée, false si le quota est dépassé
     */
    public boolean tenterAcces(String ip) {
        long maintenant = Instant.now().toEpochMilli();
        tentatives.compute(ip, (k, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            // Purge les entrées hors fenêtre
            while (!deque.isEmpty() && maintenant - deque.peekFirst() > FENETRE_MS) {
                deque.pollFirst();
            }
            deque.addLast(maintenant);
            return deque;
        });
        return tentatives.get(ip).size() <= MAX_TENTATIVES;
    }
}
