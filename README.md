



# 听书项目笔记

mybatis插件

~~~java
@Configuration
@MapperScan("com.atguigu.mp.mapper")
public class MybatisPlusConfig {

    /**
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));//如果配置多个插件,切记分页最后添加
        //interceptor.addInnerInterceptor(new PaginationInnerInterceptor()); 如果有多数据源可以不配具体类型 否则都建议配上具体的DbType
        return interceptor;
    }
}
~~~





## 一、专辑管理

![album数据表关系图](D:\资料\笔记\听书\album数据表关系图.png)

### 1、添加专辑

#### 1.1、视图

视图  :是一个虚拟表，本身并不存放数据，数据来源于原始表(单表，关联查询多条)

语法：create [or replace] view 视图名称 as SQL语句

作用1：屏蔽掉敏感字段，不希望某些开发者查看的列（创建只读视图，视图数据只有非敏感列，给指定开发者设置权限-只能查看视图）

作用2：将一些复杂关联查询结果封装到视图中，方便后续操作，视图当成表来使用

create or replace 表示在创建视图时,如果已存在同名的视图,则重新创建, 如果只用create 创建,则需将原有的视图删除后才能创建

#### 1.2、添加专辑

##### 1.2.1、查看分类数据

三级分类联表查询，查询的结果作为视图内容创建视图

由于视图中的一级分类和二级分类有重复数据，需要去重，此时使用sql的distinct和grouping by无法去重，只能使用stream流进行去重。

```java
list.stream().collect(Collectors.groupingBy(分组字段))
分组后的结果是一个map，key为分组字段，value为同一组数据的集合
```

~~~java
public List<JSONObject> getBaseCategoryList() {
		//1.创建目标集合对象
		List<JSONObject> allList = new ArrayList<JSONObject>();
		//2.查询分类视图得到所有分类数据 共计401条记录
		List<BaseCategoryView> allCategoryList = baseCategoryViewMapper.selectList(null);
		//3.处理一级分类，对所有分类数据根据-根据一级分类ID分组
		//3.1 采用Stream流进行集合分组，得到Map集合，Map中key:1级分类ID Map中Value：1级分类集合
		Map<Long, List<BaseCategoryView>> category1Map = allCategoryList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
		if (!CollectionUtils.isEmpty(category1Map)){
			for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
				//每遍历一次处理一个一级分类
				//3.2 获取一级分类ID 以及 名称
				Long category1Id = entry1.getKey();
				String category1Name = entry1.getValue().get(0).getCategory1Name();
				//3.3 构建一级分类JSON对象
				JSONObject jsonObject1 = new JSONObject();
				jsonObject1.put("categoryId", category1Id);
				jsonObject1.put("categoryName", category1Name);
				//4.在某个一级分类内，处理二级分类，对一级分类集合根据二级分类ID分组
				Map<Long, List<BaseCategoryView>> category2Map = entry1.getValue().stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));

				if (!CollectionUtils.isEmpty(category2Map)){
					//用于存放一级分类CategoryChild的集合
					List<JSONObject> jsonObject2List = new ArrayList<JSONObject>();
					//4.2 对某个一级分类集合按照二级分类ID进行分组 得到二级分类Map Map-key 二级分类ID Map-Value 二级分类集合
					for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
						//4.3 获取二级分类ID 以及 名称
						Long category2Id = entry2.getKey();
						String category2Name = entry2.getValue().get(0).getCategory2Name();
						//4.4 遍历构建二级分类JSON对象 将二级分类对象加入到集合中
						JSONObject jsonObject2 = new JSONObject();
						jsonObject2.put("categoryId", category2Id);
						jsonObject2.put("categoryName", category2Name);
						//5.TODO 在某个二级分类内，处理三级分类
						//5.1 遍历二级分类Map中Value
						//用于存放二级分类CategoryChild的集合
						List<JSONObject> jsonObject3List = new ArrayList<JSONObject>();
						for (BaseCategoryView baseCategoryView : entry2.getValue()) {
							//5.2 获取三级分类ID跟名称
							Long category3Id = baseCategoryView.getCategory3Id();
							String category3Name = baseCategoryView.getCategory3Name();
							//5.3 构建三级分类JSON对象
							JSONObject jsonObject3 = new JSONObject();
							jsonObject3.put("categoryId", category3Id);
							jsonObject3.put("categoryName", category3Name);
							jsonObject3List.add(jsonObject3);

						}
						//5.4 将构建三级分类集合放入二级分类对象中"categoryChild"属性中
						jsonObject2.put("categoryChild",jsonObject3List);
						jsonObject2List.add(jsonObject2);
					}
					//4.5 将二级分类集合放入一级分类对象中"categoryChild"中
					jsonObject1.put("categoryChild",jsonObject2List);
				}
				//3.4 将一级分类对象加入到所有目标集合对象中
				allList.add(jsonObject1);

			}


		}
		return allList;
	}
~~~

##### 1.2.2、专辑标签列表

~~~sql
# 根据1级分类ID查询该分类下关联标签及标签值 标签-标签值：一对多
# 查询表：base_attribute，base_attribute_value
# 关联条件：base_attribute_value值表中逻辑外键：标签ID 跟 base_attribute标签表主键
# 关联方式：内连接（满足关联条件记录才会显示），外连接（左外连接，即使不满足关联条件，左表所有记录都会显示，右表   列使用null） 都可以
# 确定查询条件：根据1级分类ID查询
# 确认分组，确定排序等

~~~

~~~xml
<mapper namespace="com.atguigu.tingshu.album.mapper.BaseAttributeMapper">

    <!--自定义结果集：封装一对多，将某个标签下包含标签值封装到一方集合属性中-->
    <resultMap id="baseAttributeMap" type="com.atguigu.tingshu.model.album.BaseAttribute" autoMapping="true">
        <!--封装主键及普通字段-->
        <id column="id" property="id"></id>
        <!--collection标签 封装集合属性attributeValueList ofType：多方对应类型-->
        <collection property="attributeValueList" ofType="com.atguigu.tingshu.model.album.BaseAttributeValue" autoMapping="true">
            <!--封装多方注解及普通字段-->
            <id column="base_attribute_value_id" property="id"></id>
        </collection>
    </resultMap>

    <!--根据一级分类Id获取分类（标签名包含标签值） 列表-->
    <select id="getAttributesByCategory1Id" resultMap="baseAttributeMap">
        select
            ba.id,
            ba.category1_id,
            ba.attribute_name,
            bav.id base_attribute_value_id,
            bav.attribute_id,
            bav.value_name
        from base_attribute ba left join base_attribute_value bav on bav.attribute_id = ba.id
        where ba.category1_id = #{category1Id}
          and ba.is_deleted = 0
    </select>
</mapper>
~~~

##### 1.2.3、专辑文件上传

Tomcat默认限制上传文件大小：1MB

通过修改配置更改：

~~~yml
spring:
  servlet:
    multipart:
      max-file-size: 10MB     #单个文件最大限制
      max-request-size: 20MB  #多个文件最大限制
~~~

将文件上传到MInIO，且返回上传后文件在线地址，方便用户进行预览

使用属性类读取配置文件的内容，通过配置类注入MinioClient对象

~~~java
@Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpointUrl)
                .credentials(accessKey, secreKey)
                .build();
    }
~~~

ImageIO.read()读取文件，如果图片非法则返回空

~~~java
hutool使用
IdUtil    id工具
FileUtil  文件工具
DateUtil  日期工具
~~~

~~~java
@Override
    public String imageUpload(MultipartFile file) {
        try{
            //1.业务校验验证文件是否为图片，借助ImageIO读取图片。继续判断图片文件后缀名是否符合要求，图片长宽
            BufferedImage read = ImageIO.read(file.getInputStream());
            if (read == null){
                throw new GuiguException(400,"图片非法！");
            }
            //2.调用MInIO-Java客户端对象将文件上传到MInIO-文件上传
            //2.1 动态生成上传文件名称 /当日日期/UUID.后缀
            String folderName = "/" + DateUtil.today() +"/";
            String fileName = IdUtil.randomUUID();
            String extName = FileUtil.extName(file.getOriginalFilename());
            String objectName = folderName + fileName + "." + extName;
            //2.2 调用文件上传接口
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConstantProperties.getBucketName())
                                  .object(objectName)
                    .stream(file.getInputStream(),file.getSize(),-1)
                    .contentType(file.getContentType())
                    .build());
            //3.拼接上传文件成功后在线地址 http://ip:9000/存储空间名称/路径/文件标识名称
            return minioConstantProperties.getEndpointUrl() + "/" + minioConstantProperties.getBucketName() + objectName;
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }
~~~

##### 1.2.4、保存专辑

使用mq向数据库中插入数据，会自动为对象设置id属性值

#### 1.3、分页查询专辑列表

当使用group by进行分组时，查询的字段只能是分组字段，聚合函数，一组相同的字段

当使用group by进行分组时，同一组中的每一条记录均需过一次聚合函数

联表查询，行转列可以使用group by+聚合函数

concat函数中可以使用#{}取出service层传递过来的对象中的属性，进行字符串拼接

涉及到的表，连接方式，连接条件，查询条件

~~~sql
#需求：分页条件查询专辑列表，包含每个专辑四项统计信息（播放、订阅、购买、评论）
#分页：只要在持久层传递Page对象,MP会自动完成分页（自动拼接limit SQL）
#确定数据涉及到表：专辑信息表album_info、专辑统计信息表album_stat
#确定表关联条件：统计表中逻辑外键album_id跟专辑表中主键ID关联
#确定关联方式：内->专辑包含统计信息记录会被展示
#查询条件：用户ID、审核状态、关键字模糊查询
~~~

~~~xml
<mapper namespace="com.atguigu.tingshu.album.mapper.AlbumInfoMapper">


    <!--查询当前用户专辑分页列表-->
    <select id="getUserAlbumPage" resultType="com.atguigu.tingshu.vo.album.AlbumListVo">
        select
            ai.id albumId,
            ai.album_title,
            ai.cover_url,
            ai.include_track_count,
            ai.create_time,
            max(if(stat.stat_type = '0401', stat_num, 0)) playStatNum,
            max(if(stat.stat_type = '0402', stat_num, 0)) subscribeStatNum,
            max(if(stat.stat_type = '0403', stat_num, 0)) buyStatNum,
            max(if(stat.stat_type = '0404', stat_num, 0)) commentStatNum
        from album_info ai
                 inner join album_stat stat
                            on stat.album_id = ai.id
        <where>
            <if test="vo.userId != null">
                ai.user_id = #{vo.userId}
            </if>
            <if test="vo.status != null and vo.status != ''">
                and ai.status = #{vo.status}
            </if>
            <if test="vo.albumTitle != null and vo.albumTitle != ''">
                and ai.album_title like concat('%', #{vo.albumTitle} ,'%')
            </if>
        </where>
        and ai.is_deleted = 0
        group by ai.id
        order by ai.id desc
    </select>
</mapper>
~~~

#### 1.4、删除专辑

只有专辑下没有声音才能删除专辑

先删主表，再删从表

一条sql语句就是一个事务，当要执行多条sql语句时，加@Transactional(rollbackFor = Exception.class)

#### 1.5、修改专辑

先将旧的专辑关联标签记录全部删除，再添加新的

### 2、声音管理

#### 2.1、新增声音

##### 2.1.1、获取用户专辑列表

~~~java
mybatisplus扩展
LambdaQueryWrapper是通过实体类来设置条件
QueryWrapper是通过表的字段来设置条件
select()指定查询的字段
last()在自动生成的sql后面添加一段sql语句
    
IService 接口的 saveBatch()底层还是逐条插入
~~~

insertBatchSomeColumn方法实现批量插入

1. 自定义SQL注入器实现DefaultSqlInjector

   ~~~java
   public class MySqlInjector extends DefaultSqlInjector {
       @Override
       public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
           List<AbstractMethod> methodList = super.getMethodList(mapperClass, tableInfo);
           methodList.add(new InsertBatchSomeColumn(i -> i.getFieldFill() != FieldFill.UPDATE));
           return methodList;
       }
   }
   ~~~

2. 将MySqlInjector注入到Bean中

   ~~~java
   @Configuration
   public class MyBatisConfig {
       @Bean
       public MySqlInjector sqlInjector() {
           return new MySqlInjector();
       }
   }
   ~~~

3. 继承BaseMapper，添加插入方法，要实现批量插入的类在继承此类

   ~~~java
   public interface MyBaseMapper<T> extends BaseMapper<T> {
       int insertBatchSomeColumn(Collection<T> entityList);
   }
   ~~~


##### 2.1.2、 保存声音

~~~java
public void saveTrackInfo(TrackInfoVo trackInfoVo) {
		//1.封装声音对象
		//1.1.设置用户id
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
		trackInfo.setUserId(AuthContextHolder.getUserId());
		//1.2.设置声音在专辑中的排序值
		LambdaQueryWrapper<TrackInfo> queryWrapper = Wrappers.lambdaQuery(TrackInfo.class).eq(TrackInfo::getAlbumId, trackInfoVo.getAlbumId()).orderByDesc(TrackInfo::getOrderNum).last("limit 1");
		TrackInfo preTrackInfo = trackInfoMapper.selectOne(queryWrapper);
		trackInfo.setOrderNum(ObjectUtil.isEmpty(preTrackInfo) ? 0 : preTrackInfo.getOrderNum() + 1);
		//1.3.mediaDuration,mediaFileId,mediaSize,mediaType
		TrackMediaInfoVo trackMediaInfoVo = vodService.getTrackMediaInfo(trackInfoVo.getMediaFileId());
		if (ObjectUtil.isNotEmpty(trackMediaInfoVo)){
			trackInfo.setMediaDuration(new BigDecimal(trackMediaInfoVo.getDuration()));
			trackInfo.setMediaSize(trackMediaInfoVo.getSize());
			trackInfo.setMediaType(trackMediaInfoVo.getType());
		}
		//1.4.设置source,status
		trackInfo.setSource("1");
		trackInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
		//2.保存声音
		trackInfoMapper.insert(trackInfo);
		Long trackInfoId = trackInfo.getId();
		//3.保存声音统计信息
		saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_PLAY, 0);
		saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_COLLECT, 0);
		saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_PRAISE, 0);
		saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_COMMENT, 0);
		//4.更新专辑信息
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
		albumInfo.setStatus(albumInfo.getEstimatedTrackCount() == albumInfo.getIncludeTrackCount() ? "1" : "0");
		albumInfoMapper.updateById(albumInfo);

	}
~~~

#### 2.2、 分页查询声音列表

~~~sql
<select id="findUserTrackPage" resultType="com.atguigu.tingshu.vo.album.TrackListVo">
        select
        ti.id trackId,
        ti.track_title,
        ti.album_id,
        ti.cover_url,
        ti.media_duration,
        ti.status,
        max(if(stat_type='0701', stat_num, 0)) playStatNum,
        max(if(stat_type='0702', stat_num, 0)) collectStatNum,
        max(if(stat_type='0703', stat_num, 0)) praiseStatNum,
        max(if(stat_type='0704', stat_num, 0)) commentStatNum
        from track_info ti inner join track_stat ts on ti.id = ts.track_id
        <where>
            <if test = "query.userId != null">
                ts.user_id = #{query.userId}
            </if>
            <if test="query.status != '' and query.status != null">
                and ti.status = #{query.status}
            </if>
            <if test="query.title and query.title != null">
                and ti.title like concat('%',#{query.title},'%')
            </if>
            and ti.is_deleted = 0
        </where>
        group by ti.id
        order by ti.id DESC
    </select>
~~~

#### 2.3、 修改声音

修改声音时应判断音频文件是否变更，如果变更则需删除云点播服务上旧的音频文件，再获取新的音频信息并设置到新的TrackInfo对象中

#### 2.4、 更新声音

~~~java
public void removeTrackInfo(Long id) {
		//1.根据id查询声音信息
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		//2.删除云点播服务的音频文件
		vodService.deleteTrackMedia(trackInfo.getMediaFileId());
		//3.删除声音统计信息
		trackStatMapper.delete(Wrappers.lambdaQuery(TrackStat.class).eq(TrackStat::getTrackId,id));
		//4.更新序号大于当前声音的声音序号
		trackInfoMapper.updateTrackNum(trackInfo.getAlbumId(), trackInfo.getOrderNum());
		//5.删除声音信息
		trackInfoMapper.deleteById(id);
		//6.更新专辑信息
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
		LambdaUpdateWrapper<AlbumInfo> updateWrapper = Wrappers.lambdaUpdate(AlbumInfo.class).eq(AlbumInfo::getId, trackInfo.getAlbumId())
				.set(AlbumInfo::getIncludeTrackCount, albumInfo.getIncludeTrackCount() - 1)
				.set(AlbumInfo::getStatus, albumInfo.getIncludeTrackCount() == albumInfo.getEstimatedTrackCount() ? "1" : "0");
		albumInfoMapper.update(null,updateWrapper);

	}
~~~

## 二、 登录校验

### 1、 自定义注解

元注解：

@Target：注解使用位置，可选：类/接口/注解、方法、属性、方法参数、构造等

@Retention：注解生命周期（注解会保留到什么阶段）可选：SOURCE、CLASS、RUNNTIME 选择CLASS该注解在运行时没了

@Inherited：该注解是否可以被继承

@Documented：通过JDK提供javadoc命令产生文档，是否会生成该注解文档

~~~java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GuiGuLogin {

    /**
     * 是否必须登录，默认为：必须登录
     * @return
     */
    boolean required() default true;


}
~~~

