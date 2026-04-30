package com.team05.fooddelivery.delivery.config;

import java.util.List;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import org.jspecify.annotations.NonNull;


/**
 * Cassandra configuration for automatic keyspace creation on application startup.
 * Handles both keyspace initialization and connection management.
 */
@Configuration
public class CreateKeyspaceConfiguration extends AbstractCassandraConfiguration implements BeanClassLoaderAware {

    @Value("${spring.cassandra.keyspace-name:fooddeliveryks}")
    private String keyspaceName;

    @Value("${spring.cassandra.replication-factor:1}")
    private int replicationFactor;

    @Value("${spring.cassandra.contact-points:cassandra}")
    private String contactPoints;

    @Override
    @NonNull
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    @NonNull
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    /**
     * Creates the keyspace specification with SimpleStrategy replication.
     * SimpleStrategy is used for single-datacenter deployments (suitable for Docker).
     */
    @Override
    @NonNull
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        final CreateKeyspaceSpecification keyspaceSpecification = CreateKeyspaceSpecification.createKeyspace(keyspaceName)
                .ifNotExists()
                .with(KeyspaceOption.DURABLE_WRITES, true)
                .withSimpleReplication(replicationFactor);

        return List.of(keyspaceSpecification);
    }

}

