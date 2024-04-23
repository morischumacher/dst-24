package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EmploymentKey implements IEmploymentKey, Serializable {

    @ManyToOne
    @JoinColumn(name = Constants.I_DRIVER)
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = Constants.I_ORGANIZATION)
    private Organization organization;


    @Override
    public IDriver getDriver() {
        return driver;
    }

    @Override
    public void setDriver(IDriver driver) {
        this.driver = (Driver) driver;
    }

    @Override
    public IOrganization getOrganization() {
        return organization;
    }

    @Override
    public void setOrganization(IOrganization organization) {
        this.organization = (Organization) organization;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof EmploymentKey)) {
            return false;
        }

        EmploymentKey k = (EmploymentKey) o;
        return Double.compare(this.driver.getId(), k.driver.getId()) == 0
                && Double.compare(this.organization.getId(), k.organization.getId()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.driver.getId(), this.organization.getId());
    }
}