### 2、 请求上下文工具类

#### 2.1、 基本使用

RequestContextHolder是Spring提供的一个用于管理请求上下文的工具类。它允许在应用程序的任何地方访问当前请求和响应信息，而不需要将这些对象显式地传递给每个需要它们的方法或对象

~~~java
//  获取请求对象
RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//  转化为ServletRequestAttributes
ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
//  获取到HttpServletRequest 对象
HttpServletRequest request = sra.getRequest();
//	获取到HttpServletResponse 对象
HttpServletResponse response = sra.getResponse();
~~~

#### 2.2、 源码分析

~~~java
public abstract class RequestContextHolder {
    // 得到存储进去的request
    private static final ThreadLocal<RequestAttributes> requestAttributesHolder = new NamedThreadLocal("Request attributes");
    //可被子线程继承的reques
    private static final ThreadLocal<RequestAttributes> inheritableRequestAttributesHolder = new NamedInheritableThreadLocal("Request context");	
~~~

NamedThreadLocal是ThreadLocal的子类，它为每个ThreadLocal实例添加了一个名称属性，使得可以通过名称来获取和设置变量值。NamedThreadLocal适用于需要在多个线程之间共享变量并通过名称进行访问的场景。相比之下，标准的ThreadLocal变量副本只能在当前线程内部访问，不能在不同线程间通过名称共享。

NamedInheritableThreadLocal是一种特殊的ThreadLocal，它允许子线程在初始化时继承父线程的变量值。这意味着子线程可以访问到父线程设置的初始值，但是子线程对InheritableThreadLocal变量的修改对父线程不可见，反之亦然。这种特性使得NamedInheritableThreadLocal适用于需要在线程及其子线程之间传递某些上下文信息的场景。

将当前请求和响应信息存入线程局部变量的时机：

FrameWorkServlet ---> processRequest

~~~java
        RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes requestAttributes = this.buildRequestAttributes(request, response, previousAttributes);
        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
        asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new FrameworkServlet.RequestBindingInterceptor());
        this.initContextHolders(request, localeContext, requestAttributes);//初始化请求上下文时将请求对象，响应对象设置到ThreadLocal中
~~~

### 3、 切面类

~~~java
 @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)")
    public Object loginAspect(ProceedingJoinPoint joinPoint, GuiGuLogin guiGuLogin) {
        Object resultObject = new Object();
        log.info("前置通知...");

        //1.获取小程序端（客户端）提交令牌Token
        //1.1 通过请求上下文对象RequestContextHolder获取请求属性对象 - 普通类中获取到请求对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //1.2 将RequestAttributes（接口）转为ServletRequestAttributes（实现类）
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        //1.3 获取请求对象
        HttpServletRequest request = servletRequestAttributes.getRequest();
        //HttpServletResponse response = servletRequestAttributes.getResponse();

        //1.4 基于请求对象获取请求头：token
        String token = request.getHeader("token");

        //2.查询存放在Redis中用户信息（何时存入：登录成功后）
        //2.1 构建用户登录信息key
        String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        //2.2 查询存入Redis中用户信息
        UserInfoVo UserInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(loginKey);

        //3.判断注解是否要求必须登录，如果要求登录且用户信息为空，抛出异常业务状态：208引导用户登录
        if (guiGuLogin.required() && UserInfoVo == null) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        //4.如果用户信息存在，将用户ID存入ThreadLocal
        if (UserInfoVo != null) {
            AuthContextHolder.setUserId(UserInfoVo.getId());
        }

        //二、执行目标方法
        resultObject = joinPoint.proceed();


        //5.将ThreaLocal中数据手动清理避免OOM出现
        AuthContextHolder.removeUserId();
        log.info("后置通知...");
        return resultObject;
    }
~~~

### 4、 用户登录

#### 4.1、 登录流程

![image-20260219184726674](C:\Users\陈浩铭\AppData\Roaming\Typora\typora-user-images\image-20260219184726674.png)

#### 4.2、 登录

~~~java
public Map<String, String> wxLogin(String code) {
        try {
            //1.根据入参提交临时票据，调用微信获取微信账户唯一标识接口，得到微信账户openId
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            if (sessionInfo != null) {
                //2.根据微信唯一标识查询用户记录
                String wxOpenId = sessionInfo.getOpenid();

                //2.1 如果查询为空-将微信OpenId跟听书项目中用户关联（新增用户记录中存储微信账户唯一标识）
                LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(UserInfo::getWxOpenId, wxOpenId);
                UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
                if (userInfo == null) {
                    //2.2 为首次登录用户构建用户对象，保存用户记录
                    userInfo = new UserInfo();
                    userInfo.setWxOpenId(wxOpenId);
                    userInfo.setNickname("听友" + IdUtil.getSnowflakeNextId());
                    userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
                    userInfo.setIsVip(0);
                    userInfoMapper.insert(userInfo);
                    //2.3 发送Kafka异步消息，通知账户微服务新增账户记录
                    kafkaService.sendMessage(KafkaConstant.QUEUE_USER_REGISTER, userInfo.getId().toString());
                }

                //3.基于用户记录生成Token 将用户令牌存入Redis Key:前缀+token  Value:用户信息UserInfoVo
                String token = IdUtil.fastUUID();
                String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
                //排除掉用户隐私数据
                UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
                redisTemplate.opsForValue().set(loginKey, userInfoVo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

                //4.将用户token封装结果返回
                Map<String, String> mapResult = new HashMap<>();
                mapResult.put("token", token);
                return mapResult;
            }
            return null;
        } catch (Exception e) {
            log.error("[用户服务]微信登录异常：{}", e);
            throw new RuntimeException(e);
        }
    }
~~~

#### 4.3、 初始化账户信息

如果用户是第一次登录，需要消息中间件初始化账户信息和账户交易信息

## 三、 ElasticSearch

### 1、 倒排索引

正排索引： 文档-关键字

倒排索引：关键字-文档

建立倒排索引的步骤：

1. 将数据根据词条分词，同时记录对应文档的位置
2. 将词条相同的数据合并
3. 对词条进行排序

搜索过程：

先将搜索词语分词，分词后去倒排索引列表查询文档位置(docId)，根据docId查询文档数据。

### 2、 elasticsearch核心概念

#### 2.1、 对照关系型数据库

![es核心概念](D:\资料\笔记\听书\es核心概念.png)

#### 2.2、 索引

一个索引就是一个具有相似特征的文档的集合，索引名必须使用小写字母

#### 2.3、 类型

一个类型就是一个具有相同字段的文档的集合，默认类型为_doc

#### 2.4、 映射

对处理数据的方式和规则做一些限制

### 3、 基础功能

#### 3.1、 索引操作

创建索引：PUT /索引名称

查看所有索引：

~~~json
GET /_cat/indices?v
~~~

查看单个索引：GET /索引名称

删除索引：DELETE /索引名称

#### 3.2、 文档操作

创建文档：

~~~json
PUT /索引名称/_doc/id
{
    jsonbody
}
~~~

查看文档：GET /索引名称/_doc/id

查看所有文档： GET /索引名称/_search

修改文档：替换

~~~json
PUT /索引名称/_doc/id
{
    jsonbody
}
~~~

修改局部属性：

~~~json
POST /索引名称/_update/id
{
  "doc": {
    "属性": "值"
  }
}
~~~

删除文档： DELETE /索引名称/_doc/id

#### 3.3、 映射

查看映射： GET /索引名称/_mapping

动态映射：在将文档保存到索引库时，会根据文档字段自动识别类型

| 数据        | 对应的类型 |
| ----------- | ---------- |
| null        | 字段不添加 |
| true\|flase | boolean    |
| 字符串      | text       |
| 数值        | long       |
| 小数        | float      |
| 日期        | date       |

静态映射：在创建索引库时，事先定义好字段类型

~~~json
PUT /索引名称
{
  "mappings": {
    "properties": {
      "属性": {
        "type": "类型",
        "index": true,
        "store": true,
        "analyzer": "ik_max_word",  //存储分词器
        "search_analyzer": "ik_smart"  //搜索分词器
      }
    }
  }
}
~~~

text类型支持分词，但不能用于排序和聚合

keyword类型不支持分词，可以用于排序和聚合

### 4、 DSL

#### 4.1、 查询所有文档

~~~json
POST /索引名称/_search
{
  "query": {
    "match_all": {}
  }
}
~~~

~~~json
{
  "took" : 0,
  "timed_out" : false,
  "_shards" : {
    "total" : 1,
    "successful" : 1,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : {
      "value" : 3,
      "relation" : "eq"
    },
    "max_score" : 1.0,
    "hits" : [
      {
        "_index" : "my_index",
        "_type" : "_doc",
        "_id" : "1",
        "_score" : 1.0,
        "_source" : {
          "id" : 1,
          "title" : "华为笔记本电脑",
          "category" : "华为",
          "images" : "http://www.gulixueyuan.com/xm.jpg",
          "price" : 5388
        }
      }
    ]
  }
}
"took"字段表示查询花费的时间，单位是毫秒。
"timed_out"字段表示查询是否超时。
"_shards"字段表示查询在哪些分片上执行
"hits"字段表示查询的结果，包括总数量、最大分数和具体的文档内容。
每个文档包含以下字段：
"_index"字段表示文档所在的索引名。
"_id"字段表示文档的唯一标识符。
"_score"字段表示文档的相关性得分。
"_source"字段表示文档内容
~~~

#### 4.2、 匹配查询

match关键字用于执行全文搜索，并具有模糊匹配的功能

~~~json
POST /索引名称/_search
{
  "query": {
    "match": {
        "属性" : "值"
    }
  }
}
~~~

#### 4.3、 多字段匹配查询

~~~json
POST /索引名称/_search
{
  "query": {
    "multi_match": {
      "query": "值",
      "fields": ["属性1","属性2"]
    }
  }
}
~~~

#### 4.4、 关键字精确查询

~~~json
POST /索引名称/_search
{
  "query": {
   "term": {
     "属性": {
       "value": "值"
     }
   }
  }
}
~~~

#### 4.5、 多关键字精确查询

~~~json
POST /索引名称/_search
{
  "query": {
   "terms": {
     "属性": [
       "值1",
       "值2"
     ]
   }
  }
}
~~~

#### 4.6、 范围查询

~~~json
POST /索引名称/_search
{
  "query": {
    "range": {
      "属性": {
        "gte": 5000
      }
    }
  }
}
gte:大于等于
lte:小于等于
gt:大于
lt:小于
~~~

#### 4.7、 指定返回字段

~~~json
POST /索引名称/_search
{
  "query": {
    "range": {
      "属性": {
        "gte": 5000
      }
    }
  },
    "_source": [字段列表]
}
~~~

#### 4.8、 组合查询

- must: 各个条件都必须满足，所有条件是and的关系  &
- should: 各个条件有一个满足即可，所有条件是or的关系 ||
- must_not: 各个条件都不满足，所有条件是not的关系 !=
- filter: 与must效果等同，但是它不计算得分，效率更高。

~~~json
POST /索引名称/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "title": "华为"
          }
        },
        {
          "range": {
            "price": {
              "gte": 3000,
              "lte": 5400
            }
          }
        }
      ]
    }
  }
}
~~~

如果must和should同时存在，他们之间是and的关系

#### 4.9、 聚合查询

与关系型数据库中的group by类似

~~~json
POST /索引名称/_search
{
  "query": {
    "range": {
      "属性": {
        "gte": 5000
      }
    }
  },
    "aggs": {
        "自定义名称": {
            "max": {
                "field": 属性
            }
        }
    }
}
~~~

min,max,sum,avg,stats

~~~json
//桶聚合
POST /my_index/_search
{
  "query": {
    "match_all": {}
  },
  "size": 0, 
  "aggs": {
    "自定义名称": {
      "terms": {
        "field": "分组属性",
        "size": 10
      }
    }
  }
}
~~~

#### 4.10、 排序

~~~json
POST /索引名称/_search
{
  "query": {
    "range": {
      "属性": {
        "gte": 5000
      }
    }
  },
    "sort": [
        {
            "属性": {
                "order": "asc"
            }
        }
    ]
}
~~~

#### 4.11、 分页查询

from：当前页的起始索引

size：每页记录数

#### 4.12、 高亮查询

必须是关键字匹配查询

~~~json
  "highlight": {
    "fields": {
      "属性": {}
    },
    "pre_tags": ["<font color='red'>"],
    "post_tags": ["</font>"]
  }
~~~

#### 4.13、 嵌套查询

~~~json
POST /索引名称/_search
{
  "query": {
    "nested": {
      "path": 数组名
      "query":{
       "match": {
        "属性" : "值"
  }
   }  
    }
    
  }
}
~~~

当将字段设置为nested 嵌套对象将数组中的每个对象索引为单独的隐藏文档，这意味着可以独立于其他对象查询每个嵌套对象







### 5、 Elasticsearch Java API Client

~~~xml
<dependency>
            <groupId>co.elastic.clients</groupId>
            <artifactId>elasticsearch-java</artifactId>
            <version>8.5.3</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.12.3</version>
        </dependency>

        <dependency>
            <groupId>jakarta.json</groupId>
            <artifactId>jakarta.json-api</artifactId>
            <version>2.0.1</version>
        </dependency>
~~~

~~~java
SearchResponse<Object> search = elasticsearchClient.search(s ->
                        s.index("my_index")
                                .query(q -> q.match(m -> m.field("title").query("华为")))
                ,
                Object.class);
~~~

### 6、 Spring Data Elasticsearch

~~~xml
 <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>
~~~

document映射

~~~java
@Data
@Document(indexName = "product")
public class Product implements Serializable {
    
    @Id
    private Long id;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String productName;
    @Field(type = FieldType.Integer)
    private Integer store;
    @Field(type = FieldType.Double, index = true, store = false)
    private double price;
}
~~~

持久层操作对象

~~~java
public interface ProductDao extends ElasticsearchRepository<Product,Long> {


}
~~~

简单查询，创建索引，映射使用Spring Data Elasticsearch  Api；复杂查询使用Elasticsearch Java API Client

## 四、 检索模块

### 1、 封装实体类

~~~java
@Data
@Document(indexName = "albuminfo")
@JsonIgnoreProperties(ignoreUnknown = true)//目的：防止json字符串转成实体对象时因未识别字段报错
public class AlbumInfoIndex implements Serializable {

    private static final long serialVersionUID = 1L;

    // 专辑Id
    @Id
    private Long id;

    //  es 中能分词的字段，这个字段数据类型必须是 text！keyword 不分词！ analyzer = "ik_max_word"
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String albumTitle;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String albumIntro;
    
    //  主播名称
    @Field(type = FieldType.Keyword)
    private String announcerName;

    //专辑封面
    @Field(type = FieldType.Keyword, index = false)
    private String coverUrl;

    //专辑包含声音总数
    @Field(type = FieldType.Long, index = false)
    private Integer includeTrackCount;

    //专辑是否完结：0-否；1-完结
    @Field(type = FieldType.Long, index = false)
    private String isFinished;

    //付费类型：免费、vip免费、付费
    @Field(type = FieldType.Keyword, index = false)
    private String payType;

