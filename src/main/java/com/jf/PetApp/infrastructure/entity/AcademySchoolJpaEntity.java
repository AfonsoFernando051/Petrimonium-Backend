package com.jf.PetApp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "academy_schools", schema = "education")
@Getter
@Setter
public class AcademySchoolJpaEntity {

    @Id
    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "domain_id")
    private String domainId;

    @Column(name = "order_index")
    private int orderIndex;

    @Column(name = "icon_key")
    private String iconKey;

    @Column(name = "content_available")
    private boolean contentAvailable;
}
