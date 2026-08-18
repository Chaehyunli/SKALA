package com.skala.day1.domain;

/** 밖으로 나가지 않는 안쪽 모델. ownerId 로 소유자를 가린다. */
public record Order(String id, String ownerId, String item, OrderStatus status, String eta) {}