    @Field(type = FieldType.Date,format = DateFormat.date_time, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime; //

    @Field(type = FieldType.Long)
    private Long category1Id;

    @Field(type = FieldType.Long)
    private Long category2Id;

    @Field(type = FieldType.Long)
    private Long category3Id;

    //播放量
    @Field(type = FieldType.Integer)
    private Integer playStatNum = 0;

    //订阅量
    @Field(type = FieldType.Integer)
    private Integer subscribeStatNum = 0;

    //购买量
    @Field(type = FieldType.Integer)
    private Integer buyStatNum = 0;

    //评论数
    @Field(type = FieldType.Integer)
    private Integer commentStatNum = 0;

    //商品的热度！
    @Field(type = FieldType.Double)
    private Double hotScore = 0d;

    // 专辑属性值
    // Nested 支持嵌套查询
    @Field(type = FieldType.Nested)
    private List<AttributeValueIndex> attributeValueIndexList;

}
~~~

只对专辑标题和专辑简介分词

### 2、 专辑上下架

![专辑上下架](D:\资料\笔记\听书\专辑上下架.png)

根据专辑id查询专辑信息

根据三级分类id查询分类视图对象

根据用户id查询用户信息

#### 2.1、 上架

~~~java
public void upperAlbum(Long albumId) {
        //1.远程调用专辑服务，根据专辑id查询专辑信息
        AlbumInfo albumInfo = albumFeignClient.queryAlbumById(albumId).getData();
        Assert.notNull(albumInfo, "专辑不存在，专辑ID{}", albumId);
        AlbumInfoIndex albumInfoIndex = BeanUtil.copyProperties(albumInfo, AlbumInfoIndex.class);
        //2.处理属性值列表
        List<AlbumAttributeValue> voList = albumInfo.getAlbumAttributeValueVoList();
        List<AttributeValueIndex> attributeValueIndices = new ArrayList<>();
        if (CollUtil.isNotEmpty(voList)){
            attributeValueIndices = voList.stream().map(vo -> {
                AttributeValueIndex attributeValueIndex = BeanUtil.copyProperties(vo, AttributeValueIndex.class);
                return attributeValueIndex;
            }).collect(Collectors.toList());
        }
        albumInfoIndex.setAttributeValueIndexList(attributeValueIndices);
        //3.远程调用专辑服务，根据三级分类id查询分类视图对象
        BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
        Assert.notNull(baseCategoryView,"分类不存在，分类ID：{}", albumInfo.getCategory3Id());
        albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
        albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
        //4.远程调用用户服务，根据用户id查询用户信息
        UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
        Assert.notNull(userInfoVo,"用户不存在，用户ID：{}", albumInfo.getUserId());
        albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
        //5.TODO 封装统计信息，采用产生随机值 以及专辑热度
        //5.1 随机为专辑产生播放量，订阅量，购买量，评论量 、
        int num1 = RandomUtil.randomInt(1000, 2000);
        int num2 = RandomUtil.randomInt(500, 1000);
        int num3 = RandomUtil.randomInt(200, 400);
        int num4 = RandomUtil.randomInt(100, 200);
        albumInfoIndex.setPlayStatNum(num1);
        albumInfoIndex.setSubscribeStatNum(num2);
        albumInfoIndex.setBuyStatNum(num3);
        albumInfoIndex.setCommentStatNum(num4);

        //5.2 基于统计值计算出专辑得分 为不同统计类型设置不同权重
        BigDecimal bigDecimal1 = new BigDecimal(num4).multiply(new BigDecimal("0.4"));
        BigDecimal bigDecimal2 = new BigDecimal(num3).multiply(new BigDecimal("0.3"));
        BigDecimal bigDecimal3 = new BigDecimal(num2).multiply(new BigDecimal("0.2"));
        BigDecimal bigDecimal4 = new BigDecimal(num1).multiply(new BigDecimal("0.1"));
        BigDecimal hotScore = bigDecimal1.add(bigDecimal2).add(bigDecimal3).add(bigDecimal4);
        albumInfoIndex.setHotScore(hotScore.doubleValue());

        //6.将索引库文档对象存入索引库
        albumInfoIndexRepository.save(albumInfoIndex);

    }
~~~

Assert.notNull(albumInfo, "专辑不存在，专辑ID{}", albumId)，用于校验albumInfo变量是否为null，如果为null会抛出异常且异常信息为专辑不存在，专辑ID{}，其中的{}会被替换为albumId的值

#### 2.2、 异步任务优化

自定义线程池

~~~java
@Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        //1.动态得到线程数 IO密集型：CPU逻辑处理器个数*2
        int processorsCount = Runtime.getRuntime().availableProcessors();
        int coreCount = processorsCount * 2;

        //2.通过线程池构造创建线程池对象
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                coreCount,
                coreCount,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                Executors.defaultThreadFactory(),
                (r, e) -> {
                    //r:被拒绝任务  e:线程池对象
                    //自定义拒绝策略：重试-将任务再次提交给线程执行
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    e.submit(r);
                }
        );
        //3.线程池核心线程默认第一个任务提交才创建
        threadPoolExecutor.prestartCoreThread();
        //线程池会尝试创建并启动一个新的线程，作为核心线程之一。这可以确保在有任务提交到线程池时，有一个可用的线程来处理这些任务，从而减少任务等待的时间。
        return threadPoolExecutor;
    }
~~~

#### 2.3、 下架

下架就是将索引库中的文档删除

### 3、 专辑关键字检索

~~~json
GET /albuminfo/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "bool": {
            "should": [
              {
                "match": {
                  "albumTitle": "经典留声机"
                }
              },
               {
                "match": {
                  "albumIntro": "经典留声机"
                }
              },
              {
                "term": {
                  "announcerName": {
                    "value": "经典留声机"
                  }
                }
              }
            ]
          }
        }
      ],
      "filter": [
        {
         "term": {
           "category1Id": "8"
         } 
        },
         {
         "term": {
           "category2Id": "148"
         } 
        },
         {
         "term": {
           "category3Id": "1261"
         } 
        },
        {
          "nested": {
            "path": "attributeValueIndexList",
            "query": {
              "bool": {
                "must": [
                  {
                    "term": {
                      "attributeValueIndexList.attributeId": {
                        "value": "10"
                      }
                    }
                  },
                  {
                    "term": {
                      "attributeValueIndexList.valueId": {
                        "value": "21"
                      }
                    }
                  }
                ]
              }
            }
          }
        },
         {
          "nested": {
            "path": "attributeValueIndexList",
            "query": {
              "bool": {
                "must": [
                  {
                    "term": {
                      "attributeValueIndexList.attributeId": {
                        "value": "11"
                      }
                    }
                  },
                  {
                    "term": {
                      "attributeValueIndexList.valueId": {
                        "value": "23"
                      }
                    }
                  }
                ]
              }
            }
          }
        }
      ]
    }
  },
   "from": 0,
  "size": 10,
  "sort": [
    {
      "hotScore": {
        "order": "desc"
      }
    }
  ],
  "_source": {"excludes": ["category1Id", "category2Id", "category3Id"]},
  "highlight": {
    "fields": {"albumTitle": {}},
    "pre_tags": "<font style='color:red'>",
    "post_tags": "</font>"
  }
}
~~~

构建请求对象

~~~java
 public SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery) {
        //1.创建检索请求构建器对象-封装检索索引库 及 所有检索DSL语句
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(INDEX_NAME);
        //2.设置请求体参数"query",处理查询条件（关键字、分类、标签）
        //2.1 创建最外层bool组合条件对象
        BoolQuery.Builder allBoolQueryBuilder = new BoolQuery.Builder();
        //2.2 处理关键字查询条件 采用must必须满足，包含bool组合三个子条件，三个子条件或者关系
        String keyword = albumIndexQuery.getKeyword();
        if (ObjectUtil.isNotEmpty(keyword)){
            BoolQuery.Builder keyWordBoolQueryBuilder = new BoolQuery.Builder();
            keyWordBoolQueryBuilder.should(s -> s.match(m -> m.field("albumTitle").query(keyword)));
            keyWordBoolQueryBuilder.should(s -> s.match(m -> m.field("albumIntro").query(keyword)));
            keyWordBoolQueryBuilder.should(s -> s.term(t-> t.field("announcerName").value(keyword)));
            allBoolQueryBuilder.must(keyWordBoolQueryBuilder.build()._toQuery());
        }
        //2.3 处理分类ID查询条件
        if (ObjectUtil.isNotEmpty(albumIndexQuery.getCategory1Id())){
            allBoolQueryBuilder.filter(f->f.term(t->t.field("category1Id").value(albumIndexQuery.getCategory1Id())));
        }
        if (ObjectUtil.isNotEmpty(albumIndexQuery.getCategory2Id())){
            allBoolQueryBuilder.filter(f->f.term(t->t.field("category2Id").value(albumIndexQuery.getCategory2Id())));
        }
        if (ObjectUtil.isNotEmpty(albumIndexQuery.getCategory3Id())){
            allBoolQueryBuilder.filter(f->f.term(t->t.field("category3Id").value(albumIndexQuery.getCategory3Id())));
        }
        //2.4 处理标签查询条件(可能有多个)
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if (CollUtil.isNotEmpty(attributeList)){
            for (String attribute : attributeList) {
                String[] split = attribute.split(":");
                if (split != null && split.length ==2){
                    allBoolQueryBuilder.filter(f->f.nested(n->n.path("attributeValueIndexList")
                            .query(q->q.bool(b->b.must(m->m.term(t->t.field("attributeValueIndexList.attributeId").value(split[0])))
                                    .must(m->m.term(t->t.field("attributeValueIndexList.valueId").value(split[1]))))) ));
                }
            }
        }
        //2.5 将最外层bool组合条件对象设置到请求体参数"query"中
        builder.query(allBoolQueryBuilder.build()._toQuery());
        //3.设置请求体参数"from","size" 处理分页
        int from = (albumIndexQuery.getPageNo() -1) * albumIndexQuery.getPageSize();
        int size = albumIndexQuery.getPageSize();
        builder.from(from);
        builder.size(size);
        //4.设置请求体参数"sort" 处理排序（动态 综合、播放量、发布时间）
        //4.1 判断参数排序是否提交 提交形式： 排序字段（1：综合 2：播放量 3：发布时间）:排序方式
        if (ObjectUtil.isNotEmpty(albumIndexQuery.getOrder())){
            String[] split = albumIndexQuery.getOrder().split(":");
            if (split != null && split.length == 2){
                String orderField = "";
                switch (split[0]){
                    case "1":
                        orderField = "hotScore";
                        break;
                    case "2":
                        orderField = "playStatNum";
                        break;
                    case "3":
                        orderField = "createTime";
                        break;
                }
                String finalOrderField = orderField;
                builder.sort(s->s.field(f->f.field(finalOrderField).order("asc".equals(split[1]) ? SortOrder.Asc : SortOrder.Desc)));
            }
        }
        //5.设置请求体参数"highlight" 处理高亮，前提：用户录入关键字
        if (ObjectUtil.isNotEmpty(keyword)){
            builder.highlight(h -> h.fields("albumTitle", hf -> hf.preTags("<font style='color:red'>").postTags("</font>")));
        }
        //6.设置请求体参数"_source" 处理字段指定
        builder.source(s -> s.filter(f -> f.excludes("category1Id",
                "category2Id",
                "category3Id",
                "attributeValueIndexList.attributeId",
                "attributeValueIndexList.valueId")));

        //7.调用构建器builder返回检索请求对象
        return builder.build();

    }
~~~

封装检索结果

~~~java
public AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery queryVo) {
    //1.构建响应VO对象
    AlbumSearchResponseVo· vo = new AlbumSearchResponseVo();
    //2.封装分页信息（总记录数、总页数、页码、页大小）
    vo.setPageNo(queryVo.getPageNo());
    Integer pageSize = queryVo.getPageSize();
    vo.setPageSize(pageSize);
    //1.1 从ES响应结果中得到总记录数
    long total = searchResponse.hits().total().value();
    vo.setTotal(total);
    //1.2 动态计算总页数
    long totalPages = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;
    vo.setTotalPages(totalPages);
    //3.封装检索到业务数据（专辑搜索Vo集合）
    List<Hit<AlbumInfoIndex>> hitList = searchResponse.hits().hits();
    if (CollectionUtil.isNotEmpty(hitList)) {
        List<AlbumInfoIndexVo> infoIndexVoList = hitList.stream().map(hit -> {
            //将获取到的文档对象AlbumInfoIndex类型转为AlbumInfoIndexVo类型
            AlbumInfoIndexVo albumInfoIndexVo = BeanUtil.copyProperties(hit.source(), AlbumInfoIndexVo.class);
            //处理高亮片段
            Map<String, List<String>> highlightMap = hit.highlight();
            if(CollectionUtil.isNotEmpty(highlightMap) && highlightMap.containsKey("albumTitle")){
                String highlightAlbumTitle = highlightMap.get("albumTitle").get(0);
                albumInfoIndexVo.setAlbumTitle(highlightAlbumTitle);
            }
            return albumInfoIndexVo;
        }).collect(Collectors.toList());
        vo.setList(infoIndexVoList);
    }
    //4.返回自定义VO对象
    return vo;
}
~~~

### 4、 热门专辑检索

![首页](D:\资料\笔记\听书\首页.png)

当进入首页，会自动发出三个请求

- 查询所有（1、2、3）分类列表
- 根据一级分类id查询三级分类列表
- 根据一级分类查询所有三级分类下包含的热门专辑（热度前6）

#### 4.1、 根据一级分类id查询三级分类列表

~~~java
	@Override
	public List<BaseCategory3> findTopBaseCategory3(Long category1Id) {
		LambdaQueryWrapper<BaseCategory2> category2LambdaQueryWrapper = Wrappers.lambdaQuery(BaseCategory2.class).eq(BaseCategory2::getCategory1Id, category1Id);
		List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(category2LambdaQueryWrapper);
		if (CollUtil.isNotEmpty(baseCategory2List)){
			List<Long> category2IdList = baseCategory2List.stream().map(BaseCategory2::getId).collect(Collectors.toList());
			LambdaQueryWrapper<BaseCategory3> queryWrapper = Wrappers.lambdaQuery(BaseCategory3.class).eq(BaseCategory3::getIsTop, 1).in(BaseCategory3::getCategory2Id, category2IdList)
					.last("limit 7").orderByDesc(BaseCategory3::getOrderNum);
			List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(queryWrapper);
			return baseCategory3List;
		}
		return null;
	}
~~~

#### 4.2、 根据以及分类id查询分类列表

~~~java
	@Override
	public JSONObject getBaseCategoryListByCategory1Id(Long category1Id) {
		LambdaQueryWrapper<BaseCategoryView> queryWrapper = Wrappers.lambdaQuery(BaseCategoryView.class).eq(BaseCategoryView::getCategory1Id, category1Id);
		List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(queryWrapper);
		if (CollUtil.isNotEmpty(baseCategoryViewList)){
			JSONObject category1 = new JSONObject();
			category1.put("categoryId",baseCategoryViewList.get(0).getCategory1Id());
			category1.put("categoryName",baseCategoryViewList.get(0).getCategory1Name());
			ArrayList<JSONObject> category1Child = new ArrayList<>();
			Map<Long, List<BaseCategoryView>> map1 = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			for (Map.Entry<Long, List<BaseCategoryView>> entry : map1.entrySet()){
				if (ObjectUtil.isNotEmpty(entry)){
					JSONObject category2 = new JSONObject();
					category2.put("categoryId",entry.getKey());
					category2.put("categoryName",entry.getValue().get(0).getCategory2Name());
					ArrayList<JSONObject> category2Child = new ArrayList<>();
					if (CollUtil.isNotEmpty(entry.getValue())){
						for (BaseCategoryView baseCategoryView:entry.getValue()){
							JSONObject category3 = new JSONObject();
							category3.put("categoryId",baseCategoryView.getCategory3Id());
							category3.put("categoryName",baseCategoryView.getCategory3Name());
							category2Child.add(category3);
						}
					}

					category2.put("categoryChild",category2Child);
					category1Child.add(category2);
				}
			}
			category1.put("categoryChild",category1Child);
			return category1;
		}
		return  null;


	}
~~~

#### 4.3、 查询分类下的热门专辑

~~~json
GET /albuminfo/_search
{
  "query": {
    "terms": {
      "category3Id": [
        "1001",
        "1002",
        "1007",
        "1008",
        "1009",
        "1012",
        "1013"
      ]
    }
  },
  "size": 0, 
  "aggs": {
    "category3IdAgg": {
      "terms": {
        "field": "category3Id",
        "size": 10
      },
      "aggs": {
        "top6Agg":{
          "top_hits": {
            "size": 6,
            "sort": [{"hotScore": {"order": "desc"}}]
          }
        }
      }
    }
  }
}
~~~

