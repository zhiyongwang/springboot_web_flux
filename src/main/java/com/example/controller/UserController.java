package com.example.controller;

import com.example.exception.ResourceNotFoundException;
import com.example.service.UserService;
import com.example.vo.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST API
 * @author wangzhiyong
 * @date 2018/3/9
 */
@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Resource not found")
    @ExceptionHandler(ResourceNotFoundException.class)
    public void notFound() {
    }

    @GetMapping("")
    public Flux<User> list() {
        return this.userService.list();
    }

    @GetMapping("/{id}")
    public Mono<User> getById(@PathVariable("id") final String id) {
        return this.userService.getById(id);
    }

    @PostMapping("")
    public Flux<User> create(@RequestBody final Flux<User>  users) {
        return this.userService.createOrUpdate(users);
    }

    @PutMapping("/{id}")
    public Mono<User>  update(@PathVariable("id") final String id, @RequestBody final User user) {
        Objects.requireNonNull(user);
        user.setId(id);
        return this.userService.createOrUpdate(user);
    }

    @DeleteMapping("/{id}")
    public Mono<User>  delete(@PathVariable("id") final String id) {
        return this.userService.delete(id);
    }
}
