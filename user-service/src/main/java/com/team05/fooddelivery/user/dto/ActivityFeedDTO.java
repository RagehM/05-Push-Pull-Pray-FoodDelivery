package com.team05.fooddelivery.user.dto;

import com.team05.shared.model.mongo.AuthEvent;

import java.util.List;

public record ActivityFeedDTO(List<AuthEvent> content,
                              Integer page,
                              Integer size,
                              Integer totalElements) {
}
