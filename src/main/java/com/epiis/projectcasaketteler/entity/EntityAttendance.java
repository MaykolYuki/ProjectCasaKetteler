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

	// Momento exacto en que ocurrió el evento (marca de la cámara)
	@Column(name = "eventTimestamp")
	private Date eventTimestamp;

	// Tipo de evento: hecho crudo e inalterable
	@Enumerated(EnumType.STRING)
	@Column(name = "eventType")
	private AttendanceEventType eventType;

	// true si el evento contradice el estado esperado (ej. dos salidas seguidas)
	@Column(name = "esAnomalia")
	private Boolean esAnomalia = false;

	// Para INTENTO_FALLIDO: por qué falló (rostro no reconocido, red inválida)
	@Column(name = "motivoFallo")
	private String motivoFallo;

	// Motivo declarado por el residente (ej. "clases", "cita médica")
	@Column(name = "description")
	private String description;

	// Datos de red capturados en el momento del evento (auditoría)
	@Column(name = "ssid")
	private String ssid;

	@Column(name = "bssid")
	private String bssid;

	// Similitud facial confirmada por el servidor Python (auditoría)
	@Column(name = "serverSimilarity")
	private Double serverSimilarity;

	@Column(name = "created_at")
	private Date created_at;

	public enum AttendanceEventType {
		ENTRADA, SALIDA, INTENTO_FALLIDO
	}
}