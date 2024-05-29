package dst.ass2.ioc.di.impl;

import dst.ass2.ioc.di.*;
import dst.ass2.ioc.di.annotation.*;

import java.lang.reflect.*;
import java.util.*;


// The ObjectContainer class is responsible for creating objects and managing their dependencies.
public class ObjectContainer implements IObjectContainer {

    private Properties appProperties;

    //Managing singleton instances
    private HashMap<Class<?>, Object> singletonInstances = new HashMap<>();

    public ObjectContainer(Properties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public Properties getProperties() {
        return appProperties;
    }


    // Create new objects.
    // Hint: When injectFields is called, it iterates over the fields of the object. If a field has the @Inject annotation,
        // it attempts to get an instance of the field's type by calling getObject(targetType).  If the target type also has
        // fields with the @Inject annotation, getObject will again call injectFields, which in turn may call getObject again,
        // and so on. This is a form of recursion, where a method indirectly calls itself.
    // synchronized keyword is used to prevent multiple threads from accessing the same object at the same time.
        @Override
    synchronized public <T> T getObject(Class<T> type) throws ObjectCreationException {

        // Validate id´f @Component annotation is present.
        validateComponentAnnotation(type);


        // If the object is already created, return the object.
        if (singletonInstances.containsKey(type))
            return (T) singletonInstances.get(type);

        // Clone ensures that we don't modify the original properties, so a new copy not a new reference.
        Properties copiedProperties = (Properties) this.appProperties.clone();

        // Creates an instance of a given class.
        T obj = instantiateObject(type);

        // Retrieve all fields and methods of the class.
        List<Field> allFields = retrieveAllFields(type);
        List<Method> allMethods = retrieveAllMethods(type);

        // Inject fields, set property values and call initializer methods.
        injectFields(obj, allFields, copiedProperties);
        setPropertyValues(obj, allFields, copiedProperties);

        // After instantiating a component, methods (either declared or inherited) annotated with @Initialize should be
            // executed by the container
        callInitializerMethods(obj, allMethods);

        // If the component is a singleton, store the instance.
        if (type.getAnnotation(Component.class).scope().equals(Scope.SINGLETON)) {
            singletonInstances.put(type, obj);
        }

        return obj;
    }

    // Creates an instance of a given class
    private <T> T instantiateObject(Class<T> type) throws ObjectCreationException {
        try {

            // Retrieve the non-argument constructor.
            Constructor<T> constructor = type.getDeclaredConstructor();

            // Make the constructor accessible, in case its private.
            constructor.setAccessible(true);

            // Create a new instance using the constructor.
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                InvocationTargetException e) {
            throw new ObjectCreationException(e);
        }
    }


    // Responsible for validating the component annotation.
    private void validateComponentAnnotation(Class<?> type) throws InvalidDeclarationException {
        if (type.getAnnotation(Component.class) == null) {
            throw new InvalidDeclarationException("The class is not a component");
        }
    }

    private List<Field> retrieveAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>(Arrays.asList(type.getDeclaredFields()));
        for (Class<?> sclass = type.getSuperclass(); sclass != null; sclass = sclass.getSuperclass()) {
            fields.addAll(Arrays.asList(sclass.getDeclaredFields()));
        }
        return fields;
    }

    private List<Method> retrieveAllMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>(Arrays.asList(type.getDeclaredMethods()));
        for (Class<?> sclass = type.getSuperclass(); sclass != null; sclass = sclass.getSuperclass()) {
            methods.addAll(Arrays.asList(sclass.getDeclaredMethods()));
        }
        return methods;
    }

    // Injecting dependencies into the fields of an object.
    private void injectFields(Object obj, List<Field> fields, Properties properties) throws ObjectCreationException {
        for (Field field : fields) {

            // Retrieve the inject annotation of the current field.
            Inject annotation = field.getAnnotation(Inject.class);
            if (annotation == null) continue;
            // Make the field accessible, in case its private.
            field.setAccessible(true);

            try {
                // Specify the target type of the field, if the Inject annotation has a target type, it uses this one,
                // the field type else.
                Class<?> targetType = (annotation.targetType() != Void.class) ? annotation.targetType() : field.getType();

                // Get the object of the target type (if annotated with @Inject --> recursive call).
                Object injectable = getObject(targetType);

                // Set the field value to the object of the target type.
                field.set(obj, injectable);
            } catch (ObjectCreationException e) {
                if (!annotation.optional())
                    throw e;
            } catch (IllegalArgumentException | IllegalAccessException e) {
                if (!annotation.optional())
                    throw new InvalidDeclarationException(e);
            }
        }
    }

    // Inject property values into the fields of an object.
    // synchronized keyword is not used because the method is called from getObject which is synchronized.
     private void setPropertyValues(Object obj, List<Field> fields, Properties properties) throws ObjectCreationException {
        for (Field field : fields) {

            // Retrieve the Property annotation of the current field.
            Property annotation = field.getAnnotation(Property.class);
            if (annotation == null)
                continue;

            String propertyName = annotation.value();
            String propertyValue = properties.getProperty(propertyName);

            if (propertyValue == null)
                throw new ObjectCreationException(propertyName + " is not defined in the properties");

            try {
                field.setAccessible(true);
                Object value;
                Class<?> fieldType = field.getType();

                // If the field type is a string, set the value to the property value.
                if (fieldType == String.class) {
                    value = propertyValue;
                    // If the field type is a primitive, convert the property value to the primitive type.
                } else if (fieldType.isPrimitive()) {
                    if (fieldType == int.class) {
                        value = Integer.parseInt(propertyValue);
                    } else if (fieldType == float.class) {
                        value = Float.parseFloat(propertyValue);
                    } else if (fieldType == boolean.class) {
                        value = Boolean.parseBoolean(propertyValue);
                    } else {
                        throw new TypeConversionException("Not supported for type " + fieldType);
                    }
                } else {
                    // If value is not primitive or string use valueOf method to convert the string to the object.
                    value = fieldType.getDeclaredMethod("valueOf", String.class).invoke(null, propertyValue);
                }
                field.set(obj, value);
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new TypeConversionException(e);
            }
        }
    }

    private void callInitializerMethods(Object obj, List<Method> methods) throws ObjectCreationException {
        Set<String> seenInitializers = new HashSet<>();
        for (Method method : methods) {
            // If not annotated with @Initialize, skip.
            if (!method.isAnnotationPresent(Initialize.class))
                continue;

            // Make accessible, in case its private.
            method.setAccessible(true);

            // If the initializer has already been called, skip.
            if (seenInitializers.contains(method.getName()))
                continue;
            seenInitializers.add(method.getName());

            // If the initializer has parameters, throw an exception.
            if (method.getParameterCount() > 0)
                throw new InvalidDeclarationException("Initializer should not have any parameters: " + method);

            // Try to invoke (execute) the method.
            try {
                method.invoke(obj);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ObjectCreationException(e);
            }
        }
    }
}
