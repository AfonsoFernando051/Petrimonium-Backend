package com.jf.PetApp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "academy_module_prerequisites", schema = "education")
public class AcademyModulePrerequisiteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_id")
    private String moduleId;

    @Column(name = "prerequisite_module_id")
    private String prerequisiteModuleId;

    public Long getId() {
        return id;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getPrerequisiteModuleId() {
        return prerequisiteModuleId;
    }

    public void setPrerequisiteModuleId(String prerequisiteModuleId) {
        this.prerequisiteModuleId = prerequisiteModuleId;
    }
}
