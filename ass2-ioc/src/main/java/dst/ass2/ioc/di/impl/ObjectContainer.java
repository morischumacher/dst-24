package dst.ass2.ioc.di.impl;

import dst.ass2.ioc.di.*;
import dst.ass2.ioc.di.annotation.*;

import java.lang.reflect.*;
import java.util.*;

public class ObjectContainer implements IObjectContainer {

    private Properties appProperties;
    private HashMap<Class<?>, Object> singletonInstances = new HashMap<>();

    public ObjectContainer(Properties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public Properties getProperties() {
        return appProperties;
    }

    @Override
    synchronized public <T> T getObject(Class<T> type) throws ObjectCreationException {
        validateComponentAnnotation(type);

        if (singletonInstances.containsKey(type))
            return (T) singletonInstances.get(type);

        Properties copiedProperties = (Properties) this.appProperties.clone();
        T obj = instantiateObject(type);

        List<Field> allFields = retrieveAllFields(type);
        List<Method> allMethods = retrieveAllMethods(type);

        injectFields(obj, allFields, copiedProperties);
        setPropertyValues(obj, allFields, copiedProperties);
        callInitializerMethods(obj, allMethods);

        if (type.getAnnotation(Component.class).scope().equals(Scope.SINGLETON)) {
            singletonInstances.put(type, obj);
        }

        return obj;
    }

    private <T> T instantiateObject(Class<T> type) throws ObjectCreationException {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                InvocationTargetException e) {
            throw new ObjectCreationException(e);
        }
    }

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

    private void injectFields(Object obj, List<Field> fields, Properties properties) throws ObjectCreationException {
        for (Field field : fields) {
            Inject annotation = field.getAnnotation(Inject.class);
            if (annotation == null) continue;
            field.setAccessible(true);

            try {
                Class<?> targetType = (annotation.targetType() != Void.class) ? annotation.targetType() : field.getType();
                Object injectable = getObject(targetType);
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

    private void setPropertyValues(Object obj, List<Field> fields, Properties properties) throws ObjectCreationException {
        for (Field field : fields) {
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
                if (fieldType == String.class) {
                    value = propertyValue;
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
            if (!method.isAnnotationPresent(Initialize.class))
                continue;

            method.setAccessible(true);

            if (seenInitializers.contains(method.getName()))
                continue;
            seenInitializers.add(method.getName());

            if (method.getParameterCount() > 0)
                throw new InvalidDeclarationException("Initializer should not have any parameters: " + method);

            try {
                method.invoke(obj);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ObjectCreationException(e);
            }
        }
    }
}
