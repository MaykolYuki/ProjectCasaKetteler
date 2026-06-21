package com.epiis.projectcasaketteler.entity;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tuser")
@Setter
@Getter
public class EntityUser {

	public enum UserRole {
		RESIDENTE, ADMIN, GUARDIA
	}

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

	@Column(name = "email", unique = true)
	private String email;

	@Column(name = "password")
	private String password;

	@Column(name = "cellPhoneNumber")
	private String cellPhoneNumber; // CAMBIADO a String

	@Column(name = "cellPhoneEmergency")
	private String cellPhoneEmergency; // CAMBIADO a String

	@Column(name = "ipAddressLocal")
	private String ipAddressLocal; // NOMBRE CORREGIDO

	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	private UserRole role = UserRole.RESIDENTE;

	@Column(name = "active")
	private Boolean active = true;

	@Column(name = "firstLogin")
	private Boolean firstLogin = true;

	@Column(name = "temporalPassword")
	private String temporalPassword;

	@Column(name = "bestPhotoReference")
	private String bestPhotoReference; // nombre del archivo de la mejor foto

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

	@PrePersist
	protected void onCreate() {
		created_at = new Date();
		updated_at = new Date();
	}

	@PreUpdate
	protected void onUpdate() {
		updated_at = new Date();
	}

	// GETTER Y SETTER explícito para ipAddressLocal (por si lombok no funciona)
	public String getIpAddressLocal() {
		return ipAddressLocal;
	}

	public void setIpAddressLocal(String ipAddressLocal) {
		this.ipAddressLocal = ipAddressLocal;
	}
}