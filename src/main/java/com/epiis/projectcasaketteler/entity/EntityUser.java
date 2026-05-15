package com.epiis.projectcasaketteler.entity;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tuser")
@Setter
@Getter
public class EntityUser {
	@Id
	@Column(name = "idUser")
	private String idUser;
	
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
	
	@Column(name = "cellPhoneNumber")
	private int cellPhoneNumber;
	
	@Column(name = "cellPhoneEmergency")
	private int cellPhoneEmergency;
	
	@Column(name = "ipAddressLocal")
	private String idAddressLocal;
	
	@Column(name = "created_at")
	private Date created_at;
	
	@Column(name = "updated_at")
	private Date updated_at;
	
	@JsonManagedReference
	@OneToMany(mappedBy = "parentUser", cascade = CascadeType.ALL)
	private List<EntityPhoto> childPhoto;
	
	@JsonManagedReference
	@OneToMany(mappedBy = "parentUser", cascade = CascadeType.ALL)
	private List<EntityAttendance> childAteAttendance;
	
	@JsonManagedReference
	@OneToMany(mappedBy = "parentUser", cascade = CascadeType.ALL)
	private List<EntityDocumentEnter> childDocumentEnter;
	
	@JsonManagedReference
	@OneToMany(mappedBy = "parentUser", cascade = CascadeType.ALL)
	private List<EntityDocumentGeneral> childDocumentGeneral;
	
	@JsonManagedReference
	@OneToMany(mappedBy = "parentUser", cascade = CascadeType.ALL)
	private List<EntityDocumentResignation> childDocumentResignation;
}
