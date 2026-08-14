package com.zeropick.productservice.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "sweetener")
public class Sweetener {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 40)
    private String name;

    protected Sweetener() {
        // JPA 기본 생성자
    }

    public Sweetener(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
