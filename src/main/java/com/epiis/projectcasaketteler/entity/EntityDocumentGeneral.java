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
@Table(name = "tdocumentGeneral")
@Setter
@Getter
public class EntityDocumentGeneral {
	@Id
	@Column(name = "idDocumentGeneral")
	private String idDocumentGeneral;

	@JsonBackReference
	@JoinColumn(name = "idUser")
	@ManyToOne(fetch = FetchType.LAZY)
	private EntityUser parentUser;

	@Column(name = "type")
	private String type;

	@Column(name = "nameDocumentGeneral")
	private String nameDocumentGeneral;

	@Column(name = "extensionDocumentGeneral")
	private String extensionDocumentGeneral;

	@Column(name = "created_at")
	private Date created_at;

	@Column(name = "updated_at")
	private Date updated_at;

	@Column(name = "downloadable")
	private Boolean downloadable = false;

	@Column(name = "observations")
	private String observations;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private DocumentStatus status = DocumentStatus.PENDIENTE;

	public enum DocumentStatus {
		PENDIENTE, APROBADO, OBSERVADO, RECHAZADO
	}

	@Column(name = "period")
	private String period; // formato "2026-06" para mensual, "2026-1" para semestral
}
