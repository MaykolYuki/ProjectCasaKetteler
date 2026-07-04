package com.epiis.projectcasaketteler.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tadmin")
@Setter
@Getter
public class EntityAdmin {

	public enum AdminRole {
		SUPER_ADMIN, ADMIN
	}

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

	@Column(name = "email", unique = true)
	private String email;

	@Column(name = "password")
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	private AdminRole role = AdminRole.ADMIN;

	@Column(name = "active")
	private Boolean active = true;

	@Column(name = "loginAttempts")
	private Integer loginAttempts = 0;

	@Column(name = "lockedUntil")
	private Date lockedUntil;

	@Column(name = "created_at")
	private Date created_at;

	@Column(name = "updated_at")
	private Date updated_at;

	@PrePersist
	protected void onCreate() {
		created_at = new Date();
		updated_at = new Date();
	}

	@PreUpdate
	protected void onUpdate() {
		updated_at = new Date();
	}
}