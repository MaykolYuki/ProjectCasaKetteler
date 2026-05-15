package com.epiis.projectcasaketteler.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tadmin")
@Setter
@Getter
public class EntityAdmin {
	@Id
	@Column(name = "idAdmin")
	private String idAdmin;
	
	@JsonBackReference
	@JoinColumn(name = "idResidence")
	@ManyToOne(fetch = FetchType.LAZY)
	private EntityResidence parentResidence;
	
	@Column(name = "firstName")
	private String firstName;
	
	@Column(name = "surName")
	private String surName;
	
	@Column(name = "email")
	private String email;
	
	@Column(name = "password")
	private String password;
	
	@Column(name = "created_at")
	private Date created_at;
	
	@Column(name = "updated_at")
	private Date updated_at;
}
