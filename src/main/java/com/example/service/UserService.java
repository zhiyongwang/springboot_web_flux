package com.example.service;

import com.example.exception.ResourceNotFoundException;
import com.example.vo.User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wangzhiyong
 * @date 2018/3/9
 */
@Service
public class UserService {
    private final Map<String, User> data = new ConcurrentHashMap<>();

    public Flux<User> list() {
        return Flux.fromIterable(data.values());
    }

    public Flux<User> getById(final Flux<String> ids) {
        return ids.flatMap(id -> Mono.justOrEmpty(this.data.get(id)));
    }

    public Mono<User> getById(final String id) {
        return Mono.justOrEmpty(this.data.get(id))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException()));
    }

    public Flux<User> createOrUpdate(final Flux<User> users) {
        return users.doOnNext(user -> this.data.put(user.getId(), user));
    }

    public Mono<User> createOrUpdate(final User user) {
        this.data.put(user.getId(), user);
        return Mono.just(user);
    }

    public Mono<User> delete(final String id) {
        return Mono.justOrEmpty(this.data.remove(id));
    }
}
