package dst.ass2.service.auth.impl;

import dst.ass1.jpa.dao.IDAOFactory;
import dst.ass1.jpa.model.IRider;
import dst.ass2.service.api.auth.AuthenticationException;
import dst.ass2.service.api.auth.NoSuchUserException;
import dst.ass2.service.auth.ICachingAuthenticationService;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
@ManagedBean
public class CachingAuthenticationService implements ICachingAuthenticationService {

    private final ConcurrentHashMap<String, byte[]> passwords = new ConcurrentHashMap<>(); // Email -> password hash
    private final ConcurrentHashMap<String, String> emails = new ConcurrentHashMap<>();    // Token -> email

    @PersistenceContext(name = "dst")
    private EntityManager em;

    @Inject
    private IDAOFactory daoFactory;

    @Override
    public void changePassword(String email, String newPassword) throws NoSuchUserException {
        IRider rider = getRiderByEmail(email);
        byte[] newSha1 = hashPassword(newPassword);
        rider.setPassword(newSha1);
        passwords.put(email, newSha1);
    }

    @Override
    public String getUser(String token) {
        return (token != null) ? emails.get(token) : null;
    }

    @Override
    public boolean isValid(String token) {
        return (token != null) && emails.containsKey(token);
    }

    @Override
    public boolean invalidate(String token) {
        return (token != null) && (emails.remove(token) != null);
    }

    @Override
    @Transactional
    public String authenticate(String email, String password) throws NoSuchUserException, AuthenticationException {
        byte[] savedPassword = getPasswordHash(email);
        if (savedPassword == null) {
            savedPassword = getRiderByEmail(email).getPassword();
            passwords.put(email, savedPassword);
        }

        if (!MessageDigest.isEqual(savedPassword, hashPassword(password))) {
            throw new AuthenticationException();
        }

        String token = UUID.randomUUID().toString();
        emails.put(token, email);
        return token;
    }

    @Override
    @PostConstruct
    public void loadData() {
        passwords.putAll(daoFactory.createRiderDAO()
                .findAll()
                .stream()
                .collect(Collectors.toMap(IRider::getEmail, IRider::getPassword)));
    }

    @Override
    public void clearCache() {
        passwords.clear();
    }

    private byte[] hashPassword(String password) {
        try {
            return MessageDigest.getInstance("SHA1").digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private IRider getRiderByEmail(String email) throws NoSuchUserException {
        IRider rider = daoFactory.createRiderDAO().findByEmail(email);
        if (rider == null) {
            throw new NoSuchUserException();
        }
        return rider;
    }

    private byte[] getPasswordHash(String email) {
        return passwords.get(email);
    }
}
