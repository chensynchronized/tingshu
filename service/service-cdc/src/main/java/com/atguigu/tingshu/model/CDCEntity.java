package com.atguigu.tingshu.model;

import lombok.Data;

import javax.persistence.Column;

@Data
public class CDCEntity {

    @Column(name = "id")
    private Long id;
}
