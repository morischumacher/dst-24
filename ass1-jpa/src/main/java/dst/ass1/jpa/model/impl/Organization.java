package dst.ass1.jpa.model.impl;


import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;


@Entity
@Table(name = Constants.T_ORGANIZATION)
public class Organization implements IOrganization {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    @Column(name = Constants.I_ORGANIZATION)
    private Long id;

    @Column(name = Constants.M_LOCATION_NAME)
    private String name;

    @OneToMany(mappedBy = "id.organization")
    private Collection<Employment> employments = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = Constants.J_ORGANIZATION_VEHICLE,
            joinColumns = @JoinColumn(name = Constants.I_ORGANIZATION),
            inverseJoinColumns = @JoinColumn(name = Constants.I_VEHICLES)
    )
    private Collection<Vehicle> vehicles = new ArrayList<>();


    @ManyToMany
    @JoinTable(
            name = Constants.J_ORGANIZATION_PARTS,
            joinColumns = @JoinColumn(name = Constants.I_ORGANIZATION_PARTS),
            inverseJoinColumns = @JoinColumn(name = Constants.I_ORGANIZATION_PART_OF)
    )
    private Collection<Organization> parts = new ArrayList<>();

    @ManyToMany(mappedBy = "parts")
    private Collection<Organization> partOf = new ArrayList<>();

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Collection<IOrganization> getParts() {
        return this.parts.stream().map(organization -> (IOrganization) organization).collect(Collectors.toList());
    }

    @Override
    public void setParts(Collection<IOrganization> parts) {
        this.parts = parts.stream().map(org -> (Organization) org).collect(Collectors.toSet());
    }

    @Override
    public void addPart(IOrganization part) {
        this.parts.add((Organization) part);
    }

    @Override
    public Collection<IOrganization> getPartOf() {
        return partOf.stream().map(org -> (IOrganization) org).collect(Collectors.toSet());
    }

    @Override
    public void setPartOf(Collection<IOrganization> partOf) {
        this.partOf = partOf.stream().map(org -> (Organization) org).collect(Collectors.toSet());
    }

    @Override
    public void addPartOf(IOrganization partOf) {
        this.partOf.add((Organization) partOf);
    }

    @Override
    public Collection<IEmployment> getEmployments() {
        return this.employments.stream().map(employment -> (IEmployment) employment).collect(Collectors.toSet());
    }

    @Override
    public void setEmployments(Collection<IEmployment> employments) {
        this.employments = employments.stream().map(employment -> (Employment) employment).collect(Collectors.toSet());
    }

    @Override
    public void addEmployment(IEmployment employment) {
        this.employments.add((Employment) employment);
    }

    @Override
    public Collection<IVehicle> getVehicles() {
        return this.vehicles.stream().map(vehicle -> (IVehicle) vehicle).collect(Collectors.toSet());
    }

    @Override
    public void setVehicles(Collection<IVehicle> vehicles) {
        this.vehicles = vehicles.stream().map(vehicle -> (Vehicle) vehicle).collect(Collectors.toSet());
    }

    @Override
    public void addVehicle(IVehicle vehicle) {
        this.vehicles.add((Vehicle) vehicle);
    }
}