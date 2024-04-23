package dst.ass1.jpa.model.impl;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = Constants.T_DRIVER)
@AttributeOverride(name = "id", column = @Column(name = Constants.I_DRIVER, nullable = false))
@AttributeOverride(name = "name", column = @Column(name = Constants.M_DRIVER_NAME, nullable = false))
@AttributeOverride(name = "tel", column = @Column(name = Constants.M_DRIVER_TEL, nullable = false))
@AttributeOverride(name = "avgRating", column = @Column(name = Constants.M_DRIVER_AVG_RATING, nullable = false))

public class Driver extends PlatformUser implements IDriver {
    @ManyToOne
    @JoinColumn(name = Constants.I_VEHICLE, nullable = false)
    private Vehicle vehicle;
    @OneToMany(mappedBy = "id.driver")
    private Set<Employment> employments = new HashSet<>();

    public Driver() {super();}

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
    public IVehicle getVehicle() {
        return this.vehicle;
    }

    @Override
    public void setVehicle(IVehicle vehicle) {
        this.vehicle = (Vehicle) vehicle;
    }
}
