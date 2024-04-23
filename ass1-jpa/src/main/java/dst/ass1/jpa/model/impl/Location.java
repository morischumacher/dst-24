package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.*;

@Entity
public class Location implements ILocation {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    @Column(name = Constants.I_LOCATION)
    private Long id;

    @Column(name = Constants.M_LOCATION_NAME)
    private String name;

    @Column(name = Constants.M_LOCATION_GEO)
    private Long locationId;


    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Long getLocationId() {
        return locationId;
    }

    @Override
    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}