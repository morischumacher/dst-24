package dst.ass2.service.facade.impl;


import javax.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Name binding annotation for authentication "AuthenticationRequired"
@NameBinding
// Used on methods and types
@Target({ElementType.METHOD, ElementType.TYPE})
// Can be inspected at runtime
@Retention(value = RetentionPolicy.RUNTIME)
public @interface AuthenticationRequired {
}