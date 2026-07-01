package com.alhrb.forestry.mapper;

import java.util.List;

/**
 * Базовый интерфейс маппера с универсальными методами
 * @param <E> Entity класс
 * @param <RQ> Request DTO класс
 * @param <RS> Response DTO класс
 */
public interface BaseMapper<E, RQ, RS> {

    /**
     * Преобразование Request DTO в Entity
     */
    E toEntity(RQ request);

    /**
     * Преобразование Entity в Response DTO
     */
    RS toResponse(E entity);

    /**
     * Преобразование списка Entity в список Response DTO
     */
    List<RS> toResponseList(List<E> entities);

    /**
     * Преобразование списка Request DTO в список Entity
     */
    List<E> toEntityList(List<RQ> requests);

    /**
     * Обновление существующей Entity из Request DTO
     */
    void updateEntity(RQ request, @org.mapstruct.MappingTarget E entity);
}