package com.skala.lab0.web;

/** 밖으로 나가는 모양. 안쪽 모델(Snack)을 그대로 내보내지 않는다. */
public record SnackResponse(String name, String reason) {}