top_hits是一种用于获取查询结果的聚合方式。它允许用户从每个聚合桶中检索顶部文档，以便进一步分析和可视化。

返回值格式：

~~~java
Map map = new HashMap<String, Object>(); //某个分类下热门专辑对象
     * map.put("baseCategory3", baseCategory3(三级分类对象));
     * map.put("list", albumInfoIndexList（当前三级分类下热门前6的专辑列表）);
~~~

~~~java
@Override
public List<Map<String, Object>> getTopCategory3HotAlbumList(Long category1Id) {
    try {
        //1.根据1级分类ID远程调用专辑服务获取置顶前7个三级分类集合
        //1.1 远程调用专辑服务获取置顶三级分类集合
        List<BaseCategory3> baseCategory3List = albumFeignClient.getTop7BaseCategory3(category1Id).getData();
        Assert.notNull(baseCategory3List, "一级分类{}未包含置顶三级分类", category1Id);
        //1.2 获取所有置顶三级分类ID集合
        List<Long> baseCategory3IdList = baseCategory3List.stream().map(BaseCategory3::getId).collect(Collectors.toList());
        //1.3 将三级分类集合转为Map<三级分类ID，三级分类对象> 方便解析结果封装三级分类对象
        //对BaseCategory3集合处理，转为Map Map中Key:ID，Map中val:三级分类对象BaseCategory3
        Map<Long, BaseCategory3> category3Map = baseCategory3List.stream()
                .collect(Collectors.toMap(BaseCategory3::getId, baseCategory3 -> baseCategory3));
        //1.4 将置顶三级分类ID转为FieldValue类型
        List<FieldValue> fieldValueList = baseCategory3IdList.stream()
                .map(baseCategory3Id -> FieldValue.of(baseCategory3Id))
                .collect(Collectors.toList());
        //2.检索ES获取置顶三级分类（7个）不同置顶三级分类下热度前6个的专辑列表
        //2.1 采用ES检索方法，通过lambda构建请求参数：query,size,aggs
        SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(
                s -> s.index(INDEX_NAME).size(0)
                        .query(q -> q.terms(t -> t.field("category3Id").t
                                            
                                            (tf -> tf.value(fieldValueList))))
                        .aggregations("category3Agg", a -> a.terms(
                                t -> t.field("category3Id").size(10)
                        ).aggregations("top6Agg", a1 -> a1.topHits(t -> t.size(6).sort(sort -> sort.field(f -> f.field("hotScore").order(SortOrder.Desc)))))),
                AlbumInfoIndex.class);
        //3.解析ES响应聚合
        System.out.println(searchResponse);
        //3.1 获取三级分类聚合结果对象
        Aggregate category3Agg = searchResponse.aggregations().get("category3Agg");
        //3.2 获取三级分类聚合“桶”集合 由于三级分类ID字段类型为Long调用lterms方法
        Buckets<LongTermsBucket> buckets = category3Agg.lterms().buckets();
        List<LongTermsBucket> bucketList = buckets.array();
        if (CollectionUtil.isNotEmpty(bucketList)) {
            //3.3 遍历“桶”集合，每遍历一个“桶”处理某个置顶三级分类热门专辑
            List<Map<String, Object>> listMap = bucketList.stream().map(bucket -> {
                Map<String, Object> map = new HashMap<>();
                //3.3.1 处理热门专辑->分类
                long category3Id = bucket.key();
                BaseCategory3 baseCategory3 = category3Map.get(category3Id);
                map.put("baseCategory3", baseCategory3);
                //3.3.2 处理热门专辑->专辑列表
                //3.3.2.1 继续下钻获取子聚合得到当前分类下热门专辑
                Aggregate top6Agg = bucket.aggregations().get("top6Agg");
                List<Hit<JsonData>> hits = top6Agg.topHits().hits().hits();
                if (CollectionUtil.isNotEmpty(hits)) {
                    List<AlbumInfoIndex> hotAlbumList = hits.stream().map(hit -> {
                        //获取专辑JSON对象类型
                        JsonData source = hit.source();
                        return JSON.parseObject(source.toString(), AlbumInfoIndex.class);
                    }).collect(Collectors.toList());
                    map.put("list", hotAlbumList);
                }
                return map;
            }).collect(Collectors.toList());
            return listMap;
        }
    } catch (Exception e) {
        log.error("[检索服务]首页热门专辑异常：{}", e);
        throw new RuntimeException(e);
    }
    return null;
}
~~~

### 5、 关键字自动补全

#### 5.1、 completion suggester

Elasticsearch提供了Completion Suggester查询来实现自动补全功能，这个查询会匹配以用户输入内容开头的词条并返回，要求参与补全的字段是Completion 类型

会构建不是倒排索引，也不是正排索引，就是纯用于进行前缀搜索的一种特殊的数据结构，而且存储在内存中

~~~json
GET /test/_search
{
  "suggest": {
    "自定义名称": {
      "prefix": "前缀",
      "completion": {
        "field": "suggestPinyin",
		"skip_duplicates": true,
        "fuzzy": {
          "fuzziness": "auto"
        }
skip_duplicates属性是用于控制是否跳过查询结果中的重复文档;
fuzziness属性用于设置自动设置编辑距离
~~~

#### 5.2、 初始化题词索引库

~~~java
@Data
@Document(indexName = "suggestinfo")
@JsonIgnoreProperties(ignoreUnknown = true)//目的：防止json字符串转成实体对象时因未识别字段报错
public class SuggestIndex {

    /*悲惨世界*/

    @Id
    private String id;

    /*
       专辑名称，主播名称，用于给用户展示提词  悲惨世界
    * */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;


    /**
     * 用与检索建议词查询字段 汉字 悲 惨 世 界
     */
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keyword;

    /**
     * 用与检索建议词查询字段 完整汉语拼音 beicanshijie
     */
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keywordPinyin;

    /**
     * 用与检索建议词查询字段 完整汉字拼音首字母 bcsj
     */
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keywordSequence;

}
~~~

#### 5.3、 修改上架方法

在上架的最后将专辑标题存入题词库

~~~java
/**
 * 新增提词记录到提词索引库
 *
 * @param albumInfoIndex
 */
@Override
public void saveSuggetIndex(AlbumInfoIndex albumInfoIndex) {
    //1.将专辑标题内容作为提词原始记录存入提词库
    SuggestIndex suggestIndex = new SuggestIndex();
    //1.1 封装提词记录主键 - 跟专辑文档主键一致
    suggestIndex.setId(albumInfoIndex.getId().toString());
    //1.2 封装提词原始内容 给用户展示提词内容（专辑名称）
    String albumTitle = albumInfoIndex.getAlbumTitle();
    suggestIndex.setTitle(albumTitle);
    //1.3 用于提词字段：汉字提词
    suggestIndex.setKeyword(new Completion(new String[]{suggestIndex.getTitle()}));
    //1.4 用于提词字段：拼音提词 将中文转为拼音 采用""
    suggestIndex.setKeywordPinyin(new Completion(new String[]{PinyinUtil.getPinyin(albumTitle, "")}));
    //1.4 用于提词字段：首字母提词 将中文转为拼音首字母 采用""分割
    suggestIndex.setKeywordSequence(new Completion(new String[]{PinyinUtil.getFirstLetter(albumTitle, "")}));
    //2.执行保存
    suggestIndexRepository.save(suggestIndex);
}
~~~

#### 5.4、 关键字自动补全实现

~~~java
//建议词词库
private static final String SUCCEST_INDEX_NAME = "suggestinfo";


/**
 * 根据用户录入部分关键字进行自动补全
 *
 * @param keyword
 * @return
 */
@Override
public List<String> completeSuggest(String keyword) {
    try {
        //1.根据用户录入关键字进行建议提词请求发起
        SearchResponse<SuggestIndex> searchResponse = elasticsearchClient.search(
                s -> s.index(SUCCEST_INDEX_NAME)
                        .suggest(s1 -> s1.suggesters("mySuggestKeyword", fs -> fs.prefix(keyword).completion(c -> c.field("keyword").size(10).skipDuplicates(true)))
                                .suggesters("mySuggestPinyin", fs -> fs.prefix(keyword).completion(c -> c.field("keywordPinyin").size(10).skipDuplicates(true)))
                                .suggesters("mySuggestSequence", fs -> fs.prefix(keyword).completion(c -> c.field("keywordSequence").size(10).skipDuplicates(true)))
                        )
                , SuggestIndex.class
        );
        //2.解析建议词响应结果，将结果进行去重
        Set<String> hashSet = new HashSet<>();
        hashSet.addAll(this.parseSuggestResult("mySuggestKeyword", searchResponse));
        hashSet.addAll(this.parseSuggestResult("mySuggestPinyin", searchResponse));
        hashSet.addAll(this.parseSuggestResult("mySuggestSequence", searchResponse));
        if (hashSet.size() >= 10) {
            return new ArrayList<>(hashSet).subList(0, 10);
        }
        //3.如果建议词记录数小于10，采用全文查询专辑索引库尝试补全
        SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(
                s -> s.index(INDEX_NAME).query(q -> q.match(m -> m.field("albumTitle").query(keyword))),
                AlbumInfoIndex.class

        );
        //解析检索结果，将结果放入HashSet
        List<Hit<AlbumInfoIndex>> hits = response.hits().hits();
        if (CollectionUtil.isNotEmpty(hits)) {
            for (Hit<AlbumInfoIndex> hit : hits) {
                AlbumInfoIndex albumInfoIndex = hit.source();
                hashSet.add(albumInfoIndex.getAlbumTitle());
                if (hashSet.size() >= 10) {
                    break;
                }
            }
        }
        //4.最多返回10个自动补全提示词
        return new ArrayList<>(hashSet);
    } catch (Exception e) {
        log.error("[搜索服务]建议词自动补全异常：{}", e);
        throw new RuntimeException(e);
    }
}


/**
 * 解析建议词结果
 *
 * @param suggestName    自定义建议名称
 * @param searchResponse ES响应结果对象
 * @return
 */
@Override
public Collection<String> parseSuggestResult(String suggestName, SearchResponse<SuggestIndex> searchResponse) {
    //1.获取指定自定义建议词名称获取建议结果
    List<Suggestion<SuggestIndex>> suggestionList = searchResponse.suggest().get(suggestName);
    //2.获取建议自动补全对象
    List<String> list = new ArrayList<>();
    suggestionList.forEach(suggestIndexSuggestion -> {
        //3.获取options中自动补全结果
        for (CompletionSuggestOption<SuggestIndex> suggestOption : suggestIndexSuggestion.completion().options()) {
            SuggestIndex suggestIndex = suggestOption.source();
            list.add(suggestIndex.getTitle());
        }
    });
    return list;
}
~~~

### 6、 整合ELK

~~~xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>5.1</version>
</dependency>
~~~

~~~xml
<!-- logstash日志 -->
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <!-- logstash ip和暴露的端口，logback就是通过这个地址把日志发送给logstash -->
    <destination>192.168.200.6:5044</destination>
    <encoder charset="UTF-8" class="net.logstash.logback.encoder.LogstashEncoder" />
</appender>

<!-- 开发环境 -->
<springProfile name="dev">
    <!-- com.atguigu日志记录器：业务程序INFO级别  -->
    <logger name="com.atguigu" level="INFO" />
    <!--<logger name="com.alibaba" level="WARN" />-->
    <!-- 根日志记录器：INFO级别  -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="LOGSTASH" />
    </root>
</springProfile>
~~~



## 五、 mongoDB

### 1、 基本概念

| RDBMS（MySQL） | MongoDB  |
| -------------- | -------- |
| 数据库         | 数据库   |
| 表             | 集合     |
| 行             | 文档     |
| 列             | 字段     |
| 表联合（join） | 嵌套文档 |
| 主键           | _id      |

适合存储大数据量，低价值的数据。

可以实时的查询，插入和修改数据

### 2、 基本命令

mongoDB区分大小写

mongoDB的文档不能有重复的键

#### 2.1、 数据库操作

查看所有数据库：show dbs

创建数据库：use 数据库名

查看当前数据库：db.getName()

查看当前数据库状态：db.stats()

删除当前数据库：db.dropDatabase()

#### 2.2、 集合操作

查看所有集合：show collections

创建集合：db.createCollection(集合名)

删除集合：db.集合名.drop()

#### 2.3、 文档操作

ObjectId：是一个12字节的BSON数据，4字节表示时间戳，3字节表示机器码，2字节表示进程id，3字节是随机数

文档必须有一个_id键，可以是任何类型，默认是ObjectId类型

插入数据：db.集合名.insert()

查询所有文档：db.集合名.find()

条件查询文档：db.集合名.find({属性：值})

更新文档：db.集合名.update({属性：值},{set{属性：值}},{multi:true})multi为false时只更新匹配到的第一条记录

删除文档：db.集合名.remove(id)

创建索引：db.集合名称.createIndex({属性:1})1为升序，-1为降序

### 3、 集成mongoDB

#### 3.1、 mongoRepository

pom.xml

~~~xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
~~~

自定义实体类与文档进行映射，在实体类上标注@Document注解

自定义接口extends MongoRepository接口，可以根据指定规则自定义方法，MongoRepository会根据方法名自动生成方法体

~~~java
//构建分页对象
Pageable pageable = PageRequest.of(pageNo,pageSize);
//构建条件对象，可以将条件对象封装到实体类中
Example example = Example.of();
//构建排序对象
Sort sort = Sort.by(Sort.Direction.DESC,"属性");
~~~

#### 3.2、 mongoTemplate

~~~java
//构建条件对象
Query query = new Query();
query.addCriteria(Criteria.where(属性).is(值));
//构建修改对象
Update update = new Update();
update.set(属性,值);
//在条件对象中封装分页对象
query.with(PageRequest.of(pageNo,pageSize));
//在条件对象中封装排序对象
query.with(Sort.by(Sort.Direction.DESC, "属性"));
~~~





## 六、 专辑声音详情



### 1、 专辑详情信息

搜索微服务远程调用用户微服务和专辑微服务

1. 通过专辑Id 获取专辑数据albumInfo
2. 通过专辑Id 获取专辑统计信息albumStatVo
3. 通过三级分类Id 获取到分类数据baseCategoryView
4. 通过用户Id 获取到主播信息announcer

将数据汇总封装到map中返回给前端

~~~json
result.put("albumInfo", albumInfo);			获取专辑信息
result.put("albumStatVo", albumStatVo);		获取专辑统计信息
result.put("baseCategoryView", baseCategoryView);	获取分类信息
result.put("announcer", userInfoVo);	获取主播信息
~~~

~~~java
 @Override
    public Map<String, Object> getItemInfo(Long albumId) {
        //1.创建响应结果Map对象 HashMap在多线程环境下并发读写线程不安全：导致key覆盖；导致死循环,采用线程安全的ConcurrentHashMap
        Map<String, Object> mapResult = new ConcurrentHashMap<>();
        //2.远程调用专辑服务，查询专辑信息
        CompletableFuture<AlbumInfo> albumInfoFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.queryAlbumById(albumId).getData();
            Assert.notNull(albumInfo, "专辑：{}不存在", albumId);
            mapResult.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolExecutor);
        //3.远程调用专辑服务，查询专辑统计信息
        CompletableFuture<Void> albumStatCompletableFuture = CompletableFuture.runAsync(() -> {
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Assert.notNull(albumStatVo, "专辑统计信息：{}不存在", albumId);
            mapResult.put("albumStatVo", albumStatVo);
        }, threadPoolExecutor);
        //4.远程调用专辑服务查询分类信息
        CompletableFuture<Void> baseCategoryViewCompletableFuture = albumInfoFuture.thenAcceptAsync(albumInfo -> {
            Long category3Id = albumInfo.getCategory3Id();
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(category3Id).getData();
            Assert.notNull(baseCategoryView, "分类：{}不存在", category3Id);
            mapResult.put("baseCategoryView", baseCategoryView);
        }, threadPoolExecutor);
        //5.远程调用用户服务查询作者信息
        CompletableFuture<Void> announcerCompletableFuture = albumInfoFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "用户：{}不存在", albumInfo.getUserId());
            mapResult.put("userInfoVo", userInfoVo);
        }, threadPoolExecutor);
        //6.组合多个任务
        CompletableFuture.allOf(albumInfoFuture, albumStatCompletableFuture, baseCategoryViewCompletableFuture, announcerCompletableFuture).join();

        return mapResult;
    }
