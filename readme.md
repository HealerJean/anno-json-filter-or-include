### 自定义注解： JSON过滤字段


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
import java.util.ArrayList;
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
                    JSONObject jsonObject = (JSONObject)jsonT;
                    responseBean.setResult(new ObjectMapper().readValue(jsonObject.toString(),json.type()));
                    return responseBean ;

                 }else if (jsonT instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) jsonT;
                    List objects = new ArrayList<>();
                    for(int i = 0 ;i <jsonArray.size() ;i++){
                        objects.add(new ObjectMapper().readValue(jsonArray.get(i).toString(),json.type()));
                    }
                    responseBean.setResult(objects);
                    return responseBean ;
                }


                return  responseBean ;
            }

            //没有包装类
            String resultJson = jsonSerializer.toJson(object);

            Object jsonT = new JSONTokener(resultJson).nextValue();
            if(jsonT instanceof JSONObject){
                JSONObject jsonObject = (JSONObject)jsonT;
                return new ObjectMapper().readValue(jsonObject.toString(),json.type());

            }else if (jsonT instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) jsonT;

                List objects = new ArrayList<>();
                for(int i = 0 ;i <jsonArray.size() ;i++){
                    objects.add(new ObjectMapper().readValue(jsonArray.get(i).toString(),json.type()));
                }
                return objects ;
            }
            return   object;
        } catch (Exception e) {
            throw e;
        }
    }



}

```

#### 4、进行测试

```


    @GetMapping("responBean")
    @ResponseBody
    @JSON(type =DemoEntity.class, include = "id")
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
    @JSON(type =DemoEntity.class, filter = "id")
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


```

#### 5、浏览器访问
[http://localhost:8080/responBean](http://localhost:8080/responBean)

``` 
{
    "success": true,
    "result": {
        "id": 1,
        "name": null,
        "balance": null
    },
    "message": "",
    "code": "200",
    "date": "1537424885236"
}

```


[http://localhost:8080/responBean/list](http://localhost:8080/responBean/list)

``` 
{
    "success": true,
    "result": [
        {
            "id": null,
            "name": "healerjean",
            "balance": 24
        },
        {
            "id": null,
            "name": "healerjean",
            "balance": 24
        }
    ],
    "message": "",
    "code": "200",
    "date": "1537424921694"
}
```
[http://localhost:8080/jsonFilter](http://localhost:8080/jsonFilter)

``` 

{
    "id": null,
    "name": "healerjean",
    "balance": 24
}
```

[http://localhost:8080/jsonInclude/list](http://localhost:8080/jsonInclude/list)

``` 
[
    {
        "id": null,
        "name": "healerjean",
        "balance": 24
    },
    {
        "id": null,
        "name": "healerjean",
        "balance": 24
    }
]

```