---
title: 自定义注解实现Json字段过滤
date: 2018-09-21 03:33:00
tags: 
- JSON
category: 
- JSON
description: 自定义注解实现Json字段过滤
---
<!-- image url 
https://raw.githubusercontent.com/HealerJean/HealerJean.github.io/master/blogImages
　　首行缩进
<font color="red">  </font>
-->

### 自定义注解： JSON过滤字段

1、使用[**@JsonInclude(JsonInclude.Include.NON_NULL)**]() 进行null值不给前台返回

2、使用自定义注解讲有结果集的字段设置为null，这样就通过1 不给前台返回了




#### 1、自定义注解

```

/**
 * @Desc: 自定义注解
 * @Author HealerJean
 * @Date 2018/9/20  上午11:20.
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JSON {
    Class<?> type();
    String include() default "";
    String filter() default "";
}

```


#### 4、自定义flterOrInclude

``` 

package com.hlj.springboot.dome.anno;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * @Desc: HealerJean
 * @Date:  2018/9/20 下午2:07.
 */

public class JsonAnnoSerializer {

    static final String DYNC_INCLUDE = "DYNC_INCLUDE";
    static final String DYNC_FILTER = "DYNC_FILTER";
    ObjectMapper mapper = new ObjectMapper();

    @JsonFilter(DYNC_FILTER)
    interface DynamicFilter {
    }

    @JsonFilter(DYNC_INCLUDE)
    interface DynamicInclude {
    }

    /**
     * @param clazz 需要设置规则的Class
     * @param include 转换时包含哪些字段
     * @param filter 转换时过滤哪些字段
     */
    public void filter(Class<?> clazz, String include, String filter) {
        if (clazz == null) return;
        if (include != null && include.length() > 0) {
            mapper.setFilterProvider(new SimpleFilterProvider().addFilter(DYNC_INCLUDE,
                    SimpleBeanPropertyFilter.filterOutAllExcept(include.split(","))));
            mapper.addMixIn(clazz, DynamicInclude.class);
        } else if (filter !=null && filter.length() > 0) {
            mapper.setFilterProvider(new SimpleFilterProvider().addFilter(DYNC_FILTER,
                    SimpleBeanPropertyFilter.serializeAllExcept(filter.split(","))));
            mapper.addMixIn(clazz, DynamicFilter.class);
        }
    }

    public String toJson(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }
}

```

#### 3、自定义Aop拦截

``` 
package com.hlj.springboot.dome.anno.aop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlj.springboot.dome.anno.JsonAnnoSerializer;
import com.hlj.springboot.dome.anno.JSON;
import com.hlj.springboot.dome.common.data.ResponseBean;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONTokener;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @Desc:
 * @Author HealerJean
 * @Date 2018/9/20  上午11:21.
 */
@Aspect
@Component
public class JsonInterceptor {

    /**
     * ObjectMapper
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    /**
     *
     * @param point
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.hlj.springboot.dome.anno.JSON) || @within(com.hlj.springboot.dome.anno.JSON)")
    protected Object invoke(ProceedingJoinPoint point) throws Throwable {
        JSON json = null;
        Signature signature = point.getSignature();
        if (signature instanceof MethodSignature) {
            Method method = ((MethodSignature) signature).getMethod();
            json = method.getAnnotation(JSON.class);
        }
        if (json == null) {
            return point.proceed();
        }

        try {
            Object object = point.proceed();
            JsonAnnoSerializer jsonSerializer = new JsonAnnoSerializer();
            jsonSerializer.filter(json.type(), json.include(), json.filter());

            //如果包装类是ResponBean
            if(ResponseBean.class.getName().equals(  object.getClass().getName())){ //如果返回类型是ResponseBean
                ResponseBean responseBean = (ResponseBean)object;
                 String innerJsonStr = jsonSerializer.toJson(responseBean.getResult());
                 Object jsonT = new JSONTokener(innerJsonStr).nextValue();

                if(jsonT instanceof JSONObject){
                       responseBean.setResult (OBJECT_MAPPER.readValue(innerJsonStr, json.type()));
                }else if (jsonT instanceof JSONArray) {
                       responseBean.setResult (OBJECT_MAPPER.readValue(innerJsonStr, new TypeReference<List>() { } ));
                }
                return  responseBean ;
            }

            //没有包装类
            String resultJson = jsonSerializer.toJson(object);
            Object jsonT = new JSONTokener(resultJson).nextValue();
            if(jsonT instanceof JSONObject){
                return OBJECT_MAPPER.readValue(resultJson, json.type());
            }else if (jsonT instanceof JSONArray) {
                return OBJECT_MAPPER.readValue(resultJson, new TypeReference<List>() { } );
            }
            return   object;
        } catch (Exception e) {
            throw e;
        }
    }



}

```

