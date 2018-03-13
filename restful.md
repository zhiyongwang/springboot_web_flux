### REST API

 1. 列表项

简单的 Hello World 示例并不足以说明 WebFlux 的用法。在下面的小节中，本文将介绍其他具体的实例。先从 REST API 开始说起。REST API 在 Web 服务器端应用中占据了很大的一部分。我们通过一个具体的实例来说明如何使用 WebFlux 来开发 REST API。

该 REST API 用来对用户数据进行基本的 CRUD 操作。作为领域对象的 User 类中包含了 id、name 和 email 等三个基本的属性。为了对 User 类进行操作，我们需要提供服务类 UserService，如代码清单 2 所示。类 UserService 使用一个 Map 来保存所有用户的信息，并不是一个持久化的实现。这对于示例应用来说已经足够了。类 UserService 中的方法都以 Flux 或 Mono 对象作为返回值，这也是 WebFlux 应用的特征。在方法 getById()中，如果找不到 ID 对应的 User 对象，会返回一个包含了 ResourceNotFoundException 异常通知的 Mono 对象。方法 getById()和 createOrUpdate()都可以接受 String 或 Flux 类型的参数。Flux 类型的参数表示的是有多个对象需要处理。这里使用 doOnNext()来对其中的每个对象进行处理。