~~~

### 2、 获取专辑声音列表

哪个声音需要展示付费标识

#### 2.1、 获取用户声音列表付费情况

`user_paid_album` 这张表记录了用户购买过的专辑

`user_paid_track` 这张表记录了用户购买过的声音

描述：用于展示当前用户某一页中声音列表购买情况

请求方式：POST

请求路径：/userInfo/userIsPaidTrack/{userId}/{albumId}

请求参数：用户id，专辑id，声音id列表

返回值：Map<Long,Integer> key为trackId，value为是否购买

~~~java
Override
	public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needChackTrackIdList) {
		//1.根据用户id和专辑id查询用户是否购买专辑
		LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumQueryWrapper = Wrappers.lambdaQuery(UserPaidAlbum.class).eq(UserPaidAlbum::getUserId, userId).eq(UserPaidAlbum::getAlbumId, albumId);
		Long count = userPaidAlbumMapper.selectCount(userPaidAlbumQueryWrapper);
		if (count > 0){
			//1.1.如果用户购买了改专辑，则将待检查声音列表设置为已购买
			HashMap<Long, Integer> map = new HashMap<>();
			for (Long trackId:needChackTrackIdList){
				map.put(trackId,1);
			}
			return map;
		}
		//2.根据用户id和声音id列表查询是否购买声音
		LambdaQueryWrapper<UserPaidTrack> userPaidTrackQueryWrapper = Wrappers.lambdaQuery(UserPaidTrack.class).eq(UserPaidTrack::getUserId, userId).in(UserPaidTrack::getTrackId, needChackTrackIdList);
		List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(userPaidTrackQueryWrapper);
		if (CollUtil.isEmpty(userPaidTrackList)){
			//2.1.如果用户没有购买声音，则将待检查声音列表设置为未购买
			HashMap<Long, Integer> map = new HashMap<>();
			for (Long trackId:needChackTrackIdList){
				map.put(trackId,0);
			}
			return map;
		}
		List<Long> userPaidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
		HashMap<Long, Integer> map = new HashMap<>();
		for (Long trackId:needChackTrackIdList){
			if (userPaidTrackIdList.contains(trackId)){
				map.put(trackId,1);
			}else {
				map.put(trackId,0);
			}
		}
		return map;
	}
~~~

#### 2.2、 查询专辑声音列表

~~~java
@Override
	public Page<AlbumTrackListVo> getAlbumTrackPage(Long albumId, Long userId, Page<AlbumTrackListVo> pageParam) {
		//1.根据专辑ID分页获取该专辑下包含声音列表（包含声音统计信息）-默认声音付费标识为false
		pageParam = albumInfoMapper.getAlbumTrackPage(pageParam, albumId);
		//2.根据专辑id查询专辑信息
		AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
		Assert.notNull(albumInfo, "专辑：{}不存在", albumId);
		String payType = albumInfo.getPayType();
		//3.处理用户未登录情况
		if(userId == null){
			if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(payType) || SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(payType)){
				List<AlbumTrackListVo> records = pageParam.getRecords();
				records.stream().filter(vo->vo.getOrderNum() > albumInfo.getTracksForFree()).forEach(vo->vo.setIsShowPaidMark(true));
			}
		}else {
			//4.处理用户以登录情况
			//4.1.远程调用用户服务，查询用户信息
			UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
			Assert.notNull(userInfoVo, "用户{}不存在", userId);
			Integer isVip = userInfoVo.getIsVip();
			//4.2 默认设置需要进一步确定购买情况标识：默认false
			Boolean isNeedCheckPayStatus = false;
			//4.3.如果专辑是vip免费
			if(SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(payType)){
				if (isVip.intValue() == 0){
					//普通用户
					isNeedCheckPayStatus = true;
				}
				if (isVip.intValue() == 1 && userInfoVo.getVipExpireTime().before(new Date())){
					//vip用户，但vip已过期
					isNeedCheckPayStatus = true;
				}
			}
			//4.4.如果专辑是付费
			if(SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(payType)){
				isNeedCheckPayStatus = true;
			}
			if (isNeedCheckPayStatus){
				List<Long> trackIdList = pageParam.getRecords().stream().filter(vo -> vo.getOrderNum() > albumInfo.getTracksForFree()).map(AlbumTrackListVo::getTrackId).collect(Collectors.toList());
				Map<Long, Integer> userPayStatusTrackMap = userFeignClient.userIsPaidTrack(userId, albumId, trackIdList).getData();
				pageParam.getRecords().stream().filter(vo -> vo.getOrderNum() > albumInfo.getTracksForFree() && userPayStatusTrackMap.containsKey(vo.getTrackId()))
						.forEach(vo->vo.setIsShowPaidMark(userPayStatusTrackMap.get(vo.getTrackId()) == 1));
			}

		}
		return pageParam;
	}
~~~

### 3、 声音详情信息

#### 3.1、 播放进度实体类

~~~java
@Data
@Schema(description = "UserListenProcess")
@Document
public class UserListenProcess {

   @Schema(description = "id")
   @Id
   private String id;

   @Schema(description = "用户id")
   private Long userId;

   @Schema(description = "专辑id")
   private Long albumId;

   @Schema(description = "声音id，声音id为0时，浏览的是专辑")
   private Long trackId;

   @Schema(description = "相对于音频开始位置的播放跳出位置，单位为秒。比如当前音频总时长60s，本次播放到音频第25s处就退出或者切到下一首，那么break_second就是25")
   private BigDecimal breakSecond;

   @Schema(description = "是否显示")
   private Integer isShow;

   @Schema(description = "创建时间")
   private Date createTime;

   @Schema(description = "更新时间")
   private Date updateTime;

}
~~~

#### 3.2、 获取声音播放进度

在查看声音详情信息时，会触发一个获取声音播放进度，同时页面每隔10s触发一次保存声音播放进度，将声音播放进度保存到mongodb中

#### 3.3、 更新声音播放进度

~~~java
@Override
	public void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo) {
		//1.根据用户id和声音id查询播放进度
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
		query.limit(1);
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		if (ObjectUtil.isEmpty(userListenProcess)){
			//2.如果播放进度为null，则插入播放进度
			userListenProcess = new UserListenProcess();
			userListenProcess.setUserId(userId);
			userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
			userListenProcess.setTrackId(userListenProcessVo.getTrackId());
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setIsShow(1);
			userListenProcess.setCreateTime(new Date());
			userListenProcess.setUpdateTime(new Date());
		}else {
			//3。如果不为null,则更新播放进度
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setUpdateTime(new Date());
		}
		mongoTemplate.save(userListenProcess,MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		//4.标识当前播放记录是否已经统计
		String key = RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX + userId + ":" + userListenProcessVo.getTrackId();
		long ttl = DateUtil.endOfDay(new Date()).getTime() - System.currentTimeMillis();
		Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, userListenProcess.getTrackId(), ttl, TimeUnit.MILLISECONDS);
		if(flag){
			//5.如果是首次更新播放进度，发送消息到Kafka话题
			//5.1 构建更新声音播放进度MQVO对象
			TrackStatMqVo mqVo = new TrackStatMqVo();
			//生成业务唯一标识，消费者端（专辑服务、搜索服务）用来做幂等性处理，确保一个消息只能只被处理一次
			mqVo.setBusinessNo(IdUtil.fastSimpleUUID());
			mqVo.setAlbumId(userListenProcessVo.getAlbumId());
			mqVo.setTrackId(userListenProcessVo.getTrackId());
			mqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
			mqVo.setCount(1);
			//5.2 发送消息到更新声音统计话题中
			kafkaService.sendMessage(KafkaConstant.QUEUE_TRACK_STAT_UPDATE, JSON.toJSONString(mqVo));
		}
	}
~~~

在service-album微服务中监听消息

~~~java
@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateTrackStat(TrackStatMqVo mqVo) {
		//1.做幂等性校验
		String key = "mq:" + mqVo.getBusinessNo();
		try{
			Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, mqVo.getBusinessNo(), 1, TimeUnit.HOURS);
			if (flag){
				//2.跟新声音的统计信息
				trackStatMapper.updateStat(mqVo.getTrackId(), mqVo.getStatType(), mqVo.getCount());
				//3.更新专辑的统计信息
				if (SystemConstant.TRACK_STAT_PLAY.equals(mqVo.getStatType())) {
					albumStatMapper.updateStat(mqVo.getAlbumId(), SystemConstant.ALBUM_STAT_PLAY, mqVo.getCount());
				}
				if (SystemConstant.TRACK_STAT_COMMENT.equals(mqVo.getStatType())) {
					albumStatMapper.updateStat(mqVo.getAlbumId(), SystemConstant.ALBUM_STAT_COMMENT, mqVo.getCount());
				}
			}
		}catch (Exception e){
			//如果更新数据库发送异常，事务会进行回滚，下次再次投递消息允许继续处理统一个消息
			redisTemplate.delete(key);
			throw new RuntimeException(e);
		}
	}
~~~

#### 3.4、 专辑上次播放专辑声音

将专辑id和声音id封装到map中返回

~~~java
public Map<String, Long> getLatelyTrack() {
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(AuthContextHolder.getUserId()));
		query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
		query.limit(1);
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, AuthContextHolder.getUserId()));
		HashMap<String, Long> resultMap = new HashMap<>();
		if (ObjectUtil.isNotEmpty(userListenProcess)){
			resultMap.put("albumId", userListenProcess.getAlbumId());
			resultMap.put("trackId", userListenProcess.getTrackId());
		}
		return resultMap;

	}
~~~

#### 3.5、 获取声音统计信息

根据trackId查询track_stat,行转列

#### 3.6、更新排行榜

后续通过定时任务调用此方法更新排行榜

~~~java
 @Override
    public void updateLatelyAlbumRanking() {
        try{
            //1查询所有一级分类
            List<BaseCategory1> category1List = albumFeignClient.getAllCategory1().getData();
            Assert.isNull(category1List,"一级分类为空");
            //2声明排序方式数组
            String[] rankingDimensionArray =
                    new String[]{"hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};
            //3.遍历一级分类列表
            for(BaseCategory1 category1 :category1List){
                Long category1Id = category1.getId();
                //4.遍历五种统计方式
                for (String rankingDimension:rankingDimensionArray ){
                    SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(s -> s.index(INDEX_NAME).query(q->q.term(
                            t->t.field("category1Id").value(category1Id)
                    )).sort(st->st.field(f->f.field(rankingDimension).order(SortOrder.Desc))).size(10), AlbumInfoIndex.class);
                    List<Hit<AlbumInfoIndex>> hits = response.hits().hits();
                    if(CollUtil.isNotEmpty(hits)){
                        List<AlbumInfoIndex> albumInfoIndexList = hits.stream().map(hit -> {
                            return hit.source();
                        }).collect(Collectors.toList());
                        //5.将榜单存入redis中
                        String key = RedisConstant.RANKING_KEY_PREFIX + category1Id;
                        redisTemplate.opsForHash().put(key,rankingDimension,albumInfoIndexList);
                    }

                }
            }
        }catch (Exception e){
            log.error("[搜索服务]更新排行榜异常：{}", e);
            throw new RuntimeException(e);
        }

    }
~~~

#### 3.7、 获取排行榜

直接从redis中获取





## 七、 详情优化

### 1、 zipkin



~~~xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zipkin</artifactId>
    <version>2.2.8.RELEASE</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
~~~

~~~yml
spring:
  zipkin:
    base-url: http://192.168.254.156:9411 #Zipkin服务器的URL，用于收集跟踪数据。
    discovery-client-enabled: false #设置为false表示不使用服务发现来查找Zipkin服务器。
    sender:
      type: web #发送器类型，web意味着数据将通过HTTP发送到Zipkin服务器。
management:
  zipkin:
    tracing:
      endpoint: http://192.168.254.156:9411/api/v2/spans #这是Zipkin API的端点，用于获取跟踪数据。
  tracing:
    sampling:
      probability: 1.0 # 记录速率100%
~~~

### 2、 缓存常见问题

#### 2.1、 缓存穿透

缓存穿透是指查询一个不存在的数据，由于缓存无法命中，每次都需要查询数据库，但数据也没有此数据

解决：

1. 空对象缓存，但缓存的过期时间很短，最长不超过五分钟
2. 布隆过滤器

#### 2.2、 缓存雪崩

缓存雪崩是指所有缓存在某一时刻同时失效，请求全部转发到数据库

解决：

1. 差异失效时间，在原有过期时间的基础上增加1-5分钟的随机值
2. 集群部署

#### 2.3、 缓存击穿

缓存击穿是指热点key突然失效，高并发请求全部转发到数据库

解决：锁

### 3、 缓存击穿解决方案

#### 3.1、 ApacheBench性能测试工具

- `-n`：要发送的请求数量。 5000
- `-c`：并发请求数量。 100
- `-t`：测试的最大运行时间。
- `-p`：要发送的POST数据文件。
- `-H`：要包含的HTTP请求头。
- `-T`：POST数据的内容类型。

#### 3.2、 自定义分布式锁

1. setnx 业务逻辑出现异常，锁无法释放
2. 设置过期时间，要使加锁和设置过期时间的代码具有原子性，否则在设置过期时间之前出现异常也会导致锁无法释放
3. 使用uuid防止误删锁，要使判断是否是自己的锁和删除锁的代码具有原子性，否则在判断后锁刚好过期，此时会删除别的线程创建的锁
4. 使用lua脚本保证原子性

~~~java
		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        stringRedisTemplate.execute(redisScript, Arrays.asList("lock"), uuid);
~~~

#### 3.3、 redisson

~~~java
//获取锁
RLock lock = redisson.getLock("myLock");

//阻塞获取锁，锁的有效期为30s，每隔10s看门狗会刷新锁的有效期
lock.lock();
//阻塞获取锁，并指定锁的有效期
lock.lock(10, TimeUnit.SECONDS);

//非阻塞获取锁，指定等待获取锁时间和锁的有效期
boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
~~~

### 4、 获取专辑信息优化

~~~java
@Override
	public AlbumInfo queryAlbumById(Long albumId) {
		try{
			//1.直接从缓存中获取专辑信息
			String dataKey = RedisConstant.ALBUM_INFO_PREFIX + albumId;
			AlbumInfo albumInfo = (AlbumInfo)redisTemplate.opsForValue().get(dataKey);
			if(ObjectUtil.isNotEmpty(albumInfo)){
				log.info("命中缓存，直接返回，线程ID：{}，线程名称：{}", Thread.currentThread().getId(), Thread.currentThread().getName());
				return albumInfo;
			}
			//2.缓存中没有，先获取分布式锁
			String lockKey = RedisConstant.ALBUM_INFO_PREFIX + albumId +RedisConstant.CACHE_LOCK_SUFFIX;
			RLock lock = redissonClient.getLock(lockKey);
			lock.lock();
			try{
				//3.获取锁成功，再次查询缓存
				albumInfo = (AlbumInfo)redisTemplate.opsForValue().get(dataKey);
				if (ObjectUtil.isNotEmpty(albumInfo)){
					log.info("当前线程{},获取锁成功，且再次命中缓存成功", Thread.currentThread().getName());
					return albumInfo;
				}
				//4.缓存中没有，查询数据库，并将数据保存到缓存中
				albumInfo = this.getAlbumInfoFromDB(albumId);
				long ttl = albumInfo == null ? RedisConstant.ALBUM_TEMPORARY_TIMEOUT : RedisConstant.ALBUM_TIMEOUT;
				redisTemplate.opsForValue().set(dataKey,albumInfo,ttl, TimeUnit.SECONDS);
			}finally {
				//4.业务执行完毕释放锁
				log.info("当前线程：{}，释放锁", Thread.currentThread().getName());
				lock.unlock();
			}
		}catch (Exception e){
			//5.兜底处理方案：Redis服务有问题，将业务数据获取自动从数据库获取
			log.error("[专辑服务]查询专辑数据异常：{}", e);
			return getAlbumInfoFromDB(albumId);
		}
		return null;
	}
	/***
	 * 根据专辑ID查询专辑信息包含专辑标签列表
	 * @param id 专辑ID
	 * @return
	 */
	@Override
	public AlbumInfo getAlbumInfoFromDB(Long albumId) {
		AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
		if (ObjectUtil.isEmpty(albumInfo)){
			throw new GuiguException(400,"专辑不存在");
		}
		LambdaQueryWrapper<AlbumAttributeValue> attributeQueryWrapper = Wrappers.lambdaQuery(AlbumAttributeValue.class).eq(AlbumAttributeValue::getAlbumId, albumId);
		List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueService.list(attributeQueryWrapper);
		albumInfo.setAlbumAttributeValueVoList(albumAttributeValueList);
		return albumInfo;
	}
~~~

### 5、 AOP与分布式锁整合

#### 5.1、 自定义注解

~~~java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GuiGuCache {


    /**
     * 传递缓存key的数据区别
     * @return
     */
    String prefix() default "data:";

}
~~~

#### 5.2、 自定义切面类

~~~java
@Component
@Slf4j
@Aspect
public class GuiGuCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @SneakyThrows
    @Around("@annotation(guiGuCache)")
    public Object guiGuCacheAdvice(ProceedingJoinPoint joinPoint,GuiGuCache guiGuCache){
        try{
            //1.优先从redis中查询数据
            String prefix = guiGuCache.prefix();
            String paramVal = "none";
            Object[] args = joinPoint.getArgs();
            if(args != null && args.length > 0){
                paramVal = Arrays.asList(args).stream().map(arg -> arg.toString()).collect(Collectors.joining(":"));
            }
            String dataKey = prefix  + paramVal;
            Object resultObject = redisTemplate.opsForValue().get(dataKey);
            if (ObjectUtil.isNotEmpty(resultObject)){
                return resultObject;
            }
            //2.如果缓存中没有数据，获取分布式锁
            String lockKey = dataKey + RedisConstant.CACHE_LOCK_SUFFIX;
            RLock lock = redissonClient.getLock(lockKey);
            lock.lock();
            try{
                //3.获取分布式锁成功后，再次查询缓存
                resultObject = redisTemplate.opsForValue().get(dataKey);
                if (ObjectUtil.isNotEmpty(resultObject)){
                    return resultObject;
                }
                //4.缓存没有命中，则查询数据库,并保存到缓存中
                resultObject = joinPoint.proceed();
                long ttl = resultObject == null ? RedisConstant.ALBUM_TEMPORARY_TIMEOUT : RedisConstant.ALBUM_TIMEOUT;
                redisTemplate.opsForValue().set(dataKey,resultObject,ttl);
                return resultObject;
            }finally {
                lock.unlock();
            }

        }catch (Throwable e){
            log.info("自定义缓存切面异常：{}", e);
            //5.兜底处理方案：如果redis服务不可用，则执行查询数据库方法
            return joinPoint.proceed();
        }
        return null;
    }
}
~~~

