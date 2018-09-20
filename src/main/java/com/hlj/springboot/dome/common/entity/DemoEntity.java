package com.hlj.springboot.dome.common.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 测试实体类，这个随便;
 */
@Data
@Accessors(chain = true)
public class DemoEntity {

	private Long id;

	private String name;

	private Long balance;



}
