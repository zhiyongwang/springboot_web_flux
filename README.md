# WebFlux 开发响应式 Web 应用

标签 ： webflux

---

Spring 5 是流行的 Spring 框架的下一个重大的版本升级。Spring 5 中最重要改动是把反应式编程的思想应用到了框架的各个方面，Spring 5 的反应式编程以 Reactor库为基础。在之前的文章《使用Reactor进行反应式编程》中，已经对Reactor库进行了详细的介绍。读者如果需要了解 Reactor，可以参考之前的那篇文章。Spring 5框架所包含的内容很多，本文只重点介绍其中新增的 WebFlux 模块。开发人员可以使用 WebFlux 创建高性能的 Web 应用和客户端。本文对 WebFlux 模块进行了详细介绍，包括其中的 HTTP、服务器推送事件和 WebSocket 支持。

## WebFlux 简介
WebFlux 模块的名称是 spring-webflux，名称中的 Flux 来源于 Reactor 中的类 Flux。该模块中包含了对反应式 HTTP、服务器推送事件和 WebSocket 的客户端和服务器端的支持。对于开发人员来说，比较重要的是服务器端的开发，这也是本文的重点。在服务器端，WebFlux 支持两种不同的编程模型：第一种是 Spring MVC 中使用的基于 Java 注解的方式；第二种是基于 Java 8 的 lambda 表达式的函数式编程模型。这两种编程模型只是在代码编写方式上存在不同。它们运行在同样的反应式底层架构之上，因此在运行时是相同的。WebFlux 需要底层提供运行时的支持，WebFlux 可以运行在支持 Servlet 3.1 非阻塞 IO API 的 Servlet 容器上，或是其他异步运行时环境，如 Netty 和 Undertow。

最方便的创建 WebFlux 应用的方式是使用 Spring Boot 提供的应用模板。直接访问 Spring Initializ 网站（http://start.spring.io/），选择创建一个 Maven 或 Gradle 项目。Spring Boot 的版本选择 2.0.0 M2。在添加的依赖中，选择 Reactive Web。最后输入应用所在的分组和名称，点击进行下载即可。需要注意的是，只有在选择了 Spring Boot 2.0.0 M2 之后，依赖中才可以选择 Reactive Web。下载完成之后可以导入到 IDE 中进行编辑。本文的示例代码使用 Intellij IDEA 2017.2 进行编写。

本文从三个方面对 WebFlux 进行介绍。首先是使用经典的基于 Java 注解的编程模型来进行开发，其次是使用 WebFlux 新增的函数式编程模型来进行开发，最后介绍 WebFlux 应用的测试。通过这样循序渐进的方式让读者了解 WebFlux 应用开发的细节。

## Java 注解编程模型
基于 Java 注解的编程模型，对于使用过 Spring MVC 的开发人员来说是再熟悉不过的。在 WebFlux 应用中使用同样的模式，容易理解和上手。我们先从最经典的 Hello World 的示例开始说明。代码清单 1 中的 BasicController 是 REST API 的控制器，通过@RestController 注解来声明。在 BasicController 中声明了一个 URI 为/hello_world 的映射。其对应的方法 sayHelloWorld()的返回值是 Mono<String>类型，其中包含的字符串"Hello World"会作为 HTTP 的响应内容。

清单 1. Hello World 示例
```
@RestController
public class BasicController {
    @GetMapping("/hello_world")
    public Mono<String> sayHelloWorld() {
        return Mono.just("Hello World");
    }
}
```
从代码清单 1 中可以看到，使用 WebFlux 与 Spring MVC 的不同在于，WebFlux 所使用的类型是与反应式编程相关的 Flux 和 Mono 等，而不是简单的对象。对于简单的 Hello World 示例来说，这两者之间并没有什么太大的差别。对于复杂的应用来说，反应式编程和负压的优势会体现出来，可以带来整体的性能的提升。

### REST API

 1. 列表项

简单的 Hello World 示例并不足以说明 WebFlux 的用法。在下面的小节中，本文将介绍其他具体的实例。先从 REST API 开始说起。REST API 在 Web 服务器端应用中占据了很大的一部分。我们通过一个具体的实例来说明如何使用 WebFlux 来开发 REST API。