#### 5.3、 布隆过滤器

由一个二进制数组和一系列哈希函数构成，用于判断集合中是否存在某个函数

存入：

1. 通过K个哈希函数计算出K个哈希值
2. k个哈希值映射到数组的k个下标
3. 将数组的k个下标位置置为1

查询：

1. 通过K个哈希函数计算出K个哈希值
2. k个哈希值映射到数组的k个下标
3. 如果有一个数组下标对应的元素为0，那么该数据不存在，如果所有数组下标对应的元素都存在，那么该数据可能存在

缺点：存在误判率，删除困难，只能重构布隆过滤器

初始化布隆过滤器：

让启动类实现CommandLineRunner类，在容器刷新后执行特定操作

~~~java
@Autowired
    private RedissonClient redissonClient;

    /**
     * 初始化布隆过滤器
     * CommandLineRunner是springboot提供方法该方法boot应用启动成功后，自动触发一次
     *
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        System.out.println("布隆过滤器初始化");
        //1.获取到布隆过滤器对象
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        //2.调用初始化方法 p1:数据规模  p2:误判率
        bloomFilter.tryInit(500000L, 0.03);
    }
~~~

在上架专辑时将专辑id存入布隆过滤器

在根据专辑id查询专辑详情数据时先在布隆过滤器判断专辑是否存在

#### 5.4、 数据一致性方案

服务端：

先更新数据库在删除缓存。先更新数据库，数据库就会产生一条变更日志，记录在binlog中，通过订阅binlog日志获取更新的数据，然后再执行删除缓存

cannal模拟mysql的主从交互协议，将自己伪装成从节点，向主节点发送dump请求。

客户端：

封装变更数据

~~~java
@Data
public class CDCEntity {

    @Column(name = "id")
    private Long id;
}
~~~

定义一个类实现EntryHandle接口，该类的作用是处理 Canal 数据变更事件

当 Canal 客户端连接到 Canal 服务器并订阅了相应的数据库表后，每当表中的数据发生变更时，Canal 服务器会将变更的数据封装成一个 `Entry` 对象，然后通过 Canal 协议将这个对象发送给 Canal 客户端。Canal 客户端接收到这个 `Entry` 对象后，会调用 `EntryHandler` 类的相应方法来处理这个对象。

`@CanalTable("album_info")`的作用是指定一个表名，用于在MyBatis Plus中与Canal进行数据同步。当MyBatis Plus执行数据库操作时，它会将操作记录到Canal中，然后通过监听器将Canal中的数据同步到目标数据库。通过使用`@CanalTable`注解，可以指定要同步的表名，以便只同步特定的表。





## 八、 订单

### 1、 账户处理

#### 1.1、 显示账户余额

根据用户id查询用户账户表user_account

### 2、 订单结算

~~~java
@Schema(description = "订单确认对象")
public class TradeVo {

    @NotEmpty(message = "付款项目类型不能为空")
    @Schema(description = "付款项目类型: 1001-专辑 1002-声音 1003-vip会员", required = true)
    private String itemType;

    @Positive(message = "付款项目类型Id不能为空") //被标记的元素必须是正数
    @Schema(description = "付款项目类型Id", required = true)
    private Long itemId;

    @Schema(description = "针对购买声音，购买当前集往后多少集", required = false)
    private Integer trackCount;

}
~~~

~~~java
@Data
@Schema(description = "订单对象")
public class OrderInfoVo {

    @NotEmpty(message = "交易号不能为空")
    @Schema(description = "交易号", required = true)
    private String tradeNo;

    @NotEmpty(message = "支付方式不能为空")
    @Schema(description = "支付方式：1101-微信 1102-支付宝 1103-账户余额", required = true)
    private String payWay;

    @NotEmpty(message = "付款项目类型不能为空")
    @Schema(description = "付款项目类型: 1001-专辑 1002-声音 1003-vip会员", required = true)
    private String itemType;

    /**
     * value：最小值
     * inclusive：是否可以等于最小值，默认true，>= 最小值
     * message：错误提示（默认有一个错误提示i18n支持中文）
     *
     * @DecimalMax 同上
     * @Digits integer： 整数位最多几位
     * fraction：小数位最多几位
     * message：同上，有默认提示
     */
    @DecimalMin(value = "0.00", inclusive = false, message = "订单原始金额必须大于0.00")
    @DecimalMax(value = "9999.99", inclusive = true, message = "订单原始金额必须大于9999.99")
    @Digits(integer = 4, fraction = 2)
    @Schema(description = "订单原始金额", required = true)
    @JsonSerialize(using = Decimal2Serializer.class)
    private BigDecimal originalAmount;

    @DecimalMin(value = "0.00", inclusive = true, message = "减免总金额必须大于0.00")
    @DecimalMax(value = "9999.99", inclusive = true, message = "减免总金额必须大于9999.99")
    @Digits(integer = 4, fraction = 2)
    @Schema(description = "减免总金额", required = true)
    @JsonSerialize(using = Decimal2Serializer.class)
    private BigDecimal derateAmount;

    @DecimalMin(value = "0.00", inclusive = false, message = "订单总金额必须大于0.00")
    @DecimalMax(value = "9999.99", inclusive = true, message = "订单总金额必须大于9999.99")
    @Digits(integer = 4, fraction = 2)
    @Schema(description = "订单总金额", required = true)
    @JsonSerialize(using = Decimal2Serializer.class)
    private BigDecimal orderAmount;

    @Valid
    @NotEmpty(message = "订单明细列表不能为空")
    @Schema(description = "订单明细列表", required = true)
    private List<OrderDetailVo> orderDetailVoList;

    @Schema(description = "订单减免明细列表")
    private List<OrderDerateVo> orderDerateVoList;

    @Schema(description = "时间戳", required = true)
    private Long timestamp;
    
    @Schema(description = "签名", required = true)
    private String sign;
}
~~~



#### 2.1、 获取vip套餐列表

查询vip_service_config

#### 2.2、 根据套餐ID获取套餐详情

根据id查询vip_service_config

#### 2.3、 vip会员结算

~~~java
    @Override
    public OrderInfoVo tradeOrderData(TradeVo tradeVo) {
        //1.创建订单确认对象
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setItemType(tradeVo.getItemType());
        BigDecimal orderAmount = new BigDecimal(0.0);//订单总金额
        BigDecimal derateAmount = new BigDecimal(0.0);//减免总金额
        BigDecimal originalAmount = new BigDecimal(0.0);//订单原始金额
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        //2.处理vip订单
        if(SystemConstant.ORDER_ITEM_TYPE_VIP.equals(tradeVo.getItemType())){
            //2.1.远程调用用户服务，根据vipId查询vip套餐
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(tradeVo.getItemId()).getData();
            //2.2.设置订单金额，订单原始金额，减免金额
            orderAmount = vipServiceConfig.getDiscountPrice();
            originalAmount = vipServiceConfig.getPrice();
            derateAmount = originalAmount.subtract(orderAmount);
            //2.3.设置订单减免明细
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("VIP限时优惠：" + derateAmount);
            orderDerateVoList.add(orderDerateVo);
            //2.4.设置订单明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVo.setItemPrice(orderAmount);
            orderDetailVoList.add(orderDetailVo);
        }else if(SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(tradeVo.getItemType())){
            //todo 3.处理声音订单
        }else if(SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(tradeVo.getItemType())){
            //todo 4.处理专辑订单
        }
        //5.统一设置属性
        orderInfoVo.setOrderAmount(orderAmount);//订单总金额
        orderInfoVo.setDerateAmount(derateAmount);//减免总金额
        orderInfoVo.setOriginalAmount(originalAmount);//订单原始金额
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        //6.生成订单流水号，防止订单重复提交
        Long userId = AuthContextHolder.getUserId();
        String tradeNoKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String tradeNo = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo,5, TimeUnit.MINUTES);
        orderInfoVo.setTradeNo(tradeNo);
        //7.设置结算时间戳
        orderInfoVo.setTimestamp(DateUtil.current());
        //8.生成签名
        Map<String, Object> paramsMap = BeanUtil.beanToMap(orderInfoVo, false, true);
        String sign = SignHelper.getSign(paramsMap);
        orderInfoVo.setSign(sign);

        return orderInfoVo;



    }
~~~

#### 2.4、 判断用户是否购买过专辑

查询user_paid_album

#### 2.5、 专辑结算

~~~java
//4.处理专辑订单
            //4.1.判断用户是否已经购买
            Long albumId = tradeVo.getItemId();
            Boolean isBuy = userFeignClient.isPaidAlbum(albumId).getData();
            if (isBuy){
                throw new GuiguException(400, "当前用户已购买该专辑！");
            }
            //4.2.远程调用专辑服务，根据专辑id查询专辑信息
            AlbumInfo albumInfo = albumFeignClient.queryAlbumById(albumId).getData();
            Assert.notNull(albumInfo,"专辑信息不存在");
            //4.3.远程调用用户服务，根据用户id查询用户信息
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
            Assert.notNull(userInfoVo,"用户信息不存在");
            //4.4.计算专辑价格
            originalAmount = albumInfo.getPrice();
            orderAmount = originalAmount;
            //4.4.1.判断用户是普通会员且专辑有普通用户折扣
            if (albumInfo.getDiscount().intValue() != -1){
                if (userInfoVo.getIsVip() == 0 ||(userInfoVo.getIsVip() ==1 && userInfoVo.getVipExpireTime().before(new Date()))){
                    derateAmount = originalAmount.multiply(albumInfo.getDiscount().divide(new BigDecimal(10.0),2, RoundingMode.HALF_UP));
                    orderAmount = originalAmount.subtract(derateAmount);
                }
            }
            //4.4.2.判断用户是会员且专辑有会员折扣
            if (albumInfo.getVipDiscount().intValue() != -1){
                if (userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().after(new Date())){
                    derateAmount = orderAmount.multiply(albumInfo.getVipDiscount().divide(new BigDecimal(10.0),2, RoundingMode.HALF_UP));
                    orderAmount = originalAmount.subtract(derateAmount);
                }
            }
            //4.5.设置订单减免明细
            if (originalAmount.compareTo(orderAmount) != 1){
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setRemarks("专辑限时优惠" + derateAmount);
            }

            //4.6.设置订单明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(orderAmount);
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVoList.add(orderDetailVo);

~~~

#### 2.6、 获取用户已购买声音id列表

查询user_paid_track

#### 2.7、 声音分集购买列表

![声音分级购买列表](D:\资料\笔记\听书\声音分级购买列表.png)

返回值：

~~~java
Map<String, Object> map = new HashMap<>();
map.put("name","本集"); // 显示文本
map.put("price",albumInfo.getPrice()); // 专辑声音对应的价格
map.put("trackCount",1); // 记录购买集数
list.add(map);
~~~

~~~java
@Override
	public List<Map<String, Object>> getUserWaitBuyTrackPayList(Long trackId) {
		//1.根据声音id查询声音信息
		TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
		Long albumId = trackInfo.getAlbumId();
		//2.查询orderNum大于当前声音的声音列表
		LambdaQueryWrapper<TrackInfo> queryWrapper = Wrappers.lambdaQuery(TrackInfo.class).ge(TrackInfo::getOrderNum, trackInfo.getOrderNum()).eq(TrackInfo::getAlbumId, albumId);
		List<TrackInfo> waitBuyTrackList = trackInfoMapper.selectList(queryWrapper);
		//3.远程调用用户服务，查询用户已购买的声音列表
		List<Long> buyTrackIdList = userFeignClient.findUserPaidTrackList(albumId).getData();
		//4.过滤掉用户已购买的声音
		if(CollUtil.isNotEmpty(buyTrackIdList)){
			waitBuyTrackList = waitBuyTrackList.stream().filter(waitBuyTrackInfo -> !buyTrackIdList.contains(waitBuyTrackInfo.getId())).collect(Collectors.toList());
		}
		//5.构造返回值
		List<Map<String, Object>> mapList = new ArrayList<>();
		if (CollUtil.isNotEmpty(waitBuyTrackList)){
			AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
			BigDecimal price = albumInfo.getPrice();
			Map<String, Object> currMap = new HashMap<>();
			currMap.put("name", "本集");
			currMap.put("price", price);
			currMap.put("trackCount", 1);
			mapList.add(currMap);
			int count = waitBuyTrackList.size();
			for (int i = 10; i <= 50; i += 10) {
				//判断数量>i 固定显示后i集
				if (count > i) {
					Map<String, Object> map = new HashMap<>();
					map.put("name", "后" + i + "集");
					map.put("price", price.multiply(new BigDecimal(i)));
					map.put("trackCount", i);
					mapList.add(map);
				} else {
					//反之全集（动态构建后count集合）
					Map<String, Object> map = new HashMap<>();
					map.put("name", "后" + count + "集");
					map.put("price", price.multiply(new BigDecimal(count)));
					map.put("trackCount", count);
					mapList.add(map);
					break;
				}
			}

		}
		return mapList;
	}
~~~

#### 2.8 、获取待结算声音列表

```java
@Override
public List<TrackInfo> getWaitBuyTrackInfoList(Long trackId, Long trackCount) {
   //1.根据trackId查询声音信息
   TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
   Long albumId = trackInfo.getAlbumId();
   //2.查询用户已购买声音列表
   List<Long> buyTrackIdList = userFeignClient.findUserPaidTrackList(albumId).getData();
   //3.查询声音列表
   LambdaQueryWrapper<TrackInfo> queryWrapper = Wrappers.lambdaQuery(TrackInfo.class).eq(TrackInfo::getAlbumId, albumId)
         .ge(TrackInfo::getOrderNum, trackInfo.getOrderNum())
         .notIn(CollUtil.isNotEmpty(buyTrackIdList), TrackInfo::getId, buyTrackIdList)
         .select(TrackInfo::getId, TrackInfo::getTrackTitle, TrackInfo::getCoverUrl, TrackInfo::getAlbumId)
         .orderByAsc(TrackInfo::getOrderNum)
         .last("limit 10");
   List<TrackInfo> waitBuyTrackList = trackInfoMapper.selectList(queryWrapper);
   if (CollUtil.isEmpty(waitBuyTrackList)){
      throw new GuiguException(400,"该专辑下没有符合购买要求声音");
   }
   return waitBuyTrackList;


}
```

