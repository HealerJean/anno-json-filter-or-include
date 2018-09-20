### 自定义注解： JSON过滤字段

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