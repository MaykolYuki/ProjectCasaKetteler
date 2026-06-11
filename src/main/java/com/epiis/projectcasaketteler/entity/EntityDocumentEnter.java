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
@Table(name = "tdocumentEnter")
@Setter
@Getter
public class EntityDocumentEnter {
	@Id
	@Column(name = "idDocumentEnter")
	private String idDocumentEnter;

	@JsonBackReference
	@JoinColumn(name = "idUser")
	@ManyToOne(fetch = FetchType.LAZY)
	private EntityUser parentUser;

	@Column(name = "nameDocumentEnter")
	private String nameDocumentEnter;

	@Column(name = "extensionDocumentEnter")
	private String extensionDocumentEnter;

	@Column(name = "created_at")
	private Date created_at;

	@Column(name = "updated_at")
	private Date updated_at;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private DocumentEnterStatus status = DocumentEnterStatus.PENDIENTE;

	@Column(name = "observations")
	private String observations;

	@Column(name = "downloadable")
	private Boolean downloadable = false;

	public enum DocumentEnterStatus {
		PENDIENTE, APROBADO, OBSERVADO, RECHAZADO
	}
}
