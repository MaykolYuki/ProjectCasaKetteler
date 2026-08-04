package com.epiis.projectcasaketteler.entity;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tresidence")
@Setter
@Getter
public class EntityResidence {
	@Id
	@Column(name = "idResidence")
	private String idResidence;

	@Column(name = "name")
	private String name;

	@Column(name = "idAddress")
	private String ipAddress;

	@Column(name = "created_at")
	private Date created_at;

	@Column(name = "updated_at")
	private Date updated_at;

	@Column(name = "wifiSsid")
	private String wifiSsid;

	@Column(name = "wifiBssid")
	private String wifiBssid;

	@JsonManagedReference
	@OneToMany(mappedBy = "parentResidence", cascade = CascadeType.ALL)
	private List<EntityAdmin> childAdmin;

	@JsonManagedReference
	@OneToMany(mappedBy = "parentResidence", cascade = CascadeType.ALL)
	private List<EntityUser> childUser;
}
