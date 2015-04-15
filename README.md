[![Build Status](https://travis-ci.org/rahulsom/swaggydoc.svg?branch=develop)](https://travis-ci.org/rahulsom/swaggydoc)

Configuration
----

Add this to your `Config.groovy`

```groovy
swaggydoc {
    contact = "rahul.som@gmail.com"
    description = """\
        | This is a sample server Petstore server.  You can find out more about Swagger
        | at <a href="http://swagger.wordnik.com">http://swagger.wordnik.com</a> or on irc.freenode.net, #swagger.
        | For this sample,
        | you can use the api key "special-key" to test the authorization filters""".stripMargin()
    license = "Apache 2.0"
    licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html"
    termsOfServiceUrl = "http://helloreverb.com/terms/"
    title = "Swaggydoc Demo App"
    apiVersion = "1.0"
}
```

Usage
----

This plugin uses annotations from swagger-annotations when you need low level access.
However if you want to do things with less effort, you can use simpler Annotations.

The Simpler Annotations:
```groovy
@Transactional(readOnly = true)
@Api(value = 'demo')
class DemoController extends RestfulController {

    static responseFormats = ['json', 'xml']

    DemoController() {
        super(Demo)
    }

    @Override @SwaggyList
    def index() {
        super.index()
    }

    @Override @SwaggyShow
    def show() {
        super.show()
    }

    @Override @SwaggySave
    def save() {
        super.save()
    }

    @Override @SwaggyUpdate
    def update() {
        super.update()
    }

    @Override @SwaggyDelete
    def delete() {
        super.delete()
    }

    @Override @SwaggyPatch
    Object patch() {
        return super.patch()
    }
}
```

The low level annotations

```groovy
@Transactional(readOnly = true)
@Api(
        value = 'demo',
        description = 'Demo API',
        position = 0,
        produces = 'application/json,application/xml,text/html',
        consumes = 'application/json,application/xml,application/x-www-form-urlencoded'
)
class DemoController extends RestfulController {

    static responseFormats = ['json', 'xml']

    DemoController() {
        super(Demo)
    }

    @Override
    @ApiOperation(value = 'List demos', response = Demo, responseContainer = 'list')
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'offset', value = 'Records to skip', defaultValue = '0', paramType = 'query', dataType = 'int'),
            @ApiImplicitParam(name = 'max', value = 'Max records to return', defaultValue = '10', paramType = 'query', dataType = 'int'),
            @ApiImplicitParam(name = 'sort', value = 'Field to sort by', defaultValue = 'id', paramType = 'query', dataType = 'string'),
            @ApiImplicitParam(name = 'order', value = 'Order to sort by', defaultValue = 'asc', paramType = 'query', dataType = 'string'),
            @ApiImplicitParam(name = 'q', value = 'Query', paramType = 'query', dataType = 'string'),
    ])
    def index() {
        super.index()
    }

    @Override
    @ApiOperation(value = "Show Demo", response = Demo)
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to fetch', paramType = 'path', dataType = 'int', required = true),
    ])
    def show() {
        super.show()
    }

    @ApiOperation(value = "Save Demo", response = Demo)
    @ApiResponses([
            @ApiResponse(code = 422, message = 'Bad Entity Received'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = 'Demo'),
    ])
    @Override
    def save() {
        super.save()
    }

    @Override
    @ApiOperation(value = "Update Demo", response = Demo)
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
            @ApiResponse(code = 422, message = 'Bad Entity Received'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to update', paramType = 'path', dataType = 'int', required = true),
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = 'Demo')
    ])
    def update() {
        super.update()
    }

    @Override
    @ApiOperation(value = "Delete Demo")
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to delete', paramType = 'path', dataType = 'int', required = true),
    ])
    def delete() {
        super.delete()
    }

    @Override
    @ApiOperation(value = "Patch Demo", response = Demo)
    @ApiResponses([
            @ApiResponse(code = 400, message = 'Bad Id provided'),
            @ApiResponse(code = 404, message = 'Could not find Demo with that Id'),
            @ApiResponse(code = 422, message = 'Bad Entity Received'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'id', value = 'Id to patch', paramType = 'path', dataType = 'int', required = true),
            @ApiImplicitParam(name = 'body', paramType = 'body', required = true, dataType = 'Demo')
    ])
    Object patch() {
        return super.patch()
    }
}
```