#### 4、进行测试

```java
/**
 * 测试实体类，这个随便;
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DemoEntity {

	private Long id;

	private String name;

	private Long balance;

}
```



```

package com.hlj.springboot.dome.common.moudle.controller;


import com.hlj.springboot.dome.anno.JSON;
import com.hlj.springboot.dome.common.data.ResponseBean;
import com.hlj.springboot.dome.common.entity.DemoEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author HealerJean
 * @Date 2018/3/22  上午10:22.
 */
@Controller
public class HomeController {



    @GetMapping("responBean")
    @ResponseBody
    @JSON(type =DemoEntity.class, include = "name")
    public ResponseBean jsonIgnore(){
        try {
            DemoEntity demoEntity = new DemoEntity().setId(1L).setName("healerjean").setBalance(24L);
            return  ResponseBean.buildSuccess(demoEntity);
        }catch (Exception e){
            return  ResponseBean.buildFailure(e.getMessage());
        }
    }

    @GetMapping("responBean/list")
    @ResponseBody
    @JSON(type =DemoEntity.class, filter = "id")
    public ResponseBean list(){
        try {
            List<DemoEntity> demoEntityList  = new ArrayList<>();
            DemoEntity demoEntity = new DemoEntity().setId(1L).setName("healerjean").setBalance(24L);
            DemoEntity demoEntity2 = new DemoEntity().setId(1L).setName("healerjean").setBalance(24L);
            demoEntityList.add(demoEntity);
            demoEntityList.add(demoEntity2);
            return  ResponseBean.buildSuccess(demoEntityList);
        }catch (Exception e){
            return  ResponseBean.buildFailure(e.getMessage());
        }
    }

    @GetMapping("jsonFilter")
    @ResponseBody
    @JSON(type =DemoEntity.class, filter = "id")
    public DemoEntity jsonFilter(){
        try {
            DemoEntity demoEntity = new DemoEntity().setId(1L).setName("healerjean").setBalance(24L);
            return  demoEntity;
        }catch (Exception e){
            return  null;
        }
    }

    @GetMapping("jsonInclude/list")
    @ResponseBody
    @JSON(type =DemoEntity.class, include = "id")
    public List<DemoEntity> jsonIncludeList(){
        try {
            List<DemoEntity> demoEntityList  = new ArrayList<>();
            DemoEntity demoEntity = new DemoEntity().setId(1L).setName("healerjean").setBalance(24L);
            DemoEntity demoEntity2 = new DemoEntity().setId(1L).setName("healerjean").setBalance(24L);
            demoEntityList.add(demoEntity);
            demoEntityList.add(demoEntity2);
            return  demoEntityList;
        }catch (Exception e){
            return  null;
        }
    }



}


```

#### 5、浏览器访问
```http
http://localhost:8080/jsonFilter
```



``` 
{
  "name": "healerjean",
  "balance": 24
}
```

```http
http://localhost:8080/jsonInclude/list
```



``` 
[
  {
    "id": 1
  },
  {
    "id": 1
  }
]
```
```http
http://localhost:8080/responBean
```



``` 
{
  "success": true,
  "result": {
    "name": "healerjean"
  },
  "message": "",
  "code": "200",
  "date": "1555644132805"
}
```

```http
http://localhost:8080/responBean/list
```



``` 
{
  "success": true,
  "result": [
    {
      "name": "healerjean",
      "balance": 24
    },
    {
      "name": "healerjean",
      "balance": 24
    }
  ],
  "message": "",
  "code": "200",
  "date": "1555644151816"
}

```


## [ 代码下载](https://github.com/HealerJean/anno-json-filter-or-include.git)






<br/><br/><br/>
如果满意，请打赏博主任意金额，感兴趣的在微信转账的时候，添加博主微信哦， 请下方留言吧。可与博主自由讨论哦

|支付包 | 微信|微信公众号|
|:-------:|:-------:|:------:|
|![支付宝](https://raw.githubusercontent.com/HealerJean/HealerJean.github.io/master/assets/img/tctip/alpay.jpg) | ![微信](https://raw.githubusercontent.com/HealerJean/HealerJean.github.io/master/assets/img/tctip/weixin.jpg)|![微信公众号](https://raw.githubusercontent.com/HealerJean/HealerJean.github.io/master/assets/img/my/qrcode_for_gh_a23c07a2da9e_258.jpg)|


