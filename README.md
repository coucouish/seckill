# JAVA高并发秒系统

- 项目优化1.0 页面优化
	- 1.添加缓存
		- QPS的一大瓶颈在于对数据库的操作，所以我们可以通过把对数据库的操作数据提取出来，存到缓存中，进而达到优化的目的。
			- 并不是说任何的数据库操作都可以放到缓存，一般我们只会将那种需要频繁被读取并且变更较少的数据放到缓存中，因为做缓存就需要考虑数据一致性的问题。
		- 当用户再次访问页面时，将不再是单纯的执行页面跳转，而是会返回一个完整的页面
		- 缓存的类型：
			- 页面缓存
			- url缓存
			- 对象缓存
		- 用redis做页面缓存
			- 页面缓存
				- 访问页面后，redis数据库的更新结果：新增userTicket信息，以及相应的页面缓存信息
				- 页面缓存的缺陷
					- 虽然我们将页面数据放在redis中做了缓存，加快了页面数据的读取速度，但我们从后端处理完成发送给前端的仍然是整个页面，数据量还是比较大的
					- 对于商品列表，我们一般都会做分页，做页面缓存时一般也只会缓存前面几页（因为大部分用户只会浏览前几页）
			- url缓存
				- url缓存的本质还是页面缓存
					- 当进入一个商品列表后，再通过列表进入不同商品的详情页面的url的goodsId是不一样的，进入的详情页面展示也是不一样的
					- url缓存就是把goodsId不一样的页面（url中的id不一样）同样也做缓存
				- 访问页面的不同详情页后，redis数据库的更新结果：新增userTicket信息，以及相应的页面缓存信息
			- 对象缓存
				- 对象缓存时更加细粒度的缓存，它是针对对象做缓存。
					- 比如在秒杀项目中，对于登录之后的一系列功能，比如展示商品列表页、商品详情页、秒杀等，都会有一个入参User，该User是直接通过Ticket获取的
						- 而为了解决分布式情况下用户不一致问题，我们通过redisTemplate直接将userTicket信息存到redis中了
						- 故在解决用户不一致问题时，也同时对用户对象User进行了缓存（即：对象缓存）。做了该缓存后，后续所有的功能都是从redis中获取的用户信息。
				- 对象缓存需要注意数据一致性问题
					- 对于User对象的缓存，大部分时候时不需要更新的，所以我们的redis缓存中并未对其设置失效时间。但当用户对自身信息做了变更时（如更新密码等，这时数据库的信息会更新），我们就需要及时地更新redis缓存中的相应数据。
					- 更新redis中的对象缓存方法
						- 每次对数据库进行操作时，就把redis内的相关数据清空。然后当用户再次登录时，redis就会获取到最新的用户信息（因为当redis没有缓存数据时，是会直接从数据库中获取数据，然后将最新的数据同步更新到redis中）
	- 2.页面的静态化，前后端分离
		- 为什么要做页面静态化？
			- 当前秒杀项目只使用了thymeleaf模板，每次浏览器请求时，都要从服务器端获取数据，拼接成模板，渲染，然后返回给浏览器。即使加了缓存，但中间传输时，传输的还是一整个模板引擎。
			- 做了页面静态化后，前端就是html，我们只需要通过服务器端发给前端一些动态的数据，其它基本不会变更的前端元素都是静态的。静态页面获取数据时，不再是通过模板引擎来传输数据、进行页面的跳转，而是在前端进行页面跳转，并通过ajax去请求接口、接收数据并处理数据
		- 优化具体内容
			- 商品详情页面的静态化
				- 将商品详情页面跳转功能换成了创建一个公共返回对象（DetailVo对象），以前通过model传参，改成了通过DetailVo传参。
				- 把thymeleaf模板换成了静态页面，通过静态页面跳转，并通过ajax调用接口并获取数据
			- 秒杀页面的静态化
				- 大致同上
		- 解决商品超卖问题
			- 具体操作
				- 在OrderServiceImpl类中实现扣库存操作时，将原来的直接update处理换成了用sql语句处理，并保留了“操作时库存必须大于零才能操作”的判断条件
				- 在秒杀订单数据库（“t_seckill_order”）中，添加索引，通过将用户id和商品id捆绑成一条独有的索引，来确保同一个用户多次秒杀同一个商品的现象不发生
				- 在秒杀订单SeckillController类中，之前是通过QueryWrapper从数据库中取秒杀订单信息的，以此来判断该用户是否有重复抢购行为，后面将其改为了从redis中读取之前的秒杀订单缓存信息，直接做判断
	- 3.静态资源的优化
		- 把一些资源（如CSS, JS, 图片等）提前放到另外的地方，并做一些提前的处理
	- 4.[CDN优化](https://juejin.cn/post/7052282275211247646)
		- 什么是CDN？
			- CDN（Content Delivery Network）即内容分发网络，其基本思路是尽可能的避开互联网上有可能影响数据传输速度和稳定性的瓶颈和环节，使内容传输更快、更稳定。
			- CDN是一组分布在多个不同地理位置的Web服务器，用于更加有效地向用户发布内容。
			- CDN系统能够实时的根据网络流量和各节点的连接，负载状况以及用户的距离和响应时间等综合信息将用户的请求重新导向离用户最近的服务节点上。其目的就是使用户能够就近的获取请求数据，解决网络访问拥挤状况，提高用户访问系统的响应时间。
			- CDN将网站的资源发布到离用户最近的网络边缘，用户可以就近取得资源内容。
		- 什么是CDN优化？
			- CDN的本质仍然是一个缓存，而且将数据缓存在里用户最近的地方，使用户以最快的速度获取数据，即网络访问第一跳。
			- 当用户发起内容请求时，不同地区的用户访问同一个域名能得到不同CDN节点的IP地址，这要依赖于CDN服务商提供的智能DNS服务，浏览器发起域名查询时，智能DNS服务会根据用户IP计算并返回离它最近的相同网络运营商的CDN节点IP；
			- 前端需要被加速的文件大致包括js、css、图片、视频等这些文件都是静态的，改动较小，这类静态文件最适合做cdn加速。我们把这些静态文件通过cdn分发到全国乃至世界的各个节点，用户就可以在距离最近的边缘节点拿到所需要的内容，从而提升内容下载速度加快网页打开速度达到性能优化的目的。




- 项目优化2.0 服务优化
	- 减少数据库的访问
		- 用Redis预减库存，减少数据库访问
			- 页面优化的一些操作最终还是需要频繁地同数据库交互，而数据库的并发瓶颈较低，远不如缓存，所以在秒杀系统中添加了Redis缓存。但即使添加了缓存，任然有很多接口还是需要访问数据库，比如需要从数据库中获取库存并扣减库存等，故可以通过用Redis预减库存，减少数据库的访问
			- 具体步骤
				- 1.封装消息对象
				- 2.通过RabbitMQ发送该消息对象，并借助一个EmptyStockMap，记录所有已经为空的商品，如果之后还有该商品的订单请求，则直接返回没有库存，而不是仍然一遍遍访问redis，从而达到通过内存标记，减少Redis的访问的目的
				- 3.在RabbitMQ消费者类中接收该消息对象，并在各种判断之后，做下单操作（`MQReceiver.java`）（由于用了RabbitMQ，下单操作变成了一个异步的操作，能够达到流量削峰的目的）
		- 通过Redis标记，减少Redis的访问
			- 用Redis预减库存，就会使Redis被频繁访问。Redis一般会放在单独的服务器上，所以还是需要大量地同Redis服务器进行网络通信。故可以进一步优化，通过内存标记，减少Redis的访问。
				- 如：借助一个EmptyStockMap，记录所有已经为空的商品，如果之后还有该商品的订单请求，则直接返回没有库存，而不是仍然一遍遍访问redis，从而达到通过内存标记，减少Redis的访问的目的
		- 使用RabbitMQ消息队列优化下单操作
			- 下单时，如果都直接查询数据库的话，数据库仍然难以抗住如此高并发，故可以使用队列，让请求先进入队列中进行缓冲，然后通过队列进行异步下单，进而提升用户体验
			- 使用RabbitMQ消息队列
				- [RabbitMQ](https://www.rabbitmq.com/#getstarted)
					- 安装RabbitMQ
						- [参考安装指导1](https://blog.tericcabrel.com/install-rabbitmq-ubuntu-server/)
						- [参考安装指导2](https://blog.csdn.net/qq_38272530/article/details/126031056)
						- ubuntu中其它关于RabbitMQ的设置
							-
							  ```
							  sudo rabbitmqctl list_users # 查看所有用户
							  sudo rabbitmqctl change_password 用户名 '新密码' # 修改用户名密码
							  ```
							- [其它参考设置](https://blog.csdn.net/qq_32014795/article/details/115702755)
					- 配置用户可以远程访问 RabbitMQ WebUI
					- RabbitMQ Web UI 界面介绍
						- Virtual Host 虚拟主机，用来分割数据。
							- 当前对于不同的项目，又用的是同一个RabbitMQ服务器，但对于不同的项目，所处理的数据、队列及交换机等都应该是分开的，此时就需要使用虚拟主机进行分割。不同的项目使用把不同的虚拟主机，以起到数据隔离的作用。
				- SpringBoot 集成RabbitMQ
					- 在SpringBoot项目中添加RabbitMQ依赖
						- AMQP，即Advanced Message Queuing Protocol（高级消息队列协议），一个提供统一消息服务的应用层标准高级消息队列协议，是应用层协议的一个开放标准，为面向消息的中间件设计，基于此协议的客户端与消息中间件传递消息，不受客户端/中间件不同产品、不同开发语言等条件的限制。RabbitMQ实现了AMQP标准。
					- 配置RabbitMQ
					- RabbitMQ消息传递模型
						- 生产者和消费者模型
						
				- 交换机
					- RabbitMQ消息传递模型的核心思想是生产者从不直接向队列发送任何消息。实际上，很多时候生产者甚至根本不知道消息是否将被传递到任何队列。生产者的消息其实是发送到交换机的。
					- 交换机
						- 交换机的功能：一边接受来自生产者的消息，一边将消息推送到队列。交换机必须确切地知道如何处理它接收到的消息，如消息是否需要被丢弃，或是需要被附加到一个特定的队列以及是否应该被附加到多个队列。
						- 交换机类型
							- fanout：扇形交换机（广播模式）
								- 广播模式指交换机所发送的消息不仅仅能够被一个队列所接收，而是可以被多个队列所接收。多个队列所接收的都是同一个生产者所发出的同一个消息。
								- 规则
									- 扇形交换机并不会处理路由键，只会简单地将队列绑定到交换机上。发送到扇形交换机的消息就会被转发到和此台交换机绑定的所有地方。由于不需要处理路由键，所以扇形交换机转发消息是最快速的。
									- 路由键：交换机是通过匹配不同的路由键来决定消息是需要被丢弃，还是发给指定的队列，亦或是发给多个队列。
										- [参考文章](https://blog.csdn.net/shenhaiyushitiaoyu/article/details/84308771#:~:text=RabbitMQ%E7%9A%84%E5%AD%A6%E4%B9%A0%20%28%E4%B8%89%29%EF%BC%9ARabbitMQ%E4%B8%8D%E5%90%8C%E7%9A%84%E4%BA%A4%E6%8D%A2%E6%9C%BA%E9%85%8D%E5%90%88%E8%B7%AF%E7%94%B1%E9%94%AE%E5%8F%91%E9%80%81%E6%B6%88%E6%81%AF%20%E6%9C%89%E7%8C%BF%E5%86%8D%E8%A7%81%20%E4%BA%8E%202018-11-20%2019%3A56%3A56%20%E5%8F%91%E5%B8%83%2052486,10%20%E5%88%86%E7%B1%BB%E4%B8%93%E6%A0%8F%EF%BC%9A%20%E3%80%90%E4%B8%AD%E9%97%B4%E4%BB%B6%E3%80%91%20%E6%96%87%E7%AB%A0%E6%A0%87%E7%AD%BE%EF%BC%9A%20RabbitMQ%20RabbitMQ%E4%BA%A4%E6%8D%A2%E6%9C%BA%20RabbitMQ%E5%88%86%E5%8F%91%20%E4%B8%AD%E9%97%B4%E4%BB%B6)
								- 使用fanout交换机代码示例
									- 创建一个扇形交换机，并创建几个队列用来接收消息，然后将扇形交换机和这几个队列绑定。
									- 在`MQSerder.java`中将消息接收者设置为该扇形交换机的名字，routineKey设置为""
									- 在`MQReceiver.java` 中，增添两个方法，设置新增的两个队列监听消息
							- direct:：直连交换机（单播模式）
								- 直列交换机的工作模式
									- 通过message中携带的路由键，将消息发送到与相同路由键绑定的队列上。所有发送到直连交换机的消息都会转发到消息所携带的路由键所指定的队列上。
								- AMQP默认模式下就是使用直连交换机，此时无需明确指定路由键。
								- 使用direct交换机代码示例
									- 创建一个直连交换机，并创建几个队列用来接收消息，然后用不同的路由键将队列和交换机绑定。
									- 在`MQSerder.java`中将消息接收者设置为该直连扇形交换机的名字，routineKey设置为与目标队列所绑定的路由键相同的名称
									- 在`MQReceiver.java` 中，增添两个方法，设置新增的两个队列监听消息
							- topic：主题交换机
								- direct模式的弊端
									- 当项目逐渐壮大，所需要匹配的路由键越来越多时，路由键就会比较难以管理。而topic模式正好可以解决这一痛点。
								- topic模式
									- topic模式也使用了路由键，不过为了方便管理路由键，该模式下支持路由键中使用通配符
									- 规则
										- 所有被发送到主题交换机的消息都会被转发到所有与路由键匹配的队列中
											- 交换机会对路由键和队列所绑定的topic进行模糊匹配
										- 路由键定义规则
											- 路由键是由句号 '.' 分割的字符串，被该句号分割的每一个子串都称为一个单词，然后将这些单词通过 ‘*‘ 和 ‘#’ 同topic进行匹配
									- 使用topic交换机代码示例
										- 创建一个topic交换机，并创建几个队列用来接收消息，然后用不同的路由键将队列和交换机绑定。（`RabbitMQTopicConfig.java`）
										- 在`MQSerder.java`中将消息接收者设置为该直连扇形交换机的名字，routineKey设置为与目标队列所绑定的路由键能够模糊匹配的名称
							- headers：首部交换机
								- 使用比较麻烦，效率较低，较少使用
								- 代码示例
									- 绑定模式
									- 发送消息格式，用message properties的头部信息进行匹配
									- 接收消息格式
									- controller的设置
		- 用Redis实现分布式锁
			- 什么是锁？
				- 通俗地讲，锁就是占位的过程。来了一个线程，先去占位，当别的线程来操作时，发现已经被占位了，就会放弃或是排队等待，等到正在占位的线程执行完成后，会删除正在占位的这个锁，这样别的线程开始运行了。
				- 死锁和超时时间
					- 需要考虑的是，如果线程在占线过程中抛了异常，非正常结束，那么这个锁很大概率就没有被删除，一直在占位，此时别的线程也没办法开始运行，这就形成了死锁。
					- 为了避免死锁的发生，我们在创建锁的时候就需要设置一个超时时间，到了一定的时间，这个锁会被自动销毁。
				- 避免删错锁
					- 由于网络的波动，一个线程所需要的操作时间可能远远超过或远远小于所设置的超时时间，为了避免上一个线程设置的删除锁操作把后面的线程设置的锁给误删了，我们需要对每个所都设置特殊的标记（比如`valueOperations.set("name","coucou");`中的value是用随机值），并在每次删锁之前需要匹配key和value是不是都对的上，再进行删锁
			- 设置锁的过程包括四个步骤：加锁，获取锁，比较锁，删除锁。故设置锁的过程不是原子性的，为了进行原子性操作，可以借助lua脚本。
				- 程序的原子性
					- **程序**的**原子****性**指：整个程序中的所有操作，要么全部完成，要么全部不完成，不可能停滞在中间某个环节。
					- 如果程序由多条命令组成，就有可能在运行到中间某一条代码时崩溃，导致后面的代码没办法执行，故不具有原子性。用不具有原子性的代码设置锁就容易发生死锁的现象。
				- lua脚本的优势
					- 使用方便，redis内置了对lua脚本的支持。lua脚本可以在redis服务端原子性地执行多个redis命令。redis的性能一般受网络的影响较大，所以我们可以使用lua脚本让多个命令一起执行，这样可以有效解决网络带给redis的性能波动。
					- 为什么要使用Lua语言来实现锁呢？因为要确保多个操作是原子性的。简单来说，就是在执行Lua代码的时候，Lua代码将被当成一个整体命令去执行，并且直到命令执行完成，Redis才会执行其他命令。
				- 使用lua脚本的方法
					- 方法一：在redis端写好redis脚本，然后在java客户端调用；
					- 方法二：在java客户端写lua脚本，然后通过java客户端将脚本发到redis客户端执行
						- `lock.lua`
						- 在`RedisConfig.java`文件中添加对lua的配置
						- 添加锁
			- 运用lua的原子性优化预减库存
				- 添加`stock.lua`脚本
				- 在`RedisConfig.java`中配置lua脚本支持方法
				- 在`SeckillController.java`中进行域减库存
			- [基于Redis+Lua实现分布式锁模拟秒杀扣减库存业务](https://blog.csdn.net/guobinhui/article/details/108714081)
	- 增强数据库
		- 可以对数据库做集群，或者用阿里巴巴研发的中间件MyCat对数据库进行分库分表，以此来增强数据库的新能









