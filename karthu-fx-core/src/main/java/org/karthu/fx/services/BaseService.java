package org.karthu.fx.services;

/**
 * Base interface for services.
 */
public interface BaseService {

    /**
     * Initialize the service.
     */
    void initialize();

    /**
     * Shutdown the service.
     */
    void shutdown();
}
