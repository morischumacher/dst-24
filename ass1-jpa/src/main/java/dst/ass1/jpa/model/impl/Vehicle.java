package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.*;

@Entity
@Table(name = Constants.T_VEHICLE)
public class Vehicle implements IVehicle {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    @Column(name = Constants.I_VEHICLE)
    private Long id;

    @Column(unique=true, name = Constants.M_VEHICLE_LICENSE)
    private String license;

    @Column(name = Constants.M_VEHICLE_COLOR)
    private String color;

    @Column(name = Constants.M_VEHICLE_TYPE)
    private String type;

    public Vehicle() {}

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getLicense() {
        return this.license;
    }

    @Override
    public void setLicense(String license) {
        this.license = license;
    }

    @Override
    public String getColor() {
        return this.color;
    }

    @Override
    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }
}
