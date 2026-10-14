package com.froidcheikh.ecommerce.repository;

import com.froidcheikh.ecommerce.entity.ParametresSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametresSiteRepository extends JpaRepository<ParametresSite, Long> {
}
