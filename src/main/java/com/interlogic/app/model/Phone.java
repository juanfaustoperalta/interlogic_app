package com.interlogic.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "phone")
public class Phone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone;

    private Long externalId;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private Status status;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Correction> corrections = new ArrayList<>();

}