#### 2.9、 声音结算

~~~java
//3.处理声音订单
            //3.1.远程调用专辑服务，获取待购买专辑列表
            List<TrackInfo> trackInfoList = albumFeignClient.getWaitBuyTrackInfoList(tradeVo.getItemId(), tradeVo.getTrackCount()).getData();
            //3.2.查询专辑信息
            AlbumInfo albumInfo = albumFeignClient.queryAlbumById(trackInfoList.get(0).getAlbumId()).getData();
            BigDecimal price = albumInfo.getPrice();
            //3.3.计算金额
            originalAmount = price.multiply(new BigDecimal(tradeVo.getTrackCount()));
            orderAmount = originalAmount;
            //3.4.设置订单明细
            for (TrackInfo trackInfo : trackInfoList){
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                orderDetailVo.setItemId(trackInfo.getId());
                orderDetailVo.setItemPrice(price);
                orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                orderDetailVo.setItemName(trackInfo.getTrackTitle());
                orderDetailVoList.add(orderDetailVo);
            }
~~~



### 3、 提交订单

#### 3.1、 整合分布式事务seata

在订单服务，账户服务，用户服务添加seata依赖

~~~xml
<!--seata-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
    <!-- 默认seata客户端版本比较低，排除后重新引入指定版本-->
    <exclusions>
        <exclusion>
            <groupId>io.seata</groupId>
            <artifactId>seata-spring-boot-starter</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
~~~

