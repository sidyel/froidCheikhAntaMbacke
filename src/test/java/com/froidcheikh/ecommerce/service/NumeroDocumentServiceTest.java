package com.froidcheikh.ecommerce.service;

import com.froidcheikh.ecommerce.entity.CompteurDocument;
import com.froidcheikh.ecommerce.entity.CompteurDocument.TypeDocument;
import com.froidcheikh.ecommerce.repository.CompteurDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumeroDocumentServiceTest {

    @Mock
    private CompteurDocumentRepository compteurRepo;

    @InjectMocks
    private NumeroDocumentService service;

    private int sequence;

    @BeforeEach
    void setUp() {
        sequence = 0;
        // Simule un compteur incrémenté à chaque appel save()
        when(compteurRepo.findByTypeAndAnneeForUpdate(any(), anyInt()))
                .thenAnswer(inv -> {
                    CompteurDocument c = new CompteurDocument();
                    c.setTypeDocument(TypeDocument.FACTURE);
                    c.setAnnee(LocalDate.now().getYear());
                    c.setDernierNumero(sequence++);
                    return Optional.of(c);
                });
        when(compteurRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void formatNumeroCorrect() {
        String num = service.attribuerNumero(TypeDocument.FACTURE, "FAC");
        int annee = LocalDate.now().getYear();
        assertThat(num).matches("FAC-" + annee + "-\\d{4}");
    }

    @Test
    void sequenceIncrementale() {
        String n1 = service.attribuerNumero(TypeDocument.FACTURE, "FAC");
        String n2 = service.attribuerNumero(TypeDocument.FACTURE, "FAC");
        int annee = LocalDate.now().getYear();
        assertThat(n1).isEqualTo("FAC-" + annee + "-0001");
        assertThat(n2).isEqualTo("FAC-" + annee + "-0002");
    }

    @Test
    void prefixeDifferent() {
        String num = service.attribuerNumero(TypeDocument.AVOIR, "AVO");
        assertThat(num).startsWith("AVO-");
    }
}
