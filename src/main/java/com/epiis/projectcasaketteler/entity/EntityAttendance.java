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
@Table(name = "tattendance")
@Setter
@Getter
public class EntityAttendance {
	@Id
	@Column(name = "idAtendance")
	private String idAtendance;

	@JsonBackReference
	@JoinColumn(name = "idUser")
	@ManyToOne(fetch = FetchType.LAZY)
	private EntityUser parentUser;

	@Column(name = "entryDate")
	private Date entryDate;

	@Column(name = "departureDate")
	private Date departureDate;

	@Column(name = "status")
	private Boolean status;

	@Column(name = "description")
	private String description;

	@Column(name = "created_at")
	private Date created_at;

	@Column(name = "updated_at")
	private Date updated_at;

	@Column(name = "recordedAt")
	private Date recordedAt; // hora real cuando ocurrió offline

	@Column(name = "syncedAt")
	private Date syncedAt; // hora cuando llegó al servidor

	@Column(name = "verifiedByServer")
	private Boolean verifiedByServer = false; // confirmación Python

	@Column(name = "clientSimilarity")
	private Double clientSimilarity; // similitud reportada por el móvil

	@Column(name = "serverSimilarity")
	private Double serverSimilarity; // similitud confirmada por Python
}