该 REST API 用来对用户数据进行基本的 CRUD 操作。作为领域对象的 User 类中包含了 id、name 和 email 等三个基本的属性。为了对 User 类进行操作，我们需要提供服务类 UserService，如代码清单 2 所示。类 UserService 使用一个 Map 来保存所有用户的信息，并不是一个持久化的实现。这对于示例应用来说已经足够了。类 UserService 中的方法都以 Flux 或 Mono 对象作为返回值，这也是 WebFlux 应用的特征。在方法 getById()中，如果找不到 ID 对应的 User 对象，会返回一个包含了 ResourceNotFoundException 异常通知的 Mono 对象。方法 getById()和 createOrUpdate()都可以接受 String 或 Flux 类型的参数。Flux 类型的参数表示的是有多个对象需要处理。这里使用 doOnNext()来对其中的每个对象进行处理。
清单 2. UserService
```
@Service
class UserService {
    private final Map<String, User> data = new ConcurrentHashMap<>();
 
    Flux<User> list() {
        return Flux.fromIterable(this.data.values());
    }
 
    Flux<User> getById(final Flux<String> ids) {
        return ids.flatMap(id -> Mono.justOrEmpty(this.data.get(id)));
    }
 
    Mono<User> getById(final String id) {
        return Mono.justOrEmpty(this.data.get(id))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException()));
    }
 
    Flux<User> createOrUpdate(final Flux<User> users) {
        return users.doOnNext(user -> this.data.put(user.getId(), user));
    }
 
    Mono<User> createOrUpdate(final User user) {
        this.data.put(user.getId(), user);
        return Mono.just(user);
    }
 
    Mono<User> delete(final String id) {
        return Mono.justOrEmpty(this.data.remove(id));
    }
}
```
代码清单 3 中的类 UserController 是具体的 Spring MVC 控制器类。它使用类 UserService 来完成具体的功能。类 UserController 中使用了注解@ExceptionHandler 来添加了 ResourceNotFoundException 异常的处理方法，并返回 404 错误。类 UserController 中的方法都很简单，只是简单地代理给 UserService 中的对应方法。
清单 3. UserController
```
@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
 
    @Autowired
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
    public Mono<User>getById(@PathVariable("id") final String id) {
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
```
### 服务器推送事件
服务器推送事件（Server-Sent Events，SSE）允许服务器端不断地推送数据到客户端。相对于 WebSocket 而言，服务器推送事件只支持服务器端到客户端的单向数据传递。虽然功能较弱，但优势在于 SSE 在已有的 HTTP 协议上使用简单易懂的文本格式来表示传输的数据。作为 W3C 的推荐规范，SSE 在浏览器端的支持也比较广泛，除了 IE 之外的其他浏览器都提供了支持。在 IE 上也可以使用 polyfill 库来提供支持。在服务器端来说，SSE 是一个不断产生新数据的流，非常适合于用反应式流来表示。在 WebFlux 中创建 SSE 的服务器端是非常简单的。只需要返回的对象的类型是 Flux<ServerSentEvent>，就会被自动按照 SSE 规范要求的格式来发送响应。

代码清单 4 中的 SseController 是一个使用 SSE 的控制器的示例。其中的方法 randomNumbers()表示的是每隔一秒产生一个随机数的 SSE 端点。我们可以使用类 ServerSentEvent.Builder 来创建 ServerSentEvent 对象。这里我们指定了事件名称 random，以及每个事件的标识符和数据。事件的标识符是一个递增的整数，而数据则是产生的随机数。
清单 4. 服务器推送事件示例
```
@RestController
@RequestMapping("/sse")
public class SseController {
    @GetMapping("/randomNumbers")
    public Flux<ServerSentEvent<Integer>> randomNumbers() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(seq -> Tuples.of(seq, ThreadLocalRandom.current().nextInt()))
                .map(data -> ServerSentEvent.<Integer>builder()
                        .event("random")
                        .id(Long.toString(data.getT1()))
                        .data(data.getT2())
                        .build());
    }
}
```
在测试 SSE 时，我们只需要使用 curl 来访问即可。代码清单 5 给出了调用 curl http://localhost:8080/sse/randomNumbers 的结果。
清单 5. SSE 服务器端发送的响应
```
id:0
event:random
data:751025203
 
id:1
event:random
data:-1591883873
 
id:2
event:random
data:-1899224227
```
### WebSocket
WebSocket 支持客户端与服务器端的双向通讯。当客户端与服务器端之间的交互方式比较复杂时，可以使用 WebSocket。WebSocket 在主流的浏览器上都得到了支持。WebFlux 也对创建 WebSocket 服务器端提供了支持。在服务器端，我们需要实现接口 org.springframework.web.reactive.socket.WebSocketHandler 来处理 WebSocket 通讯。接口 WebSocketHandler 的方法 handle 的参数是接口 WebSocketSession 的对象，可以用来获取客户端信息、接送消息和发送消息。代码清单 6 中的 EchoHandler 对于每个接收的消息，会发送一个添加了"ECHO -> "前缀的响应消息。WebSocketSession 的 receive 方法的返回值是一个 Flux<WebSocketMessage>对象，表示的是接收到的消息流。而 send 方法的参数是一个 Publisher<WebSocketMessage>对象，表示要发送的消息流。在 handle 方法，使用 map 操作对 receive 方法得到的 Flux<WebSocketMessage>中包含的消息继续处理，然后直接由 send 方法来发送。

