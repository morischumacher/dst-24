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

//Used of Java EE to specify that a bean should be used a singleton (only one instance for each JVM), useful for shared
// states across the application
@Singleton
@ManagedBean
public class CachingAuthenticationService implements ICachingAuthenticationService {

    // Used for authentication
    private final ConcurrentHashMap<String, byte[]> passwords = new ConcurrentHashMap<>(); // Email -> password hash
    // Used for token validation
    private final ConcurrentHashMap<String, String> emails = new ConcurrentHashMap<>();    // Token -> email

    // Injecting the JPA EntityManager
    @PersistenceContext(name = "dst")
    private EntityManager em;

    @Inject
    private IDAOFactory daoFactory;


    //Changes password in the hash
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

        //load password if they are not in the cache
        if (savedPassword == null) {
            savedPassword = getRiderByEmail(email).getPassword();
            passwords.put(email, savedPassword);
        }
        //Compare the password with the saved password
        if (!MessageDigest.isEqual(savedPassword, hashPassword(password))) {
            throw new AuthenticationException();
        }

        //Generate a token and store it in the cache
        String token = UUID.randomUUID().toString();
        emails.put(token, email);
        return token;
    }

    @Override
    //Done after initial startup (dp injection)
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


    // Helper method to hash a password
    private byte[] hashPassword(String password) {
        try {
            return MessageDigest.getInstance("SHA1").digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // Helper method to get a rider by email
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
