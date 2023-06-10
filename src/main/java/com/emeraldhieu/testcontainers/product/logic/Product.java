package com.emeraldhieu.testcontainers.product.logic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "product")
@Getter
@Setter
@EqualsAndHashCode
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double price;

    /**
     * Set default value before persisting.
     * See https://stackoverflow.com/questions/197045/setting-default-values-for-columns-in-jpa#13432234
     */
    @PrePersist
    void preInsert() {
        if (externalId == null) {
            externalId = UUID.randomUUID().toString().replace("-", "");
        }
    }
}