package com.cms.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for tests that need a real Microsoft SQL Server. The container is shared by all subclasses
 * in the JVM. Tests are skipped (not failed) when Docker is not available on the machine.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractSqlServerTest {

    @Container
    @ServiceConnection
    protected static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
}
