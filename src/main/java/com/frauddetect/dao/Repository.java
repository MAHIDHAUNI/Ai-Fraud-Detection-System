package com.frauddetect.dao;

import com.frauddetect.exception.DatabaseException;

import java.util.List;
import java.util.Optional;

/**
 * RUBRIC: 2 - Collections & Generics
 * Generic repository interface providing standard CRUD operations for domain entities.
 *
 * @param <T>  the domain entity type
 * @param <ID> the primary key identifier type
 */
public interface Repository<T, ID> {

    /**
     * Persists a new entity into the database.
     *
     * @param entity the entity to persist
     * @return the persisted entity with generated keys populated
     * @throws DatabaseException if persistence fails
     */
    T save(T entity) throws DatabaseException;

    /**
     * Retrieves an entity by its unique identifier.
     *
     * @param id the primary key
     * @return Optional containing the entity if found, empty otherwise
     * @throws DatabaseException on database error
     */
    Optional<T> findById(ID id) throws DatabaseException;

    /**
     * Retrieves all entities in the table.
     *
     * @return List of all entities
     * @throws DatabaseException on database error
     */
    List<T> findAll() throws DatabaseException;

    /**
     * Updates an existing entity's state in the database.
     *
     * @param entity the entity with updated values
     * @return true if updated successfully, false if not found
     * @throws DatabaseException on database error
     */
    boolean update(T entity) throws DatabaseException;

    /**
     * Deletes an entity by its identifier.
     *
     * @param id the primary key
     * @return true if deleted, false if not found
     * @throws DatabaseException on database error
     */
    boolean delete(ID id) throws DatabaseException;
}