清单 6. WebSocket 的 EchoHandler 示例
```
@Component
public class EchoHandler implements WebSocketHandler {
    @Override
    public Mono<Void> handle(final WebSocketSession session) {
        return session.send(
                session.receive()
                        .map(msg -> session.textMessage("ECHO -> " + msg.getPayloadAsText())));
    }
}
```
在创建了 WebSocket 的处理器 EchoHandler 之后，下一步需要把它注册到 WebFlux 中。我们首先需要创建一个类 WebSocketHandlerAdapter 的对象，该对象负责把 WebSocketHandler 关联到 WebFlux 中。代码清单 7 中给出了相应的 Spring 配置。其中的 HandlerMapping 类型的 bean 把 EchoHandler 映射到路径 /echo。

清单 7. 注册 EchoHandler
```
@Configuration
public class WebSocketConfiguration {
 
    @Autowired
    @Bean
    public HandlerMapping webSocketMapping(final EchoHandler echoHandler) {
        final Map<String, WebSocketHandler> map = new HashMap<>(1);
        map.put("/echo", echoHandler);
 
        final SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        mapping.setUrlMap(map);
        return mapping;
    }
 
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
```
运行应用之后，可以使用工具来测试该 WebSocket 服务。打开工具页面 https://www.websocket.org/echo.html，然后连接到 ws://localhost:8080/echo，可以发送消息并查看服务器端返回的结果。

## 客户端
除了服务器端实现之外，WebFlux 也提供了反应式客户端，可以访问 HTTP、SSE 和 WebSocket 服务器端。

### HTTP
对于 HTTP 和 SSE，可以使用 WebFlux 模块中的类 org.springframework.web.reactive.function.client.WebClient。代码清单 10 中的 RESTClient 用来访问前面小节中创建的 REST API。首先使用 WebClient.create 方法来创建一个新的 WebClient 对象，然后使用方法 post 来创建一个 POST 请求，并使用方法 body 来设置 POST 请求的内容。方法 exchange 的作用是发送请求并得到以 Mono<ServerResponse>表示的 HTTP 响应。最后对得到的响应进行处理并输出结果。ServerResponse 的 bodyToMono 方法把响应内容转换成类 User 的对象，最终得到的结果是 Mono<User>对象。调用 createdUser.block 方法的作用是等待请求完成并得到所产生的类 User 的对象。

清单 10. 使用 WebClient 访问 REST API
```
public class RESTClient {
    public static void main(final String[] args) {
        final User user = new User();
        user.setName("Test");
        user.setEmail("test@example.org");
        final WebClient client = WebClient.create("http://localhost:8080/user");
        final Monol<User> createdUser = client.post()
                .uri("")
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(user), User.class)
                .exchange()
                .flatMap(response -> response.bodyToMono(User.class));
        System.out.println(createdUser.block());
    }
}
```
### SSE
WebClient 还可以用同样的方式来访问 SSE 服务，如代码清单 11 所示。这里我们访问的是在之前的小节中创建的生成随机数的 SSE 服务。使用 WebClient 访问 SSE 在发送请求部分与访问 REST API 是相同的，所不同的地方在于对 HTTP 响应的处理。由于 SSE 服务的响应是一个消息流，我们需要使用 flatMapMany 把 Mono<ServerResponse>转换成一个 Flux<ServerSentEvent>对象，这是通过方法 BodyExtractors.toFlux 来完成的，其中的参数 new ParameterizedTypeReference<ServerSentEvent<String>>() {}表明了响应消息流中的内容是 ServerSentEvent 对象。由于 SSE 服务器会不断地发送消息，这里我们只是通过 buffer 方法来获取前 10 条消息并输出。

