package com.team05.fooddelivery.user.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.lang.annotation.Annotation;

public class Reflection {
    public static void testReflection() throws Exception{
        String className = "com.team05.fooddelivery.user.config.JwtConfigurationManager";
        Class<?> clazz = Class.forName(className);
        Method getInstance = clazz.getDeclaredMethod("getInstance");

        System.out.println("Is class found: " + (clazz != null));
        System.out.println("Number of constructors: " + clazz.getDeclaredConstructors().length);
        System.out.println("getInstance output: " + clazz.getDeclaredMethod("getInstance").invoke(getInstance));

        Object instance = getInstance.invoke(getInstance);
        Object instance2 = getInstance.invoke(getInstance);

        System.out.println("Instance 1: " + instance);
        System.out.println("Instance 2: " + instance2);
        System.out.println("Are both instances the same: " + (instance == instance2));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Future<Object>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                barrier.await(); // wait until all 10 threads are ready
                return getInstance.invoke(null); // all call getInstance() at the same time
            }));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        Object expected = futures.get(0).get();
        boolean allSame = true;

        for (int i = 1; i < threadCount; i++) {
            Object result = futures.get(i).get();
            if (result != expected) {
                allSame = false;
                System.out.println("Thread " + i + " got a DIFFERENT instance: " + result);
            }
        }

        System.out.println("All 10 threads returned the same instance: " + allSame);

        // ---- Spring Stereotype Annotation Check ----
        List<String> forbiddenAnnotations = List.of(
                "org.springframework.stereotype.Component",
                "org.springframework.stereotype.Service",
                "org.springframework.stereotype.Repository",
                "org.springframework.context.annotation.Configuration",
                "org.springframework.web.bind.annotation.RestController",
                "org.springframework.stereotype.Controller"
        );

        System.out.println("\n-- Checking for Spring stereotype annotations --");

        Annotation[] presentAnnotations = clazz.getAnnotations();
        boolean hasSpringAnnotation = false;

        for (Annotation annotation : presentAnnotations) {
            String annotationName = annotation.annotationType().getName();
            if (forbiddenAnnotations.contains(annotationName)) {
                hasSpringAnnotation = true;
                System.out.println("FOUND forbidden annotation: @" + annotation.annotationType().getSimpleName());
            }
        }

        if (!hasSpringAnnotation) {
            System.out.println("No Spring stereotype annotations found. ✓");
        }

        System.out.println("Class is free of Spring stereotypes: " + !hasSpringAnnotation);

    }
    public static void main(String[] args) throws Exception {
        testReflection();
    }
}