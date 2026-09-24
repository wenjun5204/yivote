package com.yivote.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 群档案(openGId 维度):备注名成员共编,战功/凝聚力分只升不降 */
@Entity
@Table(name = "group_profile")
public class GroupProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "open_gid", nullable = false, unique = true, length = 64)
    private String openGId;

    /** 群备注名;未设置时为「这个群」 */
    @Column(nullable = false, length = 16)
    private String name;

    /** 改过名的最近用户(审计用,不暴露) */
    @Column(name = "renamed_by")
    private Long renamedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOpenGId() { return openGId; }
    public void setOpenGId(String openGId) { this.openGId = openGId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getRenamedBy() { return renamedBy; }
    public void setRenamedBy(Long renamedBy) { this.renamedBy = renamedBy; }
}
