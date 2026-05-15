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
@Table(name = "tphoto")
@Setter
@Getter
public class EntityPhoto {
	@Id
	@Column(name = "idPhoto")
	private String idPhoto;
	
	@JsonBackReference
	@JoinColumn(name = "iduser")
	@ManyToOne(fetch = FetchType.LAZY)
	private EntityUser parentUser;
	
	@Column(name = "namePhoto")
	private String namePhoto;
	
	@Column(name = "extensionPhoto")
	private String extensionPhoto;
	
	@Column(name = "created_at")
	private Date created_at;
	
	@Column(name = "updated_at")
	private Date updated_at;
}
