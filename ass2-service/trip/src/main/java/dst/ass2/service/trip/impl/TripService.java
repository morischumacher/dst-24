package dst.ass2.service.trip.impl;

import dst.ass1.jpa.dao.IDAOFactory;
import dst.ass1.jpa.model.*;
import dst.ass2.service.api.match.IMatchingService;
import dst.ass2.service.api.trip.*;
import javax.annotation.ManagedBean;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
@ManagedBean
public class TripService implements ITripService {

    @Inject
    private IMatchingService matchingService;

    @Inject
    private IModelFactory modelFactory;

    @Inject
    private IDAOFactory daoFactory;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public TripDTO create(Long riderId, Long pickupId, Long destinationId) throws EntityNotFoundException {
        var locationDAO = daoFactory.createLocationDAO();
        var riderDAO = daoFactory.createRiderDAO();
        var trip = modelFactory.createTrip();

        var pickup = locationDAO.findById(pickupId);
        var destination = locationDAO.findById(destinationId);
        var rider = riderDAO.findById(riderId);

        if (pickup == null || destination == null || rider == null) {
            throw new EntityNotFoundException("pickup, destination, or rider do not exist");
        }

        trip.setCreated(new Date());
        trip.setDestination(destination);
        trip.setPickup(pickup);
        trip.setState(TripState.CREATED);
        trip.setRider(rider);

        em.persist(trip);
        em.flush();

        TripDTO tripDTO = getTripDTO(trip);
        updateFare(tripDTO);

        return tripDTO;
    }

    @Override
    @Transactional
    public void confirm(Long tripId) throws EntityNotFoundException, IllegalStateException, InvalidTripException {
        var tripDAO = daoFactory.createTripDAO();
        var trip = tripDAO.findById(tripId);

        if (trip == null) {
            throw new EntityNotFoundException("No trip with the id: " + tripId + " found");
        }

        if (trip.getRider() == null || trip.getState() != TripState.CREATED) {
            throw new IllegalStateException("The trip was in the " + trip.getState() + " state and the rider was: " + trip.getRider());
        }

        var fare = matchingService.calculateFare(getTripDTO(trip));

        trip.setState(TripState.QUEUED);
        matchingService.queueTripForMatching(tripId);

        em.flush();
    }

    @Override
    @Transactional
    public void match(Long tripId, MatchDTO match) throws EntityNotFoundException, DriverNotAvailableException, IllegalStateException {
        var driverDAO = daoFactory.createDriverDAO();
        var vehicleDAO = daoFactory.createVehicleDAO();
        var tripDAO = daoFactory.createTripDAO();

        ITrip trip = tripDAO.findById(tripId);
        IDriver driver = driverDAO.findById(match.getDriverId());
        IVehicle vehicle = vehicleDAO.findById(match.getVehicleId());

        if (trip == null || driver == null || vehicle == null) {
            handleTripMatchingFailure(tripId);
            throw new EntityNotFoundException("Trip, Vehicle, or Driver not found");
        }

        if (!tripDAO.findDriversActiveTrips(match.getDriverId()).isEmpty()) {
            handleTripMatchingFailure(tripId);
            throw new DriverNotAvailableException("The driver is not currently available");
        }

        if (trip.getRider() == null || trip.getState() != TripState.QUEUED) {
            handleTripMatchingFailure(tripId);
            throw new IllegalStateException("The rider's trip is null or the trip has not been in a queued state");
        }

        em.lock(trip, LockModeType.READ);

        IMatch _match = modelFactory.createMatch();
        _match.setTrip(trip);
        _match.setDate(new Date());
        _match.setFare(getMoney(match.getFare()));
        _match.setVehicle(vehicle);
        _match.setDriver(driver);

        trip.setState(TripState.MATCHED);
        trip.setMatch(_match);

        em.persist(_match);
        em.flush();

        em.lock(trip, LockModeType.NONE);
    }

    @Override
    @Transactional
    public void complete(Long tripId, TripInfoDTO tripInfoDTO) throws EntityNotFoundException {
        var tripDAO = daoFactory.createTripDAO();

        ITrip trip = tripDAO.findById(tripId);

        if (trip == null) {
            throw new EntityNotFoundException("The trip with id: " + tripId + " does not exist");
        }

        ITripInfo tripInfo = getTripInfo(tripInfoDTO);
        tripInfo.setTrip(trip);

        IMoney money = getMoney(tripInfoDTO.getFare());
        ITripReceipt receipt = modelFactory.createTripReceipt();
        receipt.setTotal(money);
        tripInfo.setReceipt(receipt);

        trip.setState(TripState.COMPLETED);
        trip.setTripInfo(tripInfo);

        em.persist(receipt);
        em.persist(tripInfo);
        em.flush();
    }

