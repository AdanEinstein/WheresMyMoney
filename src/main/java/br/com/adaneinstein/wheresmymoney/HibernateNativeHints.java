package br.com.adaneinstein.wheresmymoney;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class HibernateNativeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
        registerHibernateGeneratedLoggers(hints, classLoader, "org.hibernate.Session");
        registerHibernateGeneratedLoggers(hints, classLoader, "org.hibernate.community.dialect.SQLiteDialect");
        registerPackageTypes(hints, classLoader, "org.hibernate.Session", "org.hibernate.boot.models.annotations.internal");
        registerPackageTypes(hints, classLoader, "org.hibernate.Session", "org.hibernate.annotations");
        registerArrayTypesForPackage(hints, classLoader, "org.hibernate.Session", "org.hibernate.event.spi");

        registerTypeConstructors(hints, classLoader, "org.hibernate.boot.model.relational.ColumnOrderingStrategyStandard");
        registerTypeConstructors(hints, classLoader, "org.hibernate.boot.model.relational.ColumnOrderingStrategyLegacy");
        registerTypeConstructors(hints, classLoader, "org.hibernate.boot.model.relational.ColumnOrderingStrategyNoop");
        registerTypeConstructors(hints, classLoader, "org.hibernate.community.dialect.SQLiteDialect");

        // PropertyTheme faz Class.forName() para toda classe referenciada nos .properties de tema
        registerPackageTypes(hints, classLoader, "com.googlecode.lanterna.gui2.MultiWindowTextGUI", "com.googlecode.lanterna");
    }

    private static void registerTypeConstructors(RuntimeHints hints, @Nullable ClassLoader classLoader, String className) {
        Class<?> type = loadClass(classLoader, className);
        if (type == null) {
            return;
        }

        hints.reflection().registerType(
                type,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.INVOKE_DECLARED_METHODS
        );
    }

    private static void registerPackageTypes(
            RuntimeHints hints,
            @Nullable ClassLoader classLoader,
            String anchorClassName,
            String packageName
    ) {
        Class<?> anchorClass = loadClass(classLoader, anchorClassName);
        if (anchorClass == null) {
            return;
        }

        URL resource = anchorClass.getResource('/' + anchorClassName.replace('.', '/') + ".class");
        if (resource == null) {
            return;
        }

        String packagePath = packageName.replace('.', '/') + '/';
        try {
            URLConnection connection = resource.openConnection();
            if (!(connection instanceof JarURLConnection jarConnection)) {
                return;
            }

            try (JarFile jarFile = jarConnection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (!entryName.startsWith(packagePath) || !entryName.endsWith(".class")) {
                        continue;
                    }

                    String className = entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
                    registerTypeConstructors(hints, classLoader, className);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void registerArrayTypesForPackage(
            RuntimeHints hints,
            @Nullable ClassLoader classLoader,
            String anchorClassName,
            String packageName
    ) {
        Class<?> anchorClass = loadClass(classLoader, anchorClassName);
        if (anchorClass == null) {
            return;
        }

        URL resource = anchorClass.getResource('/' + anchorClassName.replace('.', '/') + ".class");
        if (resource == null) {
            return;
        }

        String packagePath = packageName.replace('.', '/') + '/';
        try {
            URLConnection connection = resource.openConnection();
            if (!(connection instanceof JarURLConnection jarConnection)) {
                return;
            }

            try (JarFile jarFile = jarConnection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (!entryName.startsWith(packagePath) || !entryName.endsWith(".class")) {
                        continue;
                    }

                    String className = entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
                    registerArrayType(hints, classLoader, className);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void registerArrayType(RuntimeHints hints, @Nullable ClassLoader classLoader, String className) {
        Class<?> arrayType = loadClass(classLoader, "[L" + className + ";");
        if (arrayType != null) {
            hints.reflection().registerType(arrayType);
        }
    }

    private static void registerHibernateGeneratedLoggers(RuntimeHints hints, @Nullable ClassLoader classLoader, String anchorClassName) {
        Class<?> anchorClass = loadClass(classLoader, anchorClassName);
        if (anchorClass == null) {
            return;
        }

        URL resource = anchorClass.getResource('/' + anchorClassName.replace('.', '/') + ".class");
        if (resource == null) {
            return;
        }

        try {
            URLConnection connection = resource.openConnection();
            if (!(connection instanceof JarURLConnection jarConnection)) {
                return;
            }

            try (JarFile jarFile = jarConnection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (!entryName.endsWith("_$logger.class")) {
                        continue;
                    }

                    String implClassName = entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
                    String interfaceClassName = implClassName.substring(0, implClassName.length() - "_$logger".length());
                    registerLoggerImplementation(hints, classLoader, implClassName, interfaceClassName);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void registerLoggerImplementation(RuntimeHints hints, @Nullable ClassLoader classLoader, String implementationClassName, String interfaceClassName) {
        Class<?> implementationClass = loadClass(classLoader, implementationClassName);
        if (implementationClass != null) {
            hints.reflection().registerType(
                    implementationClass,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.INVOKE_DECLARED_METHODS
            );
        }

        Class<?> interfaceClass = loadClass(classLoader, interfaceClassName);
        if (interfaceClass == null) {
            return;
        }

        for (Method method : interfaceClass.getMethods()) {
            hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
        }

        if (implementationClass == null) {
            return;
        }

        for (Method method : implementationClass.getDeclaredMethods()) {
            if (method.getName().endsWith("$str")) {
                hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
            }
        }
    }

    @Nullable
    private static Class<?> loadClass(@Nullable ClassLoader classLoader, String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
