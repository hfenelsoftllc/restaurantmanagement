package com.hfenelsoftllc.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.util.List;

/**
 * Cassandra configuration that bootstraps the keyspace and wires Spring Data Cassandra.
 *
 * <p>Extends {@link AbstractCassandraConfiguration} so Spring Data scans entity packages,
 * auto-creates the keyspace on startup, and registers the {@link org.springframework.data.cassandra.core.CassandraTemplate} bean.
 * The parent class provides all necessary beans (CqlSession, CassandraTemplate, etc.).</p>
 *
 * <p>Schema action is {@code CREATE_IF_NOT_EXISTS} — all tables and UDTs are created
 * automatically from annotated entity classes. In production, switch to {@code NONE}
 * and manage schema migrations explicitly.</p>
 */
@Configuration
@EnableCassandraRepositories(basePackages = "com.hfenelsoftllc.order.repository")
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${spring.cassandra.keyspace-name:order_keyspace}")
    private String keyspaceName;

    @Value("${spring.cassandra.contact-points:127.0.0.1}")
    private String contactPoints;

    @Value("${spring.cassandra.port:9042}")
    private int port;

    @Value("${spring.cassandra.local-datacenter:datacenter1}")
    private String localDatacenter;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    protected String getLocalDataCenter() {
        return localDatacenter;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    /**
     * Keyspace DDL executed before any table/UDT creation.
     * SimpleStrategy with RF=1 is suitable for single-node dev/test.
     * Replace with NetworkTopologyStrategy in production.
     */
    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        return List.of(
                CreateKeyspaceSpecification.createKeyspace(keyspaceName)
                        .ifNotExists()
                        .withSimpleReplication(1)
        );
    }

    /** Create all tables and UDTs if they do not yet exist. */
    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    /** Scan this package for @Table and @UserDefinedType entities. */
    @Override
    public String[] getEntityBasePackages() {
        return new String[]{"com.hfenelsoftllc.order.entity"};
    }
}