清单 11. 使用 WebClient 访问 SSE 服务
```
public class SSEClient {
    public static void main(final String[] args) {
        final WebClient client = WebClient.create();
        client.get()
                .uri("http://localhost:8080/sse/randomNumbers")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .flatMapMany(response -> response.body(BodyExtractors.toFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })))
                .filter(sse -> Objects.nonNull(sse.data()))
                .map(ServerSentEvent::data)
                .buffer(10)
                .doOnNext(System.out::println)
                .blockFirst();
    }
}
```
### WebSocket
访问 WebSocket 不能使用 WebClient，而应该使用专门的 WebSocketClient 客户端。Spring Boot 的 WebFlux 模板中默认使用的是 Reactor Netty 库。Reactor Netty 库提供了 WebSocketClient 的实现。在代码清单 12 中，我们访问的是上面小节中创建的 WebSocket 服务。WebSocketClient 的 execute 方法与 WebSocket 服务器建立连接，并执行给定的 WebSocketHandler 对象。该 WebSocketHandler 对象与代码清单 6 中的作用是一样的，只不过它是工作于客户端，而不是服务器端。在 WebSocketHandler 的实现中，首先通过 WebSocketSession 的 send 方法来发送字符串 Hello 到服务器端，然后通过 receive 方法来等待服务器端的响应并输出。方法 take(1)的作用是表明客户端只获取服务器端发送的第一条消息。

清单 12. 使用 WebSocketClient 访问 WebSocket
```
public class WSClient {
    public static void main(final String[] args) {
        final WebSocketClient client = new ReactorNettyWebSocketClient();
        client.execute(URI.create("ws://localhost:8080/echo"), session ->
                session.send(Flux.just(session.textMessage("Hello")))
                        .thenMany(session.receive().take(1).map(WebSocketMessage::getPayloadAsText))
                        .doOnNext(System.out::println)
                        .then())
                .block(Duration.ofMillis(5000));
    }
}
```

## 测试
在 spring-test 模块中也添加了对 WebFlux 的支持。通过类 org.springframework.test.web.reactive.server.WebTestClient 可以测试 WebFlux 服务器。进行测试时既可以通过 mock 的方式来进行，也可以对实际运行的服务器进行集成测试。代码清单 13 通过一个集成测试来测试 UserController 中的创建用户的功能。方法 WebTestClient.bindToServer 绑定到一个运行的服务器并设置了基础 URL。发送 HTTP 请求的方式与代码清单 10 相同，不同的是 exchange 方法的返回值是 ResponseSpec 对象，其中包含了 expectStatus 和 expectBody 等方法来验证 HTTP 响应的状态码和内容。方法 jsonPath 可以根据 JSON 对象中的路径来进行验证。

清单 13. 测试 UserController
```
public class UserControllerTest {
    private final WebTestClient client = WebTestClient.bindToServer().baseUrl("http://localhost:8080").build();
 
    @Test
    public void testCreateUser() throws Exception {
        final User user = new User();
        user.setName("Test");
        user.setEmail("test@example.org");
        client.post().uri("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(user), User.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("name").isEqualTo("Test");
    }
}
```

## 小结
反应式编程范式为开发高性能 Web 应用带来了新的机会和挑战。Spring 5 中的 WebFlux 模块可以作为开发反应式 Web 应用的基础。由于 Spring 框架的流行，WebFlux 会成为开发 Web 应用的重要趋势之一。本文对 Spring 5 中的 WebFlux 模块进行了详细的介绍，包括如何用 WebFlux 开发 HTTP、SSE 和 WebSocket 服务器端应用，以及作为客户端来访问 HTTP、SSE 和 WebSocket 服务。对于 WebFlux 的基于 Java 注解和函数式编程等两种模型都进行了介绍。最后介绍了如何测试 WebFlux 应用。

### 参考资源 (resources)
- 参考 WebFlux 的[参考指南][1]，了解 WebFlux 的更多内容。
- 查看[《使用 Reactor 进行反应式编程》][2]一文，了解 Reactor 项目的更多内容。
- 了解 [Reactor Netty][3] 的更多内容。


  [1]: http://docs.spring.io/spring/docs/5.0.0.RC2/spring-framework-reference/web.html#web-reactive "参考指南"
  [3]: https://github.com/reactor/reactor-netty
  [2]: https://www.ibm.com/developerworks/cn/java/j-cn-with-reactor-response-encode/index.html