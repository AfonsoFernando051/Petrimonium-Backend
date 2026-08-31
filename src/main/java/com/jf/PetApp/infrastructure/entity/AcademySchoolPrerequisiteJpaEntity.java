package com.jf.PetApp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "academy_school_prerequisites", schema = "education")
public class AcademySchoolPrerequisiteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "prerequisite_school_id")
    private String prerequisiteSchoolId;

    public Long getId() {
        return id;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getPrerequisiteSchoolId() {
        return prerequisiteSchoolId;
    }

    public void setPrerequisiteSchoolId(String prerequisiteSchoolId) {
        this.prerequisiteSchoolId = prerequisiteSchoolId;
    }
}
