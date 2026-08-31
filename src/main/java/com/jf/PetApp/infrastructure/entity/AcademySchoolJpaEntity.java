package com.jf.PetApp.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "academy_schools")
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

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public boolean isContentAvailable() {
        return contentAvailable;
    }

    public void setContentAvailable(boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
    }
}
