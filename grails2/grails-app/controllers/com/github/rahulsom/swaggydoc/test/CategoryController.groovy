package com.github.rahulsom.swaggydoc.test

import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiOperation
import grails.converters.JSON
import grails.rest.RestfulController

@Api(value = 'Categories')
class CategoryController extends RestfulController {
    CategoryController() {
        super(Category)
    }

    @ApiOperation(value = 'List all categories', httpMethod = 'GET', response = Category, responseContainer = "array")
    def all() {
        render Category.list() as JSON
    }

}