~~~sql
CREATE TABLE `undo_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  `ext` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3;
~~~

添加seata配置

~~~yml
seata:
  enabled: true
  tx-service-group: ${spring.application.name}-group # 事务组名称
  service:
    vgroup-mapping:
      #指定事务分组至集群映射关系，集群名default需要与seata-server注册到Nacos的cluster保持一致
      service-user-group: default
  registry:
    type: nacos # 使用nacos作为注册中心
    nacos:
      server-addr: 192.168.254.156:8848 # nacos服务地址
      group: DEFAULT_GROUP # 默认服务分组
      namespace: "" # 默认命名空间
      cluster: default # 默认TC集群名称
~~~



#### 3.2、 检查并扣减账户余额



操作user_account,user_account_detail



#### 3.3、 新增购买记录

~~~java
@Override
@Transactional(rollbackFor = Exception.class)
public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
    //1.判断购买项目类型-处理专辑
    if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(userPaidRecordVo.getItemType())) {
        //1.1 根据订单编号查询专辑购买记录
        LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo());
        Long count = userPaidAlbumMapper.selectCount(userPaidAlbumLambdaQueryWrapper);
        if (count > 0) {
            return;
        }
        //1.2 查询到专辑购买记录为空则新增购买记录
        UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
        userPaidAlbumMapper.insert(userPaidAlbum);
    } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(userPaidRecordVo.getItemType())) {
        //2.判断购买项目类型-处理声音
        //2.1 根据订单编号查询声音购买记录
        LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo());
        Long count = userPaidTrackMapper.selectCount(userPaidTrackLambdaQueryWrapper);
        if (count > 0) {
            return;
        }
        //2.2 查询到声音购买记录为空则新增购买记录（循环批量新增）
        //2.2.1 远程调用专辑服务-根据声音ID查询声音对象-获取声音所属专辑ID
        TrackInfo trackInfo = albumFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0)).getData();
        Long albumId = trackInfo.getAlbumId();
        //2.2.2 遍历购买项目ID集合批量新增声音购买记录
        userPaidRecordVo.getItemIdList().forEach(trackId -> {
            UserPaidTrack userPaidTrack = new UserPaidTrack();
            userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
            userPaidTrack.setUserId(userPaidRecordVo.getUserId());
            userPaidTrack.setAlbumId(albumId);
            userPaidTrack.setTrackId(trackId);
            userPaidTrackMapper.insert(userPaidTrack);
        });
    } else if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(userPaidRecordVo.getItemType())) {
        //3.判断购买项目类型-处理VIP会员-允许多次购买
        //3.1 新增VIP购买记录
        UserVipService userVipService = new UserVipService();
        //3.1.1 根据VIP套餐ID查询套餐信息-得到VIP会员服务月数
        Long vipConfigId = userPaidRecordVo.getItemIdList().get(0);
        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(vipConfigId);
        Integer serviceMonth = vipServiceConfig.getServiceMonth();
        //3.1.2 获取用户身份，如果是VIP会员，则续费
        UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
        Integer isVip = userInfo.getIsVip();
        if (isVip.intValue() == 1 && userInfo.getVipExpireTime().after(new Date())) {
            //如果是VIP会员，则续费
            userVipService.setStartTime(userInfo.getVipExpireTime());
            //续费会员过期时间=现有会员过期时间+套餐服务月数
            userVipService.setExpireTime(DateUtil.offsetMonth(userInfo.getVipExpireTime(), serviceMonth));
        } else {
            //3.1.3 获取用户身份，如果是普通用户，则新开
            userVipService.setStartTime(new Date());
            //续费会员过期时间=现有会员过期时间+套餐服务月数
            userVipService.setExpireTime(DateUtil.offsetMonth(new Date(), serviceMonth));
        }
        //3.1.4 构建VIP购买记录对象保存
        userVipService.setUserId(userPaidRecordVo.getUserId());
        userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
        userVipServiceMapper.insert(userVipService);

        //3.2 更新用户表中VIP状态及会员过期时间
        userInfo.setIsVip(1);
        userInfo.setVipExpireTime(userVipService.getExpireTime());
        userInfoMapper.updateById(userInfo);
    }
}
~~~

#### 3.4、 策略模式优化

- 抽象策略（Strategy）类：这是一个抽象角色，通常由一个接口或抽象类实现。此角色给出所有的具体策略类所需的功能。

- 具体策略（Concrete Strategy）类：实现了抽象策略定义的接口，提供具体的算法实现或行为。

- 环境（Context）类：用来操作策略的上下文环境，屏蔽高层模块（客户端）对策略、算法的直接访问，封装可能存在的变化。

策略接口：

~~~java
package com.atguigu.tingshu.user.strategy;

import com.atguigu.tingshu.vo.user.UserPaidRecordVo;

/**
 * 抽象：策略接口
 * 定义抽象方法：
 */
public interface ItemTypeStrategy {

    /**
     * 处理用户购买记录
     *
     * @param userPaidRecordVo
     */
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo);
}
~~~

策略工厂

~~~java
@Slf4j
@Component
public class StrategyFactory {
    @Autowired
    private Map<String,ItemTypeStrategy> strategyMap;
    public ItemTypeStrategy getStrategy(String itemType){
        if (strategyMap.containsKey(itemType)){
            return strategyMap.get(itemType);
        }
        log.error("该策略实现类不存在");
        throw new GuiguException(500, "该策略" + itemType + "实现类不存在");
    }
}
~~~

#### 3.5、 提交订单

~~~java
@Override
@GlobalTransactional(rollbackFor = Exception.class)
public Map<String, String> submitOrder(OrderInfoVo orderInfoVo, Long userId) {
    //1.业务校验-验证流水号-解决订单重复提交问题
    String tradeNoKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
    //1.1 构建验证流水号lua脚本
    String scriptText = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";

    //2.2 执行脚本，如果脚本返回结果为false 抛出异常即可
    DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(scriptText, Boolean.class);
    boolean flag = (boolean) redisTemplate.execute(redisScript, Arrays.asList(tradeNoKey), orderInfoVo.getTradeNo());
    if (!flag) {
        throw new GuiguException(400, "流水号异常！");
    }
    //2.验证签名-解决用户篡改订单中数据
    //2.1 将提交订单VO参数转为Map 加签并未加入"payWay" ,手动将提交参数Map中payWay移除掉
    Map<String, Object> mapParams = BeanUtil.beanToMap(orderInfoVo);
    mapParams.remove("payWay");
    //2.2 调用签名工具类进行验签
    SignHelper.checkSign(mapParams);
    //3.保存订单及订单明细、优惠明细
    OrderInfo orderInfo = this.saveOrderInfo(orderInfoVo, userId);
    //4.处理余额付款 支付方式：1103 余额支付
    if (SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfoVo.getPayWay())) {
        // 4.1 TODO 余额支付-远程调用账户服务扣减账户余额
        AccountLockVo accountDeductVo = new AccountLockVo();
        accountDeductVo.setOrderNo(orderInfo.getOrderNo());
        accountDeductVo.setUserId(userId);
        accountDeductVo.setAmount(orderInfo.getOrderAmount());
        accountDeductVo.setContent(orderInfo.getOrderTitle());
        Result deductResult = accountFeignClient.checkAndDeduct(accountDeductVo);
        if (200 != deductResult.getCode()) {
            //扣减余额失败：全局事务都需要回滚
            throw new GuiguException(ResultCodeEnum.ACCOUNT_LESS);
        }
        // 4.2 TODO 虚拟物品发货-远程调用用户服务新增购买记录
        UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
        userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
        userPaidRecordVo.setUserId(userId);
        userPaidRecordVo.setItemType(orderInfo.getItemType());
        List<Long> itemIdList = orderInfoVo.getOrderDetailVoList().stream().map(OrderDetailVo::getItemId).collect(Collectors.toList());
        userPaidRecordVo.setItemIdList(itemIdList);

        Result paidRecordResult = userFeignClient.savePaidRecord(userPaidRecordVo);
        if (200 != paidRecordResult.getCode()) {
            //新增购买记录失败：全局事务都需要回滚
            throw new GuiguException(211, "新增购买记录异常");
        }
        // 4.3 订单状态：已支付
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
        orderInfoMapper.updateById(orderInfo);
    }
    //5.响应提交成功订单编号
    Map<String, String> mapResult = new HashMap<>();
    mapResult.put("orderNo", orderInfo.getOrderNo());
    return mapResult;
}
~~~



#### 3.6、 保存订单

~~~java
/**
 * 保存订单及订单商品明细优惠明细
 *
 * @param orderInfoVo 订单信息VO对象
 * @param userId      用户ID
 * @return 保存后订单对象
 */
@Override
@Transactional(rollbackFor = Exception.class)
public OrderInfo saveOrderInfo(OrderInfoVo orderInfoVo, Long userId) {
    //1.保存订单
    //1.1 通过拷贝将订单VO中信息拷贝到订单PO对象中
    OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoVo, OrderInfo.class);
    //1.2 设置用户ID
    orderInfo.setUserId(userId);
    //1.3 为订单设置初始付款状态：未支付
    orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
    //1.4 生成全局唯一订单编号 形式：当日日期+雪花算法
    String orderNo = DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
    orderInfo.setOrderNo(orderNo);
    //1.5 保存订单
    orderInfoMapper.insert(orderInfo);
    Long orderId = orderInfo.getId();

    //2.保存订单商品明细
    List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
    if (CollectionUtil.isNotEmpty(orderDetailVoList)) {
        orderDetailVoList.forEach(orderDetailVo -> {
            OrderDetail orderDetail = BeanUtil.copyProperties(orderDetailVo, OrderDetail.class);
            //关联订单ID
            orderDetail.setOrderId(orderId);
            orderDetailMapper.insert(orderDetail);
        });
    }

    //3.保存订单优惠明细
    List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
    if (CollectionUtil.isNotEmpty(orderDerateVoList)) {
        orderDerateVoList.forEach(orderDetailVo -> {
            OrderDerate orderDerate = BeanUtil.copyProperties(orderDetailVo, OrderDerate.class);
            //关联订单ID
            orderDerate.setOrderId(orderId);
            orderDerateMapper.insert(orderDerate);
        });
    }
    //4.返回订单对象
    return orderInfo;
}
~~~



### 4、 订单明细

~~~java
@Override
    public OrderInfo getOrderInfo(String orderNo) {

        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery(OrderInfo.class).eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNotEmpty(orderInfo)){
            Long orderId = orderInfo.getId();
            LambdaQueryWrapper<OrderDetail> detailLambdaQueryWrapper = Wrappers.lambdaQuery(OrderDetail.class).eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(detailLambdaQueryWrapper);
            orderInfo.setOrderDetailList(orderDetailList);
            LambdaQueryWrapper<OrderDerate> derateLambdaQueryWrapper = Wrappers.lambdaQuery(OrderDerate.class).eq(OrderDerate::getOrderId, orderId);
            List<OrderDerate> orderDerateList = orderDerateService.list(derateLambdaQueryWrapper);
            orderInfo.setOrderDerateList(orderDerateList);
            orderInfo.setOrderStatusName(getOrderStatusName(orderInfo.getOrderStatus()));
            orderInfo.setPayWayName(getPayWayName(orderInfo.getPayWay()));
            return orderInfo;
        }
        return  null;
    }
    private String getOrderStatusName(String orderStatus) {
        if (SystemConstant.ORDER_STATUS_UNPAID.equals(orderStatus)) {
            return "未支付";
        } else if (SystemConstant.ORDER_STATUS_PAID.equals(orderStatus)) {
            return "已支付";
        } else if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderStatus)) {
            return "取消";
        }
        return null;
    }

    /**
     * 根据支付方式编号得到支付名称
     *
     * @param payWay
     * @return
     */
    private String getPayWayName(String payWay) {
        if (SystemConstant.ORDER_PAY_WAY_WEIXIN.equals(payWay)) {
            return "微信";
        } else if (SystemConstant.ORDER_PAY_ACCOUNT.equals(payWay)) {
            return "余额";
        } else if (SystemConstant.ORDER_PAY_WAY_ALIPAY.equals(payWay)) {
            return "支付宝";
        }
        return "";
    }
~~~

### 5、 订单列表

order_info和order_detail联表查询



~~~xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd" >


<mapper namespace="com.atguigu.tingshu.order.mapper.OrderInfoMapper">


    <!--自定义 结果集-->
    <resultMap id="orderResultMap" type="com.atguigu.tingshu.model.order.OrderInfo" autoMapping="true">
        <id column="id" property="id"></id>
        <collection property="orderDetailList" ofType="com.atguigu.tingshu.model.order.OrderDetail" autoMapping="true">
            <id column="order_detail_id" property="id"></id>
        </collection>
    </resultMap>

    <select id="getUserOrderByPage" resultMap="orderResultMap">
        select
            oi.id,
            oi.order_title,
            oi.order_no,
            oi.order_status,
            oi.original_amount,
            oi.order_amount,
            oi.derate_amount,
            oi.item_type,
            oi.pay_way,
            oi.create_time,
            od.id order_detail_id,
            od.item_id,
            od.item_name,
            od.item_url,
            od.item_price
        from order_info oi left join order_detail od
                                     on od.order_id = oi.id
        where user_id = #{userId} and od.is_deleted = 0
        order by id desc
    </select>


    <!--自定义 结果集-->
    <resultMap id="orderResultMap1" type="com.atguigu.tingshu.model.order.OrderInfo" autoMapping="true">
        <id column="id" property="id"></id>
        <collection property="orderDetailList" column="id" ofType="com.atguigu.tingshu.model.order.OrderDetail" select="getOrderDetailList" autoMapping="true"></collection>
    </resultMap>

    <!--根据订单ID查询订单明细-->
    <select id="getOrderDetailList" resultType="com.atguigu.tingshu.model.order.OrderDetail">
        select * from order_detail where order_id = #{id}
    </select>

    <!--分页查询订单列表-->
    <select id="getUserOrderByPage1" resultMap="orderResultMap1">
        select
            oi.id,
            oi.order_title,
            oi.order_no,
            oi.order_status,
            oi.original_amount,
            oi.order_amount,
            oi.derate_amount,
            oi.item_type,
            oi.pay_way,
            oi.create_time
        from order_info oi
        where user_id = #{userId} and is_deleted = 0
        order by id desc
    </select>
</mapper>
~~~

ofType关注集合中元素的数据类型

javaType关系属性本身的数据类型



### 6、 延迟关单

基于redisson的延迟队列和阻塞队列实现

~~~java
@Slf4j
@Component
public class DelayMsgService {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 基于Redisson（Redis）实现延迟消息
     *
     * @param data      数据
     * @param queueName 延迟队列名称
     * @param ttl       延迟时间：单位s
     */
    public void sendDelayMessage(String queueName, String data, int ttl) {
        try {
            //7.1 创建阻塞队列
            RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(queueName);
            //7.2 基于阻塞队列创建延迟队列
            RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
            //7.3 发送延迟消息 测试阶段：设置为30s
            delayedQueue.offer(data, ttl, TimeUnit.SECONDS);
            log.info("发送延迟消息成功：{}", data);
        } catch (Exception e) {
            log.error("[延迟消息]发送异常：{}", data);
            throw new RuntimeException(e);
        }
    }
}
~~~

消费者

~~~java
public void orderCancal(){
        log.info("开启线程监听延迟消息：");
        //1.创建阻塞队列（当队列内元素超过上限，继续队列发送消息，进入阻塞状态/当队列中元素为空，继续拉取消息，进入阻塞状态）
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(KafkaConstant.QUEUE_ORDER_CANCEL);
        //2.开启线程监听阻塞队列中消息 只需要单一核心线程线程池对象即可
        Executors.newSingleThreadExecutor().submit(()->{
            while(true){
                String take = null;
                try {
                    take = blockingQueue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (StringUtils.isNotBlank(take)) {
                    log.info("监听到延迟关单消息：{}", take);
                    //查询订单状态，关闭订单
                    orderInfoService.orderCanncal(Long.valueOf(take));
                }
            }
        });
    }
~~~

消费者端只需获取阻塞队列，从阻塞队列中获取消息



redisson延迟队列的原理：

1. 生产者将消息和对应的延迟时间存储到有序集合中，当前时间与延迟时间之和作为元素的分数。同时，将已发送消息的id存入一个单独的key中
2. 消费者通过执行 `BLPOP` 命令从有序集合中最早可以发送的消息。如果当前有序集合中没有可发送的消息，消费者会一直阻塞等待，直到有消息可供发送。
3. 当消费者成功获取到消息后，将消息id存入已发送消息id列表中，以避免重复处理。
4. 如果消费者处理消息时出现异常，消费者可以将消息 ID 从发送消息id列表中移除，并将消息添加回有序集合中并更新其分数为新的延迟时间。
5. 生产者可以定期清理已发送消息id列表，以释放内存空间。





## 九、 微信支付充值业务

### 1、 保存交易记录

根据订单号查询订单详情

获取充值记录信息---查询recharge_info

保存交易记录---需校验订单状态或充值订单状态是否为已支付

~~~java
@Override
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo, Long userId) {
        //1.根据订单浩查询付款信息表
        LambdaQueryWrapper<PaymentInfo> queryWrapper = Wrappers.lambdaQuery(PaymentInfo.class).eq(PaymentInfo::getOrderNo, orderNo).eq(PaymentInfo::getUserId, userId);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNotEmpty(paymentInfo)){
            return paymentInfo;
        }
        //2.构建本地付款信息对象
        paymentInfo = new PaymentInfo();
        paymentInfo.setUserId(userId);
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentStatus(SystemConstant.ORDER_STATUS_UNPAID);
        paymentInfo.setPaymentType(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        if(SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)){
            //订单
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo).getData();
            if (ObjectUtil.isEmpty(orderInfo)){
                throw new GuiguException(404,"订单不存在");
            }
            if(!SystemConstant.PAYMENT_STATUS_PAID.equals(orderInfo.getOrderStatus())){
                throw new GuiguException(211,"订单状态错误");
            }
            paymentInfo.setAmount(orderInfo.getOrderAmount());
            paymentInfo.setContent(orderInfo.getOrderTitle());
        }else {
            RechargeInfo rechargeInfo = accountFeignClient.getRechargeInfo(orderNo).getData();
            Assert.notNull(rechargeInfo,"充值信息不存在");
            if(!SystemConstant.ORDER_STATUS_PAID.equals(rechargeInfo.getRechargeStatus())){
                throw new GuiguException(211,"充值订单状态错误");
            }
            paymentInfo.setAmount(rechargeInfo.getRechargeAmount());
            paymentInfo.setContent("充值" + rechargeInfo.getRechargeAmount());
        }
        //3.保存付款信息
        paymentInfoMapper.insert(paymentInfo);
        return paymentInfo;


    }
~~~

### 2、获取微信小程序拉起本地微信支付所需要参数

用于调用wx.requestPayment(Object object)进行微信支付

~~~java
@Override
	public Map<String, Object> getWxPrePayParams(String paymentType, String orderNo) {
		//1.将交易记录保存到本地交易表
		Long userId = AuthContextHolder.getUserId();
		PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo, userId);
		//2.调用微信SDK获取小程序支付所需参数
		//2.1 构建调用微信接口业务对象
		JsapiServiceExtension jsapiService = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
		//2.2 构建预支付请求对象
		PrepayRequest prepayRequest = new PrepayRequest();
		Amount amount = new Amount();
		//2.2.1 设置预支付订单金额 单位：分 TODO开发接口暂时硬编码1分 实际应该从paymentInfo对象中获取
		amount.setTotal(1);
		prepayRequest.setAmount(amount);
		prepayRequest.setAppid(wxPayV3Config.getAppid());
		prepayRequest.setMchid(wxPayV3Config.getMerchantId());
		prepayRequest.setDescription(paymentInfo.getContent());
		prepayRequest.setNotifyUrl(wxPayV3Config.getNotifyUrl());
		//2.3 TODO 目前小程序未发布，开发阶段必选设置付款人（真正付款只有应用开发者列表中微信账户才有权限）
		Payer payer = new Payer();
		payer.setOpenid("odo3j4qp-wC3HVq9Z_D9C0cOr0Zs");//TODO同学改为自己微信账户OpenID 能拉起微信支付，但是付款会报错：当前用户不是开发者
		prepayRequest.setPayer(payer);
		//2.2.2 商户端订单编号
		prepayRequest.setOutTradeNo(paymentInfo.getOrderNo());
		//2.3 调用下单方法（小程序所需参数），得到应答
		PrepayWithRequestPaymentResponse response = jsapiService.prepayWithRequestPayment(prepayRequest);
		if (response != null) {
			Map<String, Object> mapResult = new HashMap<>();
			mapResult.put("timeStamp", response.getTimeStamp());
			mapResult.put("package", response.getPackageVal());
			mapResult.put("paySign", response.getPaySign());
			mapResult.put("signType", response.getSignType());
			mapResult.put("nonceStr", response.getNonceStr());
			return mapResult;
		}
		return null;
	}
~~~

### 3、 查询支付状态

用户支付后，前端会轮询查询支付状态

~~~java
@Override
	public Boolean queryPayStatus(String orderNo) {
		//1.创建查询交易请求对象
		QueryOrderByOutTradeNoRequest queryOrderByOutTradeNoRequest = new QueryOrderByOutTradeNoRequest();
		queryOrderByOutTradeNoRequest.setMchid(wxPayV3Config.getMerchantId());
		queryOrderByOutTradeNoRequest.setOutTradeNo(orderNo);
		//2.构建微信业务接口对象
		JsapiServiceExtension jsapiService = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
		//3.调用微信查询交易状态接口
		Transaction transaction = jsapiService.queryOrderByOutTradeNo(queryOrderByOutTradeNoRequest);
		//4.解析响应结果返回交易状态
		if (transaction != null) {
			Transaction.TradeStateEnum tradeState = transaction.getTradeState();
			if (Transaction.TradeStateEnum.SUCCESS == tradeState) {
				//用户支付成功
				return true;
			}
		}
		return false;
	}
~~~

### 4、 异步回调接口

~~~java
@Override
@GlobalTransactional(rollbackFor = Exception.class)
public Map<String, String> paySuccessNotify(HttpServletRequest request) {
    //1.从请求头中获取微信提交参数
    String wechatPaySerial = request.getHeader("Wechatpay-Serial");  //签名
    String nonce = request.getHeader("Wechatpay-Nonce");  //签名中的随机数
    String timestamp = request.getHeader("Wechatpay-Timestamp"); //时间戳
    String signature = request.getHeader("Wechatpay-Signature"); //签名类型

    //HTTP 请求体 body。切记使用原始报文，不要用 JSON 对象序列化后的字符串，避免验签的 body 和原文不一致。
    String requestBody = PayUtil.readData(request);
    //2.构建RequestParam请求参数对象
    RequestParam requestParam = new RequestParam.Builder()
            .serialNumber(wechatPaySerial)
            .nonce(nonce)
            .signature(signature)
            .timestamp(timestamp)
            .body(requestBody)
            .build();
    //3.// 初始化 NotificationParser 解析器对象
    NotificationParser parser = new NotificationParser(rsaAutoCertificateConfig);
    //4. 调用解析器对象解析方法 验签、解密 并转换成 Transaction
    Transaction transaction = parser.parse(requestParam, Transaction.class);
    if (transaction != null) {
        //4.1 业务验证，验证付款状态以及用户实际付款金额跟商户侧金额是否一致
        if (Transaction.TradeStateEnum.SUCCESS == transaction.getTradeState()) {
            Integer payerTotal = transaction.getAmount().getPayerTotal();
            //todo 调试阶段支付金额为1分，后续改为动态从本地交易记录中获取实际金额
            if (payerTotal.intValue() == 1) {
                //4.2 更新本地交易记录状态
                paymentInfoService.updatePaymentInfoSuccess(transaction);
                Map<String, String> map = new HashMap<>();
                map.put("code", "SUCCESS");
                map.put("message", "SUCCESS");
                return map;
            }
        }
    }
    return null;
    
    
}
~~~

修改本地交易状态

~~~java
/**
 * 用户付款成功后，修改本地交易记录
 *
 * @param transaction 微信交易对象
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void updatePaymentInfoSuccess(Transaction transaction) {
    //1.根据订单编号查询本地交易记录状态
    String orderNo = transaction.getOutTradeNo();
    LambdaQueryWrapper<PaymentInfo> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(PaymentInfo::getOrderNo, orderNo);
    PaymentInfo paymentInfo = paymentInfoMapper.selectOne(queryWrapper);
    if (SystemConstant.PAYMENT_STATUS_PAID.equals(paymentInfo.getPaymentStatus())) {
        //如果已支付：返回即可
        return;
    }
    //2.修改本地交易记录
    //2.1 本地交易记录关联微信支付交易ID
    paymentInfo.setOutTradeNo(transaction.getTransactionId());
    //2.2 更新回调时间，及回调内容
    paymentInfo.setCallbackTime(new Date());
    paymentInfo.setCallbackContent(transaction.toString());
    //2.3 将本地交易支付状态：已支付
    paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_PAID);
    paymentInfoMapper.updateById(paymentInfo);

    //3.todo 远程调用订单服务/账户服务 完成订单/充值状态变更：已支付
    //3.1 判断支付类型：1301-订单
    if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentInfo.getPaymentType())) {
        Result result = orderFeignClient.orderPaySuccess(orderNo);
        if (200 != result.getCode()) {
            throw new GuiguException(500, "远程修改订单状态异常：" + orderNo);
        }
    }

    //3.2 TODO 判断支付类型：1302-充值
    if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentInfo.getPaymentType())) {
        Result result = accountFeignClient.rechargePaySuccess(orderNo);
        if (200 != result.getCode()) {
            throw new GuiguException(500, "远程修改余额异常：" + orderNo);
        }
    }
}
~~~

修改订单状态

~~~java
    @Override
    public void orderPaySuccess(String orderNo) {

        //1.查询订单状态，如果是已支付注解返回
        LambdaQueryWrapper<OrderInfo> queryWrapper = Wrappers.lambdaQuery(OrderInfo.class).eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNotEmpty(orderInfo) && SystemConstant.ORDER_STATUS_PAID.equals(orderInfo.getOrderStatus())){
            return ;
        }
        //2.修改订单状态
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
        orderInfoMapper.updateById(orderInfo);
        //3.新增商品购买记录
        UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
        userPaidRecordVo.setUserId(orderInfo.getUserId());
        userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
        userPaidRecordVo.setItemType(orderInfo.getItemType());
        userPaidRecordVo.setItemIdList(orderInfo.getOrderDetailList().stream().map(OrderDetail::getItemId).collect(Collectors.toList()));
        Result result = userFeignClient.savePaidRecord(userPaidRecordVo);
        if (200 != result.getCode()) {
            throw new GuiguException(500, "新增购买记录异常！");
        }
~~~

### 5、 充值

recharge_info

~~~java
@Override
	public Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo) {
		Long userId = AuthContextHolder.getUserId();
		RechargeInfo rechargeInfo = BeanUtil.copyProperties(rechargeInfoVo, RechargeInfo.class);
		rechargeInfo.setUserId(userId);
		rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
		String orderNo = "cz" + DateUtil.today().replace("-","") + IdUtil.getSnowflakeNextIdStr();
		rechargeInfo.setOrderNo(orderNo);
		rechargeInfoMapper.insert(rechargeInfo);
		HashMap<String, String> map = new HashMap<>();
		map.put("orderNo",orderNo);
		return map;
	}
~~~

用户支付成功，微信回调接口调用此接口

~~~java
@Transactional(rollbackFor = Exception.class)
	public void rechargePaySuccess(String orderNo) {
		//1.查询用户充值消息
		LambdaQueryWrapper<RechargeInfo> queryWrapper = Wrappers.lambdaQuery(RechargeInfo.class).eq(RechargeInfo::getOrderNo, orderNo);
		RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(queryWrapper);
		if (ObjectUtil.isNotEmpty(rechargeInfo) && SystemConstant.ORDER_STATUS_PAID.equals(rechargeInfo.getRechargeStatus())){
			return;
		}
		//2.更新用户账户余额
		int count = userAccountMapper.addAccount(rechargeInfo.getUserId(),rechargeInfo.getRechargeAmount());
		if (count == 0){
			throw new GuiguException(400,"充值失败");
		}
		//3.新增用户账户明细
		UserAccountDetail userAccountDetail = new UserAccountDetail();
		userAccountDetail.setUserId(rechargeInfo.getUserId());
		userAccountDetail.setTitle("充值" + rechargeInfo.getRechargeAmount());
		userAccountDetail.setTradeType(SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT);
		userAccountDetail.setAmount(rechargeInfo.getRechargeAmount());
		userAccountDetail.setOrderNo(orderNo);
		userAccountDetailMapper.insert(userAccountDetail);
		//4.更新用户充值状态
		rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
		rechargeInfoMapper.updateById(rechargeInfo);

	}
~~~

### 6、 充值记录

分页查询user_account_detail





## 十、 xxl-job

### 1、 概述

项目结构说明

~~~text
xxl-job-master：
    xxl-job-admin：调度中心
    xxl-job-core：公共依赖
~~~

### 2、 注册执行器

执行器配置说明

~~~properties
### 执行器注册的目标调度中心地址；为空则关闭自动注册；
xxl.job.admin.addresses=http://127.0.0.1:8080/xxl-job-admin
### 执行器通讯TOKEN [选填]：非空时启用；
xxl.job.accessToken=default_token
### 执行器AppName，执行器心跳注册分组依据；为空则关闭自动注册
xxl.job.executor.appname=xxl-job-executor-sample
### 执行器注册地址，为空则用IP：port作为执行器注册地址
xxl.job.executor.address=
### 执行器IP：默认为空表示自动获取IP，用于 "执行器注册" 和 "调度中心请求并触发任务"；
xxl.job.executor.ip=
### 执行器端口号：默认端口为9999，单机部署多个执行器时，注意要配置不同执行器端口；
xxl.job.executor.port=9999
### 执行器运行日志文件存储磁盘路径
xxl.job.executor.logpath=/data/applogs/xxl-job/jobhandler
### 执行器日志文件保存天数
xxl.job.executor.logretentiondays=30
~~~

执行器组件

~~~java
@Configuration
public class XxlJobConfig {
    private Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.address}")
    private String address;

    @Value("${xxl.job.executor.ip}")
    private String ip;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;


    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        logger.info(">>>>>>>>>>> xxl-job config init.");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setAddress(address);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        return xxlJobSpringExecutor;
    }
}
~~~

### 3、 xxl-job运行模式

BEAN模式是指在项目中编写Java类，在方法上标注@XxxlJob()注解，并在调度中心的jobHandle处填写任务名称

GLUE模式是直接在调度中心中编写任务代码，支持多种语言

### 4、 cron表达式

包含七个参数，分别为秒，分，时，日，月，星期，年，其中日和星期不能同时指定，指定了其中一个，另一个就要使用？占位

*通配符

，列表

-范围

/步长

？无意义占位符







































































