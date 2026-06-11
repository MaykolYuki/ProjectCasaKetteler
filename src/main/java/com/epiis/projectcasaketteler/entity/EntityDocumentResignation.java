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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tdocumentResignation")
@Setter
@Getter
public class EntityDocumentResignation {
	@Id
	@Column(name = "idDocumentResignation")
	private String idDocumentResignation;

	@JsonBackReference
	@JoinColumn(name = "idUser")
	@ManyToOne(fetch = FetchType.LAZY)
	private EntityUser parentUser;

	@Column(name = "nameDocumentResignation")
	private String nameDocumentResignation;

	@Column(name = "extensionDocumentResignation")
	private String extensionDocumentResignation;

	public enum ResignationStatus {
		PENDIENTE, APROBADO, OBSERVADO, RECHAZADO
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private ResignationStatus status = ResignationStatus.PENDIENTE;

	@Column(name = "observations")
	private String observations;

	@Column(name = "created_at")
	private Date created_at;

	@Column(name = "updated_at")
	private Date updated_at;
}