    @Override
    @Transactional
    public void cancel(Long tripId) throws EntityNotFoundException {
        var tripDAO = daoFactory.createTripDAO();

        ITrip trip = tripDAO.findById(tripId);

        if (trip == null) {
            throw new EntityNotFoundException("The trip with id: " + tripId + " does not exist");
        }

        trip.setState(TripState.CANCELLED);
        em.flush();
    }

    @Override
    @Transactional
    public boolean addStop(TripDTO trip, Long locationId) throws EntityNotFoundException, IllegalStateException {
        var tripDAO = daoFactory.createTripDAO();
        var locationDAO = daoFactory.createLocationDAO();

        ITrip _trip = tripDAO.findById(trip.getId());
        ILocation _location = locationDAO.findById(locationId);

        if (_trip == null || _location == null) {
            throw new EntityNotFoundException("The trip or location does not exist");
        }

        if (_trip.getState() != TripState.CREATED) {
            throw new IllegalStateException("The trip has not been in the created state");
        }

        if (_trip.getStops().stream().anyMatch(stop -> Objects.equals(stop.getId(), locationId))) {
            return false;
        }

        _trip.addStop(_location);
        trip.getStops().add(locationId);

        updateTripFare(trip);

        em.flush();
        return true;
    }

    @Override
    @Transactional
    public boolean removeStop(TripDTO trip, Long locationId) throws EntityNotFoundException, IllegalStateException {
        var tripDAO = daoFactory.createTripDAO();
        var locationDAO = daoFactory.createLocationDAO();

        ITrip _trip = tripDAO.findById(trip.getId());
        ILocation _location = locationDAO.findById(locationId);

        if (_trip == null || _location == null) {
            throw new EntityNotFoundException("The trip or location does not exist");
        }

        if (_trip.getState() != TripState.CREATED) {
            throw new IllegalStateException("The trip has not been in the created state");
        }

        if (!_trip.getStops().stream().anyMatch(stop -> Objects.equals(stop.getId(), locationId))) {
            return false;
        }

        _trip.setStops(_trip.getStops().stream().filter(stop -> !Objects.equals(stop.getId(), locationId)).collect(Collectors.toList()));
        trip.getStops().remove(locationId);

        updateTripFare(trip);

        em.flush();
        return true;
    }

    @Override
    @Transactional
    public void delete(Long tripId) throws EntityNotFoundException {
        var tripDAO = daoFactory.createTripDAO();

        ITrip _trip = tripDAO.findById(tripId);

        if (_trip == null) {
            throw new EntityNotFoundException("The trip does not exist");
        }

        em.remove(_trip);
        em.flush();
    }

    @Override
    public TripDTO find(Long tripId) {
        var tripDAO = daoFactory.createTripDAO();
        ITrip _trip = tripDAO.findById(tripId);

        if (_trip == null) {
            return null;
        }

        TripDTO tripDTO = getTripDTO(_trip);

        updateFare(tripDTO);

        return tripDTO;
    }

    private TripDTO getTripDTO(ITrip trip) {
        TripDTO tripDTO = new TripDTO();

        tripDTO.setPickupId(trip.getPickup().getId());
        tripDTO.setDestinationId(trip.getDestination().getId());
        tripDTO.setRiderId(trip.getRider().getId());
        tripDTO.setStops(trip.getStops().stream().map(ILocation::getId).collect(Collectors.toList()));
        tripDTO.setId(trip.getId());

        return tripDTO;
    }

    private IMoney getMoney(MoneyDTO moneyDTO) {
        var money = modelFactory.createMoney();

        money.setCurrency(moneyDTO.getCurrency());
        money.setCurrencyValue(moneyDTO.getValue());

        return money;
    }

    private ITripInfo getTripInfo(TripInfoDTO tripInfoDTO) {
        var tripInfo = modelFactory.createTripInfo();

        tripInfo.setCompleted(tripInfoDTO.getCompleted());
        tripInfo.setDistance(tripInfoDTO.getDistance());

        return tripInfo;
    }

    private void updateFare(TripDTO tripDTO) {
        try {
            tripDTO.setFare(matchingService.calculateFare(tripDTO));
        } catch (InvalidTripException e) {
            tripDTO.setFare(null);
        }
    }

    private void updateTripFare(TripDTO trip) {
        try {
            trip.setFare(matchingService.calculateFare(trip));
        } catch (InvalidTripException e) {
            trip.setFare(null);
        }
    }

    private void handleTripMatchingFailure(Long tripId) {
        matchingService.queueTripForMatching(tripId);
    }
}

